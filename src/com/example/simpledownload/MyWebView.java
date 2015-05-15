package com.example.simpledownload;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.webkit.WebView;
import android.widget.Toast;

public class MyWebView {
	String LOG_TAG = "MyWebView";
	Context context;
	WebView webView;
	String policy;
	int maxDelay;
	
	public MyWebView(Context context, WebView webView, String policy, int maxDelay) {
		this.context = context;
		this.webView = webView;
		this.policy = policy;
		this.maxDelay = maxDelay;
		init();
	}
	
	private void init() {
		if(policy != SsService.SS_ALWAYS_READY_ACTION) {
			MyBroadcastReceiver receiver = new MyBroadcastReceiver(Thread.currentThread());
			//context.registerReceiver(this.ssReceiver, 
			context.registerReceiver(receiver,
				new IntentFilter(policy));
			Log.i(LOG_TAG, "ssReceiver registered");
		}
	}
	
	private boolean checkSignal() {
		if(policy == SsService.SS_ALWAYS_READY_ACTION)
			return true;
		WifiManager wm = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
		return SsService.canTransfer(wm.getConnectionInfo().getRssi(), policy);
	}
	
	public void loadUrl(String url) {
		if(checkSignal()) {
			webView.loadUrl(url);
			return;
		}
		else {
			try {
				Thread.sleep(maxDelay * 1000);
			} catch(InterruptedException e) {
				webView.loadUrl(url);
				return;
			}
			// sleep
			webView.loadUrl(url);
			return;
		}
	}

	public class MyBroadcastReceiver extends BroadcastReceiver {
		Thread thread;
		public MyBroadcastReceiver(Thread thread) {
			this.thread = thread;
		}
		@Override
		public void onReceive(Context arg0, Intent arg1) {
			// TODO Auto-generated method stub
			String outputline = "ss broadcast received";
			Log.i(LOG_TAG, "Ss broadcast received");
			thread.interrupt();
			Toast.makeText(context, outputline, Toast.LENGTH_SHORT).show();
		}	
	}
}
