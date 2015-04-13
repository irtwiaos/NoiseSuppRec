package ece420.backgroundnoisesuppressionrecorder;

import android.media.AudioRecord;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.media.MediaRecorder;
import android.media.MediaPlayer;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.ToggleButton;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import java.io.IOException;
import java.util.Calendar;

import static android.view.View.*;

public class MainActivity extends ActionBarActivity {

    //private static final String LOG_TAG = "AudioRecordTest";

    private String mFileName = null;

    private MediaRecorder mRec = null;
    private MediaPlayer mPlay = null;

    ToggleButton RecButton;
    Button PlayButton;
    Chronometer timer;

    private boolean startPlay;

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
        startPlay = true;

        timer = (Chronometer)findViewById(R.id.chronometer);
        timer.setText("00:00");
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
            CreateFile();
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
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
        try {
            mPlay.setDataSource(mFileName);
            mPlay.prepare();
            mPlay.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mPlay.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                timer.stop();
                timer.setText("00:00");
                PlayButton.setText("Play");
                startPlay = true;
                RecButton.setEnabled(true);

            }
        });
    }

    private void stopPlaying() {
        timer.stop();
        timer.setText("00:00");
        mPlay.release();
        mPlay = null;
    }

    private void startRecording() {
        mRec = new MediaRecorder();
        mRec.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRec.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRec.setOutputFile(mFileName);
        mRec.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try {
            mRec.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
        mRec.start();
    }

    private void stopRecording() {
        mRec.stop();
        timer.stop();
        timer.setText("00:00");
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
                    PlayButton.setEnabled(false);
                }

                else{
                    RecButton.setChecked(false);
                    PlayButton.setEnabled(true);
                }

                startRec = !startRec;
            }
    };

    OnClickListener PlayClick = new OnClickListener() {

       @Override
       public void onClick(View v) {

           onPlay(startPlay);

           if (startPlay){
               PlayButton.setText("Stop");
               RecButton.setEnabled(false);
           }

           else{
               PlayButton.setText("Play");
               RecButton.setEnabled(true);
           }

           startPlay = !startPlay;
       }
   };

    private void CreateFile(){

        Calendar now = Calendar.getInstance();

        String ext = Integer.toString(now.get(Calendar.YEAR));
        ext += Integer.toString(now.get(Calendar.MONTH));
        ext += Integer.toString(now.get(Calendar.DATE));
        ext += Integer.toString(now.get(Calendar.HOUR));
        ext += Integer.toString(now.get(Calendar.MINUTE));

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/"+ext + ".3gp";
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
