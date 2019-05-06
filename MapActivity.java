package com.example.mguide;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;
        if (lpg && isMapsEnabled()) {
            getUserDetails();
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                return;
            }
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            init();


    }
    }
    private static final String TAG="MapActivity";
    private static final String FINE_LOCATION=Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION=Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int Location_Permission_Request_code=1234;
    private static final int PERMISSIONS_REQUEST_ENABLE_GPS = 9003;
    private static final float Default_zoom=15f;
    private FirebaseFirestore mDb;
    private UserLocation mUserLocation;
    private DocumentReference myRef;
    //widgets
    private EditText searchText;
    private ImageView gps;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private boolean lpg=false;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        searchText=(EditText) findViewById(R.id.input_search);
        mDb=FirebaseFirestore.getInstance();
        myRef=mDb.document("Location_Description/google_hqt");
        getLocationPermission();
        gps=(ImageView) findViewById(R.id.gps);


        }
    private void init(){
        searchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_SEARCH || actionId==EditorInfo.IME_ACTION_DONE || event.getAction()==KeyEvent.ACTION_DOWN || event.getAction()==KeyEvent.KEYCODE_ENTER)
                {
                    //execute method for searching
                    geoLocate();
                }
                return false;
            }
        });
        gps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserDetails();
            }
        });
        HideSoftKeyboard();
    }
    private void geoLocate()
    {
        String searchString=searchText.getText().toString();
        Geocoder geocoder=new Geocoder(MapActivity.this);
        List<Address> list=new ArrayList<>();
        try {
            list=geocoder.getFromLocationName(searchString, 1);
        }catch(IOException e)
        {
            Log.e(TAG,"geoLocate: IOException" + e.getMessage());
        }
        if(list.size()>0){
            Address address=list.get(0);
           // Toast.makeText(getApplicationContext(), address.toString(), Toast.LENGTH_SHORT).show();
            moveCamera(new LatLng(address.getLatitude(), address.getLongitude()), Default_zoom, address.getAddressLine(0));
        }
    }
    private void getUserDetails(){
        if(mUserLocation == null){
            mUserLocation = new UserLocation();
            DocumentReference userRef = mDb.collection(getString(R.string.collection_users))
                    .document(FirebaseAuth.getInstance().getUid());

            userRef.get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if(task.isSuccessful()){
                        Users user = task.getResult().toObject(Users.class);
                        mUserLocation.setUser(user);
                        ((UserClient)getApplicationContext()).setUser(user);
                        getdevicelocation();
                    }
                }
            });
        }
        else{
            getdevicelocation();
        }
    }
    private void saveUserLocation()
    {
        if(mUserLocation!= null)
        {
            DocumentReference locationRef=mDb.
                    collection(getString(R.string.collection_user_location))
                    .document(FirebaseAuth.getInstance().getUid());
            locationRef.set(mUserLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful())
                    {
                        Log.d(TAG, "saveUserLocation: inserted user location in database."+
                                "\n Latitude: " +mUserLocation.getGeo_point().getLatitude()+
                                "\n Longitude: "+mUserLocation.getGeo_point().getLongitude());
                    }
                }
            });
        }
    }
    private void getdevicelocation()
    {
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this);
        try
        {
            if(lpg && isMapsEnabled())
            {
                Task location=fusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if(task.isSuccessful())
                        {

                            Location currentLocation=(Location) task.getResult();
                            final GeoPoint geoPoint=new GeoPoint(currentLocation.getLatitude(), currentLocation.getLongitude());
                            moveCamera(new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude()), Default_zoom, "My Location");
                            mUserLocation.setGeo_point(geoPoint);
                            mUserLocation.setTimestamp(null);
                            saveUserLocation();
                            startLocationService();
                        }
                        else
                        {
                            Toast.makeText(getApplicationContext(),"Unable to fetch current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
        catch(SecurityException e)
        {
            Log.e(TAG,"getDeviceLocation: Security Exception" + e.getMessage());
        }
    }


    private void startLocationService(){
        if(!isLocationServiceRunning()){
            Intent serviceIntent = new Intent(this, LocationService.class);
//        this.startService(serviceIntent);

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O){

                MapActivity.this.startForegroundService(serviceIntent);
            }else{
                startService(serviceIntent);
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)){
            if("com.example.mguide.services.LocationService".equals(service.service.getClassName())) {
                Log.d(TAG, "isLocationServiceRunning: location service is already running.");
                return true;
            }
        }
        return false;
    }
    private void moveCamera(LatLng latLng, float zoom, String title)
    {
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));

        if(!title.equals("My Location")) {
            MarkerOptions options = new MarkerOptions()
                    .position(latLng)
                    .title(title);
            mMap.addMarker(options);
        }
        HideSoftKeyboard();
    }
    private void initMap(){
        SupportMapFragment mapFragment=(SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(MapActivity.this);
    }
    private void getLocationPermission()
    {
        String[] permissions={Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};

        if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            if(ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
                lpg=true;
                if(isMapsEnabled()) {
                    initMap();
                }
                else
                {
                    ActivityCompat.requestPermissions(this,permissions,PERMISSIONS_REQUEST_ENABLE_GPS);
                }
            }
            else
            {
                ActivityCompat.requestPermissions(this,permissions,Location_Permission_Request_code);
            }
        }
        else
        {
            ActivityCompat.requestPermissions(this,permissions,Location_Permission_Request_code);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        lpg=false;
        switch(requestCode)
        {
            case Location_Permission_Request_code:
            case PERMISSIONS_REQUEST_ENABLE_GPS:{
                if(grantResults.length>0 && isMapsEnabled()){
                    for(int i=0;i<grantResults.length;i++)
                    {
                        if(grantResults[i]!=PackageManager.PERMISSION_GRANTED)
                        {
                            lpg=false;
                            return;
                        }
                    }
                    lpg=true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }
    private void HideSoftKeyboard()
    {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent enableGpsIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS);
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public boolean isMapsEnabled(){
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
            return false;
        }
        return true;
    }
}

