/**
 * Copyright (C) 2013-2013 Min Wang (aka mingewang@gmail.com - www.comrite.com)
 * This file is part of GCM-CSipSimple.
 *
 *  GCM-CSipSimple is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  If you own a pjsip commercial license you can also redistribute it
 *  and/or modify it under the terms of the GNU Lesser General Public License
 *  as an android library.
 *
 *  GCM-CSipSimple is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with CSipSimple.  If not, see <http://www.gnu.org/licenses/>.
 *  
 *  This file and this file only is also released under Apache license as an API file
 */


package com.csipsimple.pushnotifications;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import com.csipsimple.api.SipProfile;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * register, unregister, post to GCM Server
 */
public class GcmRegister {

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    
    // give your server registration url here
    static final String SERVER_URL = "http://192.168.1.204/gcm_server_php/register.php"; 
 
    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "840450064827";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "GCM CSipsimple";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    Context context;

    String regid;
    JSONObject reg_json;
    
    public GcmRegister(Context _context) {
    	context = _context;
    	
    }

    // the device need to add google account if < 4.0.4
    // need at api level 5, need to a logged in google account
    /*
    private boolean deviceHasGoogleAccount(){
        AccountManager accMan = AccountManager.get(context);
        Account[] accArray = accMan.getAccountsByType("com.google");
        return accArray.length >= 1 ? true : false;
    }
    */
    
    public void register() {
      

        // Check device for Play Services APK. If check succeeds, proceed with GCM registration.
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(context);
            regid = getRegistrationId(context);

            if (regid.length() <= 0 ) {
                registerInBackground();
            } else {
            	// no matter what, send regid to our own backend server
            	sendRegistrationIdToBackend();
            }
        } else {
            Log.i(TAG, "No valid Google Play Services APK found.");
        }
    }

    
    protected void onResume() {
        // Check device for Play Services APK.
        checkPlayServices();
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                //GooglePlayServicesUtil.getErrorDialog(resultCode, context,
                //        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            	Log.i(TAG, "error result code is :" + resultCode);
            } else {
                Log.i(TAG, "This device is not supported.");
                //finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Stores the registration ID and the app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGcmPreferences(context);
        int appVersion = getAppVersion(context);
        Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    /**
     * Gets the current registration ID for application on GCM service, if there is one.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGcmPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length()<= 0 ) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }
        return registrationId;
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and the app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    //regid="xdfdf";
                    msg = "Device registered, registration ID=" + regid;

                    // You should send the registration ID to your server over HTTP, so it
                    // can use GCM/HTTP or CCS to send messages to your app.
                    sendRegistrationIdToBackend();

                    // Persist the regID - no need to register again.
                    storeRegistrationId(context, regid);
                //} catch (IOException ex) {
                } catch (Exception ex) {
                    msg = "Error :" + ex.getMessage();
                    // If there is an error, don't just keep trying to register.
                    // Require the user to click a button again, or perform
                    // exponential back-off.
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
            	 Log.i(TAG, "gcm register status: "+ msg);
            }
        }.execute(null, null, null);
    }

    // not used so far
    // Send an upstream message.
    public void sendUpStream(String msg) {

            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... params) {
                    String msg = "";
                    try {
                        Bundle data = new Bundle();
                        data.putString("my_message", "Hello World");
                        data.putString("my_action", "com.google.android.gcm.demo.app.ECHO_NOW");
                        String id = Integer.toString(msgId.incrementAndGet());
                        gcm.send(SENDER_ID + "@gcm.googleapis.com", id, data);
                        msg = "Sent message";
                    } catch (IOException ex) {
                        msg = "Error :" + ex.getMessage();
                    }
                    return msg;
                }

                @Override
                protected void onPostExecute(String msg) {
                	 Log.i(TAG, "msg send:" + msg);
                }
            }.execute(null, null, null);
     
    }


    /**
     * @return Application's version code from the {@code PackageManager}.
     */
    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (NameNotFoundException e) {
            // should never happen
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGcmPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return context.getSharedPreferences(PROPERTY_REG_ID,
                Context.MODE_PRIVATE);
    }
    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP or CCS to send
     * messages to your app. 
     */
    // need to change to asynch task
    private void sendRegistrationIdToBackend() {
    	ArrayList<SipProfile> accounts;
    	accounts = SipProfile.getAllProfiles(context, 
    			false,
                new String[] {
                        SipProfile.FIELD_ID,
                        SipProfile.FIELD_ACC_ID,
                        SipProfile.FIELD_ACTIVE,
                        SipProfile.FIELD_DISPLAY_NAME,
                        SipProfile.FIELD_WIZARD
                });
    	
    	if(accounts.isEmpty()) {
    		Log.i(TAG, "sipuri is null , return");
    		return;
    	}
    	
    	try {
    	
    	JSONArray sip_uris = new JSONArray();
    	
    	for(SipProfile account: accounts) {
    		sip_uris.put(account.getUriString());
			
    	}	
    	reg_json = new JSONObject();
    	reg_json.put("reg_id", regid);
    	reg_json.put("sip_uris", sip_uris);
    	} catch ( Exception e ) {
    		Log.i(TAG, "json construct error"+e.getMessage());
    	}
    	
    	//{"reg_id":"xdfdf","sip_uris":["<sip:12345@192.168.1.26>"]}
    	
      // Your implementation here.
    	 new AsyncTask<String, Void, String>() {
              @Override
              protected String doInBackground(String... bodys ) {
            	  // can only pass the String[], not string!
            	  String serverUrl = SERVER_URL;
            	  String body = bodys[0];
                  try {
                		post(serverUrl, body);
                	
                  } catch (IOException e) {
               	   Log.i(TAG, "post error"+e.getMessage());
                  }
                  return "OK";
              }
              @Override
              protected void onPostExecute(String msg) {
              	 Log.i(TAG, "msg send:" + msg);
              }
			
          }.execute(reg_json.toString());
            	  
    	   
    }

    private static void post(String endpoint, String params)
            throws IOException {    
         
        URL url;
        try {
            url = new URL(endpoint);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("invalid url: " + endpoint);
        }
        
        StringBuilder bodyBuilder = new StringBuilder();
        // constructs the POST body using the parameters
        bodyBuilder.append("params").append('=')
                    .append(params);
            
        String body = bodyBuilder.toString();
        Log.v(TAG, "Posting '" + body + "' to " + url);
        byte[] bytes = body.getBytes();
        HttpURLConnection conn = null;
        try {
            Log.e("URL", ":" + url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setFixedLengthStreamingMode(bytes.length);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded;charset=UTF-8");
            // post the request
            OutputStream out = conn.getOutputStream();
            out.write(bytes);
            out.close();
            // handle the response
            int status = conn.getResponseCode();
            if (status != 200) {
              throw new IOException("Post failed with error code " + status);
            }
        //} catch (Exception e) {
     	//   Log.i(TAG, "post error: "+e.getMessage());
        }finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
      }

    
   
}
