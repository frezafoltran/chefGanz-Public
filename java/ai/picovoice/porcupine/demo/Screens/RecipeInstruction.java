package ai.picovoice.porcupine.demo;


import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ai.api.model.AIResponse;
import pl.droidsonroids.gif.GifImageView;

import static ai.picovoice.porcupine.demo.ParsingUtils.setBottomNavigation;

/**
* This class corresponds to the RecipeInstruction activity. Its goal is to show the user the current steps 
* for a particular recipe. 
*/
public class RecipeInstruction extends AppCompatActivity{

    private String LOG_TAG = "---RECIPE INSTRUCTION TAG";

    private SlidingUpPanelLayout mLayout;

    //for now these store the last user query and assistant response
    //TODO display recent history of conversation to user
    private String userResponseStr;
    private String assistantResponseStr;
    private TextView currInstruction;
    private int curInstructionIndex = 0;
    private int curStepIndex = -1;

    String lambdaResult;

    String recipe_id = "";
    String recipe_name = "";
    ArrayList<String> cooking_instructions = new ArrayList<String>();
    ArrayList<String []> cooking_steps = new ArrayList<>();
    private String curSentToRead;


    private String requestedRecipe = "";
    private String requestedRecipePhotoUrl = "";

    private ImageView triggerAssistant;

    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private int width;

    private String activityName = "RecipeInstruction";
    private String recipe_image = "";
    private GifImageView listeningGif;

    // for local language processing
    HashSet<String> nextStepsSyns = ProcessDialog.nextStepSyns();
    HashSet<String> previousStepsSyns = ProcessDialog.previousStepSyns();
    HashSet<String> pauseStepsSyns = ProcessDialog.pauseStepSyns();
    HashSet<String> repeatStepsSyns = ProcessDialog.repeatStepSyns();

