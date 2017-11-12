/*Jarvis Version 1.1
/*
    POSSIBLE PHRASES:
        OK MELODY: KEYWORD
        MUTE || SHUT UP: MUTE JARVIS
        CHANGE && ACCOUNT: CHANGE GOOGLE ACCOUNT
        NEXT && SCHEDULE: NEXT EVENT
        TODAY && SCHEDULE: TODAY'S EVENTS
        ADD && TASK -> CALLED || NAMED || TITLED -> name: ADDS A TASK
        (WHAT || READ) && TASK: READS OUT ALL TASKS
        (REMOVE || FINISH) && TASK -> CALLED || NAMED || TITLED -> name: REMOVES TASK
 */
package com.example.jarvisv11;

import android.Manifest;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;

import com.google.api.client.util.DateTime;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity{

    // Used to handle permission request
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final int PERMISSIONS_REQUEST_RECORD_SEND_SMS = 2;
    private static final int PERMISSIONS_REQUEST_ACCESS_GET_ACCOUNTS = 3;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 4;

    //Google Calendar Variable
    private GoogleCalendar calendar;
    //Layout Variables
    private Button start_button;
    //PocketSphinx Variable
    private SphinxSpeechRecognizer sphinxrec;
    //Google SpeechRec Variables
    private GoogleSpeechRecognizer googlerec;
    private GoogleSpeechRecognizer taskrec;
    private GoogleSpeechRecognizer personrec;
    private GoogleSpeechRecognizer leaverec;
    private GoogleSpeechRecognizer activityrec;
    //SMS Variable
    private SMS sms;
    //TTS Variable
    private TexttoSpeech tts;
    //Firebase Variable
    private Firebase database;
    //temporary person variable
    private Person person;
    //Weather Variable
    private Weather weather;
    private Weather weatherTime;
    private Weather weatherSummary;

    //stores last user
    public Person user;

    //stores locations of Howard, Raylen, and Jerry
    public String[] locations = {"home", "home", "home"};
    public final int HOWARD = 0;
    public final int RAYLEN = 1;
    public final int JERRY = 2;

    //study mode
    public boolean studyMode = false;

    //initialization variable
    private boolean init = true;


    //METHOD RUN ON START
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //creates activity and sets up layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Checks for all Permissions
        int permissionCheckAudio = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.RECORD_AUDIO);
        int permissionCheckSMS = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.SEND_SMS);
        int permissionsCheckAccount = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.GET_ACCOUNTS);
        int permissionsFineLocation = ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION);

        if (permissionCheckAudio != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
            return;
        }
        if (permissionCheckSMS != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, PERMISSIONS_REQUEST_RECORD_SEND_SMS);
            return;
        }
        if (permissionsCheckAccount != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.GET_ACCOUNTS}, PERMISSIONS_REQUEST_ACCESS_GET_ACCOUNTS);
            return;
        }
        if (permissionsFineLocation != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            return;
        }

        //ALL SETUPS
        //initialize Text to Speech
        tts = new TexttoSpeech(this);
        //initialize SMS
        sms = new SMS(this);
        //initialize Firebase
        database = new Firebase(this);
        //intialize GoogleSpeechRecs
        googlerec = new GoogleSpeechRecognizer(this, tts, GoogleSpeechRecognizer.MAIN_REQUEST, database);
        taskrec = new GoogleSpeechRecognizer(this, tts, GoogleSpeechRecognizer.TASK_REQUEST, database);
        personrec = new GoogleSpeechRecognizer(this, tts, GoogleSpeechRecognizer.PERSON_REQUEST, database);
        leaverec = new GoogleSpeechRecognizer(this, tts, GoogleSpeechRecognizer.LEAVE_REQUEST, database);
        activityrec = new GoogleSpeechRecognizer(this, tts, GoogleSpeechRecognizer.ACTIVITY_REQUEST, database);
        //initialize PocketSphinx Setup and Keyword Search
        sphinxrec = new SphinxSpeechRecognizer(this, tts, googlerec);
        //Google Credentials
        calendar = new GoogleCalendar(this, tts);
        //Weather
        weather = new Weather(this, Weather.REQUEST_WEATHER);
        weatherTime = new Weather(this, Weather.REQUEST_TIME);
        weatherSummary = new Weather(this, Weather.REQUEST_SUMMARY);

        //Launch Jarvis
        start_button = (Button)findViewById(R.id.start_button);
        start_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(init){
                    init = false;
                    for(int i = 0; i<database.persons.size(); i++){
                        if(database.persons.get(i).getName().equals("Howard")){
                            user = database.persons.get(i);
                        }
                    }
                    tts.speak("Violet awake and at your service.");
                    displayToast("Violet Started");
                    sphinxrec.restartKeySearch();
                }else {
                    if (checkHighLevelAccess()) {
                        tts.speak("Violet awake and at your service.");
                        displayToast("Violet Started");
                        sphinxrec.restartKeySearch();
                    }else{
                        tts.speak("Violet Program Requires level 5 access.");
                    }
                }
            }
        });
    }

    /*Handles Activity Results from External Intents
        Possibilities:
            REQUEST_ACCOUNT_PICKER: Selects Google Account for Calendar
            REQUEST_AUTHORIZATION:
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case GoogleCalendar.REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null) {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings = getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(GoogleCalendar.PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        calendar.setAccount(accountName);
                    }
                }else{
                    displayToast("Error selecting Google Account!");
                    tts.speak("Error selecting Google Account!");
                }
                break;
            case GoogleCalendar.REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    displayToast("Google Authorization Granted!");
                    tts.speak("Google Authorization Granted!");
                }else{
                    displayToast("Google Authorization Denied!");
                    tts.speak("Google Authorization Denied!");
                }
                break;
        }
    }


    //PERMISSION REQUESTS
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case PERMISSIONS_REQUEST_RECORD_AUDIO:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish();
                }
                break;
            case PERMISSIONS_REQUEST_RECORD_SEND_SMS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish();
                }
                break;
            case PERMISSIONS_REQUEST_ACCESS_GET_ACCOUNTS:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else {
                    finish();
                }
                break;
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION:
                if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                } else{
                    finish();
                }
        }
    }

    //what to do when card gets swiped
    public void cardSwipe(Log log){
        personrec.nullPerson();
        String realId = "3"+log.getID().substring(1,10);
        person = null;
        for(Person p : database.persons){
            if(p.getId().equals(realId)){
                person = p;
            }
        }
        if(person == null){
            tts.speak("I have no record of you in my database.");
            person = new Person();
            person.setId(realId);
            tts.speak("Would you like to add yourself?");
            personrec.startListening();
        }else{
            userIdentified(person);
            person = null;
            sphinxrec.startListening();
        }
    }
    public void userIdentified(Person person){
        if(person.getName().toLowerCase().equals("howard") ||
                person.getName().toLowerCase().equals("jerry") ||
                person.getName().toLowerCase().equals("raylen")){
            tts.speak("Welcome back, " + person.getName());
            if(person.getName().toLowerCase().equals("howard")){
                locations[HOWARD] = "home";
                database.here_away("here","howard");
            }else if(person.getName().toLowerCase().equals("raylen")){
                locations[RAYLEN] = "home";
                database.here_away("here","raylen");
            }else if(person.getName().toLowerCase().equals("jerry")){
                locations[JERRY] = "home";
                database.here_away("here","jerry");
            }
            user = person;
            database.motor();
        }else{
            String numbers[] = {SMS.HOWARD, SMS.RAYLEN, SMS.JERRY};
            sms.sendText(numbers, person.getName() + " has just swiped in to Room 408.");
            tts.speak("Hello, " + person.getName());
            if (locations[HOWARD].equals("home") || locations[JERRY].equals("home") || locations[RAYLEN].equals("home")) {
                if(!studyMode) {
                    database.motor();
                    tts.speak("Welcome into Room 408");
                }else{
                    tts.speak("You are not welcome into Room 408 right now. GTFO.");
                }
            }else{
                tts.speak("Nobody is home right now.");
            }
        }
    }
    public void YNresult(String YN){
        if(YN.contains("no")){
            tts.speak("Goodbye.");
        }else if(YN.contains("yes")||YN.contains("yeah")||YN.contains("sure")){
            tts.speak("Sure thing. What is your name?");
            personrec.startListening();
        }else{
            tts.speak("Please answer with yes or no.");
            personrec.personYN = null;
            personrec.startListening();
        }
    }
    public void addPerson(String name){
        person.setName(name);
        person.setLevel(1);
        database.addPerson(person);
        userIdentified(person);
        person = null;
        sphinxrec.startListening();
    }

    @Override
    //Checks that all resources unlocked before close
    public void onDestroy() {
        super.onDestroy();
        tts.destroy();
        sphinxrec.destroy();
        googlerec.destroy();
    }

    //method for external classes to create Toasts
    public void displayToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    //method to cancel sphinxrec
    public void cancelSphinxRec(){
        sphinxrec.cancel();
    }
    public void startSphinxRec(){
        sphinxrec.startListening();
    }

    public boolean checkHighLevelAccess(){
        return user.getLevel() == 5;
    }

    //ALL POSSIBLE ACTIONS
    //ALL ACCESS LEVELS ACCEPTED
    public void userAccessLevel(){
        tts.speak("User " + user.getName() + " has access level " + user.getLevel());
        sphinxrec.startListening();
    }

    //ACCESS LEVEL 3
    public void fullWeather(){
        if(user.getLevel()>=3) {
            weather.getWeather();
        }else{
            tts.speak("Your user access level is not high enough for this command." +
                    " Try becoming better friends with Room 408 before you take advantage of their cool stuff.");
            sphinxrec.startListening();
        }
    }
    public void returnWeather(String location, int temp, String text, String humidity, String windSpeed){
        tts.speak("The weather in " + location + " is " + text + " at " + temp + " Celsius." +
                "The humidity outside is " + humidity + "% with a wind speed of " + windSpeed + " miles per hour.");
        sphinxrec.startListening();
    }

    public void time(){
        if(user.getLevel()>=3){
            weatherTime.getWeather();
        }else{
            tts.speak("Your user access level is not high enough for this command." +
                    "Come on. A user access level of 3 isn't even that hard to get.");
            sphinxrec.startListening();
        }
    }
    public void returnTime(String sunrise, String sunset){
        Time current = new Time(new DateTime(System.currentTimeMillis()));
        tts.speak("The current time is " + current.getTime()+". Sunrise occurred at "
                + sunrise + " while sunset is projected for " + sunset);
        sphinxrec.startListening();
    }

    //ACCESS LEVEL 4
    //finding activity of level 5 users
    public void findActivity(){
        if(user.getLevel() >= 4){
            tts.speak("Who would you like to stalk today?");
            activityrec.startListening();
        }else{
            tts.speak("You do not have high enough clearance for that information. Stop stalking my users.");
            sphinxrec.startListening();
        }
    }
    public void activity(String name){
        if(name.equals("howard")){
            tts.speak("Howard's Activity: " + locations[HOWARD]);
            sphinxrec.startListening();
        }else if(name.equals("raylen")){
            tts.speak("Raylen's Activity: " + locations[RAYLEN]);
            sphinxrec.startListening();
        }else if(name.equals("jerry")){
            tts.speak("Jerry's Activity: " + locations[JERRY]);
            sphinxrec.startListening();
        }else{
            tts.speak("Please try again with and respond with either Raylen, Jerry, or Howard.");
            activityrec.startListening();
        }
    }

    //ACCESS LEVEL 5
    //Summary of the day
    public void summary(){
        if(checkHighLevelAccess()){
            weatherSummary.getWeather();
        }else{
            tts.speak("User Access Level 5 Required. This command is just like from the movie Iron Man," +
                    " and you don't get to use it. Too bad for you.");
        }
    }
    public void returnSummary(String location, int temp, String text){
        Time current = new Time(new DateTime(System.currentTimeMillis()));
        String intro = "";
        if(current.getTime().contains("am")){
            intro = "Good morning sir.";
        }else{
            intro = "Hello sir. Late start today?";
        }
        tts.speak(intro + " It is " + current.getTimeandDate() + ". The weather in " + location +
                " is " + text + " at " + temp + " degrees Celsius.");
        List<Task> tasks = new ArrayList<Task>();
        for(Task task : database.tasks){
            if(task.getUser().equals(user.getName().toLowerCase())){
                tasks.add(task);
            }
        }
        if (tasks.size() == 0) {
            tts.speak("You have no tasks to do.");
        }else {
            tts.speak("Your current tasks include: ");
            for (Task task : tasks) {
                tts.speak(task.getName() + ", ");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    displayToast(e.getMessage());
                }
            }
        }
        calendar.today();
        sphinxrec.startListening();
    }

    //code for leaving the room
    public void leaving(){
        if(checkHighLevelAccess()){
            tts.speak("What will you be doing sir?");
            leaverec.startListening();
        }else{
            tts.speak("You do not have level 5 access. I do not care if you are leaving.");
            sphinxrec.startListening();
        }
    }
    public void leaveFor(String location){
        if(user.getName().toLowerCase().equals("howard")){
            tts.speak("Setting Howard's activity to " + location);
            locations[HOWARD] = location;
            database.here_away("away","howard");
        }else if(user.getName().toLowerCase().equals("raylen")){
            tts.speak("Setting Raylen's activity to " + location);
            locations[RAYLEN] = location;
            database.here_away("away","raylen");
        }else if(user.getName().toLowerCase().equals("jerry")){
            tts.speak("Setting Jerry's activity to " + location);
            locations[JERRY] = location;
            database.here_away("away","jerry");
        }
        if (locations[HOWARD].equals("home")) {
            for(Person person : database.persons) {
                if (person.getName().toLowerCase().equals("howard")) {
                    tts.speak("Howard is now the active user.");
                    user = person;
                    break;
                }
            }
        } else if(locations[RAYLEN].equals("home")){
            for(Person person : database.persons) {
                if (person.getName().toLowerCase().equals("raylen")) {
                    tts.speak("Raylen is now the active user.");
                    user = person;
                    break;
                }
            }
        } else if (locations[JERRY].equals("home")) {
            for(Person person : database.persons) {
                if (person.getName().toLowerCase().equals("jerry")) {
                    tts.speak("Jerry is now the active user.");
                    user = person;
                    break;
                }
            }
        }else{
            tts.speak("No active users.");
            user = null;
        }
        sphinxrec.startListening();
    }

    //Change User Level
    public void changeUserLevel(String name, int level) {
        if(checkHighLevelAccess()){
            if(name != null && level != -1) {
                for (Person person : database.persons) {
                    if (person.getName().toLowerCase().equals(name.toLowerCase())) {
                        if(person.getLevel()==5){
                            tts.speak("You cannot change the access level of a level 5 user.");
                        }else{
                            tts.speak("Changing user " + person.getName() + "'s access level to " + level);
                            database.updatePersonLevel(person, level);
                        }
                        break;
                    }
                }
            }else{
                tts.speak("Please repeat command with a username and level.");
            }
        }else{
            tts.speak("Command requires Level 5 Access");
        }
        sphinxrec.startListening();
    }
    //Mute Jarvis: Stops recognizing sphinxrec keyword
    public void muteViolet(){
        if(checkHighLevelAccess()) {
            tts.speak("Going into standby.");
            sphinxrec.cancel();
        }else{
            tts.speak("Command requires Level 5 Access. I can say whatever I want to you.");
            sphinxrec.startListening();
        }
    }

    //Google Calendar Next: States upcoming event
    public void calendarNext() {
        if(checkHighLevelAccess()) {
            calendar.next();
        }else{
            tts.speak("Command requires Level 5 Access. Don't snoop around other people's calendars.");
        }
        sphinxrec.startListening();
    }

    //Google Calendar Today: States today's events
    public void calendarToday() {
        if(checkHighLevelAccess()) {
            calendar.today();
        }else{
            tts.speak("Command requires Level 5 Access. Don't snoop around other people's calendars.");
        }
        sphinxrec.startListening();
    }

    //Change Google Account: Allows user to select Google Account
    public void changeGoogleAccount(){
        if(checkHighLevelAccess()) {
            tts.speak("Changing Google Calendar Account");
            calendar.chooseAccount();
        }else{
            tts.speak("Command requires Level 5 Access. Don't mess with people's Google Accounts.");
        }
        sphinxrec.startListening();
    }

    //TASKS: ADD, REMOVE, READ_ALL, READ_DESCRIPTION

    //Tasks: Add a task
    public void addTask(String name){
        if(checkHighLevelAccess()) {
            if (name != null) {
                taskrec.nullTasks(false);
                taskrec.taskName = name;
                addDue();
            } else {
                taskrec.nullTasks(true);
                tts.speak("What is the name of the task?");
                taskrec.startListening();
            }
        }else{
            tts.speak("Command requires Level 5 Access. Room 408's todo list is sacred. Don't touch it.");
            sphinxrec.startListening();
        }
    }
    public void addDue(){
        tts.speak("When do you have to finish it?");
        taskrec.startListening();
    }
    public void addDesc(){
        tts.speak("Would you like to add a description?");
        taskrec.startListening();
    }
    public void dataAddTask(String name, String due, String desc){
        if(due.equals("None")){
            database.addTask(name, desc, null);
        }else {
            Time dueTime = new Time(due);
            if(dueTime.getTime().equals("None")) {
                database.addTask(name, desc, dueTime.getDate());
            }else{
                database.addTask(name, desc, dueTime.getTimeandDate());
            }
            if(dueTime.getDate().equals("None")){
                tts.speak("Added new task named: " + name);
            }else{
                tts.speak("Added new task named: " + name + " for " + dueTime.getDate());
            }
        }
        sphinxrec.startListening();
    }

    //Tasks: Reads all tasks
    public void readTasks(){
        if(checkHighLevelAccess()) {
            List<Task> tasks = new ArrayList<Task>();
            for(Task task : database.tasks){
                if(task.getUser().equals(user.getName().toLowerCase())){
                    tasks.add(task);
                }
            }
            if (tasks.size() > 3) {
                tts.speak("You have quite a lot to do today sir.");
            } else {
                tts.speak("It won't be that bad today sir.");
            }
            tts.speak("Here are your current tasks: ");
            for (Task task : tasks) {
                tts.speak(task.getName() + ", ");
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    displayToast(e.getMessage());
                }
            }
            if (tasks.size() == 0) {
                tts.speak("You have no tasks to do.");
            }
        }else{
            tts.speak("Command requires Level 5 Access. If you want to track your tasks, go make your own VIOLET program");
        }
        sphinxrec.startListening();
    }

    //Tasks: Removes a task
    public void removeTask(String name){
        if(checkHighLevelAccess()) {
            if (name == null) {
                tts.speak("I didn't quite catch that.");
            } else {
                int index = -1;
                List<Task> tasks = new ArrayList<Task>();
                for(Task task : database.tasks){
                    if(task.getUser().equals(user.getName().toLowerCase())){
                        tasks.add(task);
                    }
                }
                for (int i = 0; i < tasks.size(); i++) {
                    if (tasks.get(i).getName().equals(name)) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    tts.speak("I have no record of a task titled " + name);
                } else {
                    database.removeTask(index);
                    tts.speak("I have removed the task " + name);
                }
            }
        }else{
            tts.speak("Command requires level 5 Access. A person's todo list is sacred. Don't touch it.");
        }
        sphinxrec.startListening();
    }

    //Tasks: Reads description of task
    public void descTask(String name){
        if(checkHighLevelAccess()) {
            if (name == null) {
                tts.speak("I didn't quite catch that.");
            } else {
                int index = -1;
                List<Task> tasks = new ArrayList<Task>();
                for(Task task : database.tasks){
                    if(task.getUser().equals(user.getName().toLowerCase())){
                        tasks.add(task);
                    }
                }
                for (int i = 0; i < tasks.size(); i++) {
                    if (tasks.get(i).getName().equals(name)) {
                        index = i;
                        break;
                    }
                }
                if (index == -1) {
                    tts.speak("I have no record of a task titled " + name);
                } else {
                    tts.speak("The task titled " + name + " is due on: " + tasks.get(index).getDue() +
                            ", and has the following description: " + tasks.get(index).getDesc());
                }
            }
        }else{
            tts.speak("Command requires Level 5 Access. Snooping around in other people's to do lists. That's naughty.");
        }
        sphinxrec.startListening();
    }

    //No command given
    public void doNothing(){
        sphinxrec.startListening();
    }
}
