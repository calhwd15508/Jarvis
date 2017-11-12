package com.example.jarvisv11;

/**
 * Created by howardzhang on 11/9/17.
 */

public class Log {
    private String date;
    private String time;
    private String id;
    public String getID(){
        return id;
    }
    public String getTime(){
        return time;
    }
    public String getDate(){
        return date;
    }
    public void setID(String id){
        this.id = id;
    }
    public void setTime(String time){
        this.time = time;
    }
    public void setDate(String date){
        this.date = date;
    }
}
