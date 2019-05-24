package com.example.audiorecorderapplication;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public final void recorderStart(final View view){
        startActivity(new Intent(this, Recorder.class));
    }

    public final void recordingListStart(final View view){
        startActivity(new Intent(this, RecordingList.class));
    }
}
