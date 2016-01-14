package com.hardis.aki.fastwind;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.Vector;

/**
 * Created by aki on 14.10.2015.
 */
public class WeatherData implements Serializable {

    private Date step[] = null;
    private double windspeed[] = null;
    private double tempature[] = null;
    private double winddirection[] = null;
    private double windspeedwg[] = null;
    private int minutesincycle;
    private Date updated;

    public WeatherData(Vector time,Vector v1,Vector v2,Vector v3,Vector v4){
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:SS");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        step = new Date[v1.size()];
        windspeed = new double[v1.size()];
        tempature = new double[v1.size()];
        winddirection = new double[v1.size()];
        windspeedwg = new double[v1.size()];

        for (int loop = 0; loop < v1.size(); loop++) {
            try{
                step[loop] = df.parse((String)time.get(loop));
            } catch (Exception e) {
                e.printStackTrace();
            }
            windspeed[loop] = Double.parseDouble((String) v1.get(loop));
            winddirection[loop] = Double.parseDouble((String) v2.get(loop)) + 90.0;
            windspeedwg[loop] = Double.parseDouble((String) v3.get(loop));
            tempature[loop] = Double.parseDouble((String) v4.get(loop));
        }

        minutesincycle = (int)(step[1].getTime() - step[0].getTime()) / 60000;
        updated = new java.util.Date();
    }

    public Date[] getStep(){
        return step;
    }
    public double[] getWindspeed(){
        return windspeed;
    }
    public double[] getTempature(){
        return tempature;
    }
    public double[] getWinddirection(){
        return winddirection;
    }
    public double[] getWindspeedwg(){
        return windspeedwg;
    }
    public int minutesInCycle() { return minutesincycle;}

    public boolean isUpdated(int minute){
        Date d = new java.util.Date();
        if(updated == null || (d.getTime() > updated.getTime() + minute * 60000)) {
            return false;
        }
        return true;
    }
}
