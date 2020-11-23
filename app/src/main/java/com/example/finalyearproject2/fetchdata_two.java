package com.example.finalyearproject2;

import android.os.AsyncTask;
import android.speech.tts.TextToSpeech;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class fetchdata_two extends AsyncTask<Void,Void,Void> {

    String data="",line;
    String Usd="",china="";
    String pak="";

    @Override
    protected Void doInBackground(Void... voids) {

        try {
            URL url=new URL("http://data.fixer.io/api/latest?access_key=8c383cbd653ed5735b5f7e09a96fb6f8");
            HttpURLConnection httpURLConnection= (HttpURLConnection) url.openConnection();
            InputStream inputStream=httpURLConnection.getInputStream();
            BufferedReader bufferedReader=new BufferedReader(new InputStreamReader(inputStream));
            line="";
            while (line!=null){
                line=bufferedReader.readLine();
                data=data+line;
            }

            JSONObject myJsonObject=new JSONObject(data);
            JSONObject rateobject=myJsonObject.getJSONObject("rates");
            pak = rateobject.getString("PKR");
            Usd = rateobject.getString("USD");
            china = rateobject.getString("CNY");





        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }


        return null;
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if(pak==""&&Usd==""&&china==""){

            //frontpage.tts.speak("Your Internet Connection is Weak Try Again", TextToSpeech.QUEUE_ADD,null);

            cameracode.dol_value=0;
            cameracode.yuan_value=0;



        }else {
            float pkr_to_Dollar = Float.valueOf(pak) / Float.valueOf(Usd);
            float pkr_to_China = Float.valueOf(pak) / Float.valueOf(china);

            cameracode.dol_value=pkr_to_Dollar;
            cameracode.yuan_value=pkr_to_China;

            frontpage.dol_value=pkr_to_Dollar;
            frontpage.yuan_value=pkr_to_China;



        }



    }
}



