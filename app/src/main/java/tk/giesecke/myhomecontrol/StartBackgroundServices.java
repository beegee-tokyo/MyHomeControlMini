package tk.giesecke.myhomecontrol;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

public class StartBackgroundServices extends Service {
	public StartBackgroundServices() {
	}

	@Override
	public IBinder onBind(Intent intent) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public void onCreate() {
		super.onCreate();

		/** Context of application */
		Context intentContext = getApplicationContext();

		/** IntentFilter to receive screen on/off broadcast msgs */
		IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
		filter.addAction(Intent.ACTION_SCREEN_OFF);
		filter.addAction(android.net.ConnectivityManager.CONNECTIVITY_ACTION);
		/** Receiver for screen on/off broadcast msgs */
		BroadcastReceiver mReceiver = new EventReceiver();
		registerReceiver(mReceiver, filter);

		// Start service to listen to UDP broadcast messages
		intentContext.startService(new Intent(intentContext, UDPlistener.class));

		// Start frequent updates for solar panel widgets and notifications
		Utilities.startStopUpdates(intentContext, true);
	}
}
