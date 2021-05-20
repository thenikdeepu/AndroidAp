package com.ownmyway.views.activities;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.ownmyway.App;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.model.Trip;
import com.ownmyway.model.User;
import com.ownmyway.model.UserLocation;
import com.ownmyway.R;
import com.ownmyway.views.components.MapUIAddOnsManager;
import com.ownmyway.views.components.NotificationManager;
import com.ownmyway.views.components.GetPathFromLocation;
import com.ownmyway.views.UIErrorHandler;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.Task;

import java.util.List;
import java.util.Observable;
import java.util.Observer;

import static com.ownmyway.model.Trip.STATUS.DRIVER_PICKING_UP;
import static com.ownmyway.model.Trip.STATUS.EN_ROUTE;
import static com.ownmyway.model.User.TYPE.DRIVER;
import static com.ownmyway.model.User.TYPE.RIDER;

/**
 * Main Map activity. Activity uses similiar UI for Rider and Driver, but changes functionality based
 * on the type of user currently logged in (Rider, Driver). Activity links into the Google Maps
 * APi which is used to handle the current user location. Future iterations will include using
 * The sessionTrip object to display the route on the map and enable selecting start / end locations
 */
public class MapActivity extends AppCompatActivity implements Observer, OnMapReadyCallback, UIErrorHandler {

    // GOOGLE MAP STATE
    public Location mLastKnownUserLocation;
    private static final int ERROR_DIALOG_REQUEST = 9001;
    private GoogleMap mMap;
    private boolean mLocationPermissionGranted = false;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1234;
    private static final float DEFAULT_ZOOM = 15;
    public final double GEOFENCE_DETECTION_TOLERANCE = 0.040; // 40 meters is the average house perimeter width in North America

    // LOCAL TRIP STATE
    public Trip.STATUS currentTripStatus = null;

    // OTHER UI ELEMENTS (BUTTONS, SIDE-PANEL, and STATUS)
    private MapUIAddOnsManager uiAddOnsManager;

    // NOTIFICATIONS
    private NotificationManager notifManager;

    //POLYLINE OPTIONS LIST OF LATLNG POINTS FOR CURRENT ROUTE
    private List<LatLng> routePointList;


    /**
     * onCreate method creates MapActivity when it is called
     *
     * @param savedInstanceState is a previous saved state if applicable
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        if (googleConnectionSuccessful()) {
            getLocationPermission();
        } else {
            Toast.makeText(this,
                    "map connection failed", Toast.LENGTH_SHORT).show();
        }

        // INSTANTIATE UI ADD-ONs MANAGER
        uiAddOnsManager = new MapUIAddOnsManager(this);

        // INSTANTIATE NOTIFICATION MANAGER
        notifManager = new NotificationManager(this, MapActivity.class);

        App.getModel().addObserver(this);
        Trip sessionTrip = App.getModel().getSessionTrip();
        if (sessionTrip != null) {
            currentTripStatus = sessionTrip.getStatus();
            uiAddOnsManager.showStatusButton();
            uiAddOnsManager.showActiveMainActionButton();
        }
    }

    /**
     * update method updates the activity when necessary, USED MOSTLY FOR NOTIFICATIONS
     *
     * @param o,arg are instances of the observable and object
     */
    @Override
    public void update(Observable o, Object arg) {
        Trip sessionTrip = App.getModel().getSessionTrip();
        User.TYPE currentUserType = App.getModel().getSessionUser().getType();

        // Handles State Resets regarding Trip Cancellations
        if (sessionTrip == null) {
            if (this.currentTripStatus != null) {
                switch (this.currentTripStatus) {
                    case DRIVER_ACCEPT:
                        if (currentUserType == DRIVER) {
                            notifManager.notifyOnDriverChannel(
                                    1,"Unfortunately, a rider has declined your offer.",
                                    "", Color.RED);
                        }
                        break;
                    case DRIVER_PICKING_UP:
                        if (currentUserType == DRIVER) {
                            notifManager.notifyOnDriverChannel(
                                    2, "Unfortunately, the rider no longer needs a ride from you.",
                                    "Rider no longer needs to be picked up.", Color.RED);
                        }
                        break;
                    case EN_ROUTE:
                        notifManager.notifyOnAllChannels(
                                3, "Unfortunately, a rider or driver has stopped this trip.",
                                "", Color.RED);
                        clearMapRoute();
                        break;
                }
                this.currentTripStatus = null;
            }
            uiAddOnsManager.showActiveMainActionButton();
        }
        // Handles Forward State Transitions regarding Trip Progress
        else if (sessionTrip.getStatus() != this.currentTripStatus) {
            this.currentTripStatus = sessionTrip.getStatus();
            switch (sessionTrip.getStatus()) {
                case DRIVER_ACCEPT:
                    if (currentUserType == RIDER) {
                        notifManager.notifyOnRiderChannel(
                                4,"A driver has accepted your request!",
                                "OwnMyWay requires your action!",
                                Color.GREEN);
                    }
                    break;
                case DRIVER_PICKING_UP:
                    if (currentUserType == RIDER) {
                        notifManager.notifyOnRiderChannel(
                                5, "Your ride is on the way!",
                                "",
                                Color.GREEN);
                    } else if (currentUserType == DRIVER) {
                        notifManager.notifyOnDriverChannel(
                                6, "The rider has accepted and is now ready for pickup!",
                                "Rider Username: " + sessionTrip.getRiderUserName(),
                                Color.GREEN);
                        // Edge case: check if driver is already at rider's location
                        updateOnLocationChange();
                    }
                    break;
                case DRIVER_ARRIVED:
                    if (currentUserType == RIDER) {
                        notifManager.notifyOnRiderChannel(
                                7, "Your driver has arrived!",
                                "",
                                Color.GREEN);
                    }
                    break;
                case EN_ROUTE:
                    getDeviceLocation(false,true);
                    break;
                case COMPLETED:
                    notifManager.notifyOnAllChannels(
                            8, "Trip completed. Your destination has been reached!",
                            "You have arrived at: " + sessionTrip.getEndUserLocation().getAddress(),
                            Color.GREEN);
                    clearMapRoute();
                    break;
            }
            uiAddOnsManager.showActiveMainActionButton();
        }
        // Edge states
        else {
            Log.e("%s", "Unknown state has been encountered!");
        }
    }

