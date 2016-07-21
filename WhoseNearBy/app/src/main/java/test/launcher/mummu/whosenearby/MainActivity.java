package test.launcher.mummu.whosenearby;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class MainActivity extends FragmentActivity implements LocationListener, OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private GoogleMap mMap;
    private LocationRequest locationRequest;
    private static final long INTERVAL = 1000 * 10;
    private static final long FATEST_INTERVAL = 1000 * 5;
    private GoogleApiClient googleMapClient;
    private String deviceId;
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Marker unknown;
    private Location currentLocation;
    private Marker myMarker;
    HashMap<String, Marker> users = new HashMap<>();
    private Circle myCircle;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        deviceId = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ANDROID_ID);
        database = FirebaseDatabase.getInstance();
        myRef = database.getReference();

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        createLocationRequest();
        googleMapClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
        mMap.getUiSettings().setMyLocationButtonEnabled(true);
        googleMapClient.connect();

        myRef.child("users").addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getKey().equalsIgnoreCase(getDeviceId())) {

                } else {
                    createMarker(dataSnapshot);
                }
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                if (dataSnapshot.getKey().equalsIgnoreCase(getDeviceId())) {

                } else {
                    createMarker(dataSnapshot);
                }
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void createMarker(DataSnapshot dataSnapshot) {
        try {

            Marker marker = users.get(dataSnapshot.getKey());
            if (marker != null) {
                marker.remove();
            }
            JSONObject object = new JSONObject(dataSnapshot.getValue().toString());
            String latitude = object.getString("latitude");
            String longitude = object.getString("longitude");

            double lat = Double.parseDouble(latitude);
            double log = Double.parseDouble(longitude);

            LatLng latLng = new LatLng(lat, log);

            addToMap(latLng, dataSnapshot);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void addToMap(LatLng latLng, DataSnapshot dataSnapshot) {
        Location location = new Location("");
        location.setLatitude(latLng.latitude);
        location.setLongitude(latLng.longitude);
        float v = currentLocation.distanceTo(location);
        if (currentLocation != null && v <= 5) {

            unknown = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.
                    defaultMarker(BitmapDescriptorFactory.HUE_AZURE)).position(latLng).title("Unknown"));
            users.put(dataSnapshot.getKey(), unknown);
        }
    }

    private void createLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FATEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    @Override
    protected void onStart() {

        super.onStart();
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        PendingResult<Status> pendingResult = LocationServices.FusedLocationApi.requestLocationUpdates(
                googleMapClient, locationRequest, this);

    }

    @Override
    public void onLocationChanged(Location location) {
        this.currentLocation = location;

        myRef.child("users").child(getDeviceId()).child("latitude").setValue(location.getLatitude());
        myRef.child("users").child(getDeviceId()).child("longitude").setValue(location.getLongitude());

        if (myMarker != null) {
            myMarker.remove();
        }
        if (myCircle != null) {
            myCircle.remove();

        }
        myMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(),
                location.getLongitude())).icon(BitmapDescriptorFactory.fromResource(R.drawable.marker)).title("Me!"));

        myCircle = mMap.addCircle(new CircleOptions().center(new LatLng(location.getLatitude(),
                location.getLongitude())).radius(5).strokeColor(Color.GREEN));
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(location.getLatitude(),
                        location.getLongitude()), mMap.getMaxZoomLevel()));

    }

    @Override
    public boolean onMyLocationButtonClick() {
        return false;
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        startLocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Location getCurrentLocation() {
        return currentLocation;
    }

    public void setCurrentLocation(Location currentLocation) {
        this.currentLocation = currentLocation;
    }
}
