package ai.picovoice.porcupine.demo;

import android.app.ActionBar;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;

import java.util.ArrayList;

/**
* This class implements the standard RecognitionListener to achieve a particular behavior after listening the user's input.
* The basic idea is that the user input will be either handled locally (statically) or through a message sent to dialogFlow. 
* The details for this implementation are in the onResults method below.
*/
public class BumblebeeListener implements RecognitionListener {

    private Context curContext;
    private Application application;
    private ImageView triggerAssistant;
    private TextToSpeech bumblebeeVoice;
    private String activityName;

    public BumblebeeListener(Context ctx, Application application, ImageView triggerAssistant,
                             TextToSpeech bumblebeeVoice, String activityName)
    {
        this.curContext = ctx;
        this.application = application;
        this.triggerAssistant = triggerAssistant;
        this.bumblebeeVoice = bumblebeeVoice;
        this.activityName = activityName;
    }

    @Override
    public void onReadyForSpeech(Bundle bundle) {}

    @Override
    public void onBeginningOfSpeech() {}

    @Override
    public void onRmsChanged(float v) {}

    @Override
    public void onBufferReceived(byte[] bytes) {}

    @Override
    public void onEndOfSpeech() {}

    @Override
    public void onError(int i) {
        
        //this error seems to be the no speech error (i == 7)
        if (true){
            ((GanzApplication) application).initListeningHotword(curContext,
                    ((GanzApplication) application).getBumblebeeSpecs(), triggerAssistant, bumblebeeVoice);

            switch (activityName){
                case "MainActivity":
                    ((MainActivity) curContext).updateUIOnResponse("");
                    break;

                case "RecipeIntro":
                    ((RecipeIntro) curContext).updateUIOnResponse("");
                    break;

                case "RecipeIngredient":
                    ((RecipeIngredient) curContext).updateUIOnResponse("");
                    break;

                case "RecipeInstruction":
                    ((RecipeInstruction) curContext).updateUIOnResponse("");
                    break;

                case "ProfileSetup":
                    ((ProfileSetup) curContext).updateUIOnResponse("");
                    break;
            }
        }

    }

    @Override
    public void onResults(Bundle bundle) {

        //getting all the matches
        ArrayList<String> matches = bundle
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

        //displaying the first match
        if (matches != null) {

            //does not require sending message to dialogflow
            // TODO standardize the process of getting a static response.
            if (activityName.equals("ProfileSetup")){
                ((ProfileSetup) curContext).callback(matches.get(0));
            }
            else if (activityName.equals("RecipeInstruction")){
                ((RecipeInstruction) curContext).callbackLocal(matches.get(0));
            }
            else {
                ParsingUtils.sendMessage(matches.get(0), ((GanzApplication) application).getAiRequest(),
                        curContext, ((GanzApplication) application).getAiDataService(),
                        ((GanzApplication) application).getCustomAIServiceContext(), activityName);
            }
        }


        //resume listening for hotword
        ((GanzApplication) application).initListeningHotword(curContext,
                ((GanzApplication) application).getBumblebeeSpecs(), triggerAssistant, bumblebeeVoice);

        //TODO if updateUIOnResponse has the same effect in all activities, we can make this a global method.
        switch (activityName){
            case "MainActivity":
                ((MainActivity) curContext).updateUIOnResponse(matches.get(0));
                break;

            case "RecipeIntro":
                ((RecipeIntro) curContext).updateUIOnResponse(matches.get(0));
                break;

            case "RecipeIngredient":
                ((RecipeIngredient) curContext).updateUIOnResponse(matches.get(0));
                break;

            case "RecipeInstruction":
                ((RecipeInstruction) curContext).updateUIOnResponse(matches.get(0));
                break;

            case "ProfileSetup":
                ((ProfileSetup) curContext).updateUIOnResponse("");
                break;
        }
    }

    @Override
    public void onPartialResults(Bundle bundle) {}

    @Override
    public void onEvent(int i, Bundle bundle) {}

}
