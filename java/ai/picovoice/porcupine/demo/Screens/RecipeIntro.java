package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ai.api.model.AIResponse;
import ai.picovoice.porcupine.demo.Util.AssistantUtil;
import ai.picovoice.porcupine.demo.Util.HotwordUtil;
import pl.droidsonroids.gif.GifImageView;

import static ai.picovoice.porcupine.demo.ParsingUtils.setBottomNavigation;


/**
 * This class corresponds to the RecipeIntro activity. Its main goal is to provide an overview of the current recipe
 * to the user, along with a link to the recipe's ingredients page.
 */
public class RecipeIntro extends MainBaseActivity implements Serializable {

    private String LOG_TAG = "---RECIPE INTRO TAG";

    private SlidingUpPanelLayout mLayoutSimilar;
    private SlidingUpPanelLayout mLayout;

    ArrayList<SubjectData> similarRecipeArray = new ArrayList<SubjectData>();

    //for now these store the last user query and assistant response
    //TODO display recent history of conversation to user
    private String userResponseStr;
    private String assistantResponseStr;

    private ImageView triggerAssistant;
    private GifImageView listeningGif;
    private ImageView gooseLogo;
    String lambdaResult;

    private LinearLayout popupWrapper;
    private BottomNavigationView bottomNavigationView;

    String recipe_id = "";
    String recipe_name = "";
    String recipe_time = "";
    String recipe_image;

