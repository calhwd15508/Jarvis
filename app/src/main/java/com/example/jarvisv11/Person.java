package com.example.jarvisv11;

/**
 * Created by howardzhang on 11/9/17.
 */

public class Person {
    private String name;
    private String id;
    private int level;
    private String UID;
    public void setName(String name){
        this.name = name;
    }
    public void setId(String id){
        this.id = id;
    }
    public void setUID(String UID){
        this.UID = UID;
    }
    public void setLevel(int level){
        this.level = level;
    }

    public String getName(){
        return name;
    }
    public String getId(){
        return id;
    }
    public String getUID(){
        return UID;
    }
    public int getLevel(){
        return level;
    }
}
