package ai.picovoice.porcupine.demo;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class SearchResults extends AppCompatActivity {

    private String LOG_TAG = "--- SEARCH RESULTS";

    //list of recipes for main page
    ArrayList<SubjectData> recipeArray = new ArrayList<SubjectData>();

    private Application application;
    private String activityName = "Search Results";
    private ImageView triggerAssistant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_results);

        application = this.getApplication();
        triggerAssistant = findViewById(R.id.trigger_ganz);

        Bundle b = getIntent().getExtras();
        String [] allIds = {};
        String [] allPhotoUrls = {};
        String [] allNames = {};

        if(b != null) {

            allPhotoUrls = b.getString("allRecipePhotoUrls").split(",!");
            allIds = b.getString("allRecipeIds").split(",!");
            allNames = b.getString("allRecipeNames").split(",!");

        }

        for (int i = 0; i < allIds.length; i ++){
            SubjectData newResult = new SubjectData(allNames[i], allIds[i], allPhotoUrls[i]);
            recipeArray.add(newResult);
        }

        final ListView list = findViewById(R.id.recipe_list_main);
        CustomAdapter customAdapter = new CustomAdapter(SearchResults.this, this,
                application,recipeArray);
        list.setAdapter(customAdapter);


    }

    @Override
    protected void onStart(){
        super.onStart();

        Utils.configurePorcupine(this);
        ((GanzApplication) application).initBumblebeeSpecs(this);
        ((GanzApplication) application).initBumblebeeVoice();
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
}
