package ece420.backgroundnoisesuppressionrecorder;

import android.media.AudioRecord;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.ToggleButton;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import java.io.IOException;

import static android.view.View.*;

public class MainActivity extends ActionBarActivity {

    //private static final String LOG_TAG = "AudioRecordTest";

    private String mFileName = null;

    private MediaRecorder mRec = null;
    private MediaPlayer mPlay = null;

    ToggleButton RecButton;
    Button PlayButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecButton = (ToggleButton)findViewById(R.id.toggle);
        RecButton.setOnClickListener(RecClick);
        RecButton.setChecked(false);

        PlayButton = (Button)findViewById(R.id.button);
        PlayButton.setOnClickListener(PlayClick);
        PlayButton.setText("Play");
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onRecord(boolean start){
        if (start){
            startRecording();
        }

        else {
            stopRecording();
        }
    }

    private void onPlay(boolean start) {
        if (start) {

            /*get switch and checkbox options

            if (switch is true){
                NoiseRed(check box shit);
            }
            */

            startPlaying();
        }

        else {
            stopPlaying();
        }
    }

    private void startPlaying() {
        mPlay = new MediaPlayer();
        try {
            mPlay.setDataSource(mFileName);
            mPlay.prepare();
            mPlay.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void stopPlaying() {
        mPlay.release();
        mPlay = null;
    }

    private void startRecording() {
        mRec = new MediaRecorder();
        mRec.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRec.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mRec.setOutputFile(mFileName);
        mRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRec.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRec.start();
    }

    private void stopRecording() {
        mRec.stop();
        mRec.release();
        mRec = null;
    }

    OnClickListener RecClick = new OnClickListener() {
        boolean startRec = true;

            @Override
            public void onClick(View v){
                onRecord(startRec);

                if (startRec){
                    RecButton.setChecked(true);
                }

                else{
                    RecButton.setChecked(false);
                }

                startRec = !startRec;
            }
    };

    OnClickListener PlayClick = new OnClickListener() {
       boolean startPlay = true;

       @Override
       public void onClick(View v) {

           onPlay(startPlay);

           if (startPlay){
               PlayButton.setText("Stop");
           }

           else{
               PlayButton.setText("Play");
           }
       }
   };

    private void CreateFile(){

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/audiorecordtest.3gp";
    }
    @Override
    public void onPause(){

        super.onPause();

        if (mRec != null){
            mRec.release();
            mRec = null;
        }

        if (mPlay != null){
            mPlay.release();
            mPlay = null;
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if (mRec != null){
            mRec.release();
            mRec = null;
        }

        if (mPlay != null){
            mPlay.release();
            mPlay = null;
        }
    }
}
