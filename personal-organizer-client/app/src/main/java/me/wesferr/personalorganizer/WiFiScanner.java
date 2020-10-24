package me.wesferr.personalorganizer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WiFiScanner {

    WifiManager wifiManager;
    Context context;
    WiFiListActivity listActivity;


    public WiFiScanner(Context context, WiFiListActivity wiFiListActivity){

        this.context = context;
        this.listActivity = wiFiListActivity;

        wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if ( !wifiManager.isWifiEnabled() ) {
            Toast.makeText(context, "Wifi desligado, Ligando", Toast.LENGTH_LONG).show();
            wifiManager.setWifiEnabled(true);
        }

    }
    BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
        private static final String TAG = "wifi";

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
                    listActivity.addItems(scanResult);
                }

                file.close();

            } catch (IOException e) {
                Log.e("Exception", "File write failed: " + e.toString());
            }



        }
    };

    public void scan_itens(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        this.context.registerReceiver(wifiScanReceiver, intentFilter);
//        wifiManager.startScan();
        ArrayList<ScanResult> redes = (ArrayList<ScanResult>) wifiManager.getScanResults();
        for(ScanResult rede: redes){
            Log.d("redes", rede.SSID);
            listActivity.addItems(rede);
        }
//        Toast.makeText(this.context, redes.get(0).SSID, Toast.LENGTH_SHORT).show();
        Toast.makeText(this.context, "Escaneando wifi", Toast.LENGTH_SHORT).show();
    }


}
