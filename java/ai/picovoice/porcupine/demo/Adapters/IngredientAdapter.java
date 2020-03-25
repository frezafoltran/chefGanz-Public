package ai.picovoice.porcupine.demo;

import android.app.Activity;
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
class IngredientAdapter implements ListAdapter {

    ArrayList<IngredientData> arrayList;
    Context context;
    Activity activity;

    public IngredientAdapter(Activity activity, Context context, ArrayList<IngredientData> arrayList) {
        this.arrayList=arrayList;
        this.context=context;
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
        final IngredientData ingredientData = arrayList.get(position);
        if(convertView == null) {
            LayoutInflater layoutInflater = LayoutInflater.from(context);

            convertView = layoutInflater.inflate(R.layout.ingredient_list_value, null);

            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ((RecipeIngredient) activity).goToIngredientInfo(ingredientData.name);
                }
            });
            TextView title = convertView.findViewById(R.id.ingredientName);
            title.setText(ingredientData.display_name);

            TextView quantity = convertView.findViewById(R.id.ingredientQuantity);
            quantity.setText(ingredientData.count + " " + ingredientData.unit);
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

