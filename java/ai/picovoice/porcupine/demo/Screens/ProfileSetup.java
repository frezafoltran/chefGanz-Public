package ai.picovoice.porcupine.demo;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;

import ai.picovoice.porcupinemanager.PorcupineManager;
import ai.picovoice.porcupinemanager.PorcupineManagerException;

/**
* This class corresponds to the ProfileSetup activity. It allows users to set up their profile 
* by having a conversation with bumblebee. 
*/
public class ProfileSetup extends AppCompatActivity {

    //TTS
    private String introMsg = "Hi there, I'd like to ask you some questions " +
            "to get to know your food taste better. Are you ready?";

    private String endMsg = "Done. Thank you ";

    private ArrayList<TextView> allOptionViews;

    private ArrayList<Boolean> allOptionClicked;

    // speech-text variables
    private Boolean checkIfDone = true;

    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    //Question flow objects
    private ProfileQuestions questionFlow = new ProfileQuestions();
    private ProfileQuestions.Question curQuestion = null;
    private int curQuestionIndex = 0;
    private Boolean questionaireStarted = false;

    private String answerToStore = "";
    private String answerToStoreClicker = "";

    //UI
    private TextView curQuestionView;
    private EditText curAnswerView;
    private LinearLayout answerOptionsView;
    private Button yesStart;

    Button nextQuestion;
    private FloatingActionButton triggerAssistant;
    private String userResponseStr;

