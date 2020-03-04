package me.wesferr.personalorganizer;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.camera2.CameraAccessException;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity {

    String TAG = "CAMERALOG";
    TextureView previewView;
    String[] permissions = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.READ_EXTERNAL_STORAGE,
    };

    final int CAMERA_REQUEST_CODE = 0x1;
    final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 0x2;
    final int ACCESS_WIFI_STATE_REQUEST_CODE = 0x4;
    final int CHANGE_WIFI_STATE_REQUEST_CODE = 0x8;
    final int ACCESS_FINE_LOCATION_STATE_REQUEST_CODE = 0x10;
    final int READ_EXTERNAL_STORAGE_REQUEST_CODE = 0x20;

    final int permissions_request_codes =
        CAMERA_REQUEST_CODE +
        WRITE_EXTERNAL_STORAGE_REQUEST_CODE +
        ACCESS_WIFI_STATE_REQUEST_CODE +
        CHANGE_WIFI_STATE_REQUEST_CODE +
        ACCESS_FINE_LOCATION_STATE_REQUEST_CODE +
        READ_EXTERNAL_STORAGE_REQUEST_CODE;

    CameraPreviewClass previewClass;

    TakePhotoClass photoClass;
    BroadcastReceiver wifiScanReciever;
    WifiManager wifiManager;
    ListView listview;
    Button Addbutton;
    final List < String > ListElementsArrayList = new ArrayList< String >();
    EditText GetValue;



    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //carrega a texture view para a previsualização
        previewView = findViewById(R.id.textureView);


        Log.i(TAG, "0.0.028");
        if (ContextCompat.checkSelfPermission(this, permissions[0]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[1]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[2]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[3]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[4]) != PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, permissions[5]) != PERMISSION_GRANTED) {



            ActivityCompat.requestPermissions(this, permissions, permissions_request_codes);

        } else {

            permissionsSuccess();

        }



    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.i(TAG, "onRequestPermissionsResult: "+ requestCode);

        boolean permissions_granted =
                grantResults.length > 5 && grantResults[0] == PERMISSION_GRANTED &&
                grantResults[1] == PERMISSION_GRANTED &&
                grantResults[2] == PERMISSION_GRANTED &&
                grantResults[3] == PERMISSION_GRANTED &&
                grantResults[4] == PERMISSION_GRANTED &&
                grantResults[5] == PERMISSION_GRANTED;

        if (requestCode == permissions_request_codes ) {
            if ( permissions_granted ) {
                permissionsSuccess();
            } else {
                permissionsFailed();
            }
        }
    }

    protected void permissionsSuccess(){
        previewClass = new CameraPreviewClass();
        previewClass.initPreview(this, previewView);

        photoClass = new TakePhotoClass();
        photoClass.setupCallback(this);

        Button buttonScan = findViewById(R.id.capture_wifi);
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IntentFilter intentFilter = new IntentFilter();
                intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
                getApplicationContext().registerReceiver(wifiScanReciever, intentFilter);
                wifiManager.startScan();
                Toast.makeText(getApplicationContext(), "Escaneando wifi", Toast.LENGTH_SHORT).show();
            }
        });

        Button buttonPhoto = findViewById(R.id.take_photo);
        buttonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                photoClass.takePicture(previewClass);
            }
        });


        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled())
        {
            Toast.makeText(getApplicationContext(), "Wifi desligado, Ligando", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

        wifiScanReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                List<ScanResult> results = wifiManager.getScanResults();
                Log.i(TAG, "Redes Escaneadas: " + results.size());

                try {
                    FileOutputStream file = context.openFileOutput("redes.csv", Context.MODE_PRIVATE);
                    file.write("Rede;Sinal".getBytes());

                    for (ScanResult scanResult : results) {
                        Log.i(TAG, "Rede: " + scanResult.SSID + " - Sinal: " + scanResult.level);
                        Log.i(TAG, "onReceive: " + context.getFilesDir());
                        file.write((scanResult.SSID+";"+scanResult.level).getBytes());
                    }

                    file.close();

                } catch (IOException e) {
                    Log.e("Exception", "File write failed: " + e.toString());
                }
            }
        };

        listview = findViewById(R.id.wifi_list);
        Addbutton = findViewById(R.id.capture_wifi);


        Addbutton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                final ArrayAdapter< String > adapter = new ArrayAdapter < String >
                        (MainActivity.this, android.R.layout.simple_list_item_1,
                                ListElementsArrayList);

                listview.setAdapter(adapter);
                ListElementsArrayList.add(GetValue.getText().toString());
                adapter.notifyDataSetChanged();
            }
        });


    }

    protected void permissionsFailed(){
        Toast.makeText(this, "Desculpe, todas as permissões são necessarias", Toast.LENGTH_LONG).show();
    }


    @Override
    protected void onResume(){
        super.onResume();
        if (previewClass!=null){
            previewClass.resumePreview();
        }
    }

    protected void onPause(){
        if (previewClass!=null){
            previewClass.pausePreview();
        }
        super.onPause();
    }

}
