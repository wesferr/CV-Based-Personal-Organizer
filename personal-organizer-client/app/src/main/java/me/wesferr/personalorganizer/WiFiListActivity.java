package me.wesferr.personalorganizer;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HeaderViewListAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class WiFiListActivity extends AppCompatActivity {
    //LIST OF ARRAY STRINGS WHICH WILL SERVE AS LIST ITEMS
    ArrayList<String> listItems=new ArrayList<String>();

    //DEFINING A STRING ADAPTER WHICH WILL HANDLE THE DATA OF THE LISTVIEW
    ArrayAdapter<String> adapter;

    //RECORDING HOW MANY TIMES THE BUTTON HAS BEEN CLICKED
    int clickCounter=0;
    private ListView mListView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_wi_fi_list);

        if (mListView == null) {
            mListView = (ListView) findViewById(R.id.wifi_list);
        }

        adapter=new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listItems);
        setListAdapter(adapter);
    }

    //METHOD WHICH WILL HANDLE DYNAMIC INSERTION
    public void addItems(View v) {
        listItems.add("Clicked : "+clickCounter++);
        adapter.notifyDataSetChanged();
    }

    protected ListView getListView() {
        if (mListView == null) {
            mListView = (ListView) findViewById(R.id.wifi_list);
        }
        return mListView;
    }

    protected void setListAdapter(ListAdapter adapter) {
        getListView().setAdapter(adapter);
    }

    protected ListAdapter getListAdapter() {
        ListAdapter adapter = getListView().getAdapter();
        if (adapter instanceof HeaderViewListAdapter) {
            return ((HeaderViewListAdapter)adapter).getWrappedAdapter();
        } else {
            return adapter;
        }
    }
}

//listview = findViewById(R.id.wifi_list);
//Addbutton = findViewById(R.id.capture_wifi);
//
//
//Addbutton.setOnClickListener(new View.OnClickListener() {
//
//@Override
//public void onClick(View v) {
//
//final ArrayAdapter< String > adapter = new ArrayAdapter < String >
//                        (MainActivity.this, android.R.layout.simple_list_item_1,
//                                ListElementsArrayList);
//
//                                listview.setAdapter(adapter);
//                                ListElementsArrayList.add(GetValue.getText().toString());
//                                adapter.notifyDataSetChanged();
//                                }
//                                });

//wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        if (!wifiManager.isWifiEnabled())
//        {
//        Toast.makeText(getApplicationContext(), "Wifi desligado, Ligando", Toast.LENGTH_LONG).show();
//        wifiManager.setWifiEnabled(true);
//        }
//
//        wifiScanReciever = new BroadcastReceiver() {
//@Override
//public void onReceive(Context context, Intent intent) {
//        List<ScanResult> results = wifiManager.getScanResults();
//        Log.i(TAG, "Redes Escaneadas: " + results.size());
//
//        try {
//        FileOutputStream file = context.openFileOutput("redes.csv", Context.MODE_PRIVATE);
//        file.write("Rede;Sinal".getBytes());
//
//        for (ScanResult scanResult : results) {
//        Log.i(TAG, "Rede: " + scanResult.SSID + " - Sinal: " + scanResult.level);
//        Log.i(TAG, "onReceive: " + context.getFilesDir());
//        file.write((scanResult.SSID+";"+scanResult.level).getBytes());
//        }
//
//        file.close();
//
//        } catch (IOException e) {
//        Log.e("Exception", "File write failed: " + e.toString());
//        }
//        }
//        };

//
//    Button buttonScan = findViewById(R.id.capture_wifi);
//        buttonScan.setOnClickListener(new View.OnClickListener() {
//@Override
//public void onClick(View view) {
//        IntentFilter intentFilter = new IntentFilter();
//        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
//        getApplicationContext().registerReceiver(wifiScanReciever, intentFilter);
//        wifiManager.startScan();
//        Toast.makeText(getApplicationContext(), "Escaneando wifi", Toast.LENGTH_SHORT).show();
//        }
//        });