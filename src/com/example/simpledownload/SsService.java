package com.example.simpledownload;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

public class SsService extends Service {
	String LOG_TAG = "SsService";
	Context context;
	int activeInterface;
	
	public static final int NUM_LEVELS = 5;
	
	public static final int POWERSAVING_THRESHOLD = 4;
	public static final int MODERATE_THRESHOLD = 3;
	public static final int PERFORMANCE_THRESHOLD = 2;
	
	public static final String SS_POWERSAVING_READY_ACTION = "com.example.ssservice.SS_POWERSAVING_READY";
	public static final String SS_PERFORMANCE_READY_ACTION = "com.example.ssservice.SS_PERFORMANCE_READY";
	public static final String SS_MODERATE_READY_ACTION = "com.example.ssservice.SS_MODERATE_READY";
	public static final String SS_SSCHANGED_READY_ACTION = "com.example.ssservice.SS_CHANGED";
	public static final String SS_ALWAYS_READY_ACTION = "com.example.ssservice.SS_ALWAYS_READY";
	
	private static int currentWifiSignalLv = -1;
	private static int currentCellularSignalLv = -1;
	
	public static int calculateWifiSignalLevel(int rssi) {
		return WifiManager.calculateSignalLevel(rssi, NUM_LEVELS);
	}
	
	public static int calculateCellularSignalLevel(int rssi) {
		int MIN_RSSI = -110;
		int MAX_RSSI = -60;
		int numLevels = NUM_LEVELS;
		if(rssi <= MIN_RSSI) {
			return 0;
		}
		else if(rssi >= MAX_RSSI) {
			return numLevels - 1;
		}
		else {
			float inputRange = (MAX_RSSI - MIN_RSSI);
			float outputRange = (numLevels - 1);
			return (int)((float)(rssi - MIN_RSSI)*outputRange/inputRange);
		}
	}
	
	public SsService() {
		// TODO Auto-generated constructor stub
	}
	
	public int getActiveInterface() {
		ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
		NetworkInfo netinfo = cm.getActiveNetworkInfo();
		String outputline = "";
		if (netinfo != null) {
			if(netinfo.isConnected()){
				outputline = "active: " + netinfo.getTypeName() + " connected";
				Toast.makeText(context, outputline, Toast.LENGTH_LONG).show();
				return netinfo.getType();
			}
			else {
				for(NetworkInfo ni :cm.getAllNetworkInfo()) {
					if(ni.isConnected()) {
						outputline += netinfo.getTypeName() + " connected";
						Toast.makeText(context, outputline, Toast.LENGTH_LONG).show();
						return ni.getType();
					}
				}
			}
		}
		return ConnectivityManager.TYPE_DUMMY;
	}
	
	public static boolean canTransfer(int rssi, String action) {
		return false;
		/*int signalLv = calculateWifiSignalLevel(rssi);
		if(action == SS_POWERSAVING_READY_ACTION && signalLv >= POWERSAVING_THRESHOLD) {
			return true;
		}
		else if(action == SS_MODERATE_READY_ACTION && signalLv >= MODERATE_THRESHOLD) {
			return true;
		}
		else if(action == SS_PERFORMANCE_READY_ACTION && signalLv >= PERFORMANCE_THRESHOLD) {
			return true;
		}
		return false;*/
	}
	
