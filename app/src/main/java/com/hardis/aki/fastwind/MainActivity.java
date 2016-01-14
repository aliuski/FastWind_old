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


public class MainActivity extends ActionBarActivity {

    private static final int RESULT_SETTINGS = 1;
    private WindSpeedView windscreens;
    private String t2[];

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
                                if (windscreens.getDrawType() == WindSpeedView.MEASURED_WIND && (_xDelta > X))
                                    windscreens.setDrawType(WindSpeedView.MEASURED_AND_FORECAST_WIND);
                                else if (windscreens.getDrawType() == WindSpeedView.MEASURED_AND_FORECAST_WIND && (_xDelta > X))
                                    windscreens.setDrawType(WindSpeedView.FORECAST_WIND);
                                else if (windscreens.getDrawType() == WindSpeedView.FORECAST_WIND && (_xDelta < X))
                                    windscreens.setDrawType(WindSpeedView.MEASURED_AND_FORECAST_WIND);
                                else if (windscreens.getDrawType() == WindSpeedView.MEASURED_AND_FORECAST_WIND && (_xDelta < X))
                                    windscreens.setDrawType(WindSpeedView.MEASURED_WIND);
                                else if (windscreens.getDrawType() == WindSpeedView.FORECAST_WIND && (_xDelta > X))
                                    windscreens.setDrawType(WindSpeedView.MEASURED_TEMPATURE);
                                else if (windscreens.getDrawType() == WindSpeedView.FORECAST_TEMPATURE && (_xDelta < X))
                                    windscreens.setDrawType(WindSpeedView.MEASURED_TEMPATURE);
                                else if (windscreens.getDrawType() == WindSpeedView.MEASURED_TEMPATURE && (_xDelta > X))
                                    windscreens.setDrawType(WindSpeedView.FORECAST_TEMPATURE);
                                else if (windscreens.getDrawType() == WindSpeedView.MEASURED_TEMPATURE && (_xDelta < X))
                                    windscreens.setDrawType(WindSpeedView.FORECAST_WIND);

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

        windscreens.setApikey(sharedPrefs.getString("prefUserkey",null));
        String t = sharedPrefs.getString("prefUserObservationStation", null);
        if(t != null)
            t2 = t.split("\n");

        if(savedInstanceState != null)
            windscreens.getBundleData(savedInstanceState);
        else
            windscreens.setPlace(getProbePlaceList(0));
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
            Intent i = new Intent(this, UserSettingActivity.class);
            startActivityForResult(i, RESULT_SETTINGS);
            return true;
        } else if (id == R.id.action_favorite) {
            registerForContextMenu(windscreens);//When wanna use Options menu to open a context menu
            openContextMenu(windscreens);//Call register for context menu thing
            unregisterForContextMenu(windscreens);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        menu.setHeaderTitle(R.string.action_favorite);
        for (int loop = 0; loop < t2.length; loop++) {
            if(t2[loop].charAt(0) != '*')
                menu.add(0, loop, 0, t2[loop]);
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        windscreens.setPlace(getProbePlaceList(item.getItemId()));
        return true;
    }

    private String getProbePlaceList(int index){
        if(((index+1) < t2.length) && (t2[index].charAt(0) != '*') && (t2[index+1].charAt(0) == '*')) {
            String ret = "";
            for (int loop = index+1; loop < t2.length; loop++) {
                if(t2[loop].charAt(0) != '*')
                    break;
                ret += t2[loop].substring(1) + "\n";
            }
            return ret;
        } else {
            if(t2[index].charAt(0) == '*')
                return t2[index].substring(1);
            else
                return t2[index];
        }
    }
}
