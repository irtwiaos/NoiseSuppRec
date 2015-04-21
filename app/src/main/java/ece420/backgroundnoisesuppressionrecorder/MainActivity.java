package ece420.backgroundnoisesuppressionrecorder;

import android.media.AudioRecord;
import android.app.Activity;
import android.media.MediaCodec;
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

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
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

    private MediaRecorder mRec = null;
    private MediaPlayer mPlay = null;

    ToggleButton RecButton;
    Button PlayButton;
    Chronometer timer;

    Switch NoiseReduction;
    CheckBox ResNoise;
    CheckBox AdditionalAtt;

    private int size;
    private boolean startPlay;

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
            startPlaying();
        }

        else {
            stopPlaying();
        }
    }

    private void noiseRed(boolean ResNoise, boolean AddAtt)throws IOException{

            /*basic noise reduction algorithm                       MediaRecorder method -> XXXX

            String mMime = "audio/3gpp";
            MediaCodec codec = MediaCodec.createDecoderByType(mMime);

            MediaFormat mMediaFormat = new MediaFormat();
            mMediaFormat = MediaFormat.createAudioFormat(mMime,
                    mMediaFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE),
                    mMediaFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT));

            codec.configure(mMediaFormat, null, null, 0);
            codec.start();

            ByteBuffer[] inputBuffers = codec.getInputBuffers();
            ByteBuffer[] outputBuffers = codec.getOutputBuffers();


            MediaCodec.BufferInfo buf_info = new MediaCodec.BufferInfo();
            int outputBufferIndex = codec.dequeueOutputBuffer(buf_info, 0);
            byte[] pcm = new byte[buf_info.size];
            outputBuffers[outputBufferIndex].get(pcm, 0, buf_info.size); */


/********************** Create Spectrogram **************************/
        // grab the original sound as a double array from the readPCM function
        double[] sound = readPCM();
        // get the noise signal-- first (0.4*sample rate) samples of the sound signal
        double[] noise = new double[17640];
        for (int i = 0; i < noise.length; i++){
            noise[i] = sound[i];
        }
        // create spectrogram of the whole signal and the noise
        double[][] SSignal = spectrogram(sound); // S in matlab
        int SizeRow = SSignal.length;
        int SizeColumn = SSignal[0].length;
        double[][] SNoise = spectrogram(noise); // S_N in matlab
/******************* END of Creating Spectrogram ******************/
        /* noise reduction algorithm */
        int hopsize = 256; // directly from matlab
        double[] avgSN = new double[hopsize+1]; // avg_SN
        int ColumnN = SNoise[0].length; // size(S_N, 2), number of colums of S_N
        for (int i = 0; i < avgSN.length; i++){
            for (int j = 0; j < ColumnN; j++){
                avgSN[i] = avgSN[i] + Math.abs(SNoise[i][j]); // summation, as as matlab
            }
        }
        for (int i = 0; i < avgSN.length; i++){
            avgSN[i] = avgSN[i]/ColumnN; // division to get avg, same as matlab
        }

        // 3-frame averaging not implemented
        /* bias removal and half-wave rectifying, suppose no average and no attenuation*/
        double[][] SNew = new double[SizeRow][SizeColumn]; // S_new
        for (int i = 0; i < hopsize+1; i++){
            for (int j = 0; j < SizeColumn; j++){
                SNew[i][j] = Math.abs(SSignal[i][j]) - 3*avgSN[i];
                if (SNew[i][j] < 0){
                    SNew[i][j] = 0;
                }
            }
        }

        if(ResNoise){
                // call residual noise reduction
        }
        if(AddAtt){
                // call additional signal attenuation
        }
    }

    private double[][] spectrogram(double[] sound){
        int framesize = 512;
        int noverlap = 256;
        double[] w = new double[framesize]; //Hann Window
        /*double[] sound = new double[4096]; //original sound, just say it has 4096 samples*/
        double[] framebuffer = new double [2*framesize];
        int ncol = (int) Math.floor((sound.length-noverlap)/(framesize-noverlap)); // how many columns of Spectrogram
        double[][] S = new double[framesize/2+1][ncol];     //Actual 2D Array holding Spectrogram
        DoubleFFT_1D fft = new DoubleFFT_1D(2*framesize);

        for (int n=0;n<2*framesize;n++)         //zero all elements in the temp array, for ***ZERO-PADDING***
        {
            framebuffer[n] = 0;
        }

        for (int n = 0; n < framesize; n++)     // Hann window
        {
            w[n] = 0.54 - 0.46*Math.cos(2*Math.PI*n/(framesize-1));
        }

        for (int i = 0; i < sound.length; i = i + noverlap) {
            for (int j = 0; j < framesize; j++)         // Cut
            {
                framebuffer[i + j] = sound[i + j];
                framebuffer[i + j] *= w[j];                // Apply Window
            }
            fft.complexForward(framebuffer);            // FFT and same to the original array (this is a feature of JTransform Library)

            for (int j = 0; j < ncol; j++) {
                for (int k = 0; k < framesize; k++)       // Retain only half of temp array because FFT has redundant symmetrical conjugate.
                {
                    S[k][j] = framebuffer[k];       // Save as Real Spectrogram
                }
            }
        }
        return S;
    }

    private double[] readPCM() {
        double[] result = null;
        try {
            File file = new File(mFileName);
            InputStream in = new FileInputStream(file);
            int bufferSize = (int) (file.length()/2);
            int size = (int) file.length();
            result = new double[bufferSize];
            DataInputStream is = new DataInputStream(in);

            for (int i = 0; i < bufferSize; i++) {
                result[i] = is.readShort() / 32768.0;
            }
        } catch (FileNotFoundException e) {
            Log.i("File not found", "" + e);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void readPCMstream() {
        try {
            File file = new File(mFileName);
            InputStream in = new FileInputStream(file);
            int size = (int) file.length();
            DataInputStream data = new DataInputStream(in);
        } catch (FileNotFoundException e) {
            Log.i("File not found", "" + e);
        } catch (IOException e) {
            e.printStackTrace();
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
                timer.setText("0:00");
                PlayButton.setText("Play");
                startPlay = true;
                RecButton.setEnabled(true);

            }
        });
    }

    private void stopPlaying() {
        timer.stop();
        timer.setText("0:00");
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
        timer.setText("0:00");
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
        ext = ext + ""+Integer.toString(now.get(Calendar.MONTH)+1);
        ext = ext + ""+Integer.toString(now.get(Calendar.DATE));
        ext = ext + "_"+Integer.toString(now.get(Calendar.HOUR));
        ext = ext + ""+Integer.toString(now.get(Calendar.MINUTE));

        mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
        mFileName += "/Music/"+ext + ".3gp";

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


