package com.cardinal.cardinalota;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NavUtils;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static com.cardinal.cardinalota.Utils.compareDate;
import static com.cardinal.cardinalota.Utils.getCurBuildDate;
import static com.cardinal.cardinalota.Utils.getProp;

public class CardinalOTAActivity extends PreferenceActivity implements Preference.OnPreferenceChangeListener,
        SharedPreferences.OnSharedPreferenceChangeListener, FetchTask.AsyncResponse {

    public String device, name, currentVer;
    private static final String KEY_ROM_INFO = "rom_info";
    private static final String KEY_CHECK_UPDATE = "check_update";
    private static final String KEY_UPDATE_LINK = "update_link";
    private Preference mRomInfo;
    private Preference mCheckUpdate;
    private Preference mUpdateLink;
    private ProgressDialog dialog;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private String downUri;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        ListView lv = getListView();
        lv.setDivider(new ColorDrawable(Color.TRANSPARENT));
        lv.setDividerHeight(0);

        dialog = ProgressDialog.show(CardinalOTAActivity.this, "", "Checking...", true);

        addPreferencesFromResource(R.xml.preference_cardinal_ota);
        mRomInfo = (Preference) getPreferenceScreen().findPreference(KEY_ROM_INFO);
        mCheckUpdate = (Preference) getPreferenceScreen().findPreference(KEY_CHECK_UPDATE);
        mRomInfo.setIcon(R.drawable.ic_ota_info);
        mCheckUpdate.setIcon(R.drawable.ic_ota_refresh);


        mCheckUpdate.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                if (isConnected()) {
                    String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    SharedPreferences lastCheckPref = getApplicationContext().getSharedPreferences("lastCheckDate", 0);
                    SharedPreferences.Editor editor = lastCheckPref.edit();
                    editor.putString("datetime", currentDateTimeString);
                    editor.commit();
                    mRomInfo.setSummary("");

                    updatePreferences();
                } else updatePreferences();
                return false;
            }
        });
        if (isConnected()) {
            String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
            SharedPreferences lastCheckPref = getApplicationContext().getSharedPreferences("lastCheckDate", 0);
            SharedPreferences.Editor editor = lastCheckPref.edit();
            editor.putString("datetime", currentDateTimeString);
            editor.commit();
            currentVer = getProp(Constants.BUILD_FLAVOR_PROP);
            mRomInfo.setSummary("Your system is up-to-date.");
            updatePreferences();
        } else updatePreferences();
    }

    private void updatePreferences() {
        updateRomInfo();
        if (isConnected())
            updateLinks();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                NavUtils.navigateUpFromSameTask(this);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(getApplicationContext(), WRITE_EXTERNAL_STORAGE);
        return result == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
    }

    public boolean isConnected() {
        ConnectivityManager cm =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        Log.i(Constants.LOG_TAG, "isConnected: " + Boolean.toString(netInfo != null && netInfo.isConnectedOrConnecting()));
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    public void updateRomInfo() {
        currentVer = getProp(Constants.BUILD_FLAVOR_PROP);
        mRomInfo.setTitle(currentVer);
        if (isConnected()) {
            SharedPreferences lastCheckPref = getApplicationContext().getSharedPreferences("lastCheckDate", 0);
            if (!lastCheckPref.getString("datetime", null).equalsIgnoreCase(""))
                mCheckUpdate.setSummary("Last Checked: " + lastCheckPref.getString("datetime", null));
            else
                mCheckUpdate.setSummary("Last Checked: " + DateFormat.getDateTimeInstance().format(new Date()));
        } else {
            mCheckUpdate.setSummary("Network Error");
            mRomInfo.setSummary("Your system is up-to-date.");
        }
    }

    public void updateLinks() {
        String checkDevice = getProp(Constants.ROM_DEVICE_PROP);
        if (!checkDevice.equalsIgnoreCase("")) device = checkDevice;
        Uri ctrBaseUrl = Uri.parse(Constants.SF_PROJECTS_BASE_URL)
                .buildUpon()
                .appendPath(Constants.ROM_NAME)
                .appendPath("files")
                .appendPath(device)
                .build();
        Log.i(Constants.LOG_TAG, ctrBaseUrl.toString());
        dialog.show();
        new FetchTask(this).execute(ctrBaseUrl.toString());

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object o) {
        return false;
    }

    @Override
    public void processFinish(String output, int date) {
        dialog.dismiss();
        Log.i(Constants.LOG_TAG, "processFinish");
        if (!output.equalsIgnoreCase(null) && date != 0) {
            name = output;
            currentVer = getProp(Constants.BUILD_FLAVOR_PROP);
            if ((isConnected()) && (!output.equalsIgnoreCase(""))) {
                if (compareDate(date, getCurBuildDate(currentVer))) {
                    Log.i(Constants.LOG_TAG, "Not up-to-date");
                    mCheckUpdate.setSummary("Last Checked: " + DateFormat.getDateTimeInstance().format(new Date()));
                    mRomInfo.setSummary("Latest build is available for update.");
                    mUpdateLink = (Preference) getPreferenceScreen().findPreference(KEY_UPDATE_LINK);

                    final Uri uri = Uri.parse(Constants.SF_PROJECTS_DOWNLOAD_BASE_URL)
                            .buildUpon()
                            .appendPath(Constants.ROM_NAME)
                            .appendPath(device)
                            .appendPath(name)
                            .build();
                    downUri = uri.toString();

                    final ListView lv = getListView();
                    lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                            Log.i(Constants.LOG_TAG, Long.toString(id));
                            switch ((int) id) {
                                case 3:
                                    registerForContextMenu(lv);
                                    break;
                            }
                            return false;
                        }
                    });

                    mUpdateLink.setIcon(R.drawable.ic_ota_download);
                    mUpdateLink.setTitle("Download");
                    mUpdateLink.setSummary("Long Press for more options.");
                    mUpdateLink.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {

                            DownloadManager.Request r = new DownloadManager.Request(uri);
                            r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, name);
                            r.allowScanningByMediaScanner();
                            r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                if (checkPermission())
                                    dm.enqueue(r);
                                else requestPermission();
                            } else dm.enqueue(r);
                            return false;
                        }
                    });
                } else {
                    Log.i(Constants.LOG_TAG, "Up-to-date");
                    mRomInfo.setSummary("Your system is up-to-date.");
                    mCheckUpdate.setSummary("Last Checked: " + DateFormat.getDateTimeInstance().format(new Date()));
                }
            }
        } else {
            Toast.makeText(CardinalOTAActivity.this, "Null", Toast.LENGTH_LONG).show();
            Log.i(Constants.LOG_TAG, "Up-to-date");
            mRomInfo.setSummary("Your system is up-to-date.");
            mCheckUpdate.setSummary("Last Checked: " + DateFormat.getDateTimeInstance().format(new Date()));
        }
    }

    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflateLayout = getMenuInflater();
        inflateLayout.inflate(R.menu.download_menu, menu);
    }

    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_browser:
                Intent i = new Intent(Intent.ACTION_VIEW);
                i.setData(Uri.parse(downUri));
                startActivity(i);
                break;
            case R.id.copy_link:
                ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("", downUri);
                clipboard.setPrimaryClip(clip);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

}