    //holds id of requested recipe
    private String requestedRecipe = "";
    private String requestedRecipePhotoUrl = "";
    private String activityName = "RecipeIntro";


    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    //assistant popup
    private EditText userInput;
    private TextView assistantInput;
    private ImageView recipeImagePopup;
    private Button goToRecipePopup;
    private Button moreRecipesPopup;

    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.transition.left_slide, R.transition.no_action);
        setContentView(R.layout.activity_recipe_intro);

        application = this.getApplication();

        initViews();
        AssistantUtil.initSlidingPopup(mLayout, popupWrapper);
        AssistantUtil.initTriggerButton(triggerAssistant, listeningGif, application);

        Utils.configurePorcupine(this);
        HotwordUtil.initHotword(application, this,triggerAssistant, activityName,
                userInput, assistantInput, recipeImagePopup, goToRecipePopup, moreRecipesPopup);


        initSimilarRecipes();

        getRecipeId();

        findViewById(R.id.go_to_ingredient).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ParsingUtils.navigateToTarget(application, getApplicationContext(),
                        RecipeIngredient.class, recipe_id);
            }
        });

        LinearLayout view = findViewById(R.id.main_activity_wrapper);

        view.setOnTouchListener(new OnSwipeTouchListener(this) {
            @Override
            public void onSwipeLeft() {
                findViewById(R.id.go_to_ingredient).performClick();
            }
        });


        BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);
        setBottomNavigation(bottomNavigationView, this, triggerAssistant, application, activityName, mLayout);

        // saves current recipe in local memory for fast access
        settings = getSharedPreferences("curRecipe", 0);
        editor = settings.edit();
    }

    @Override
    protected void onStart(){
        super.onStart();

        String recipeById = ParsingUtils.getRecipeUrl(recipe_id);
        HttpGetRequest getRequest = new HttpGetRequest();
        getRequest.setActivity(this, activityName);

        try {
            lambdaResult = getRequest.execute(recipeById).get();
        }
        catch (ExecutionException | InterruptedException e){
            Log.e("Error in GET", e.toString());
        }


    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        //Intent openPrevActivity = new Intent(RecipeIntro.this, MainActivity.class);
        //openPrevActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //startActivityIfNeeded(openPrevActivity, 0);
    }

    public void initSimilarRecipes(){
        for (int i = 0; i < 5; i ++){
            similarRecipeArray.add(new SubjectData("Similar recipe" + i, "-2", "https://frezafoltran.github.io/goose_logo.JPG"));
        }

        ListView similarRecipesList = (ListView) findViewById(R.id.similar_recipes_list);

        CustomAdapter customAdapter = new CustomAdapter(RecipeIntro.this, this,
                application, similarRecipeArray);

        similarRecipesList.setAdapter(customAdapter);

        mLayoutSimilar = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout_similar_recipes);
        mLayoutSimilar.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
                Log.i(LOG_TAG, "onPanelSlide, offset " + slideOffset);
            }

            @Override
            public void onPanelStateChanged(View panel, SlidingUpPanelLayout.PanelState previousState, SlidingUpPanelLayout.PanelState newState) {
                Log.i(LOG_TAG, "onPanelStateChanged " + newState);
            }
        });
        mLayoutSimilar.setFadeOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mLayoutSimilar.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            }
        });
    }

    public void initViews(){
        /**
         * Initializes the views for activity
         */

        triggerAssistant = findViewById(R.id.trigger_ganz);
        gooseLogo = findViewById(R.id.goose_logo);
        listeningGif = findViewById(R.id.listening_gif);
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        popupWrapper = findViewById(R.id.assistant_popup_head);
        assistantInput = findViewById(R.id.assistant_input);
        userInput = findViewById(R.id.user_input);

        recipeImagePopup = findViewById(R.id.recipe_image_popup);
        goToRecipePopup = findViewById(R.id.go_to_recipe_popup);
        moreRecipesPopup = findViewById(R.id.more_results_popup);

        mLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
    }

    /**
     * Method to request recipe info based on the id passed in bundle.
     */
    private void getRecipeId(){
        Bundle b = getIntent().getExtras();
        if(b != null)
            recipe_id = b.getString("recipe_id");

    }

    public void updateRecipeInfo(){
        TextView recipeNameView = findViewById(R.id.recipe_name);
        recipeNameView.setText(recipe_name);

        TextView recipeTimeView = findViewById(R.id.recipe_time);
        recipeTimeView.setText("(" + recipe_time + " min)");
        final ImageView iv = (ImageView) findViewById(R.id.recipe_image);
        new DownloadImageTask(iv).execute(recipe_image);
    }

    private String parseSteps(JSONObject json){

        String out = "";

        try {

            JSONObject instructionJson = new JSONObject(json.get("cooking_instructions").toString());
            Iterator<?> instructionKeys = instructionJson.keys();

            while (instructionKeys.hasNext()){
                String key = (String) instructionKeys.next();

                JSONObject stepJson = new JSONObject(instructionJson.get(key).toString());

                //cooking_instructions.add(stepJson.get("to_be_read").toString());

                out += stepJson.get("to_be_read").toString() + "&";
                //String [] sentences = stepJson.get("to_be_read").toString().split("\\.");

                //cooking_steps.add(sentences);
            }

        }
        catch (JSONException e){
            Log.e("Lambda parsing error:", e.toString());
        }

        return out;
    }

    /**
     *  Method produces a String that contains sufficient information to produce
     *  SubjectData objects to represent ingredients. Each ingredient is separated by "&"
     * @param json
     * @return a string that will be used to locally store ingredient info.
     */
    private String parseIngredients(JSONObject json){

        String out = "";
        String quantityOut = "";
        try {
            JSONObject ingredientJson = new JSONObject(json.get("recipe_ingredients").toString());
            Iterator<?> ingredientKeys = ingredientJson.keys();

            while (ingredientKeys.hasNext()) {
                String key = (String) ingredientKeys.next();

                int extraIndex1 = key.indexOf("(");
                int extraIndex2 = key.indexOf(")");

                String displayName = key;
                if (extraIndex1 != -1 && extraIndex2 != -1 && extraIndex1 < extraIndex2) {
                    displayName = key.substring(0, extraIndex1).trim();
                }
                displayName = displayName.replace(" ", "_");

                JSONObject quantityInfo = new JSONObject(ingredientJson.get(key).toString());
                quantityInfo = new JSONObject(quantityInfo.get("quantity").toString());

                //key is the full ingredient info, that is includes info in parenthesis
                out += key + "::" + displayName + "::" + quantityInfo.get("count") + "::"
                        + quantityInfo.get("unit") + "&";

            }
        }
        catch (JSONException e){
            Log.e("Error parsing ingredients", e.toString());
            return "";
        }

        return out;
    }

    public void parseResults(String lambdaResponse){

        try {

            JSONObject json = new JSONObject(lambdaResponse);

            recipe_name = json.get("recipe_name").toString();
            editor.putString("name", recipe_name);

            try {
                recipe_time = json.get("recipe_time").toString();
                editor.putString("time", recipe_time);
            }
            catch (JSONException e){
                recipe_time = "-";
                Log.e("Non existant entry in json: ", "recipe_time");
            }

            String photos = json.get("photos_url").toString();
            photos = photos.substring(1, photos.length() - 1);
            List<String> photosList = new ArrayList<String>(Arrays.asList(photos.split(",")));

            recipe_image = photosList.get(0).substring(1, photosList.get(0).length() - 1);
            editor.putString("image", recipe_image);

            editor.putString("ingredients", parseIngredients(json));
            editor.putString("steps", parseSteps(json));

        }
        catch (JSONException e){
            Log.e("Lambda parsing error:", e.toString());
        }


        editor.commit();
    }

    public void showPopup(){
        ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                requestedRecipe, requestedRecipePhotoUrl);
    }

    public void callback(AIResponse aiResponse) {
        if (aiResponse != null) {

            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);
            assistantResponseStr = answerObj.txtResponse;

            ((GanzApplication) application).getBumblebeeVoice().speak(assistantResponseStr,
                    TextToSpeech.QUEUE_FLUSH, null);

            ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                    answerObj);

            requestedRecipe = answerObj.recipeIds.get(0);
            requestedRecipePhotoUrl = answerObj.photoUrls.get(0);


        } else {
            Log.d(LOG_TAG, "No response from assistant");
        }
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