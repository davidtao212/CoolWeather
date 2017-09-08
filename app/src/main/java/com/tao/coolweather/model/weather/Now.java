package com.tao.coolweather.model.weather;

import com.google.gson.annotations.SerializedName;

/**
 * Created by tao on 17-9-8.
 */

public class Now {

    @SerializedName("tmp")
    public String temperature;

    @SerializedName("cond")
    public More more;

    public class More {

        @SerializedName("txt")
        public String info;

    }
}
