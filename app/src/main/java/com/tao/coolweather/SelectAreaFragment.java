package com.tao.coolweather;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tao.coolweather.model.Area;
import com.tao.coolweather.model.City;
import com.tao.coolweather.model.County;
import com.tao.coolweather.model.Province;
import com.tao.coolweather.util.HttpUtil;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * Created by tao on 17-9-8.
 */

public class SelectAreaFragment extends Fragment {

    public static final String END_POINT = "http://guolin.tech/api/china/";

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private int currentLevel;

    private ProgressBar progressBar;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    private List<Province> provinceList;

    private List<City> cityList;

    private List<County> countyList;

    private Province selectedProvince;

    private City selectedCity;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.select_area, container, false);
        progressBar = view.findViewById(R.id.progress_bar);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(i);
                    queryCitiesOf(selectedProvince);
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(i);
                    queryCountiesOf(selectedCity);
                } else if (currentLevel == LEVEL_COUNTY) {
                    // TODO: start weather activity
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCitiesOf(selectedProvince);
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });

        queryProvinces();
    }

    private void queryProvinces() {
        currentLevel = LEVEL_PROVINCE;
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        if (provinceList == null) {
            provinceList = DataSupport.findAll(Province.class);
        }
        if (provinceList.size() > 0) {
            updateDataListWith(provinceList);
        } else {
            queryFromServer(currentLevel, "");
        }
    }

    private void queryCitiesOf(Province province) {
        currentLevel = LEVEL_CITY;
        titleText.setText(province.getAreaName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceid = ?", String.valueOf(province.getAreaId())).find(City.class);

        if (cityList.size() > 0) {
            updateDataListWith(cityList);
        } else {
            String path = String.valueOf(province.getAreaId());
            queryFromServer(currentLevel, path);
        }
    }

    private void queryCountiesOf(City city) {
        currentLevel = LEVEL_COUNTY;
        titleText.setText(city.getAreaName());
        backButton.setVisibility(View.VISIBLE);
        countyList = DataSupport.where("cityid = ?", String.valueOf(city.getAreaId())).find(County.class);

        if (countyList.size() > 0) {
            updateDataListWith(countyList);
        } else {
            String path = String.valueOf(city.getProvinceId()) + "/" + String.valueOf(city.getAreaId());
            queryFromServer(currentLevel, path);
        }
    }

    private void queryFromServer(final int level, final String path) {
        progressBar.setVisibility(View.VISIBLE);
        String address = END_POINT + path;
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String body = response.body().string();
                Gson gson = new Gson();
                final List<? extends Area> refreshedList;
                if (level == LEVEL_PROVINCE) {
                    provinceList = gson.fromJson(body, new TypeToken<List<Province>>() {}.getType());
                    DataSupport.saveAll(provinceList);
                    refreshedList = provinceList;
                } else if (level == LEVEL_CITY) {
                    int provinceId = Integer.valueOf(path);
                    cityList = gson.fromJson(body, new TypeToken<List<City>>() {}.getType());
                    cityList.forEach(city -> city.setProvinceId(provinceId));
                    DataSupport.saveAll(cityList);
                    refreshedList = cityList;
                } else if (level == LEVEL_COUNTY) {
                    int cityId = Integer.valueOf(path.substring(path.lastIndexOf('/') + 1));
                    countyList = gson.fromJson(body, new TypeToken<List<County>>() {}.getType());
                    countyList.forEach(county -> county.setCityId(cityId));
                    DataSupport.saveAll(countyList);
                    refreshedList = countyList;
                } else {
                    refreshedList = null;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        updateDataListWith(refreshedList);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateDataListWith(List<? extends Area> areaList) {
        dataList.clear();;
        for (Area area : areaList) {
            dataList.add(area.getAreaName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
    }
}
