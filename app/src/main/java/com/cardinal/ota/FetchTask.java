package com.cardinal.ota;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

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
import static com.cardinal.ota.Utils.getProp;
import static com.cardinal.ota.Utils.isValidDate;

public class FetchTask extends AsyncTask<String, Void, ArrayList<String>> {

    public interface AsyncResponse {
        void processFinish(String output, int date);
    }

    public AsyncResponse delegate = null;

    public FetchTask(AsyncResponse delegate) {
        this.delegate = delegate;
    }

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
            int date = 0, maxDate = 0, location = 1;
            String newestRom = "";
            String[] builds = result.toArray(new String[result.size()]);
            Log.i(Constants.LOG_TAG, "Size: " + result.size());
            for (String build : builds) {
                Log.i(Constants.LOG_TAG, "Build: " + build);
                StringTokenizer st = new StringTokenizer(build, Constants.ROM_ZIP_DELIMITER);
                while (st.hasMoreTokens()) {
                    String value = st.nextToken();
                    if (isValidDate(value)) {
                        date = Integer.parseInt(value);
                        //Log.e(Constants.LOG_TAG, value);
                    }
                    location++;

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
                location = 1;
            }

            Log.i(Constants.LOG_TAG, "Update: " + newestRom);
            Log.i(Constants.LOG_TAG, "Update Build Date: " + Integer.toString(maxDate));

            if (!newestRom.equals("")) delegate.processFinish(newestRom, maxDate);
        } else {
            Log.e(Constants.LOG_TAG, "Null err");
            delegate.processFinish("", 0);
        }
    }

}
