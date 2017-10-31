/*Jarvis Version 1.1
Keyword Detection- PocketSphinx:
    import files into app folder
    change gradle app module script
    add record audio permission into android manifest (and into Activity)

GoogleSpeechRec:
    add internet permission into android manifest
 */
package com.example.jarvisv11;

import android.Manifest;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

//PocketSphinx imports
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity implements RecognitionListener, TextToSpeech.OnInitListener, android.speech.RecognitionListener{

    //VARIABLES USED

    //Layout Variables
    private Button start_button;

    // Used to handle permission request
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_RECORD_SEND_SMS = 2;

    //PocketSphinx Variables
    //KeySearch name
    private final String KWS = "wakeup";
    //KeySearch phrase
    private final String KEYPHRASE = "ok jarvis";
    private SpeechRecognizer sphinxrec;

    //Google SpeechRec Variables
    private android.speech.SpeechRecognizer googlerec;
    private Intent speechIntent;

    //SMS Variables
    private SmsManager sms;

    //TTS Variables
    private TextToSpeech tts;


    //METHOD RUN ON START
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Checks for AudioRecord Permissions
        int permissionCheckAudio = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        int permissionCheckSMS = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS);
        if (permissionCheckAudio != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        if (permissionCheckSMS != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_RECORD_SEND_SMS);
            return;
        }

        //ALL SETUPS
        //Sets up Text to Speech
        tts = new TextToSpeech(this, this);
        //intialize GoogleSpeechRec
        setupGoogleRec();
        //Starts PocketSphinx Setup and Keyword Search
        runSphinxSetup();

        //Launch Jarvis
        start_button = (Button)findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getApplicationContext(), "Jarvis Launched!", Toast.LENGTH_LONG).show();
                restartKeySearch();
            }
        });
    }


    //PERMISSIONS
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    runSphinxSetup();
                } else {
                    finish();
                }
            case PERMISSIONS_REQUEST_RECORD_SEND_SMS:
                if (grantResults.length > 1 && grantResults[1] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    finish();
                }
        }
    }

    @Override
    /* Method run on Destroy:
     *  Checks to make sure every resource is closed
     *  Resources:
     *      TexttoSpeech (tts)
     *      PocketSphinx (sphinxrec)
     *      GoogleRecognition (googlerec)
     */
    public void onDestroy() {
        super.onDestroy();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }

        if (sphinxrec != null) {
            sphinxrec.cancel();
            sphinxrec.shutdown();
        }

        if (googlerec != null) {
            googlerec.cancel();
            googlerec.destroy();
        }
    }





    /*POCKETSPHINX SECTION:
     *  sphinxrec.startListening(): start search
     *  sphinxrec.cancel(): stop search with no results
     *  sphinxrec.stop(): stop search with results
     *  sphinxrec.shutdown(): destroys PocketSphinx object*/

    /* POCKETSPHINX SETUP METHOD:
     *  Starts an asynchronous method
     *  Uses Main Activity's implementation of Sphinx's RecognitionListener
     *  Starts a keyword search for wakeup call
     *  Resources:
     *      Default Acoustic Model
     *      Default English Dictionary Model
     *  Keywords:
     *      wakeup: ok jarvis   */

    private void runSphinxSetup(){
        //Sets up PocketSphinx
        new AsyncTask<Void, Void, Exception>(){

            @Override
            protected Exception doInBackground(Void... params) {
                try{
                    //Brings in PocketSphinx resources
                    Assets assets = new Assets(MainActivity.this);
                    File assetDir = assets.syncAssets();
                    SpeechRecognizerSetup sphinxSetup = SpeechRecognizerSetup.defaultSetup();
                    sphinxSetup.setAcousticModel(new File(assetDir, "en-us-ptm"));
                    sphinxSetup.setDictionary(new File(assetDir, "cmudict-en-us.dict"));

                    // Threshold to tune for keyphrase to balance between false alarms and misses
                    sphinxSetup.setKeywordThreshold(1e-40f);
                    sphinxrec = sphinxSetup.getRecognizer();
                    sphinxrec.addKeyphraseSearch(KWS,KEYPHRASE);
                    sphinxrec.addListener(MainActivity.this);

                }catch(IOException e){
                    return e;
                }
                return null;
            }
            @Override
            //Starts Keyword Search or displays error
            protected void onPostExecute(Exception result) {
                if (result != null) {
                    Toast.makeText(MainActivity.this, result.getMessage(),Toast.LENGTH_LONG).show();
                } else {
                    restartKeySearch();
                }
            }
        }.execute();
    }

    //restart the keyword search
    private void restartKeySearch(){
        sphinxrec.cancel();
        sphinxrec.startListening(KWS);
    }

    //IMPLEMENTATION OF POCKETSPHINX RECOGNITION LISTENER

    /* Checks to see if keyword is spoken each time:
     *  Starts GoogleRecognition if keyword is spoken */

    @Override
    public void onPartialResult(Hypothesis hypothesis) {
        if(hypothesis == null){
            return;
        }
        if(hypothesis.getHypstr().equals(KEYPHRASE)){
            tts.speak("Yes sir.", TextToSpeech.QUEUE_FLUSH, null);
            Toast.makeText(this, "You just spoke the keyword!", Toast.LENGTH_LONG).show();
            googlerec.startListening(speechIntent);
            sphinxrec.cancel();
        }
    }

    //Displays errors
    @Override
    public void onError(Exception error) {
        Toast.makeText(this, "PocketSphinx Error: " + error.getMessage(),Toast.LENGTH_LONG).show();
    }

    //Methods not Overridden
    @Override
    public void onResult(Hypothesis hypothesis) {}
    @Override
    public void onTimeout() {}





    /*TEXTTOSPEECH SECTION:
     *  tts.speak(): speaks
     *  tts.stop(): stops speaking
     *  tts.shutdown(): destroys TexttoSpeech object*/

    /*  TexttoSpeech Initialization Method:
     *  Resources:
     *      UK Locale   */

    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.UK);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Toast.makeText(this, "Language for Text to Speech not supported!", Toast.LENGTH_LONG).show();
            }

        } else {
            Toast.makeText(this, "Error Initializing Text to Speech!", Toast.LENGTH_LONG).show();
        }

    }





    /* GOOGLE SPEECH RECOGNITION SECTION:
     *  googlerec.startListening(): listens for speech
     *  googlerec.cancel(): stops listenign with no results
     *  googlerec.destroy(): destroys GoogleRecognition object
     */

    /* GoogleRecognition Setup Method:
     *  Uses MainActivity's implementation of Google's RecognitionListener
     *  Creates Intent to use Google Servers
     *  Resources:
     *      Default English Language Model
     *      Free Form Language Model
     */

    private void setupGoogleRec() {
        googlerec = android.speech.SpeechRecognizer.createSpeechRecognizer(this);
        googlerec.setRecognitionListener(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    }

    //Restart GoogleRecognition
    public void restartGoogleRec(){
        googlerec.cancel();
        googlerec.startListening(speechIntent);
    }

    //Implementation of Google's RecognitionListener

    //Displays Errors and cancels recognition if error
    @Override
    public void onError(int i) {
        if (i == android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                || i == android.speech.SpeechRecognizer.ERROR_NO_MATCH){
            tts.speak("Repeat that.", TextToSpeech.QUEUE_FLUSH, null);
            restartGoogleRec();
        }
    }

    /* Catches speech and selects next step
     *  Possibilities:
     *      text Howard: Text Test
     *      mute: Mute Jarvis*/
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        tts.speak(matches.get(0), TextToSpeech.QUEUE_FLUSH, null);
        Toast.makeText(getApplicationContext(), matches.get(0), Toast.LENGTH_SHORT).show();

        //All possibilities
        if(matches.get(0).contains("text Howard")){
            textTest();
            sphinxrec.startListening(KWS);
        }else if(matches.get(0).contains("shut up")){
            muteJarvis();
        }else{
            sphinxrec.startListening(KWS);
        }
    }

    //Methods not Overridden
    @Override
    public void onReadyForSpeech(Bundle bundle) {}
    @Override
    public void onBeginningOfSpeech() {}
    @Override
    public void onRmsChanged(float v) {}
    @Override
    public void onBufferReceived(byte[] bytes) {}
    @Override
    public void onEndOfSpeech() {}
    @Override
    public void onPartialResults(Bundle bundle) {}
    @Override
    public void onEvent(int i, Bundle bundle) {}





    //ALL POSSIBLE ACTIONS

    //Text Test
    private void textTest(){
        //Get the SmsManager instance and send text message
        sms=SmsManager.getDefault();
        sms.sendTextMessage("8586105086", null, "<3 Jerry", null,null);

        Toast.makeText(getApplicationContext(), "Message Sent successfully!",
                Toast.LENGTH_LONG).show();
    }

    //Mute Jarvis
    private void muteJarvis(){
        sphinxrec.cancel();
        Toast.makeText(getApplicationContext(), "Muted!", Toast.LENGTH_LONG).show();
    }
}
