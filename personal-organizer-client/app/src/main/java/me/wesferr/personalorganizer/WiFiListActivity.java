package me.wesferr.personalorganizer;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class WiFiListActivity extends AppCompatActivity {

    ListView listView;
    WiFiScanner wifiScanner;
    ArrayList<ScanResult> listItems;
    WifiElementAdapter adapter;
    TextView azimuteText;

    float azimuth = 0;
    SensorManager sensorManager = null;
    Sensor acelerometer, magnetometer;

    float[] acelerometerVector = new float[3];
    float[] magnetometerVector = new float[3];
    float[] rotation = new float[9];
    float[] inclination = new float[9];


    private SensorEventListener mSensorEventListener = new SensorEventListener() {

        float[] orientation = new float[3];
        float[] rMat = new float[9];

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float alpha = 0.97f;
            synchronized (this) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                    acelerometerVector[0] = alpha * acelerometerVector[0] + (1 - alpha) * event.values[0];
                    acelerometerVector[1] = alpha * acelerometerVector[1] + (1 - alpha) * event.values[1];
                    acelerometerVector[2] = alpha * acelerometerVector[2] + (1 - alpha) * event.values[2];

                }

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                    magnetometerVector[0] = alpha * magnetometerVector[0] + (1 - alpha) * event.values[0];
                    magnetometerVector[1] = alpha * magnetometerVector[1] + (1 - alpha) * event.values[1];
                    magnetometerVector[2] = alpha * magnetometerVector[2] + (1 - alpha) * event.values[2];

                }
            }

            boolean success = SensorManager.getRotationMatrix(rotation, inclination, acelerometerVector, magnetometerVector);

            if(success) {

                float orientation[] = new float[3];
                SensorManager.getOrientation(rotation, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;
                azimuteText.setText("" + azimuth);

            }
        }

    };


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi_list);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        acelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(mSensorEventListener, acelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(mSensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_GAME);

        wifiScanner = new WiFiScanner(getApplicationContext(), this);


        listView = (ListView) findViewById(R.id.wifi_list);
        azimuteText = (TextView) findViewById(R.id.azimute_text);
        listItems = new ArrayList<ScanResult>();
        adapter = new WifiElementAdapter(listItems, this);
        listView.setAdapter(adapter);

        Button buttonScan = findViewById(R.id.capture_wifi);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                wifiScanner.scan_itens();
            }
        });

    }

    public void addItems(ScanResult scan) {
        listItems.add(scan);
        adapter.notifyDataSetChanged();
    }

}