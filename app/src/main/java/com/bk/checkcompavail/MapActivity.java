package com.bk.checkcompavail;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.bk.computeravailcheck.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnInfoWindowClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.ksoap2.SoapEnvelope;
import org.ksoap2.serialization.SoapObject;
import org.ksoap2.serialization.SoapSerializationEnvelope;
import org.ksoap2.transport.HttpTransportSE;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Semaphore;

public class MapActivity extends Activity {

    final private int INDEX_WINDOWS = 5;
    final private int INDEX_MACINTOSH = 6;
    final private int INDEX_LINUX = 7;

    // Don't forget to change this!!! When god damn building changes!!!
    final private int NUMBER_OF_BUILDINGS = 41;

    final private int NUMBER_OF_USEFUL_ATTRIBUTES = 3;

    // opp code
    final private ArrayList<String> OPP_CODE = new ArrayList<String>();

    // Name of each building
    final private ArrayList<String> NAME_OF_EACH_BUILDING = new ArrayList<String>();

    // total number of rooms in each building
    final private ArrayList<String> BUILDING_TOTAL_ROOMS = new ArrayList<String>();

    // The total amount of computer of each building
    final private ArrayList<String> BUILDING_TOTAL_COMPUTERS = new ArrayList<String>();

    // The geo-location data of the center of the building
    final private LatLng CENTER = new LatLng(40.800509, -77.864252);

    // The geo-location data of all the buildings (latitude and longitude)
    final private LatLng[] BUILDINGS_LOC = new LatLng[]{
            new LatLng(40.803636, -77.863764),
            new LatLng(40.800507, -77.857044),
            new LatLng(40.799136, -77.861684),
            new LatLng(40.801270, -77.854822),
            new LatLng(40.809093, -77.855406),
            new LatLng(40.803926, -77.865199),
            new LatLng(40.799135, -77.86838),
            new LatLng(40.798325, -77.867731),
            new LatLng(40.798116, -77.862798),
            new LatLng(40.794266, -77.865405),
            new LatLng(40.804889, -77.856182),
            new LatLng(40.792146, -77.87088),
            new LatLng(40.801011, -77.863638),
            new LatLng(40.806479, -77.862265),
            new LatLng(40.799691, -77.869528),
            new LatLng(40.80483, -77.863994),
            new LatLng(40.793742, -77.862985),
            new LatLng(40.796982, -77.861375),
            new LatLng(40.796544, -77.859884),
            new LatLng(40.794306, -77.863671),
            new LatLng(40.794668, -77.865838),
            new LatLng(40.798136, -77.861272),
            new LatLng(40.793673, -77.868112),
            new LatLng(40.807461, -77.866494),
            new LatLng(40.798144, -77.870666),
            new LatLng(40.800934, -77.861492),
            new LatLng(40.798623, -77.870312),
            new LatLng(40.798607, -77.862223),
            new LatLng(40.798493, -77.865452),
            new LatLng(40.800239, -77.864937),
            new LatLng(40.831589, -77.844789),
            new LatLng(40.801129, -77.85851),
            new LatLng(40.798233, -77.868628),
            new LatLng(40.795435, -77.868651),
            new LatLng(40.799532, -77.855962),
            new LatLng(40.794757, -77.862641),
            new LatLng(40.796962, -77.865757),
            new LatLng(40.801108, -77.866744),
            new LatLng(40.79323, -77.866857),
            new LatLng(40.795715, -77.867405),
            new LatLng(40.80294, -77.866079),
            new LatLng(40.797546, -77.866610),
            new LatLng(40.795967, -77.864255)};

    private String[][] query_result;
    private Semaphore lock;

    // Map Object
    private GoogleMap map;
    private CameraPosition initial_position;
    private ArrayList<Marker> marker_array = new ArrayList<Marker>();

    // Drawer Object
    private DrawerLayout drawer_layout;
    private ListView drawer_list;
    private ActionBarDrawerToggle drawer_toggle;

