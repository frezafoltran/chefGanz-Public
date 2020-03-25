package ai.picovoice.porcupine.demo;

import android.app.Application;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

/**
* This class corresponds to the SimilarRecipesPopup activity. This activity is specific to a given activity and 
* shows the user recipes related to the main recipe.
*/
public class SimilarRecipesPopup extends AppCompatActivity {


    ArrayList<SubjectData> similar_recipes = new ArrayList<SubjectData>();
    String activityName = "SimilarRecipesPopup";
    private Application application;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overridePendingTransition(R.transition.slide_in, R.transition.slide_out);
        setContentView(R.layout.activity_similar_recipes_popup);

        application = this.getApplication();

        for (int i = 1; i <= 5; i ++){
            similar_recipes.add(new SubjectData("Similar recipe #" + i, "-2",
                    "https://frezafoltran.github.io/goose_logo.JPG"));
        }

    }

    @Override
    protected void onStart(){
        super.onStart();

        final ListView list = findViewById(R.id.similar_recipes_list);
        CustomAdapter customAdapter = new CustomAdapter(SimilarRecipesPopup.this,
                this, application, similar_recipes);
        list.setAdapter(customAdapter);
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

    public void goBack(View view) {
        setResult(RESULT_CANCELED);
        onBackPressed();
    }

    @Override
    public void onBackPressed(){
        super.onBackPressed();
        finish();
    }
}
