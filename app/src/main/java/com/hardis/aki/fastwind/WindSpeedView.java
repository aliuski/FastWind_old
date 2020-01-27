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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import com.hardis.aki.fastwind.datasource.WeatherData;
import com.hardis.aki.fastwind.datasource.Fmi;
import com.hardis.aki.fastwind.datasource.WindGuru;

/**
 * Created by aki on 12.10.2015.
 */

public class WindSpeedView extends View {
    public static final int MEASURED_WIND = 0;
    public static final int MEASURED_AND_FORECAST_WIND = 1;
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
    private int forecast_place_colors[] = new int[]{Color.WHITE, Color.WHITE, Color.WHITE, Color.WHITE};
    private static final long HOUR12 = 3600*12000;

    private static final int MARGINALSIZE = 25;
    private static final int MARGINALSIZE2 = 50;
    private static final int RELOADTIME = 5;

    private ArrayList<WeatherData> forecast = new ArrayList<WeatherData>();
    private ArrayList<WeatherData> observations = new ArrayList<WeatherData>();
    private ArrayList<String[]> weatherplace = new ArrayList<String[]>();

    private Date startDate = new Date();
    private Date endDate = new Date();
    private int type = 0;
    private int sizex;
    private int sizey;
    private Paint paint;
    private Paint paintfill;
    private Context context;
    private double max = 0;
    private double min = 0;
    private String[] option_arrays;
    private int changeindex = -1;
    private SimpleDateFormat dateFormat;
    private SimpleDateFormat fmiformat;
    private SimpleDateFormat windguruformat;

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
        paintfill.setColor(Color.rgb(0,30,20));
        paintfill.setStyle(Paint.Style.FILL);
        Resources res = getResources();
        option_arrays = res.getStringArray(R.array.option_arrays);
        dateFormat = new SimpleDateFormat("dd'.'MM'.'");
        fmiformat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm':00Z'");
        fmiformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        windguruformat = new SimpleDateFormat("yyyy-MM-dd'+'HH:mm':00'");
    }

    public void setBundleData(Bundle bundle){
        if (observations.size() > 0) {
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oout = new ObjectOutputStream(bos);

                oout.writeObject(new Integer(type));
                oout.writeObject(startDate);
                oout.writeObject(endDate);
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

            type = ((Integer) ois.readObject()).intValue();
            startDate = (Date) ois.readObject();
            endDate = (Date) ois.readObject();
            weatherplace = (ArrayList<String[]>) ois.readObject();
            forecast = (ArrayList<WeatherData>) ois.readObject();
            observations = (ArrayList<WeatherData>) ois.readObject();
            bis.close();
            ois.close();
            if(forecast.isEmpty() && type >= 0)
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
        forecast.clear();
        new WeatherWebServiceTask().execute(place);
    }

    public void setStart() {
        if (observations.size() > 0 && !observations.get(0).isUpdated(RELOADTIME)) {
            type = MEASURED_WIND;
            new WeatherWebServiceTask().execute("");
        }
    }

    public void setDrawType(int newtype) {
        if(newtype < 0 || (newtype == 0 && this.type < 0))
            new WeatherWebServiceTask().execute("");
        this.type = newtype;
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

        paint.setColor(Color.WHITE);
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
        Date this_time = stobservations.getTime();
        stobservations.add(Calendar.HOUR_OF_DAY, -6);
        Date start_point = stobservations.getTime();

        double tx = (double) (sizex - MARGINALSIZE) / 12.0;
        for (loop = 0; loop < 13; loop++) {
            int x = (int) ((double) loop * tx) + MARGINALSIZE;
            if (loop != 12) {
                paint.setColor(Color.GRAY);
                canvas.drawLine(x, sizey - 10, x, sizey, paint);
            }
            paint.setColor(Color.WHITE);
            canvas.drawText("" + stobservations.get(Calendar.HOUR_OF_DAY), x, sizey + 15, paint);
            paint.setColor(Color.BLUE);
            if(loop>2 || this.getWidth() < this.getHeight()) {
                if (loop < 6) {
                    if (observations.get(0).getTempature() != null) {
                        canvas.drawText((int) (observations.get(0).getTempature()[observations.get(0).getIndex(stobservations.getTime())] + 0.5) + "°", x, sizey - MARGINALSIZE, paint);
                    }
                } else {
                    index = loop - 6;
                    canvas.drawText((int) (forecast.get(0).getTempature()[index] + 0.5) + "°", x, sizey - MARGINALSIZE, paint);
                    drewSymbols(canvas, (int) forecast.get(0).getWindspeedwg()[index], x);
                }
            }
            stobservations.add(Calendar.HOUR_OF_DAY, 1);
        }
        Date end_time = stobservations.getTime();

        stobservations.add(Calendar.HOUR_OF_DAY, -13);
        double maxtmp = -1;
        max = 0;
        for(int a=0 ; a<forecast.size() ; a++) {
            for (loop = 0 ; loop < forecast.get(a).getIndex(end_time) ; loop++) {
                if (forecast.get(a).getWindspeed()[loop] > maxtmp)
                    maxtmp = forecast.get(a).getWindspeed()[loop];
            }
        }
        for(int a=0 ; a<observations.size() ; a++) {
            for (loop = observations.get(a).getIndex(start_point) ; loop < observations.get(a).getStep().length ; loop++) {
                if (observations.get(a).getWindspeed()[loop] > maxtmp)
                    maxtmp = observations.get(a).getWindspeed()[loop];
            }
        }

        while (maxtmp > max)
            max += 5;

        double ty = (double) (sizey - MARGINALSIZE) / 5.0;
        for (loop = 0; loop < 6; loop++) {
            int y = sizey - MARGINALSIZE - (int) ((double) loop * ty) + MARGINALSIZE;
            paint.setColor(Color.GRAY);
            canvas.drawLine(MARGINALSIZE, y, sizex, y, paint);
            paint.setColor(Color.WHITE);
            int ms = (int) (max / 5.0 * loop);
            canvas.drawText("" + ms, 5, y, paint);
        }

        int test_place;
        int text_size;
        if(this.getWidth() > this.getHeight()) {
            test_place = sizey - MARGINALSIZE2;
            text_size = 18;
        } else {
            test_place = this.getHeight() / 2;
            paint.setTextSize(30);
            text_size = 34;
        }
        for(int a=0 ; a<observations.size() ; a++) {
            int last = observations.get(a).getWindspeed().length - 1;
            if(changeindex == -1 || changeindex == a) {
                if ((observations.size() > 1) && (a < 4))
                    paint.setColor(place_colors[a]);
                else
                    paint.setColor(Color.WHITE);
                drawFigure(canvas, start_point, this_time, observations.get(a).getStep(), observations.get(a).getWindspeed(), observations.get(a).getWinddirection(), (changeindex == -1) ? (MARGINALSIZE2 + (a * 15)) : MARGINALSIZE2,1);
            } else {
                paint.setColor(Color.LTGRAY);
            }
            String plasetext = weatherplace.get(a)[0]+" "+observations.get(a).getWindspeed()[last] + " m/s " + observations.get(a).getWinddirection()[last] + " º";
            canvas.drawText(plasetext, MARGINALSIZE2, test_place - (a * text_size), paint);
        }
        paint.setColor(Color.WHITE);
        canvas.drawText(option_arrays[type], MARGINALSIZE2, test_place - (observations.size() * text_size), paint);
        paint.setTextSize(16);
        for(int a=0 ; a<forecast.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                if ((observations.size() > 1) && (a < 4))
                    paint.setColor(forecast_place_colors[a]);
                else
                    paint.setColor(Color.WHITE);
                drawFigure(canvas, this_time, end_time, forecast.get(a).getStep(), forecast.get(a).getWindspeed(), forecast.get(a).getWinddirection(), (changeindex == -1) ? (MARGINALSIZE2 + (a * 15)) : MARGINALSIZE2, 2);
            }
        }
    }

    private void drawNormal(Canvas canvas) {
        int loop;
        double max_tmp = -100;
        double min_tmp = 100;
        int x;
        Date start_point;
        Date end_point;
        int move = 0;
        int move_step = 0;
        ArrayList<WeatherData> weatherdata;
        Calendar timestep = Calendar.getInstance();
        double kerroin = (double) (sizex - MARGINALSIZE) / 12;

        int[] temp_place_colors;
        if (type == FORECAST_WIND || type == FORECAST_TEMPATURE) {
            kerroin = (double) sizex / 12.0;
            weatherdata = forecast;
            temp_place_colors = forecast_place_colors;
            start_point = weatherdata.get(0).getStep()[0];
            end_point = weatherdata.get(0).getStep()[weatherdata.get(0).getStep().length-1];
            timestep.setTime(weatherdata.get(0).getStep()[0]);
            int test = timestep.get(Calendar.HOUR_OF_DAY);
            double d = (double)test / 3.0;
            int i = (int)d;
            int b = 3 - (int)((d - (double)i) * 3.0 + 0.1);
            move = (int)(((double)b/3.0) * kerroin);
            if(b > 0)
                timestep.add(Calendar.HOUR_OF_DAY, b);
            move_step = 3;
        } else {
            weatherdata = observations;
            temp_place_colors = place_colors;
            start_point = startDate;
            end_point = endDate;
            timestep.setTime(start_point);
            timestep.add(Calendar.HOUR_OF_DAY, 1);
            move = (int)(kerroin - ((double)timestep.get(Calendar.MINUTE)/60.0) * kerroin);
            move_step = 1;
        }

        for (loop = 0; loop < 12; loop++) {
            int tempindex = weatherdata.get(0).getIndex(timestep.getTime());
            x = (int) ((double) loop * kerroin) + move + MARGINALSIZE;
            if (type != MEASURED_TEMPATURE && type != FORECAST_TEMPATURE && weatherdata.get(0).getTempature() != null && (loop>2 || this.getWidth() < this.getHeight())) {
                paint.setColor(Color.BLUE);
                canvas.drawText((int) (weatherdata.get(0).getTempature()[tempindex] + 0.5) + "°", x, sizey - MARGINALSIZE, paint);
            }
            paint.setColor(Color.GRAY);
            canvas.drawLine(x, sizey - 10, x, sizey, paint);
            paint.setColor(Color.WHITE);
            canvas.drawText("" + timestep.get(Calendar.HOUR_OF_DAY), x, sizey + 15, paint);
            if (type == FORECAST_WIND || type == FORECAST_TEMPATURE)
                drewSymbols(canvas, (int)weatherdata.get(0).getWindspeedwg()[tempindex], x);
            timestep.add(Calendar.HOUR_OF_DAY, move_step);
        }

        for(int a=0 ; a<weatherdata.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                double t[];
                if (type == MEASURED_TEMPATURE || type == FORECAST_TEMPATURE) {
                    t = weatherdata.get(a).getTempature();
                    if(t == null)
                        continue;
                } else if (type <= 0)
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
            int y = sizey - (int) ((double) loop * ty);
            paint.setColor(Color.GRAY);
            canvas.drawLine(MARGINALSIZE, y, sizex, y, paint);
            paint.setColor(Color.WHITE);
            int ms = (int) (min + (max - min) / 5.0 * loop);
            canvas.drawText("" + ms, 5, y, paint);
        }
        if(min < 0 && max > 0){
            int y = sizey + (int)((min / (max - min)) * (double)sizey) + MARGINALSIZE/2;
            canvas.drawText("0", 5, y, paint);
            paint.setColor(Color.GRAY);
            canvas.drawLine(MARGINALSIZE, y, sizex, y, paint);
        }
        int test_place;
        int text_size;
        if(this.getWidth() > this.getHeight()) {
            test_place = sizey - MARGINALSIZE2;
            text_size = 18;
        } else {
            test_place = this.getHeight() / 2;
            paint.setTextSize(30);
            text_size = 34;
        }

        for(int a=0 ; a<weatherdata.size() ; a++) {
            if(changeindex == -1 || changeindex == a) {
                double t[];
                int at[];
                if (type == MEASURED_TEMPATURE || type == FORECAST_TEMPATURE) {
                    t = weatherdata.get(a).getTempature();
                    if(t == null)
                        continue;
                    at = null;
                } else {
                    t = weatherdata.get(a).getWindspeed();
                    at = weatherdata.get(a).getWinddirection();
                }
                if (type <= 0 && (changeindex != -1 || weatherdata.size() == 1)) {
                    paint.setColor(Color.LTGRAY);
                    drawFigure(canvas, start_point, end_point, weatherdata.get(a).getStep(), weatherdata.get(a).getWindspeedwg(), null, 0,0);
                }
                if ((weatherdata.size() > 1) && (a < 4))
                    paint.setColor(temp_place_colors[a]);
                else
                    paint.setColor(Color.WHITE);
                drawFigure(canvas, start_point, end_point, weatherdata.get(a).getStep(), t, at, (changeindex == -1) ? (MARGINALSIZE2 + (a * 15)) : MARGINALSIZE2, 0);
            } else {
                paint.setColor(Color.LTGRAY);
            }
            String plasetext = weatherplace.get(a)[0];
            if(type >= 0) {
                int last = observations.get(a).getWindspeed().length - 1;
                plasetext +=" "+observations.get(a).getWindspeed()[last] + " m/s " + observations.get(a).getWinddirection()[last] + " º";
            }
            canvas.drawText(plasetext, MARGINALSIZE2, test_place - (a * text_size), paint);
        }
        paint.setColor(Color.WHITE);
        canvas.drawText(type < 0 ? (option_arrays[0]+" "+dateFormat.format(startDate)) : option_arrays[type], MARGINALSIZE2, test_place - (weatherdata.size() * text_size), paint);
        paint.setTextSize(16);
    }

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

    private void drawFigure(Canvas canvas, Date start_point, Date end_point, Date destination[], double wave[], int angle[], int angley, int drawtype){
        int siirra;
        double kerroin;
        int loop;
        int start_index = 0;
        int end_index = destination.length;

        if(drawtype == 0) { // Normal cases
            siirra = MARGINALSIZE;
            kerroin = (double) (sizex - MARGINALSIZE);
        } else if(drawtype == 1) { // Show observation figure.
            siirra = MARGINALSIZE;
            kerroin = (double) (sizex / 2.0 - MARGINALSIZE);
            for (loop = 0; loop < destination.length; loop++) {
                if (destination[loop].compareTo(start_point) >= 0) {
                    start_index = loop;
                    break;
                }
            }
        } else { // Show forecast figure.
            siirra = (sizex - MARGINALSIZE) / 2 + MARGINALSIZE;
            kerroin = (double) (sizex / 2.0 - MARGINALSIZE);
            for (loop = 0; loop < destination.length; loop++) {
                if (destination[loop].compareTo(end_point) > 0) {
                    end_index = loop;
                    break;
                }
            }
        }

        int points = end_index - start_index;
        double time_length = (double)(end_point.getTime() - start_point.getTime()) / kerroin;
        int dd = (int) ((double) (sizey - MARGINALSIZE) * min / (max - min));
        float xyw[] = new float[(points - 1) * 4];
        int l2 = 0;

        for (loop = start_index; loop < end_index - 1; loop++) {
                xyw[l2++] = (int) (((double) (destination[loop].getTime() - start_point.getTime()) / time_length)) + siirra;
                xyw[l2++] = sizey - (int) ((double) (sizey - MARGINALSIZE) * wave[loop] / (max - min)) + dd;
                xyw[l2++] = (int) (((double) (destination[loop + 1].getTime() - start_point.getTime()) / time_length)) + siirra;
                xyw[l2++] = sizey - (int) ((double) (sizey - MARGINALSIZE) * wave[loop + 1] / (max - min)) + dd;
        }
        canvas.drawLines(xyw, 0, xyw.length, paint);

        if(angle != null) {
                double length = (double) (xyw[xyw.length - 2] - siirra);
                int pointa = sizey / ((drawtype == 0) ? 15 : 30);
                for (loop = 0; loop <= pointa; loop++) {
                    double ai = length / (double) pointa * (double) loop;
                    int index = (int) ((ai / length * (double) (points - 1)) + 0.5);
                        drewAngle(canvas, (double) angle[start_index+index]+90, (int) ai + siirra, angley);
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

            if(type >= 0){
                endDate = new Date();
                startDate = new Date(endDate.getTime() - HOUR12);
            } else {
                Date nowtime = new Date();
                startDate = new Date(nowtime.getTime() + HOUR12 * (type - 1));
                endDate = new Date(nowtime.getTime() + HOUR12 * type);
            }

            observations.clear();
            for (String[] str : weatherplace) {
                if(str[4].equals("Fmi")) {
                    observations.add(new Fmi("http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::observations::weather::timevaluepair&fmisid="
                            + str[1] + "&starttime=" + fmiformat.format(startDate) + "&endtime=" + fmiformat.format(endDate) + "&parameters=windspeedms,WindDirection,wg_10min,temperature"));
                } else {
                    if (type >= 0)
                        observations.add(new WindGuru("https://www.windguru.cz/int/wgsapi.php?id_station="+str[1]+"&password="+str[5]+"&q=station_data_last&hours=12&avg_minutes=5&vars=wind_avg,wind_max,wind_direction"));
                    else
                        observations.add(new WindGuru("https://www.windguru.cz/int/wgsapi.php?id_station="+str[1]+"&password="+str[5]+"&q=station_data&from="
                                + windguruformat.format(startDate) + "&to=" + windguruformat.format(endDate) + "&vars=wind_avg,wind_max,wind_direction"));
                }
                progressDialog.incrementProgressBy(prosent);
                if(type >= 0)
                    type = MEASURED_WIND;
            }

            if((type >= 0) && (place.length() > 0 || forecast.size() == 0 || forecast.get(0).getStep()[0].before(observations.get(0).getStep()[observations.get(0).getStep().length-1]))){
                forecast.clear();
                for (String[] str : weatherplace) {
                    if(str[3].equals("true")) {
                        forecast.add(new Fmi("http://opendata.fmi.fi/wfs?request=getFeature&storedquery_id=fmi::forecast::hirlam::surface::point::timevaluepair&place="
                                + str[0] + "&parameters=windspeedms,WindDirection,weathersymbol3,temperature"));
                        type = MEASURED_AND_FORECAST_WIND;
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
}

}
