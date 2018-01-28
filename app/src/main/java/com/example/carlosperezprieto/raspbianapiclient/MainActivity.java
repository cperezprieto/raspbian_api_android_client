package com.example.carlosperezprieto.raspbianapiclient;

import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONArray;
import org.json.JSONObject;
import android.widget.Switch;
import android.os.AsyncTask;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.CompoundButton;
import android.support.v4.content.ContextCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSIONS_REQUEST_INTERNET = 1 ;

    SharedPreferences prefs;
    String hostname;
    String port;
    Switch greenSwitch;
    Switch redSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Permissions
        ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.INTERNET)) {

            } else {

                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.INTERNET},
                        PERMISSIONS_REQUEST_INTERNET);

            }
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openSettingsActivity();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_INTERNET: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 ) { //&& grantResults[] == PackageManager.PERMISSION_GRANTED)


                } else {

                }
                return;
            }
        }
    }

    @Override
    protected void onResume() {
        prefs = PreferenceManager.getDefaultSharedPreferences(MainActivity.this);
        hostname = prefs.getString("hostname", "");
        port = prefs.getString("port", "");

        greenSwitch = (Switch) findViewById(R.id.switch1);
        redSwitch = (Switch) findViewById(R.id.switch2);

        greenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changeLedBightness("0", isChecked);
            }
        });

        redSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                changeLedBightness("1", isChecked);
            }
        });

        super.onResume();
        if (!isRaspberryAvailable(hostname, port)) {
            openSettingsActivity();
        }
        else {
            updateLedsStatus(hostname, port);
        }
    }

    protected void updateLedsStatus(String hostname, String port) {
        Response response;
        JSONArray jsonArray;

        try {
            APIConnection task = new APIConnection();
            response = task.execute(hostname, port, "GET", "/api/v1/leds").get();

            if (response.Code == 200) {
                jsonArray = new JSONArray(response.Body);

                JSONObject green = jsonArray.getJSONObject(0);
                JSONObject red = jsonArray.getJSONObject(1);

                if (green.get("status").equals("0")) {
                    greenSwitch.setChecked(false);
                }
                else
                    greenSwitch.setChecked(true);

                if (red.get("status").equals("0")) {
                    redSwitch.setChecked(false);
                }
                else
                    redSwitch.setChecked(true);
            }
            else {
                // Show message to alert unavailable
            }

        } catch(Exception e){

        }
    }


    protected void openSettingsActivity() {
        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    protected boolean isRaspberryAvailable(String hostname, String port) {
        Response response;
        try {
            APIConnection task = new APIConnection();
            response = task.execute(hostname, port, "GET", "").get();

            if (response.Code == 200)
                return true;
            else
                return false;
        } catch(Exception e){

        }
        return false;
    }

    private class APIConnection extends AsyncTask<Object, Void, Response> {
        /*ProgressDialog dialog;
        @Override
        protected void onPreExecute() {
            dialog = ProgressDialog.show(Main.this, "", "Loading....");
        }*/
        @Override
        protected Response doInBackground(Object... args) {
            String hostname = args[0].toString();
            String port = args[1].toString();
            String method = args[2].toString();
            String uri = args[3].toString();

            String composedUrl = "http://" + hostname + ":" + port;
            if (uri != "")
                composedUrl = composedUrl + uri;

            URL url;
            HttpURLConnection urlConnection = null;
            Response response = new Response();
            try {
                url = new URL(composedUrl);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setConnectTimeout(1000);
                urlConnection.setReadTimeout(2000);
                urlConnection.setRequestMethod(method);

                response.Code = urlConnection.getResponseCode();
                response.Message = urlConnection.getResponseMessage();
                BufferedReader br;

                if (200 <= response.Code && response.Code <= 299) {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                } else {
                    br = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()));
                }

                StringBuilder body = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    body.append(line).append('\n');
                }

                response.Body = body.toString();


            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return response;
        }
    }

    private void changeLedBightness(String led, boolean powered) {
        String status = "0";

        if (powered)
            status = "1";

        try {
            APIConnection task = new APIConnection();
            task.execute(hostname, port, "PUT", "/api/v1/leds/" + led + "/" + status).get();


        } catch(Exception e){

        }
    }

    private class Response {
        Integer Code;
        String Message;
        String Body;
    }
}
