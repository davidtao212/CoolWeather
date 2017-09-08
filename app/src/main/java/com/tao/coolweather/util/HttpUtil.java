package com.tao.coolweather.util;

import android.text.TextUtils;

import com.google.gson.Gson;
import com.tao.coolweather.WeatherActivity;
import com.tao.coolweather.model.weather.Weather;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Created by tao on 17-9-8.
 */

public class HttpUtil {

    private static final String WEATHER_NAME = "HeWeather5";

    public static void sendOkHttpRequest(String address, okhttp3.Callback callback) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(address).build();
        client.newCall(request).enqueue(callback);
    }

    public static Weather handleWeatherResponse(String response) {
        if (TextUtils.isEmpty(response)) {
            return null;
        }
        try {
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray(WEATHER_NAME);
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent, Weather.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
