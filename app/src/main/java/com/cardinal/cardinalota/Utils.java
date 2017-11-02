package com.cardinal.cardinalota;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;

import eu.chainfire.libsuperuser.Shell;

public class Utils {
    public static String getProp(String prop) {
        String value = Shell.SH.run("getprop " + prop).get(0);
        return value;
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
        Log.i(Constants.LOG_TAG, "Build: " + curBuildVer);
        StringTokenizer st = new StringTokenizer(curBuildVer, Constants.ROM_ZIP_DELIMITER);
        while (st.hasMoreTokens()) {
            switch (location) {
                case Constants.ROM_ZIP_DATE_LOCATION:
                    try {
                        date = Integer.parseInt(st.nextToken());
                        Log.i(Constants.LOG_TAG, "Date: " + date);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    Log.i(Constants.LOG_TAG, "Default: " + st.nextToken());
                    break;
            }
            Log.i(Constants.LOG_TAG, "Location: " + location);
            location++;

            if (date != 0) break;
        }
        Log.i(Constants.LOG_TAG, "meh Date: " + date);
        return date;
    }
}
