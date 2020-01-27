package com.hardis.aki.fastwind.datasource;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class WindGuru extends WeatherData {
    public WindGuru(String urlstr) throws Exception{
        readWindguru(urlstr);
    }

    public void readWindguru(String urlstr) throws Exception {
        URLConnection urlConn = null;
        BufferedReader bufferedReader = null;

        URL url = new URL(urlstr);
        urlConn = url.openConnection();
        bufferedReader = new BufferedReader(new InputStreamReader(urlConn.getInputStream()));

        StringBuffer stringBuffer = new StringBuffer();
        String line;
        while ((line = bufferedReader.readLine()) != null)
        {
            stringBuffer.append(line);
        }
        parseJson(stringBuffer.toString());
        bufferedReader.close();
        updated = new java.util.Date();
    }

    private double knotsToMS(double knots){
        return (double)((int)(knots * 5.144 + 0.5)) / 10.0;
    }

    private void parseJson(String json) throws Exception {
        JSONObject jObject = new JSONObject(json);
        JSONArray unixtimeArray = jObject.getJSONArray("unixtime");
        JSONArray wind_avgArray = jObject.getJSONArray("wind_avg");
        JSONArray windspeedwgArray = jObject.getJSONArray("wind_max");
        JSONArray wind_directionArray = jObject.getJSONArray("wind_direction");
        step = new Date[unixtimeArray.length()];
        windspeed = new double[unixtimeArray.length()];
        windspeedwg = new double[unixtimeArray.length()];
        winddirection = new int[unixtimeArray.length()];

        for (int i=0; i < unixtimeArray.length(); i++)
        {
            step[i] = new Date(unixtimeArray.getLong(i) * 1000);
            windspeed[i] = knotsToMS(wind_avgArray.getDouble(i));
            windspeedwg[i] = knotsToMS(windspeedwgArray.getDouble(i));
            winddirection[i] = wind_directionArray.getInt(i);
        }
    }
}
