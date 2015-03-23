package kr.ac.kaist.nlp2cal;

import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;

/**
 * Created by Woo on 15. 1. 27..
 */
public class Extractor extends AsyncTask<String, Void, String> {
    private static final String TAG = "Extractor";

    private static final String serverUrl = Constants.URL_MEETINGINFO_EXTRACTOR;

    private Context mContext;

    private Date sentDate;
    private String body;
    private Date startTime;
    private Date endTime;
    private String location;
    private String type;

    private TextView fTitle;
    private TextView fTime;
    private TextView fStartTime;
    private TextView fEndTime;
    private TextView fLocation;
    private TextView fType;

    private TextView mTitle;
    private TextView mStartTime;
    private TextView mEndTime;
    private TextView mLocation;
    private TextView mType;

    private TextView mInsert;
    private TextView mUpdate;
    private TextView mDelete;

    ProgressDialog progressDialog;

    Date date_stime;
    Date date_etime;

    public Extractor(Context context, String pasteData,
                     TextView aTitle, TextView aTime, TextView aStartTime, TextView aEndTime, TextView aLocation, TextView aType,
                     TextView title, TextView startTime, TextView endTime, TextView location, TextView type,
                     TextView btnInsert, TextView btnUpdate, TextView btnDelete) {
        mContext = context;

        body = pasteData;

        fTitle = aTitle;
        fTime = aTime;
        fStartTime = aStartTime;
        fEndTime = aEndTime;
        fLocation = aLocation;
        fType = aType;

        mTitle = title;
        mStartTime = startTime;
        mEndTime = endTime;
        mLocation = location;
        mType = type;

        mInsert = btnInsert;
        mUpdate = btnUpdate;
        mDelete = btnDelete;

        sentDate = new Date();
    }

    @Override
    protected void onPreExecute() {
        progressDialog = ProgressDialog.show(mContext, "Please wait for ", "extracting meeting info.", true);
    }

