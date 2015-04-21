package ece420.backgroundnoisesuppressionrecorder;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.app.Activity;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
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
import android.widget.Switch;
import android.widget.CheckBox;
import android.util.Log;
import android.view.View;
import android.content.Context;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import android.app.ListActivity;
import org.jtransforms.fft.DoubleFFT_1D;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;


import static android.view.View.*;

public class MainActivity extends ActionBarActivity {

    //private static final String LOG_TAG = "AudioRecordTest";

    private String mFileName = null;

    private AudioRecord mRec = null;
    private AudioTrack mPlay = null;

    ToggleButton RecButton;
    Button PlayButton;
    Chronometer timer;

    Switch NoiseReduction;
    CheckBox ResNoise;
    CheckBox AdditionalAtt;

    private int min;

    private boolean startPlay;

    BufferedOutputStream os = null;

    private boolean state = true;
    private int size;
    DataInputStream data = null;

    private ListView lv;
    private ArrayAdapter<String> listAdapter ;

    private List<String> myList;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        RecButton = (ToggleButton) findViewById(R.id.toggle);
        RecButton.setOnClickListener(RecClick);
        RecButton.setChecked(false);

        PlayButton = (Button) findViewById(R.id.button);
        PlayButton.setOnClickListener(PlayClick);
        PlayButton.setText("Play");
        startPlay = true;
        //stop = false;

        timer = (Chronometer) findViewById(R.id.chronometer);
        timer.setText("0:00");

        NoiseReduction = (Switch) findViewById(R.id.switch1);

        ResNoise = (CheckBox) findViewById(R.id.checkBox2);

        AdditionalAtt = (CheckBox) findViewById(R.id.checkBox3);
        //FileList
        lv = (ListView) findViewById(R.id.listView);
        myList = new ArrayList<String>();
        File directory = Environment.getExternalStorageDirectory();
        file = new File(directory + "/Music");
        if (!file.exists()) {
            file.mkdir();
        }

        File list[] = file.listFiles();
        for (int i = 0; i < list.length; i++) {
            myList.add(list[i].getName());
        }
        listAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, myList);


        listAdapter.notifyDataSetChanged();
        lv.setAdapter(listAdapter); //Set all the file in the list.
        lv.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
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
            boolean NoiseRed = NoiseReduction.isChecked();
            boolean ResidualNoise = ResNoise.isChecked();
            boolean AdditionalAttenuation = AdditionalAtt.isChecked();

            if(NoiseRed){
                try {
                    noiseRed(ResidualNoise, AdditionalAttenuation);
                }
                catch(IOException ex){
                   ex.printStackTrace();
                }
            }   // if switch is on, call basic noise reduction function
            try {
                startPlaying();
            } catch(IOException e){
                e.printStackTrace();
            }
        }

        else {
            stopPlaying();
        }
    }

    private void noiseRed(boolean ResNoise, boolean AddAtt)throws IOException{

            //basic noise reduction algorithm

            if(ResNoise){
                // call residual noise reduction
            }
            if(AddAtt){
                // call additional signal attenuation
            }
    }

    private void startPlaying()throws IOException{

        readPCMstream();

        int maxJitter = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mPlay = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT, maxJitter, AudioTrack.MODE_STREAM);

        int bytesread = 0;
        int ret = 0;
        int count = 512*1024;

        byte [] byteData = new byte[count];
        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();

        mPlay.play();

        while (bytesread < size){

            ret = data.read(byteData,0, count);

            if(ret!=-1) {
                mPlay.write(byteData, 0, ret);
                bytesread += ret;
            }
            else {
                break;
            }
        }

        timer.stop();
        timer.setText("0:00");
        PlayButton.setText("Play");
        startPlay = true;
        RecButton.setEnabled(true);

        mPlay.release();
        mPlay = null;
    }

    private void stopPlaying() {
        timer.stop();
        timer.setText("0:00");
        mPlay.release();
        mPlay = null;
    }

    private void startRecording() {

        min = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        mRec = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT, min);

        CreateFile();

        byte audioData[] = new byte[min];
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

        timer.setBase(SystemClock.elapsedRealtime());
        timer.start();
        mRec.startRecording();

        try {
            os = new BufferedOutputStream(new FileOutputStream(mFileName));
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (true) {
            int status = mRec.read(audioData, 0, audioData.length);

            if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                status == AudioRecord.ERROR_BAD_VALUE) {
                return;
            }

            try {
               os.write(audioData, 0, audioData.length);
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
        }
    }

    private void stopRecording() {
        mRec.stop();

        timer.stop();
        timer.setText("0:00");

        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

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

           //stop = !stop;
           startPlay = !startPlay;
       }
   };

    private void CreateFile(){

        Calendar now = Calendar.getInstance();

        String ext = Integer.toString(now.get(Calendar.YEAR));
        ext = ext + ""+Integer.toString(now.get(Calendar.MONTH)+1);
        ext = ext + ""+Integer.toString(now.get(Calendar.DATE));
        ext = ext + "_"+Integer.toString(now.get(Calendar.HOUR));
        ext = ext + ""+Integer.toString(now.get(Calendar.MINUTE));

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/Music/"+ext + ".raw";

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


