package com.example.jarvisv11;

/**
 * Created by howardzhang on 11/2/17.
 */

import android.content.Context;
import android.speech.tts.TextToSpeech;

import java.util.Locale;


public class TexttoSpeech implements TextToSpeech.OnInitListener{

    private TextToSpeech tts;
    private final int ttsWait = 100;
    private MainActivity GUI;

    public TexttoSpeech(MainActivity GUI, Context context){
        this.GUI = GUI;
        tts = new TextToSpeech(context, this);
    }

    //implementation of OnInitListener
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.UK);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                GUI.displayToast("Language for Text to Speech not supported!");
            }

        } else {
            GUI.displayToast("Error Initializing Text to Speech!");
        }

    }

    //speaks given dialogue "message" and waits until finish
    public void speak(String message){
        tts.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        while (tts.isSpeaking()) {
            try {
                Thread.sleep(ttsWait);
            } catch (InterruptedException e) {
                GUI.displayToast("InterruptedException Error!");
            }
        }
    }

    //unlocks resources
    public void destroy(){
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

}
