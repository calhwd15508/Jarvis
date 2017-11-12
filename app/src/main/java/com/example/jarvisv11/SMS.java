/* Requires:
    Permissions: SEND_SMS
 */

package com.example.jarvisv11;

/**
 * Created by howardzhang on 11/2/17.
 */

import android.telephony.SmsManager;

public class SMS {
    private SmsManager sms;
    private MainActivity GUI;
    public static final String RAYLEN = "8582055221";
    public static final String HOWARD = "8586105086";
    public static final String JERRY = "8583567679";

    public SMS(MainActivity GUI){
        sms = SmsManager.getDefault();
        this.GUI = GUI;
    }

    //sends text "message" to "number"
    public void sendText(String[] numbers, String message){
        try{
            for(String number : numbers) {
                sms.sendTextMessage(number, null, message, null, null);
            }
        }catch(Exception e){
            GUI.displayToast("Error sending text!\n" + e.getMessage());
        }
    }
}
