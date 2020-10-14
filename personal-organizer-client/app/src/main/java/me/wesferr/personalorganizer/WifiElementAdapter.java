package me.wesferr.personalorganizer;

import android.app.Activity;
import android.net.wifi.ScanResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

public class WifiElementAdapter extends BaseAdapter {

    private final ArrayList<ScanResult> wifi_list;
    private final Activity act;
    private Integer counter = 0;

    public WifiElementAdapter(ArrayList<ScanResult> wifi_list, Activity act) {
        this.wifi_list = wifi_list;
        this.act = act;
    }

    @Override
    public int getCount() {
        return wifi_list.size();
    }

    @Override
    public Object getItem(int position) {
        return wifi_list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = act.getLayoutInflater().inflate(R.layout.wi_fi_list_element, parent,false);

        ScanResult rede = wifi_list.get(position);

        TextView counter = (TextView) view.findViewById(R.id.wifi_counter);
        TextView ssid = (TextView) view.findViewById(R.id.wifi_ssid);
        TextView bssid = (TextView) view.findViewById(R.id.wifi_bssid);
        TextView level = (TextView) view.findViewById(R.id.wifi_level);

        counter.setText(new Integer(position).toString());
        ssid.setText(rede.SSID);
        bssid.setText(rede.BSSID);
        level.setText(new Integer(rede.level).toString());

        return view;

    }
}
