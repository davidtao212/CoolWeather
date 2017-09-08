package com.tao.coolweather.model.weather;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by tao on 17-9-8.
 */

public class Weather {

    public String status;

    public Basic basic;

    public AQI aqi;

    public Now now;

    public Suggestion suggestion;

    @SerializedName("daily_forecast")
    public List<Forecast> dailyForecast;

}
