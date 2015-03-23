package kr.ac.kaist.nlp2cal;

import android.app.AlertDialog;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.provider.CalendarContract;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Created by Woo on 15. 1. 24..
 */
public class CalendarManager {
    private static final String TAG = "CalendarManager";

    private static final int MAX_CANDIDATE = 3;
    private static int buffKey;
    private static int selected = -1;

    public static void insertEvent(Context ctx, String title, String location, Date sTime, Date eTime) {
        Log.i(TAG, "CalendarManager.insertEvent() 들어옴");

//        Date date_stime = Extractor.parseDate(sTime);
//        Date date_etime = Extractor.parseDate(eTime);

        Date date_stime = sTime;
        Date date_etime = eTime;

        int stime_year = date_stime.getYear() + 1900;
        int stime_month = date_stime.getMonth();
        int stime_day = date_stime.getDate();
        int stime_hour = date_stime.getHours();
        int stime_min = date_stime.getMinutes();

        int etime_year = date_etime.getYear() + 1900;
        int etime_month = date_etime.getMonth();
        int etime_day = date_etime.getDate();
        int etime_hour = date_etime.getHours();
        int etime_min = date_etime.getMinutes();

        Calendar beginTime = Calendar.getInstance();
        beginTime.set(stime_year, stime_month, stime_day, stime_hour, stime_min);
        Calendar endTime = Calendar.getInstance();
        endTime.set(etime_year, etime_month, etime_day, etime_hour, etime_min);
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location);
        ctx.startActivity(intent);
    }

    public static void findEvent(Context ctx, String mTitle, final Date sTime, final Date eTime, final String location, String type) {
        // Projection array. Creating indices for this array instead of doing
        // dynamic lookups improves performance.
        final String[] INSTANCE_PROJECTION = new String[]{
                CalendarContract.Instances.EVENT_ID,      // 0
                CalendarContract.Instances.BEGIN,         // 1
                CalendarContract.Instances.TITLE,         // 2
                CalendarContract.Instances.CALENDAR_ID    // 3
        };
        // The indices for the projection array above.
        final int PROJECTION_EVENT_INDEX = 0;
        final int PROJECTION_BEGIN_INDEX = 1;
        final int PROJECTION_TITLE_INDEX = 2;
        final int PROJECTION_CALENDAR_INDEX = 3;

        // Specify the date range you want to search for recurring
        // event instances
        Calendar beginTime = Calendar.getInstance();
//        beginTime.set(2015, 0, 15, 8, 0);
        long startMillis = beginTime.getTimeInMillis(); // get current system time
        Calendar endTime = Calendar.getInstance();
        endTime.set(2017, 11, 31, 9, 0); // set current system time + two years 하는게 좋겠지만, 당분간 이렇게 set.
        long endMillis = endTime.getTimeInMillis();

        // Construct the query with the desired date range.
        Uri.Builder builder = CalendarContract.Instances.CONTENT_URI.buildUpon();
        ContentUris.appendId(builder, startMillis);
        ContentUris.appendId(builder, endMillis);

        // Submit the query
        Cursor cur = null;
        ContentResolver cr = ctx.getContentResolver();
        cur = cr.query(builder.build(),
                INSTANCE_PROJECTION,
                null,
                null,
                null); // 여기서 죽음

        Map<String, Double> idTitleScore = new HashMap<String, Double>();

        while (cur.moveToNext()) {
            String title = null;
            long eventID = 0;
            long beginVal = 0;
            long calID = 0;

            // Get the field values
            eventID = cur.getLong(PROJECTION_EVENT_INDEX); ////
            beginVal = cur.getLong(PROJECTION_BEGIN_INDEX);
            title = cur.getString(PROJECTION_TITLE_INDEX); ////
            calID = cur.getLong(PROJECTION_CALENDAR_INDEX);

            // Do something with the values.
            Log.i(TAG, "Event:  " + title);
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(beginVal);
            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            Log.i(TAG, "Date: " + formatter.format(calendar.getTime()));

            // Get the score of the similarity
            double score = 0;
            score = diceCoefficientOptimized(mTitle, title); ////
            Log.i(TAG, "Score:  " + score);

            // Set three values which are eventID, eventTitle and score
            idTitleScore.put(eventID + "\t" + title, score);
        }

        // Sort by the score
        Map<String, Double> idTitleScore_sorted = sortByValue(idTitleScore);

        // Show the user the list of most similar events
//        ArrayList<String> scoreTile = new ArrayList<String>();
        final ArrayList<String> idList = new ArrayList<String>();
        List<CharSequence> scoreTitle = new ArrayList<CharSequence>();

        int i = 0;
        for (String idTitle : idTitleScore_sorted.keySet()) {
            if (i == MAX_CANDIDATE) break;
            double score = idTitleScore_sorted.get(idTitle);
            String[] col = idTitle.split("\t");

            String id = col[0];
            String title = col[1];
//            Log.i(TAG, score + " " + id + " " + title);

            String strScore = String.format("%.2f", score);
            if (score > 0.1) {
                scoreTitle.add("[" + strScore + "] " + title);
                idList.add(id);
            }
            i++;
        }

        final String dTitle = mTitle;
        final String dType = type;

        selected = -1;
        final Context context = ctx;
        final CharSequence[] items = scoreTitle.toArray(new CharSequence[scoreTitle.size()]);
        Log.i(TAG, "scoreTitle.size() " + scoreTitle.size());
        // 검색된 일정이 없을 경우
        if (scoreTitle.size() < 1) {
            Toast.makeText(context, "다가올 일정이 없습니다.", Toast.LENGTH_SHORT).show();
        }
        // 검색된 일정이 있을 경우
        else {
            AlertDialog.Builder builder3 = new AlertDialog.Builder(ctx);
            if (dType == "update") {
                builder3.setTitle("변경할 일정을 선택하세요.");
            } else {
                builder3.setTitle("삭제할 일정을 선택하세요.");
            }
            builder3.setSingleChoiceItems(items, 0, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    Toast.makeText(context, items[i], Toast.LENGTH_SHORT).show();
                    buffKey = i;
                }
            })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            if (dType == "update") {
                                Toast.makeText(context, "변경할 일정을 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(context, "삭제할 일정을 선택하지 않았습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }
                    })
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Log.i(TAG, "Which value: " + i);
                            Log.i(TAG, "Selected value: " + buffKey);
                            selected = buffKey;

                            if (dType == "update") {
//                                updateEvent(context, idList.get(selected), dTitle);
                                updateEvent2(context, idList.get(selected), dTitle, sTime, eTime, location);
                            } else {
                                deleteEvent(context, idList.get(selected));
                            }
                        }
                    });
            AlertDialog dialog = builder3.create();
            dialog.show();
        }

        // return eventID chosen by the user
        if (selected == -1) {
            Log.i(TAG, "selected = -1");
        } else {
            Log.i(TAG, "selected = " + selected);
            if (type == "update") {
                Log.i(TAG, "updateEvent() " + idList.get(selected));
            } else if (type == "cancel") {
                Log.i(TAG, "deleteEvent() " + idList.get(selected));
            } else {
                Log.e(TAG, "unexpected type");
            }
        }
    }

    private static void deleteEvent(Context context, String s) {
        Log.i(TAG, "deleteEvent(삭제할 이벤트 ID)");

        try {
            long eventID = Long.parseLong(s);

            ContentResolver cr = context.getContentResolver();
            ContentValues values = new ContentValues();
            Uri deleteUri = null;
            deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
            int rows = context.getContentResolver().delete(deleteUri, null, null);
            Log.i(TAG, "Rows deleted: " + rows);

            Toast.makeText(context, "일정을 삭제하였습니다.", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateEvent(Context context, String s, String title) {
        Log.i(TAG, "updateEvent(변경할 이벤트 ID)");

        try {
            long eventID = Long.parseLong(s);

            Uri uri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
            Intent intent = new Intent(Intent.ACTION_EDIT)
                    .setData(uri)
                    .putExtra(CalendarContract.Events.TITLE, "변경"); // title 은 바꾸면 안될려나? title 을 provenance ID 로 사용하고 있으니까.
            context.startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void updateEvent2(Context context, String s, String title, Date sTime, Date eTime, String location) {
        Log.i(TAG, "updateEvent()");

        Date date_stime = sTime;
        Date date_etime = eTime;

        int stime_year = date_stime.getYear() + 1900;
        int stime_month = date_stime.getMonth();
        int stime_day = date_stime.getDate();
        int stime_hour = date_stime.getHours();
        int stime_min = date_stime.getMinutes();

        int etime_year = date_etime.getYear() + 1900;
        int etime_month = date_etime.getMonth();
        int etime_day = date_etime.getDate();
        int etime_hour = date_etime.getHours();
        int etime_min = date_etime.getMinutes();

        try {
            long eventID = Long.parseLong(s);

            ContentResolver cr = context.getContentResolver();
            ContentValues values = new ContentValues();
            Uri deleteUri = null;
            deleteUri = ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, eventID);
            int rows = context.getContentResolver().delete(deleteUri, null, null);
            Log.i(TAG, "Rows deleted: " + rows);

        } catch (Exception e) {
            e.printStackTrace();
        }

        Calendar beginTime = Calendar.getInstance();
        beginTime.set(stime_year, stime_month, stime_day, stime_hour, stime_min);
        Calendar endTime = Calendar.getInstance();
        endTime.set(etime_year, etime_month, etime_day, etime_hour, etime_min);
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location);
        context.startActivity(intent);
    }

    // value sorting
    public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        List<Map.Entry<K, V>> list =
                new LinkedList<Map.Entry<K, V>>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        Map<K, V> result = new LinkedHashMap<K, V>();

        Collections.reverse(list);

        for (Map.Entry<K, V> entry : list) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static double diceCoefficientOptimized(String s, String t) {
        // Verifying the input:
        if (s == null || t == null)
            return 0;
        // Quick check to catch identical objects:
        if (s == t)
            return 1;
        // avoid exception for single character searches
        if (s.length() < 2 || t.length() < 2)
            return 0;

        // Create the bigrams for string s:
        final int n = s.length() - 1;
        final int[] sPairs = new int[n];
        for (int i = 0; i <= n; i++)
            if (i == 0)
                sPairs[i] = s.charAt(i) << 16;
            else if (i == n)
                sPairs[i - 1] |= s.charAt(i);
            else
                sPairs[i] = (sPairs[i - 1] |= s.charAt(i)) << 16;

        // Create the bigrams for string t:
        final int m = t.length() - 1;
        final int[] tPairs = new int[m];
        for (int i = 0; i <= m; i++)
            if (i == 0)
                tPairs[i] = t.charAt(i) << 16;
            else if (i == m)
                tPairs[i - 1] |= t.charAt(i);
            else
                tPairs[i] = (tPairs[i - 1] |= t.charAt(i)) << 16;

        // Sort the bigram lists:
        Arrays.sort(sPairs);
        Arrays.sort(tPairs);

        // Count the matches:
        int matches = 0, i = 0, j = 0;
        while (i < n && j < m) {
            if (sPairs[i] == tPairs[j]) {
                matches += 2;
                i++;
                j++;
            } else if (sPairs[i] < tPairs[j])
                i++;
            else
                j++;
        }
        return (double) matches / (n + m);
    }
}
