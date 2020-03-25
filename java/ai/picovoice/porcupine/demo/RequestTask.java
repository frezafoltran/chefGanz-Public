package ai.picovoice.porcupine.demo;


import android.app.Activity;
import android.os.AsyncTask;

import ai.api.AIServiceContext;
import ai.api.AIServiceException;
import ai.api.android.AIDataService;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

/**
* This class takes care of language processing done through dialogflow. It contains methods to perform async requests
* to dialogflow to process user input without disrupting the user experience.
*/
public class RequestTask  extends AsyncTask<AIRequest, Void, AIResponse> {

    Activity activity;
    private AIDataService aiDataService;
    private AIServiceContext customAIServiceContext;
    private String activityName;

    RequestTask(Activity activity, AIDataService aiDataService, AIServiceContext customAIServiceContext, String activityName){
        this.activity = activity;
        this.aiDataService = aiDataService;
        this.customAIServiceContext = customAIServiceContext;
        this.activityName = activityName;
    }

    @Override
    protected AIResponse doInBackground(AIRequest... aiRequests) {
        final AIRequest request = aiRequests[0];
        try {
            return aiDataService.request(request, customAIServiceContext);
        } catch (AIServiceException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(AIResponse aiResponse) {
        if (activityName.equals("MainActivity")) {
            ((MainActivity) activity).callback(aiResponse);
        }
        else if (activityName.equals("RecipeIntro")){
            ((RecipeIntro) activity).callback(aiResponse);
        }
        else if (activityName.equals("RecipeIngredient")){
            ((RecipeIngredient) activity).callback(aiResponse);
        }
        else if (activityName.equals("RecipeInstruction")){
            ((RecipeInstruction) activity).callback(aiResponse);
        }
        else if (activityName.equals("IngredientInfo")){
            ((IngredientInfo) activity).callback(aiResponse);
        }
        else if (activityName.equals("AssistantPopup")){
            ((AssistantPopup) activity).callback(aiResponse);
        }
    }
}
