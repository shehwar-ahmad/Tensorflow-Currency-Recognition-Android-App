package com.example.finalyearproject2;

import android.app.Activity;
import android.os.Bundle;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(savedInstanceState==null){

            getFragmentManager().beginTransaction().replace(R.id.Container,cameracode.newInstance()).commit();

        }
    }
}
