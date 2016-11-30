package com.group3.synthesizerapp;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.group3.synthesizerapp.database.PresetDataHelper;
import com.group3.synthesizerapp.keyboard.KeyboardView;
import com.group3.synthesizerapp.keyboard.ScrollStripView;
import com.group3.synthesizerapp.knob.KnobListener;
import com.group3.synthesizerapp.knob.KnobView;
import com.group3.synthesizerapp.midi.MidiListener;
import com.group3.synthesizerapp.verticalSeekBar.CustomSeekBar;
import com.group3.synthesizerapp.verticalSeekBar.CustomSeekBarListener;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Semaphore;
import static android.net.Uri.*;

public class MainActivity extends AppCompatActivity
{
    static final int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 10;

    KnobView [] knobViews;
    CustomSeekBar [] seekBars;
    KeyboardView keyboardView;
    ScrollStripView scrollStripView;

    PresetDataHelper pdh;
    
    Button record;
    Button preset;
    Button play;
    Button change_view;

    Button [] sub_views;
    
    ArrayList<String> presetList;
    ArrayAdapter<String> fileAdapter;
    
    Spinner presetSpinner;
    Spinner fileSpinner;
    
    MediaPlayer mp;

    Thread thread;
    File wavDirectory;
    File wavFile;

    String wavDirectoryPath = Environment.getExternalStorageDirectory().getAbsolutePath()+"/SynthesizerApp/";

    boolean isPlaying;
    boolean isRecording;
    boolean permissionGranted;
    
    int xView;
    int parameterCounter;
    int lastView;
    int playId;
    int sampleRate;






    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        isPlaying = false;
        isRecording = false;
        permissionGranted = false;

        xView = 0;
        parameterCounter = 100;
        lastView = 0;
        playId = 0;
        sampleRate = 44100;





        change_view = (Button) findViewById(R.id.change_view);
        preset = (Button) findViewById(R.id.save_preset);
        record = (Button) findViewById(R.id.record);
        play = (Button) findViewById(R.id.play);

        sub_views = new Button[5];
        sub_views[0] = (Button) findViewById(R.id.mstr);
        sub_views[1] = (Button) findViewById(R.id.op_0);
        sub_views[2] = (Button) findViewById(R.id.op_1);
        sub_views[3] = (Button) findViewById(R.id.op_2);
        sub_views[4] = (Button) findViewById(R.id.op_3);


        thread = new Thread() {
            public void run() {
                setPriority(Thread.MAX_PRIORITY);
                if (Build.DEVICE.startsWith("generic")) {
                    System.out.println("Emulator detected...");
                    try {
                        sampleRate = 16000;
                        startProcess(sampleRate, 1024);
                    } catch (UnsatisfiedLinkError e) {
                        System.err.println("function not found.\n" + e);
                        System.exit(1);
                    }
                } else {
                    System.out.println("Hardware detected...");
                    try {
                        sampleRate = 44100;
                        startProcess(sampleRate, 512);
                    } catch (UnsatisfiedLinkError e) {
                        System.err.println("function not found\n" + e);
                        System.exit(1);
                    }
                }
            }
        };
        thread.start();

        pdh = new PresetDataHelper(this);


        checkPermissionForWrite();
        if (permissionGranted) {
            wavDirectory = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/SynthesizerApp");

            if (!wavDirectory.exists()) {
                wavDirectory.mkdir();
            }

        }


        setupMenuButtons();
        setupMidi();
        setupKnobs();
        setupSeekBarsAndSwitch();
        xorView();
        tabbedView();
        setupSpinners();