    private Application application;
    private String activityName = "ProfileSetup";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_setup);

        Utils.configurePorcupine(this);
        application = this.getApplication();

        curQuestionView = findViewById(R.id.cur_question);
        curAnswerView = findViewById(R.id.cur_answer);
        triggerAssistant = findViewById(R.id.trigger_ganz);
        nextQuestion = findViewById(R.id.next_question);
        answerOptionsView = (LinearLayout)findViewById(R.id.answer_options);
        yesStart = findViewById(R.id.yes);

        ((GanzApplication) application).getBumblebeeVoice().speak(introMsg,
                TextToSpeech.QUEUE_FLUSH, null);

        // Start recognizing hot-word.
        ((GanzApplication) application).initListeningHotword(this,
                ((GanzApplication) application).getBumblebeeSpecs(), triggerAssistant,
                ((GanzApplication) application).getBumblebeeVoice());

        //initialize speech recognizer. Will only start listening when assistant is triggered
        ((GanzApplication) application).initSpeechRecognizer(this, application, triggerAssistant,
                ((GanzApplication) application).getBumblebeeVoice(), activityName);

        settings = getSharedPreferences("UserProfile", 0);
        editor = settings.edit();

        nextQuestion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                questionaireStarted = true;
                checkIfDone = true;

                if (curQuestionIndex < questionFlow.questions.size()) {
                    nextQuestion.setVisibility(View.GONE); // get rid of button while assistant speaks
                    isTTSSpeaking();

                }
                askQuestion();
                curQuestionIndex ++;
            }
        });

        findViewById(R.id.start_profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isTTSSpeaking();
                ((GanzApplication) application).getBumblebeeVoice().speak(introMsg,
                        TextToSpeech.QUEUE_FLUSH, null);
                Button startButton = findViewById(R.id.start_profile);
                startButton.setVisibility(View.GONE);
                curQuestionView.setText(introMsg);

            }
        });

        yesStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nextQuestion.performClick();
                yesStart.setVisibility(View.GONE);
            }
        });

        findViewById(R.id.go_home).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((GanzApplication) application).getBumblebeeVoice().stop();
                ((GanzApplication) application).stopListeningHotword();
                ((GanzApplication) application).getmSpeechRecognizer().stopListening();
                Intent intent = new Intent(ProfileSetup.this, MainActivity.class);
                Bundle id = new Bundle();
                id.putString("skip_profile","TRUE");
                intent.putExtras(id);
                startActivity(intent);
            }
        });

    }

    public void askQuestion(){

        //saves previous answer
        if (curQuestion != null && curQuestionIndex > 0 && curQuestionIndex <= questionFlow.questions.size()){
            commitChanges(questionFlow.questions.get(curQuestionIndex - 1).answerAttribute);
        }

        answerToStore = "";
        answerToStoreClicker = "";

        // get info for next question
        if (curQuestionIndex < questionFlow.questions.size()) {
            curQuestion = questionFlow.questions.get(curQuestionIndex);

            curQuestionView.setText(curQuestion.questionText);
            ((GanzApplication) application).getBumblebeeVoice().speak(
                    curQuestion.questionText, TextToSpeech.QUEUE_FLUSH, null);

            setAnswerOptions(curQuestion.answerOptions);
        }
        else if (curQuestionIndex == questionFlow.questions.size()){
            curQuestionView.setText(endMsg + settings.getString("username", ""));
            ((GanzApplication) application).getBumblebeeVoice().speak(
                    endMsg + settings.getString("username", ""), TextToSpeech.QUEUE_FLUSH, null);
        }
        else{
            ((GanzApplication) application).getBumblebeeVoice().stop();
            ((GanzApplication) application).stopListeningHotword();
            ((GanzApplication) application).getmSpeechRecognizer().stopListening();
            Intent intent = new Intent(ProfileSetup.this, EditProfile.class);
            startActivity(intent);
        }
    }

    public void optionClicker(int curIndex, TextView curOptionView){

        //if option is already clicked, uncoloer and delete from answer
        if (allOptionClicked.get(curIndex)){
            curOptionView.setBackgroundColor(Color.parseColor("#ffffff"));
            answerToStoreClicker = answerToStoreClicker.replace(curOptionView.getText(), "");
        }
        else {
            allOptionClicked.set(curIndex, true);
            curOptionView.setBackgroundColor(Color.parseColor("#00ff00"));
            answerToStoreClicker += " " + curOptionView.getText();
        }

    }

    public void setAnswerOptions(final String [] answerOptions){

        allOptionClicked = new ArrayList<>(); //stores whether option is currently clicked or not
        allOptionViews = new ArrayList<>();

        if (answerOptions == null){
            answerOptionsView.setVisibility(View.GONE);
            curAnswerView.setVisibility(View.VISIBLE);

        }
        else{
            curAnswerView.setVisibility(View.GONE);
            answerOptionsView.removeAllViews();

            for (int i = 0; i < answerOptions.length; i ++){

                final int curIndex = i;
                final TextView curOption = new TextView(this);

                curOption.setText(answerOptions[i]);
                curOption.setId(i);
                curOption.setGravity(Gravity.CENTER);

                curOption.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                ));
                LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) curOption.getLayoutParams();
                params.height = getResources().getDimensionPixelSize(R.dimen.questionaire_item_height);
                params.width = ViewGroup.LayoutParams.MATCH_PARENT;
                params.gravity = Gravity.CENTER;

                curOption.setLayoutParams(params);

                curOption.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        optionClicker(curIndex, curOption);
                    }
                });

                allOptionClicked.add(false);

                allOptionViews.add(curOption);
                answerOptionsView.addView(curOption);

            }

            answerOptionsView.setVisibility(View.VISIBLE);
        }
    }

    public void commitChanges(String fieldToUpdate){

        String outAnswer = "";
        String combinedAnswers = answerToStoreClicker + " " + answerToStore;

        Log.d("-----------COMMITTING", combinedAnswers);
        if (questionFlow.questions.get(curQuestionIndex - 1).typeOfAnswer.equals("list")) {
            String partialAnswer = combinedAnswers.trim().toLowerCase();
            String[] possibleSelections = questionFlow.questions.get(curQuestionIndex - 1).answerOptions;

            for (int selIndex = 0; selIndex < possibleSelections.length; selIndex++) {
                if (partialAnswer.contains(possibleSelections[selIndex].toLowerCase())){
                    outAnswer += possibleSelections[selIndex] + ",";
                }
            }
        }
        else{
            outAnswer = combinedAnswers.trim();

            if (outAnswer.equals("")){
                outAnswer = curAnswerView.getText().toString();
            }
            //replace non-digits if asking for age
            if(questionFlow.questions.get(curQuestionIndex - 1).answerAttribute.equals("user_age")){
                outAnswer = outAnswer.replaceAll("\\D+","");
            }
        }

        Log.d("--------------- SAVED", outAnswer);

        editor.putString(fieldToUpdate, outAnswer);
        editor.commit();

        curAnswerView.setText("");
    }


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

    @Override
    public void onBackPressed() {

    }

    /**
     * Updates the UI after response from assistant.
     *
     * @param match the assistant response
     */
    public void updateUIOnResponse(String match) {
        //ParsingUtils.toggleSpeakingGif(gooseLogo, listeningGif);
        userResponseStr = match;
        if (!questionaireStarted){
            yesStart.setVisibility(View.VISIBLE);
        }
        else{
            nextQuestion.setVisibility(View.VISIBLE);
        }
    }

    public void callback(String match){

        //indicates answer to 'get started' question
        if (match.toLowerCase().equals("yes") && !questionaireStarted){
            yesStart.setVisibility(View.GONE);
            nextQuestion.performClick();
        }
        else if(questionaireStarted) {

            //check if answer matches one of the displayed options
            for (int i = 0; i < allOptionViews.size(); i ++){
                TextView curOptionView = allOptionViews.get(i);
                String curDisplayedOption = curOptionView.getText().toString().toLowerCase();
                if (match.toLowerCase().contains(curDisplayedOption)){
                    curOptionView.setBackgroundColor(Color.parseColor("#00ff00"));
                }
            }

            curAnswerView.setText(match);
            answerToStore += " " + match;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if (item.getItemId() == R.id.profile) {
            //((GanzApplication) application).getmSpeechRecognizer().stopListening();
            //((GanzApplication) application).stopListeningHotword();

            Intent intent = new Intent(this, EditProfile.class);

            Bundle id = new Bundle();
            id.putString("calling_activity", activityName);
            intent.putExtras(id);
            startActivity(intent);

        }

        return true;
    }

}
