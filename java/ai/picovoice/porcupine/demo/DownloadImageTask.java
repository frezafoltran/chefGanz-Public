package ai.picovoice.porcupine.demo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

/**
* This class takes care of downloading images in the background instead of doing that in the main UI. This ensures
* a good user experience since users won't need to wait for images to download to use activity.
*/
public class DownloadImageTask extends AsyncTask<String,Void, Bitmap> {
    ImageView imageView;

    public DownloadImageTask(ImageView imageView){
        this.imageView = imageView;
    }

  
    protected Bitmap doInBackground(String...urls){
        String urlOfImage = urls[0];
        Bitmap logo = null;
        try{
            InputStream is = new URL(urlOfImage).openStream();
              
            logo = BitmapFactory.decodeStream(is);
        }catch(Exception e){ // Catch the download exception
            e.printStackTrace();
        }
        return logo;
    }

    /** 
    * This method runs on the UI thread after doInBackground
     */
    protected void onPostExecute(Bitmap result){
        imageView.setImageBitmap(result);
    }
}
