package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Rect;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static ai.picovoice.porcupine.demo.ParsingUtils.setBottomNavigation;

/**
* This class corresponds to the EditProfile activity. It is an activity where users can go back to see their profile
* and make any necessary changes.
*/
public class EditProfile extends AppCompatActivity {

    private  String LOG_TAG = "--------EDIT PROFILE";
    private ProfileQuestions questionFlow = new ProfileQuestions();
    private LinearLayout allFieldsView;
    private SharedPreferences settings;
    private SharedPreferences.Editor editor;

    private BottomNavigationView bottomNavigationView;
    private ImageView triggerAssistant;
    private SlidingUpPanelLayout mLayout;

    private ArrayList<EditFieldInfo> allVals = new ArrayList<>();

    private Application application;
    private Activity ctx;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        application = this.getApplication();
        settings = getSharedPreferences("UserProfile", 0);
        editor = settings.edit();
        allFieldsView = findViewById(R.id.main_container);
        bottomNavigationView = (BottomNavigationView) findViewById(R.id.bottom_navigation);

        triggerAssistant = findViewById(R.id.trigger_ganz);

        ctx = this;

        //allFieldsView.removeAllViews();
        //allFieldsLabelView.removeAllViews();

        // for each question, display the current user response along with a question label
        for (int i = 0; i < questionFlow.questions.size(); i ++){

            //------ add labels
            allFieldsView.addView(createLabelField(i));

            // ------ add values from user
            if (questionFlow.questions.get(i).typeOfAnswer.equals("string")) {
                EditText newField = createEditField(i);
                allFieldsView.addView(newField);
                allVals.add(new EditFieldInfo(newField, null,
                        questionFlow.questions.get(i).answerAttribute));
            }
            else if (questionFlow.questions.get(i).typeOfAnswer.equals("list")){
                MultiSelectionSpinner newSpinnerField = createSpinnerField(i);
                allFieldsView.addView(newSpinnerField);
                allVals.add(new EditFieldInfo(null, newSpinnerField,
                        questionFlow.questions.get(i).answerAttribute));

            }

        }

        findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                saveChanges();
            }
        });
        setBottomNavigation(bottomNavigationView, this, triggerAssistant, application, "EditProfile", mLayout);


    }

    @Override
    public void onBackPressed () {
        super.onBackPressed(); 
    }

    /**
    * This methods makes sure that users can select multiple answers for answers where it makes sense to do so.
    *
    * @param i the index of the question to be added in the global questions list.
    * @return a spinner containing the current user answers.
    */
    private MultiSelectionSpinner createSpinnerField(int i){

        ProfileQuestions.Question curQ = questionFlow.questions.get(i);

        final MultiSelectionSpinner spinner = new MultiSelectionSpinner(this);

        ArrayList<Item> items = new ArrayList<>();
        for (int j = 0; j < curQ.answerOptions.length; j ++) {
            items.add(Item.builder().name(curQ.answerOptions[j]).value(curQ.answerOptions[j]).build());
        }

        spinner.setItems(items);

        // TODO select based on what's saved in memory
        String [] savedAnswers = settings.getString(
                questionFlow.questions.get(i).answerAttribute, "").split(",");
        ArrayList<Item> selected = new ArrayList<>();
        for (int k = 0; k < savedAnswers.length; k++) {
            selected.add(Item.builder().name(savedAnswers[k]).value(savedAnswers[k]).build());
        }

        spinner.setSelection(selected);

        spinner.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) spinner.getLayoutParams();
        //params.height = getResources().getDimensionPixelSize(R.dimen.edit_item_height);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.LEFT;
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margin_profile_edit);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);

        spinner.setLayoutParams(params);

        // To get the selected Item list
        //ArrayList<Item> selectedItems = spinner.getSelectedItems();


        return spinner;
    }

    /**
    * This method is called when the user hits "Save" to make sure the changes they made are commited. It gets the currently
    * input values in the spinners/text fields and updates the user profile.
    */
    public void saveChanges(){

        String lambdaStr = "1.";
        for (int i = 0; i < allVals.size(); i ++){

            String newVal = "";
            // in case answer is from EditText
            if (allVals.get(i).spinnerView == null){
                newVal = allVals.get(i).textView.getText().toString();
            }
            else{
                ArrayList<Item> selected = allVals.get(i).spinnerView.getSelectedItems();
                for (int j = 0; j < selected.size(); j ++){

                    if (j < selected.size() - 1) {
                        newVal += selected.get(j).getValue() + ",";
                    }
                    else{
                        newVal += selected.get(j).getValue();
                    }
                }
            }
            editor.putString(allVals.get(i).answerAttribute, newVal);

            if (newVal == ""){
                newVal = "_";
            }

            if (i < allVals.size() - 1) {
                lambdaStr += newVal + ".";
            }
            else{
                lambdaStr += newVal;
            }
        }

        editor.commit();

        HttpGetRequest getRequest = new HttpGetRequest();
        getRequest.setActivity(ctx, "EditProfile");

        String lambdaResult = "";
        try {
            lambdaResult = getRequest.execute(ParsingUtils.updateUserInfoUrl(lambdaStr)).get();
        } catch (ExecutionException | InterruptedException e) {
            Log.e("Error in GET", e.toString());
        }

        Log.d(LOG_TAG, lambdaStr);
        Log.d(LOG_TAG, lambdaResult);

        Toast.makeText(this, "Changes saved", Toast.LENGTH_SHORT).show();

    }

    /**
    * @param i the index of the question to be added in the global questions list.
    * @return a text field where users can see/edit their selection
    */
    private EditText createEditField(int i){
        final EditText curOption = new EditText(this);

        String curValue = settings.getString(questionFlow.questions.get(i).answerAttribute, "");

        curOption.setText(curValue);
        curOption.setId(i);
        curOption.setGravity(Gravity.LEFT);
        curOption.setTextSize(TypedValue.COMPLEX_UNIT_SP,26);


        curOption.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams) curOption.getLayoutParams();
        //params.height = getResources().getDimensionPixelSize(R.dimen.edit_item_height);
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        params.gravity = Gravity.LEFT;
        params.bottomMargin = getResources().getDimensionPixelSize(R.dimen.margin_profile_edit);
        params.leftMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);
        params.rightMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);

        curOption.setLayoutParams(params);

        return curOption;
    }

    /**
    * @param i the index of the question to be added in the global questions list.
    * @return a text field where users can see the label for the current question
    */
    private TextView createLabelField(int i){

        final TextView curOptionLabel = new TextView(this);

        String curLabel = questionFlow.questions.get(i).displayLabel;

        curOptionLabel.setText(curLabel);
        curOptionLabel.setId(i);
        curOptionLabel.setGravity(Gravity.LEFT);
        curOptionLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP,26);
        curOptionLabel.setTextColor(getResources().getColor(R.color.colorOrange));

        curOptionLabel.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        LinearLayout.LayoutParams labelParams = (LinearLayout.LayoutParams) curOptionLabel.getLayoutParams();
        labelParams.height = getResources().getDimensionPixelSize(R.dimen.edit_item_height);
        labelParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        labelParams.gravity = Gravity.LEFT;
        if (i == 0){
            labelParams.topMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);
        }
        labelParams.leftMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);
        labelParams.rightMargin = getResources().getDimensionPixelSize(R.dimen.supermargin_profile_edit);

        curOptionLabel.setLayoutParams(labelParams);

        return curOptionLabel;
    }

    /**
    * This class organizes how data is stored for each question type and makes it easy to scale if other types of
    * questions are to be added
    */
    public class EditFieldInfo{

        public EditText textView = null;
        public MultiSelectionSpinner spinnerView = null;
        public String answerAttribute;

        public EditFieldInfo(EditText textView, MultiSelectionSpinner spinnerView, String answerAttribute){
            this.textView = textView;
            this.spinnerView = spinnerView;
            this.answerAttribute = answerAttribute;
        }
    }
}
