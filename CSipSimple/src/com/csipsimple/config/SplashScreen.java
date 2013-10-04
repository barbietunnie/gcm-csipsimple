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
 *  
 */

package com.csipsimple.config;

import com.csipsimple.R;
import com.csipsimple.ui.SipHome;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class SplashScreen extends Activity {
	SharedPreferences settings;
	boolean already_configured;
	private static int SPLASH_TIME_OUT = 1000;
 
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
 
    	// get the settings
		settings = getSharedPreferences(ConfigDialog.JSON_CONFIG_NAME,ConfigDialog.MODE_PRIVATE);
			
		already_configured = settings.getBoolean("already_configured", false);
		
		if(already_configured) {
			new Handler().postDelayed(new Runnable() {
				@Override
	            public void run() {
	                Intent i = new Intent(SplashScreen.this, SipHome.class);
	                startActivity(i);
	                // close this activity
	                finish();
	            }
	        }, SPLASH_TIME_OUT);
			
		} else {
		  	//pop up user/name dialog and fetch json config
	       	ConfigDialog configDialog = new ConfigDialog(this);
	       	configDialog.setTitle("Config");
	       	configDialog.show();
	    }
 
    }
 
    
 
}