/* Requires:
    PocketSphinx Files
    Gradle App Changes
    Permissions: RECORD_AUDIO
 */

package com.example.jarvisv11;

import android.os.AsyncTask;

import java.io.File;
import java.io.IOException;

import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

/**
 * Created by howardzhang on 11/2/17.
 */

public class SphinxSpeechRecognizer implements RecognitionListener{
    //KeySearch name
    private final String KWS = "wakeup";
    //KeySearch phrase
    private final String KEYPHRASE = "ok violet";

    private SpeechRecognizer sphinxrec;
    private MainActivity GUI;
    private TexttoSpeech tts;
    private GoogleSpeechRecognizer googlerec;

    public SphinxSpeechRecognizer(MainActivity GUI, TexttoSpeech tts, GoogleSpeechRecognizer googlerec){
        this.googlerec = googlerec;
        this.GUI = GUI;
        this.tts = tts;
        runSphinxSetup();
    }

    /* POCKETSPHINX SETUP METHOD:
     *  Starts an asynchronous method
     *  Uses SphinxSpeechRecognizer's implementation of Sphinx's RecognitionListener
     *  Starts a keyword search for wakeup call
     *  Resources:
     *      Default Acoustic Model
     *      Default English Dictionary Model*/

    private void runSphinxSetup(){
        //Sets up PocketSphinx
        new AsyncTask<Void, Void, Exception>(){

            @Override
            protected Exception doInBackground(Void... params) {
                try{
                    //Brings in PocketSphinx resources
                    Assets assets = new Assets(GUI);
                    File assetDir = assets.syncAssets();
                    SpeechRecognizerSetup sphinxSetup = SpeechRecognizerSetup.defaultSetup();
                    sphinxSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    sphinxSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));

                    // Threshold to tune for keyphrase to balance between false alarms and misses
                    // Lower for lower false positive count
                    sphinxSetup.setKeywordThreshold(1e-30f);
                    sphinxrec = sphinxSetup.getRecognizer();
                    sphinxrec.addKeyphraseSearch(KWS,KEYPHRASE);
                    sphinxrec.addListener(SphinxSpeechRecognizer.this);

                }catch(IOException e){
                    return e;
                }
                return null;
            }
            @Override
            //Starts Keyword Search or displays error
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    GUI.displayToast("PocketSphinx Setup Error!" + result.getMessage());
                }
            }
        }.execute();
    }

    //Implementation of RecognitionListene
    /* Checks to see if keyword is spoken each time:
     *  Starts GoogleRecognition if keyword is spoken */
    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if(hypothesis == null){
            return;
        }
        if(hypothesis.getHypstr().equals(KEYPHRASE)){
            if(GUI.user == null){
                tts.speak("No active user. Please identify yourself.");
                restartKeySearch();
            }else {
                tts.speak("Yes?");
                googlerec.startListening();
                sphinxrec.cancel();
            }
        }
    }

    //displays error as toast
    @Override
    public void onError(Exception e) {
        GUI.displayToast("PocketSphinx Error: " + e.getMessage());
    }

    //unused implementation methods
    @Override
    public void onBeginningOfSpeech() {
    }
    @Override
    public void onEndOfSpeech() {
    }
    @Override
    public void onResult(Hypothesis hypothesis) {
    }
    @Override
    public void onTimeout() {
    }


    //restarts the keyword search
    public void restartKeySearch(){
        sphinxrec.cancel();
        sphinxrec.startListening(KWS);
    }

    //starts keyword search
    public void startListening(){
        sphinxrec.startListening(KWS);
    }

    //cancels keyword search
    public void cancel(){
        sphinxrec.cancel();
    }

    //unlocks resources
    public void destroy(){
        if (sphinxrec != null) {
            sphinxrec.cancel();
            sphinxrec.shutdown();
        }
    }
}
