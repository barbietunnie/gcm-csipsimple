/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Copyright (C) 2013-2013 Min Wang (aka mingewang@gmail.com - www.comrite.com)
 * This file is part of GCM-CSipSimple.
 * Part of it is add handling msg.
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

import java.util.ArrayList;

import com.csipsimple.api.SipProfile;
import com.google.android.gms.gcm.GoogleCloudMessaging;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

/**
 * This {@code IntentService} does the actual handling of the GCM message.
 * {@code GcmBroadcastReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    // record the previous network status: wifi on/off etc
    public static final String PROPERTY_NETWORK_STATUS = "network_status";
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }
    public static final String TAG = "GCM CSipSimple";

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM will be
             * extended in the future with new message types, just ignore any message types you're
             * not interested in, or that you don't recognize.
             */
            if (GoogleCloudMessaging.MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " + extras.toString());
            // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
                //extras.toString() =  Bundle[{android.support.content.wakelockid=1, price=test, collapse_key=do_not_collapse, from=840450064827}]
            	handleMsg(extras);
                
            }
        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    /**
     * handle the msg
     * @param extras 
     */
    private void handleMsg(Bundle extras){
    	String msg = extras.getString("incoming_msg");
    	
    	// wake up wifi
    	WifiManager wifiManager = (WifiManager) this.getSystemService(Context.WIFI_SERVICE);
    	boolean wifiEnabled = wifiManager.isWifiEnabled();
    	
    	
    	if (msg.equals("register")) {
    		storeStatus(wifiEnabled);
	    	if (wifiEnabled) {
	    		// do nothing
	    	} else {
	    	  // wifiManager.setWifiEnabled(false);
	    	  wifiManager.setWifiEnabled(true);
	    	}
	    	registerSipAccounts(true);
	    	Log.i(TAG, "tried to register all sip accounts");
    	}
    	
    	
    	if (msg.equals("unregister")) {
    		registerSipAccounts(false);
    		boolean previos_status = getStatus();
    		if( previos_status == false ) {
    			 wifiManager.setWifiEnabled(false);
    		}
    		Log.i(TAG, "tried to un-register all sip accounts");
    	}
    	
    	// Post notification of received message.
        //sendNotification("Received: " + msg);
        //Log.i(TAG, "Received: " + msg);
    			
    }
    
    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getPreferences() {
        return this.getSharedPreferences(PROPERTY_NETWORK_STATUS,
                Context.MODE_PRIVATE);
    }
    
    private void storeStatus(boolean status) {
        final SharedPreferences prefs = getPreferences();
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PROPERTY_NETWORK_STATUS, status);
        editor.commit();
    }
    
    private boolean getStatus() {
        final SharedPreferences prefs = getPreferences();
        boolean status = prefs.getBoolean(PROPERTY_NETWORK_STATUS, false);
        return status;
    }

    
    private void registerSipAccounts(boolean active) {
    	ArrayList<SipProfile> accounts;
    	accounts = SipProfile.getAllProfiles(this, 
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
    	
    	for(SipProfile account: accounts) {
	    	ContentValues cv = new ContentValues();
	    	cv.put(SipProfile.FIELD_ACTIVE, active);
	    	getContentResolver().update(
	    			ContentUris.withAppendedId(SipProfile.ACCOUNT_ID_URI_BASE, 
	    					account.id), cv, null, null);
	    }
    	
    }
    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
    	/*
         mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, DemoActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
      //  .setSmallIcon(R.drawable.ic_stat_gcm)
        .setContentTitle("GCM Notification")
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        */
        Log.i(TAG, "Received: here" + msg);
    }
}
