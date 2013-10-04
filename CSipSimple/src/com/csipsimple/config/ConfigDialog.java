
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
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class ConfigDialog extends Dialog{
	public static final String TAG = "GCM-CSipsimple-Configure";		
	// The  filename to store the preference
	public static final String JSON_CONFIG_NAME = "json-configure";
	public static final int MODE_PRIVATE = 0;
	public static final int FETCH_STATUS_OK = 200;
	private static Activity mActivity = null;
	SharedPreferences settings;
	
	boolean already_configured;
	
	EditText  config_user_pw_EditText;
	String	 config_user_pw;
	
	EditText  config_user_name_EditText;
	String	  config_user_name;
	
	int  statusCode = 0;
	

	public ConfigDialog(Activity _activity) {
		super(_activity);
		mActivity = _activity;
		// get the settings
		settings = mActivity.getSharedPreferences(JSON_CONFIG_NAME,MODE_PRIVATE);
			
		already_configured = settings.getBoolean("already_configured", false);
		config_user_name = settings.getString("config_user_name", "");
		config_user_pw = settings.getString("config_user_pw", "");
		  
		
	 }
	
	public boolean alreadyConfigured() {
		return already_configured;
	}
		 /* Standard Android on create method that gets called when the activity initialized.
		 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.config_dialog);
		
		config_user_pw_EditText = (EditText) findViewById(R.id.id_config_user_pw);
		config_user_pw_EditText.setText(config_user_pw);
		
		
		config_user_name_EditText = (EditText) findViewById(R.id.id_config_user_name);
		config_user_name_EditText.setText(config_user_name);
		
		Button  confirmButton = (Button) findViewById(R.id.id_confirm);
		confirmButton.setOnClickListener(new android.view.View.OnClickListener() {
	        @Override
			public void onClick(View arg0) {
				if ( saveConfig() ) {
					dismiss();
				}
			}

		
	      });
	}
	
	public void alert(String title_string, String yes_string){
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(mActivity);
		// set title
		alertDialogBuilder.setTitle(title_string);
		 
		// set dialog message
		if(yes_string != null ) {
		   alertDialogBuilder
			.setMessage(yes_string)
			.setCancelable(false)
			.setPositiveButton("Yes",new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog,int id) {
						dialog.cancel();
					}
			});
		}
		
		// create alert dialog
		AlertDialog alertDialog = alertDialogBuilder.create();
		// show it
		alertDialog.show();
		
	}
	
	


	
	public boolean saveConfig()
	{
		SharedPreferences.Editor editor = settings.edit();
		
		String value1 = config_user_name_EditText.getText().toString();
		config_user_name=value1;
		
		String value2 = config_user_pw_EditText.getText().toString();
		config_user_pw=value2;
		
		editor.putString("config_user_name",config_user_name);
		editor.putString("config_user_pw", config_user_pw);
		editor.putBoolean("already_configured",true);
		
		editor.commit();
		
		LauchMainActivity();
		
		if( statusCode == FETCH_STATUS_OK ) return true;
		
		return false;
		
	}
	
    
    private void LauchMainActivity(){
    	new AsyncTaskFetchConfig().execute();
		
    }

   
    private class AsyncTaskFetchConfig extends AsyncTask<Void, Void, Integer> {
 
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }
 
        @Override
        protected Integer doInBackground(Void... arg0) {
    		FetchJsonConfigure cfg = new FetchJsonConfigure(mActivity,config_user_name,config_user_pw);
    		statusCode = cfg.applyConfig();
            return statusCode;
        }
 
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            if(result == FETCH_STATUS_OK ) {
	            Intent i = new Intent(mActivity, SipHome.class);
	            mActivity.startActivity(i);
	            mActivity.finish();
	        } else {
	        	alert("Something wrong, StatusCode: " + statusCode, "Please check your username and password, try again");
	        }
        }
 
    }
 
	

	
}