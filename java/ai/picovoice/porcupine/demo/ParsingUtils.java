package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Image;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.gson.JsonElement;
import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Text;

import java.security.KeyException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ai.api.AIServiceContext;
import ai.api.android.AIDataService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;
import ai.api.model.ResponseMessage;
import ai.picovoice.porcupinemanager.KeywordCallback;
import ai.picovoice.porcupinemanager.PorcupineManager;
import ai.picovoice.porcupinemanager.PorcupineManagerException;
import pl.droidsonroids.gif.GifImageView;

/**
* This class contains many of the global methods in this application. The methods here 
* are usually used across multiple activities.
* TODO This file contains a wide range of methods. These should be split into multiple classes. 
*/
final class ParsingUtils {

    private String LOG_TAG = "-------- PARSGIN UTILS";

    private static String getRecipeLambda = "https://72whmgit15.execute-api.us-east-2.amazonaws.com/beta/recipe-by-id/";
    private static String getIngredientLambda = "https://72whmgit15.execute-api.us-east-2.amazonaws.com/beta/ingredient-by-name/";
    private static String addNewUser = "https://72whmgit15.execute-api.us-east-2.amazonaws.com/beta/user-by-id/";

    static String updateUserInfoUrl(String userInfo){return addNewUser + userInfo;}

    static String getGetIngredientUrl(String code){return getIngredientLambda + code; }

    static String getRecipeUrl(String code){
        return getRecipeLambda + code;
    }

    static String getHomepageRecipeUrl(){ return  getRecipeLambda + "-1/homepage";}

    static String getRecipeIntroRecipeUrl(String recipeId){ return  getRecipeLambda + recipeId + "/intro";}

    /**
     * Parses the data from lambda function into recipeArray. For now, the call returns
     * the entire database. TODO restrict the number of entries that are displayed.
     */
    static ArrayList<SubjectData> parseResults(String lambdaResponse){

        ArrayList<SubjectData> out = new ArrayList<SubjectData>();

        try {

            JSONObject json = new JSONObject(lambdaResponse);
            Iterator<?> keys = json.keys();

            while( keys.hasNext() ) {
                String key = (String) keys.next();
                Log.d("----------SEEE", key);

                JSONObject recipe_json = new JSONObject(json.get(key).toString());

                //TODO for now we get the first photo of the list. Want to display all if there
                // is more than one
                String photos = recipe_json.get("photos_url").toString();
                photos = photos.substring(1, photos.length() - 1);

                //TODO better way to convert string to List?
                //['potato, tomato', 'potato, tomato']
                List<String> photosList = new ArrayList<String>(Arrays.asList(photos.split(",")));

                String toDisplay = photosList.get(0).substring(1, photosList.get(0).length() - 1);

                out.add(new SubjectData(recipe_json.get("recipe_name").toString(), recipe_json.get("recipe_id").toString(), toDisplay));

            }
        }
        catch (JSONException e){
            Log.e("Lambda parsing error:", e.toString());
        }
        return out;
    }


    static class AssistantAnswer{
        /**
         * Class to carry info on the assistant response.
         * */

        public String txtResponse;
        public List<String> recipeIds;
        public List<String> recipeNames;
        public List<String> photoUrls;
        public Boolean goToRecipe;

        public AssistantAnswer(String txtResponse, List<String> recipeIds, List<String> recipeNames,
                               List<String> photoUrls, Boolean goToRecipe){

            this.txtResponse = txtResponse;
            this.recipeIds = recipeIds;
            this.recipeNames = recipeNames;
            this.photoUrls = photoUrls;
            this.goToRecipe = goToRecipe;
        }
    }