        findViewById(R.id.keyboard_layout).setVisibility(View.VISIBLE);
        findViewById(R.id.parameter_layout).setVisibility(View.INVISIBLE);
        defaultParameters();

    }

    @Override
    protected void onStart() {
        super.onStart();
        defaultParameters();
    }
    @Override
    protected void onResume() {

        super.onResume();
    }
    @Override
    protected void onRestart() {

        super.onRestart();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(isPlaying)
        {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),"Now stopped.", Toast.LENGTH_SHORT).show();
                }
            });
            mp.setLooping(false);
            mp.stop();
            mp.release();
        }
        stopRecordJava();
    }
    @Override
    protected void onStop() {
        super.onStop();
    }
    public void onDestroy() {
        super.onDestroy();
        stopProcess();
        try {
            thread.join();
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        thread = null;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                    permissionGranted = true;

                } else {

                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    void checkPermissionForWrite()
    {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

            } else {

                // No explanation needed, we can request the permission.

                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }else{
            permissionGranted = true;
        }
    }



    void defaultParameters()
    {
        //Mods

        setParameterInterface(100,0.15f);
        setParameterInterface(101,0.00f);
        setParameterInterface(102,0.00f);
        setParameterInterface(103,0.00f);

        setParameterInterface(104,0.00f);
        setParameterInterface(105,0.15f);
        setParameterInterface(106,0.00f);
        setParameterInterface(107,0.00f);

        setParameterInterface(108,0.00f);
        setParameterInterface(109,0.00f);
        setParameterInterface(110,0.15f);
        setParameterInterface(111,0.00f);

        setParameterInterface(112,0.00f);
        setParameterInterface(113,0.00f);
        setParameterInterface(114,0.00f);
        setParameterInterface(115,0.15f);

        //Outputs
        setParameterInterface(116,1.0f);
        setParameterInterface(117,1.0f);
        setParameterInterface(118,1.0f);
        setParameterInterface(119,1.0f);

        //Pans
        setParameterInterface(120,0.5f);
        setParameterInterface(121,0.5f);
        setParameterInterface(122,0.5f);
        setParameterInterface(123,0.5f);

        //Semitones
        setParameterInterface(124,.25f);
        setParameterInterface(125,.25f);
        setParameterInterface(126,.50f);
        setParameterInterface(127,.50f);

        //Fine
        setParameterInterface(128,.50f);
        setParameterInterface(129,.60f);
        setParameterInterface(130,.50f);
        setParameterInterface(131,.60f);


        //ADSRs
        for(int i = 132 ; i < 148; i+=4)
        {
            setParameterInterface(i,0.80f);
            setParameterInterface(i+1,0.0f);
            setParameterInterface(i+2,1.0f);
            setParameterInterface(i+3,0.90f);
        }

        //master
        setParameterInterface(148,1.0f);

        //portamento
        setParameterInterface(149,0.f);

        //mono
        setParameterInterface(150,0.0f);

    }

    void pluckParameters()
    {
        //Mods

        setParameterInterface(100,0.00f);
        setParameterInterface(101,1.00f);
        setParameterInterface(102,0.00f);
        setParameterInterface(103,0.00f);

        setParameterInterface(104,0.00f);
        setParameterInterface(105,0.00f);
        setParameterInterface(106,0.00f);
        setParameterInterface(107,0.00f);

        setParameterInterface(108,0.00f);
        setParameterInterface(109,0.50f);
        setParameterInterface(110,0.00f);
        setParameterInterface(111,0.00f);

        setParameterInterface(112,0.00f);
        setParameterInterface(113,0.00f);
        setParameterInterface(114,0.00f);
        setParameterInterface(115,0.00f);

        //Outputs
        setParameterInterface(116,1.0f);
        setParameterInterface(117,0.0f);
        setParameterInterface(118,1.0f);
        setParameterInterface(119,0.0f);

        //Pans
        setParameterInterface(120,0.5f);
        setParameterInterface(121,0.5f);
        setParameterInterface(122,0.5f);
        setParameterInterface(123,0.5f);

        //Semitones
        setParameterInterface(124,.50f);
        setParameterInterface(125,1.0f);
        setParameterInterface(126,.50f);
        setParameterInterface(127,.50f);

        //Fine
        setParameterInterface(128,.50f);
        setParameterInterface(129,.50f);
        setParameterInterface(130,.50f);
        setParameterInterface(131,.50f);


        //ADSRs

        setParameterInterface(132,0.0f);
        setParameterInterface(133,0.50f);
        setParameterInterface(134,0.20f);
        setParameterInterface(135,0.9f);

        setParameterInterface(136,0.0f);
        setParameterInterface(137,0.05f);
        setParameterInterface(138,0.10f);
        setParameterInterface(139,0.9f);

        setParameterInterface(140,0.0f);
        setParameterInterface(141,0.50f);
        setParameterInterface(142,0.20f);
        setParameterInterface(143,0.9f);

        setParameterInterface(144,0.0f);
        setParameterInterface(145,0.0f);
        setParameterInterface(146,1.0f);
        setParameterInterface(147,0.0f);

        //master
        setParameterInterface(148,1.0f);

        //portamento
        setParameterInterface(149,0.f);

        //mono
        setParameterInterface(150,0.0f);

    }

     void toyPianoParameters()
    {
        //Mods

        setParameterInterface(100,0.065f);
        setParameterInterface(101,0.00f);
        setParameterInterface(102,0.00f);
        setParameterInterface(103,0.00f);

        setParameterInterface(104,0.00f);
        setParameterInterface(105,0.00f);
        setParameterInterface(106,0.00f);
        setParameterInterface(107,0.00f);

        setParameterInterface(108,0.10f);
        setParameterInterface(109,0.0735f);
        setParameterInterface(110,0.00f);
        setParameterInterface(111,0.00f);

        setParameterInterface(112,0.00f);
        setParameterInterface(113,0.00f);
        setParameterInterface(114,0.00f);
        setParameterInterface(115,0.00f);

        //Outputs
        setParameterInterface(116,0.0f);
        setParameterInterface(117,0.0f);
        setParameterInterface(118,1.0f);
        setParameterInterface(119,0.0f);

        //Pans
        setParameterInterface(120,0.5f);
        setParameterInterface(121,0.5f);
        setParameterInterface(122,0.5f);
        setParameterInterface(123,0.5f);

        //Semitones
        setParameterInterface(124,.50f);
        setParameterInterface(125,.75f);
        setParameterInterface(126,.50f);
        setParameterInterface(127,.50f);

        //Fine
        setParameterInterface(128,.5321f);
        setParameterInterface(129,.5134f);
        setParameterInterface(130,.50f);
        setParameterInterface(131,.50f);


        //ADSRs

        setParameterInterface(132,0.0f);
        setParameterInterface(133,0.75f);
        setParameterInterface(134,0.00f);
        setParameterInterface(135,0.70f);

        setParameterInterface(136,0.0f);
        setParameterInterface(137,0.50f);
        setParameterInterface(138,0.30f);
        setParameterInterface(139,0.70f);

        setParameterInterface(140,0.0f);
        setParameterInterface(141,0.75f);
        setParameterInterface(142,0.00f);
        setParameterInterface(143,0.70f);

        setParameterInterface(144,0.0f);
        setParameterInterface(145,0.0f);
        setParameterInterface(146,1.0f);
        setParameterInterface(147,0.0f);

        //master
        setParameterInterface(148,1.0f);

        //portamento
        setParameterInterface(149,0.f);

        //mono
        setParameterInterface(150,0.0f);

    }

    void initParameters()
    {
        //Mods

        setParameterInterface(100,0.00f);
        setParameterInterface(101,0.00f);
        setParameterInterface(102,0.00f);
        setParameterInterface(103,0.00f);

        setParameterInterface(104,0.00f);
        setParameterInterface(105,0.00f);
        setParameterInterface(106,0.00f);
        setParameterInterface(107,0.00f);

        setParameterInterface(108,0.00f);
        setParameterInterface(109,0.00f);
        setParameterInterface(110,0.00f);
        setParameterInterface(111,0.00f);

        setParameterInterface(112,0.00f);
        setParameterInterface(113,0.00f);
        setParameterInterface(114,0.00f);
        setParameterInterface(115,0.00f);

        //Outputs
        setParameterInterface(116,1.0f);
        setParameterInterface(117,0.0f);
        setParameterInterface(118,0.0f);
        setParameterInterface(119,0.0f);

        //Pans
        setParameterInterface(120,0.5f);
        setParameterInterface(121,0.5f);
        setParameterInterface(122,0.5f);
        setParameterInterface(123,0.5f);

        //Semitones
        setParameterInterface(124,.50f);
        setParameterInterface(125,.50f);
        setParameterInterface(126,.50f);
        setParameterInterface(127,.50f);

        //Fine
        setParameterInterface(128,.50f);
        setParameterInterface(129,.50f);
        setParameterInterface(130,.50f);
        setParameterInterface(131,.50f);


        //ADSRs
        for(int i = 132 ; i < 148; i+=4)
        {
            setParameterInterface(i,0.0f);
            setParameterInterface(i+1,0.0f);
            setParameterInterface(i+2,1.0f);
            setParameterInterface(i+3,0.0f);
        }

        //master
        setParameterInterface(148,1.0f);

        //portamento
        setParameterInterface(149,0.f);

        //mono
        setParameterInterface(150,0.0f);

    }

     void stopRecordJava()
    {
        if (isRecording) {

            Toast.makeText(MainActivity.this,"Now stopped.", Toast.LENGTH_SHORT).show();
            setStopped();
            short[] recordedData = getRecordedData();
            try {
                Date date = new Date();
                String filepath = Environment.getExternalStorageDirectory().getAbsolutePath();
                String str = "/SynthesizerApp/SA_" + date.getTime() + ".wav";
                wavFile = new File(filepath + str);
                FileOutputStream outputStream = new FileOutputStream(wavFile);
                PCMtoFile(outputStream, recordedData, sampleRate, 2, 16);

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (wavDirectory != null){
                fileAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, wavDirectory.list());
                fileSpinner.setAdapter(fileAdapter);
                playId = wavDirectory.list().length-1;
            }

            isRecording = false;
        }
    }

     void setParameterInterface(int id, float value)
    {
        switch (id)
        {
            case 100:
                knobViews[0].setValue(value);
                setParameter(id,value);
                break;
            case 101:
                knobViews[1].setValue(value);
                setParameter(id,value);
                break;
            case 102:
                knobViews[2].setValue(value);
                setParameter(id,value);
                break;
            case 103:
                knobViews[3].setValue(value);
                setParameter(id,value);
                break;
            case 104:
                knobViews[4].setValue(value);
                setParameter(id,value);
                break;
            case 105:
                knobViews[5].setValue(value);
                setParameter(id,value);
                break;
            case 106:
                knobViews[6].setValue(value);
                setParameter(id,value);
                break;
            case 107:
                knobViews[7].setValue(value);
                setParameter(id,value);
                break;
            case 108:
                knobViews[8].setValue(value);
                setParameter(id,value);
                break;
            case 109:
                knobViews[9].setValue(value);
                setParameter(id,value);
                break;
            case 110:
                knobViews[10].setValue(value);
                setParameter(id,value);
                break;
            case 111:
                knobViews[11].setValue(value);
                setParameter(id,value);
                break;
            case 112:
                knobViews[12].setValue(value);
                setParameter(id, value);
                break;
            case 113:
                knobViews[13].setValue(value);
                setParameter(id, value);
                break;
            case 114:
                knobViews[14].setValue(value);
                setParameter(id, value);
                break;
            case 115:
                knobViews[15].setValue(value);
                setParameter(id, value);
                break;
            case 116:
                knobViews[16].setValue(value);
                setParameter(id, value);
                break;
            case 117:
                knobViews[17].setValue(value);
                setParameter(id, value);
                break;
            case 118:
                knobViews[18].setValue(value);
                setParameter(id, value);
                break;
            case 119:
                knobViews[19].setValue(value);
                setParameter(id, value);
                break;
            case 120:
                knobViews[20].setValue(value);
                setParameter(id, value);
                break;
            case 121:
                knobViews[21].setValue(value);
                setParameter(id, value);
                break;
            case 122:
                knobViews[22].setValue(value);
                setParameter(id, value);
                break;
            case 123:
                knobViews[23].setValue(value);
                setParameter(id, value);
                break;
            case 124:
                knobViews[24].setValue(value);
                setParameter(id, value);
                break;
            case 125:
                knobViews[25].setValue(value);
                setParameter(id, value);
                break;
            case 126:
                knobViews[26].setValue(value);
                setParameter(id, value);
                break;
            case 127:
                knobViews[27].setValue(value);
                setParameter(id, value);
                break;
            case 128:
                knobViews[28].setValue(value);
                setParameter(id, value);
                break;
            case 129:
                knobViews[29].setValue(value);
                setParameter(id, value);
                break;
            case 130:
                knobViews[30].setValue(value);
                setParameter(id, value);
                break;
            case 131:
                knobViews[31].setValue(value);
                setParameter(id, value);
                break;
            case 132:
                seekBars[0].setValue(value);
                setParameter(id,value);
                break;
            case 133:
                seekBars[1].setValue(value);
                setParameter(id,value);
                break;
            case 134:
                seekBars[2].setValue(value);
                setParameter(id,value);
                break;
            case 135:
                seekBars[3].setValue(value);
                setParameter(id,value);
                break;
            case 136:
                seekBars[4].setValue(value);
                setParameter(id,value);
                break;
            case 137:
                seekBars[5].setValue(value);
                setParameter(id,value);
                break;
            case 138:
                seekBars[6].setValue(value);
                setParameter(id,value);
                break;
            case 139:
                seekBars[7].setValue(value);
                setParameter(id,value);
                break;
            case 140:
                seekBars[8].setValue(value);
                setParameter(id,value);
                break;
            case 141:
                seekBars[9].setValue(value);
                setParameter(id,value);
                break;
            case 142:
                seekBars[10].setValue(value);
                setParameter(id,value);
                break;
            case 143:
                seekBars[11].setValue(value);
                setParameter(id,value);
                break;
            case 144:
                seekBars[12].setValue(value);
                setParameter(id,value);
                break;
            case 145:
                seekBars[13].setValue(value);
                setParameter(id,value);
                break;
            case 146:
                seekBars[14].setValue(value);
                setParameter(id,value);
                break;
            case 147:
                seekBars[15].setValue(value);
                setParameter(id,value);
                break;
            case 148:
                seekBars[16].setValue(value);
                setParameter(id,value);
                break;
            case 149:
                seekBars[17].setValue(value);
                setParameter(id,value);
                break;
            case 150:
                seekBars[18].setValue(value);
                setParameter(id,value);
                break;
        }
    }


    void setupSpinners()
    {
        presetList = new ArrayList<String>();
        presetSpinner = (Spinner) findViewById(R.id.spinner);
        presetList.add("Default");
        presetList.add("Toy Piano");
        presetList.add("Pluck");
        presetList.add("Init");
        ArrayList<String> presetNames = pdh.getPresetNames();
        for (int i = 0; i < presetNames.size(); i++) {
            presetList.add(presetNames.get(i));
        }

        final ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, presetList);
        presetSpinner.setAdapter(adapter);

        presetSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch ((int) id) {
                    case 0:
                        defaultParameters();
                        break;
                    case 1:
                        toyPianoParameters();
                        break;
                    case 2:
                        pluckParameters();
                        break;
                    case 3:
                        initParameters();
                        break;
                    default:
                        float v[] = pdh.getPresetValues(presetList.get((int) id));
                        for (int i = 0; i < 52; i++) {
                            setParameterInterface(100 + i, v[i]);
                        }
                        break;
                }
            }


            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });




        if (wavDirectory != null){
            fileSpinner = (Spinner) findViewById(R.id.file_spinner);
            fileAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, wavDirectory.list());
            fileSpinner.setAdapter(fileAdapter);
            fileSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    playId = (int) (id);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    void setupMenuButtons()
    {

        record.setOnClickListener(new View.OnClickListener() {
            int c = 0;

            @Override
            public void onClick(View v) {
                checkPermissionForWrite();

                if (permissionGranted) {
                    c ^= 1;
                    if (c == 1) {
                        if (!isRecording) {

                            Toast.makeText(MainActivity.this,"Now recording.", Toast.LENGTH_SHORT).show();
                            setRecord();
                            isRecording = true;
                        }
                    } else {
                        stopRecordJava();
                    }
                } else {

                    Toast.makeText(MainActivity.this,"Check permissions.", Toast.LENGTH_SHORT).show();

                }
            }
        });


        if(wavDirectory != null) {
            play.setOnClickListener(new View.OnClickListener() {
                int c = 0;

                @Override
                public void onClick(View v) {

                    checkPermissionForWrite();
                    if (permissionGranted) {
                        c ^= 1;
                        if (c == 1) {
                            stopRecordJava();
                            if (wavDirectory.list().length == 0) {

                                Toast.makeText(MainActivity.this,"Queue empty.", Toast.LENGTH_SHORT).show();

                                c ^= 1;
                                return;
                            }

                            Toast.makeText(MainActivity.this,"Now playing.", Toast.LENGTH_SHORT).show();
                            mp = MediaPlayer.create(MainActivity.this, parse(wavDirectoryPath + wavDirectory.list()[playId]));
                            mp.setLooping(true);
                            mp.start();
                            isPlaying = true;
                        } else {

                            Toast.makeText(MainActivity.this,"Now stopping.", Toast.LENGTH_SHORT).show();
                            mp.setLooping(false);
                            mp.stop();
                            mp.release();
                            isPlaying = false;
                        }
                    }
                }
            });
        }
        else
        {
            Toast.makeText(MainActivity.this,"Check permissions.", Toast.LENGTH_SHORT).show();
        }

        preset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Date date = new Date();
                String presetName = "CP_" + date.getTime();
                float r[] = new float[52];
                for (int i = 0; i < 52; i++) {
                    r[i] = getParameter(i + 100);
                }
                pdh.storePresetValue(presetName, r);
                presetList.add(presetName);
            }
        });
    }

     void setupMidi()
    {
        keyboardView = (KeyboardView) findViewById(R.id.kv);

        keyboardView.setMidiListener(new MidiListener() {
            Semaphore mutex = new Semaphore(1);

            @Override
            public void onNoteOff(int channel, int note, int velocity) {
                try {
                    mutex.acquire();
                    try {

                        setMidiMessage(0x80, note, velocity);
                    } finally {
                        mutex.release();
                    }
                } catch (InterruptedException e) {

                }
            }

            @Override
            public void onNoteOn(int channel, int note, int velocity) {
                try {
                    mutex.acquire();
                    try {

                        setMidiMessage(0x90, note, velocity);
                    } finally {
                        mutex.release();
                    }
                } catch (InterruptedException e) {

                }
            }
        });

        scrollStripView = (ScrollStripView) findViewById(R.id.ssv);
        scrollStripView.bindKeyboard(keyboardView);
    }

    void xorView()
    {
        xView = 1;
        findViewById(R.id.change_view).setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick (View v){
                if (xView == 1) {
                    findViewById(R.id.keyboard_layout).setVisibility(View.INVISIBLE);
                    findViewById(R.id.parameter_layout).setVisibility(View.VISIBLE);

                    switch (lastView) {
                        case 0:
                            findViewById(R.id.mstr_layout).setVisibility(View.VISIBLE);
                            findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);
                            break;
                        case 1:
                            findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_0_layout).setVisibility(View.VISIBLE);
                            findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);
                            break;
                        case 2:
                            findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_1_layout).setVisibility(View.VISIBLE);
                            findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);
                            break;
                        case 3:
                            findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_2_layout).setVisibility(View.VISIBLE);
                            findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);
                            break;
                        case 4:
                            findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                            findViewById(R.id.adsr_3_layout).setVisibility(View.VISIBLE);
                            break;
                    }

                } else {

                    findViewById(R.id.keyboard_layout).setVisibility(View.VISIBLE);
                    findViewById(R.id.parameter_layout).setVisibility(View.INVISIBLE);
                }
                xView ^= 1;

            }
        }

        );
    }
    void tabbedView()
    {
        sub_views[0].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.mstr_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);
                lastView = 0;


            }

        });
        sub_views[1].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_0_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);

                lastView = 1;

            }


        });

        sub_views[2].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_1_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);

                lastView = 2;

            }

        });
        sub_views[3].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_2_layout).setVisibility(View.VISIBLE);
                findViewById(R.id.adsr_3_layout).setVisibility(View.INVISIBLE);
                lastView = 3;
            }

        });
        sub_views[4].setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                findViewById(R.id.mstr_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_0_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_1_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_2_layout).setVisibility(View.INVISIBLE);
                findViewById(R.id.adsr_3_layout).setVisibility(View.VISIBLE);
                lastView = 4;


            }

        });
    }

    void setupKnobs()
    {
        //KNOBS//
        knobViews = new KnobView[32];
        knobViews[0] = (KnobView) findViewById(R.id.mod_0_0);
        knobViews[1] = (KnobView) findViewById(R.id.mod_0_1);
        knobViews[2] = (KnobView) findViewById(R.id.mod_0_2);
        knobViews[3] = (KnobView) findViewById(R.id.mod_0_3);
        knobViews[4] = (KnobView) findViewById(R.id.mod_1_0);
        knobViews[5] = (KnobView) findViewById(R.id.mod_1_1);
        knobViews[6] = (KnobView) findViewById(R.id.mod_1_2);
        knobViews[7] = (KnobView) findViewById(R.id.mod_1_3);
        knobViews[8] = (KnobView) findViewById(R.id.mod_2_0);
        knobViews[9] = (KnobView) findViewById(R.id.mod_2_1);
        knobViews[10] = (KnobView) findViewById(R.id.mod_2_2);
        knobViews[11] = (KnobView) findViewById(R.id.mod_2_3);
        knobViews[12] = (KnobView) findViewById(R.id.mod_3_0);
        knobViews[13] = (KnobView) findViewById(R.id.mod_3_1);
        knobViews[14] = (KnobView) findViewById(R.id.mod_3_2);
        knobViews[15] = (KnobView) findViewById(R.id.mod_3_3);
        knobViews[16] = (KnobView) findViewById(R.id.out_0);
        knobViews[17] = (KnobView) findViewById(R.id.out_1);
        knobViews[18] = (KnobView) findViewById(R.id.out_2);
        knobViews[19] = (KnobView) findViewById(R.id.out_3);
        knobViews[20] = (KnobView) findViewById(R.id.pan_0);
        knobViews[21] = (KnobView) findViewById(R.id.pan_1);
        knobViews[22] = (KnobView) findViewById(R.id.pan_2);
        knobViews[23] = (KnobView) findViewById(R.id.pan_3);

        
        knobViews[24] = (KnobView) findViewById(R.id.semitone_0);
        knobViews[25] = (KnobView) findViewById(R.id.semitone_1);
        knobViews[26] = (KnobView) findViewById(R.id.semitone_2);
        knobViews[27] = (KnobView) findViewById(R.id.semitone_3);

        knobViews[28] = (KnobView) findViewById(R.id.fine_0);
        knobViews[29] = (KnobView) findViewById(R.id.fine_1);
        knobViews[30] = (KnobView) findViewById(R.id.fine_2);
        knobViews[31] = (KnobView) findViewById(R.id.fine_3);


        KnobListener knobListener = new KnobListener() {

            @Override
            public void onKnobChanged(KnobView knobView, double newValue) {
                int id = knobView.getId();
                setParameter(id,(float) newValue);
                setTextViewReadOut(id,(float)newValue);
            }

        };
        for(int i = 0 ; i < 32; i++){
            knobViews[i].setId(parameterCounter++);
            knobViews[i].setKnobListener(knobListener);
        }


    }
    
    void setupSeekBarsAndSwitch()
    {
        seekBars = new CustomSeekBar[19];
        seekBars[0] = (CustomSeekBar) findViewById(R.id.attack_0);
        seekBars[1] = (CustomSeekBar) findViewById(R.id.decay_0);
        seekBars[2] = (CustomSeekBar) findViewById(R.id.sustain_0);
        seekBars[3] = (CustomSeekBar) findViewById(R.id.release_0);

        seekBars[4] = (CustomSeekBar) findViewById(R.id.attack_1);
        seekBars[5] = (CustomSeekBar) findViewById(R.id.decay_1);
        seekBars[6] = (CustomSeekBar) findViewById(R.id.sustain_1);
        seekBars[7] = (CustomSeekBar) findViewById(R.id.release_1);

        seekBars[8] = (CustomSeekBar) findViewById(R.id.attack_2);
        seekBars[9] = (CustomSeekBar) findViewById(R.id.decay_2);
        seekBars[10] = (CustomSeekBar) findViewById(R.id.sustain_2);
        seekBars[11] = (CustomSeekBar) findViewById(R.id.release_2);

        seekBars[12] = (CustomSeekBar) findViewById(R.id.attack_3);
        seekBars[13] = (CustomSeekBar) findViewById(R.id.decay_3);
        seekBars[14] = (CustomSeekBar) findViewById(R.id.sustain_3);
        seekBars[15] = (CustomSeekBar) findViewById(R.id.release_3);

        seekBars[16] = (CustomSeekBar) findViewById(R.id.master);
        seekBars[17] = (CustomSeekBar) findViewById(R.id.port);
        seekBars[18] = (CustomSeekBar) findViewById(R.id.mono);



        CustomSeekBarListener listener  = new CustomSeekBarListener() {
            @Override
            public void OnCustomSeekBarChanged(View view, float value) {
                int id = view.getId();
                setParameter(id,(float) value);
                setTextViewReadOut(id,value);
            }
        };

        for(int i = 0; i < 19 ; i ++)
        {
            seekBars[i].setId(parameterCounter++);
            seekBars[i].setListener(listener);
        }
        seekBars[18].setActAsSwitch(true);

    }

    void setTextViewReadOut(int id, float value)
    {
        String str = "Read Out";
        Float v;
        Integer w;
        if(id>=100 && id<=115)
        {
            value *= 3.162277660168379f;
        }

        switch (id)
        {
            case 100:
                str = "mod 0->0" + getValuedB(value);
                break;
            case 101:
                str = "mod 1->0" + getValuedB(value);
                break;
            case 102:
                str = "mod 2->0" + getValuedB(value);
                break;
            case 103:
                str = "mod 3->0" + getValuedB(value);
                break;
            case 104:
                str = "mod 0->1" + getValuedB(value);
                break;
            case 105:
                str = "mod 1->1" + getValuedB(value);
                break;
            case 106:
                str = "mod 2->1" + getValuedB(value);
                break;
            case 107:
                str = "mod 3->1" + getValuedB(value);
                break;
            case 108:
                str = "mod 0->2" + getValuedB(value);
                break;
            case 109:
                str = "mod 1->2" + getValuedB(value);
                break;
            case 110:
                str = "mod 2->2" + getValuedB(value);
                break;
            case 111:
                str = "mod 3->2" + getValuedB(value);
                break;
            case 112:
                str = "mod 0->3" + getValuedB(value);
                break;
            case 113:
                str = "mod 1->3" + getValuedB(value);
                break;
            case 114:
                str = "mod 2->3" + getValuedB(value);
                break;
            case 115:
                str = "mod 3->0" + getValuedB(value);
                break;
            case 116:
                str = "osc 0" + getValuedB(value);
                break;
            case 117:
                str = "osc 1" + getValuedB(value);
                break;
            case 118:
                str = "osc 2" + getValuedB(value);
                break;
            case 119:
                str = "osc 3" + getValuedB(value);
                break;
            case 120:
                v = -.5f + value;
                str = "pan 0 = "+v.toString();
                break;
            case 121:
                v = -.5f + value;
                str = "pan 1 = "+v.toString();
                break;
            case 122:
                v = -.5f + value;
                str = "pan 2 = "+v.toString();
                break;
            case 123:
                v = -.5f + value;
                str = "pan 3 = "+v.toString();
                break;
            case 124:
                v = (float)Math.floor((-24+value*48.f));
                str = "semi 0 = "+v.toString();
                break;
            case 125:
                v = (float)Math.floor((-24+value*48.f));
                str = "semi 1 = "+v.toString();
                break;
            case 126:
                v = (float)Math.floor((-24+value*48.f));
                str = "semi 2 = "+v.toString();
                break;
            case 127:
                v = (float)Math.floor((-24+value*48.f));
                str = "semi 3 = "+v.toString();
                break;
            case 128:
                v = -100.f+value*200.f;
                str = "fine 0 = "+v.toString()+" cents";
                break;
            case 129:
                v = -100.f+value*200.f;
                str = "fine 1 = "+v.toString()+" cents";
                break;
            case 130:
                v = -100.f+value*200.f;
                str = "fine 2 = "+v.toString()+" cents";
                break;
            case 131:
                v = -100.f+value*200.f;
                str = "fine 3 = "+v.toString()+" cents";
                break;
            case 132:
                str = "attack 0"+getTimeSeconds(10.f,.001f,value);
                break;
            case 133:
                str = "decay 0"+getTimeSeconds(10.f,.001f,value);
                break;
            case 134:
                str = "sustain 0"+getValuedB(value);
                break;
            case 135:
                str = "release 0"+getTimeSeconds(10.f,.001f,value);
                break;
            case 136:
                str = "attack 1"+getTimeSeconds(10.f,.001f,value);
                break;
            case 137:
                str = "decay 1"+getTimeSeconds(10.f,.001f,value);
                break;
            case 138:
                str = "sustain 1"+getValuedB(value);
                break;
            case 139:
                str = "release 1"+getTimeSeconds(10.f,.001f,value);
                break;
            case 140:
                str = "attack 2"+getTimeSeconds(10.f,.001f,value);
                break;
            case 141:
                str = "decay 2"+getTimeSeconds(10.f,.001f,value);
                break;
            case 142:
                str = "sustain 2"+getValuedB(value);
                break;
            case 143:
                str = "release 2"+getTimeSeconds(10.f,.001f,value);
                break;
            case 144:
                str = "attack 3"+getTimeSeconds(10.f,.001f,value);
                break;
            case 145:
                str = "decay 3"+getTimeSeconds(10.f,.001f,value);
                break;
            case 146:
                str = "sustain 3"+getValuedB(value);
                break;
            case 147:
                str = "release 3"+getTimeSeconds(10.f,.001f,value);
                break;
            case 148:
                str = "master"+getValuedB(value);
                break;
            case 149:
                v = value;
                str = "portamento = "+v.toString();
                break;
            case 150:
                if(value>=1.0f)
                {
                    str = "mono on";
                }else{
                    str = "mono off";
                }
                break;
            default:
                str = "Read Out";
                break;
        }

        TextView read = (TextView) findViewById(R.id.read_out);
        read.setText(str);
    }

     void PCMtoFile(FileOutputStream os, short[] pcmdata, int srate, int channel, int format) throws IOException {
        byte[] header = new byte[44];
        byte[] data = get16BitPcm(pcmdata);

        long totalDataLen = data.length + 36;
        long bitrate = srate * channel * format;

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
        header[16] = (byte) format;
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;
        header[21] = 0;
        header[22] = (byte) channel;
        header[23] = 0;
        header[24] = (byte) (srate & 0xff);
        header[25] = (byte) ((srate >> 8) & 0xff);
        header[26] = (byte) ((srate >> 16) & 0xff);
        header[27] = (byte) ((srate >> 24) & 0xff);
        header[28] = (byte) ((bitrate / 8) & 0xff);
        header[29] = (byte) (((bitrate / 8) >> 8) & 0xff);
        header[30] = (byte) (((bitrate / 8) >> 16) & 0xff);
        header[31] = (byte) (((bitrate / 8) >> 24) & 0xff);
        header[32] = (byte) ((channel * format) / 8);
        header[33] = 0;
        header[34] = 16;
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (data.length  & 0xff);
        header[41] = (byte) ((data.length >> 8) & 0xff);
        header[42] = (byte) ((data.length >> 16) & 0xff);
        header[43] = (byte) ((data.length >> 24) & 0xff);

        os.write(header, 0, 44);
        os.write(data);
        os.close();
    }

    byte[] get16BitPcm(short[] data) {
        byte[] resultData = new byte[2 * data.length];
        int iter = 0;
        for (double sample : data) {
            short maxSample = (short)((sample));
            resultData[iter++] = (byte)(maxSample & 0x00ff);
            resultData[iter++] = (byte)((maxSample & 0xff00) >>> 8);
        }
        return resultData;
    }

    String getValuedB(float value)
    {
        if(value > 0.0f) {
            Float string = (float) Math.log10(value) * 20.0f;
            return " = "+string.toString()+ " dB";
        }else{
            return " = -inf dB";
        }
    }



    String getTimeSeconds(float mMax,float mMin, float mParam)
    {
        float logmax = (float) Math.log10( mMax );
        float logmin = (float)Math.log10( mMin );
        float logdata = (mParam * (logmax-logmin)) + logmin;
        float  mData = (float)Math.pow( 10.0f, logdata );

        if (mData < mMin)
        {
            mData = mMin;
        }

        if (mData > mMax)
        {
            mData = mMax;
        }
        Float v =  mData;

        return " "+v.toString()+" = seconds";
    }



    public native  void setParameter(int id, float value);
    public native float getParameter(int id);
    public native void setMidiMessage(int message, int note, int vel);
    public native void startProcess(int sampleRate, int frames);
    public native void stopProcess();
    public native void setRecord();
    public native void setStopped();
    public native short [] getRecordedData();


    static {
        try {
           System.loadLibrary("native-synthesizer-thread");
            System.out.println("LIBRARY LOADED");
        } catch (UnsatisfiedLinkError e) {
            System.err.println("native code library failed to load.\n" + e);
            System.exit(1);
        }
    }


}