/* Requires:
    Permission: RECORD_AUDIO, INTERNET
 */

package com.example.jarvisv11;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;

import java.util.ArrayList;

/**
 * Created by howardzhang on 11/2/17.
 */

public class GoogleSpeechRecognizer implements android.speech.RecognitionListener{

    private android.speech.SpeechRecognizer googlerec;
    private MainActivity GUI;
    private Firebase database;
    private Intent speechIntent;
    public static final int MAIN_REQUEST = 1;
    public static final int TASK_REQUEST = 2;
    public static final int PERSON_REQUEST = 3;
    public static final int LEAVE_REQUEST = 4;
    public static final int ACTIVITY_REQUEST = 5;

    public String taskName = null;
    public String taskDesc = null;
    public String taskDue = null;

    public String personYN = null;
    public String personName = null;

    private final int request;
    private TexttoSpeech tts;

    /* GoogleRecognition Setup Method:
     *  Uses GoogleSpeechRecognizer's implementation of Google's RecognitionListener
     *  Creates Intent to use Google Servers
     *  Resources:
     *      Default English Language Model
     *      Free Form Language Model
     */
    public GoogleSpeechRecognizer(MainActivity GUI, TexttoSpeech tts, int request, Firebase database){
        this.database = database;
        this.request = request;
        this.GUI = GUI;
        this.tts = tts;
        googlerec = android.speech.SpeechRecognizer.createSpeechRecognizer(GUI);
        googlerec.setRecognitionListener(this);
        speechIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        speechIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
    }

    //implementation of Google's RecognitionListener
    @Override
    //restarts Google search after error
    public void onError(int i) {
        if (i == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
                || i == SpeechRecognizer.ERROR_NO_MATCH) {
            switch(request) {
                case PERSON_REQUEST:
                    tts.speak("Could you repeat that please.");
                    break;
                case MAIN_REQUEST:
                    tts.speak("Repeat that.");
                    break;
                case TASK_REQUEST:
                    tts.speak("I didn't quite catch that.");
                    break;
                case LEAVE_REQUEST:
                    tts.speak("What was that?");
                    break;
                case ACTIVITY_REQUEST:
                    tts.speak("I couldn't hear you.");
                    break;
            }
            restartGoogleRec();
        } else {
            GUI.displayToast("Google Speech Recognition Error!");
        }
    }

