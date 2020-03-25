package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;


/**
* This class builds elements of a list by using the information stored in SubjectData objects. 
* TODO currently this is the only class doing this, but there are different lists with different formats in the app. 
* Need to create separate classes to deal with each case
*/
class CustomAdapter implements ListAdapter {

    ArrayList<SubjectData> arrayList;
    Context context;
    Activity activity;
    Application application;

    public CustomAdapter(Activity activity, Context context, Application application,
                         ArrayList<SubjectData> arrayList) {

        this.application = application;
        this.arrayList = arrayList;
        this.context = context;
        this.activity = activity;
    }
    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }
    @Override
    public boolean isEnabled(int position) {
        return true;
    }
    @Override
    public void registerDataSetObserver(DataSetObserver observer) {
    }
    @Override
    public void unregisterDataSetObserver(DataSetObserver observer) {
    }
    @Override
    public int getCount() {
        return arrayList.size();
    }
    @Override
    public Object getItem(int position) {
        return position;
    }
    @Override
    public long getItemId(int position) {
        return position;
    }
    @Override
    public boolean hasStableIds() {
        return false;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        final SubjectData subjectData = arrayList.get(position);
        if(convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);

            if (subjectData.Image.length() > 0) {
                convertView = layoutInflater.inflate(R.layout.recipe_main_value, null);
            }
            else{
                convertView = layoutInflater.inflate(R.layout.ingredient_list_value, null);
            }

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    //"-2" indicates not clickable item for now. TODO make all items clickable
                    if (!(subjectData.id).equals("-2")) {

                        //The format "ing_{ingredient_name} indicates click on an ingredient"
                        //TODO make this more elegant/scalable
                        if (subjectData.id.length() > 3 && subjectData.id.substring(0, 4).equals("ing_")){
                            ((RecipeIngredient) activity).goToIngredientInfo(
                                    subjectData.id.substring(4, subjectData.id.length()));
                        }
                        else {
                            // TODO change to ParsingUtils.navigateToTarget();
                            //((MainActivity) activity).goToRecipe(subjectData.id);
                            ParsingUtils.navigateToTarget(application, context, RecipeIntro.class, subjectData.id);
                        }
                    }

                }
            });
            TextView tittle = convertView.findViewById(R.id.recipeTitle);
            tittle.setText(subjectData.SubjectName);

            if (subjectData.Image.length() > 0) {

                ImageView imag = convertView.findViewById(R.id.icon);

                Picasso.with(context)
                        .load(subjectData.Image).resize(0, 3200)
                        .placeholder(R.drawable.loading_image)
                        .onlyScaleDown() // only resize if bigger than target
                        .into(imag);
            }
        }
        return convertView;
    }
    @Override
    public int getItemViewType(int position) {
        return position;
    }
    @Override
    public int getViewTypeCount() {
        return arrayList.size();
    }
    @Override
    public boolean isEmpty() {
        return false;
    }
}