    /* Processes the response from assistant and constructs a AssistantAnswer
    object to be used in activities.
    * */
    static AssistantAnswer parseAssistantAnswer(AIResponse aiResponse){

        //TODO actually use this map in code below
        HashMap<String, String> intentMap = new HashMap<String, String>();
        intentMap.put("recipe.search", "1de472c4-ceba-47cd-b9c7-4d9857ce2af8");
        intentMap.put("app.navigation", "d9eb7b2d-8638-4a90-ae0c-fe4362573715");
        intentMap.put("recipe.navigation", "3c306374-3e9d-4c3e-95c7-eb8946432e51");
        intentMap.put("binary.intent", "ffc262a7-a93f-47d6-82b0-59403294fc40");
        intentMap.put("recipe.search.see.result", "222d0ea7-8ffc-4432-a8cb-97a55102a99d");

        // contains a template answer such as "What about <> ?"
        String textAnswer = aiResponse.getResult().getFulfillment().getSpeech();
        String intentId = aiResponse.getResult().getMetadata().getIntentId();

        List<String> recipeNames = new ArrayList<>();
        List<String> recipeIds = new ArrayList<>();
        List<String> photoUrls = new ArrayList<>();
        Boolean goToRecipe = false;
        int stepNavigation = -1;

        switch(intentId){

            //recipe.search intent
            case "1de472c4-ceba-47cd-b9c7-4d9857ce2af8":

                int messagesCount = aiResponse.getResult().getFulfillment().getMessages().size();
                if (messagesCount > 1) {
                    for (int i = 0; i < messagesCount; i++) {
                        ResponseMessage.ResponseQuickReply responseMessage = (ResponseMessage.ResponseQuickReply) aiResponse.getResult().getFulfillment().getMessages().get(i);
                        recipeNames.add(responseMessage.getTitle());

                        List<String> recipeInfo = responseMessage.getReplies();
                        recipeIds.add(recipeInfo.get(0));
                        photoUrls.add(recipeInfo.get(1));
                    }
                }

                break;

            //recipe.search.see.result intent
            case "222d0ea7-8ffc-4432-a8cb-97a55102a99d":
                goToRecipe = true;
                break;

            default:

        }

        return new AssistantAnswer(textAnswer, recipeIds, recipeNames, photoUrls, goToRecipe);
    }


    static void showPopup(Context ctx, Activity activity, String userResponseStr,
                          String assistant_responseStr, AssistantAnswer answerObj){
        /**
         * Handle the popup that opens when user talks to assistant.
         * */
        String allResultsNames = "";
        String allResultsIds = "";
        String allResultsPhotoUrls = "";

        for (int i = 0; i < answerObj.recipeNames.size(); i++){

            if (i < answerObj.recipeNames.size() - 1) {
                allResultsNames += answerObj.recipeNames.get(i) + ",!";
                allResultsIds += answerObj.recipeIds.get(i) + ",!";
                allResultsPhotoUrls += answerObj.photoUrls.get(i) + ",!";
            }
            else{
                allResultsNames += answerObj.recipeNames.get(i);
                allResultsIds += answerObj.recipeIds.get(i);
                allResultsPhotoUrls += answerObj.photoUrls.get(i);
            }
        }


        Intent intent = new Intent(ctx, AssistantPopup.class);
        Bundle id = new Bundle();

        id.putString("userInput", userResponseStr);
        id.putString("assistant_response", assistant_responseStr);

        id.putString("recipeId", answerObj.recipeIds.size() > 0 ? answerObj.recipeIds.get(0) : ""); // if need to show button for recipe
        id.putString("recipePhotoUrl", answerObj.photoUrls.size() > 0 ? answerObj.photoUrls.get(0) : "");

        id.putString("allRecipeIds", allResultsIds);
        id.putString("allRecipePhotoUrls", allResultsPhotoUrls);
        id.putString("allRecipeNames", allResultsNames);

        intent.putExtras(id);

        ActivityCompat.startActivityForResult(activity, intent, 100,
                ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());

    }

    /**
     * Handle the popup that opens when user talks to assistant.
    * */
    static void showPopup(Context ctx, Activity activity,
                           String userResponseStr,
                           String assistant_responseStr,
                           String recipeId,
                          String recipePhotoUrl){

        Intent intent = new Intent(ctx, AssistantPopup.class);
        Bundle id = new Bundle();

        id.putString("userInput", userResponseStr);
        id.putString("assistant_response", assistant_responseStr);

        id.putString("recipeId", recipeId); // if need to show button for recipe
        id.putString("recipePhotoUrl", recipePhotoUrl);

        intent.putExtras(id);

        ActivityCompat.startActivityForResult(activity, intent, 100,
                ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());

    }

    /**
     * Handles popup that opens to see related recipes in RecipeIntro
     */
    static void similarRecipesPopup(Context ctx, Activity activity){

        Intent intent = new Intent(ctx, SimilarRecipesPopup.class);
        //ActivityCompat.startActivityForResult(activity, intent, 100,
          //      ActivityOptions.makeSceneTransitionAnimation(activity).toBundle());
        ctx.startActivity(intent);
    }

