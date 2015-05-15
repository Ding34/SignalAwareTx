package com.example.simpledownload;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class MyHttpURLConnection {
	String LOG_TAG = "MyHttpURLConnection";
	HttpURLConnection conn;
	TelephonyManager tm;
	Context context;
	String policy;
	int max_delay_sec;
	MyBroadcastReceiver receiver;
	//MyPhoneStateListener myphoneStateListener;
	
	public MyHttpURLConnection(HttpURLConnection conn, Context context, String policy, int max_delay_sec) {
		this.conn = conn;
		this.context = context;
		this.policy = policy;
		this.max_delay_sec = max_delay_sec;
		init();
		Log.i(LOG_TAG, "MyHttpURLConnection constructor done");
	}
	
	private void init() {
		if(policy != SsService.SS_ALWAYS_READY_ACTION) {
			receiver = new MyBroadcastReceiver(Thread.currentThread());
			//context.registerReceiver(this.ssReceiver, 
			context.registerReceiver(receiver,
				new IntentFilter(policy));
			Log.i(LOG_TAG, "ssReceiver registered");
		}
	}
	
	public void setReadTimeout(int timeoutMillis) {
		conn.setReadTimeout(timeoutMillis);
	}
	
	public void setConnectTimeout(int timeoutMillis) {
		conn.setConnectTimeout(timeoutMillis);
	}
	
	public void setRequestMethod(String method) throws ProtocolException {
		conn.setRequestMethod(method);
	}
	
	public void setDoInput(boolean newValue) {
		conn.setDoInput(newValue);
	}
	
	private boolean checkSignal() {
		if(policy == SsService.SS_ALWAYS_READY_ACTION)
			return true;
		WifiManager wm = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
		return SsService.canTransfer(wm.getConnectionInfo().getRssi(), policy);
	}
	
	private void unregister() {
		if(receiver != null) {
			context.unregisterReceiver(receiver);
			receiver = null;
		}
	}
	
	public void connect() throws IOException {
		if(checkSignal()) {
			conn.connect();
			unregister();
			return;
		}
		else {
			try {
				Thread.sleep(max_delay_sec * 1000);
			} catch(InterruptedException e) {
				conn.connect();
				unregister();
				return;
			}
		}
		conn.connect();
		unregister();
		return;
	}
	
	public int getResponseCode() throws IOException {
		return conn.getResponseCode();
	}
	
	public InputStream getInputStream() throws IOException {
		return conn.getInputStream();
	}
	
	public void disconnect() {
		conn.disconnect();
	}

	public BroadcastReceiver ssReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			String outputline = "ss broadcast received";
			Log.i(LOG_TAG, "Ss broadcast received");
			Toast.makeText(context, outputline, Toast.LENGTH_SHORT).show();
		}
	};
	
	public class MyBroadcastReceiver extends BroadcastReceiver {
		Thread thread;
		public MyBroadcastReceiver(Thread thread) {
			this.thread = thread;
		}
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			Bundle bundle = arg1.getExtras();
			String info = bundle.getString("info");
			//String outputline = "ss broadcast received";
			Log.i(LOG_TAG, "Ss broadcast received");
			thread.interrupt();
			//TextView statusView = (TextView) context.findViewById(R.id.status);
			Toast.makeText(context, info, Toast.LENGTH_LONG).show();
		}	
	}
}
