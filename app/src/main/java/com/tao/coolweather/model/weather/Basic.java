package com.tao.coolweather.model.weather;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tao on 17-9-8.
 */

public class Basic {

    @SerializedName("city")
    public String cityName;

    @SerializedName("id")
    public String weatherId;

    public Update update;

    public class Update {

        @SerializedName("loc")
        public String updateTime;

    }
}
