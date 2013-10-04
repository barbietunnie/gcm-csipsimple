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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.util.ByteArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.content.Context;
import android.content.Intent;
import android.net.Credentials;
import android.os.AsyncTask;
import android.text.format.DateFormat;

import com.csipsimple.backup.SipProfileJson;
import com.csipsimple.ui.SipHome;
import com.csipsimple.utils.Log;
import com.csipsimple.utils.PreferencesWrapper;

public class FetchJsonConfigure {

	private Context ctxt;
	private Thread asyncFetchConfiger;
	private String username;
	private String pw;
	public static final String server_url = "https://clientconfig.ng-voice.com/";
	
	public FetchJsonConfigure(Context _ctxt){
		ctxt = _ctxt;
	}
	
	public FetchJsonConfigure(Context _ctxt, String _username, String _pw){
		ctxt = _ctxt;
		username = _username;
		pw = _pw;
	}
	
    public void run(){
    	// 	Async check
    	asyncFetchConfiger = new Thread() {
    		public void run() {
    			asyncFetchConfig2();
        	};
    	};
    	asyncFetchConfiger.start();
    }
    
    // return statusCode
    public int applyConfig(){
    	return asyncFetchConfig2();
    }

    private void asyncFetchConfig() {
     
    	try{
    		File dir = PreferencesWrapper.getConfigFolder(ctxt);
            Date d = new Date();
            File file = new File(dir.getAbsoluteFile() + File.separator + "config_"
                    + DateFormat.format("yy-MM-dd_kkmmss", d) + ".json");
            Log.d("FetchJsonConfigure", "Out dir " + file.getAbsolutePath());
    		URL url = new URL("https://" + username + ":" + pw + "@" + server_url);
    		
    		HttpsURLConnection ucon = (HttpsURLConnection)url.openConnection();
    		
    		InputStream is = (InputStream) ucon.getInputStream();
    		BufferedInputStream bis = new BufferedInputStream(is);
    		
            // Read bytes to the Buffer until there is nothing more to read(-1).
            ByteArrayBuffer baf = new ByteArrayBuffer(50);
            int current = 0;
            while ((current = bis.read()) != -1) {
                    baf.append((byte) current);
            }

            // Convert the Bytes read to a String. 
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baf.toByteArray());
            fos.close();
            
            SipProfileJson.restoreSipConfiguration(ctxt,
            		file);
    	} catch (IOException e) {
    		Log.d("FetchJsonConfigure", "config exeption " + e.getMessage());
    	}
    }
    

    private int asyncFetchConfig2() {
    	int statusCode = -1;
    	try{
    		File dir = PreferencesWrapper.getConfigFolder(ctxt);
            Date d = new Date();
            File file = new File(dir.getAbsoluteFile() + File.separator + "config_"
                    + DateFormat.format("yy-MM-dd_kkmmss", d) + ".json");
            Log.d("FetchJsonConfigure", "Out dir " + file.getAbsolutePath());
           
    		HttpClient client = HttpsClient.getClient();//new DefaultHttpClient();
    		UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username, pw);
    		((AbstractHttpClient) client).getCredentialsProvider().setCredentials(
    				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT), 
    				creds);
    		HttpGet httpGet = new HttpGet(server_url);
    		
        	try {
                HttpResponse response = client.execute(httpGet);
                StatusLine statusLine = response.getStatusLine();
                statusCode = statusLine.getStatusCode();
                if(statusCode == 200) {
              	 HttpEntity entity = response.getEntity();
              	 if ( entity != null ) {
              		 //String res = EntityUtils.toString(entity);
              		 InputStream is = (InputStream)entity.getContent();
              		 BufferedInputStream bis = new BufferedInputStream(is);
              		
                     // Read bytes to the Buffer until there is nothing more to read(-1).
                      
                     ByteArrayBuffer baf = new ByteArrayBuffer(50);
                     int current = 0;
                     while ((current = bis.read()) != -1) {
                             baf.append((byte) current);
                     }
						
                     // Convert the Bytes read to a String. 
                     FileOutputStream fos = new FileOutputStream(file);
                     fos.write(baf.toByteArray());
                     //fos.write(res);
                     fos.close();
                     
                     if ( !SipProfileJson.restoreSipConfiguration(ctxt,file) ) {
                    	 statusCode = 900; 
                     }
              	 }
                } else {
                	Log.d("FetchJsonConfigure", "statusCode is" + statusCode);
                }
                
          } catch (ClientProtocolException e) {
                e.printStackTrace();
          } catch (IOException e) {
                e.printStackTrace();
          }
    		
    	} catch (Exception e) {
    		Log.d("FetchJsonConfigure", "config exeption " + e.getMessage());
    	}
		return statusCode;
    }

}
