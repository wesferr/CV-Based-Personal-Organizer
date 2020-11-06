package me.wesferr.personalorganizer;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import java.util.ArrayList;

import static android.content.Context.SENSOR_SERVICE;

public class SensorsScanner {

    SensorManager sensorManager;
    Sensor accelerometer, magnetometer;

    float[] accelerometerVector = new float[3];
    float[] magnetometerVector = new float[3];
    float[] rotation = new float[9];
    float[] inclination = new float[9];
    Float azimuth = null;
    boolean success;
    Context context;

    SensorEventListener mSensorEventListener = new SensorEventListener() {

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            float alpha = 0.97f;
            synchronized (this) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

                    accelerometerVector[0] = alpha * accelerometerVector[0] + (1 - alpha) * event.values[0];
                    accelerometerVector[1] = alpha * accelerometerVector[1] + (1 - alpha) * event.values[1];
                    accelerometerVector[2] = alpha * accelerometerVector[2] + (1 - alpha) * event.values[2];

                }

                if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

                    magnetometerVector[0] = alpha * magnetometerVector[0] + (1 - alpha) * event.values[0];
                    magnetometerVector[1] = alpha * magnetometerVector[1] + (1 - alpha) * event.values[1];
                    magnetometerVector[2] = alpha * magnetometerVector[2] + (1 - alpha) * event.values[2];

                }
            }
            success = SensorManager.getRotationMatrix(rotation, inclination, accelerometerVector, magnetometerVector);

            if(success) {

                float[] orientation = new float[3];
                SensorManager.getOrientation(rotation, orientation);
                azimuth = (float) Math.toDegrees(orientation[0]); // orientation
                azimuth = (azimuth + 360) % 360;

            }
        }

    };

    ArrayList<String> get_wifi_list(){
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        ArrayList<ScanResult> redes = (ArrayList<ScanResult>) wifiManager.getScanResults();

        ArrayList<String> string_redes = new ArrayList<>();
        for(ScanResult rede: redes){
            string_redes.add("{\"SSID\": \"" + rede.SSID + "\"; \"BSSID\": \"" + rede.BSSID + "\"; \"LEVEL\": " + rede.level + "}");
        }

        return string_redes;
    }

    boolean azimuth_ready(){
        return this.azimuth != null;
    }

    float get_azimuth(){
        return this.azimuth;
    }

    SensorsScanner(Context context){

        this.context = context;

        sensorManager = (SensorManager) context.getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sensorManager.registerListener(mSensorEventListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(mSensorEventListener, magnetometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

}