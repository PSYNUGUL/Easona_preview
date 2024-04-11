package com.example.map_navigation;

import android.location.Location;

import com.google.firebase.database.DatabaseReference;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeatherForecast {
    public List<Object[]> temperatureDataList;
    public double gridTemp;
    public double dewpoint;
    public double maxTemperature;
    public double minTemperature;
    public double relativeHumidity;
    public double apparentTemperature;
    public double windChill;
    public double skyCover;
    public int windDirection;
    public double windSpeed;
    public double windGust;
    public String weather;
    public String coverage;
    public int probPrecipitation;
    public double amtRain;
    public String time;

    public WeatherForecast(List<Object[]> temperatureDataList, double gridTemp, double dewpoint, double maxTemperature, double minTemperature,
                           double relativeHumidity, double apparentTemperature, double windChill,
                           double skyCover, int windDirection, double windSpeed, double windGust,
                           String weather, String coverage, int probPrecipitation, double amtRain, String time) {



        this.temperatureDataList = temperatureDataList;
        this.gridTemp = gridTemp;

        this.dewpoint = dewpoint;
        this.maxTemperature = maxTemperature;
        this.minTemperature = minTemperature;
        this.relativeHumidity = relativeHumidity;
        this.apparentTemperature = apparentTemperature;
        this.windChill = windChill;
        this.skyCover = skyCover;
        this.windDirection = windDirection;
        this.windSpeed = windSpeed;
        this.windGust = windGust;
        this.weather = weather;
        this.coverage = coverage;
        this.probPrecipitation = probPrecipitation;
        this.amtRain = amtRain;
        this.time = time;
    }
}
