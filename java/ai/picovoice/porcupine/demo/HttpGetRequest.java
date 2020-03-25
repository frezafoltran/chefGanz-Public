package ai.picovoice.porcupine.demo;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.channels.AcceptPendingException;

/**
* This class takes care of HTTP requests done in the background asyncronously to avoid delays when using the app. 
* The class is built generically such that any HTTP requests can be made, but this app mainly performs HTTP requests
* through AWS lambda.
*/
public class HttpGetRequest extends AsyncTask<String, Void, String> {

    public static final String REQUEST_METHOD = "GET";
    public static final int READ_TIMEOUT = 15000;
    public static final int CONNECTION_TIMEOUT = 15000;

    public static Activity activity;
    public static String activityName;

    public void setActivity(Activity curActivity, String activityName){
        this.activity = curActivity;
        this.activityName = activityName;
    }

    @Override
    protected String doInBackground(String... params){
        String stringUrl = params[0];
        String result;
        String inputLine;
        try {
            //Create a URL object holding url
            URL myUrl = new URL(stringUrl);
            //Create connection
            HttpURLConnection connection =(HttpURLConnection)
                    myUrl.openConnection();
            //Set methods and timeouts
            connection.setRequestMethod(REQUEST_METHOD);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setConnectTimeout(CONNECTION_TIMEOUT);

            connection.connect();
            //Create a new InputStreamReader
            InputStreamReader streamReader = new
                    InputStreamReader(connection.getInputStream());
            //Create a new buffered reader and String Builder
            BufferedReader reader = new BufferedReader(streamReader);
            StringBuilder stringBuilder = new StringBuilder();
            //Check if the line we are reading is not null
            while((inputLine = reader.readLine()) != null){
                stringBuilder.append(inputLine);
            }
            //Close InputStream and Buffered reader
            reader.close();
            streamReader.close();

            result = stringBuilder.toString();
        }
        catch(IOException e){
            e.printStackTrace();
            result = null;
        }
        return result;
    }

    protected void onPostExecute(String result){
        /**
         * This method parses the results from the lambda call
         */
        super.onPostExecute(result);

        if (activityName.equals("RecipeIntro")) {
            ((RecipeIntro) activity).parseResults(result);
            ((RecipeIntro) activity).updateRecipeInfo();

        }
        else if (activityName.equals("MainActivity")){
            ((MainActivity) activity).parseResults(result);
            ((MainActivity) activity).updateRecipesLayout();
        }
    }
}
