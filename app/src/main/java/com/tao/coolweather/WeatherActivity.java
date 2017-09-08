package com.tao.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.tao.coolweather.model.weather.Forecast;
import com.tao.coolweather.model.weather.Weather;
import com.tao.coolweather.util.HttpUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    public static final String END_POINT = "https://free-api.heweather.com/v5/";

//    private static final String MY_KEY = "eb9bb243993047f6aea266910fa23275";
    private static final String KEY = "bc0418b57b2d4918819d3974ac1285d9";

    public static final String WEATHER_NAME = "HeWeather5";

    private static final String DATE_PATTERN = "yyyy-MM-dd HH:mm";

    private static final long REFRESH_INTERVAL = 2 * 60 * 60 * 1000; // 2 hours

    private static final String PIC_END_POINT = "http://guolin.tech/api/bing_pic";

    private static final long PIC_REFRESH_INTERVAL = 24 * 60 * 60 * 1000; // 1 day

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_weather);
        initializeControls();

        loadWeather();
        loadBingPic();
    }

    private void initializeControls() {
        weatherLayout = findViewById(R.id.weather_layout);
        titleCity = findViewById(R.id.title_city);
        titleUpdateTime = findViewById(R.id.title_update_time);
        degreeText = findViewById(R.id.degree_text);
        weatherInfoText = findViewById(R.id.weather_info_text);
        forecastLayout = findViewById(R.id.forecast_layout);
        aqiText = findViewById(R.id.aqi_text);
        pm25Text = findViewById(R.id.pm25_text);
        comfortText = findViewById(R.id.comfort_text);
        carWashText = findViewById(R.id.car_wash_text);
        sportText = findViewById(R.id.sport_text);
        bingImage = findViewById(R.id.bing_image);

        if (Build.VERSION.SDK_INT >= 21) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }

    private void loadWeather() {
        String weatherString = getSharedPrefs().getString(WEATHER_NAME, null);
        Weather cachedWeather = handleWeatherResponse(weatherString);
        String weatherId = null;
        boolean needRefresh = true;

        if (cachedWeather != null) {
            weatherId = cachedWeather.basic.weatherId;
            SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_PATTERN);
            try {
                long last = dateFormat.parse(cachedWeather.basic.update.updateTime).getTime();
                long current = System.currentTimeMillis();
                if (current - last < REFRESH_INTERVAL) {
                    needRefresh = false;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        } else {
            weatherId = getIntent().getStringExtra("weather_id");
        }

        if (needRefresh) {
            weatherLayout.setVisibility(View.INVISIBLE);
            requestWeather(weatherId);
        } else {
            showWeatherInfo(cachedWeather);
        }
    }

    private void requestWeather(String weatherId) {
        String weatherUrl = END_POINT + "weather?city=" + weatherId + "&key=" + KEY;
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Weather weather = handleWeatherResponse(body);
                boolean success = weather != null && "ok".equals(weather.status);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (success) {
                            getSharedPrefsEditor().putString(WEATHER_NAME, body).apply();
                            showWeatherInfo(weather);
                        } else {
                            showErrorMessage();
                        }
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showErrorMessage();
                    }
                });
            }
        });
    }

    private Weather handleWeatherResponse(String response) {
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

    private void showWeatherInfo(Weather weather) {
        titleCity.setText(weather.basic.cityName);
        titleUpdateTime.setText(weather.basic.update.updateTime.split(" ")[1]);
        degreeText.setText(weather.now.temperature + "℃");
        weatherInfoText.setText(weather.now.more.info);

        forecastLayout.removeAllViews();
        for (Forecast forecast : weather.dailyForecast) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = view.findViewById(R.id.date_text);
            TextView infoText = view.findViewById(R.id.info_text);
            TextView maxText = view.findViewById(R.id.max_text);
            TextView minText = view.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(view);
        }

        if (weather.aqi != null) {
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        comfortText.setText("舒适度：" + weather.suggestion.comfort.info);
        carWashText.setText("洗车指数：" + weather.suggestion.carWash.info);
        sportText.setText("运动建议：" + weather.suggestion.sport.info);
        weatherLayout.setVisibility(View.VISIBLE);
    }

    private void showErrorMessage() {
        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
    }

    private void loadBingPic() {
        String picUrl = getSharedPrefs().getString("bing_pic", null);
        long last = getSharedPrefs().getLong("bing_pic_date", 0);
        long current = System.currentTimeMillis();

        if (picUrl == null || current - last > PIC_REFRESH_INTERVAL) {
            requestBingPic();
        } else {
            Glide.with(this).load(picUrl).into(bingImage);
        }
    }

    private void requestBingPic() {
        HttpUtil.sendOkHttpRequest(PIC_END_POINT, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String picUrl = response.body().string();
                getSharedPrefsEditor().putString("bing_pic", picUrl)
                                      .putLong("bing_pic_date", System.currentTimeMillis())
                                      .apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(picUrl).into(bingImage);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }
        });
    }

    private SharedPreferences getSharedPrefs() {
        return PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this);
    }

    private SharedPreferences.Editor getSharedPrefsEditor() {
        return PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
    }
}
