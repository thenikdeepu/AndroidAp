package com.ownmyway.views.components;

import android.content.DialogInterface;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.ownmyway.App;
import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.Trip;
import com.ownmyway.model.User;
import com.ownmyway.R;
import com.ownmyway.views.activities.ContactViewerActivity;
import com.ownmyway.views.activities.EditAccountActivity;
import com.ownmyway.views.activities.LoginActivity;
import com.ownmyway.views.activities.MapActivity;
import com.ownmyway.views.activities.PaymentActivity;
import com.ownmyway.views.activities.RequestStatusActivity;
import com.ownmyway.views.activities.TripBuilderActivity;
import com.ownmyway.views.activities.TripSearchActivity;

import java.util.concurrent.atomic.AtomicBoolean;

import static com.ownmyway.model.User.TYPE.DRIVER;
import static com.ownmyway.model.User.TYPE.RIDER;

public class MapUIAddOnsManager {
    private MapActivity map;

    // RIDER MAIN ACTION BUTTON(s)
    private Button riderRequestMainBtn;
    private Button riderRequestCancelMainBtn;
    private Button riderCancelPickupBtn;
    private Button riderQRPaymentBtn;

    // DRIVER MAIN ACTION BUTTON(s)
    private Button driverShowRequestsMainBtn;
    private Button driverQRPaymentAcceptBtn;
    private Button driverShowAcceptedPendingRequestsBtn;

    // RIDER/DRIVER BUTTON(s)
    private Button cancelRideInProgress;

    // STATUS BUTTON(s)
    private Button statusButton;

    // SIDEBAR BUTTONS
    private Button settingsButton;
    private Button accountButton;
    private Button logoutButton;

    // SIDEBAR STATE
    private boolean showSideBar;
    private View sideBarView;


    public MapUIAddOnsManager(MapActivity map) {
        this.map = map;

        // INSTANTIATE RIDER MAIN ACTION BUTTONS
        this.riderRequestMainBtn = map.findViewById(R.id.rider_request_btn);
        this.riderRequestCancelMainBtn = map.findViewById(R.id.rider_request_cancel_btn);
        this.riderCancelPickupBtn = map.findViewById(R.id.rider_cancel_pickup_btn);
        this.riderQRPaymentBtn = map.findViewById(R.id.rider_qrpayment_btn);

        // INSTANTIATE DRIVER MAIN ACTION BUTTONS
        this.driverShowRequestsMainBtn = map.findViewById(R.id.driver_show_requests_btn);
        this.driverQRPaymentAcceptBtn = map.findViewById(R.id.driver_qrpaymentaccept_btn);
        this.driverShowAcceptedPendingRequestsBtn = map.findViewById(R.id.driver_show_accepted_requests_btn);

        // INSTANTIATE RIDER/DRIVER BUTTON(s)
        this.cancelRideInProgress = map.findViewById(R.id.cancel_ride_in_progess);

        // INSTANTIATE STATUS BUTTON
        this.statusButton = map.findViewById(R.id.rideStatus);

        // INSTANTIATE SIDEBAR BUTTONS
        this.settingsButton = map.findViewById(R.id.settings_button);
        this.accountButton = map.findViewById(R.id.account_button);
        this.logoutButton = map.findViewById(R.id.logout_button);

        // SIDEBAR STATE
        sideBarView = map.findViewById(R.id.sidebar);

        hideSettingsPanel();
        setMapButtonOnClickListeners();
    }

