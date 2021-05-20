package com.ownmyway.views.activities;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.ownmyway.App;
import com.ownmyway.model.ApplicationModel;
import com.ownmyway.model.Driver;
import com.ownmyway.model.User;
import com.ownmyway.R;

public class ContactViewerActivity extends AppCompatActivity {
    private String email;
    private String phoneNumber;
    private TextView ratingText;
    private static final int PERMISSIONS_REQUEST_ACCESS_CALL_PHONE = 1232;

    /**
     * onCreate used to create the ContactViewerActivity when called
     *
     * @param savedInstanceState is a saved state instance
     */
    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.contact_viewer_activity);
        Intent i = this.getIntent();
        email = i.getStringExtra("email");
        phoneNumber = i.getStringExtra("phoneNumber");
        String userID = i.getStringExtra("ID");
        String userName = i.getStringExtra("username");
        TextView userEmailTextView = findViewById(R.id.userEmailTextView);
        TextView userPhoneNumberTextView = findViewById(R.id.userPhoneTextView);
        TextView userNameTextView = findViewById(R.id.userNameTextView);
        Button phoneButton = findViewById(R.id.phoneButton);
        Button emailButton = findViewById(R.id.emailBtn);
        ratingText = findViewById(R.id.driver_rating_text);
        TextView ratingBanner = findViewById(R.id.driver_rating_banner);

        // Only show contact information if userId == trip.driverId
        ApplicationModel m = App.getModel();
        User currentUser = App.getModel().getSessionUser();

        if (m.getSessionTrip() != null) {
            if (m.getSessionTrip().getDriverID().equals(userID)) {
                phoneButton.setVisibility(View.VISIBLE);
                emailButton.setVisibility(View.VISIBLE);
            }
        } else {
            phoneButton.setVisibility(View.INVISIBLE);
            emailButton.setVisibility(View.INVISIBLE);
        }

        //If user is a RIDER they will be able to view Drivers Ratings
        if (currentUser.getType() == User.TYPE.RIDER) {
            ratingText.setVisibility(View.VISIBLE);
            ratingBanner.setVisibility(View.VISIBLE);
            String driverID = m.getSessionTrip().getDriverID();
            App.getDbManager().getDriver(driverID, (resultData, err) -> {
                if (resultData != null) {
                    Driver driver = (Driver) resultData.get("user");
                    assert driver != null;
                    ratingText.setText(driver.getRating() + "/ 100.0");
                }
            });
        } else {
            ratingText.setVisibility(View.INVISIBLE);
            ratingBanner.setVisibility(View.INVISIBLE);
        }

        userNameTextView.setText(userName);
        userEmailTextView.setText(email);
        userPhoneNumberTextView.setText(phoneNumber);

        phoneButton.setOnClickListener(v -> {
            this.getPhonePermission();
            this.handlePhoneRequest();
        });
        emailButton.setOnClickListener(v -> this.handleEmailRequest());
    }

    /**
     * handlePhoneRequest function handles user interaction with the phone button
     * it allows user to call another users phone number
     */
    public void handlePhoneRequest() {
        new AlertDialog.Builder(this)
                .setTitle("Phone User")
                .setMessage("Do you want to phone this driver?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + phoneNumber));
                    if (ActivityCompat.checkSelfPermission(ContactViewerActivity.this,
                            Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(ContactViewerActivity.this,
                                "Unable to make a call at this time. You may not have permissions enabled",
                                Toast.LENGTH_LONG).show();
                        return;
                    }
                    startActivity(callIntent);
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    /**
     * handleEmailRequest function allows the user to email another user when the email button
     * in the ContactViewer is pressed
     */
    @SuppressLint("IntentReset")
    public void handleEmailRequest() {
        new AlertDialog.Builder(this)
                .setTitle("Email User")
                .setMessage("Do you want to email this driver?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Intent emailIntent = new Intent(Intent.ACTION_SEND);
                    String[] TO = {email};
                    emailIntent.setData(Uri.parse("mailto:"));
                    emailIntent.setType("text/plain");
                    emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
                    emailIntent.putExtra(Intent.EXTRA_SUBJECT, "From your Rider");
                    if (emailIntent.resolveActivity(getPackageManager()) != null) {
                        startActivity(emailIntent);
                    }
                })
                .setNegativeButton(android.R.string.no, null).show();
    }

    /**
     * getPhonePermission asks allows OwnMyWay to ask for a users permission to use their phone
     */
    private void getPhonePermission() {
        if (ContextCompat
                .checkSelfPermission(this.getApplicationContext(), android.Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CALL_PHONE}, PERMISSIONS_REQUEST_ACCESS_CALL_PHONE);
        }
    }
}