    /**
     * Initialize the porcupineManager library.
     * @return Porcupine instance.
     */
    static PorcupineManager initPorcupine(BumblebeeSpecs bumblebeeSpecs,
                                                  Context ctx,
                                                  ImageView triggerAssistant,
                                          TextToSpeech bumblebeeVoice) throws PorcupineManagerException {

        final Activity curActivity = (Activity) ctx;
        final ImageView triggerButton = triggerAssistant;
        final TextToSpeech voice = bumblebeeVoice;

        return new PorcupineManager(bumblebeeSpecs.getModelFilePath(),
                bumblebeeSpecs.getKeywordFilePath(), bumblebeeSpecs.getSensitivity(), new KeywordCallback() {
            @Override
            public void run(int keyword_index) {

                curActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        try {
                            if (voice.isSpeaking()) {
                                voice.stop();
                            }
                        }
                        catch (NullPointerException e){
                            Log.e("Error stopping voice:", "Not yet initialized");
                        }
                        finally {
                            triggerButton.performClick();
                        }

                    }
                });
            }
        });
    }


    /**
     * Stops listening for hot word and starts recognizing input
    * */
    static public void startRecognizingSpeech(PorcupineManager porcupineManager,
                                          SpeechRecognizer mSpeechRecognizer,
                                          Intent mSpeechRecognizerIntent,
                                          Context ctx){
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();

            } catch (PorcupineManagerException e) {
                Utils.showErrorToast(ctx);
            }
        }

        mSpeechRecognizer.stopListening();
        mSpeechRecognizer.startListening(mSpeechRecognizerIntent);
    }

    /**
     * Navigates to targetActivity page from ctx. It also stops listening for hotword if needed.
     *
     * @param ctx
     * @param targetActivity
     */
    static public void navigateToActivity(Application application, Context ctx, Class targetActivity,
                                        Bundle id){

        ((GanzApplication) application).getmSpeechRecognizer().stopListening();
        ((GanzApplication) application).stopListeningHotword();


        Intent intent = new Intent(ctx, targetActivity);

        intent.putExtras(id);
        ctx.startActivity(intent);
    }

    /**
     * Navigates to targetActivity page from ctx. It also stops listening for hotword if needed.
     *
     * @param ctx
     * @param recipe_id
     * @param targetActivity
     */
    static public void navigateToTarget(Application application, Context ctx, Class targetActivity,
                                        String recipe_id){

        ((GanzApplication) application).getmSpeechRecognizer().stopListening();
        ((GanzApplication) application).stopListeningHotword();


        Intent intent = new Intent(ctx, targetActivity);
        if (!recipe_id.equals("")) {
            Bundle id = new Bundle();
            id.putString("recipe_id", recipe_id); //Your id
            intent.putExtras(id);
        }
        ctx.startActivity(intent);
    }

    /**
     * Method to request the dialogFlow agent for a response given msg.
     * The RequestTask object calls a callback function from (Activity) ctx that will update
     * UI and call follow up functions (i.e. to convert TTS)
     * @param msg : message that assistant responds to.
     */
    static public void sendMessage(String msg, AIRequest aiRequest, Context ctx,
                                    AIDataService aiDataService,
                                    AIServiceContext customAIServiceContext, String activityName) {

        aiRequest.setQuery(msg);
        RequestTask requestTask = new RequestTask((Activity) ctx, aiDataService, customAIServiceContext, activityName);
        requestTask.execute(aiRequest);

    }

    static public PorcupineManager startListeningHotword(Context ctx,
                                                         BumblebeeSpecs bumblebeeSpecs,
                                                         ImageView triggerAssistant,
                                                         TextToSpeech bumblebeeVoice){

        PorcupineManager porcupineManager = null;
        try {
            if (Utils.hasRecordPermission(ctx)) {

                porcupineManager = ParsingUtils.initPorcupine(bumblebeeSpecs, ctx, triggerAssistant, bumblebeeVoice);
                porcupineManager.start();

            }
            //Ask user audio permission if needed
            else {
                Utils.showRecordPermission((Activity) ctx);
            }
        } catch (PorcupineManagerException e) {
            Utils.showErrorToast(ctx);
        }

        return porcupineManager;
    }

    static public void toggleSpeakingGif(ImageView gooseLogo, GifImageView listeningGif){

        if (gooseLogo.getVisibility() == View.VISIBLE){
            gooseLogo.setVisibility(View.GONE);
            listeningGif.setVisibility(View.VISIBLE);
        }
        else{
            gooseLogo.setVisibility(View.VISIBLE);
            listeningGif.setVisibility(View.GONE);
        }
    }

    static public void setBottomNavigation(BottomNavigationView bottomNavigationView, final Context ctx,
                                           final ImageView triggerAssistant,
                                           final Application application,
                                           final String activityName, final SlidingUpPanelLayout mLayout){

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.home:

                        if (!activityName.equals("MainActivity")) {
                            navigateToTarget(application, ctx, MainActivity.class,
                                    "");
                        }
                        else{

                            mLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
                            Toast.makeText(ctx, "Already in Home", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case R.id.assistant_popup:
                        mLayout.setPanelState(SlidingUpPanelLayout.PanelState.ANCHORED);
                        break;
                    case R.id.bumblebee:
                        triggerAssistant.performClick();
                        break;
                }
                return true;
            }
        });

    }

}
