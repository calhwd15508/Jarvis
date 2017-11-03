package com.example.jarvisv11;

/**
 * Created by howardzhang on 11/2/17.
 */

import android.telephony.SmsManager;

public class SMS {
    private SmsManager sms;
    private MainActivity GUI;

    public SMS(MainActivity GUI){
        sms = SmsManager.getDefault();
        this.GUI = GUI;
    }

    //sends text "message" to "number"
    public void sendText(String number, String message){
        try{
            sms.sendTextMessage(number, null, message, null, null);
            GUI.displayToast("Message sent!");
        }catch(Exception e){
            GUI.displayToast("Error sending text!\n" + e.getMessage());
        }
    }
}
