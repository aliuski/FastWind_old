package com.hardis.aki.fastwind;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Toast;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * Created by aki on 12.10.2015.
 */

public class WindSpeedView extends View {
    public static final int MEASURED_AND_FORECAST_WIND = 0;
    public static final int MEASURED_WIND = 1;
    public static final int FORECAST_WIND = 2;
    public static final int MEASURED_TEMPATURE = 3;
    public static final int FORECAST_TEMPATURE = 4;
    private static final int weathersymbolimages[][] =
            {{1, R.drawable.p1}, {21, R.drawable.p21}, {22, R.drawable.p22},
            {23, R.drawable.p23}, {2, R.drawable.p2}, {31, R.drawable.p31}, {32, R.drawable.p32}, {33, R.drawable.p33},
            {3, R.drawable.p3}, {41, R.drawable.p41}, {42, R.drawable.p42}, {43, R.drawable.p43}, {51, R.drawable.p51},
            {52, R.drawable.p52}, {53, R.drawable.p53}, {61, R.drawable.p61}, {62, R.drawable.p62}, {63, R.drawable.p63},
            {64, R.drawable.p64}, {71, R.drawable.p71}, {72, R.drawable.p72}, {73, R.drawable.p73}, {81, R.drawable.p81},
            {82, R.drawable.p82}, {83, R.drawable.p83}};
    private static final int place_colors[] = new int[]{Color.RED, Color.GREEN, Color.BLUE, Color.MAGENTA};
    private int forecast_place_colors[] = new int[]{Color.BLACK, Color.BLACK, Color.BLACK, Color.BLACK};

    private static final int MARGINALSIZE = 25;
    private static final int MARGINALSIZE2 = 50;
    private static final int RELOADTIME = 5;

    private ArrayList<WeatherData> forecast = new ArrayList<WeatherData>();
    private ArrayList<WeatherData> observations = new ArrayList<WeatherData>();
    private ArrayList<String[]> weatherplace = new ArrayList<String[]>();

    private int type = 0;
    private int sizex;
    private int sizey;
    private Paint paint;
    private Paint paintfill;
    private Context context;
    private double max = 0;
    private double min = 0;
    private String[] option_arrays;

    public WindSpeedView(Context context) {
        super(context);
        this.context = context;
        initWSV();
    }

    public WindSpeedView(Context context, AttributeSet set) {
        super(context, set);
        this.context = context;
        initWSV();
    }

    private void initWSV(){
        paint = new Paint();
        paintfill = new Paint();
        paintfill.setColor(0xFFFAFAD2);
        paintfill.setStyle(Paint.Style.FILL);
        Resources res = getResources();
        option_arrays = res.getStringArray(R.array.option_arrays);
    }