	public class PollingThread extends Thread {
		public void run() {
			/*while(true) {
				try {
					Thread.sleep(3000);
					Toast.makeText(context, "Wake up", Toast.LENGTH_SHORT).show();
					Log.i(LOG_TAG, "wake up");
				} catch(InterruptedException e) {
				}
			}*/
		}
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		// Listen to Cellular signal strength
		Log.i(LOG_TAG, "enter onStartCommand");
		context = getApplicationContext();
		activeInterface = getActiveInterface();
		TelephonyManager tm = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		tm.listen(new MyPSListener(), PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
		// Listen to WiFi signal strength
		//WifiManager wm = (WifiManager) getSystemService(WIFI_SERVICE);
		this.registerReceiver(this.myWifiReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
		PollingThread pt = new PollingThread();
		pt.run();
		return START_NOT_STICKY;
	}
	
	private class MyPSListener extends PhoneStateListener {
		int prevRssi = -1;
		@Override
		public void onSignalStrengthsChanged(SignalStrength ss) {
			int newRssi = 2* ss.getGsmSignalStrength() - 113;
			//Toast.makeText(context, "New cellular signal: " +  ss_int, Toast.LENGTH_SHORT).show();
			SsService.currentCellularSignalLv = newRssi;
			broadcastCellularRssi(newRssi);
			prevRssi = newRssi;
		}
		
		private void broadcastCellularRssi(int newRssi) {
			int prevLv = SsService.calculateCellularSignalLevel(prevRssi);
			int newLv = SsService.calculateCellularSignalLevel(newRssi);
			if(newLv != prevLv) {
				String outputline = "Cellular signal "+prevRssi+" ["+prevLv+"] -> "+newRssi+" ["+newLv+"]";
				//Toast.makeText(context, outputline, Toast.LENGTH_SHORT).show();
				Intent itt = new Intent();
				itt.setAction(SS_SSCHANGED_READY_ACTION);
				itt.putExtra("info", "Signal change signal " + newRssi + " dBm");
				context.sendBroadcast(itt);
				Log.i(LOG_TAG, "Cellular broadcast sent");
				if(newLv >= POWERSAVING_THRESHOLD && prevLv <= POWERSAVING_THRESHOLD) {
					Intent intent = new Intent();
					intent.setAction(SS_POWERSAVING_READY_ACTION);
					intent.putExtra("info", "Powersaving ready signal " + newRssi + " dBm");
					context.sendBroadcast(intent);
				}
				if(newLv >= MODERATE_THRESHOLD && prevLv <= MODERATE_THRESHOLD) {
					Intent intent = new Intent();
					intent.setAction(SS_MODERATE_READY_ACTION);
					intent.putExtra("info", "Moderate ready signal " + newRssi + " dBm");
					context.sendBroadcast(intent);
				}
				if(newLv >= PERFORMANCE_THRESHOLD && prevLv <= PERFORMANCE_THRESHOLD) {
					Intent intent = new Intent();
					intent.setAction(SS_PERFORMANCE_READY_ACTION);
					intent.putExtra("info", "Performance ready signal " + newRssi + " dBm");
					context.sendBroadcast(intent);
				}
			}
		}
	}

	private BroadcastReceiver myWifiReceiver = new BroadcastReceiver() {
		int prevRssi = -1;
		int numLevels = 5;
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			int newRssi = arg1.getIntExtra(WifiManager.EXTRA_NEW_RSSI, 0);
			SsService.currentWifiSignalLv = newRssi;
			broadcastWifiRssi(newRssi);
			prevRssi = newRssi;
		}
		
		private void broadcastWifiRssi(int newRssi) {
			int prevLv = WifiManager.calculateSignalLevel(prevRssi, numLevels);
			int newLv = WifiManager.calculateSignalLevel(newRssi, numLevels);
			if(newLv != prevLv) {
				String outputline = "WiFi signal "+prevRssi+" ["+prevLv+"] -> "+newRssi+" ["+newLv+"]";
				//Toast.makeText(context, outputline, Toast.LENGTH_SHORT).show();
				Intent itt = new Intent();
				itt.setAction(SS_SSCHANGED_READY_ACTION);
				context.sendBroadcast(itt);
				itt.putExtra("info", "Signal change signal " + newRssi + " dBm");
				Log.i(LOG_TAG, "WiFi broadcast sent");
				if(newLv >= POWERSAVING_THRESHOLD && prevLv <= POWERSAVING_THRESHOLD) {
					Intent intent = new Intent();
					intent.setAction(SS_POWERSAVING_READY_ACTION);
					intent.putExtra("info", "Powersaving ready signal " + newRssi + " dBm");
					context.sendBroadcast(intent);
				}
				if(newLv >= MODERATE_THRESHOLD && prevLv <= MODERATE_THRESHOLD) {
					Intent intent = new Intent();
					intent.setAction(SS_MODERATE_READY_ACTION);
					intent.putExtra("info", "Moderate ready signal " + newRssi + " dBm");
					context.sendBroadcast(intent);
				}
				if(newLv >= PERFORMANCE_THRESHOLD && prevLv <= PERFORMANCE_THRESHOLD) {
					Intent intent = new Intent();
					intent.setAction(SS_PERFORMANCE_READY_ACTION);
					intent.putExtra("info", "Performance ready signal " + newRssi + " dBm");
					context.sendBroadcast(intent);
				}
			}
		}
	};
	
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return null;
	}

}
