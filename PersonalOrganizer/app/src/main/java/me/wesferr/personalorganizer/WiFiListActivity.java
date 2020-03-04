package me.wesferr.personalorganizer;

import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
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