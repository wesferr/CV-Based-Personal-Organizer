package me.wesferr.personalorganizer;

import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class WiFiListActivity extends AppCompatActivity {

    private ListView listView;
    private WiFiScanner wifiScanner;
    ArrayList<ScanResult> listItems;
    WifiElementAdapter adapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wi_fi_list);

        wifiScanner = new WiFiScanner(getApplicationContext(), this);


        listView = (ListView) findViewById(R.id.wifi_list);
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