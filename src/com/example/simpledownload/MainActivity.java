package com.example.simpledownload;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	private static final String LOG_TAG = "Debug";
	private EditText urlText;
	private EditText maxDelayText;
	private WebView webView;
	private TextView statusView;
	private TextView contentView;
	private String policy;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        urlText = (EditText) findViewById(R.id.myUrl);
        maxDelayText = (EditText) findViewById(R.id.maxDelay);
        webView = (WebView) findViewById(R.id.webView);
        statusView = (TextView) findViewById(R.id.status);
        contentView = (TextView) findViewById(R.id.content);
        //textView = new TextView(Context); 
        startSsService();
        Log.i(LOG_TAG, "onCreate done");
    }
    
    @Override
    protected void onPause() {
    		super.onPause();
    		//stopSsService();
    }
    
    @Override
    protected void onResume() {
    		super.onResume();
    		//startSsService();
    }
    
    private void startSsService() {
    		Intent mServiceIntent = new Intent(this, SsService.class);
    		startService(mServiceIntent);
    		Log.i(LOG_TAG, "startSsService done");
    		if(isServiceRunning()) {
    			Log.i(LOG_TAG, "SsService running");
    		}
    		else {
    			Log.i(LOG_TAG, "SsService not running");
    		}
    }
    
    private void stopSsService() {
    		if(isServiceRunning()) {
    			Intent mServiceIntent = new Intent(this, SsService.class);
    			stopService(mServiceIntent);
    			Log.i(LOG_TAG, "stopSsService done");
    		}
    		else {
    			Log.i(LOG_TAG, "SsService not running");
    		}
    }
    
    private boolean isServiceRunning() {
    		ActivityManager m = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    		for(RunningServiceInfo service : m.getRunningServices(Integer.MAX_VALUE)) {
    			if(SsService.class.getName().equals(service.service.getClassName())) {
    				return true;
    			}
    		}
    		return false;
    }
    
    public void myClickHandler_directsend(View view) {
    	statusView.setText("Directly send selected");
    		policy = SsService.SS_ALWAYS_READY_ACTION;
    		startDownload();
    }
    
    public void myClickHandler_performance(View view) {
    	statusView.setText("Performance delay policy selected");
    		policy = SsService.SS_PERFORMANCE_READY_ACTION;
    		startDownload();
    }
    
    public void myClickHandler_moderate(View view) {
    	statusView.setText("Moderate delay policy selected");
		policy = SsService.SS_MODERATE_READY_ACTION;
		startDownload();
    }

    public void myClickHandler_powersaving(View view) {
    	statusView.setText("Powersaving delay policy selected");
		policy = SsService.SS_POWERSAVING_READY_ACTION;
		startDownload();
    }
    
    /*public void myClickHandler_anysschange(View view) {
		policy = SsService.SS_SSCHANGED_READY_ACTION;
		startDownload();
    }*/
    
    public void startDownload() {
    		//String stringUrl = "http://www.cnn.com";
    		String stringUrl = urlText.getText().toString();
    		int maxDelay = Integer.parseInt(maxDelayText.getText().toString());
    		contentView.setText("");
    		//MyWebView myWebView = new MyWebView(getApplicationContext(), webView, policy, maxDelay);
    		//myWebView.loadUrl(stringUrl);
    		ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    		NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
    		if(networkInfo != null && networkInfo.isConnected()) {
    			new DownloadWebpageTask(policy, maxDelay).execute(stringUrl);
    		} else {
    			statusView.setText("No network connection available");
    		}
    }
    
    
    
    private class DownloadWebpageTask extends AsyncTask<String, String, String> {
    		String policy;
    		int maxDelay;
    		public DownloadWebpageTask(String policy, int maxDelay) {
    			super();
    			this.policy = policy;
    			this.maxDelay = maxDelay;
    		}
    	
    		@Override
    		protected String doInBackground(String... urls) {
    			try {
    				return downloadUrl(urls[0]);
    			} catch (IOException e) {
    				return "Unable to retrieve webpage. URL may be invalid.";
    			}
    		}
    		
    		@Override
    		protected void onPostExecute(String result) {
    			contentView.setText(result);
    		}
    		
    		@Override
    		protected void onProgressUpdate(String... values) {
    			
    		}
    		
    		private String downloadUrl(String myurl) throws IOException {
    			InputStream is = null;
    			int len = 500;
    			getSystemService(Context.WIFI_SERVICE);
    			try {
    				URL url = new URL(myurl);
    				HttpURLConnection conno = (HttpURLConnection) url.openConnection();
    				//PhoneStateListener psl = new PhoneStateListener();
    				//String policy = SsService.SS_POWERSAVING_READY_ACTION;
    				MyHttpURLConnection conn = new MyHttpURLConnection(conno, getApplicationContext(), policy, maxDelay);
    				conn.setReadTimeout(10000); // ms
    				conn.setConnectTimeout(15000); // ms
    				conn.setRequestMethod("GET");
    				conn.setDoInput(true);
				conn.connect();
    				int response = conn.getResponseCode();
    				Log.d(LOG_TAG, "The response is: " + response);
    				is = conn.getInputStream();
    				
    				
    				String contentAsString = readIt(is, len);
    				return contentAsString;
    			} finally {
    				if(is != null) {
    					is.close();
    				}
    			}
    		}
    		
    		public String readIt(InputStream stream, int len) throws IOException, UnsupportedEncodingException {
    			Reader reader = null;
    			reader = new InputStreamReader(stream, "UTF-8");
    			char[] buffer = new char[len];
    			reader.read(buffer);
    			return new String(buffer);
    		}
    }
}
