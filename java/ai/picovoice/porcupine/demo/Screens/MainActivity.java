package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;

import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;

import android.util.DisplayMetrics;
import android.util.Log;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import junit.framework.Test;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import ai.api.model.AIResponse;
import ai.picovoice.porcupine.demo.Util.AssistantUtil;
import ai.picovoice.porcupine.demo.Util.HotwordUtil;
import pl.droidsonroids.gif.GifImageView;

import static ai.picovoice.porcupine.demo.ParsingUtils.setBottomNavigation;

/**
* This class corresponds to the MainActivity activity. This is the homepage of chefGanz. 
It consists of a welcome message to the user, along with links to relevant pages. The larger part of the screen
* is a list with recipes that can be scrolled through.
*/
public class MainActivity extends MainBaseActivity implements Serializable {

    private String LOG_TAG = "---MAIN ACTIVITY TAG";

    //list of recipes for main page
    ArrayList<SubjectData> recipeArray = new ArrayList<SubjectData>();
    String lambdaResult;

    //for now these store the last user query and assistant response
    //TODO display recent history of conversation to user
    private String userResponseStr;
    private String assistantResponseStr;

    private LinearLayout popupWrapper;
    private BottomNavigationView bottomNavigationView;

    private ImageView triggerAssistant;
    private Button triggerBumblebee;
    private SlidingUpPanelLayout mLayout;

    //holds id of requested recipe
    private String requestedRecipe = "";
    private String requestedRecipePhotoUrl = "";
    private String requestedRecipeId = "";

    private String activityName = "MainActivity";
    private ImageView gooseLogo;
    private GifImageView listeningGif;

    private DisplayMetrics displayMetrics = new DisplayMetrics();
    private int width;

    //user info
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    //assistant popup
    private EditText userInput;
    private TextView assistantInput;
    private ImageView recipeImagePopup;
    private Button goToRecipePopup;
    private Button moreRecipesPopup;

    private Application application;
    private Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTheme(R.style.AppTheme);
        setContentView(R.layout.activity_main);

        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        width = displayMetrics.widthPixels;

        application = this.getApplication();
        ctx = this;

        initViews();
        AssistantUtil.initSlidingPopup(mLayout, popupWrapper);

        AssistantUtil.initTriggerButton(triggerAssistant, listeningGif, application);


        Utils.configurePorcupine(this);
        HotwordUtil.initHotword(application, this,triggerAssistant, activityName,
                userInput, assistantInput, recipeImagePopup, goToRecipePopup, moreRecipesPopup);


        initProfileSetup();

        setBottomNavigation(bottomNavigationView, this, triggerAssistant, application, "MainActivity", mLayout);

    }

    @Override
    protected void onStart() {
        super.onStart();

        // get recipes from lambda function
        HttpGetRequest getRequest = new HttpGetRequest();
        getRequest.setActivity(this, "MainActivity");

        try {
            lambdaResult = getRequest.execute(ParsingUtils.getHomepageRecipeUrl()).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Error in GET", e.toString());
        }


        TextView talkToAssistantPrompt = findViewById(R.id.talk_to_assistant);
        String personalPrompt = "";
        if (!settings.getString("username", "").isEmpty()) {
            personalPrompt = settings.getString("username", "") + ", say bumblebee to start.";
        } else {
            personalPrompt = "say bumblebee to start.";
        }
        talkToAssistantPrompt.setText(personalPrompt);

    }

    public void parseResults(String lambdaResult){
        recipeArray = ParsingUtils.parseResults(lambdaResult);
    }

    public void updateRecipesLayout(){
        // set results of get request to list UI component
        final ListView list = findViewById(R.id.recipe_list_main);
        CustomAdapter customAdapter = new CustomAdapter(MainActivity.this, this,
                application, recipeArray);
        list.setAdapter(customAdapter);
    }

    public void initProfileSetup(){
        //open profile page only if no username is found
        settings = getSharedPreferences("UserProfile", 0);
        editor = settings.edit();
        Bundle b = getIntent().getExtras();
        String skip_profile = "TRUE";
        if (b != null){
            skip_profile = b.getString("skip_profile");
        }

        if (skip_profile.isEmpty() || skip_profile.equals("FALSE")) {

            ((GanzApplication) application).getmSpeechRecognizer().stopListening();
            ((GanzApplication) application).stopListeningHotword();

            Intent intent = new Intent(this, ProfileSetup.class);
            startActivity(intent);
        }
    }

    public void initViews(){
        /**
         * Initializes the views for activity
         */

        //TODO is triggerAssistant needed at all?
        triggerAssistant = findViewById(R.id.trigger_ganz);

        triggerBumblebee = findViewById(R.id.triggerBumblebee);
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

        triggerBumblebee.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ParsingUtils.sendMessage(userInput.getText().toString(),
                        ((GanzApplication) application).getAiRequest(),
                        ctx, ((GanzApplication) application).getAiDataService(),
                        ((GanzApplication) application).getCustomAIServiceContext(), activityName);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    public void callback(AIResponse aiResponse) {
        if (aiResponse != null) {

            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);
            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.EXPANDED);
            if (answerObj.goToRecipe) {
                Intent intent = new Intent(this, RecipeIntro.class);
                Bundle id = new Bundle();
                id.putString("recipe_id", requestedRecipe);
                intent.putExtras(id);

                startActivity(intent);
            }
            else {
                ((GanzApplication) application).getBumblebeeVoice().speak(answerObj.txtResponse, TextToSpeech.QUEUE_FLUSH, null);
                ((GanzApplication) application).updateAssistantDisplay(this, answerObj);
            }


        } else {
            Log.d(LOG_TAG, "No response from assistant");
        }
    }
    /*
     * Process the response from assistant
     * */
    public void callbackTemp(AIResponse aiResponse) {
        if (aiResponse != null) {

            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);

            if (answerObj.goToRecipe) {
                Intent intent = new Intent(this, RecipeIntro.class);
                Bundle id = new Bundle();
                id.putString("recipe_id", requestedRecipe);
                intent.putExtras(id);

                startActivity(intent);
            }
            else {

                assistantResponseStr = answerObj.txtResponse;
                requestedRecipe = answerObj.recipeIds.size() > 0 ? answerObj.recipeIds.get(0) : "";
                requestedRecipePhotoUrl = answerObj.photoUrls.size() > 0 ? answerObj.photoUrls.get(0) : "";
                ((GanzApplication) application).getBumblebeeVoice().speak(assistantResponseStr, TextToSpeech.QUEUE_FLUSH, null);
                ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr, answerObj);
            }


        } else {
            Log.d(LOG_TAG, "No response from assistant");
        }
    }

    public void showPopup(){
        ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                "!" + requestedRecipe, requestedRecipePhotoUrl);
    }

    /**
     * Check the result of the record permission request.
     *
     * @param requestCode  request code of the permission request.
     * @param permissions  requested permissions.
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
                Log.d(LOG_TAG, "onActivityResult: sortBy " + data.getStringExtra("sortBy"));
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
        AssistantUtil.toggleSpeakingGif(listeningGif);
        if (match != null && match != "") {
            userResponseStr = match;
            userInput.setText(userResponseStr);
        }
    }
}
