package kr.ac.kaist.nlp2cal;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.*;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class MyActivity extends Activity {
    private static final String TAG = "MyActivity";

    private Context context;
    private ClipboardManager myClipboard;
    private Extractor extractor;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        context = this;
        myClipboard = (ClipboardManager)getSystemService(CLIPBOARD_SERVICE);

        // Pasting plain text
        // To paste the string, you get the clip object from the clipboard and copy the string to into your application's storage.


        // 복사된 텍스트가 있으면, 시공간 정보 추출 실행
        // 복사된 텍스트가 없으면, 텍스트를 복사하라는 경고창. AlertDialog 혹은 Toast


        // initialize layout
        // setOnClickListener()
        // 붙여넣기 button 눌렀을 때, editable text view 에 추출된 시공간 정보 set
        // 추출된 시공간 정보가 없으면, 시공간 정보가 있는 텍스트 복사라하라는 경고창. AlertDialog 혹은 Toast
        // 신규 button 눌렀을 때, 캘린더에 이벤트 등록. CalendarManager.insertEvent()
        // 변경 button 눌렀을 때, 캘린더에 있는 이벤트를 찾아서 변경. CalendarManager.findEvent() --> updateEvent()
        // 취소 button 눌렀을 때, 캘린더에 있는 이벤트를 찾아서 삭제. CalendarManager.findEvent() --> deleteEvent()


        // 복사된 텍스트가 있으면, 시공간 정보 추출 실행
        // 복사된 텍스트가 없으면, 텍스트를 복사하라는 경고창. AlertDialog 혹은 Toast

        // initialize layout
        // setOnClickListener()
        // 붙여넣기 button 눌렀을 때, editable text view 에 추출된 시공간 정보 set
        // 추출된 시공간 정보가 없으면, 시공간 정보가 있는 텍스트 복사라하라는 경고창. AlertDialog 혹은 Toast
        // 신규 button 눌렀을 때, 캘린더에 이벤트 등록. CalendarManager.insertEvent()
        // 변경 button 눌렀을 때, 캘린더에 있는 이벤트를 찾아서 변경. CalendarManager.findEvent() --> updateEvent()
        // 취소 button 눌렀을 때, 캘린더에 있는 이벤트를 찾아서 삭제. CalendarManager.findEvent() --> deleteEvent()
    }

    @SuppressLint("NewApi")
    public void paste(View view){
//        context = getApplicationContext();

        // If the clipboard doesn't contain data, alert.
        // If it does contain data, decide if you can handle the data.
        if (!(myClipboard.hasPrimaryClip())) {

            Toast.makeText(context, "the clipboard doesn't contain data.", Toast.LENGTH_SHORT).show();

        } else if (!(myClipboard.getPrimaryClipDescription().hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN))) {

            // This alerts, since the clipboard has data but it is not plain text
            Toast.makeText(context, "the clipboard has data but it is no plain text.", Toast.LENGTH_SHORT).show();

        } else {

            // This gets the clipboard as text, since the clipboard contains plain text.
            ClipData.Item item = myClipboard.getPrimaryClip().getItemAt(0);
            String pasteData = item.getText().toString();
//            Toast.makeText(context, "Text Pasted", Toast.LENGTH_SHORT).show();

            Log.i(TAG, "pasteData: " + pasteData);
//            Extractor(pasteData);

            TextView aTitle = (TextView) findViewById(R.id.attrTitle);
            TextView aTime = (TextView) findViewById(R.id.attrTime);
            TextView aStartTime = (TextView) findViewById(R.id.attrStartTime);
            TextView aEndTime = (TextView) findViewById(R.id.attrEndTime);
            TextView aLocation = (TextView) findViewById(R.id.attrLocation);
            TextView aType = (TextView) findViewById(R.id.attrType);

            TextView title = (TextView) findViewById(R.id.textViewTitle);
            TextView startTime = (TextView) findViewById(R.id.textViewStartTime);
            TextView endTime = (TextView) findViewById(R.id.textViewEndTime);
            TextView location = (TextView) findViewById(R.id.textViewLocation);
            TextView type = (TextView) findViewById(R.id.textViewType);

            TextView btnInsert = (TextView) findViewById(R.id.textViewInsert);
            TextView btnUpdate = (TextView) findViewById(R.id.textViewUpdate);
            TextView btnDelete = (TextView) findViewById(R.id.textViewDelete);

            extractor = new Extractor(context, pasteData,
                    aTitle, aTime, aStartTime, aEndTime, aLocation, aType,
                    title, startTime, endTime, location, type,
                    btnInsert, btnUpdate, btnDelete);
            extractor.execute("");

            Log.i(TAG, "붙여넣기 버튼 종료");
        }
    }

    @SuppressLint("NewApi")
    public void insertEvent(View view) {
        CalendarManager.insertEvent(context, extractor.getBody(), extractor.getLocation(), extractor.getStartTime(), extractor.getEndTime());
    }

    @SuppressLint("NewApi")
    public void updateEvent(View view) {
//        Toast.makeText(context, "update", Toast.LENGTH_SHORT).show();
        // CalendarManager.findEvent()
        // 지금은 유사도가 높은 top three event 중에 사용자가 선택한 eventId 가 return 됨
        CalendarManager.findEvent(context, extractor.getBody(), extractor.getStartTime(), extractor.getEndTime(), extractor.getLocation(), "update");
    }

    @SuppressLint("NewApi")
    public void deleteEvent(View view) {
//        Toast.makeText(context, "delete", Toast.LENGTH_SHORT).show();
        CalendarManager.findEvent(context, extractor.getBody(), extractor.getStartTime(), extractor.getEndTime(), extractor.getLocation(), "cancel");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_activity_actions, menu);

//        return true;
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            // action with ID action_samples was selected
            case R.id.action_sample:
                Intent insertIntent = new Intent(getApplicationContext(), SampleActivity.class);
                startActivity(insertIntent);
                return true;
//            case R.id.action_update:
//                Intent updateIntent = new Intent(getApplicationContext(), UpdateSampleActivity.class);
//                startActivity(updateIntent);
//                return true;
//            case R.id.action_delete:
//                Intent deleteIntent = new Intent(getApplicationContext(), DeleteSampleActivity.class);
//                startActivity(deleteIntent);
//                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
