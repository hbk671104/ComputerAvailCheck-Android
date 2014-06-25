package com.bk.checkcompavail;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bk.computeravailcheck.R;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.concurrent.Semaphore;

public class RoomActivity extends Activity {
	
	final private int INDEX_ROOM_NUMBER = 0;
	final private int INDEX_WINDOWS = 4;
	final private int INDEX_MACINTOSH = 5;
	final private int INDEX_LINUX = 6;
	
	private String opp_code, number_of_rooms, building_name;
	
	private Button refresh_button;
	private Semaphore lock;
	private TextView[][] text_view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.room_activity);
		
		// Opening transition animation
		overridePendingTransition(R.animator.activity_open_translate, R.animator.activity_close_scale);
		
		/* 
		 * Create an intent that carries the data transfered 
		 * from MapActivity and retrieve the oppcode
		 */
		Intent intent = getIntent();
		opp_code = intent.getStringExtra("OppCode");
		number_of_rooms = intent.getStringExtra("NumberOfRooms");
		building_name = intent.getStringExtra("BuildingName");
		
		// Set the activity title
		setTitle(building_name);

		// Instantiate the TextView 2D array
		text_view = new TextView[Integer.parseInt(number_of_rooms)][4];
		for (int i = 0; i < text_view.length; i++) {
			for (int j = 0; j < text_view[0].length; j++) {
				String text_view_id = "";
				switch(i) {
					case 0: text_view_id += "TextView02_"; break;
					case 1: text_view_id += "TextView03_"; break;
					case 2: text_view_id += "TextView04_"; break;
					case 3: text_view_id += "TextView05_"; break;
					case 4: text_view_id += "TextView06_"; break;
					case 5: text_view_id += "TextView07_"; break;
					case 6: text_view_id += "TextView08_"; break;
					case 7: text_view_id += "TextView09_"; break;
					case 8: text_view_id += "TextView10_"; break;
					case 9: text_view_id += "TextView11_"; break;
					case 10: text_view_id += "TextView12_"; break;
					case 11: text_view_id += "TextView13_"; break;
					case 12: text_view_id += "TextView14_"; break;
					case 13: text_view_id += "TextView15_"; break;
					case 14: text_view_id += "TextView16_"; break;
					case 15: text_view_id += "TextView17_"; break;
					case 16: text_view_id += "TextView18_"; break;
					case 17: text_view_id += "TextView19_"; break;
					case 18: text_view_id += "TextView20_"; break;
					case 19: text_view_id += "TextView21_"; break;
					case 20: text_view_id += "TextView22_"; break;
					case 21: text_view_id += "TextView23_"; break;
					case 22: text_view_id += "TextView24_"; break;
				}
				
				switch(j) {
					case 0: text_view_id += "01"; break;
					case 1: text_view_id += "02"; break;
					case 2: text_view_id += "03"; break;
					case 3: text_view_id += "04"; break;
				}

				// Get resource id based on text view id
				int resource_id = getResources().getIdentifier(text_view_id, "id", getPackageName());
				text_view[i][j] = (TextView) findViewById(resource_id);

			}
		}
		
		// Instantiate the Semaphore object
		lock = new Semaphore(1);
		
		// Instantiate the button and set the onclick listener
		refresh_button = (Button) findViewById(R.id.button_refresh);
		refresh_button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				// Query data in a separate thread
				queryDataInSeparateThread();	
				
				Toast.makeText(RoomActivity.this, "Refresh Successfully!", Toast.LENGTH_SHORT).show();
			
			}
		});
		
		// Query data in a separate thread
		queryDataInSeparateThread();	
	
	}

	protected void queryDataInSeparateThread() {
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
		task.execute(opp_code);
		
		// While the permit has not been released, stay here
		while (!lock.tryAcquire());
		
		// Release the permit, done with Semaphore
		lock.release();
		
	}
	
	@Override
	public void onPause() {
	
	    super.onPause();
	
	    //closing transition animations
	    overridePendingTransition(R.animator.activity_open_scale,R.animator.activity_close_translate);
	    
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
		String namespace = "https://clc.its.psu.edu/ComputerAvailabilityWS/";
		String method_name = "Rooms";
		// Web Service endpoint, just like a normal URL, something you can type in a web browser
		String endpoint = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx";
		// Soap Action 
		String soapAction = "https://clc.its.psu.edu/ComputerAvailabilityWS/Rooms";
		
		// Instantiate a soap object
		SoapObject soap_out = new SoapObject(namespace, method_name); 
		
		// Add parameter
		Log.i("OppCode", opp_code);
		soap_out.addProperty("OppCode" , opp_code);
		
		// Add soap envelop
		SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER11);
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
	
		// Set the text in the UI thread
		runOnUiThread(new Runnable() {
			@Override
		    public void run() {

				// Filter out the building name, available Windows, available Mac and available Linux 
				if (document_element != null) {
					for (int i = 0; i < document_element.getPropertyCount(); i++) {
						// Room Number
						text_view[i][0].setText(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_ROOM_NUMBER).toString());
						// Windows 
						text_view[i][1].setText(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_WINDOWS).toString());
						// Macintosh
						text_view[i][2].setText(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_MACINTOSH).toString());
						// Linux
						text_view[i][3].setText(((SoapObject) document_element.getProperty(i)).getProperty(INDEX_LINUX).toString());
					}
				}
		    	 
		    }
		});
		
	}

}
