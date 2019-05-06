package com.example.mguide;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorManager;
import android.location.Location;

import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.example.mguide.UserClient;
import com.example.mguide.Users;
import com.example.mguide.UserLocation;
import com.example.mguide.R;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.List;
import java.util.Locale;

import static java.lang.Thread.sleep;

public class LocationService extends Service {

    private static final String TAG = "LocationService";

    private FusedLocationProviderClient mFusedLocationClient;
    private final static long UPDATE_INTERVAL =3 * 1000;  /* 3 secs */
    private final static long FASTEST_INTERVAL = 2*1000; /* 2 sec */
    private TextToSpeech mTTS;
    private FirebaseFirestore mDb;
    private DocumentReference myRef;
    private CollectionReference cRef;
    int flag=1;
    int i=0,j, count;
    double dist;
    float[] mGravity;
    float[] mGeomagnetic;
    float azimut;
    double rotation;
    float dir;




    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mDb=FirebaseFirestore.getInstance();

        cRef=mDb.collection("Location_Description");

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "My Channel",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("")
                    .setContentText("").build();

            startForeground(1, notification);
        }
        mTTS=new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status==TextToSpeech.SUCCESS)
                {
                    int result= mTTS.setLanguage(Locale.ENGLISH);
                    if (result == TextToSpeech.LANG_MISSING_DATA
                            || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "Language not supported");
                    }
                } else {
                    Log.e("TTS", "Initialization failed");
                }
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        getLocation();
        return START_NOT_STICKY;
    }

    private void getLocation() {


        LocationRequest mLocationRequestHighAccuracy = new LocationRequest();
        mLocationRequestHighAccuracy.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequestHighAccuracy.setInterval(UPDATE_INTERVAL);
        mLocationRequestHighAccuracy.setFastestInterval(FASTEST_INTERVAL);

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            stopSelf();
            return;
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {

                        final Location location = locationResult.getLastLocation();

                        if (location != null) {
                            Users user = ((UserClient)(getApplicationContext())).getUser();
                            GeoPoint geoPoint = new GeoPoint(location.getLatitude(), location.getLongitude());
                            UserLocation userLocation = new UserLocation(user, geoPoint, null);
                            saveUserLocation(userLocation);
                            cRef
                                    .get()
                                    .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                            if (task.isSuccessful()) {
                                                count=task.getResult().size();
                                                //for (QueryDocumentSnapshot document : task.getResult()) {
                                                for(int i=0;i<count;i++){
                                                   // for (DocumentSnapshot doc:task.getResult().getDocuments().get(i)) {
                                                       DocumentSnapshot doc= task.getResult().getDocuments().get(i);
                                                        myRef = mDb.document("Location_Description/" + doc.getId());
                                                    final int finalI = i;
                                                    myRef.get()
                                                                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                                    @Override
                                                                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                        if (documentSnapshot.exists()) {
                                                                            GeoPoint loc = documentSnapshot.getGeoPoint("Location");
                                                                            dist = distance(loc.getLatitude(), location.getLatitude(), loc.getLongitude(), location.getLongitude());
                                                                            if (dist <= 2.0 && dist > -2.0) {
                                                                                    j= finalI;
                                                                                if (flag == 1) {
                                                                                     //speak();
                                                                                    String description = documentSnapshot.getString("Description");
                                                                                    float pitch= 1;
                                                                                    if(pitch < 0.1) pitch = 0.1f;
                                                                                    float speed = 1;
                                                                                    if (speed < 0.1) speed = 0.1f;
                                                                                    mTTS.setPitch(pitch);
                                                                                    mTTS.setSpeechRate(speed);
                                                                                    Toast.makeText(getApplicationContext(),description,Toast.LENGTH_LONG).show();
                                                                                    mTTS.speak(description,TextToSpeech.QUEUE_FLUSH,null,null);
                                                                                    flag++;
                                                                                }
                                                                            }
                                                                        } else {
                                                                            Toast.makeText(getApplicationContext(), "Document does not exist", Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });

                                                }
                                                DocumentSnapshot doc= task.getResult().getDocuments().get(j);
                                                myRef = mDb.document("Location_Description/" + doc.getId());
                                                myRef.get()
                                                        .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                                            @Override
                                                            public void onSuccess(DocumentSnapshot documentSnapshot) {
                                                                if(documentSnapshot.exists())
                                                                {
                                                                    GeoPoint loc = documentSnapshot.getGeoPoint("Location");
                                                                    dist = distance(loc.getLatitude(), location.getLatitude(), loc.getLongitude(), location.getLongitude());
                                                                    if(dist<=2.0 && dist>-2.0)
                                                                    {
                                                                        flag=2;
                                                                    }
                                                                    else {
                                                                        if (flag > 1)
                                                                            flag=1;
                                                                    }
                                                                }
                                                            }
                                                        });

                                            } else {
                                                Log.w(TAG, "Error getting documents.", task.getException());
                                            }
                                        }
                                    });

                        }
                    }
                },
                Looper.myLooper()); // Looper.myLooper tells this to repeat forever until thread is destroyed
    }

    private void saveUserLocation(final UserLocation userLocation){

        try{
            DocumentReference locationRef = FirebaseFirestore.getInstance()
                    .collection(getString(R.string.collection_user_location))
                    .document(FirebaseAuth.getInstance().getUid());

            locationRef.set(userLocation).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if(task.isSuccessful()){
                        Log.d(TAG, "onComplete: \ninserted user location into database." +
                                "\n latitude: " + userLocation.getGeo_point().getLatitude() +
                                "\n longitude: " + userLocation.getGeo_point().getLongitude());
                    }
                }
            });
        }catch (NullPointerException e){
            stopSelf();
        }

    }
    @Override
    public void onDestroy() {
        if (mTTS != null) {
            mTTS.stop();
            mTTS.shutdown();
        }

        super.onDestroy();
    }

    public double distance(double lat1, double lat2, double lon1,
                                  double lon2) {

        final int R = 6371; // Radius of the earth

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        return Math.sqrt(distance);
    }
    /*public double onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {
            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                azimut = orientation[0];
                rotation = -azimut * 360 / (2 * 3.14159f);
            }
        }
        return rotation;
    }*/
}
