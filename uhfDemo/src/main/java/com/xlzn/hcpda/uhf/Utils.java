package com.xlzn.hcpda.uhf;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.util.Log;

public class Utils {

    private static SoundThrea soundThrea = null;
    private static SoundPool soundPool = null;
    private static int soundID = 0;
    private static Context context;

    public static void loadSoundPool(Context context1) {
        context = context1;
        try {
            soundPool = new SoundPool(10, AudioManager.STREAM_MUSIC, 10);
            soundID = soundPool.load(context, R.raw.dingdj5, 10);
        } catch (Exception e) {
            Log.d("Utils", " e=" + e.toString());
        }
        if (soundThrea == null) {
            soundThrea = new SoundThrea();
            soundThrea.start();
        }
    }

    public static void releaseSoundPool() {
        if (soundPool != null) {
            soundPool.release();
        }
        if (soundThrea != null) {
            soundThrea.isFlag = true;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            soundThrea = null;
        }
    }

    public static void play() {
        soundThrea.play = true;
    }


    static class SoundThrea extends Thread {
        boolean play = false;
        boolean isFlag = false;

        @Override
        public void run() {
            while (!isFlag) {
                if (soundThrea.play) {
                    soundThrea.play = false;

                    if (soundPool != null) {
//                          AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
//                          float audioMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//                          float audioCurrentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//                          float volume = audioCurrentVolume / audioMaxVolume;
//                          soundPool.play(soundID, volume, volume, 0, 0, 1f);
                        soundPool.play(soundID, 1, 1, 0, 0, 1f);

                    }
                } else {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }
    }


}
