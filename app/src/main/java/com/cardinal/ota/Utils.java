package com.cardinal.ota;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

import eu.chainfire.libsuperuser.Shell;

public class Utils {
    public static String getProp(String prop) {
        String value = Shell.SH.run("getprop " + prop).get(0);
        return value;
    }

    public static boolean isValidDate(String value) {
        try {
            new SimpleDateFormat("yyyyMMdd").parse(value);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    public static boolean compareDate(int currentBuild, int updateBuild) {
        try {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date currentBuildDate = formatter.parse(Integer.toString(currentBuild));
            Date updateBuildDate = formatter.parse(Integer.toString(updateBuild));
            return (currentBuildDate.compareTo(updateBuildDate) > 0);
        } catch (ParseException e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int getCurBuildDate(String curBuildVer) {
        int date = 0, location = 1;
        //Log.i(Constants.LOG_TAG, "Build: " + curBuildVer);
        StringTokenizer st = new StringTokenizer(curBuildVer, Constants.ROM_ZIP_DELIMITER);
        while (st.hasMoreTokens()) {
            String value = st.nextToken();
            if (isValidDate(value)) {
                date = Integer.parseInt(value);
                //Log.i(Constants.LOG_TAG, value);
            }
            location++;

            if (date != 0) break;
        }
        return date;
    }

    public static void scheduleFetchUpdate(Context context){
        AlarmManager alarmMgr = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, ScheduleReceiver.class);
        PendingIntent pIntent = PendingIntent.getBroadcast(context, 0, intent, 0);
        Calendar cal= Calendar.getInstance();
        SharedPreferences prefs= PreferenceManager.getDefaultSharedPreferences(context);
        String time=prefs.getString("schedule_pref", "12:00");
        cal.set(Calendar.HOUR_OF_DAY, TimePreference.getHour(time));
        cal.set(Calendar.MINUTE, TimePreference.getMinute(time));
        cal.set(Calendar.SECOND, 0);
        alarmMgr.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pIntent);
    }
}
