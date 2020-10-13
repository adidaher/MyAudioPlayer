package com.example.myaudioplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;

import org.w3c.dom.Text;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.os.Environment.DIRECTORY_RINGTONES;

public class MainActivity extends AppCompatActivity {

    private TextView songpositionTextview;
    private TextView songDurationTextview;
    private  boolean issongPlaying = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }



    private static final String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE

    };
    private static final int REQUEST_PERMISSION = 12345;  //any number
    private static final int PERMISSION_COUNT = 1;


    /*************************** check if there is allow for permission ***************************/
    @SuppressLint("NewApi")
    private boolean arepermissiondenied() {

        for (int i = 0; i < PERMISSION_COUNT; i++) {
            if (checkSelfPermission(PERMISSIONS[i]) != PackageManager.PERMISSION_GRANTED) {
                return true;
            }
        }
        return false;
    }


    @SuppressLint("NewApi")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (arepermissiondenied()) {
            ((ActivityManager) (this.getSystemService(ACTIVITY_SERVICE))).clearApplicationUserData();
            recreate();
        } else {
            onResume();
        }
    }



    private boolean isMusicPlayerInit;
    private List<String> musicFilesList;

    /**************** fill in the musicfilelist mp3 song from the recieved path *******************/

    private void addMusicFilesFrom(String dirtPath){
        final File musicDir = new File(dirtPath);
        if(!musicDir.exists()){
            musicDir.mkdir();
            return;
        }
        final File[] files = musicDir.listFiles();
        for(File file : files){
            final String path = file.getAbsolutePath();
            Log.d("*****",path);
            if(path.endsWith(".mp3") || path.endsWith(".ogg")){  //if the file end with mp3 will add it
                musicFilesList.add(path);
            }
        }
    }
    /**********************fill in the music in this files "Music" and "download".. ***************/
     private  void fillMusicList(){
         musicFilesList.clear();
         addMusicFilesFrom(String.valueOf( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)));
         addMusicFilesFrom(String.valueOf( Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_NOTIFICATIONS)));
         addMusicFilesFrom(String.valueOf( getExternalFilesDir(DIRECTORY_RINGTONES)));
         addMusicFilesFrom("/system/media/audio/ringtones");


     }

    private  MediaPlayer mp;
    private int playMusicFile(String path){
         mp = new MediaPlayer();
        try{
            //set the media player to play the new path
            mp.setDataSource(path);
            mp.prepare();
            mp.start();
        }catch (Exception e){
            e.printStackTrace();
        }
        return  mp.getDuration();
    }

    private int songPosition;


    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && arepermissiondenied()) {

            requestPermissions(PERMISSIONS, REQUEST_PERMISSION);
            return;
        }
        if (!isMusicPlayerInit) {
            final ListView listview = findViewById(R.id.listview);
            final  TextAdapter textAdapter = new TextAdapter();
            /***** set the musicfile to be the listview item *****/
            musicFilesList = new ArrayList<>();
            fillMusicList();
            textAdapter.setData(musicFilesList);
            listview.setAdapter(textAdapter);

            final SeekBar seekbar = findViewById(R.id.seekBar);
            seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                int songProgess;
                @Override
                public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                    songProgess = i;

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    songPosition = songProgess;
                    mp.seekTo(songProgess);
                }
            });

            songpositionTextview = findViewById(R.id.currentPosition);
            songDurationTextview = findViewById(R.id.songDuration);
             final Button pauseButton = findViewById(R.id.pausebutton);
             final View playbackControls = findViewById(R.id.playBackButtons);

            pauseButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                   if( issongPlaying ){
                       mp.pause();
                    //   pauseButton.setText("Play");
                   }else {
                       mp.start();
                     //  pauseButton.setText("Pause");
                   }
                   issongPlaying = !issongPlaying;
                }
            });

            listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                   if(issongPlaying){
                        mp.pause();
                    }
                    final String musicfilepath = musicFilesList.get(i);
                    final int songDuraton = playMusicFile(musicfilepath);
                    seekbar.setMax(songDuraton);
                    seekbar.setVisibility(View.VISIBLE);
                    playbackControls.setVisibility(View.VISIBLE);
                    issongPlaying=true;
                    int minutes = (songDuraton  / 1000) / 60;
                    int seconds = (songDuraton  / 1000) % 60;
                    songDurationTextview.setText(String.format("%d:%d",minutes,seconds));
                        new Thread() {
                            public void run() {
                                songPosition = 0;
                                while ( songPosition < songDuraton) {
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    if (issongPlaying) {
                                        songPosition += 400;
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                seekbar.setProgress(songPosition);
                                                int minutes2 = (songPosition / 1000) / 60;
                                                int seconds2 = (songPosition / 1000) % 60;
                                                songpositionTextview.setText(String.format("%d:%d", minutes2, seconds2));

                                            }
                                        });

                                    }
                                }
                                mp.pause();
                                songPosition = 0;
                                 mp.seekTo(songPosition);
                           //     songpositionTextview.setText("0:00");
                             //   pauseButton.setText("play");
                                issongPlaying = false;
                                seekbar.setProgress(songPosition);
                                mp.reset();
                            }
                        }.start();
                }
            });
            isMusicPlayerInit = true;
        }
    }

    class TextAdapter extends BaseAdapter {
        private List<String> data = new ArrayList<String>();

        void setData(List<String> mdata) { //clear the previous data
            data.clear();
            data.addAll(mdata);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return data.size();
        }

        @Override
        public String getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().from(parent.getContext()).inflate(R.layout.item, parent, false);
                convertView.setTag(new ViewHolder((TextView) convertView.findViewById(R.id.myItem)));
            }

            ViewHolder holder = (ViewHolder) convertView.getTag();
            final String item = data.get(position);
            holder.info.setText(item.substring(item.lastIndexOf('/') + 1));
            return convertView;
        }

        class ViewHolder {
            TextView info;

            public ViewHolder(TextView Info) {
                this.info = Info;
            }
        }

    }
}