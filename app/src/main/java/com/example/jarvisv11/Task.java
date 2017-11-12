package com.example.jarvisv11;

/**
 * Created by howardzhang on 11/7/17.
 */

public class Task {
    private String due;
    private String name;
    private String description;
    private String UID;
    private String user;

    public void setDue(String due){
        this.due = due;
    }
    public void setName(String name){
        this.name = name;
    }
    public void setDesc(String description){
        this.description = description;
    }
    public void setUID(String UID){
        this.UID = UID;
    }
    public void setUser(String user){
        this.user = user;
    }

    public String getDue(){
        return due;
    }
    public String getName(){
        return name;
    }
    public String getDesc(){
        return description;
    }
    public String getUID(){
        return UID;
    }
    public String getUser(){
        return user;
    }
}