    /**updates user location and route if the user's location has changed*/
    public void updateOnLocationChange() {
        getDeviceLocation(false, !isTripOnRoute());
        Trip sessionTrip = App.getModel().getSessionTrip();
        if (sessionTrip != null && mLastKnownUserLocation != null) {
            User.TYPE currentUserType = App.getModel().getSessionUser().getType();
            UserLocation startUserLocation = sessionTrip.getStartUserLocation();
            UserLocation endUserLocation = sessionTrip.getEndUserLocation();

            if (currentUserType == DRIVER && currentTripStatus == DRIVER_PICKING_UP) {
                UserLocation driverLoc = new UserLocation(
                        mLastKnownUserLocation.getLatitude(),
                        mLastKnownUserLocation.getLongitude());
                if (startUserLocation.distanceTo(driverLoc) <= GEOFENCE_DETECTION_TOLERANCE) {
                    Toast.makeText(getBaseContext(), "Notifying rider you have arrived...", Toast.LENGTH_LONG).show();
                    ApplicationController.handleNotifyRiderForPickup();
                }
            }

            if (currentUserType == RIDER && currentTripStatus == EN_ROUTE) {
                UserLocation riderLoc = new UserLocation(
                        mLastKnownUserLocation.getLatitude(),
                        mLastKnownUserLocation.getLongitude());
                Log.d("", "" + riderLoc.distanceTo(endUserLocation));
                if (riderLoc.distanceTo(endUserLocation) <= GEOFENCE_DETECTION_TOLERANCE) {
                    ApplicationController.completeTrip();
                }
            }

        }
    }

    /**
     * onMapReady lets the user know that the location is being gotten if permissions are granted
     *
     * @param googleMap is the map instance of GoogleMap
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        if (mLocationPermissionGranted) {
            getDeviceLocation(true, true);
            mMap.setMyLocationEnabled(true);
        }

        mMap.setOnMapClickListener(latLng -> {
            if (uiAddOnsManager.isShowSideBar()) {
                uiAddOnsManager.hideSettingsPanel();
                uiAddOnsManager.showActiveMainActionButton();
            }
        });

        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateOnLocationChange();
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        try {
            LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            long MIN_MAP_UPDATE_INTERVAL = 1000; // update map on a 1 second delta
            long MIN_MAP_UPDATE_DISTANCE = 5; // update map on a 5 meters delta (battery life conservation)
            assert locationManager != null;
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    MIN_MAP_UPDATE_INTERVAL,
                    MIN_MAP_UPDATE_DISTANCE,
                    locationListener);
        } catch (SecurityException sece) {
            sece.printStackTrace();
        }
    }

    /***** BUILDING CUSTOM GOOGLE MAP (CODE REFERENCED FROM GOOGLE API DOCS) ******/
    public void initializeMap() {
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(MapActivity.this);  //calls onMapReady
    }

