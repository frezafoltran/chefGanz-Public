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
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ai.api.model.AIResponse;

import static ai.picovoice.porcupine.demo.ParsingUtils.setBottomNavigation;

/**
* This class corresponds to the RecipeIngredient activity. It is an activity specific to each recipe. It lists
* the ingredients of each recipe in a clickable list. 
*/
public class RecipeIngredient extends AppCompatActivity {

    private String LOG_TAG = "---RECIPE INGREDIENT TAG";
    private SlidingUpPanelLayout mLayout;

    //for now these store the last user query and assistant response
    //TODO display recent history of conversation to user
    private String userResponseStr;
    private String assistantResponseStr;

    private ImageView triggerAssistant;

    String recipe_id = "";
    String recipe_name = "";
    String recipe_image = "";
    ArrayList<IngredientData> ingredients = new ArrayList<IngredientData>();
    private String activityName = "RecipeIngredient";

    private String requestedRecipe = "";
    private String requestedRecipePhotoUrl = "";

    private SharedPreferences settings;

    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.transition.left_slide, R.transition.no_action);
        setContentView(R.layout.activity_recipe_ingredient);
        Utils.configurePorcupine(this);

        application = this.getApplication();
        ((GanzApplication) application).initBumblebeeSpecs(this);
        ((GanzApplication) application).initBumblebeeVoice();


        triggerAssistant = findViewById(R.id.trigger_ganz);


        findViewById(R.id.start_cooking).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ParsingUtils.navigateToTarget(application, getApplicationContext(),
                        RecipeInstruction.class, recipe_id);
            }
        });

        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        setBottomNavigation(bottomNavigationView, this, triggerAssistant, application, "RecipeIngredient", mLayout);

    }

    @Override
    protected void onStart(){
        super.onStart();

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

        ((GanzApplication) application).initAssistant(this);

        updateRecipeInfo();
        settings = getSharedPreferences("curRecipe", 0);

        //parseResults(lambdaResult);
        parseRecipe();

        TextView recipeNameView = findViewById(R.id.recipe_name);
        recipeNameView.setText("Ingredients for " + recipe_name);

        final ListView list = findViewById(R.id.ingredient_list_main);
        IngredientAdapter ingredientAdapter = new IngredientAdapter(RecipeIngredient.this, this, ingredients);
        list.setAdapter(ingredientAdapter);

    }

    @Override
    public void onBackPressed () {
        super.onBackPressed();
        //Intent openPrevActivity = new Intent(RecipeIngredient.this, RecipeIntro.class);
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

    private void parseRecipe(){
        recipe_name = settings.getString("name", "");
        recipe_image = settings.getString("image", "");

        List<String> curIngredients = new ArrayList<String>(Arrays.asList(
                settings.getString("ingredients", "").split("&")));

        ingredients = new ArrayList<IngredientData>();
        for (int i = 0; i < curIngredients.size(); i ++){

            List<String> curIngredient = new ArrayList<String>(Arrays.asList(
                    curIngredients.get(i).split("::")));

            String fullIng = curIngredient.get(0);
            String ingName = curIngredient.get(1);
            String count = curIngredient.get(2);
            String unit = curIngredient.get(3);


            if (curIngredient.size() > 1) {
                ingredients.add(new IngredientData(ingName, count, unit));
            }
        }

    }

    public void showPopup(){
        ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                requestedRecipe, requestedRecipePhotoUrl);
    }

    public void callback(AIResponse aiResponse) {
        if (aiResponse != null) {
            // process aiResponse
            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);
            assistantResponseStr = answerObj.txtResponse;

            ((GanzApplication) application).getBumblebeeVoice().speak(assistantResponseStr,
                    TextToSpeech.QUEUE_FLUSH, null);

            ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                    answerObj);

            requestedRecipe = answerObj.recipeIds.get(0);
            requestedRecipePhotoUrl = answerObj.photoUrls.get(0);

        } else {
            Log.d("No Response", "No response from assistant");
        }
    }

    public void goToIngredientInfo(String ingredient_name){
        ((GanzApplication) application).getmSpeechRecognizer().stopListening();
        ((GanzApplication) application).stopListeningHotword();

        Intent intent = new Intent(this, IngredientInfo.class);
        Bundle id = new Bundle();
        id.putString("ingredient_name", ingredient_name);
        intent.putExtras(id);

        startActivity(intent);
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

    /**
     * Updates the UI after response from assistant.
     *
     * @param match the assistant response
     */
    public void updateUIOnResponse(String match) {
        userResponseStr = match;
    }

}
