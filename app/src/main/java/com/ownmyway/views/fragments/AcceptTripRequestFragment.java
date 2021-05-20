package com.ownmyway.views.fragments;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.ownmyway.R;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.views.activities.ContactViewerActivity;
import com.ownmyway.views.activities.TripSearchActivity;
import com.ownmyway.views.components.GetPathFromLocation;
import com.ownmyway.views.components.TripSearchRecord;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.Objects;

/**
 * Fragment used to accept a trip. Generates a modal that shows trip details and allows a user to
 * accept a trip.
 */
public class AcceptTripRequestFragment extends DialogFragment {
    private String apiKey;

    private TripSearchActivity parentActivity;
    private OnFragmentInteractionListener listener;
    private TripSearchRecord tripSearchRecord;
    private int position;

    private boolean showAcceptedPendingRides;

    /**
     * Constructor for AcceptTripRequestFragment
     *
     * @param tripSearchRecord,position the tripSearchRecord and its position in the tripSearch list
     */
    public AcceptTripRequestFragment(TripSearchRecord tripSearchRecord,
                                     int position,
                                     TripSearchActivity parentActivity,
                                     boolean showAcceptedPendingRides,
                                     String apiKey) {
        this.tripSearchRecord = tripSearchRecord;
        this.position = position;
        this.parentActivity = parentActivity;
        this.showAcceptedPendingRides = showAcceptedPendingRides;
        this.apiKey = apiKey;
    }

    /**
     * OnAcceptPressed allows the driver to accept trips by pressing the accept button
     */
    public interface OnFragmentInteractionListener {
        void onAcceptPressed(TripSearchRecord tripSearchRecord, int position);
    }

    /**
     * onAttach handles the listener when the fragment is attached
     */
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            listener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString() +
                    "must implement onFragmentInteractionListener");
        }
    }

    /**
     * OnCreateDialog is builds the TripRequest Fragment and fills the views with correct
     * information from database
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View view = View.inflate(getActivity(), R.layout.accept_trip_request_fragment, null);

        TextView estimatedCost = view.findViewById(R.id.fragment_estimated_cost);
        TextView startAdd = view.findViewById(R.id.fragment_startAdd);
        TextView endAdd = view.findViewById(R.id.fragment_endAdd);
        TextView rider = view.findViewById(R.id.fragment_riderName);
        TextView driverDistance = view.findViewById(R.id.fragment_distance);
        Button viewContactButton = view.findViewById(R.id.viewContactButton);

        //If the trip search record exists add information to the fragment
        if (tripSearchRecord != null) {
            estimatedCost.setText(tripSearchRecord.getEstimatedCost());
            startAdd.setText(tripSearchRecord.getStartAddress());
            endAdd.setText(tripSearchRecord.getEndAddress());
            rider.setText(tripSearchRecord.getRiderName());
            driverDistance.setText(tripSearchRecord.getDistanceFromDriver());
            viewContactButton.setOnClickListener(v -> handleViewRiderContact(tripSearchRecord.getRiderID()));

            //finds and initializes the mapView fragment
            MapView mapView = view.findViewById(R.id.lite_map);
            MapsInitializer.initialize(Objects.requireNonNull(getActivity()));
            mapView.onCreate(savedInstanceState);
            mapView.onResume();

            //Gets the mapAsync and overlays the route polyline and start/end markers
            mapView.getMapAsync(googleMap -> {
                LatLng origin = tripSearchRecord.getStartLatLng();
                LatLng destination = tripSearchRecord.getEndLatLng();
                new GetPathFromLocation(
                        origin,
                        destination,
                        apiKey,
                        googleMap::addPolyline
                        ).execute();
                googleMap.addMarker(new MarkerOptions().position(origin).title("start"));
                googleMap.addMarker(new MarkerOptions().position(destination).title("end"));
                LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                boundsBuilder.include(origin);
                boundsBuilder.include(destination);
                int routePadding = 120;
                LatLngBounds latLngBounds = boundsBuilder.build();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngBounds(latLngBounds, routePadding));
            });

        }

        //builds the dialog
        final AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        if (showAcceptedPendingRides) {
            return builder.setView(view).setTitle("View Trip")
                    .create();
        } else {
            return builder.setView(view).setTitle("View Trip")
                    .setNegativeButton("Ignore", null)
                    .setPositiveButton("Fair enough, offer ride", (dialog, whichButton) ->
                        listener.onAcceptPressed(tripSearchRecord, position))
                    .create();
        }
    }

    /**
     * Handles the user clicking the View Contact Details button
     *
     * @param riderID is the riderID - it is used to pull the correct information
     */
    private void handleViewRiderContact(String riderID) {
        Intent contactIntent = new Intent(parentActivity, ContactViewerActivity.class);
        ApplicationController.handleViewContactInformation(parentActivity, contactIntent, riderID, null);
    }

    /**
     * Destroys the map when the dialog fragment is closed. It prevents the app from
     * crashing when new maps are drawn
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        MapFragment f = (MapFragment) Objects.requireNonNull(getActivity())
                .getFragmentManager()
                .findFragmentById(R.id.lite_map);
        if (f != null)
            getActivity().getFragmentManager().beginTransaction()
                    .remove(f).commit();
    }

}
