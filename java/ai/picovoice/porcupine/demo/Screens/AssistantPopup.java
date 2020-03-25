package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.w3c.dom.Text;

import ai.api.model.AIResponse;

/**
* This class takes care of the popup showing the dialog between the user and bumblebee.
* It shows the latest user input along with the latest asssistant response. The information to be displayed is passed to this
* activity through a Bundle.
*/
public class AssistantPopup extends AppCompatActivity {

    private Application application;
    private String LOG_TAG = "--- ASSISTANT POPUP";
    private String activityName = "AssistantPopup";
    private Boolean checkIfDone = false;

    private Button triggerBumblebee;
    private EditText inputEditText;
    private TextView assistantResponseText;
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_assistant_popup);

        //avoid keyboard from automatically showing
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        triggerBumblebee = findViewById(R.id.triggerBumblebee);
        inputEditText = findViewById(R.id.inputEditText);
        assistantResponseText = findViewById(R.id.assistant_response);
        ctx = this;

        application = this.getApplication();

        final Bundle b = getIntent().getExtras();
        if(b != null) {

            inputEditText.setText(b.getString("userInput"));

            TextView assistant_response = findViewById(R.id.assistant_response);
            assistant_response.setText(b.getString("assistant_response"));

            //show button to go to recipe
            final String recipeId = b.getString("recipeId");
            final String recipePhotoUrl = b.getString("recipePhotoUrl");

            // if a valid recipeId is passed, show button to go to the shown recipe along with
            // a recipe image.
            if (recipeId.length() > 0 && !recipeId.equals("!")){

                // indicates popup is being open for first time
                if (!recipeId.substring(0,1).equals("!")) {
                    checkIfDone = true;
                    isTTSSpeaking();
                }

                Button goToRecipe = findViewById(R.id.goToRecipe);
                Button moreResults = findViewById(R.id.moreResultsButton);

                moreResults.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                        Intent intent = new Intent(AssistantPopup.this, SearchResults.class);
                        Bundle id = new Bundle();

                        id.putString("allRecipePhotoUrls", b.getString("allRecipePhotoUrls"));
                        id.putString("allRecipeIds", b.getString("allRecipeIds"));
                        id.putString("allRecipeNames", b.getString("allRecipeNames"));

                        intent.putExtras(id);
                        startActivity(intent);
                    }

                });

                goToRecipe.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //goBack(view);
                        Intent intent = new Intent(AssistantPopup.this, RecipeIntro.class);
                        Bundle id = new Bundle();
                        // indicates popup is being open for first time
                        if (!recipeId.substring(0,1).equals("!")) {
                            id.putString("recipe_id", recipeId);
                        }
                        else {
                            id.putString("recipe_id", recipeId.substring(1));
                        }
                        intent.putExtras(id);
                        startActivity(intent);
                    }

                });

                ImageView iv = findViewById(R.id.recipe_image);
                Log.d("-------------photo", recipePhotoUrl);
                new DownloadImageTask(iv).execute(recipePhotoUrl);
                //LinearLayout imgWrapper = findViewById(R.id.recipe_image_wrapper);
                //imgWrapper.setVisibility(View.VISIBLE);

                iv.setVisibility(View.VISIBLE);
                goToRecipe.setVisibility(View.VISIBLE);
                moreResults.setVisibility(View.VISIBLE);
            }

        }

    }

    @Override
    public void onStart(){
        super.onStart();
        triggerBumblebee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParsingUtils.sendMessage(inputEditText.getText().toString(), ((GanzApplication) application).getAiRequest(),
                        ctx, ((GanzApplication) application).getAiDataService(),
                        ((GanzApplication) application).getCustomAIServiceContext(), activityName);
            }
        });
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            onBackPressed();
        } else if (item.getItemId() == R.id.action_done) {
            setResult(RESULT_OK, new Intent().putExtra("sortBy", "name"));
            onBackPressed();
        }
        return true;
    }

    public void goBack(View view) {
        setResult(RESULT_CANCELED);
        onBackPressed();
    }

    
    /**
    * This method continuously checks if the assistant is speaking. When it is done, the method starts listening 
    * for a user input (e.g. this is useful when the assistant asks the user a question)
    */
    public void isTTSSpeaking(){

        final Handler h = new Handler();

        Runnable r = new Runnable() {

            public void run() {

                while(checkIfDone) {
                    if (!((GanzApplication) application).getBumblebeeVoice().isSpeaking()) {
                        checkIfDone = false;
                        ((GanzApplication) application).stopListeningHotword();
                        ((GanzApplication) application).getmSpeechRecognizer().stopListening();

                        ((GanzApplication) application).getmSpeechRecognizer().startListening(
                                ((GanzApplication) application).getmSpeechRecognizerIntent());

                        //nextQuestion.setVisibility(View.VISIBLE); // recover button once assitant is done speaking
                    }

                    h.postDelayed(this, 1000);
                }
            }
        };

        h.postDelayed(r, 1000);
    }

    /*
     * Process the response from assistant
     * */
    public void callback(AIResponse aiResponse) {
        if (aiResponse != null) {
            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);
            assistantResponseText.setText(answerObj.recipeNames.get(0));
            ((GanzApplication) application).getBumblebeeVoice().speak(answerObj.recipeNames.get(0),
                    TextToSpeech.QUEUE_FLUSH, null);
        } else {
            Log.d(LOG_TAG, "No response from assistant");
        }
    }

}