    @Override
    protected String doInBackground(String... params) {
        // detect type of event
        // add, update or cancel
        CancelDetector cd = new CancelDetector();
        if(cd.isCanceled(body)) {
            // type = cancel
            type = "cancel";
        }
        else if(cd.isUpdated(body)) {
            // type = update
            type = "update";
        }
        else {
            // type = add
            type = "add";
        }

        // extract time/space information through server
        String postPara = "sentDate=" + sentDate.getTime()/1000 + "&body=" + body + "&command=ExtractEmail";

        URL url = null;
        HttpURLConnection conn = null;
        PrintWriter postReq = null;
        BufferedReader postRes = null;
        StringBuilder json = null;
        String line = null;

        json = new StringBuilder();
        try {
            url = new URL(serverUrl);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            //conn.setRequestProperty("Content-Type", "text/plain");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //conn.setRequestProperty("Content-Length", Integer.toString(postPara.length()));
            conn.setDoInput(true);

            postReq = new PrintWriter(new OutputStreamWriter(conn.getOutputStream(), "EUC-KR"));
            postReq.write(postPara);
            postReq.flush();

            postRes = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            while ((line = postRes.readLine()) != null) {
                json.append(line);
            }
            conn.disconnect();

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return json.toString();
    }

    @Override
    protected void onPostExecute(String result) {
        mTitle.setText(body);
        if(type == "add") {
            mType.setText("신규");
        }
        else if(type == "update") {
            mType.setText("변경");
        }
        else if(type == "cancel") {
            mType.setText("취소");
        }

        // 서버로부터 받은 시공간 정보 추출 결과를 저장
        if (readJSON(result)) {
//            DateFormat formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm");
            DateFormat formatter = new SimpleDateFormat("yyyy년 M월 d일 (E) a h:mm");
            mStartTime.setText(formatter.format(getStartTime()));
            mEndTime.setText(formatter.format(getEndTime()));
            mLocation.setText(getLocation());

            // make Calendar button clickable
            mInsert.setClickable(true);
            mUpdate.setClickable(true);
            mDelete.setClickable(true);

            // ff6347 tomato 토마토
            // 0000ff blue 블루
            // 4169e1 royalblue 로열블루
            // 1e90ff dodgerblue 도저블루
            // 00bfff deepskyblue 딥스카이블루
            // 32cd32 limegreen 라임그린

            // set textColor of attributes
            fTitle.setTextColor(Color.parseColor("#0000ff"));
            fTime.setTextColor(Color.parseColor("#ff6347"));
            fStartTime.setTextColor(Color.parseColor("#ff6347"));
            fEndTime.setTextColor(Color.parseColor("#ff6347"));
            fLocation.setTextColor(Color.parseColor("#ff6347"));
            fType.setTextColor(Color.parseColor("#ff6347"));

            // set textColor of the content of attributes
            mTitle.setTextColor(Color.parseColor("#333333"));

            // set Background of calendar buttons
            mInsert.setBackgroundColor(Color.parseColor("#1e90ff"));
            mUpdate.setBackgroundColor(Color.parseColor("#1e90ff"));
            mDelete.setBackgroundColor(Color.parseColor("#1e90ff"));
        } else {
            Toast.makeText(mContext, "텍스트로부터 추출된 일정 정보가 없습니다.", Toast.LENGTH_LONG).show();
            mTitle.setText("붙여넣기 버튼을 누르세요.");
            mStartTime.setText("");
            mEndTime.setText("");
            mLocation.setText("");
            mType.setText("");

            mInsert.setClickable(false);
            mUpdate.setClickable(false);
            mDelete.setClickable(false);
        }

        super.onPostExecute(result);
        progressDialog.dismiss();
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
    }

    private boolean readJSON(String str) {
        try {
            JSONObject jroot = new JSONObject(str);
            Log.i(TAG, "JSONObject jroot --> " + jroot);
            JSONArray apListArr = jroot.getJSONArray("MTLIST");
            Log.i(TAG, "JSONArray apLIstArr --> " + apListArr);

            JSONObject jAP = apListArr.getJSONObject(0);
            String sTime = jAP.getString("STIME");
            String eTime = jAP.getString("ETIME");
            String isHeldAt = jAP.getString("ISHELDAT");
            String landmark = jAP.getString("LANDMARK");

            Log.i(TAG, "sTime --> " + sTime);
            Log.i(TAG, "eTime --> " + eTime);
            Log.i(TAG, "isHeldAt --> " + isHeldAt);
            Log.i(TAG, "landmark --> " + landmark);

            date_stime = parseDate(sTime);
            date_etime = parseDate(eTime);

            // 시작시간 혹은 종료시간의 [시:분:초]가 [01:00:00] 인 경우, 시간정보 보정
            // 시작시간 보정값: 09:00:00
            DateFormat formatter = new SimpleDateFormat("HH:mm:ss");
            if(formatter.format(date_stime).equals("01:00:00")) {
                Log.i("LogCat", "HH:mm:ss == 01:00:00");
//                String new_sTime = sTime.replace("01:00:00", "09:00:00");
                date_stime = parseDate(sTime.replace("01:00:00", "09:00:00"));
            }
            // 종료시간 보정값: 18:00:00
            if(formatter.format(date_etime).equals("01:00:00")) {
                Log.i("LogCat", "HH:mm:ss == 01:00:00");
//                String new_eTime = eTime.replace("01:00:00", "09:00:00");
                date_etime = parseDate(eTime.replace("01:00:00", "18:00:00"));
            }

            // 시작시간과 종료시간이 같을 경우, 종료시간 = 시작시간 +1 Hour
            if(date_stime.equals(date_etime)){
                Calendar cal_etime = Calendar.getInstance();
                cal_etime.setTime(date_etime);
                cal_etime.add(Calendar.HOUR, 1);
                date_etime = cal_etime.getTime();
            }

            // 추출된 정보 저장
            setStartTime(date_stime);
            setEndTime(date_etime);
            setLocation(isHeldAt);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e + "");
            return false;
        }
    }

    private void setStartTime(Date sTime) {
        startTime = sTime;
    }

    public Date getStartTime() {
        return startTime;
    }

    private void setEndTime(Date eTime) {
        endTime = eTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    private void setLocation(String isHeldAt) {
        location = isHeldAt;
    }

    public String getLocation() {
        return location;
    }

    public String getBody() {
        return body;
    }

    public static Date parseDate(String time) {
        Calendar c = Calendar.getInstance();
        c.set(1, 1, 1, 9, 0, 0);
        StringTokenizer tok = new StringTokenizer(time, " ");
        String weekDay = tok.nextToken();
        String month = tok.nextToken();
        int m = 0;
        if (month.equals("Jan")) {
            m = 1;
        }
        if (month.equals("Feb")) {
            m = 2;
        }
        if (month.equals("Mar")) {
            m = 3;
        }
        if (month.equals("Apr")) {
            m = 4;
        }
        if (month.equals("May")) {
            m = 5;
        }
        if (month.equals("Jun")) {
            m = 6;
        }
        if (month.equals("Jul")) {
            m = 7;
        }
        if (month.equals("Aug")) {
            m = 8;
        }
        if (month.equals("Sep")) {
            m = 9;
        }
        if (month.equals("Oct")) {
            m = 10;
        }
        if (month.equals("Nov")) {
            m = 11;
        }
        if (month.equals("Dec")) {
            m = 12;
        }
        int day = Integer.parseInt(tok.nextToken());

        StringTokenizer tok2 = new StringTokenizer(tok.nextToken(), ":");
        int hour = Integer.parseInt(tok2.nextToken());
        int min = Integer.parseInt(tok2.nextToken());
        int second = Integer.parseInt(tok2.nextToken());
        tok.nextToken();
        int year = Integer.parseInt(tok.nextToken());

        c.clear();
        c.set(year, m - 1, day, hour, min, second);
        return c.getTime();
    }
}
