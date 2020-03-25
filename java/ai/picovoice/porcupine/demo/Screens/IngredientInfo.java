package ai.picovoice.porcupine.demo;

import android.app.Application;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import ai.api.model.AIResponse;

/**
 * This class corresponds to the IngredientInfo activity. This activity is used to display information about ingredients to the
 * user. This information is pulled from the FDA's website.
 */
public class IngredientInfo extends AppCompatActivity {

    private String LOG_TAG = "--- INGREDIENT INFO";
    private String userResponseStr;
    private String assistantResponseStr;

    private String requestedRecipe = "";
    private String requestedRecipePhotoUrl = "";

    private TextView calories;
    private TextView servingSize;
    private TextView protein;
    private TextView carbs;
    private TextView fat;

    private String ingredientName;
    private TextView pageTitle;
    private TextView macroLabel;
    private TextView microLabel;

    private LinearLayout macroLayout;
    private LinearLayout microLayout;

    private String lambdaResult;

    private Application application;
    private String activityName = "IngredientInfo";
    private ImageView triggerAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ingredient_info);
        Utils.configurePorcupine(this);

        application = this.getApplication();

        macroLabel = findViewById(R.id.macro_label);
        macroLayout = findViewById(R.id.macro);

        microLabel = findViewById(R.id.micro_label);
        microLayout = findViewById(R.id.micro);

        calories = findViewById(R.id.calories);
        servingSize = findViewById(R.id.serving_size);
        carbs = findViewById(R.id.carbs);
        protein = findViewById(R.id.protein);
        fat = findViewById(R.id.fat);

        triggerAssistant = findViewById(R.id.trigger_ganz);

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

        Bundle b = getIntent().getExtras();
        if(b != null)
            ingredientName = b.getString("ingredient_name");

        Log.d("---------------- ING", ingredientName);

        pageTitle = findViewById(R.id.ingredient_name);
        pageTitle.setText("Nutrition for " + ingredientName.replace("_", " "));

        String recipeById = ParsingUtils.getGetIngredientUrl(ingredientName);
        HttpGetRequest getRequest = new HttpGetRequest();

        try {
            lambdaResult = getRequest.execute(recipeById).get();
        }
        catch (ExecutionException | InterruptedException e){
            Log.e("Error in GET", e.toString());
        }
    }

    @Override
    protected void onStart(){

        super.onStart();
        parseResults(lambdaResult);

        macroLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (macroLayout.getVisibility() == View.VISIBLE) {
                    macroLayout.setVisibility(View.GONE);
                }
                else{
                    macroLayout.setVisibility(View.VISIBLE);
                }
            }
        });

        microLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (microLayout.getVisibility() == View.VISIBLE) {
                    microLayout.setVisibility(View.GONE);
                }
                else{
                    microLayout.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    public void callback(AIResponse aiResponse) {
        if (aiResponse != null) {
            // process aiResponse
            ParsingUtils.AssistantAnswer answerObj = ParsingUtils.parseAssistantAnswer(aiResponse);
            assistantResponseStr = answerObj.recipeNames.get(0);

            ((GanzApplication) application).getBumblebeeVoice().speak(assistantResponseStr,
                    TextToSpeech.QUEUE_FLUSH, null);

            ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                    answerObj.recipeIds.get(0), answerObj.photoUrls.get(0));

            requestedRecipe = answerObj.recipeIds.get(0);
            requestedRecipePhotoUrl = answerObj.photoUrls.get(0);

        } else {
            Log.d("No Response", "No response from assistant");
        }
    }

    private void parseResults(String lambdaResponse){
        try {

            JSONObject json = new JSONObject(lambdaResponse);

            Log.d("---------- PARSE", json.toString());

            JSONObject caloriesObj = new JSONObject(json.get("calories").toString());
            JSONObject quantityObj = new JSONObject(json.get("quantity").toString());

            JSONObject macroNutrients = new JSONObject(json.get("macro").toString());
            JSONObject microNutrients = new JSONObject(json.get("micro").toString());

            JSONObject carbsObj = new JSONObject(macroNutrients.get("Carbohydrate, by difference").toString());
            JSONObject proteinObj = new JSONObject(macroNutrients.get("Protein").toString());
            JSONObject fatObj = new JSONObject(macroNutrients.get("Total lipid (fat)").toString());

            servingSize.setText(quantityObj.get("amount").toString() + " " +
                    quantityObj.get("measure_unit").toString());

            calories.setText(caloriesObj.get("count").toString());


            String carbsInfo = "<font size = '3' color='#FF8C00'>" + carbsObj.get("count") +
                    " " + carbsObj.get("unit") + "</font>";

            carbs.setText("Carbohydrates: " + Html.fromHtml(carbsInfo));

            String proteinInfo = "<font size = '3' color='#FF8C00'>" + proteinObj.get("count") +
                    " " + proteinObj.get("unit") + "</font>";

            protein.setText("Protein: " + Html.fromHtml(proteinInfo));

            String fatInfo = "<font size = '3' color='#FF8C00'>" + fatObj.get("count") +
                    " " + fatObj.get("unit") + "</font>";

            fat.setText("Fat: " + Html.fromHtml(fatInfo));

            microLayout.removeAllViews();

            Iterator<?> keys = microNutrients.keys();
            while( keys.hasNext() ) {
                String nutrientName = (String) keys.next();
                JSONObject curObj = new JSONObject(microNutrients.get(nutrientName).toString());
                microLayout.addView(createTextField(nutrientName,
                        curObj.get("count").toString(), curObj.get("unit").toString()));
            }

        }
        catch (JSONException e){
            Log.e("Lambda parsing error:", e.toString());
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
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_filter) {
            ParsingUtils.showPopup(this, this, userResponseStr, assistantResponseStr,
                    requestedRecipe, requestedRecipePhotoUrl);
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
    public void onBackPressed () {
        super.onBackPressed();
        //Intent openPrevActivity = new Intent(IngredientInfo.this, RecipeIngredient.class);
        //openPrevActivity.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        //startActivityIfNeeded(openPrevActivity, 0);
    }

    private TextView createTextField(String name, String count, String unit){

        final TextView curNutrient = new TextView(this);

        curNutrient.setText(name + ": " + count + ": " + unit);
        curNutrient.setGravity(Gravity.CENTER_HORIZONTAL);
        curNutrient.setTextSize(TypedValue.COMPLEX_UNIT_SP,20);


        curNutrient.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));


        return curNutrient;
    }
}
