package com.example.jarvisv11;

import com.google.api.client.util.DateTime;

import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by howardzhang on 11/7/17.
 */

//Class to easily handle Date Objects
public class Time {
    private String time;
    private String date;
    private String[] aMonths = {"January", "February", "March", "April", "May", "June",
            "July", "August", "September", "October", "November", "December"};
    private List<String> months = Arrays.asList(aMonths);
    private String[] aDays = {"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th",
            "9th", "10th", "11th", "12th", "13th", "14th", "15th", "16th", "17th",
            "18th", "19th", "20th", "21st", "22nd", "23rd", "24th", "25th", "26th",
            "27th", "28th", "29th", "30th", "31st"};
    private List<String> days = Arrays.asList(aDays);

    public Time(String dateTime){
        try {
            String[] parts = dateTime.split(" ");
            if(months.contains(parts[0]) && days.contains(parts[1])) {
                date = parts[0] + " " + parts[1];
            }else{
                throw new Exception();
            }
            if (dateTime.contains("at")) {
                if(parts[4].contains(":")) {
                    String[] split = parts[4].split(":");
                    int hour = Integer.parseInt(split[0]);
                    int minute = Integer.parseInt(split[1]);
                    if (hour - 12 <= 0 && minute - 60 <= 0) {
                        if (dateTime.contains("morning")) {
                            time = parts[4] + " am";
                        } else {
                            time = parts[4] + " pm";
                        }
                    } else {
                        throw new Exception();
                    }
                }else{
                    if (Integer.parseInt(parts[4]) - 12 <= 0) {
                        if (dateTime.contains("morning")) {
                            time = parts[4] + " am";
                        } else {
                            time = parts[4] + " pm";
                        }
                    } else {
                        throw new Exception();
                    }
                }
            }else{
                time = "None";
            }
        }catch(Exception e){
            date = "None";
            time = "None";
        }
    }

    //Easily parse Google DateTime Objects
    public Time(DateTime gdateTime){
        String dateTime = gdateTime.toString();
        String ampm;
        int hour = Integer.parseInt(dateTime.substring(11,13));
        //am vs. pm
        if (hour > 12){
            hour = hour - 12;
            ampm = "pm";
        }else{
            ampm = "am";
        }
        time = hour + ":" + dateTime.substring(14,16) + " " + ampm;
        String nDate = dateTime.substring(0,10);
        String[] parts = nDate.split("-");
        date = months.get(Integer.parseInt(parts[1])-1)+ " " + days.get(Integer.parseInt(parts[2])-1);
    }

    //External Access Functions
    public String getTime(){
        return time;
    }
    public String getDate(){
        return date;
    }
    public String getTimeandDate(){
        return date + " at " + time;
    }
}
