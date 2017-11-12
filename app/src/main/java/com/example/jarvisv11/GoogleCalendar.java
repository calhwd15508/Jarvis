/* Requires:
    Gradle App Changes
    Permissions: INTERNET, ACCESS_NETWORK_STATE, GET_ACCOUNTS
 */

package com.example.jarvisv11;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.text.format.DateUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by howardzhang on 11/3/17.
 */

public class GoogleCalendar {

    private GoogleAccountCredential credential;
    private static final String[] SCOPES = {CalendarScopes.CALENDAR};
    public static final String PREF_ACCOUNT_NAME = "accountName"; //Preference Variable
    public static final int REQUEST_ACCOUNT_PICKER = 1000;
    public static final int REQUEST_AUTHORIZATION = 1001;
    private static final int CREQUEST_NEXT = 1;
    private static final int CREQUEST_TODAY = 2;

    private MainActivity GUI;
    private TexttoSpeech tts;

    public GoogleCalendar(MainActivity GUI, TexttoSpeech tts){
        this.GUI = GUI;
        this.tts = tts;
        credential = GoogleAccountCredential.usingOAuth2(GUI.getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
    }

    //Asynchronous Class for Requests
    private class calendarRequest extends AsyncTask<Void, Void, List<Event>>{
        private int requestNumber;
        private com.google.api.services.calendar.Calendar service = null;
        private Exception lastError = null;

            //builds the request
            calendarRequest(GoogleAccountCredential credential, int requestNumber){
                this.requestNumber = requestNumber;
                HttpTransport transport = AndroidHttp.newCompatibleTransport();
                JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
                service = new com.google.api.services.calendar.Calendar.Builder(
                        transport, jsonFactory, credential)
                        .setApplicationName("JarvisV1.1")
                        .build();
        }

        //gets the upcoming activity
        @Override
        protected List<Event> doInBackground(Void... params) {
            try {
                DateTime now = new DateTime(System.currentTimeMillis());
                Events events = null;
                List<Event> items = new ArrayList<Event>();
                String calid = "";
                if(GUI.user.getName().toLowerCase().equals("howard")){
                    calid = "hwd15508@gmail.com";
                }else if(GUI.user.getName().toLowerCase().equals("raylen")){
                    calid = "suvldprdj19peetm5rk0vl45fc@group.calendar.google.com";
                }else if(GUI.user.getName().toLowerCase().equals("jerry")){
                    calid = "asianricenoodles@gmail.com";
                }
                switch(requestNumber){
                    case CREQUEST_NEXT:
                        events = service.events().list(calid)
                                .setMaxResults(1)
                                .setTimeMin(now)
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute();
                        items = events.getItems();
                        break;
                    case CREQUEST_TODAY:
                        Calendar cal = Calendar.getInstance();
                        cal.set(Calendar.HOUR_OF_DAY, 23);
                        cal.set(Calendar.MINUTE, 59);
                        cal.set(Calendar.SECOND, 59);
                        cal.set(Calendar.MILLISECOND, 999);
                        DateTime tmrw = new DateTime(cal.getTimeInMillis());
                        events = service.events().list(calid)
                                .setMaxResults(10)
                                .setTimeMin(now)
                                .setTimeMax(tmrw)
                                .setOrderBy("startTime")
                                .setSingleEvents(true)
                                .execute();
                        items = events.getItems();
                        break;
                    default:
                        return null;
                }
                return items;
            }catch(Exception e){
                lastError = e;
                cancel(true);
                return null;
            }
        }

        //states the upcoming activity using tts
        @Override
        protected void onPostExecute(List<Event> events) {
            if(events.size()==0){
                tts.speak("No events on calendar!");
            }else{
                switch(requestNumber){
                    case CREQUEST_NEXT:
                        Event event = events.get(0);
                        if(event.getStart().getDateTime() == null){
                            tts.speak(String.format("Next up on your schedule is %s at an unspecified time.",
                                    events.get(0).getSummary()));
                        }else {
                            tts.speak(String.format("Next up on your schedule is %s at %s",
                                    events.get(0).getSummary(),
                                    new Time(events.get(0).getStart().getDateTime())
                                            .getTime()));
                        }
                        break;
                    case CREQUEST_TODAY:
                        String scheduleMessage = "";
                        scheduleMessage += "Your schedule today includes: ";
                        for (Event e: events){
                            if(e.getStart().getDateTime() == null){
                                scheduleMessage += String.format("%s at an unspecified time, ",
                                        e.getSummary());
                            }else{
                                scheduleMessage += String.format("%s at %s, ",
                                        e.getSummary(),
                                        new Time(e.getStart().getDateTime())
                                                .getTime());
                            }
                        }
                        tts.speak(scheduleMessage);
                }
            }
        }

        @Override
        protected void onCancelled() {
            if(lastError != null){
                if(lastError instanceof UserRecoverableAuthIOException){
                    tts.speak("Requesting Authorization");
                    GUI.startActivityForResult(((UserRecoverableAuthIOException)lastError).getIntent(), REQUEST_AUTHORIZATION);
                }else{
                    GUI.displayToast("Google Calendar Test Request Error: " + lastError.getMessage());
                    tts.speak("Google Calendar Test Request Error: " + lastError.getMessage());
                }
            }else{
                GUI.displayToast("Request Cancelled!");
                tts.speak("Request Cancelled!");
            }
        }
    }

    //next request: states upcoming activity
    public void next(){
        if(checks()) {
            new calendarRequest(credential, CREQUEST_NEXT).execute();
        }
    }

    //today request: states activites for today
    public void today(){
        if(checks()) {
            new calendarRequest(credential, CREQUEST_TODAY).execute();
        }
    }

    //starts an Intent with Result to select Google Account and store into saved preferences
    public void chooseAccount(){
        tts.speak("Please set a Google Account.");
        GUI.startActivityForResult(credential.newChooseAccountIntent(), REQUEST_ACCOUNT_PICKER);
    }

    //Checks for Internet, GooglePlayServices, and a Google Account
    private boolean checks(){
        if(!isGooglePlayServicesAvailable()){
            GUI.displayToast("Google Play Services not available!");
            tts.speak("Google Play Services is not available!");
        }
        //if account not found, checks saved preferences for stored account
        if (credential.getSelectedAccountName() == null) {
            String accountName = GUI.getPreferences(Context.MODE_PRIVATE).getString(PREF_ACCOUNT_NAME, null);
            if(accountName != null){
                setAccount(accountName);
            }else {
                GUI.displayToast("No Google Account found!");
                tts.speak("No Google Account found!");
                chooseAccount();
            }
        }
        if (!isDeviceOnline()) {
            GUI.displayToast("No Internet Connection!");
            tts.speak("No Internet Connection!");
        }
        return isGooglePlayServicesAvailable() && !(credential.getSelectedAccountName() == null) && isDeviceOnline();
    }

    //Checks for Google Play Service
    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(GUI);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    public void setAccount(String accountName){
        credential.setSelectedAccountName(accountName);
    }

    //Checks for Internet Connectivity
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr = (ConnectivityManager) GUI.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }
}