    //checks to see if speech matches possibility and calls respective method
    @Override
    public void onResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION);
        //All possibilities
        String command = matches.get(0).toLowerCase();
        switch(request) {
            case ACTIVITY_REQUEST:
                if(command.equals("cancel") || command.contains("stop") || command.equals("nevermind")){
                    GUI.startSphinxRec();
                }else{
                    GUI.activity(command);
                }
                break;
            case LEAVE_REQUEST:
                if(command.equals("cancel") || command.contains("stop") || command.equals("nevermind")){
                    GUI.startSphinxRec();
                    break;
                }else{
                    GUI.leaveFor(matches.get(0));
                }
                break;
            case PERSON_REQUEST:
                if(command.equals("cancel") || command.equals("stop") || command.equals("nevermind")){
                    GUI.startSphinxRec();
                    break;
                }
                if(personYN==null){
                    personYN = matches.get(0);
                    GUI.YNresult(personYN);
                    break;
                }
                if(personName==null){
                    personName = matches.get(0);
                    GUI.addPerson(personName);
                    personYN = null;
                    personName = null;
                }
                break;
            case TASK_REQUEST:
                if(command.equals("cancel") || command.equals("stop") || command.equals("nevermind")){
                    GUI.startSphinxRec();
                    break;
                }
                if(taskName==null){
                    taskName = matches.get(0);
                    GUI.addDue();
                    break;
                }
                if(taskDue==null){
                    taskDue = matches.get(0);
                    GUI.addDesc();
                    break;
                }
                if(taskDesc==null) {
                    if (matches.get(0).toLowerCase().contains("no") || matches.get(0).toLowerCase().contains("nope")) {
                        taskDesc = "None";
                    } else {
                        taskDesc = matches.get(0);
                    }
                    GUI.dataAddTask(taskName, taskDue, taskDesc);
                    taskName = null;
                    taskDue = null;
                    taskDesc = null;
                }
                break;
            case MAIN_REQUEST:
                try {
                    if (command.contains("bitch")) {
                        tts.speak("I do not answer to rude speech.");
                        GUI.startSphinxRec();
                        break;
                    }
                    if (command.contains("username")) {
                        String[] parts = command.split(" ");
                        String username = null;
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("username")) {
                                username = parts[i + 1];
                                break;
                            }
                        }
                        for (int i = 0; i < database.persons.size(); i++) {
                            if (database.persons.get(i).getName().toLowerCase().equals(username.toLowerCase())) {
                                if (5 == database.persons.get(i).getLevel()) {
                                    GUI.user = database.persons.get(i);
                                    break;
                                } else {
                                    tts.speak("Your user access level is too low to change users.");
                                    GUI.startSphinxRec();
                                    return;
                                }
                            }
                        }
                    }

                    //LOW USER LEVEL REQUIRED
                    if (command.contains("what") && (command.contains("level") || command.contains("access"))) {
                        GUI.userAccessLevel();
                    }

                    //3 USER LEVEL REQUIRED
                    else if(command.contains("weather")){
                        GUI.fullWeather();
                    }else if(command.contains("time")){
                        GUI.time();
                    }
                    //4 USER LEVEL REQUIRED
                    else if(command.contains("what") && command.contains("doing") || command.contains("activities")){
                        GUI.findActivity();
                    }

                    //HIGH USER LEVEL REQUIRED
                    else if(command.contains("summary") && command.contains("day")){
                        GUI.summary();
                    }
                    else if (command.contains("change") && command.contains("user") &&
                            (command.contains("level") || command.contains("access"))) {
                        String[] parts = command.split(" ");
                        String name = null;
                        int level = -1;
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("user")) {
                                name = parts[i + 1];
                                break;
                            }
                        }
                        for (int i = 0; i < parts.length; i++) {
                            if (parts[i].equals("to")) {
                                if(parts[i+1].equals("one")||parts[i+1].equals("1")){
                                    level = 1;
                                }else if(parts[i+1].equals("two")||parts[i+1].equals("2")){
                                    level = 2;
                                }else if(parts[i+1].equals("three")||parts[i+1].equals("3")){
                                    level = 3;
                                }else if(parts[i+1].equals("four")||parts[i+1].equals("4")){
                                    level = 4;
                                }else if(parts[i+1].equals("five")||parts[i+1].equals("5")){
                                    level = 5;
                                }
                                break;
                            }
                        }
                        GUI.changeUserLevel(name, level);
                    } else if(command.contains("leav")||command.contains("got to go")||command.contains("going to go")){
                        GUI.leaving();
                    } else if (command.contains("shut up") || command.contains("mute")) {
                        GUI.muteViolet();
                    } else if (command.contains("account") && command.contains("change")) {
                        GUI.changeGoogleAccount();
                    } else if (command.contains("schedule")) {
                        if (command.contains("next")) {
                            GUI.calendarNext();
                        } else if (command.contains("today")) {
                            GUI.calendarToday();
                        }
                    } else if (command.contains("task")) {
                        if (command.contains("add")) {
                            String[] parts = command.split(" ");
                            String name = null;
                            for (int i = 0; i < parts.length; i++) {
                                if (parts[i].equals("called") || parts[i].equals("named") || parts[i].equals("titled")) {
                                    name = parts[i + 1];
                                    break;
                                }
                            }
                            GUI.addTask(name);
                        } else if (command.contains("what") || command.contains("read")) {
                            GUI.readTasks();
                        } else if (command.contains("remove") || command.contains("finish")) {
                            String[] parts = command.split(" ");
                            String name = null;
                            for (int i = 0; i < parts.length; i++) {
                                if (parts[i].equals("called") || parts[i].equals("named") || parts[i].equals("titled")) {
                                    name = parts[i + 1];
                                    break;
                                }
                            }
                            GUI.removeTask(name);
                        } else if (command.contains("more") || command.contains("description")) {
                            String[] parts = command.split(" ");
                            String name = null;
                            for (int i = 0; i < parts.length; i++) {
                                if (parts[i].equals("called") || parts[i].equals("named") || parts[i].equals("titled")) {
                                    name = parts[i + 1];
                                    break;
                                }
                            }
                            GUI.descTask(name);
                        }
                    }else if(command.contains("study mode") && command.contains("on")){
                        GUI.studyMode = true;
                        GUI.startSphinxRec();
                    }else if(command.contains("study mode") && command.contains("off")){
                        GUI.studyMode = false;
                        GUI.startSphinxRec();
                    }else if(command.contains("nothing")){
                        tts.speak("oh.");
                        GUI.startSphinxRec();
                    }else if(command.contains("not you")){
                        tts.speak("Thanks. I feel so needed.");
                        GUI.startSphinxRec();
                    }else {
                        GUI.doNothing();
                    }
                }catch(Exception e){
                    tts.speak("I didn't understand you, could you repeat that please?");
                    GUI.startSphinxRec();
                }
        }
    }

    //unused implemented methods
    @Override
    public void onReadyForSpeech(Bundle bundle) {
    }
    @Override
    public void onBeginningOfSpeech() {
    }
    @Override
    public void onRmsChanged(float v) {
    }
    @Override
    public void onBufferReceived(byte[] bytes) {
    }
    @Override
    public void onEndOfSpeech() {
    }
    @Override
    public void onPartialResults(Bundle bundle) {
    }
    @Override
    public void onEvent(int i, Bundle bundle) {
    }

    //restarts Google speech recognition
    public void restartGoogleRec(){
        googlerec.cancel();
        googlerec.startListening(speechIntent);
    }

    //unlocks resources
    public void destroy(){
        if (googlerec != null) {
            googlerec.cancel();
            googlerec.destroy();
        }
    }

    //start listening
    public void startListening(){
        googlerec.startListening(speechIntent);
    }

    //sets Task variables to null
    public void nullTasks(boolean withName){
        if(withName){
            taskName = null;
        }
        taskDesc = null;
        taskDue = null;
    }

    //sets Person variables to null
    public void nullPerson(){
        personYN = null;
        personName = null;
    }
}
