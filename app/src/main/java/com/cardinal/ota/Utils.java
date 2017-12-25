package com.cardinal.ota;

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
}