    // Pools
    protected ArrayList<MarkerOptions> markerOptionPool = new ArrayList<MarkerOptions>();
    protected ArrayList<MarkerOptions> markerOptionArray = new ArrayList<MarkerOptions>();
    protected ArrayList<String> buildingNamePool = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);
        setTitle("Map View");

        // Instantiate the semaphore
        lock = new Semaphore(1);

        // Add Google Map on the MainActivity
        addGoogleMap();

        // Query data in a separate thread
        queryDataInSeparateThread();

        // Add drawer
        addDrawer();

        // Init Pools
        initBuildingNamePool();
        initMarkerPool();

        // Add navigation bar
        addNavigationBar();

    }

    private void queryDataInSeparateThread() {

        // Create a new thread that runs the query in the background
        AsyncQuery task = new AsyncQuery();
        try {
            // Acquire a permit
            lock.acquire();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }

        // Start the new thread
        task.execute();

        // Add all the markers of buildings after the permit is released
        while (!lock.tryAcquire()) ;

    }

    /*
     * Add all the map elements, called in OnCreate()
     */
    public void addGoogleMap() {

        MapsInitializer.initialize(this);

        // Instantiate map and initial_position
        map = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();
        //map = ((SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        initial_position = CameraPosition.builder()
                .target(CENTER)
                .zoom(15)
                .bearing(0)
                .build();

        // Set the center of the map when the MapAcitivity is executed at the beginning
        map.moveCamera(CameraUpdateFactory.newCameraPosition(initial_position));

        // Enable user's current location
        map.setMyLocationEnabled(true);

        // Set info window click listener. Kinda self explanatory
        map.setOnInfoWindowClickListener(new OnInfoWindowClickListener() {

            @Override
            public void onInfoWindowClick(Marker marker) {

                // get the marker index of the marker
                int marker_index = Integer.parseInt(marker.getId().substring(1));

                // Instantiate an Intent object that helps us jump to RoomActivity
                Intent intent = new Intent();
                intent.setClass(MapActivity.this, RoomActivity.class);

				/*
				 *  Instantiate an Bundle object that passes the Opp Code to
				 *  RoomActivity as the parameter for parsing room detail data
				 */
                Bundle bundle = new Bundle();
                bundle.putString("OppCode", OPP_CODE.get(marker_index));
                bundle.putString("BuildingName", NAME_OF_EACH_BUILDING.get(marker_index));

                // Put the bundle into the intent
                intent.putExtras(bundle);

                // Start the activity
                startActivity(intent);

            }
        });

    }

    /*
     * Add drawer layout and drawer list
     */
    public void addDrawer() {

        // Instantiate drawer layout and drawer list objects
        drawer_layout = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer_list = (ListView) findViewById(R.id.left_drawer);

        // add adapter to the drawer list
        drawer_list.setAdapter(new ArrayAdapter<String>(this, R.layout.drawer_list_item, NAME_OF_EACH_BUILDING));

        // Add drawer on item click listener
        drawer_list.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // TODO Auto-generated method stub
                drawer_list.setItemChecked(position, true);

                // close the drawer while users click on one of the drawer items
                drawer_layout.closeDrawer(drawer_list);

                // Animate the camera position to the corresponding marker
                map.animateCamera(CameraUpdateFactory.newLatLng(marker_array.get(position).getPosition()));

                // Show the info window
                marker_array.get(position).showInfoWindow();

            }

        });

        // enable ActionBar app icon to behave as action to toggle nav drawer
        getActionBar().setDisplayHomeAsUpEnabled(true);
        getActionBar().setHomeButtonEnabled(true);

        // ActionBarDrawerToggle ties together the the proper interactions
        // between the sliding drawer and the action bar app icon
        drawer_toggle = new ActionBarDrawerToggle(
                this,
                drawer_layout,
                R.drawable.ic_navigation_drawer,
                R.string.drawer_open,
                R.string.drawer_close) {

            public void onDrawerClosed(View view) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
            }

            public void onDrawerOpened(View drawerView) {
                invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()             
            }

        };

        // link the drawer toggle with drawer layout
        drawer_layout.setDrawerListener(drawer_toggle);

    }

    /*
     * Add navigation bar
     */
    public void addNavigationBar() {

        ActionBar action_bar = getActionBar();
        action_bar.setDisplayShowTitleEnabled(false);
        action_bar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        action_bar.show();

        SpinnerAdapter spinner_adapter = ArrayAdapter.createFromResource(this,
                R.array.action_bar_item,
                android.R.layout.simple_spinner_dropdown_item);

        // Set list navigation callbacks
        action_bar.setListNavigationCallbacks(spinner_adapter, new ActionBar.OnNavigationListener() {

            @Override
            public boolean onNavigationItemSelected(int itemPosition, long itemId) {

                switch (itemPosition) {
                    case 0:
                        map.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                        return true;

                    case 1:
                        map.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                        return true;

                    case 2:
                        map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                        return true;

                    default:
                        return false;

                }

            }
        });

    }

    // Called whenever we call invalidateOptionsMenu()
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        return super.onPrepareOptionsMenu(menu);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Toggle will open the drawer
        if (drawer_toggle.onOptionsItemSelected(item)) {
            return true;
        }

        int itemId = item.getItemId();

        if (itemId == R.id.about) {

            String html = "Thank you for your support! If you have any questions " +
                    "regarding to this app, please contact me through my " +
                    "<a href='mailto:hbk671104@gmail.com?subject=CheckCompAvail Feekback'>Email</a>" +
                    " or simply post on my " +
                    "<a href='https://www.facebook.com/biggiekake'>Facebook</a>" + " wall!" +
                    "<br/><br/>" +
                    "Special thanks to: <br/>" +
                    "1. <a href='mailto:derekmorr@psu.edu'>Derek Morr</a> " +
                    "for providing the computer availablity data <br/>" +
                    "2. <a href='mailto:tlang@usc.edu'>Tianyu Lang</a>" +
                    " for critical technical support <br/>" +
                    "3. <a href='mailto:hyqqrdfzfx@gmail.com'>Chloe Yangqingqing Hu</a>" +
                    " for logo optimization";

            TextView view = new TextView(this);
            view.setText(Html.fromHtml(html));
            view.setMovementMethod(LinkMovementMethod.getInstance());

            // Create a customized alertDialog
            new AlertDialog.Builder(this)
                    .setIcon(R.drawable.psu_paw)
                    .setTitle("About")
                    .setView(view)
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;

        } else {

            return super.onOptionsItemSelected(item);

        }

    }

    /*
     * Doing the data query job in a new thread
     */
    private class AsyncQuery extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {

            Log.i("PreExecute", "onPreExecute");

        }

        @Override
        protected Void doInBackground(Void... params) {

            Log.i("Background", "doInBackground");

            // Doing the query in the background
            queryBuildingResult();

            // Release the permit after the query result has been all set to the String array
            lock.release();

            return null;

        }

        @Override
        protected void onPostExecute(Void result) {

            Log.i("PostExecute", "onPostExecute");
            initMarker();
            addMarker();

        }

    }

    /*
     * Query building result from the web service
     */
    public void queryBuildingResult() {

        // Web Service namespace
        String namespace = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx";
        String method_name = "Buildings";

        // Web Service endpoint, just like a normal URL, something you can type in a web browser
        String endpoint = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx";

        // Soap Action
        String soapAction = "https://clc.its.psu.edu/ComputerAvailabilityWS/Service.asmx/Buildings";

        // Instantiate a soap object
        SoapObject soap_out = new SoapObject(namespace, method_name);

        // Add parameter
        soap_out.addProperty("Campus", "UP");

        // Add soap envelop
        SoapSerializationEnvelope envelope = new SoapSerializationEnvelope(SoapEnvelope.VER12);
        envelope.bodyOut = soap_out;
        envelope.dotNet = true;

        // Instantiate a Transport object and call Web Service
        HttpTransportSE transport = new HttpTransportSE(endpoint);
        try {
            // make soap call
            transport.call(soapAction, envelope);

        } catch (IOException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();

        } catch (XmlPullParserException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();

        }

        // Get the query result
        SoapObject soap_in = (SoapObject) envelope.bodyIn;
        SoapObject buildings_response = (SoapObject) soap_in.getProperty(0);
        SoapObject buildings_result = (SoapObject) buildings_response.getProperty(1);
        SoapObject document_element = (SoapObject) buildings_result.getProperty(0);

        // Filter out the building name, available Windows, available Mac and available Linux
        if (document_element != null) {

            query_result = new String[document_element.getPropertyCount()][NUMBER_OF_USEFUL_ATTRIBUTES];

            for (int i = 0; i < document_element.getPropertyCount(); i++) {

                // Opp code
                OPP_CODE.add(((SoapObject) document_element.getProperty(i)).getProperty(0).toString());

                // Building Name
                NAME_OF_EACH_BUILDING.add(((SoapObject) document_element.getProperty(i)).getProperty(1).toString());

                // Total number of rooms in each building
                BUILDING_TOTAL_ROOMS.add(((SoapObject) document_element.getProperty(i)).getProperty(2).toString());

                // Total number of computers in each building
                BUILDING_TOTAL_COMPUTERS.add(((SoapObject) document_element.getProperty(i)).getProperty(3).toString());

                // Windows
                query_result[i][0] = ((SoapObject) document_element.getProperty(i)).getProperty(INDEX_WINDOWS).toString();

                // Macintosh
                query_result[i][1] = ((SoapObject) document_element.getProperty(i)).getProperty(INDEX_MACINTOSH).toString();

                // Linux
                query_result[i][2] = ((SoapObject) document_element.getProperty(i)).getProperty(INDEX_LINUX).toString();

            }

        }

    }

    /*
     * Init Marker Pool
     */
    public void initMarkerPool() {

        for (int i = 0; i < BUILDINGS_LOC.length; i++) {

            MarkerOptions tempOption = new MarkerOptions();
            tempOption.position(BUILDINGS_LOC[i]);
            markerOptionPool.add(tempOption);

        }

    }

    /*
     * Init Building Name Pool
     */
    public void initBuildingNamePool() {

        buildingNamePool.add("AgSci");
        buildingNamePool.add("Beaver");
        buildingNamePool.add("Boucke");
        buildingNamePool.add("Brill Hall");
        buildingNamePool.add("Bryce Jordan Center");
        buildingNamePool.add("Business Bldg");
        buildingNamePool.add("Cedar");
        buildingNamePool.add("Chambers");
        buildingNamePool.add("Davey Lab");
        buildingNamePool.add("Deike");
        buildingNamePool.add("EAL");
        buildingNamePool.add("EES");
        buildingNamePool.add("Ferguson");
        buildingNamePool.add("Findlay");
        buildingNamePool.add("Ford Building");
        buildingNamePool.add("Forest Resources");
        buildingNamePool.add("Hammond");
        buildingNamePool.add("Henderson");
        buildingNamePool.add("HHDev");
        buildingNamePool.add("Hintz");
        buildingNamePool.add("Hosler");
        buildingNamePool.add("HUB");
        buildingNamePool.add("IST");
        buildingNamePool.add("Katz");
        buildingNamePool.add("Keller");
        buildingNamePool.add("LifeSci");
        buildingNamePool.add("Mateer");
        buildingNamePool.add("Osmond");
        buildingNamePool.add("Paterno");
        buildingNamePool.add("Patterson");
        buildingNamePool.add("Penn Stater Hotel");
        buildingNamePool.add("Pollock");
        buildingNamePool.add("Rackley");
        buildingNamePool.add("RecHall");
        buildingNamePool.add("Redifer");
        buildingNamePool.add("Sackett");
        buildingNamePool.add("Sparks");
        buildingNamePool.add("Stuckeman");
        buildingNamePool.add("Walker");
        buildingNamePool.add("Waring");
        buildingNamePool.add("Warnock");
        buildingNamePool.add("West Pattee");
        buildingNamePool.add("Willard");

    }

    public void initMarker() {

        for (int i = 0; i < NAME_OF_EACH_BUILDING.size(); i++) {

            String name = NAME_OF_EACH_BUILDING.get(i);

            for (int j = 0; j < buildingNamePool.size(); j++) {

                if (name.equals(buildingNamePool.get(j))) {

                    // If matches, add marker to the array
                    markerOptionArray.add(markerOptionPool.get(j));
                    break;

                }

            }

        }

    }

    /*
     * add the markers of all the buildings
     */
    public void addMarker() {

        for (int i = 0; i < markerOptionArray.size(); i++) {

            MarkerOptions option = markerOptionArray.get(i);
            option.title(NAME_OF_EACH_BUILDING.get(i));
            option.snippet("Win:" + query_result[i][0]
                    + " Mac:" + query_result[i][1]
                    + " Linux:" + query_result[i][2]);

            marker_array.add(map.addMarker(option));

        }

        // Set marker color
        setMarkerColor();

        // release the permit
        lock.release();

    }

    /*
     * If a building is less crowded, set color the marker to GREEN
     * If a building is mildly crowded, set color the marker to YELLOW
     * If a building is very crowded, set color the marker to RED
     */
    public void setMarkerColor() {

        for (int i = 0; i < marker_array.size(); i++) {
            int result_value = isCrowded(Integer.parseInt(BUILDING_TOTAL_COMPUTERS.get(i)), query_result[i][0], query_result[i][1], query_result[i][2]);
            if (result_value == 0)
                marker_array.get(i).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.computers_green));
            else if (result_value == 1)
                marker_array.get(i).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.computers_yellow));
            else
                marker_array.get(i).setIcon(BitmapDescriptorFactory.fromResource(R.drawable.computers_red));
        }

    }

    /*
     * Check if a building is crowded with people who use computers
     */
    public int isCrowded(int total, String win, String mac, String lin) {

        int windows = Integer.parseInt(win);
        int macintosh = Integer.parseInt(mac);
        int linux = Integer.parseInt(lin);
        int real_time_total = windows + macintosh + linux;

        if (real_time_total <= total / 3)
            return 2;
        else if ((real_time_total <= total * 2 / 3) && (real_time_total > total / 3))
            return 1;
        else
            return 0;

    }

}
