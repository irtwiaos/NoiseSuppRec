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

public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
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

    private static final String LOG_TAG = "AudioRecordTest";

    private static String mFileName = null;

    private MediaRecorder mRec = null;
    private MediaPlayer mPlay = null;

    private RecordButton RecButton = null;

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
            Log.e(LOG_TAG, "prepare() failed");
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
            Log.e(LOG_TAG, "prepare() failed");
        }

        mRec.start();
    }

    private void stopRecording() {
        mRec.stop();
        mRec.release();
        mRec = null;
    }

    class RecordButton extends ToggleButton{
        boolean startRec = true;

        OnClickListener click = new OnClickListener() {
            public void onClick(View v){
                onRecord(startRec);

                if (startRec){
                    setChecked(true);
                }

                else{
                    setChecked(false);
                }

                startRec = !startRec;
            }
    };

        public RecordButton(Context ctx){
            super(ctx);
            setChecked(false);
            setOnClickListener(click);
        }
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
