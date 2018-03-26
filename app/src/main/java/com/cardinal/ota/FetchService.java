package com.cardinal.ota;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.LinkTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;

import java.util.ArrayList;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.cardinal.ota.Utils.compareDate;
import static com.cardinal.ota.Utils.getCurBuildDate;
import static com.cardinal.ota.Utils.getProp;
import static com.cardinal.ota.Utils.isValidDate;

public class FetchService extends Service {

    public String device;
    private String LOG_TAG = FetchService.class.getSimpleName();

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(LOG_TAG, "Fetch Service Start");
        Toast.makeText(getApplicationContext(), "Start Service", Toast.LENGTH_LONG).show();

        String CHANNEL_ID = "id";
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Cardinal-AOSP", NotificationManager.IMPORTANCE_DEFAULT);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build();
        startForeground(1, notification);

        String checkDevice = getProp(Constants.ROM_DEVICE_PROP);
        if (!checkDevice.equalsIgnoreCase("")) device = checkDevice;
        Uri ctrBaseUrl = Uri.parse(Constants.SF_PROJECTS_BASE_URL)
                .buildUpon()
                .appendPath(Constants.ROM_NAME)
                .appendPath("files")
                .appendPath(device)
                .build();
        if (isConnected())
            new FetchTask().execute(ctrBaseUrl.toString());
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        Log.i(LOG_TAG, "isConnected: " + Boolean.toString(netInfo != null && netInfo.isConnectedOrConnecting()));
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private class FetchTask extends AsyncTask<String, Void, ArrayList<String>> {

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected ArrayList<String> doInBackground(String... strings) {
            try {
                final String device = getProp(Constants.ROM_DEVICE_PROP);
                final Parser htmlParser = new Parser(strings[0]);
                final NodeList tagNodeList = htmlParser.extractAllNodesThatMatch(new NodeClassFilter(LinkTag.class));
                Uri ctrDevBaseUrl = Uri.parse(Constants.SF_PROJECTS_BASE_URL)
                        .buildUpon()
                        .appendPath(Constants.ROM_NAME)
                        .appendPath("files")
                        .appendPath(device)
                        .appendPath(Constants.ROM_ZIP_NAME)
                        .build();
                Uri downTempUri = Uri.parse(Constants.SF_PROJECTS_BASE_URL)
                        .buildUpon()
                        .appendPath(Constants.ROM_NAME)
                        .appendPath("files")
                        .appendPath(device)
                        .build();
                Pattern p = Pattern.compile(ctrDevBaseUrl.toString());
                ArrayList<String> result = new ArrayList<String>();
                int i = 0;
                for (int j = 0; j < tagNodeList.size(); j++) {
                    final LinkTag loopLink = (LinkTag) tagNodeList.elementAt(j);
                    final String loopLinkStr = loopLink.getLink();
                    Matcher m = p.matcher(loopLinkStr);
                    if (m.find()) {
                        int x = loopLinkStr.lastIndexOf('/');
                        int y = loopLinkStr.indexOf('/', downTempUri.toString().length());
                        Matcher m2 = Pattern.compile("download").matcher(loopLinkStr);
                        if (m2.find()) {
                            result.add(loopLinkStr.substring(y + 1, x));
                        }
                    }
                }
                return result;
            } catch (ParserException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<String> result) {
            super.onPostExecute(result);

            if (result != null && !result.isEmpty()) {
                int date = 0, maxDate = 0;
                String newestRom = "";
                String[] builds = result.toArray(new String[result.size()]);
                Log.i(LOG_TAG, "Size: " + result.size());
                for (String build : builds) {
                    Log.i(LOG_TAG, "Build: " + build);
                    StringTokenizer st = new StringTokenizer(build, Constants.ROM_ZIP_DELIMITER);
                    while (st.hasMoreTokens()) {
                        String value = st.nextToken();
                        if (isValidDate(value)) {
                            date = Integer.parseInt(value);
                            //Log.e(LOG_TAG, value);
                        }

                        if (date == 0)
                            continue;

                        if (date != 0 && maxDate != 0)
                            if (compareDate(date, maxDate)) {
                                maxDate = date;
                                newestRom = build;
                                break;
                            }

                        if (date != 0 && maxDate == 0) {
                            maxDate = date;
                            newestRom = build;
                            break;
                        }
                    }
                }

                Log.i(LOG_TAG, "Update: " + newestRom);
                Log.i(LOG_TAG, "Update Build Date: " + Integer.toString(maxDate));

                Intent intent = new Intent("ROMUpdates");
                intent.putExtra("Update", newestRom);
                intent.putExtra("BuildDate", Integer.toString(maxDate));
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                stopForeground(true);
                NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                notificationManager.cancel(1);
                if (!newestRom.equalsIgnoreCase(null) && maxDate != 0) {
                    String currentVer = getProp(Constants.BUILD_FLAVOR_PROP);
                    if ((isConnected()) && (!newestRom.equalsIgnoreCase(""))) {
                        if (compareDate(maxDate, getCurBuildDate(currentVer))) {
                            Log.i(LOG_TAG, "Not up-to-date");

                            String id = "id";
                            CharSequence name = "Cardinal-AOSP";
                            String description = "Notifications regarding Cardinal-AOSP updates";
                            int importance = NotificationManager.IMPORTANCE_HIGH;
                            NotificationChannel mChannel = new NotificationChannel(id, name, importance);
                            mChannel.setDescription(description);
                            mChannel.enableLights(true);
                            mChannel.setLightColor(Color.WHITE);
                            notificationManager.createNotificationChannel(mChannel);
                            Intent intent1 = new Intent(getApplicationContext(), MainActivity.class);
                            PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 123, intent1, PendingIntent.FLAG_UPDATE_CURRENT);
                            NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), "id")
                                    .setSmallIcon(R.drawable.ic_ota_download)
                                    .setBadgeIconType(R.drawable.ic_ota_download)
                                    .setContentTitle("Cardinal-AOSP Update Available")
                                    .setAutoCancel(true).setContentIntent(pendingIntent)
                                    .setNumber(1)
                                    .setColor(255)
                                    .setContentText("Tap here to open OTA app")
                                    .setWhen(System.currentTimeMillis());

                            notificationManager.notify(1, notificationBuilder.build());
                            //startForeground(1, notificationBuilder.build());
                        }
                    }
                }
            } else {
                Log.e(LOG_TAG, "Null error");
                Intent intent = new Intent("ROMUpdates");
                intent.putExtra("Update", "");
                intent.putExtra("BuildDate", "0");
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }

        }


    }

}