    /**
     * Sets up OnClick listeners for ALL buttons on the MapActivity
     **/
    private void setMapButtonOnClickListeners() {
        riderRequestMainBtn.setOnClickListener((View v) -> {
            Intent intent = new Intent(map, TripBuilderActivity.class);
            intent.putExtra(
                    "currentLatLong",
                    new double[]{
                            map.mLastKnownUserLocation.getLatitude(),
                            map.mLastKnownUserLocation.getLongitude()});
            intent.putExtra("GEOFENCE_DETECTION_TOLERANCE",
                    map.GEOFENCE_DETECTION_TOLERANCE);
            map.startActivity(intent);
        });

        riderRequestCancelMainBtn.setOnClickListener((View v) -> {
            DialogInterface.OnClickListener dialogClickListener = ((DialogInterface dialog, int choice) -> {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Toast.makeText(map, "Cancelling trip...", Toast.LENGTH_SHORT).show();
                        ApplicationController.deleteRiderCurrentTrip(map);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(map);
            builder.setMessage("Cancel request?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        });

        riderCancelPickupBtn.setOnClickListener((View v) -> {
            Toast.makeText(map, "Cancelling trip...", Toast.LENGTH_SHORT).show();
            ApplicationController.deleteRiderCurrentTrip(map);
        });


        driverShowRequestsMainBtn.setOnClickListener((View v) -> map.startActivity(new Intent(map, TripSearchActivity.class)));


        driverShowAcceptedPendingRequestsBtn.setOnClickListener((View v) -> {
            Intent intent = new Intent(map, TripSearchActivity.class);
            intent.putExtra("ShowAcceptedPendingRidesFlag", true);
            map.startActivity(intent);
        });


        cancelRideInProgress.setOnClickListener((View v) -> {
            if (map.currentTripStatus != Trip.STATUS.EN_ROUTE) {
                return;
            }

            DialogInterface.OnClickListener dialogClickListener = ((DialogInterface dialog, int choice) -> {
                switch (choice) {
                    case DialogInterface.BUTTON_POSITIVE:
                        Toast.makeText(map, "Cancelling trip...", Toast.LENGTH_SHORT).show();
                        ApplicationController.deleteRiderCurrentTrip(map);
                        break;
                    case DialogInterface.BUTTON_NEGATIVE:
                        break;
                }
            });

            AlertDialog.Builder builder = new AlertDialog.Builder(map);
            builder.setMessage("Ride in progress! Are you extra sure?")
                    .setPositiveButton("Yes", dialogClickListener)
                    .setNegativeButton("No", dialogClickListener).show();
        });


        riderQRPaymentBtn.setOnClickListener((View v) -> {
            Intent paymentIntent = new Intent(map, PaymentActivity.class);
            map.startActivity(paymentIntent);
        });


        driverQRPaymentAcceptBtn.setOnClickListener((View v) -> {
            Intent paymentIntent = new Intent(map, PaymentActivity.class);
            map.startActivity(paymentIntent);
        });


        settingsButton.setOnClickListener((View v) -> {
            sideBarView.setVisibility(View.VISIBLE);
            settingsButton.setVisibility(View.INVISIBLE);
            setShowSideBar(true);

            hideMainActionButtons();
        });


        statusButton.setOnClickListener((View v) -> map.startActivity(new Intent(map, RequestStatusActivity.class)));


        accountButton.setOnClickListener((View v) -> {
            map.startActivity(new Intent(map, EditAccountActivity.class));
            hideSettingsPanel();
        });


        logoutButton.setOnClickListener((View v) -> {
            User curUser = App.getModel().getSessionUser();
            ApplicationController.manageLoggedStateAcrossTwoUserCollections(false, curUser, curUser.getType(), map);
            map.startActivity(new Intent(map, LoginActivity.class));
            map.finish();
        });
    }

    /**
     * Shows active main action button when necessary
     **/
    public void showActiveMainActionButton() {
        hideMainActionButtons();
        User.TYPE currentUserType = App.getModel().getSessionUser().getType();

        if (map.currentTripStatus == null) {
            hideStatusButton();

            if (currentUserType == RIDER) {
                riderRequestMainBtn.setVisibility(View.VISIBLE);
            } else if (currentUserType == DRIVER) {
                driverShowRequestsMainBtn.setVisibility(View.VISIBLE);
            }

        } else {
            showStatusButton();

            switch (map.currentTripStatus) {
                case PENDING:
                    if (currentUserType == RIDER) {
                        riderRequestCancelMainBtn.setVisibility(View.VISIBLE);
                    }
                    break;
                case DRIVER_ACCEPT:
                    if (currentUserType == RIDER) {
                        String driverID = App.getModel().getSessionTrip().getDriverID();
                        if (driverID == null) {
                            return;
                        }
                        handleRiderOfferAccept(driverID);
                    } else if (currentUserType == DRIVER) {
                        driverShowAcceptedPendingRequestsBtn.setVisibility(View.VISIBLE);
                        driverShowRequestsMainBtn.setVisibility(View.VISIBLE);
                    }
                    break;
                case DRIVER_PICKING_UP:
                    if (currentUserType == RIDER) {
                        riderCancelPickupBtn.setVisibility(View.VISIBLE);
                    } else if (currentUserType == DRIVER) {
                        driverShowAcceptedPendingRequestsBtn.setVisibility(View.VISIBLE);
                        driverShowRequestsMainBtn.setVisibility(View.VISIBLE);
                    }
                    break;
                case DRIVER_ARRIVED:
                    if (currentUserType == RIDER) {
                        ensureRiderHasBoardedRide();
                    } else if (currentUserType == DRIVER) {
                        driverShowAcceptedPendingRequestsBtn.setVisibility(View.VISIBLE);
                        driverShowRequestsMainBtn.setVisibility(View.VISIBLE);
                    }
                    break;
                case EN_ROUTE:
                    cancelRideInProgress.setVisibility(View.VISIBLE);
                    break;
                case COMPLETED:
                    if (currentUserType == RIDER) {
                        riderQRPaymentBtn.setVisibility(View.VISIBLE);
                    } else if (currentUserType == DRIVER) {
                        driverQRPaymentAcceptBtn.setVisibility(View.VISIBLE);
                    }
                    break;
            }
        }
    }

    /**
     * Handles user interaction with rider accept button
     **/
    private void handleRiderOfferAccept(String driverID) {
        if (map.currentTripStatus != Trip.STATUS.DRIVER_ACCEPT) {
            return;
        }

        View view = View.inflate(map, R.layout.accept_trip_offer_fragment, null);
        Button viewContactButton = view.findViewById(R.id.viewContactButton);
        viewContactButton.setOnClickListener(v -> {
            String riderID = App.getModel().getSessionTrip().getRiderID();
            Intent contactIntent = new Intent(map, ContactViewerActivity.class);
            ApplicationController.handleViewContactInformation(map, contactIntent, riderID, driverID);
        });

        // can't modify local vars in Java, so a thread-safe AtomicBoolean wrapper is used
        final AtomicBoolean userHasConfirmed = new AtomicBoolean(false); // constructor defaults to false
        DialogInterface.OnClickListener dialogClickListener = ((DialogInterface dialog, int choice) -> {
            switch (choice) {
                case DialogInterface.BUTTON_POSITIVE:
                    ApplicationController.handleNotifyDriverForPickup();
                    userHasConfirmed.set(true);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    Toast.makeText(map, "Cancelling trip...", Toast.LENGTH_SHORT).show();
                    ApplicationController.deleteRiderCurrentTrip(map);
                    userHasConfirmed.set(true);
                    break;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(map);
        builder.setMessage("A driver has accepted! Proceed?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .setView(view);

        // This builder MUST persist because the USER has to be sure they are on their way!
        builder.setOnDismissListener(dialog -> {
            if (view.getParent() != null) {
                ((ViewGroup) view.getParent()).removeView(view);
            }
            if (!userHasConfirmed.get()) {
                builder.show();
            }
        });

        builder.show();
    }

    /**
     * Handles user interaction with rider accept button
     **/
    private void ensureRiderHasBoardedRide() {
        if (map.currentTripStatus != Trip.STATUS.DRIVER_ARRIVED) {
            return;
        }

        // can't modify local vars in Java, so a thread-safe AtomicBoolean wrapper is used
        final AtomicBoolean userHasConfirmed = new AtomicBoolean(false); // constructor defaults to false
        DialogInterface.OnClickListener dialogClickListener = ((DialogInterface dialog, int choice) -> {
            switch (choice) {
                case DialogInterface.BUTTON_POSITIVE:
                    ApplicationController.beginTrip();
                    userHasConfirmed.set(true);
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    Toast.makeText(map, "Sorry to see you go.\nCancelling trip...", Toast.LENGTH_SHORT).show();
                    userHasConfirmed.set(true);
                    ApplicationController.deleteRiderCurrentTrip(map);
                    break;
            }
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(map);
        builder.setMessage("Your ride is here.\nPlease confirm that your trip is about to begin.")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No, I am cancelling", dialogClickListener).create();

        // This builder MUST persist because the USER has to be sure they are on their way!
        builder.setOnDismissListener(dialog -> {
            if (!userHasConfirmed.get()) {
                builder.show();
            }
        });

        builder.show();
    }


    /**
     * Hides main action buttons when necessary
     */
    private void hideMainActionButtons() {
        riderRequestMainBtn.setVisibility(View.GONE);
        riderRequestCancelMainBtn.setVisibility(View.GONE);
        riderCancelPickupBtn.setVisibility(View.GONE);
        riderQRPaymentBtn.setVisibility(View.GONE);

        driverShowRequestsMainBtn.setVisibility(View.GONE);
        driverQRPaymentAcceptBtn.setVisibility(View.GONE);
        driverShowAcceptedPendingRequestsBtn.setVisibility(View.GONE);

        cancelRideInProgress.setVisibility(View.GONE);
    }

    /**
     * Hides settings sidebar panel when necessary
     */
    public void hideSettingsPanel() {
        sideBarView.setVisibility(View.INVISIBLE);
        settingsButton.setVisibility(View.VISIBLE);
        setShowSideBar(false);

        showActiveMainActionButton();
    }

    public void showStatusButton() {
        statusButton.setVisibility(View.VISIBLE);
    }

    private void hideStatusButton() {
        statusButton.setVisibility(View.GONE);
    }

    public boolean isShowSideBar() {
        return showSideBar;
    }

    private void setShowSideBar(boolean showSideBar) {
        this.showSideBar = showSideBar;
    }
}
