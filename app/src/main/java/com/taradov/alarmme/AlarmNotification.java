package com.taradov.alarmme;

import java.util.Timer;
import java.util.TimerTask;
import android.os.Bundle;
import android.os.Vibrator;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.net.Uri;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.NotificationChannel;
import android.app.Notification.Builder;
import android.app.PendingIntent;
import android.util.Log;
import android.view.WindowManager;
import android.view.View;
import android.widget.Toast;
import android.widget.TextView;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.Manifest;
import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.location.Address;
import android.location.Geocoder;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeBaseActivity;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class AlarmNotification extends YouTubeBaseActivity
{
    private final String TAG = "AlarmMe";

    private Vibrator mVibrator;
    private final long[] mVibratePattern = { 0, 500, 500 };
    private boolean mVibrate;
    private Timer mTimer;
    private Alarm mAlarm;
    private DateTime mDateTime;
    private TextView mTextView;
    private PlayTimerTask mTimerTask;

    YouTubePlayerView youTubeView;
    YouTubePlayer.OnInitializedListener listener;

    private TextToSpeech tts;
    private GpsTracker gpsTracker;

    private static final int GPS_ENABLE_REQUEST_CODE = 2001;
    private static final int PERMISSIONS_REQUEST_CODE = 100;
    String[] REQUIRED_PERMISSIONS  = {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

    TextView selectCity, cityField, detailsField, currentTemperatureField, humidity_field, pressure_field, weatherIcon, updatedField;
    ProgressBar loader;
    Typeface weatherFont;
    String city = "SEOUL, KR";
    /* Please Put your API KEY here */
    String OPEN_WEATHER_MAP_API = "dcf9694540f1bfe69264939ec323d249";
    String temp;
    int flag=0;

    @Override
    protected void onCreate(Bundle bundle)
    {
        super.onCreate(bundle);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);

        setContentView(R.layout.notification);

        mDateTime = new DateTime(this);
        mTextView = (TextView)findViewById(R.id.alarm_title_text);

        readPreferences();

        if (mVibrate)
            mVibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        start(getIntent());

    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        Log.i(TAG, "AlarmNotification.onDestroy()");

        stop();
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        super.onNewIntent(intent);
        Log.i(TAG, "AlarmNotification.onNewIntent()");

        addNotification(mAlarm);

        stop();
        start(intent);
    }

    private void start(Intent intent) {
        mAlarm = new Alarm(this);
        mAlarm.fromIntent(intent);

        Log.i(TAG, "AlarmNotification.start('" + mAlarm.getTitle() + "')");

        mTextView.setText(mAlarm.getTitle());

        if (mVibrate)
            mVibrator.vibrate(mVibratePattern, 0);

        youTubeView = (YouTubePlayerView)findViewById(R.id.youtubeView);

        loader = (ProgressBar) findViewById(R.id.loader);
        selectCity = (TextView) findViewById(R.id.selectCity);
        cityField = (TextView) findViewById(R.id.city_field);
        updatedField = (TextView) findViewById(R.id.updated_field);
        detailsField = (TextView) findViewById(R.id.details_field);
        currentTemperatureField = (TextView) findViewById(R.id.current_temperature_field);
        humidity_field = (TextView) findViewById(R.id.humidity_field);
        pressure_field = (TextView) findViewById(R.id.pressure_field);
        weatherIcon = (TextView) findViewById(R.id.weather_icon);
        weatherFont = Typeface.createFromAsset(getAssets(), "fonts/weathericons-regular-webfont.ttf");
        weatherIcon.setTypeface(weatherFont);


        taskLoadUp(city);

        if (!checkLocationServicesStatus()) {

            showDialogForLocationServiceSetting();
        } else {

            checkRunTimePermission();
        }
        //final TextView textview_address = (TextView)findViewById(R.id.textview);

        gpsTracker = new GpsTracker(AlarmNotification.this);
        double latitude = gpsTracker.getLatitude();
        double longitude = gpsTracker.getLongitude();

        String address = getCurrentAddress(latitude, longitude);
        String[] arr = address.split(" ");
        int index = 0;
        for (int i = 0; i < arr.length; i++) {
            if (arr[i].contains("시")) {
                index = i;
                //arr[index]=arr[index].split(시);
                if (arr[index].contains("구미시")) {
                    arr[index] = "KUMI";
                } else if (arr[index].contains("서울시")) {
                    arr[index] = "SEOUL";
                } else if (arr[index].contains("부산시")) {
                    arr[index] = "BUSAN";
                } else if (arr[index].contains("대구시")) {
                    arr[index] = "DAEGU";
                } else if (arr[index].contains("인천시")) {
                    arr[index] = "INCHEON";
                } else if (arr[index].contains("대전시")) {
                    arr[index] = "DAEJEON";
                } else if (arr[index].contains("창원시")) {
                    arr[index] = "CHANGWON";
                } else if (arr[index].contains("광주시")) {
                    arr[index] = "Gwangju";
                } else if (arr[index].contains("울산시")) {
                    arr[index] = "ULSAN";
                } else if (arr[index].contains("경기도")) {
                    arr[index] = "Gyeonggi-do";
                } else if (arr[index].contains("성남시")) {
                    arr[index] = "Seongnam";
                }
                break;
            } else if (arr[i].contains("군")) {
                index = i;
                break;
            }
        }

        String str1 = " ,KR";

        final String result = arr[index].concat(str1);

        city = result;

        taskLoadUp(city);

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat sdf_H = new SimpleDateFormat("hh");
        // nowDate 변수에 값을 저장한다.
        String formatHour = sdf_H.format(date);
        final int mHour = Integer.parseInt(formatHour);

        SimpleDateFormat sdf_M = new SimpleDateFormat("mm");
        // nowDate 변수에 값을 저장한다.
        final String formatMin = sdf_M.format(date);
        final int mMinute = Integer.parseInt(formatMin);
        final String t_locate = arr[index];

        SimpleDateFormat sdf_D = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm");
        String formatDate = sdf_D.format(date);

        updatedField.setText(formatDate);



        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                String speech = "현재알람은" + String.valueOf(mAlarm.getTitle()) + "이며" + "지금시간은 " + String.valueOf(mHour) + " 시 "
                        + String.valueOf(mMinute) + " 분입니다. " +"현재 지역"+t_locate+"시이며 " + "온도는"+temp+"도 입니다.";
                tts.setLanguage(Locale.KOREA);
                tts.speak(speech, TextToSpeech.QUEUE_FLUSH, null);
            }
        });


        selectCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final String[] versionArray = new String[] { "서울시", "부산시", "대구시" ,"인천시","대전시","창원시","광주시","울산시","경기도","성남시","구미시"};
                AlertDialog.Builder alertDialog = new AlertDialog.Builder(AlarmNotification.this);

                alertDialog.setTitle("지역을 선택하세요");


                alertDialog.setSingleChoiceItems(versionArray,0,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                switch(which) {
                                    case 0:
                                        if (versionArray[which].equals("서울시")) {
                                            String str2 = "SEOUL";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;

                                        }
                                        break;
                                    case 1:
                                        if (versionArray[which].equals("부산시")) {
                                            String str2 = "BUSAN";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 2:
                                        if (versionArray[which].equals("대구시")) {
                                            String str2 = "DAEGU";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 3:
                                        if (versionArray[which].equals("인천시")) {
                                            String str2 = "iNCHEON";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 4:
                                        if (versionArray[which].equals("대전시")) {
                                            String str2 = "DAEJEON";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 5:
                                        if (versionArray[which].equals("창원시")) {
                                            String str2 = "CHANGWON";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 6:
                                        if (versionArray[which].equals("광주시")) {
                                            String str2 = "Gwangju";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 7:
                                        if (versionArray[which].equals("울산시")) {
                                            String str2 = "ULSAN";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 8:
                                        if (versionArray[which].equals("경기도")) {
                                            String str2 = "Gyeonggi-do";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 9:
                                        if (versionArray[which].equals("성남시")) {
                                            String str2 = "Seongnam";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                    case 10:
                                        if (versionArray[which].equals("구미시")) {
                                            String str2 = "kUMI";
                                            String str3 = " ,KR";
                                            str2 = str2.concat(str3);
                                            city = str2;
                                        }
                                        break;
                                }

                            }
                        }).setPositiveButton("Ok",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // OK 버튼 클릭시 , 여기서 선택한 값을 메인 Activity 로 넘기면 된다.
                                taskLoadUp(city);

                            }
                        }).setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Cancel 버튼 클릭시
                                dialog.dismiss();

                            }
                        });

                alertDialog.show();



                //taskLoadUp(city);
                // view.invalidate();
                // onResume();

                // finish();
                // startActivity(getIntent());
                // onResume();




/*
                final EditText input = new EditText(MainActivity.this);
                input.setText(city);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.MATCH_PARENT);
                input.setLayoutParams(lp);
                alertDialog.setView(input);

                alertDialog.setPositiveButton("Change",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                city = input.getText().toString();
                                taskLoadUp(city);
                            }
                        });
                alertDialog.setNegativeButton("Cancel",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                alertDialog.show();
                */
            }


        });

        listener = new YouTubePlayer.OnInitializedListener(){

            //초기화 성공시
            @Override
            public void onInitializationSuccess(YouTubePlayer.Provider provider, final YouTubePlayer youTubePlayer, boolean b) {
                
                mTimerTask = new PlayTimerTask() {
                    @Override
                    public void run () {
                        mVibrator.cancel();
                        youTubePlayer.loadVideo("U_sYIKWhJvk");//url의 맨 뒷부분 ID값만 넣으면 됨
                    }
                };
                mTimer = new Timer();
                mTimer.schedule(mTimerTask, 5500);
            }

            @Override
            public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {
            }

        };
        //첫번째 인자는 API키값 두번째는 실행할 리스너객체를 넘겨줌
        youTubeView.initialize("AIzaSyA2iknf3MgcWLC-0iEv9RpvgbXCf-Zu65M", listener);


    }

    private void stop()
    {
        Log.i(TAG, "AlarmNotification.stop()");

        mTimer.cancel();
        if (mVibrate)
            mVibrator.cancel();
    }

    private void readPreferences()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        mVibrate = prefs.getBoolean("vibrate_pref", true);

    }

    private void addNotification(Alarm alarm)
    {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        Notification notification;
        PendingIntent activity;
        Intent intent;

        Log.i(TAG, "AlarmNotification.addNotification(" + alarm.getId() + ", '" + alarm.getTitle() + "', '" + mDateTime.formatDetails(alarm) + "')");

        intent = new Intent(this.getApplicationContext(), AlarmMe.class);
        intent.setAction(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        activity = PendingIntent.getActivity(this, (int)alarm.getId(), intent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationChannel channel = new NotificationChannel("alarmme_01", "AlarmMe Notifications",
                NotificationManager.IMPORTANCE_DEFAULT);

        notification = new Notification.Builder(this)
                .setContentIntent(activity)
                .setSmallIcon(R.drawable.ic_notification)
                .setAutoCancel(true)
                .setContentTitle("Missed alarm: " + alarm.getTitle())
                .setContentText(mDateTime.formatDetails(alarm))
                .setChannelId("alarmme_01")
                .build();

        notificationManager.createNotificationChannel(channel);

        notificationManager.notify((int)alarm.getId(), notification);
    }

    @Override
    public void onBackPressed()
    {
        finish();
    }

    private class PlayTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            Log.i(TAG, "AlarmNotification.PalyTimerTask.run()");
            addNotification(mAlarm);
            finish();
        }
    }
    public void taskLoadUp(String query) {
        if (Function.isNetworkAvailable(getApplicationContext())) {
            AlarmNotification.DownloadWeather task = new AlarmNotification.DownloadWeather();
            task.execute(query);
        } else {
            Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_LONG).show();
        }
    }

    class DownloadWeather extends AsyncTask< String, Void, String > {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            loader.setVisibility(View.VISIBLE);

        }
        protected String doInBackground(String...args) {
            String xml = Function.excuteGet("http://api.openweathermap.org/data/2.5/weather?q=" + args[0] +
                    "&units=metric&appid=" + OPEN_WEATHER_MAP_API);
            return xml;
        }
        @Override
        protected void onPostExecute(String xml) {

            try {
                JSONObject json = new JSONObject(xml);
                if (json != null) {
                    JSONObject details = json.getJSONArray("weather").getJSONObject(0);
                    JSONObject main = json.getJSONObject("main");
                    DateFormat df = DateFormat.getDateTimeInstance();

                    cityField.setText("도시: "+json.getString("name").toUpperCase(Locale.KOREA) + ", 국적: " + json.getJSONObject("sys").getString("country"));
                    detailsField.setText("날씨: "+details.getString("description").toUpperCase(Locale.KOREA));
                    currentTemperatureField.setText(String.format("%.2f", main.getDouble("temp")) + "°");

                    double temp_test = main.getDouble("temp");
                    temp = Double.toString(temp_test);


                    // currentTemperatureField.getText(String.format())
                    humidity_field.setText("습도: " + main.getString("humidity") + "%");
                    pressure_field.setText("기압: " + main.getString("pressure") + " hPa");
                    //updatedField.setText(df.format(new Date(json.getLong("dt") * 1000)));
                    weatherIcon.setText(Html.fromHtml(Function.setWeatherIcon(details.getInt("id"),
                            json.getJSONObject("sys").getLong("sunrise") * 1000,
                            json.getJSONObject("sys").getLong("sunset") * 1000)));

                    loader.setVisibility(View.GONE);

                }
            } catch (JSONException e) {
                Toast.makeText(getApplicationContext(), "Error, Check City", Toast.LENGTH_SHORT).show();
            }

        }

    }

    /*
     * ActivityCompat.requestPermissions를 사용한 퍼미션 요청의 결과를 리턴받는 메소드입니다.
     */
    @Override
    public void onRequestPermissionsResult(int permsRequestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grandResults) {

        if ( permsRequestCode == PERMISSIONS_REQUEST_CODE && grandResults.length == REQUIRED_PERMISSIONS.length) {

            // 요청 코드가 PERMISSIONS_REQUEST_CODE 이고, 요청한 퍼미션 개수만큼 수신되었다면

            boolean check_result = true;


            // 모든 퍼미션을 허용했는지 체크합니다.

            for (int result : grandResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    check_result = false;
                    break;
                }
            }


            if ( check_result ) {

                //위치 값을 가져올 수 있음
                ;
            }
            else {
                // 거부한 퍼미션이 있다면 앱을 사용할 수 없는 이유를 설명해주고 앱을 종료합니다.2 가지 경우가 있습니다.

                if (ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[0])
                        || ActivityCompat.shouldShowRequestPermissionRationale(this, REQUIRED_PERMISSIONS[1])) {

                    Toast.makeText(AlarmNotification.this, "퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용해주세요.", Toast.LENGTH_LONG).show();
                    finish();


                }else {

                    Toast.makeText(AlarmNotification.this, "퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용해야 합니다. ", Toast.LENGTH_LONG).show();

                }
            }

        }
    }

    void checkRunTimePermission(){
        //런타임 퍼미션 처리
        // 1. 위치 퍼미션을 가지고 있는지 체크합니다.
        int hasFineLocationPermission = ContextCompat.checkSelfPermission(AlarmNotification.this,
                Manifest.permission.ACCESS_FINE_LOCATION);
        int hasCoarseLocationPermission = ContextCompat.checkSelfPermission(AlarmNotification.this,
                Manifest.permission.ACCESS_COARSE_LOCATION);


        if (hasFineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                hasCoarseLocationPermission == PackageManager.PERMISSION_GRANTED) {

            // 2. 이미 퍼미션을 가지고 있다면
            // ( 안드로이드 6.0 이하 버전은 런타임 퍼미션이 필요없기 때문에 이미 허용된 걸로 인식합니다.)


            // 3.  위치 값을 가져올 수 있음


        } else {  //2. 퍼미션 요청을 허용한 적이 없다면 퍼미션 요청이 필요합니다. 2가지 경우(3-1, 4-1)가 있습니다.

            // 3-1. 사용자가 퍼미션 거부를 한 적이 있는 경우에는
            if (ActivityCompat.shouldShowRequestPermissionRationale(AlarmNotification.this, REQUIRED_PERMISSIONS[0])) {

                // 3-2. 요청을 진행하기 전에 사용자가에게 퍼미션이 필요한 이유를 설명해줄 필요가 있습니다.
                Toast.makeText(AlarmNotification.this, "이 앱을 실행하려면 위치 접근 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                // 3-3. 사용자게에 퍼미션 요청을 합니다. 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(AlarmNotification.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);


            } else {
                // 4-1. 사용자가 퍼미션 거부를 한 적이 없는 경우에는 퍼미션 요청을 바로 합니다.
                // 요청 결과는 onRequestPermissionResult에서 수신됩니다.
                ActivityCompat.requestPermissions(AlarmNotification.this, REQUIRED_PERMISSIONS,
                        PERMISSIONS_REQUEST_CODE);
            }

        }

    }

    public String getCurrentAddress( double latitude, double longitude) {

        //지오코더... GPS를 주소로 변환
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());

        List<Address> addresses;

        try {
            addresses = geocoder.getFromLocation(
                    latitude,
                    longitude,
                    7);
        } catch (IOException ioException) {
            //네트워크 문제
            Toast.makeText(this, "지오코더 서비스 사용불가", Toast.LENGTH_LONG).show();
            return "지오코더 서비스 사용불가";
        } catch (IllegalArgumentException illegalArgumentException) {
            Toast.makeText(this, "잘못된 GPS 좌표", Toast.LENGTH_LONG).show();
            return "잘못된 GPS 좌표";

        }



        if (addresses == null || addresses.size() == 0) {
            Toast.makeText(this, "주소 미발견", Toast.LENGTH_LONG).show();
            return "주소 미발견";

        }

        Address address = addresses.get(0);
        return address.getAddressLine(0).toString()+"\n";

    }


    //여기부터는 GPS 활성화를 위한 메소드들
    private void showDialogForLocationServiceSetting() {

        AlertDialog.Builder builder = new AlertDialog.Builder(AlarmNotification.this);
        builder.setTitle("위치 서비스 비활성화");
        builder.setMessage("앱을 사용하기 위해서는 위치 서비스가 필요합니다.\n"
                + "위치 설정을 수정하실래요?");
        builder.setCancelable(true);
        builder.setPositiveButton("설정", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                Intent callGPSSettingIntent
                        = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                startActivityForResult(callGPSSettingIntent, GPS_ENABLE_REQUEST_CODE);
            }
        });
        builder.setNegativeButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        builder.create().show();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case GPS_ENABLE_REQUEST_CODE:

                //사용자가 GPS 활성 시켰는지 검사
                if (checkLocationServicesStatus()) {
                    if (checkLocationServicesStatus()) {

                        Log.d("@@@", "onActivityResult : GPS 활성화 되있음");
                        checkRunTimePermission();
                        return;
                    }
                }

                break;
        }
    }

    public boolean checkLocationServicesStatus() {
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
}

