package com.hardis.aki.fastwind.datasource;

import java.io.Serializable;
import java.util.Date;

public class WeatherData implements Serializable {
    protected Date step[] = null;
    protected double windspeed[] = null;
    protected double tempature[] = null;
    protected int winddirection[] = null;
    protected double windspeedwg[] = null;
    protected int minutesincycle;
    protected Date updated;

    public WeatherData(){};

    public Date[] getStep(){
        return step;
    }
    public double[] getWindspeed(){
        return windspeed;
    }
    public double[] getTempature(){
        return tempature;
    }
    public int[] getWinddirection(){
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
