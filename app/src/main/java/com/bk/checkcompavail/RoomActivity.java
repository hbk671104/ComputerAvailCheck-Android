package com.bk.checkcompavail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bk.computeravailcheck.R;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class RoomActivity extends Activity {
	
	final private int INDEX_ROOM_NUMBER = 0;
	final private int INDEX_WINDOWS = 4;
	final private int INDEX_MACINTOSH = 5;
	final private int INDEX_LINUX = 6;

    private String oppCode, buildingName;

	private Semaphore lock;
    private List<String> roomNumber, availWin, availMac, availLinux;
    private List<ListItem> itemList;
    private ItemListAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
        setContentView(R.layout.room_listview_activity);

        // Opening transition animation
        //overridePendingTransition(R.animator.activity_open_translate, R.animator.activity_close_scale);

		/* 
		 * Create an intent that carries the data transfered 
		 * from MapActivity and retrieve the oppcode
		 */
		Intent intent = getIntent();
        oppCode = intent.getStringExtra("OppCode");
        buildingName = intent.getStringExtra("BuildingName");

        // Set the activity title
        setTitle(buildingName);

		// Instantiate the Semaphore object
		lock = new Semaphore(1);

        // Query data in a separate thread
        queryData();

        // Populate things
        populateItemList();
        populateListView();

        // Pull to refresh
        final SwipeRefreshLayout swipeLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_container);
        swipeLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {

            @Override
            public void onRefresh() {

                // Query data in a separate thread
                queryData();

                // Populate things
                populateItemList();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        adapter.notifyDataSetChanged();
                    }
                });

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        swipeLayout.setRefreshing(false);
                        Toast.makeText(getApplicationContext(), "Success:)", Toast.LENGTH_SHORT).show();
                    }
                }, 1000);

            }

        });

	}

    private void populateItemList() {

        // Instantiate itemlist
        itemList = new ArrayList<ListItem>();

        itemList.add(new ListItem("Room Number", "Available"));
        for (int i = 0; i < roomNumber.size(); i++) {

            String room = roomNumber.get(i);
            String win = availWin.get(i);

            itemList.add(new ListItem(room, win));

        }

        itemList.add(new ListItem("Room Number", "Available"));
        for (int i = 0; i < roomNumber.size(); i++) {

            String room = roomNumber.get(i);
            String mac = availMac.get(i);

            itemList.add(new ListItem(room, mac));

        }

        itemList.add(new ListItem("Room Number", "Available"));
        for (int i = 0; i < roomNumber.size(); i++) {

            String room = roomNumber.get(i);
            String linux = availLinux.get(i);

            itemList.add(new ListItem(room, linux));

        }

    }

    private void populateListView() {

        adapter = new ItemListAdapter(this, itemList);
        StickyListHeadersListView listView = (StickyListHeadersListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);

    }

    private class ItemListAdapter extends BaseAdapter implements StickyListHeadersAdapter {

        private List<ListItem> listItem;
        private LayoutInflater inflater;
        private int size;

        public ItemListAdapter(Context context, List<ListItem> objects) {
            listItem = objects;
            inflater = LayoutInflater.from(context);
            size = listItem.size() / 3;
        }

        @Override
        public int getCount() {
            return listItem.size();
        }

        @Override
        public Object getItem(int position) {
            return listItem.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View itemView = convertView;
            if (itemView == null)
                itemView = inflater.inflate(R.layout.item_view, parent, false);

            // Current item
            ListItem item = itemList.get(position);

            TextView room = (TextView) itemView.findViewById(R.id.textViewRoomNumber);
            room.setText(item.getRoomNumber());

            TextView avail = (TextView) itemView.findViewById(R.id.textViewAvail);
            avail.setText(item.getAvailables());

            return itemView;

        }

        @Override
        public View getHeaderView(int position, View convertView, ViewGroup parent) {

            View headerView = convertView;
            if (headerView == null)
                headerView = inflater.inflate(R.layout.header_view, parent, false);

            ImageView imageView = (ImageView) headerView.findViewById(R.id.imageView);
            if (position >= 0 && position < size) {
                imageView.setImageResource(R.drawable.systems_windows_8_icon_128);
            } else if (position >= size && position < size * 2) {
                imageView.setImageResource(R.drawable.systems_mac_os_128);
            } else {
                imageView.setImageResource(R.drawable.systems_linux_icon_128);
            }

            return headerView;

        }

        @Override
        public long getHeaderId(int i) {

            int id;

            if (i < size)
                id = 0;
            else if (i >= size && i < size * 2)
                id = 1;
            else
                id = 2;

            return id;

        }

        @Override
        public boolean isEnabled(int position) {
            return false;
        }

    }

    protected void queryData() {

        // Instantiate arrays
        roomNumber = new ArrayList<String>();
        availWin = new ArrayList<String>();
        availMac = new ArrayList<String>();
        availLinux = new ArrayList<String>();

		/*
		 * Instantiate AsyncQuery object that runs query action 
		 * in the background 
		 */
		AsyncQuery task = new AsyncQuery();
		try {
			lock.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		/*
		 *  execute the background task with opp code taken in 
		 *  as a parameter
		 */
        task.execute(oppCode);

        // While the permit has not been released, stay here
		while (!lock.tryAcquire());
		
		// Release the permit, done with Semaphore
		lock.release();
		
	}
	
	@Override
	public void onPause() {
	
	    super.onPause();
	
	    //closing transition animations
        //overridePendingTransition(R.animator.activity_open_scale,R.animator.activity_close_translate);

    }
	
	/*
	 * Test the network connection
	 */
	public boolean isOnline() {
		
	    ConnectivityManager connectivity_manager =
	        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo netInfo = connectivity_manager.getActiveNetworkInfo();
	    if (netInfo != null && netInfo.isConnectedOrConnecting()) {
	        return true;
	    }
	    return false;
	    
	}
	
	/*
	 * Doing the query job in a new thread
	 */
	private class AsyncQuery extends AsyncTask<String, Void, Void> {
	    
		//ProgressDialog progress_dialog;
		
	    @Override
	    protected void onPreExecute() {
	   	 		
	    		//progress_dialog = ProgressDialog.show(RoomActivity.this, "haha", "Downloading...");
	        Log.i("PreExecute", "onPreExecute");
	        
	    }
		
		@Override
	    protected Void doInBackground(String... params) {
			
	        Log.i("Background", "doInBackground");   
	        
	        // Doing the query in the background
	        queryRoomResult(params[0]);
	        
	        // Release the permit after the query result has been all set to the String array
	        lock.release();
	        
	        return null;
	        
	    }
	
	    @Override
	    protected void onPostExecute(Void result) {
	    	
	    		//Toast.makeText(RoomActivity.this, "Refresh Successfully!", Toast.LENGTH_SHORT).show();
	    		//progress_dialog.dismiss();
	        Log.i("PostExecute", "onPostExecute"); 
	        
	    }
	    
	}
	
	/*
	 * Query result from the web service 
	 */
	public void queryRoomResult(String opp_code) {
		
		// Web Service namespace 
        String namespace = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx";
        String method_name = "Rooms";
		// Web Service endpoint, just like a normal URL, something you can type in a web browser
		String endpoint = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx";
		// Soap Action 
        String soapAction = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx/Rooms";

        // Instantiate a soap object
		SoapObject soap_out = new SoapObject(namespace, method_name); 
		
		// Add parameter
		Log.i("OppCode", opp_code);
		soap_out.addProperty("OppCode" , opp_code);

        // Add soap envelop
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        envelope.bodyOut = soap_out;
		envelope.dotNet = true;
		
		// Instantiate a Transport object and call Web Service
		HttpTransportSE transport = new HttpTransportSE(endpoint); 
		try {
			if (isOnline()) {
				Log.i("Network State", "System online");
				transport.call(soapAction, envelope);
			}	
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 	
		
		// Get the query result, get down to the hierarchy 
		SoapObject soap_in = (SoapObject) envelope.bodyIn;
		SoapObject rooms_response = (SoapObject) soap_in.getProperty(0);
		SoapObject rooms_result = (SoapObject) rooms_response.getProperty(1);
		final SoapObject document_element = (SoapObject) rooms_result.getProperty(0);

        // Filter out the building name, available Windows, available Mac and available Linux
        if (document_element != null) {

            for (int i = 0; i < document_element.getPropertyCount(); i++) {

                // Room Number
                roomNumber.add(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_ROOM_NUMBER).toString());
                // Windows
                availWin.add(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_WINDOWS).toString());
                // Macintosh
                availMac.add(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_MACINTOSH).toString());
                // Linux
                availLinux.add(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_LINUX).toString());

            }

        }

    }

}
