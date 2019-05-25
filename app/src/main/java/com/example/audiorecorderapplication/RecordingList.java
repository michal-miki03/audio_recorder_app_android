package com.example.audiorecorderapplication;

import android.content.Context;
import android.content.Intent;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RecordingList extends AppCompatActivity {

    private ArrayList<String> voicesList = new ArrayList<>();
    private static final int BITS_PER_SAMPLE = 16;
    private static final String AUDIO_RECORDINGS_FOLDER = "AudioRecordings";
    private static final String AUDIO_RECORDINGS_FILE_EXTENSION_WAV = ".wav";
    private static final int RECORDER_SAMPLES = 44100;
    private static final int CHANNELS = 2;
    private int bufferSize = 0;
    private MyListAdapter adapter;
    private List<Integer> voicesToCombine = new ArrayList<>();
    ListView listView;
    Button combine;
    TextView title;
    MediaPlayer mp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recording_list);

        listView = findViewById(R.id.list);
        title = findViewById(R.id.title);
        combine = findViewById(R.id.combineRecords);

        bufferSize = AudioRecord.getMinBufferSize(
                8000,
                AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);
        if (!file.exists()) {
            file.mkdirs();
        }
        makeList(file);
    }

    public void makeList(File file) {

        File[] files = file.listFiles();
        voicesList.clear();
        if (files.length != 0) {
            for (File f : files) {
                String fileName = f.getName().trim();
                fileName = fileName.substring(0, fileName.length() - 4);
                String[] voiceData = fileName.split("-");
                voicesList.add("Imię: " + voiceData[0] + "\nNazwisko: " + voiceData[1] + "\nTytuł: " + voiceData[2] + "\nOpis: " + voiceData[3]);
            }
        } else title.setText("Nie dodałeś jeszcze żadnego nagrania :(");
        adapter = new MyListAdapter(this, R.layout.list_item, voicesList);
        listView.setAdapter(adapter);
    }

    private class MyListAdapter extends ArrayAdapter<String> {
        private int layout;
        private List<String> voices;
        ViewHolder viewHolder;

        private MyListAdapter(Context context, int recource, List<String> voices) {
            super(context, recource, voices);
            layout = recource;
            this.voices = voices;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {

            ViewHolder mainViewHolder = null;
            if (convertView == null) {
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(layout, parent, false);
                viewHolder = new ViewHolder();
                viewHolder.text = convertView.findViewById(R.id.text);
                viewHolder.play = convertView.findViewById(R.id.play);
                viewHolder.delete = convertView.findViewById(R.id.delete);
                viewHolder.checkBox = convertView.findViewById(R.id.checkBox);
                viewHolder.text.setText(voices.get(position));
                convertView.setTag(viewHolder);
                viewHolder.play.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String filepath = Environment.getExternalStorageDirectory().getPath();
                        File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);
                        File[] files = file.listFiles();
                        mp = new MediaPlayer();
                        try {
                            mp.setDataSource(files[position].getPath());
                            mp.prepare();
                            mp.start();
                            recordInfo();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                viewHolder.delete.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String filepath = Environment.getExternalStorageDirectory().getPath();
                        File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);
                        File[] files = file.listFiles();
                        files[position].delete();
                        deleteInfo();
                    }
                });

                viewHolder.checkBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if (((CompoundButton) view).isChecked()) {
                            voicesToCombine.add(position);
                        } else {
                            if (voicesToCombine.contains(position)) {
                                voicesToCombine.remove(position);
                            }
                        }
                        checkInfo();
                    }
                });
            } else {
                mainViewHolder = (ViewHolder) convertView.getTag();
                mainViewHolder.text.setText((getItem(position)));
            }
            return convertView;
        }
    }

    public class ViewHolder {
        TextView text;
        Button delete;
        Button play;
        CheckBox checkBox;
    }

    private void deleteInfo() {
        finish();
        startActivity(getIntent());
        Toast.makeText(this, "Usunięto", Toast.LENGTH_SHORT).show();
    }

    private void recordInfo() {
        Toast.makeText(this, "Trwa odtwarzanie", Toast.LENGTH_SHORT).show();
    }

    private void checkInfo() {
        Toast.makeText(this, "Zaznaczono/odznaczono", Toast.LENGTH_SHORT).show();
    }

    public void combineRecordings(View view) {
        if (voicesToCombine.size() == 2) {
            String filepath = Environment.getExternalStorageDirectory().getPath();
            File file = new File(filepath, AUDIO_RECORDINGS_FOLDER);
            File[] files = file.listFiles();
            Collections.sort(voicesToCombine);

            String file1Name = files[voicesToCombine.get(0)].getName();
            String file2Name = files[voicesToCombine.get(1)].getName();

            String file1Path = file.getAbsolutePath() + "/" + file1Name;
            String file2Path = file.getAbsolutePath() + "/" + file2Name;

            file1Name = file1Name.substring(0, file1Name.length() - 4);
            file2Name = file2Name.substring(0, file2Name.length() - 4);

            String[] file1SplitedName = file1Name.trim().split("-");
            String[] file2SplitedName = file2Name.trim().split("-");

            String fileDestName = file1SplitedName[0] + "," + file2SplitedName[0] + "-" +
                    file1SplitedName[1] + "," + file2SplitedName[1] + "-" +
                    file1SplitedName[2] + "," + file2SplitedName[2] + "-" +
                    file1SplitedName[3] + "," + file2SplitedName[3];

            String fileDestPath = file.getAbsolutePath() + "/" + fileDestName + AUDIO_RECORDINGS_FILE_EXTENSION_WAV;

            combineWaveFile(file1Path, file2Path, fileDestPath);

            finish();
            startActivity(getIntent());
        } else {
            Toast.makeText(this, "Zaznacz równo 2 nagrania", Toast.LENGTH_SHORT).show();
        }
    }

    private void combineWaveFile(String file1, String file2, String f3) {
        FileInputStream in1, in2;
        FileOutputStream out;
        long totalAudioLen;
        long totalDataLen;
        long byteRate = BITS_PER_SAMPLE * RECORDER_SAMPLES * CHANNELS / 8;
        byte[] data = new byte[bufferSize];
        byte[] header;

        try {
            in1 = new FileInputStream(file1);
            in2 = new FileInputStream(file2);
            out = new FileOutputStream(f3);
            totalAudioLen = in1.getChannel().size() + in2.getChannel().size();
            totalDataLen = totalAudioLen + 36;
            header = prepareWaveFileHeader(totalAudioLen, totalDataLen, RECORDER_SAMPLES, CHANNELS, byteRate);
            out.write(header, 0, 44);
            while (in1.read(data) != -1) {
                out.write(data);
            }
            while (in2.read(data) != -1) {
                out.write(data);
            }
            out.close();
            in1.close();
            in2.close();

            Toast.makeText(this, "Stworzono nowe nagranie", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
        }
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
        header[32] = (byte) (2 * 16 / 8);
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
}