    public void setBundleData(Bundle bundle){
        if (observations.size() > 0) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(bos);

                oout.writeObject(weatherplace);
                oout.writeObject(forecast);
                oout.writeObject(observations);

                byte[] yourBytes = bos.toByteArray();
                bundle.putByteArray("savedata", yourBytes);
                oout.close();
                bos.close();

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    public void getBundleData(Bundle bundle) {
        try {
            byte sd[] = bundle.getByteArray("savedata");
            if(sd == null)
                return;
            ByteArrayInputStream bis = new ByteArrayInputStream(sd);
            ObjectInputStream ois =
                    new ObjectInputStream(bis);
            weatherplace = (ArrayList<String[]>) ois.readObject();
            forecast = (ArrayList<WeatherData>) ois.readObject();
            observations = (ArrayList<WeatherData>) ois.readObject();
            bis.close();
            ois.close();
            if(forecast.isEmpty())
                type = MEASURED_WIND;
            int count = 0;
            for (int i = 0 ; i< weatherplace.size() ; i++) {
                if(i>4)
                    break;
                if (weatherplace.get(i)[3].equals("true"))
                    forecast_place_colors[count++] = place_colors[i];
            }

            if(!observations.get(0).isUpdated(RELOADTIME))
                new WeatherWebServiceTask().execute("");

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public boolean isForecast(){
        return !forecast.isEmpty();
    }

    public void setPlace(String place){
        new WeatherWebServiceTask().execute(place);
    }

    public void setStart() {
        if (observations.size() > 0 && !observations.get(0).isUpdated(RELOADTIME))
            new WeatherWebServiceTask().execute("");
    }

    public void setDrawType(int type) {
        this.type = type;
        invalidate();
    }

    public int getDrawType() {
        return type;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        sizex = this.getWidth() - MARGINALSIZE;
        sizey = this.getHeight() - MARGINALSIZE2;
        if (sizey > sizex)
            sizey /= 3;

        paint.setTextSize(16);
        if (observations.size() > 0) {
            if(type == MEASURED_AND_FORECAST_WIND) {
                canvas.drawRect(new Rect((sizex - MARGINALSIZE) / 2 + MARGINALSIZE, MARGINALSIZE, sizex, sizey), paintfill);
                drawNowAndHistory(canvas);
            } else
                drawNormal(canvas);
        }

        paint.setColor(Color.BLACK);
        canvas.drawLines(new float[]
                {MARGINALSIZE, MARGINALSIZE, sizex, MARGINALSIZE,
                        sizex, MARGINALSIZE, sizex, sizey,
                        sizex, sizey, MARGINALSIZE, sizey,
                        MARGINALSIZE, sizey, MARGINALSIZE, MARGINALSIZE}, 0, 16, paint);
    }

    private void drawNowAndHistory(Canvas canvas) {
        int loop;
        int index;
        min = 0;

        Calendar stobservations = Calendar.getInstance();
        stobservations.setTime(forecast.get(0).getStep()[0]);
        stobservations.add(Calendar.HOUR_OF_DAY, -6);

        double tx = (double) (sizex - MARGINALSIZE) / 12.0;
        for (loop = 0; loop < 13; loop++) {
            int x = (int) ((double) loop * tx) + MARGINALSIZE;
            if (loop != 12) {
                paint.setColor(Color.GRAY);
                canvas.drawLine(x, sizey - 10, x, sizey, paint);
            }
            paint.setColor(Color.BLACK);
            canvas.drawText("" + stobservations.get(Calendar.HOUR_OF_DAY), x, sizey + 15, paint);
            stobservations.add(Calendar.HOUR_OF_DAY, 1);
            paint.setColor(Color.BLUE);
            if (loop < 6) {
                index = observations.get(0).getTempature().length - (6 * 60 / observations.get(0).minutesInCycle()) + loop * (60 / observations.get(0).minutesInCycle());
                canvas.drawText((int) (observations.get(0).getTempature()[index] + 0.5) + "°", x, sizey - MARGINALSIZE, paint);
            } else {
                index = loop - 6;
                canvas.drawText((int) (forecast.get(0).getTempature()[index] + 0.5) + "°", x, sizey - MARGINALSIZE, paint);
                drewSymbols(canvas, (int) forecast.get(0).getWindspeedwg()[index], x);
            }
        }

        stobservations.add(Calendar.HOUR_OF_DAY, -13);
        double maxtmp = -1;
        max = 0;
        int pointamount;
        for(int a=0 ; a<forecast.size() ; a++) {
            for (loop = 0; loop <= 6; loop++) {
                if (forecast.get(a).getWindspeed()[loop] > maxtmp)
                    maxtmp = forecast.get(a).getWindspeed()[loop];
            }
        }
        for(int a=0 ; a<observations.size() ; a++) {
            int oblength = observations.get(a).getStep().length;
            pointamount = pointamountcounter(observations.get(a).minutesInCycle(), observations.get(a).getStep()[oblength - 1], stobservations.getTime());
            for (loop = 1; loop <= pointamount; loop++) {
                if (observations.get(a).getWindspeed()[oblength - loop] > maxtmp)
                    maxtmp = observations.get(a).getWindspeed()[oblength - loop];
            }
        }

        while (maxtmp > max)
            max += 5;

        double ty = (double) (sizey - MARGINALSIZE) / 5.0;
        for (loop = 0; loop < 6; loop++) {
            int y = sizey - MARGINALSIZE - (int) ((double) loop * ty) + MARGINALSIZE;
            paint.setColor(Color.GRAY);
            canvas.drawLine(MARGINALSIZE, y, sizex, y, paint);
            paint.setColor(Color.BLACK);
            int ms = (int) (max / 5.0 * loop);
            canvas.drawText("" + ms, 5, y, paint);
        }

        for(int a=0 ; a<observations.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                int oblength = observations.get(a).getStep().length;
                pointamount = pointamountcounter(observations.get(a).minutesInCycle(), observations.get(a).getStep()[oblength - 1],stobservations.getTime());
                if ((observations.size() > 1) && (a < 4))
                    paint.setColor(place_colors[a]);
                else
                    paint.setColor(Color.BLACK);
                canvas.drawText(weatherplace.get(a)[0], MARGINALSIZE2, (sizey - MARGINALSIZE2) - (a * 15), paint);
                drawFigure(canvas, observations.get(a).getWindspeed(), observations.get(a).getWinddirection(), (changeindex == -1) ? (MARGINALSIZE2 + (a * 15)) : MARGINALSIZE2, pointamount, oblength - pointamount, observations.get(a).minutesInCycle());
            } else {
                paint.setColor(Color.LTGRAY);
                canvas.drawText(weatherplace.get(a)[0], MARGINALSIZE2, (sizey - MARGINALSIZE2) - (a * 15), paint);
            }
        }
        paint.setColor(Color.BLACK);
        canvas.drawText(option_arrays[type], MARGINALSIZE2, (sizey - MARGINALSIZE2) - (observations.size() * 15), paint);
        for(int a=0 ; a<forecast.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                if ((observations.size() > 1) && (a < 4))
                    paint.setColor(forecast_place_colors[a]);
                else
                    paint.setColor(Color.BLACK);
                drawFigure(canvas, forecast.get(a).getWindspeed(), forecast.get(a).getWinddirection(), (changeindex == -1) ? (MARGINALSIZE2 + (a * 15)) : MARGINALSIZE2, 7, 0, 0);
            }
        }
    }

    private int pointamountcounter(int d, Date date2, Date forecastdate){
        return (int)(date2.getTime() - forecastdate.getTime()) / (d * 60000);
    }

    private void drawNormal(Canvas canvas) {
        int loop;
        double max_tmp = -100;
        double min_tmp = 100;
        int x;

        ArrayList<WeatherData> weatherdata;
        int[] temp_place_colors;
        if (type == FORECAST_WIND || type == FORECAST_TEMPATURE) {
            weatherdata = forecast;
            temp_place_colors = forecast_place_colors;
        } else {
            weatherdata = observations;
            temp_place_colors = place_colors;
        }

        Date stept[] = weatherdata.get(0).getStep();
        int tempc = (int)(stept[stept.length-1].getTime() - stept[0].getTime()) / 39600000; // 3600000 * 11
        int tempt = stept[0].getHours();

        double kerroin = (double) (sizex - MARGINALSIZE) / (stept.length - 1);
        for (loop = 0; loop < stept.length; loop++) {
            int t = stept[loop].getHours();
            if (t % tempc == 0 && tempt != t) {
                x = (int) ((double) loop * kerroin) + MARGINALSIZE;
                if (type != MEASURED_TEMPATURE && type != FORECAST_TEMPATURE) {
                    paint.setColor(Color.BLUE);
                    canvas.drawText((int) (weatherdata.get(0).getTempature()[loop] + 0.5) + "°", x, sizey - MARGINALSIZE, paint);
                }
                paint.setColor(Color.GRAY);
                canvas.drawLine(x, sizey - 10, x, sizey, paint);
                paint.setColor(Color.BLACK);
                canvas.drawText("" + t, x, sizey + 15, paint);
                if (type == FORECAST_WIND || type == FORECAST_TEMPATURE)
                    drewSymbols(canvas, (int)weatherdata.get(0).getWindspeedwg()[loop], x);
                tempt = t;
            }
        }

        for(int a=0 ; a<weatherdata.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                double t[];
                if (type == MEASURED_TEMPATURE || type == FORECAST_TEMPATURE)
                    t = weatherdata.get(a).getTempature();
                else if (type == MEASURED_WIND)
                    t = weatherdata.get(a).getWindspeedwg();
                else
                    t = weatherdata.get(a).getWindspeed();
                for (loop = 0; loop < t.length; loop++) {
                    if (max_tmp < t[loop])
                        max_tmp = t[loop];
                    if (min_tmp > t[loop])
                        min_tmp = t[loop];
                }
            }
        }
        max = 0;
        min = 0;

        while(max_tmp > max)
    		max += 5;
    	while(min_tmp < min)
    		min -= 5;

        double ty = (double) (sizey - MARGINALSIZE) / 5.0;
        for (loop = 0; loop < 6; loop++) {
            int y = sizey - MARGINALSIZE - (int) ((double) loop * ty) + MARGINALSIZE;
            paint.setColor(Color.GRAY);
            canvas.drawLine(MARGINALSIZE, y, sizex, y, paint);
            paint.setColor(Color.BLACK);
            int ms = (int) (min + (max - min) / 5.0 * loop);
            canvas.drawText("" + ms, 5, y, paint);
        }

        for(int a=0 ; a<weatherdata.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                double t[];
                double at[];
                if (type == MEASURED_TEMPATURE || type == FORECAST_TEMPATURE) {
                    t = weatherdata.get(a).getTempature();
                    at = null;
                } else {
                    t = weatherdata.get(a).getWindspeed();
                    at = weatherdata.get(a).getWinddirection();
                }
                if ((weatherdata.size() > 1) && (a < 4))
                    paint.setColor(temp_place_colors[a]);
                else
                    paint.setColor(Color.BLACK);
                canvas.drawText(weatherplace.get(a)[0], MARGINALSIZE2, (sizey - MARGINALSIZE2) - (a * 15), paint);

                drawFigure(canvas, t, at, (changeindex == -1) ? (MARGINALSIZE2 + (a * 15)) : MARGINALSIZE2, t.length, 0, 0);
                if (type == MEASURED_WIND && (changeindex != -1 || weatherdata.size() == 1)) {
                    paint.setColor(Color.LTGRAY);
                    drawFigure(canvas, weatherdata.get(a).getWindspeedwg(), null, 0, weatherdata.get(a).getWindspeedwg().length, 0, 0);
                }
            } else {
                paint.setColor(Color.LTGRAY);
                canvas.drawText(weatherplace.get(a)[0], MARGINALSIZE2, (sizey - MARGINALSIZE2) - (a * 15), paint);
            }
        }
        paint.setColor(Color.BLACK);
        canvas.drawText(option_arrays[type], MARGINALSIZE2, (sizey - MARGINALSIZE2) - (weatherdata.size() * 15), paint);
    }

    int changeindex = -1;

    public void changeIndex(boolean up){
        int size;
        if (type == FORECAST_WIND || type == FORECAST_TEMPATURE)
            size = forecast.size();
        else
            size = observations.size();
        if(size > 1){
            if(up){
                changeindex++;
                if(changeindex >= size)
                    changeindex = -1;
            } else {
                changeindex--;
                if(changeindex < -1)
                    changeindex = size-1;
            }
            invalidate();
        }
    }

    private void drewAngle(Canvas canvas, double winddirection, int tmpx, int tmpy){
        int yc = (int) (10.0 * Math.sin(winddirection * Math.PI / 180.0));
        int xc = (int) (10.0 * Math.cos(winddirection * Math.PI / 180.0));
        canvas.drawLine(tmpx + xc, tmpy + yc, tmpx - xc, tmpy - yc, paint);
        int yc1 = (int) (6.0 * Math.sin((winddirection + 45.0) * Math.PI / 180.0));
        int xc1 = (int) (6.0 * Math.cos((winddirection + 45.0) * Math.PI / 180.0));
        int yc2 = (int) (6.0 * Math.sin((winddirection - 45.0) * Math.PI / 180.0));
        int xc2 = (int) (6.0 * Math.cos((winddirection - 45.0) * Math.PI / 180.0));
        canvas.drawLine(tmpx + xc, tmpy + yc, tmpx + xc1, tmpy + yc1, paint);
        canvas.drawLine(tmpx + xc, tmpy + yc, tmpx + xc2, tmpy + yc2, paint);
    }

    private void drewSymbols(Canvas canvas, int weathersymbol, int tmpx){
        int id = R.drawable.sumu;
        for (int i = 0; i < weathersymbolimages.length; i++) {
            if (weathersymbolimages[i][0] == weathersymbol) {
                id = weathersymbolimages[i][1];
                break;
            }
        }
        Bitmap mBitmap = BitmapFactory.decodeResource(getResources(), id);
        Bitmap sBitmap = Bitmap.createScaledBitmap(mBitmap, 20, 20, true);
        canvas.drawBitmap(sBitmap, tmpx - 10, sizey + 15, paint);
    }

    private void drawFigure(Canvas canvas, double wave[], double angle[], int angley, int points, int offset, int minutesInCycle){
        int siirra;
        double kerroin;
        int loop;

        if(wave.length == points) { // Normal cases
            siirra = MARGINALSIZE;
            kerroin = (double) (sizex - MARGINALSIZE) / (points - 1);
        } else if(offset != 0) { // Show observation figure.
            siirra = MARGINALSIZE;
            kerroin = (double) (sizex - MARGINALSIZE) / 700.0 * (double) minutesInCycle; // 2/35
        } else { // Show forecast figure.
            siirra = (sizex - MARGINALSIZE) / 2 + MARGINALSIZE;
            kerroin = ((double) (sizex - MARGINALSIZE) / 6) / 2.0;
        }

        int dd = (int) ((double) (sizey - MARGINALSIZE) * min / (max - min));
        float xyw[] = new float[(points - 1) * 4];
        int l2 = 0;
        for (loop = 0; loop < points - 1; loop++) {
            xyw[l2++] = (int) ((double) loop * kerroin) + siirra;
            xyw[l2++] = sizey - (int) ((double) (sizey - MARGINALSIZE) * wave[loop + offset] / (max - min)) + dd;
            xyw[l2++] = (int) ((double) (loop + 1) * kerroin) + siirra;
            xyw[l2++] = sizey - (int) ((double) (sizey - MARGINALSIZE) * wave[loop + offset + 1] / (max - min)) + dd;
        }
        canvas.drawLines(xyw, 0, xyw.length, paint);
        if (offset > 0)
            canvas.drawText("" + wave[wave.length - 1], xyw[xyw.length - 2], xyw[xyw.length - 1], paint);

        if(angle != null) {
            double length = (double) (xyw[xyw.length - 2] - siirra);
            int pointa = sizey / ((angle.length == points) ? 15 : 30);
            for (loop = 0; loop <= pointa; loop++) {
                double ai = length / (double)pointa * (double)loop;
                int index = (int) ((ai / length * (double) (points - 1)) + 0.5);
                drewAngle(canvas, angle[index + offset], (int)ai + siirra, angley);
            }
        }
    }

    private class WeatherWebServiceTask extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;

        @Override
        protected void onPreExecute()
        {
            changeindex = -1;
            progressDialog = new ProgressDialog(context);
            progressDialog.setMax(100);
            progressDialog.setMessage(getResources().getString(R.string.download_text));
            progressDialog.setTitle(getResources().getString(R.string.app_name));
            progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progressDialog.show();
        };

        protected String doInBackground(String... urls) {
            return readXMLdata(urls[0]);
        }

        protected void onPostExecute(String result) {
            try {
                if (progressDialog != null)
                    progressDialog.dismiss();
                if (result.equals("OK"))
                    invalidate();
                else
                    Toast.makeText(context, R.string.error_download_text, Toast.LENGTH_SHORT).show();
            }catch (Exception e){
            }
        }

    private String readXMLdata(String place) {
        try {
            int forecast_count = 0;

            if(place.length() > 0) {
                weatherplace.clear();
                String[] row = place.split("\n");
                for (int i = 0; i < row.length; i++) {
                    String tmp[] = row[i].split(";");
                    weatherplace.add(tmp);
                    if(tmp[3].equals("true")) {
                        if (forecast_count < 5)
                            forecast_place_colors[forecast_count] = place_colors[i];
                        forecast_count++;
                    }
                }
            }

            int prosent = 100 / (weatherplace.size() + forecast_count);

            observations.clear();
            for (String[] str : weatherplace) {
                observations.add(readWeather("http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::weather::timevaluepair&fmisid="
                        + str[1] + "&parameters=windspeedms,WindDirection,wg_10min,temperature"));
                progressDialog.incrementProgressBy(prosent);
                type = MEASURED_WIND;
            }
            if(place.length() > 0 || forecast.size() == 0 || forecast.get(0).getStep()[0].before(observations.get(0).getStep()[observations.get(0).getStep().length-1])){
                forecast.clear();
                for (String[] str : weatherplace) {
                    if(str[3].equals("true")) {
                        forecast.add(readWeather("http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::timevaluepair&place="
                                + str[0] + "&parameters=windspeedms,WindDirection,weathersymbol3,temperature"));
                        type = 0;
                        progressDialog.incrementProgressBy(prosent);
                    }
                }
            } else
                progressDialog.incrementProgressBy(prosent * weatherplace.size());

        } catch (Exception e) {
            e.printStackTrace();
            return "FALSE";
        }
        return "OK";
    }

    private WeatherData readWeather(String input) throws Exception{

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();

        Document doc = db.parse(input);

        doc.getDocumentElement().normalize();
        NodeList nodeLst = doc.getElementsByTagName("wfs:member");
        Vector time = new Vector();
        Vector v1 = readMember((Element) nodeLst.item(0),time);
        Vector v2 = readMember((Element) nodeLst.item(1),null);
        Vector v3 = readMember((Element) nodeLst.item(2),null);
        Vector v4 = readMember((Element) nodeLst.item(3),null);
        return new WeatherData(time,v1,v2,v3,v4);
    }

    private Vector readMember(Element element, Vector time) {
        Vector v = new Vector();
        NodeList nodeLst = element.getElementsByTagName("omso:PointTimeSeriesObservation");
        element = (Element) nodeLst.item(0);
        nodeLst = element.getElementsByTagName("om:result");
        element = (Element) nodeLst.item(0);
        nodeLst = element.getElementsByTagName("wml2:MeasurementTimeseries");
        element = (Element) nodeLst.item(0);

        NodeList nodeLst1 = element.getElementsByTagName("wml2:point");

        for (int k = 0; k < nodeLst1.getLength(); k++) {
            Element element1 = (Element) nodeLst1.item(k);

            NodeList nodeLst2 = element1.getElementsByTagName("wml2:MeasurementTVP");
            Element element2 = (Element) nodeLst2.item(0);

            if(time != null) {
                nodeLst2 = element2.getElementsByTagName("wml2:time");
                element1 = (Element) nodeLst2.item(0);
                nodeLst2 = element1.getChildNodes();
                time.add(((Node) nodeLst2.item(0)).getNodeValue());
            }
            nodeLst2 = element2.getElementsByTagName("wml2:value");
            element1 = (Element) nodeLst2.item(0);
            nodeLst2 = element1.getChildNodes();

            v.add(((Node) nodeLst2.item(0)).getNodeValue());
        }
        return v;
    }
}

}