    private SharedPreferences settings;

    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.transition.left_slide, R.transition.no_action);
        setContentView(R.layout.activity_recipe_instruction);
        Utils.configurePorcupine(this);

        application = this.getApplication();
        ((GanzApplication) application).initBumblebeeSpecs(this);
        ((GanzApplication) application).initBumblebeeVoice();

        triggerAssistant = findViewById(R.id.trigger_ganz);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;

        // Start recognizing hot-word.
        ((GanzApplication) application).initListeningHotword(this,
                ((GanzApplication) application).getBumblebeeSpecs(), triggerAssistant,
                ((GanzApplication) application).getBumblebeeVoice());

        //initialize speech recognizer. Will only start listening when assistant is triggered
        ((GanzApplication) application).initSpeechRecognizer(this, application, triggerAssistant,
                ((GanzApplication) application).getBumblebeeVoice(), activityName);

        //set listener on button to trigger assistant. Button is triggered used when hot-word is heard
        findViewById(R.id.trigger_ganz).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ((GanzApplication) application).stopListeningHotword();
                ((GanzApplication) application).getmSpeechRecognizer().stopListening();
                ((GanzApplication) application).getmSpeechRecognizer().startListening(
                        ((GanzApplication) application).getmSpeechRecognizerIntent());

            }
        });

        findViewById(R.id.curr_instruction).setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    float touchLocation = event.getAxisValue(0);
                    if (touchLocation < width / 4) {
                        previousStep();
                    } else if(touchLocation > 3* width / 4) {
                        nextStep();
                    }
                }
                return false;
            }});

        ((GanzApplication) application).initAssistant(this);

        updateRecipeInfo();

        settings = getSharedPreferences("curRecipe", 0);

        parseRecipe();

        TextView recipeNameView = findViewById(R.id.recipe_name);
        recipeNameView.setText(recipe_name);

        currInstruction = findViewById(R.id.curr_instruction);

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        setBottomNavigation(bottomNavigationView, this, triggerAssistant, application, "RecipeInstruction", mLayout);

    }

    @Override
    protected void onStart(){
        super.onStart();
        final ImageView iv = (ImageView) findViewById(R.id.recipe_image);
        new DownloadImageTask(iv).execute(recipe_image);
        nextStep();

    }

    @Override
    public void onBackPressed () {
        super.onBackPressed();
        ((GanzApplication) application).getBumblebeeVoice().stop();
        //Intent openPrevActivity = new Intent(RecipeInstruction.this, RecipeIngredient.class);
        //openPrevActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //startActivityIfNeeded(openPrevActivity, 0);
    }

    /**
     * Method to request recipe info based on the id passed in bundle.
     */
    private void updateRecipeInfo(){
        Bundle b = getIntent().getExtras();
        if(b != null)
            recipe_id = b.getString("recipe_id");

    }

    private void nextStep(){

        curStepIndex ++;
        // done with the sentences in step. Move on to next step
        if (curStepIndex >= cooking_steps.get(curInstructionIndex).length){
            curStepIndex = 0;
            curInstructionIndex ++;
        }

        // done with all steps. go back to first
        if (curInstructionIndex >= cooking_instructions.size()){
            curInstructionIndex = 0;
            curStepIndex = 0;
        }

        curSentToRead = cooking_steps.get(curInstructionIndex)[curStepIndex];
        if (curSentToRead.trim().equals("")){
            nextStep();
        }
        else {
            String displayStep = cooking_instructions.get(curInstructionIndex)
                    .replaceAll(curSentToRead, "<font size = '3' color='#FF8C00'>" + curSentToRead + "</font>");

            currInstruction.setText(Html.fromHtml(displayStep));
            currInstruction.setMovementMethod(new ScrollingMovementMethod());
            readSteps();
        }
    }

    private void previousStep(){

        curStepIndex --;
        if (curStepIndex < 0){
            curStepIndex = 0;
            curInstructionIndex --;
        }

        if (curInstructionIndex < 0){
            curInstructionIndex = cooking_instructions.size() - 1;
            curStepIndex = 0;
        }

        curSentToRead = cooking_steps.get(curInstructionIndex)[curStepIndex];
        if (curSentToRead.trim().equals("")){
            previousStep();
        }
        else {
            String displayStep = cooking_instructions.get(curInstructionIndex)
                    .replaceAll(curSentToRead, "<font size = '3' color='#FF8C00'>" + curSentToRead + "</font>");

            currInstruction.setText(Html.fromHtml(displayStep));
            currInstruction.setMovementMethod(new ScrollingMovementMethod());

            readSteps();
        }
    }

    public void showPopup(){
        ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                requestedRecipe, requestedRecipePhotoUrl);
    }

    private void parseRecipe(){
        recipe_name = settings.getString("name", "");
        recipe_image = settings.getString("image", "");

        List<String> curSteps = new ArrayList<String>(Arrays.asList(
                settings.getString("steps", "").split("&")));

        for (int i = 0; i < curSteps.size(); i ++){

            cooking_instructions.add(curSteps.get(i));

            String [] sentences = curSteps.get(i).split("\\.");
            cooking_steps.add(sentences);
        }
    }

    private void parseResults(String lambdaResponse){

        try {

            JSONObject json = new JSONObject(lambdaResponse);

            recipe_name = json.get("recipe_name").toString();

            JSONObject instructionJson = new JSONObject(json.get("cooking_instructions").toString());
            Iterator<?> instructionKeys = instructionJson.keys();

            while (instructionKeys.hasNext()){
                String key = (String) instructionKeys.next();

                JSONObject stepJson = new JSONObject(instructionJson.get(key).toString());
                Log.d("==========TAG", stepJson.get("to_be_read").toString());

                cooking_instructions.add(stepJson.get("to_be_read").toString());
                String [] sentences = stepJson.get("to_be_read").toString().split("\\.");

                cooking_steps.add(sentences);
            }

        }
        catch (JSONException e){
            Log.e("Lambda parsing error:", e.toString());
        }
    }

    public void callbackLocal(String match){

        if (nextStepsSyns.contains(match.toLowerCase())){
            nextStep();
        }
        else if (previousStepsSyns.contains(match.toLowerCase())){
            previousStep();
        }
        else if (pauseStepsSyns.contains(match.toLowerCase())){
            try {
                if (((GanzApplication) application).getBumblebeeVoice().isSpeaking()) {
                    ((GanzApplication) application).getBumblebeeVoice().stop();
                }
            }
            catch (NullPointerException e){
                Log.e("Error stopping voice:", "Not yet initialized");
            }
        }
        else if (repeatStepsSyns.contains(match.toLowerCase())){
            readSteps();
        }
    }

    public void callback(AIResponse aiResponse) {
        if (aiResponse != null) {
            // process aiResponse
            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);
            assistantResponseStr = answerObj.txtResponse;

            switch (assistantResponseStr){
                case "next step":
                    nextStep();
                    break;
                case "previous step":
                    previousStep();
                    break;
                case "repeat step":
                    readSteps();
                    break;
                case "pause":

                    try {
                        if (((GanzApplication) application).getBumblebeeVoice().isSpeaking()) {
                            ((GanzApplication) application).getBumblebeeVoice().stop();
                        }
                    }
                    catch (NullPointerException e){
                        Log.e("Error stopping voice:", "Not yet initialized");
                    }
                    break;

                default:
                    ((GanzApplication) application).getBumblebeeVoice().speak(assistantResponseStr,
                            TextToSpeech.QUEUE_FLUSH, null);

                    ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                            answerObj);
            }

            requestedRecipe = answerObj.recipeIds.get(0);
            requestedRecipePhotoUrl = answerObj.photoUrls.get(0);


        } else {
            Log.d("No Response", "No response from assistant");
        }
    }

    public void readSteps(){

        ((GanzApplication) application).getBumblebeeVoice().speak(
                curSentToRead, TextToSpeech.QUEUE_FLUSH, null);
    }

    /**
     * Check the result of the record permission request.
     * @param requestCode request code of the permission request.
     * @param permissions requested permissions.
     * @param grantResults results of the permission requests.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        // We only ask for record permission.
        if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {

        } else {
            ((GanzApplication) application).initListeningHotword(this,
                    ((GanzApplication) application).getBumblebeeSpecs(), triggerAssistant,
                    ((GanzApplication) application).getBumblebeeVoice());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.profile) {

            Intent intent = new Intent(this, EditProfile.class);

            Bundle id = new Bundle();
            id.putString("calling_activity", activityName);
            intent.putExtras(id);

            startActivity(intent);

        }
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 100) {
            if (resultCode == RESULT_OK) {
                Log.d(LOG_TAG, "onActivityResult: sortBy "+ data.getStringExtra("sortBy"));
            } else {
                Log.d(LOG_TAG, "onActivityResult: canceled");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ((GanzApplication) application).getBumblebeeVoice().shutdown();
    }

    /**
     * Updates the UI after response from assistant.
     *
     * @param match the assistant response
     */
    public void updateUIOnResponse(String match) {
        userResponseStr = match;
    }

}
