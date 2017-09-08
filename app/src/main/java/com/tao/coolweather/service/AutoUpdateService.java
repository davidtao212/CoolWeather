package com.tao.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.tao.coolweather.WeatherActivity;
import com.tao.coolweather.model.weather.Weather;
import com.tao.coolweather.util.HttpUtil;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {

    private static final long UPDATE_INTERVAL = 8 * 60 * 60 * 1000;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();

        long triggerAtTime = SystemClock.elapsedRealtime() + UPDATE_INTERVAL;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, i, 0);
        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    private void updateWeather() {
        String weatherString = getSharedPrefs().getString(WeatherActivity.PREFS_KEY, null);

        if (weatherString != null) {
            Weather weather = HttpUtil.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String address = WeatherActivity.END_POINT + "weather?city=" + weatherId + "&key=" + WeatherActivity.API_KEY;
            HttpUtil.sendOkHttpRequest(address, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String body = response.body().string();
                    Weather weather = HttpUtil.handleWeatherResponse(body);
                    if (weather != null && "ok".equals(weather.status)) {
                        getSharedPrefsEditor().putString(WeatherActivity.PREFS_KEY, body).apply();
                    }
                }

                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void updateBingPic() {
        HttpUtil.sendOkHttpRequest(WeatherActivity.PIC_END_POINT, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String picUrl = response.body().string();
                getSharedPrefsEditor().putString("bing_pic", picUrl)
                                      .putLong("bing_pic_date", System.currentTimeMillis())
                                      .apply();
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this);
    }

    private SharedPreferences.Editor getSharedPrefsEditor() {
        return PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
    }
}
