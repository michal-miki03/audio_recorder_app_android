package com.example.audiorecorderapplication;


import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

public class Recorder extends AppCompatActivity {

    private static final int BITS_PER_SAMPLE = 16;
    private static final String AUDIO_RECORDINGS_FOLDER = "AudioRecordings";
    private static final String AUDIO_RECORDINGS_FILE_EXTENSION_WAV = ".wav";
    private static final String AUDIO_RECORDINGS_TEMP_FILE = "recording_temp.raw";
    private static final int RECORDER_SAMPLES = 44100;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_STEREO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static final int CHANNELS = 2;
    private Button btnStartRecord;
    private Button btnStopRecord;
    private Button btnDeleteRecording;
    private Button btnRecordingListOpen;
    private EditText txtName;
    private EditText txtSurname;
    private EditText txtTitle;
    private EditText txtDescription;
    private EditText[] txtTab = new EditText[4];

    private AudioRecord audioRecorder = null;
    private int bufferSize = 0;
    private Thread recorderThread = null;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recorder);

        btnStartRecord = findViewById(R.id.startRecording);
        btnStopRecord = findViewById(R.id.stop);
        btnDeleteRecording = findViewById(R.id.delete);
        btnRecordingListOpen = findViewById(R.id.list);
        txtName = findViewById(R.id.nameEditText);
        txtSurname = findViewById(R.id.surnameEditText);
        txtTitle = findViewById(R.id.titleEditText);
        txtDescription = findViewById(R.id.descriptionEditText);
        txtTab[0] = txtName;
        txtTab[1] = txtSurname;
        txtTab[2] = txtTitle;
        txtTab[3] = txtDescription;

        enableButtons(false);

        bufferSize = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );
    }

    private void enableButtons(boolean isRecording){
        enableButton(btnStartRecord, !isRecording);
        enableButton(btnStopRecord, isRecording);
        enableButton(btnDeleteRecording, !isRecording);
        enableButton(btnRecordingListOpen, !isRecording);
    }

    private void enableButton(Button btn, boolean isEnable){
        btn.setEnabled(isEnable);
    }

    private String getFileName(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);

        String name = txtName.getText().toString();
        String surname = txtSurname.getText().toString();
        String title = txtTitle.getText().toString();
        String description = txtDescription.getText().toString();

        if(!file.exists()){
            file.mkdirs();
        }
        return (file.getAbsolutePath() + "/" + name +  "-" + surname + '-' + title + "-" + description + AUDIO_RECORDINGS_FILE_EXTENSION_WAV);
    }

    private String getTempFileName(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath, AUDIO_RECORDINGS_TEMP_FILE);
        if(tempFile.exists()) tempFile.delete();
        return (file.getAbsolutePath() + "/" + AUDIO_RECORDINGS_TEMP_FILE);
    }

    public final void startRecording(final View view){
        boolean isCorrect = false;

        for(EditText edit : txtTab){
            if(edit.getText().toString().trim().length() != 0) {
                isCorrect = true;
            }
            else{
                edit.setText("Brak");
            }
        }

        String name = txtName.getText().toString().trim();
        String surname = txtSurname.getText().toString().trim();
        String title = txtTitle.getText().toString().trim();
        String description = txtDescription.getText().toString().trim();

        String fileName = name +  "-" + surname + '-' + title + "-" + description + AUDIO_RECORDINGS_FILE_EXTENSION_WAV;

        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);
        File[] files = file.listFiles();

        for(int ii=0;ii<files.length;ii++) {
            if (fileName.equals(files[ii].getName())) {
                isCorrect = false;
                Toast.makeText(this, "Nagranie o takich danych już istnieje. Wprowadź inne dane", Toast.LENGTH_SHORT).show();
            }
        }

        if(isCorrect) {
            enableButtons(true);
            audioRecorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    RECORDER_SAMPLES, RECORDER_CHANNELS,
                    RECORDER_AUDIO_ENCODING, bufferSize);

            int ii = audioRecorder.getState();
            if (ii == 1) audioRecorder.startRecording();
            isRecording = true;
            recorderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    saveData();
                }
            }, "AudioRecorderThread");
            recorderThread.start();
        }
        else{
            Toast.makeText(this, "Wpisz jakąś dane identyfikującą, by nagrywać", Toast.LENGTH_SHORT).show();
        }
    }

    public final void stopRecording(final View view){
        enableButtons(false);
        if(audioRecorder != null){
            isRecording = false;
            int ii = audioRecorder.getState();
            if(ii == 1) audioRecorder.stop();
            audioRecorder.release();
            audioRecorder = null;
            recorderThread = null;
        }
        copyFile(getTempFileName(),getFileName());
        deleteTempFile();
        enableButton(btnDeleteRecording, !isRecording);
        Toast.makeText(this, "Stworzono nowe nagranie", Toast.LENGTH_SHORT).show();
    }

    public final void deleteRecording(final View view){
        new File(getFileName()).delete();
        enableButton(btnDeleteRecording, isRecording);
        txtDescription.setText("");
        txtTitle.setText("");
        txtSurname.setText("");
        txtName.setText("");
        Toast.makeText(this, "Usunięto", Toast.LENGTH_SHORT).show();
    }

    private void saveData(){
        byte data[] = new byte[bufferSize];
        String fileName = getTempFileName();
        FileOutputStream os = null;

        try{
            os = new FileOutputStream(fileName);
        } catch(Exception e){
            e.printStackTrace();
        }
        int read;
        if(os != null){
            while(isRecording){
                read = audioRecorder.read(data, 0, bufferSize);
                if(AudioRecord.ERROR_INVALID_OPERATION != read){

                    try{
                        os.write(data);
                    }catch(Exception e){
                        e.printStackTrace();
                    }
                }
            }

            try{
                os.close();
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private void copyFile(String src, String dest){
        FileInputStream in;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        long byteRate = BITS_PER_SAMPLE * RECORDER_SAMPLES * CHANNELS / 8;
        byte[] data = new byte[bufferSize];
        byte[] header;

        try{
            in = new FileInputStream(src);
            out = new FileOutputStream(dest);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            header = prepareWaveFileHeader(totalAudioLen, totalDataLen, RECORDER_SAMPLES, 2, byteRate);
            out.write(header, 0, 44);
            while(in.read(data) != -1){
                out.write(data);
            }
            in.close();
            out.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    private void deleteTempFile(){
        new File(getTempFileName()).delete();
    }

    private byte[] prepareWaveFileHeader(long totalAudioLen, long totalDataLen,
                                         long longSampleRate, int channels, long byteRate) {
        byte[] header = new byte[44];
        header[0] = 'R';
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2*16/8);
        header[33] = 0;
        header[34] = BITS_PER_SAMPLE;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        return header;
    }


    public final void recordingListStart(final View view){
        finish();
        startActivity(new Intent(this, RecordingList.class));
    }
}