    /** getLocationPermission gets location permission from the user **/
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         * Code reference from Google API docs
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            initializeMap();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /** Gets the location of the users device **/
    private void getDeviceLocation(boolean updateFirebase, boolean attemptRouteRedraw) {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        FusedLocationProviderClient mFusedLocationProviderClient;
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionGranted) {
                Task locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set the map's camera position to the current location of the device.
                        mLastKnownUserLocation = (Location) task.getResult();

                        if (updateFirebase) {
                            App.getController().updateUserLocation(new UserLocation(
                                                    mLastKnownUserLocation.getAltitude(),
                                                    mLastKnownUserLocation.getLongitude()));
                        }

                        Trip sessionTrip = App.getModel().getSessionTrip();
                        if (attemptRouteRedraw &&
                            sessionTrip != null && sessionTrip.getStatus() == EN_ROUTE) {
                            //assigns start location depending on whether trip is EN ROUTE or onRoute
                            LatLng startLoc;
                            LatLng endLoc = sessionTrip.getEndUserLocation().generateLatLng();
                            if(isTripOnRoute()){
                                startLoc = sessionTrip.getStartUserLocation().generateLatLng();
                            }else {
                                startLoc = new LatLng(mLastKnownUserLocation.getLatitude(),
                                        mLastKnownUserLocation.getLongitude());
                            }
                            clearMapRoute();
                            drawRouteMap(startLoc, endLoc);
                        } else {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(
                                            mLastKnownUserLocation.getLatitude(),
                                            mLastKnownUserLocation.getLongitude()),
                                    DEFAULT_ZOOM));
                        }

                    } else {
                        Log.d("NULLLOCATION", "Current location is null. Using defaults.");
                        Log.e("LOCATIONEXCEPTION", "Exception: %s", task.getException());
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                new LatLng(mLastKnownUserLocation.getLatitude(),
                                        mLastKnownUserLocation.getLongitude()), DEFAULT_ZOOM));
                        mMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                });
            }
        } catch (SecurityException e) {
            e.printStackTrace();
        }
    }

    /** Returns true if connection to google api is successful **/
    boolean googleConnectionSuccessful() {
        int connection = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MapActivity.this);
        if (connection == ConnectionResult.SUCCESS) {
            Log.d("MAPCONNECTIONSUCCESS", "Map connection Successful");
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(connection)) {
            //for debugging. if an error occurs, print the error
            Log.e("MAPERROR", "an error occurred loading map");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MapActivity.this, connection, ERROR_DIALOG_REQUEST);
            dialog.show();
        } else {
            Log.e("MAPCONNECTIONFAILURE", "Map failed to connect");
        }
        return false;
    }

    /**
     * onRequestPermissionsResult gives the result of of the user permissions request
     *
     * @param requestCode  is the result of the request
     * @param permissions  is a list of the permissions
     * @param grantResults is a list of results granted
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //this code is copied from google map api documentation
        mLocationPermissionGranted = false;
        if (requestCode == PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {// If request is cancelled, the result arrays are empty.
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionGranted = true;
                initializeMap();
                Log.d("LOCATION PERMISSION", "UserLocation permission granted");
            } else {
                Log.d("LOCATION PERMISSION", "UserLocation permission denied");
            }
        }
    }

    /** onDestroy method destructs activity if it is closed down **/
    @Override
    public void onDestroy() {
        super.onDestroy();
        // THIS CODE SHOULD BE IMPLEMENTED IN EVERY VIEW
        ApplicationModel m = App.getModel();
        m.deleteObserver(this);
    }

    /** onError handles UI Errors if there are any **/
    @Override
    public void onError(Error e) {}

    /**Draws start/end route polylines on map when requested - also adds markers to start/end location*/
    public void drawRouteMap(LatLng startLoc, LatLng endLoc){
        //Gets a LatLng of the StartUserLocation
        Trip sessionTrip = App.getModel().getSessionTrip();
        if (sessionTrip == null) {
            return;
        }

        new GetPathFromLocation(
                startLoc,
                endLoc,
                App.getAPIKey(this, true),
                polyLine -> {
                    routePointList = polyLine.getPoints();
                    mMap.addPolyline(polyLine);
                }).execute();

        //Adds start and end point markers to map
        mMap.addMarker(new MarkerOptions().position(startLoc).title("Start Location"));
        mMap.addMarker(new MarkerOptions().position(endLoc).title("End Location"));
        //Changes camera to endlocation
        LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
        boundsBuilder.include(startLoc);
        boundsBuilder.include(endLoc);
        int routePadding = 120;
        LatLngBounds latLngBounds = boundsBuilder.build();
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds,routePadding));
}

    /**Clears map of polylines and markers - also resets camera to userlocation*/
    public void clearMapRoute(){
        mMap.clear();
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                new LatLng(mLastKnownUserLocation.getLatitude(), mLastKnownUserLocation.getLongitude()),
                DEFAULT_ZOOM));
    }

    /**Returns true or false depending on whether user is following polyline or not*/
    public boolean isTripOnRoute(){
        Trip sessionTrip = App.getModel().getSessionTrip();
        if (sessionTrip == null || routePointList == null || sessionTrip.getStatus() == EN_ROUTE) {
            return false;
        }

        //gets user's current location
        UserLocation currentUserLocation = new UserLocation(mLastKnownUserLocation.getLatitude(),
                mLastKnownUserLocation.getLongitude());

        //iterates through routePointList and checks if users location aligns with it
        for(int i = 0; i<routePointList.size(); i++){
            UserLocation ith_route = new UserLocation(routePointList.get(i).latitude,
                    routePointList.get(i).longitude);
            //Checking if current location is on polyline and/or within 40m geofence tolerance
            if (currentUserLocation.distanceTo(ith_route) <= GEOFENCE_DETECTION_TOLERANCE){
                return true;
            }
        }

        return false;
    }
}
