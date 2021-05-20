package com.ownmyway;

import android.content.Intent;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

import com.ownmyway.controllers.ApplicationController;
import com.ownmyway.model.Driver;
import com.ownmyway.model.User;
import com.ownmyway.model.UserLocation;
import com.ownmyway.views.activities.LoginActivity;
import com.ownmyway.views.activities.MapActivity;
import com.robotium.solo.Solo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class ControllerTest {
    ApplicationController c;
    Solo solo;


    @Rule
    public ActivityTestRule<LoginActivity> rule = new ActivityTestRule<>(LoginActivity.class, true, true);

    @Before
    public void setUp() throws Exception {
        solo = new Solo(InstrumentationRegistry.getInstrumentation(),rule.getActivity());
        App.getAuthDBManager().signOut();
        c = App.getController();

        App.getModel().clearModelForLogout();
    }


    @Test
    public void testLogin() {
        solo.assertCurrentActivity("Wrong Activity", LoginActivity.class);
        String email = "evan2@ownmyway.ca";
        String password = "123456";
        LoginActivity activity = (LoginActivity) solo.getCurrentActivity();
        Intent i = new Intent(activity, MapActivity.class);
        c.login(email, password, User.TYPE.RIDER, activity, i);
        assertTrue(solo.waitForActivity(MapActivity.class));
    }

    @Test
    public void testLoadSessionTrip() {
        solo.assertCurrentActivity("Wrong Activity", LoginActivity.class);
        LoginActivity activity = (LoginActivity) solo.getCurrentActivity();
        Intent i = new Intent(activity, MapActivity.class);
        c.loadSessionTrip(i, activity);
        // Should be true since no current user
        assertFalse(solo.waitForActivity(MapActivity.class));
    }

    @Test
    public void testGetTripsForUser() {
        solo.assertCurrentActivity("Wrong Activity", LoginActivity.class);
        String email = "evan2@ownmyway.ca";
        String password = "123456";
        LoginActivity activity = (LoginActivity) solo.getCurrentActivity();
        Intent i = new Intent(activity, MapActivity.class);
        c.login(email, password, User.TYPE.RIDER, activity, i);
        solo.waitForActivity(MapActivity.class);

        User mockUser = new Driver();
        mockUser.setCurrentUserLocation(new UserLocation(10, 10));
        App.getModel().setSessionUser(mockUser);
        c.getTripsForUser(null);
        solo.sleep(1000);
        assertNotNull(App.getModel().getSessionTripList());
    }
}
