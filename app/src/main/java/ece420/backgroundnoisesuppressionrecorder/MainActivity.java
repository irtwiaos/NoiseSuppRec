package ece420.backgroundnoisesuppressionrecorder;

import android.inputmethodservice.Keyboard;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.app.Activity;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.os.AsyncTask;
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
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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

        private AudioRecord mRec = null;
        private AudioTrack mPlay = null;

        ToggleButton RecButton;
        Button PlayButton;
        Button ProcessButton;
        Chronometer timer;

        Switch NoiseReduction;
        CheckBox ResNoise;
        CheckBox AdditionalAtt;

        private int size;
        private boolean startPlay;
        private boolean isRec;
        private int min;
        private boolean isCont;
        private double[] xr;

        private double[][] SSignal_raw;
        private int SizeRow;
        private int SizeColumn;
        private double[][] SNoise_raw;
        private int ColumnN;
        private int RowN;
        private double[][] SNoise;
        private double[][] SSignal;
        private int hopsize = 256;
        private int framesize = 512;
        private double[] avgSN;
        private double[][] SNew;
        private double[][] SNew_raw;

        DataInputStream data = null;
        BufferedOutputStream os = null;

        private ListView lv;
        private ArrayAdapter<String> listAdapter;

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

            ProcessButton = (Button) findViewById(R.id.button2);
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


        private void onRecord(boolean start) {
            if (start) {
                CreateFile();

                startRecording record = new startRecording();
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
                record.execute();
            } else {
                isRec = false;
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

                //startPlaying();
                isCont = true;
                startPlaying isPlay = new startPlaying();
                timer.setBase(SystemClock.elapsedRealtime());
                timer.start();
                isPlay.execute();
            } else {
                //mPlay.stop();
                isCont = false;
                //stopPlaying();
            }
        }


        private void noiseRed(boolean ResNoise, boolean AddAtt) throws IOException {
            //********************** Create Spectrogram **************************
            // grab the original sound as a double array from the readPCM function
            double[] sound = readPCM();
            // get the noise signal-- first (0.4*sample rate) samples of the sound signal
            double[] noise = new double[17640];
            for (int i = 0; i < noise.length; i++) {
                noise[i] = sound[i];
            }
            // create spectrogram of the whole signal and the noise
            SSignal_raw = spectrogram(sound); // S in matlab
            SizeRow = SSignal_raw.length/2;
            SizeColumn = SSignal_raw[0].length; //size(S, 2)
            SNoise_raw = spectrogram(noise); // S_N in matlab
            ColumnN = SNoise_raw[0].length; // size(S_N, 2), number of colums of S_N
            RowN = SNoise_raw.length/2;

            SNoise = new double [RowN][ColumnN];
            for (int i = 0; i < RowN; i++){
                for (int j = 0; j < ColumnN; j++){
                    SNoise[i][j] = Math.sqrt(SNoise_raw[i*2][j]*SNoise_raw[i*2][j] + SNoise_raw[i*2+1][j]*SNoise_raw[i*2+1][j]);
                }
            }
            SSignal = new double [SizeRow][SizeColumn];
            for (int i = 0; i < SizeRow; i++){
                for (int j = 0; j < SizeColumn; j++){
                    SSignal[i][j] = Math.sqrt(SSignal_raw[i*2][j]*SSignal_raw[i*2][j] + SSignal_raw[i*2+1][j]*SSignal_raw[i*2+1][j]);
                }
            }

            // noise reduction algorithm
            avgSN = new double[hopsize + 1]; // avg_SN
            for (int i = 0; i < avgSN.length; i++) {
                for (int j = 0; j < ColumnN; j++) {
                    avgSN[i] = avgSN[i] + Math.abs(SNoise[i][j]); // summation, as as matlab
                }
            }
            for (int i = 0; i < avgSN.length; i++) {
                avgSN[i] = avgSN[i] / ColumnN; // division to get avg, same as matlab
            }

            // 3-frame averaging not implemented
        /* bias removal and half-wave rectifying, suppose no average and no attenuation*/
            SNew = new double[SizeRow][SizeColumn]; // S_new
            for (int i = 0; i < hopsize + 1; i++) {
                for (int j = 0; j < SizeColumn; j++) {
                    SNew[i][j] = Math.abs(SSignal[i][j]) - 3 * avgSN[i];
                    if (SNew[i][j] < 0) {
                        SNew[i][j] = 0;
                    }
                }
            }
            for (int i = 0; i < hopsize + 1; i++) {
                for (int j = 0; j < SizeColumn; j++) {
                    SNew[i][j] = SNew[i][j];
                }
            }

            if (ResNoise) {
                // call residual noise reduction
                ResNoise();
            }

            // add phase to S_new
            SNew_raw = new double[SizeRow*2][SizeColumn];
            for (int i = 0; i < SizeRow; i++){
                for (int j = 0; j < SizeColumn; j++){
                    SNew_raw[i*2][j] = SSignal_raw[i*2][j]*(SNew[i][j]/SSignal[i][j]);
                    SNew_raw[i*2+1][j] = SSignal_raw[i*2+1][j]*(SNew[i][j]/SSignal[i][j]);
                }
            }

            if (AddAtt) {
                // call additional signal attenuation
                AddAtt();
            }

            // inverse FFT
            int frameN =  SizeColumn;
            xr = new double[framesize + (frameN - 1)*hopsize]; //anticipated x length
            for(int i = 0; i < xr.length; i++){
                xr[i] = 0;
            }
            double[] XR = new double[SizeRow*2 + framesize/2-1];
            double[] XR_second = new double [framesize/2-1];
            for (int i = 0; i < hopsize*frameN; i+= hopsize){
                for (int j = 0; j < SizeRow*2; j++){
                    XR[j] = SNew_raw[j][1+i/hopsize];
                }
                for (int k = 0; k < framesize/2 - 1; k--){
                    if((framesize/2 - 1 - k) % 2 == 0){
                        XR_second[k] = XR[framesize/2 - 1 - k];
                    }
                    else if ((framesize/2 - 1 - k) % 2 == 1){
                        XR_second[k] = - XR[framesize/2 - 1 - k];
                    }
                }
                for (int l = 0; l < framesize/2 - 1; l++){
                    XR[l + SizeRow*2] = XR_second[l];
                }
                DoubleFFT_1D fft = new DoubleFFT_1D(2 * framesize);
                fft.complexInverse(XR, true);
                double[] XR_real = new double[XR.length/2];
                for (int n = 0; n < XR.length/2; n++){
                    XR_real[n] = XR[2*n];
                }
                double[] w = new double[framesize]; //Hann Window
                for (int n = 0; n < framesize; n++)     // Hann window
                {
                    w[n] = 0.54 - 0.46 * Math.cos(2 * Math.PI * n / (framesize - 1));
                }
                for (int m = i; m < i + framesize; m++){
                    xr[m] = xr[m] + XR[m - i] * w[m - i];
                }
            }
        }

        private void ResNoise(){
            double[][] NR_raw = new double [RowN*2][ColumnN];
            for (int i = 0; i < RowN; i++){
                for (int j = 0; j < ColumnN; j++){
                    NR_raw[i*2][j] = SNew_raw[i*2][j] - avgSN[j] * SNew_raw[i*2][j]/SNew[i][j];
                    NR_raw[i*2+1][j] = SNew_raw[i*2+1][j] - avgSN[j] * SNew_raw[i*2+1][j]/SNew[i][j];
                }
            }
            double [][] NR = new double [RowN][ColumnN];
            for (int i = 0; i < RowN; i++){
                for (int j = 0; j < ColumnN; j++){
                    NR[i][j] = Math.sqrt(NR_raw[i*2][j]*NR_raw[i*2][j] + NR_raw[i*2+1][j]*NR_raw[i*2+1][j]);
                }
            }
            double[] maxNR_abs = new double[avgSN.length];
            for (int i = 0; i < NR.length; i++){
                maxNR_abs[i] = maxInArray(NR[i]);
            }

            int last = ColumnN - 1;
            for (int i = 0; i < RowN; i++){
                SNew[i][0] = minThree(SNew[i][0], SNew[i][1], SNew[i][2]);
                SNew[i][last] = minThree(SNew[i][last], SNew[i][last - 1], SNew[i][last - 2]);
             }

            for (int j = 1; j < ColumnN - 1; j++){
                for (int i = 0; i < RowN; i++){
                    if(SNew[i][j] < maxNR_abs[i]){
                        SNew[i][j] = minThree(SNew[i][j-1], SNew[i][j], SNew[i][j+1]);
                   }
               }
            }
        }

        private void AddAtt(){
            double[] TT = new double[SizeColumn];
            for (int j = 0; j < SizeColumn; j++){
                for (int i = 0; i < SizeRow; i++){
                    double sum = 0;
                    for(int k = 0; k < SizeRow; k++){
                        sum = sum + SNew[k][j]/avgSN[k];
                    }
                    TT[j] = 20*Math.log10(sum/SizeRow);
                    if(TT[j] < -35){
                        SNew_raw[2*i][j] = 0.0316 * SSignal_raw[2*i][j];
                        SNew_raw[2*i+1][j] = 0.0316 * SSignal_raw[2*i+1][j];
                    }
                }
            }
        }

        private double maxInArray(double[] array){
            double value = 0.0;
            if(array != null){
                int size = array.length;
                value = array[0];
                for (int i = 0; i < size; i++){
                    if (array[i] > value){
                        value = array[i];
                    }
                }
            }
            return  value;

        }

        private double minThree(double first, double second, double third){
            double result = first;
            if (second > result){
                result = second;
            }
            if(third > result){
                result = third;
            }
            return result;
        }

        private double[][] spectrogram(double[] sound){
            int framesize = 512;
            int noverlap = 256;
            double[] w = new double[framesize]; //Hann Window
            //sound = new double[4096]; //original sound, just say it has 4096 samples
            double[] framebuffer = new double [2*framesize];
            int ncol = (int) Math.floor((sound.length-noverlap)/(framesize-noverlap)); // how many columns of Spectrogram
            double[][] S = new double[(framesize/2+1)*2][ncol];     //Actual 2D Array holding Spectrogram
            DoubleFFT_1D fft = new DoubleFFT_1D(4*framesize); //framebuffer = 2*framesize; fftsize = 2*framebuffer

            for (int n=0;n<2*framesize;n++)         //zero all elements in the temp array, for ***ZERO-PADDING***
            {
                framebuffer[n] = 0;
            }

            for (int n = 0; n < framesize; n++)     // Hann window
            {
                w[n] = 0.54 - 0.46*Math.cos(2*Math.PI*n/(framesize-1));
            }

            for (int i = 0; i < sound.length; i = i + noverlap) {
                for (int j = 0; j < framesize; j++)         // Cut in half
                {
                    framebuffer[i + j] = sound[i + j];
                    framebuffer[i + j] *= w[j];                // Apply Window
                }
                fft.complexForward(framebuffer);            // FFT and save to the original array (this is a feature of JTransform Library)

                for (int j = 0; j < ncol; j++) {
                    for (int k = 0; k < framesize; k++)       // Retain only half of temp array because FFT has redundant symmetrical conjugate.
                    {
                        S[k][j] = framebuffer[k];       // Save as Output Spectrogram
                    }
                }
            }
            return S;
        }

        public double[] readPCM() {
            File file = new File(mFileName);
            InputStream in = null;
            if (file.isFile()) {
                long size = file.length();
                try {
                    in = new FileInputStream(file);
                    return readStreamAsDoubleArray(in, size);
                } catch (Exception e) {
                }
            }
            return null;
        }

        public static double[] readStreamAsDoubleArray(InputStream in, long size)
                throws IOException {
            int bufferSize = (int) (size / 2);
            double[] result = new double[bufferSize];
            DataInputStream is = new DataInputStream(in);
            for (int i = 0; i < bufferSize; i++) {
                result[i] = is.readDouble() / 32768.0;
            }
            return result;
        }

        public double[] readPCMstream() {
            File file = new File(mFileName);
            InputStream in = null;
            if (file.isFile()) {
                size = (int)file.length();
                try {
                    in = new FileInputStream(file);
                    data = new DataInputStream(in);
                } catch (Exception e) {
                }
            }
            return null;
        }

        private class startPlaying extends AsyncTask<Void, Integer, Void> {

            @Override
            protected Void doInBackground(Void... params) {
                readPCMstream();

                int maxJitter = AudioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
                mPlay = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, maxJitter, AudioTrack.MODE_STREAM);

                int bytesread = 0;
                int ret = -1;
                int count = 512;

                byte[] byteData = new byte[count];

                mPlay.play();

                while (bytesread < size && isCont) {

                    try {
                        ret = data.read(byteData, 0, count);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if (ret != -1) {
                        mPlay.write(byteData, 0, ret);
                        bytesread += ret;
                    } else {
                        break;
                    }
                }
                return null;
            }

            protected void onPostExecute(Void result) {
                mPlay.stop();
                stopPlaying();
            }
        }

        private void stopPlaying() {

            timer.stop();
            timer.setText("0:00");
            PlayButton.setText("Play");
            startPlay = true;
            RecButton.setEnabled(true);
            mPlay.release();
        }

        private class startRecording extends AsyncTask<Void, Integer, Void> {

            @Override
            protected Void doInBackground(Void... params) {

                isRec = true;

                min = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                mRec = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, min);

                //CreateFile();

                byte audioData[] = new byte[min];
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);

                try {
                    os = new BufferedOutputStream(new FileOutputStream(mFileName));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mRec.startRecording();
                while (isRec) {
                    int status = mRec.read(audioData, 0, audioData.length);

                    if (status == AudioRecord.ERROR_INVALID_OPERATION ||
                            status == AudioRecord.ERROR_BAD_VALUE) {
                        //return;
                    }

                    try {
                        os.write(audioData, 0, audioData.length);
                    } catch (IOException e) {
                        e.printStackTrace();
                        //return;
                    }
                }

                return null;
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
            public void onClick(View v) {

                if (startRec) {
                    RecButton.setChecked(true);
                    PlayButton.setEnabled(false);
                    onRecord(startRec);
                } else {
                    RecButton.setChecked(false);
                    PlayButton.setEnabled(true);
                    onRecord(startRec);
                }

                startRec = !startRec;
            }
        };

        OnClickListener PlayClick = new OnClickListener() {

            @Override
            public void onClick(View v) {

                onPlay(startPlay);

                if (startPlay) {
                    PlayButton.setText("Stop");
                    RecButton.setEnabled(false);
                } else {
                    PlayButton.setText("Play");
                    RecButton.setEnabled(true);
                }

                startPlay = !startPlay;
            }
        };

        private void CreateFile() {

            Calendar now = Calendar.getInstance();

            String ext = Integer.toString(now.get(Calendar.YEAR));
            ext = ext + "" + Integer.toString(now.get(Calendar.MONTH) + 1);
            ext = ext + "" + Integer.toString(now.get(Calendar.DATE));
            ext = ext + "_" + Integer.toString(now.get(Calendar.HOUR));
            ext = ext + "" + Integer.toString(now.get(Calendar.MINUTE));

            mFileName = Environment.getExternalStorageDirectory().getAbsolutePath();
            mFileName += "/Music/" + ext + ".pcm";

        }


        @Override
        public void onPause() {

            super.onPause();

            if (mRec != null) {
                mRec.release();
                mRec = null;
            }

            if (mPlay != null) {
                mPlay.release();
                mPlay = null;
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();

            if (mRec != null) {
                mRec.release();
                mRec = null;
            }


            if (mPlay != null) {
                mPlay.release();
                mPlay = null;
            }
        }
    }
