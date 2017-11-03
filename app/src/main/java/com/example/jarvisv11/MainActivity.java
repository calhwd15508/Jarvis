/*Jarvis Version 1.1
Keyword Detection- PocketSphinx:
    import files into app folder
    change gradle app module script
    add record audio permission into android manifest (and into Activity)

GoogleSpeechRec:
    add internet permission into android manifest
 */

/*
    POSSIBLE PHRASES:
        OK JARVIS: KEYWORD
        TEXT HOWARD: TEXTING TEST
        SHUT UP: MUTE JARVIS
        CALENDAR TEST: GOOGLE CALENDAR TEST
 */
package com.example.jarvisv11;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

//PocketSphinx imports
import edu.cmu.pocketsphinx.Assets;
import edu.cmu.pocketsphinx.Hypothesis;
import edu.cmu.pocketsphinx.RecognitionListener;
import edu.cmu.pocketsphinx.SpeechRecognizer;
import edu.cmu.pocketsphinx.SpeechRecognizerSetup;

public class MainActivity extends AppCompatActivity implements RecognitionListener, android.speech.RecognitionListener{

    //VARIABLES USED

    //Google Calendar Variables
    GoogleAccountCredential credential;
    private static final String[] SCOPES = { CalendarScopes.CALENDAR };
    private static final String PREF_ACCOUNT_NAME = "accountName"; //Preference Variable
    private static final int REQUEST_ACCOUNT_PICKER = 1000;

    //Layout Variables
    private Button start_button;

    // Used to handle permission request
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_RECORD_SEND_SMS = 2;
    private static final int PERMISSIONS_REQUEST_INTERNET = 3;
    private static final int PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE = 4;
    private static final int PERMISSIONS_REQUEST_ACCESS_GET_ACCOUNTS = 5;

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
    private SMS sms;

    //TTS Variable
    private TexttoSpeech tts;


