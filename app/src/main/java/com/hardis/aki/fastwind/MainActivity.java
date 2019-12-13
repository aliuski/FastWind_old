package com.hardis.aki.fastwind;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private SharedPreferences.OnSharedPreferenceChangeListener prefListener;
    private WindSpeedView windscreens;
    private String prefUserObservationStation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        windscreens = (WindSpeedView)findViewById(R.id.windspeedview);

        windscreens.setOnTouchListener(new View.OnTouchListener(){
            boolean move = false;
            private int _xDelta = 0;
            private int _yDelta = 0;

            public boolean onTouch(View view, MotionEvent event) {
                int X = (int) event.getRawX();
                int Y = (int) event.getRawY();
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_DOWN:
                        _xDelta = X;
                        _yDelta = Y;
                        move = true;
                        break;
                    case MotionEvent.ACTION_UP:
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if(move){
                            if((int)Math.abs(X - _xDelta) > 5) {
                                if(windscreens.isForecast()) {
                                    if (_xDelta < X)
                                        windscreens.setDrawType(windscreens.getDrawType() - 1);
                                    else if (windscreens.getDrawType() < WindSpeedView.FORECAST_TEMPATURE && (_xDelta > X))
                                        windscreens.setDrawType(windscreens.getDrawType() + 1);
                                } else {
                                    if (windscreens.getDrawType() == WindSpeedView.MEASURED_WIND && (_xDelta > X))
                                        windscreens.setDrawType(WindSpeedView.MEASURED_TEMPATURE);
                                    else if (windscreens.getDrawType() == WindSpeedView.MEASURED_TEMPATURE && (_xDelta < X))
                                        windscreens.setDrawType(WindSpeedView.MEASURED_WIND);
                                    else if (windscreens.getDrawType() <= 0 && _xDelta < X)
                                        windscreens.setDrawType(windscreens.getDrawType() - 1);
                                    else if (windscreens.getDrawType() <= 0 && _xDelta > X)
                                        windscreens.setDrawType(windscreens.getDrawType() + 1);
                                }
                            } else if((int)Math.abs(Y - _yDelta) > 5) {
                                windscreens.changeIndex(_yDelta > Y);
                            }
                            move = false;
                        }
                        break;
                }
                return true;
            }
        });

        SharedPreferences sharedPrefs = PreferenceManager
                .getDefaultSharedPreferences(this);

        prefUserObservationStation = sharedPrefs.getString("prefUserObservationStation", null);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                prefUserObservationStation = prefs.getString("prefUserObservationStation",null);
                invalidateOptionsMenu();
            }
        };
        prefs.registerOnSharedPreferenceChangeListener(prefListener);

        if(savedInstanceState != null)
            windscreens.getBundleData(savedInstanceState);
        else {
            List<String> l = getObservationList();
            if(l.size()>0)
                windscreens.setPlace(getProbePlaceList(l.get(0)));
         }
    }

    @Override
    protected void onStart(){
        super.onStart();
        windscreens.setStart();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        windscreens.setBundleData(savedInstanceState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent();
            intent.setClass(MainActivity.this, SetPreferenceActivity.class);
            startActivityForResult(intent, 0);
            return true;
        } else if (id == R.id.action_favorite) {
            registerForContextMenu(windscreens);
            openContextMenu(windscreens);
            unregisterForContextMenu(windscreens);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.action_favorite);
        List<String> l = getObservationList();
        for(int i=0 ; i<l.size() ; i++)
            menu.add(0, i, 0, l.get(i));
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        windscreens.setPlace(getProbePlaceList(item.getTitle().toString()));
        return true;
    }

    private String getProbePlaceList(String value){
        String out = "";
        if(prefUserObservationStation != null && !prefUserObservationStation.isEmpty()) {
            String[] row = prefUserObservationStation.split("\n");
            for (int i = 0; i < row.length; i++) {
                String column[] = row[i].split(";");
                if(column[0].equals(value) && column[2].isEmpty() || column[2].equals(value)) {
                    if(!out.isEmpty())
                        out += '\n';
                    out += row[i];
                }
            }
        }
        return out;
    }

    private List<String> getObservationList(){
        ArrayList<String> list = new ArrayList<String>();
        if(prefUserObservationStation != null && !prefUserObservationStation.isEmpty()) {
            String[] row = prefUserObservationStation.split("\n");
            for (int i = 0; i < row.length; i++) {
                String column[] = row[i].split(";");
                if(column[2].isEmpty())
                    list.add(column[0]);
                else {
                    if(!list.contains(column[2]))
                        list.add(column[2]);
                }
            }
        }
        return list;
    }
}
