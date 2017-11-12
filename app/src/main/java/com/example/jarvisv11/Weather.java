/* Requires:
    Permission: ACCESS_NETWORK_STATE, INTERNET, ACCESS_FINE_LOCATION
    Gradle App Changes
 */

package com.example.jarvisv11;

import zh.wang.android.yweathergetter4a.WeatherInfo;
import zh.wang.android.yweathergetter4a.YahooWeather;
import zh.wang.android.yweathergetter4a.YahooWeatherInfoListener;

/**
 * Created by howardzhang on 11/12/17.
 */

public class Weather implements YahooWeatherInfoListener{

    private MainActivity GUI;
    private YahooWeather weather = YahooWeather.getInstance();
    private String location;
    private int temp;
    private String text;
    private String sunset;
    private String sunrise;
    private String humidity;
    private String windSpeed;
    public final static int REQUEST_TIME = 1;
    public final static int REQUEST_WEATHER = 2;
    public final static int REQUEST_SUMMARY = 3;
    private int request;


    public Weather(MainActivity GUI, int request){
        this.GUI = GUI;
        this.request = request;
    }

    @Override
    public void gotWeatherInfo(WeatherInfo weatherInfo, YahooWeather.ErrorType errorType) {
        if(weatherInfo != null){
            location = weatherInfo.getLocationCity();
            temp = weatherInfo.getCurrentTemp();
            text = weatherInfo.getCurrentText();
            humidity = weatherInfo.getAtmosphereHumidity();
            sunset = weatherInfo.getAstronomySunset();
            sunrise = weatherInfo.getAstronomySunrise();
            windSpeed = weatherInfo.getWindSpeed();
            switch(request){
                case REQUEST_TIME:
                    GUI.returnTime(sunrise, sunset);
                    break;
                case REQUEST_SUMMARY:
                    GUI.returnSummary(location, temp, text);
                    break;
                case REQUEST_WEATHER:
                    GUI.returnWeather(location, temp, text, humidity, windSpeed);
            }
        }else{
            GUI.displayToast("Weather error: " + errorType.name());
        }
    }

    public void getWeather(){
        weather.queryYahooWeatherByGPS(GUI, this);
    }
}