    //METHOD RUN ON START
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Checks for AudioRecord Permissions
        int permissionCheckAudio = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        int permissionCheckSMS = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS);
        int permissionsCheckInternet = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.INTERNET);
        int permissionsCheckNetwork = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_NETWORK_STATE);
        int permissionsCheckAccount = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS);

        if (permissionCheckAudio != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        if (permissionCheckSMS != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_RECORD_SEND_SMS);
            return;
        }
        if (permissionsCheckInternet != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, PERMISSIONS_REQUEST_INTERNET);
            return;
        }
        if (permissionsCheckNetwork != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_NETWORK_STATE}, PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE);
            return;
        }
        if (permissionsCheckAccount != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, PERMISSIONS_REQUEST_ACCESS_GET_ACCOUNTS);
            return;
        }

        //ALL SETUPS
        //initialize Text to Speech
        tts = new TexttoSpeech(this, this);
        //initialize SMS
        sms = new SMS(this);
        //intialize GoogleSpeechRec
        setupGoogleRec();
        //initialize PocketSphinx Setup and Keyword Search
        runSphinxSetup();
        //Google Credentials
        credential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES))
                .setBackOff(new ExponentialBackOff());

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

    /*Handles Activity Results from External Intents
        Possibilities:
            REQUEST_ACCOUNT_PICKER: Selects Google Account for Calendar
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        credential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
        }
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
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    finish();
                }
            case PERMISSIONS_REQUEST_INTERNET:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    finish();
                }
            case PERMISSIONS_REQUEST_ACCESS_NETWORK_STATE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    finish();
                }
            case PERMISSIONS_REQUEST_ACCESS_GET_ACCOUNTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

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
        tts.destroy();
        if (sphinxrec != null) {
            sphinxrec.cancel();
            sphinxrec.shutdown();
        }

        if (googlerec != null) {
            googlerec.cancel();
            googlerec.destroy();
        }
    }

    public void displayToast(String message){
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
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
            tts.speak("Yes sir.");
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
            tts.speak("Repeat that.");
            restartGoogleRec();
        }
    }

    /* Catches speech and selects next step
     *  Possibilities:
     *      text Howard: Text Test
     *      shut up: Mute Jarvis
     *      calendar test: Google Calendar Test*/
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results
                .getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        tts.speak(matches.get(0));
        Toast.makeText(getApplicationContext(), matches.get(0), Toast.LENGTH_SHORT).show();

        //All possibilities
        String command = matches.get(0).toLowerCase();
        if(command.contains("text Howard")){
            textTest();
            sphinxrec.startListening(KWS);
        }else if(command.contains("shut up")){
            muteJarvis();
        }else if(command.contains("calendar test")){
            getResultsFromApi();
            sphinxrec.startListening(KWS);
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






    /* GOOGLE CALENDAR SECTION:
        isGooglePlayServicesAvailable(): Checks for Google Play Service
        chooseAccount(): Checks for saved account (starts an Intent with Result)
        isDeviceOnline(): Checks for Internet Connectivity
        makeRequest(): Makes request to Google Calendar Servers for Information
     */
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private void chooseAccount(){
        String accountName = getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
        if (accountName != null) {
            credential.setSelectedAccountName(accountName);
            getResultsFromApi();
        } else {
            // Start a dialog from which the user can choose an account
            tts.speak("Choose a Google Account.");
            startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
        }
    }

    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private List<Event> makeRequest(GoogleAccountCredential credential){
        return new AsyncTask<GoogleAccountCredential, Void, List<Event>>(){
            protected List<Event> doInBackground(GoogleAccountCredential... googleAccountCredentials) {
                try{
                    DateTime now = new DateTime(System.currentTimeMillis());
                    HttpTransport transport = AndroidHttp.newCompatibleTransport();
                    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                    com.google.api.services.calendar.Calendar service = new com.google.api.services.calendar.Calendar.Builder(
                            transport, jsonFactory, credential)
                            .setApplicationName("Jarvis")
                            .build();
                    Events events = service.events().list("primary")
                            .setMaxResults(1)
                            .setSingleEvents(true)
                            .setOrderBy("startTime")
                            .setTimeMin(now)
                            .execute();
                    List<String> eventStrings = new ArrayList<String>();
                    List<Event> items = events.getItems();
                    return items;
                }catch(Exception e){
                    Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    tts.speak("Error making Google Calendar Request!" + e.getMessage());
                }
                return null;
            }
        }.execute();
    }








    //ALL POSSIBLE ACTIONS

    //Text Test
    private void textTest(){
        //Get the SmsManager instance and send text message
        tts.speak("Texting Test");
        sms.sendText("8586105086", "Message sent successfully!");
    }

    //Mute Jarvis
    private void muteJarvis(){
        tts.speak("Muting");
        sphinxrec.cancel();

        Toast.makeText(getApplicationContext(), "Muted!", Toast.LENGTH_LONG).show();
    }

    //Google Calendar Test
    //Checks for Google Play Services, Account Availability, and Internet Connectivity
    private void getResultsFromApi() {
        tts.speak("Google Calendar Test");
        if (! isGooglePlayServicesAvailable()) {
            Toast.makeText(getApplicationContext(), "Google Play Services not available!", Toast.LENGTH_LONG).show();
            tts.speak("Google Play Services is not available!");
        } else if (credential.getSelectedAccountName() == null) {
            chooseAccount();
        } else if (! isDeviceOnline()) {
            Toast.makeText(getApplicationContext(), "No Internet Connect!", Toast.LENGTH_LONG).show();
            tts.speak("No Internet Connection!");
        } else {
            List<Event> events = makeRequest(credential);
            if(events.size()==0){
                tts.speak("No events on calendar!");
            }else{
                Event event = events.get(0);
                if(event.getStart().getDateTime() == null){
                    tts.speak(String.format("Next up on your schedule is %s.", events.get(0).getSummary()));
                }else {
                    tts.speak(String.format("Next up on your schedule is %s.", events.get(0).getSummary()));
                }
            }
        }
    }
}
