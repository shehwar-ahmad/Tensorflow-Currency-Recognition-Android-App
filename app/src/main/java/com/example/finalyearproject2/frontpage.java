package com.example.finalyearproject2;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class frontpage extends AppCompatActivity {

    public static TextToSpeech tts;


    boolean activity_status;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_frontpage);

        get_dollar_yuan_values_for_recognition();


    }


    private void get_dollar_yuan_values_for_recognition(){
        boolean connected = false;
        ConnectivityManager connectivityManager = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        if(connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
            connected = true;
        }
        else
        { connected = false;
        }



        if(connected==true){
            fetchdata_two proc=new fetchdata_two();
            proc.execute();

        }
    }


    private void outputvoice(){
        HashMap<String, String> params = new HashMap<String, String>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"shehwar");
        tts.speak("Input your Country",TextToSpeech.QUEUE_FLUSH,params);

    }


    private int i=1;


    private int temp;

    private void getvoiceinput(){
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //Intent intent = new Intent(RecognizerIntent.ACTION_VOICE_SEARCH_HANDS_FREE);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Pakistan | China | USA ");

        intent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, new Long(2000));

/*        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 1);
            temp=i;
            new java.util.Timer().schedule(
                    new java.util.TimerTask() {
                        @Override
                        public void run() {

                            finishActivity(1);
                            if(temp!=1){
                                temp=1;
                            }
                        }
                    },
                    6000
            );
        } else {
            Toast.makeText(getApplicationContext(), "Device Don't Support Speech Input", Toast.LENGTH_SHORT).show();
        }


        */

        startActivityForResult(intent, 1);
        Timer timer = new Timer();
        TimerTask t = new TimerTask() {
            @Override
            public void run() {
                finishActivity(1);
            }
        };
        timer.schedule(t,6000);



    }





    public static float dol_value=0;
    public static float yuan_value=0;


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case 1:
                temp++;
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    String res=result.get(0).toString().toLowerCase();

                    if(res.equals("pakistan")||res.equals("china")||res.equals("usa")){
                        classificationcode.get_choice=res;
                        Intent intent = new Intent(frontpage.this, MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                        startActivity(intent);
                        finish();

                    }else if(res.equals("conversion")||res.equals("convert")||res.equals("currency")){

                            if(dol_value!=0&&yuan_value!=0) {
                                Toast.makeText(getApplicationContext(), "Connected", Toast.LENGTH_SHORT).show();
                                HashMap<String, String> params = new HashMap<String, String>();
                                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"shehwar");
                                tts.speak("Rate of Dollar is  " + dol_value, TextToSpeech.QUEUE_ADD, null);
                                tts.speak("and Rate of Yuan is  " + yuan_value, TextToSpeech.QUEUE_ADD, params);
                                //tts.speak(" ", TextToSpeech.QUEUE_ADD, params);

                            }else {

                                HashMap<String, String> params1 = new HashMap<String, String>();
                                params1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"shehwar");
                                tts.speak("You are not connected to any network or Connection is weak", TextToSpeech.QUEUE_FLUSH,params1);
                                Toast.makeText(getApplicationContext(), "Weak Connection", Toast.LENGTH_SHORT).show();
                                get_dollar_yuan_values_for_recognition();

                            }

                    }else if (res.equals("close")||res.equals("exit")){
                        finish();
                    } else {
                        HashMap<String, String> params1 = new HashMap<String, String>();
                        params1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"shehwar");
                        tts.speak("Wrong Input", TextToSpeech.QUEUE_FLUSH,params1);

                    }
                }else if(resultCode==RESULT_CANCELED&&data==null){
                    HashMap<String, String> params1 = new HashMap<String, String>();
                    params1.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID,"shehwar");
                    tts.speak("Try Again", TextToSpeech.QUEUE_FLUSH,params1);

                }

                break;
        }
    }


    @Override
    protected void onPause() {
        activity_status=false;
        handler.removeCallbacksAndMessages(null);

            if(tts != null) {
                tts.stop();
            }
        super.onPause();
    }
    final Handler handler = new Handler();
    private void wait_for(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        getvoiceinput();
                    }
                }, 4000);

            }
        });
    }

    private int a=0;

    @Override
    protected void onRestart() {
        super.onRestart();
        a=0;
    }


    private void run_voice_func(){
        if(a==0){
            outputvoice();
            a++;
        }

    }
    @Override
    protected void onResume() {
        super.onResume();

        activity_status=true;
        tts=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS){
                    int result= tts.setLanguage(Locale.ENGLISH);
                    if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED)
                    {
                        Log.e("TTS","Language not Supported" );
                        return;
                    }
                    tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                        @Override
                        public void onDone(String utteranceId) {
                            if (utteranceId.equals("shehwar")&&activity_status==true){
                                wait_for();
                            }
                        }

                        @Override
                        public void onError(String utteranceId) {

                        }

                        @Override
                        public void onStart(String utteranceId) {

                        }
                    });
                    run_voice_func();

                }else {
                    Log.e("TTS","Init Failed" );
                }
            }
        });

    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(tts != null) {
            tts.stop();
            tts.shutdown();
        }
        finishActivity(1);
        finish();
    }


}
