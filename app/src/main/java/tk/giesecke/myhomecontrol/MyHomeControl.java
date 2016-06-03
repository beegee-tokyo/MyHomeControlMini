package tk.giesecke.myhomecontrol;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MyHomeControl extends Activity implements View.OnClickListener
		, AdapterView.OnItemClickListener {

	/** Debug tag */
	static final String DEBUG_LOG_TAG = "MHC-MAIN";

	/** Access to activities shared preferences */
	static SharedPreferences mPrefs;
	/* Name of shared preferences */
	public static final String sharedPrefName = "MyHomeControl";
	/** Context of this application */
	static Context appContext;
	/** Id of menu, needed to set user selected icons and device names */
	private Menu abMenu;
	/** Visible view 0 = security, 1 = solar panel, 2 = aircon */
	private int visibleView = 0;
	/** Flag for debug output */
	private boolean showDebug = false;
	/** Timer for updating content of main UI */
	private Timer autoUpdate;

	/** Shared preferences value for last shown view */
	private final String prefsLastView = "lastView";
	/** Shared preferences value for show debug messages flag */
	private final String prefsShowDebug = "showDebug";

	/** Shared preferences value security alarm sound */
	public static final String prefsSecurityAlarm = "secAlarm";
	/** Shared preferences value security alarm sound */
	public static final String prefsSecurityAlarmOn = "secAlarmOn";

	/** Shared preferences value for solar alarm sound */
	public static final String prefsSolarWarning = "solarAlarm";
	/** Shared preferences value for number solar widgets placed */
	public static final String prefsSolarWidgetNum = "solarWidgetNum";
	/** Shared preferences value for large widget */
	public static final String prefsSolarWidgetSize = "solarWidgetSizeLarge";

	/** Shared preferences value for last selected device */
	private final String prefsSelDevice = "airconSelDevice";
	/** Shared preferences value for show debug messages flag */
	private final String prefsLocationName = "airconLocation";
	/** Shared preferences value for show debug messages flag */
	private final String prefsDeviceIcon = "airconIcon";

	/** Flag for Initialization activity */
	private static boolean initializeIsRunning = false;
	/** Flag for initialization of security front running */
	private static boolean initSec1IsOn = false;
	/** Flag for initialization of security back running */
	private static boolean initSec2IsOn = false;
	/** Flag for initialization of solar panel running */
	private static boolean initSolIsOn = false;
	/** Flag for initialization of aircon 1 running */
	private static boolean initAir1IsOn = false;
	/** Flag for initialization of aircon 2 running */
	private static boolean initAir2IsOn = false;
	/** Flag for initialization of aircon 3 running */
	private static boolean initAir3IsOn = false;

	/** View for selecting device to change icon and device name */
	private View locationSettingsView;
	/** View of aircon device name and icon change dialog */
	private View airconDialogView;
	/** Button ids from location selection dialog */
	private final int[] buttonIds = {
			R.id.dia_sel_device0,
			R.id.dia_sel_device1,
			R.id.dia_sel_device2};
	/** Resource ids of drawables for the icons */
	private final int[] iconIDs = {R.drawable.ic_bathroom,
			R.drawable.ic_bedroom,
			R.drawable.ic_dining,
			R.drawable.ic_entertainment,
			R.drawable.ic_kids,
			R.drawable.ic_kitchen,
			R.drawable.ic_livingroom,
			R.drawable.ic_office};
	/* Resource ids of the icon buttons */
	private final int[] iconButtons = {R.id.im_bath,
			R.id.im_bed,
			R.id.im_dining,
			R.id.im_entertain,
			R.id.im_kids,
			R.id.im_kitchen,
			R.id.im_living,
			R.id.im_office};
	/** Index of device handled in dialog box */
	private int dlgDeviceIndex;
	/** R.id of selected icon for a device */
	private int dlgIconIndex;

	/** Flag for sound selector (true = security alarm, false = solar panel warning) */
	private boolean isSelAlarm = true;

	/** List of potential control device availability */
	// SPmonitor, Security front, Aircon 1, Aircon 2, Aircon 3, Security back
	static final boolean[] deviceIsOn = {false, false, false, false, false, false};
	/** deviceIsOn index for SPmonitor */
	static final int spMonitorIndex = 0;
	/** deviceIsOn index for Security front */
	static final int secFrontIndex = 1;
	/** deviceIsOn index for Aircon 1 */
	static final int aircon1Index = 2;
	/** deviceIsOn index for Aircon 2 */
	static final int aircon2Index = 3;
	/** deviceIsOn index for Aircon 3 */
	static final int aircon3Index = 4;
	/** deviceIsOn index for Security back */
	static final int secRearIndex = 5;

	/** URL to spMonitor ESP8266*/
	static String SOLAR_URL;
	/** URL to Front Security ESP8266*/
	public static String SECURITY_URL_FRONT_1;
	/** URL to Back Security ESP8266*/
	static String SECURITY_URL_BACK_1;
	/** URL to FujiDenzo aircon ESP8266*/
	static String AIRCON_URL_1;
	/** URL to Carrier aircon ESP8266*/
	static String AIRCON_URL_2;
	/** URL to another aircon ESP8266*/
	static String AIRCON_URL_3;

	// Security view related
	/** Security view */
	private RelativeLayout secView = null;
	/** TextView for status message display in security view */
	private static TextView secStatus;
	/** ImageView to show status of alarm enabled front sensor */
	static ImageView ivAlarmStatus;
	/** ImageView to show status of light enabled front sensor */
	static ImageView ivLightStatus;
	/** ImageView to show active alarm front sensor */
	static ImageView ivAlarmOn;
	/** Animator to make blinking active alarm ImageView front sensor */
	static ValueAnimator animator;
	/** ImageView to show status of alarm enabled rear sensor */
	static ImageView ivAlarmStatusBack;
	/** ImageView to show status of light enabled rear sensor */
	static ImageView ivLightStatusBack;
	/** ImageView to show active alarm rear sensor */
	static ImageView ivAlarmOnBack;
	/** Animator to make blinking active alarm ImageView rear sensor */
	static ValueAnimator animatorBack;
	/** Check box for auto activation of alarm */
	static CheckBox secAutoAlarm;
	/** Clickable text view to change activation times */
	static TextView secChangeAlarm;

	/** Status flag for alarm front sensor */
	static boolean hasAlarmOnFront = true;
	/** Status flag for alarm back sensor */
	static boolean hasAlarmOnBack = true;

	/** Auto activation on time as string */
	static String secAutoOn;
	/** Auto activation off time as string */
	static String secAutoOff;
	/** Auto activation on time as integer */
	static int secAutoOnStored;
	/** Auto activation off time as integer */
	static int secAutoOffStored;

	/** Array list with available alarm names */
	private ArrayList<String> notifNames = new ArrayList<>();
	/** Array list with available alarm uri's */
	private ArrayList<String> notifUri = new ArrayList<>();
	/** Selected alarm name */
	private String notifNameSel = "";
	/** Selected alarm uri */
	private String notifUriSel = "";

	// Solar monitor view related
	/** Solar panel view */
	private RelativeLayout solView = null;
	/** TextView for status message display in solar panel view */
	private static TextView solStatus;
	/** Solar power received from spMonitor device as minute average */
	private static Float solarPowerMin = 0.0f;
	/** Solar power received from spMonitor device as minute average */
	private static Float lastSolarPowerMin = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float consPowerMin = 0.0f;
	/** Consumption received from spMonitor device as minute average */
	private static Float lastConsPowerMin = 0.0f;

	// Aircon view related
	/** Aircon control view */
	private RelativeLayout airView = null;
	/** FujiDenzo control view */
	private RelativeLayout airFDView = null;
	/** Carrier control view */
	private RelativeLayout airCAView = null;
	/** TextView for status message display in aircon view */
	private static TextView airStatus;
	/** Light of button to switch consumption control for FujiDenzo layout */
	private static View btAutoLightFD;
	/** Light of button to switch consumption control for Carrier layout */
	private static View btAutoLightCA;
	/** Light of button to switch on/off for FujiDenzo layout */
	private static View btOnOffLightFD;
	/** Light of button to switch on/off for Carrier layout */
	private static View btOnOffLightCA;
	/** Light of button to switch fan to high speed for FujiDenzo layout */
	private static View btFanHighLightFD;
	/** Light of button to switch fan to medium speed for FujiDenzo layout */
	private static View btFanMedLightFD;
	/** Light of button to switch fan to low speed for FujiDenzo layout */
	private static View btFanLowLightFD;
	/** Light of button to switch to cool mode for FujiDenzo layout */
	private static View btCoolLightFD;
	/** Light of button to switch to cool mode for Carrier layout */
	private static View btCoolLightCA;
	/** Light of button to switch to dry mode for FujiDenzo layout */
	private static View btDryLightFD;
	/** Light of button to switch to dry mode for Carrier layout */
	private static View btDryLightCA;
	/** Light of button to switch to fan mode for FujiDenzo layout */
	private static View btFanLightFD;
	/** Light of button to switch to fan mode for Carrier layout */
	private static View btFanLightCA;
	/** Light of button to switch on sweep for Carrier layout */
	private static View btSweepLightCA;
	/** Light of button to switch on turbo mode for Carrier layout */
	private static View btTurboLightCA;
	/** Light of button to switch on ion mode for Carrier layout */
	private static View btIonLightCA;
	/** Light of button to switch on auto temp function for Carrier layout */
	private static View btAutomLightCA;
	/** Light of button to switch to timer function for FujiDenzo layout */
	/* Timer function is not supported atm
	View static btTimerLightFD;
	*/
	/** Light of button to switch to timer function for Carrier layout */
	/* Timer function is not supported atm
	View static btTimerLightCA;
	*/
	/** Button to switch fan speed for Carrier layout */
	private static Button btFanCA;
	/** Consumption value display for FujiDenzo layout */
	private static TextView txtConsValFD;
	/** Temperature value display for FujiDenzo layout */
	private static TextView txtTempValFD;
	/** Status value display for FujiDenzo layout */
	private static TextView txtAutoStatusValFD;
	/** Consumption value display for Carrier layout */
	private static TextView txtConsValCA;
	/** Temperature value display for Carrier layout */
	private static TextView txtTempValCA;
	/** Status value display for Carrier layout */
	private static TextView txtAutoStatusValCA;

	/** Color for activated button */
	private static int colorRed;
	/** Color for deactivated button */
	private static int colorGrey;

	/** ID of the selected device */
	private static int selDevice = 0;
	/** IP address of the selected device */
	private final String[] espIP = {"192.168.0.142",
			"192.168.0.143",
			"192.168.0.144",
			"192.168.0.145",
			"192.168.0.146",
			"192.168.0.147",
			"192.168.0.148",
			"192.168.0.149"};
	/** Location of the device */
	/** Name of the device */
	private static final String[] deviceName = {"", "", "", "", "", "", "", ""};
	/** Layout version for the device */
	private static final int[] deviceType = {99, 99, 99, 99, 99, 99, 99, 99};
	/** Valid device type ids */
	private static final int FUJIDENZO = 0;
	private static final int CARRIER = 1;
	private static final int OTHER_AIRCON = 2;
	/** Location of the device */
	private final String[] locationName = {"Office", "Living", "Bedroom", "", "", "", "", ""};
	/** Icon for the device */
	private final int[] deviceIcon = {7, 6, 1, 0, 0, 0, 0, 0};
	/** Fan speed of device */
	private static final int[] fanStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Mode status of device */
	private static final int[] modeStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Power status of device */
	private static final int[] powerStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Cooling temperature of device */
	private static final int[] coolStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Consumption status of device (only from master device */
	private static double consStatus = 0;
	/** Auto power status of device (only from master device */
	private static int autoStatus = 0;
	/** Auto power enabled status of device */
	private static final int[] autoOnStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Sweep enabled status of device */
	private static final int[] sweepStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Turbo enabled status of device */
	private static final int[] turboStatus = {0, 0, 0, 0, 0, 0, 0, 0};
	/** Ion enabled status of device */
	private static final int[] ionStatus = {0, 0, 0, 0, 0, 0, 0, 0};

	private static final String CMD_ON_OFF = "00";

	private static final String CMD_MODE_AUTO = "10";
	private static final String CMD_MODE_COOL = "11";
	private static final String CMD_MODE_DRY = "12";
	private static final String CMD_MODE_FAN = "13";

	private static final String CMD_FAN_HIGH = "20";
	private static final String CMD_FAN_MED = "21";
	private static final String CMD_FAN_LOW = "22";
	private static final String CMD_FAN_SPEED = "23";

	private static final String CMD_TEMP_PLUS = "30";
	private static final String CMD_TEMP_MINUS = "31";

	/* Timer function not supported atm
	static final String CMD_OTHER_TIMER = "40";
	 */
	private static final String CMD_OTHER_SWEEP = "41";
	private static final String CMD_OTHER_TURBO = "42";
	private static final String CMD_OTHER_ION = "43";

	private static final String CMD_AUTO_ON = "98";
	private static final String CMD_AUTO_OFF = "99";

	/** Communication progress bar view */
	private static RelativeLayout comView = null;

	@Override
	@SuppressWarnings("deprecation")
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_my_home_control);

		// Get pointer to shared preferences
		mPrefs = getSharedPreferences(sharedPrefName,0);

		// Get context of the application to be reused in Async Tasks
		appContext = this;

		// Register the receiver for messages from UDP & GCM listener
		// Create an intent filter to listen to the broadcast sent with the action "ACTION_STRING_ACTIVITY"
		IntentFilter intentFilter = new IntentFilter(UDPlistener.BROADCAST_RECEIVED);
		//Map the intent filter to the receiver
		registerReceiver(activityReceiver, intentFilter);

		/******************************************************************/
		/* Next values must be changed to your GCM project ID             */
		/* and the URL or IP address of your ESP8266 device               */
		/******************************************************************/
		// Get project ID and ESP8266 URL
		SOLAR_URL = this.getResources().getString(R.string.SOLAR_URL); // = "http://192.168.xxx.xx0";
		SECURITY_URL_FRONT_1 = this.getResources().getString(R.string.SECURITY_URL_FRONT_1); // = "http://192.168.xxx.xx1";
		SECURITY_URL_BACK_1 = this.getResources().getString(R.string.SECURITY_URL_BACK_1); // = "http://192.168.xxx.xx4";
		AIRCON_URL_1 = this.getResources().getString(R.string.AIRCON_URL_1); // = "http://192.168.xxx.xx2";
		AIRCON_URL_2 = this.getResources().getString(R.string.AIRCON_URL_2); // = "http://192.168.xxx.xx3";
		AIRCON_URL_3 = this.getResources().getString(R.string.AIRCON_URL_3); // = "http://192.168.xxx.xx5";

		// Enable access to internet
		if (Build.VERSION.SDK_INT > 9) {
			/** ThreadPolicy to get permission to access internet */
			StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
			StrictMode.setThreadPolicy(policy);
		}

		// Initialize variables for buttons, layouts, views, ...
		setGlobalVar();

		if (Utilities.isHomeWiFi(this)) {
			secStatus.setText(getResources().getString(R.string.at_home));
			airStatus.setText(getResources().getString(R.string.at_home));
			solStatus.setText(getResources().getString(R.string.at_home));
		} else {
			secStatus.setText(getResources().getString(R.string.not_home));
			airStatus.setText(getResources().getString(R.string.not_home));
			solStatus.setText(getResources().getString(R.string.not_home));
		}

		// Get index of last selected device */
		selDevice = mPrefs.getInt(prefsSelDevice, 0);

		// Set visible view flag to security
		visibleView = mPrefs.getInt(prefsLastView, 0);
		// Set flag for debug output
		showDebug = mPrefs.getBoolean(prefsShowDebug, false);
	}

	/**
	 * Called when activity is getting visible
	 */
	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		// Get the layouts of all three possible views
		secView = (RelativeLayout) findViewById(R.id.view_security);
		solView = (RelativeLayout) findViewById(R.id.view_solar);
		airView = (RelativeLayout) findViewById(R.id.view_aircon);
		// Get layout for the communication animation
		comView = (RelativeLayout) findViewById(R.id.com_progress);

		// Setup views
		/** Menu item pointer */
		MenuItem menuItem;
		switch (visibleView) {
			case 0: // Security
				if (abMenu != null) {
					menuItem = abMenu.getItem(0); // Alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(1); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(2); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.VISIBLE);
				break;
			case 1: // Solar panel
				if (abMenu != null) {
					menuItem = abMenu.getItem(0); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(1); // Solar alarm sound menu entry
					menuItem.setVisible(true);
					menuItem = abMenu.getItem(2); // Aircon location menu entry
					menuItem.setVisible(false);
				}
				airView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.VISIBLE);
				break;
			case 2: // Aircon
				if (abMenu != null) {
					menuItem = abMenu.getItem(0); // Alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(1); // Solar alarm sound menu entry
					menuItem.setVisible(false);
					menuItem = abMenu.getItem(2); // Aircon location menu entry
					menuItem.setVisible(true);
				}
				solView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.VISIBLE);
				break;
		}

		// Setup aircon views
		switch (selDevice) {
			case 0:
				airCAView.setVisibility(View.INVISIBLE);
				airFDView.setVisibility(View.VISIBLE);
				break;
			case 1:
				airFDView.setVisibility(View.INVISIBLE);
				airCAView.setVisibility(View.VISIBLE);
				break;
		}

		if (Utilities.isHomeWiFi(this)) {
			// Start initialization
			initializeIsRunning = true;
			new Initialize().execute();
			// Start display update every 1 minutes
			autoUpdate = new Timer();
			autoUpdate.schedule(new TimerTask() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							comView.setVisibility(View.VISIBLE);
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "UI update triggered");
							// Update solar UI
							new ESPcommunication()
									.execute(SOLAR_URL,
											"/data/get",
											"",
											"spm",
											"0");
							if (Utilities.isHomeWiFi(appContext)) {
								if (deviceIsOn[secFrontIndex]) {
									// Update security UI
									new ESPcommunication()
											.execute(SECURITY_URL_FRONT_1,
													"/?s",
													"",
													"sec",
													Integer.toString(selDevice));
								}
								if (deviceIsOn[secRearIndex]) {
									// Update security UI
									new ESPcommunication()
											.execute(SECURITY_URL_BACK_1,
													"/?s",
													"",
													"sec",
													Integer.toString(selDevice));
								}
								if (deviceIsOn[aircon1Index]) {
									// Update aircon 1 UI
									new ESPcommunication()
											.execute(AIRCON_URL_1,
													"/?s",
													"",
													"air",
													"0");
								}
								if (deviceIsOn[aircon2Index]) {
									// Update aircon 2 UI
									new ESPcommunication()
											.execute(AIRCON_URL_2,
													"/?s",
													"",
													"air",
													"0");
								}
								if (deviceIsOn[aircon3Index]) {
									// Update aircon 3 UI
									new ESPcommunication()
											.execute(AIRCON_URL_3,
													"/?s",
													"",
													"air",
													"0");
								}
							}
						}
					});
				}
			}, 1000, 60 * 1000); // updates every 1 minute
		} else {
			Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_home), Toast.LENGTH_LONG).show();
			new Initialize().execute();
			// Start display update every 10 minutes
			autoUpdate = new Timer();
			autoUpdate.schedule(new TimerTask() {
				@Override
				public void run() {
					runOnUiThread(new Runnable() {
						public void run() {
							comView.setVisibility(View.VISIBLE);
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Solar UI triggered");
							new ESPcommunication()
									.execute(SOLAR_URL, "/data/get", "", "spm", "0");
						}
					});
				}
			}, 1000, 10 * 60 * 1000); // updates every 10 minute
		}
		if (!isMyServiceRunning(UDPlistener.class)) {
			// Start background services
			startService(new Intent(this, StartBackgroundServices.class));
		}
	}

	/**
	 * Called when activity is getting invisible
	 */
	@Override
	protected void onPause() {
		super.onPause();
		// Update security widgets
		/** App widget manager for all widgets of this app */
		AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
		/** Component name of this widget */
		ComponentName thisAppWidget = new ComponentName(appContext.getPackageName(),
				SecurityWidget.class.getName());
		/** List of all active widgets */
		int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

		for (int appWidgetId : appWidgetIds) {
			SecurityWidget.updateAppWidget(appContext,appWidgetManager,appWidgetId, false);
		}

		// Stop auto updates of solar panel data
		try {
			autoUpdate.cancel();
		} catch (Exception ignore) {
		}
		animator.end();
		ivAlarmOn.setAlpha(0f);
		animatorBack.end();
		ivAlarmOnBack.setAlpha(0f);
	}

	/**
	 * Called when activity is getting destroyed
	 * Handles security fragment specific tasks
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		// Unregister the receiver for messages from UDP listener
		unregisterReceiver(activityReceiver);
		activityReceiver = null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_my_home_control, menu);
		abMenu = menu;

		MenuItem menuItem = abMenu.getItem(4); // Debug menu entry
		if (showDebug) {
			menuItem.setTitle(R.string.action_debug_off);
		} else {
			menuItem.setTitle(R.string.action_debug);
		}
		return true;
	}

	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/** Menu item pointer */
		MenuItem menuItem;

		switch (item.getItemId()) {
			case R.id.action_selWarning:
			case R.id.action_selAlarm:
				isSelAlarm = item.getItemId() != R.id.action_selWarning;
				notifNames = new ArrayList<>();
				notifUri = new ArrayList<>();
				notifNames.add(getString(R.string.no_alarm_sel));
				notifUri.add("");
				notifNames.add(getString(R.string.snd_alarm));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alarm);
				notifNames.add(getString(R.string.snd_alert));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.alert);
				notifNames.add(getString(R.string.snd_dog));
				notifUri.add("android.resource://"
						+ this.getPackageName() + "/"
						+ R.raw.dog);
				/** Index of last user selected alarm tone */
				int uriIndex = Utilities.getNotifSounds(this, notifNames, notifUri, isSelAlarm) + 2;

				// get sound_selector.xml view
				/** Layout inflater for sound selection dialog */
				LayoutInflater alarmDialogInflater = LayoutInflater.from(this);
				/** View for sound selection dialog */
				@SuppressLint("InflateParams")
				View alarmSettingsView = alarmDialogInflater.inflate(R.layout.sound_selector, null);
				/** Alert dialog builder for device selection dialog */
				AlertDialog.Builder alarmDialogBuilder = new AlertDialog.Builder(this);

				// set sound_selector.xml to alert dialog builder
				alarmDialogBuilder.setView(alarmSettingsView);

				// set dialog message
				alarmDialogBuilder
						.setTitle(getResources().getString(R.string.action_selAlarm))
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										if (!notifNameSel.equalsIgnoreCase("")) {
											if (isSelAlarm) {
												mPrefs.edit().putString(prefsSecurityAlarm, notifUriSel).apply();
											} else {
												mPrefs.edit().putString(prefsSolarWarning, notifUriSel).apply();
											}
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/** Alert dialog  for device selection */
				AlertDialog alarmDialog = alarmDialogBuilder.create();

				// show it
				alarmDialog.show();

				/** Pointer to list view with the alarms */
				ListView lvAlarmList = (ListView) alarmSettingsView.findViewById(R.id.lv_AlarmList);
				/** Array adapter for the ListView */
				final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(
						appContext,
						R.layout.my_list_item,
						notifNames );
				lvAlarmList.setAdapter(arrayAdapter);
				// Use long click listener to play the alarm sound
				lvAlarmList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
					public boolean onItemLongClick(AdapterView<?> arg0, View arg1,
												   int pos, long id) {
						/** Instance of media player */
						MediaPlayer mMediaPlayer = new MediaPlayer();
						try {
							mMediaPlayer.setDataSource(appContext, Uri.parse(notifUri.get(pos)));
							/** Audio manager to play the sound */
							final AudioManager audioManager = (AudioManager) appContext
									.getSystemService(Context.AUDIO_SERVICE);
							if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
								mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
								mMediaPlayer.prepare();
								mMediaPlayer.start();
							}
						} catch (IOException e) {
							if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Cannot play alarm");
						}
						return true;
					}
				});
				lvAlarmList.setOnItemClickListener(this);
				lvAlarmList.setItemChecked(uriIndex, true);
				lvAlarmList.setSelection(uriIndex);
				break;
			case R.id.action_refresh:
				if (Utilities.isHomeWiFi(this)) {
					new Initialize().execute();
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_home), Toast.LENGTH_LONG).show();
				}
				break;
			case R.id.action_debug:
				showDebug = !showDebug;
				mPrefs.edit().putBoolean(prefsShowDebug,showDebug).apply();
				menuItem = abMenu.getItem(4); // Debug menu entry
				if (showDebug) {
					menuItem.setTitle(R.string.action_debug_off);
				} else {
					menuItem.setTitle(R.string.action_debug);
				}
				break;
			case R.id.action_locations:
				// get location_selector.xml view
				/** Layout inflater for device selection dialog */
				LayoutInflater locationDialogInflater = LayoutInflater.from(this);
				locationSettingsView = locationDialogInflater.inflate(R.layout.location_selector, null);
				/** Alert dialog builder for device selection dialog */
				AlertDialog.Builder locationDialogBuilder = new AlertDialog.Builder(this);

				// set location_selector.xml to alert dialog builder
				locationDialogBuilder.setView(locationSettingsView);

				/** Pointer to button, used to set OnClickListener for buttons in the dialog */
				Button btToSetOnClickListener;
				/** Pointer to button text, used to give each button in the dialog a specific name */
				String buttonTxt;

				for (int i = 0; i < 3; i++) {
					if (deviceIsOn[i+2]) {
						btToSetOnClickListener = (Button) locationSettingsView.findViewById(buttonIds[i]);
						btToSetOnClickListener.setVisibility(View.VISIBLE);
						if (locationName[i].equalsIgnoreCase("")) {
							btToSetOnClickListener.setText(deviceName[i]);
						} else {
							buttonTxt = locationName[i];
							btToSetOnClickListener.setText(buttonTxt);
						}
						btToSetOnClickListener.setOnClickListener(this);
					}
				}

				// set dialog message
				locationDialogBuilder
						.setTitle(getResources().getString(R.string.dialog_selector_title))
						.setCancelable(false)
						.setNegativeButton("OK",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										for (int i = 0; i < 3; i++) {
											mPrefs.edit().putString(
													prefsLocationName + Integer.toString(i),
													locationName[i]).apply();
											mPrefs.edit().putInt(
													prefsDeviceIcon + Integer.toString(i),
													deviceIcon[i]).apply();
										}
										dialog.cancel();
									}
								});

				// create alert dialog
				/** Alert dialog  for device selection */
				AlertDialog alertDialog = locationDialogBuilder.create();

				// show it
				alertDialog.show();

				break;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * Listener for clicks in ListView for alarm sound selection
	 *
	 * @param parent
	 *              AdapterView of alert dialog
	 * @param view
	 *              View of ListView
	 * @param position
	 *              Position in ListView that has been clicked
	 * @param id
	 *              ID of item in ListView that has been clicked
	 */
	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		notifNameSel = notifNames.get(position);
		notifUriSel = notifUri.get(position);
	}

	/**
	 * Called when a view has been clicked.
	 *
	 * @param v
	 * 		The view that was clicked.
	 */
	@SuppressLint("InflateParams")
	@SuppressWarnings("deprecation")
	@Override
	public void onClick(View v) {
		/** URL for communication with ESP */
		String url = "";
		/** Command for ESP */
		String cmd = "";
		/** Sender ID (sec, air or spm) */
		String id = "";
		/** Menu item pointer */
		MenuItem menuItem;
		switch (v.getId()) {
			case R.id.dot_alarm_status:
				if (hasAlarmOnFront) {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_off));
					url = SECURITY_URL_FRONT_1;
					cmd = "/?a=0";
					id = "sec";
				} else {
					ivAlarmStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_on));
					url = SECURITY_URL_FRONT_1;
					cmd = "/?a=1";
					id = "sec";
				}
				break;
			case R.id.dot_alarm_status_back:
				if (hasAlarmOnBack) {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_off));
					url = SECURITY_URL_BACK_1;
					cmd = "/?a=0";
					id = "sec";
				} else {
					ivAlarmStatusBack.setImageDrawable(getResources().getDrawable(R.mipmap.ic_sec_widget_on));
					url = SECURITY_URL_BACK_1;
					cmd = "/?a=1";
					id = "sec";
				}
				break;
			case R.id.dot_light:
				ivLightStatus.setImageDrawable(getResources().getDrawable(R.mipmap.ic_light_on));
				url = SECURITY_URL_FRONT_1;
				cmd = "/?b";
				id = "sec";
				break;
			case R.id.bt_auto_fd:
			case R.id.bt_auto_ca:
				if (autoOnStatus[selDevice] == 1) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_AUTO_OFF;
					id = "air";
				} else {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_AUTO_ON;
					id = "air";
				}
				break;
			case R.id.bt_on_off_fd:
			case R.id.bt_on_off_ca:
				url = "http://" + espIP[selDevice];
				cmd = "/?c=" + CMD_ON_OFF;
				id = "air";
				break;
			case R.id.bt_fan_high_fd:
				if (fanStatus[selDevice] != 2) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_FAN_HIGH;
					id = "air";
				}
				break;
			case R.id.bt_fan_med_fd:
				if (fanStatus[selDevice] != 1) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_FAN_MED;
					id = "air";
				}
				break;
			case R.id.bt_fan_low_fd:
				if (fanStatus[selDevice] != 0) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_FAN_LOW;
					id = "air";
				}
				break;
			case R.id.bt_autom_ca:
				if (modeStatus[selDevice] != 3) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_MODE_AUTO;
					id = "air";
				}
				break;
			case R.id.bt_cool_fd:
			case R.id.bt_cool_ca:
				if (modeStatus[selDevice] != 2) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_MODE_COOL;
					id = "air";
				}
				break;
			case R.id.bt_dry_fd:
			case R.id.bt_dry_ca:
				if (modeStatus[selDevice] != 1) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_MODE_DRY;
					id = "air";
				}
				break;
			case R.id.bt_fan_fd:
			case R.id.bt_fan_ca:
				if (modeStatus[selDevice] != 0) {
					url = "http://" + espIP[selDevice];
					cmd = "/?c=" + CMD_MODE_FAN;
					id = "air";
				}
				break;
			case R.id.bt_sweep_ca:
				url = "http://" + espIP[selDevice];
				cmd = "/?c=" + CMD_OTHER_SWEEP;
				id = "air";
				break;
			case R.id.bt_turbo_ca:
				url = "http://" + espIP[selDevice];
				cmd = "/?c=" + CMD_OTHER_TURBO;
				id = "air";
				break;
			case R.id.bt_ion_ca:
				url = "http://" + espIP[selDevice];
				cmd = "/?c=" + CMD_OTHER_ION;
				id = "air";
				break;
			case R.id.bt_plus_fd:
			case R.id.bt_plus_ca:
				url = "http://" + espIP[selDevice];
				cmd = "/?c=" + CMD_TEMP_PLUS;
				id = "air";
				break;
			case R.id.bt_minus_fd:
			case R.id.bt_minus_ca:
				url = "http://" + espIP[selDevice];
				cmd = "/?c=" + CMD_TEMP_MINUS;
				id = "air";
				break;
			case R.id.bt_fanspeed_ca:
				url = "http://" + espIP[selDevice] + "/?c=" + CMD_FAN_SPEED;
				break;
			case R.id.im_icon_fd:
				if (deviceIsOn[aircon2Index]) { // Is Carrier aircon online?
					airFDView.setVisibility(View.INVISIBLE);
					airCAView.setVisibility(View.VISIBLE);
					selDevice = 1;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				} else if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					// TODO switch to third aircon view
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Trying to switch to aircon 3");
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				}
				break;
			case R.id.im_icon_ca:
				if (deviceIsOn[aircon3Index]) { // Is other aircon online?
					// TODO switch to third aircon view
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Trying to switch to aircon 3");
					selDevice = 2;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				} else if (deviceIsOn[aircon1Index]) {
					airCAView.setVisibility(View.INVISIBLE);
					airFDView.setVisibility(View.VISIBLE);
					selDevice = 0;
					mPrefs.edit().putInt(prefsSelDevice,selDevice).apply();
				}
				break;
			case R.id.dia_sel_device0:
			case R.id.dia_sel_device1:
			case R.id.dia_sel_device2:
				switch (v.getId()) {
					case R.id.dia_sel_device0:
						dlgDeviceIndex = FUJIDENZO;
						break;
					case R.id.dia_sel_device1:
						dlgDeviceIndex = CARRIER;
						break;
					default:
						dlgDeviceIndex = OTHER_AIRCON;
						break;
				}
				// get location_selector.xml view
				/** Layout inflater for dialog to change device name and icon */
				LayoutInflater airconDialogInflater = LayoutInflater.from(this);
				/** View of aircon device name and icon change dialog */
				airconDialogView = airconDialogInflater.inflate(R.layout.locations, null);
				/** Alert dialog builder for dialog to change device name and icon */
				AlertDialog.Builder airconDialogBuilder = new AlertDialog.Builder(this);
				// set location_selector.xml to alert dialog builder
				airconDialogBuilder.setView(airconDialogView);

				/** Button to set onClickListener for icon buttons in the dialog */
				ImageButton btOnlyClickListener;

				for (int i = 0; i < 8; i++) {
					btOnlyClickListener = (ImageButton) airconDialogView.findViewById(iconButtons[i]);
					btOnlyClickListener.setOnClickListener(this);
				}

				dlgIconIndex = deviceIcon[dlgDeviceIndex];
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);

				/** Edit text field for the user selected device name */
				final EditText userInput = (EditText) airconDialogView.findViewById(R.id.dia_et_location);
				userInput.setText(locationName[dlgDeviceIndex]);

				// set dialog message
				airconDialogBuilder
						.setTitle(getResources().getString(R.string.dialog_change_title))
						.setCancelable(false)
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {
									@SuppressWarnings("deprecation")
									public void onClick(DialogInterface dialog, int id) {
										locationName[dlgDeviceIndex] = userInput.getText().toString();
										deviceIcon[dlgDeviceIndex] = dlgIconIndex;
										// Update underlying dialog box with new device name
										/** Button of selection dialog that we are processing */
										Button btToChangeName = (Button) locationSettingsView.findViewById(buttonIds[dlgDeviceIndex]);
										btToChangeName.setText(locationName[dlgDeviceIndex]);
										locationSettingsView.invalidate();
										// Update UI
										/** Text view to show location name */
										TextView locationText;
										/** Image view to show location icon */
										ImageView locationIcon;
										if (dlgDeviceIndex == FUJIDENZO) {
											locationText = (TextView) findViewById(R.id.txt_device_fd);
											locationText.setText(locationName[dlgDeviceIndex]);
											locationIcon = (ImageView) findViewById(R.id.im_icon_fd);
											locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
										} else if (dlgDeviceIndex == CARRIER) {
											locationText = (TextView) findViewById(R.id.txt_device_ca);
											locationText.setText(locationName[dlgDeviceIndex]);
											locationIcon = (ImageView) findViewById(R.id.im_icon_ca);
											locationIcon.setImageDrawable(getResources().getDrawable(iconIDs[deviceIcon[dlgDeviceIndex]]));
										} else if (dlgDeviceIndex == OTHER_AIRCON) {
											// TODO add another aircon layout
											if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Selected another aircon");
										}
										// TODO add other aircon control layouts here
									}
								})
						.setNegativeButton("Cancel",
								new DialogInterface.OnClickListener() {
									public void onClick(DialogInterface dialog, int id) {
										dialog.cancel();
									}
								});

				// create alert dialog
				AlertDialog alertDialog = airconDialogBuilder.create();

				// show it
				alertDialog.show();
				break;
			case R.id.im_bath:
				dlgIconIndex = 0;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_bed:
				dlgIconIndex = 1;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_dining:
				dlgIconIndex = 2;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_entertain:
				dlgIconIndex = 3;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_kids:
				dlgIconIndex = 4;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_kitchen:
				dlgIconIndex = 5;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_living:
				dlgIconIndex = 6;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.im_office:
				dlgIconIndex = 7;
				Utilities.highlightDlgIcon(iconButtons[dlgIconIndex], airconDialogView);
				break;
			case R.id.cb_sec_auto_alarm:
				if (secAutoAlarm.isChecked()) {
					String onTime = Integer.toString(secAutoOnStored);
					if (secAutoOnStored < 10) {
						onTime = "0" + onTime;
					}
					String offTime = Integer.toString(secAutoOffStored);
					if (secAutoOffStored < 10) {
						offTime = "0" + offTime;
					}
					url = SECURITY_URL_FRONT_1;
					cmd = "/?a=2," + onTime + "," + offTime;
					id = "sec";
					secAutoAlarm.setText(getResources().getString(R.string.sec_auto_alarm_on,secAutoOn,secAutoOff));
					secChangeAlarm.setVisibility(View.VISIBLE);
				} else {
					url = SECURITY_URL_FRONT_1;
					cmd = "/?a=3";
					id = "sec";
					secAutoAlarm.setText(getResources().getString(R.string.sec_auto_alarm_off));
					secChangeAlarm.setVisibility(View.INVISIBLE);
				}
				break;
			case R.id.tv_change_alarm:
				final Dialog d = new Dialog(MyHomeControl.this);
				final int orgOnTime = secAutoOnStored;
				final int orgOffTime = secAutoOffStored;
				d.setTitle("NumberPicker");
				d.setContentView(R.layout.alarm_settings);
				Button cancelButton = (Button) d.findViewById(R.id.bt_sec_cancel);
				Button okButton = (Button) d.findViewById(R.id.bt_sec_ok);
				final NumberPicker npOnTime = (NumberPicker) d.findViewById(R.id.np_Alarm_on);
				npOnTime.setMaxValue(23);
				npOnTime.setMinValue(0);
				npOnTime.setValue(secAutoOnStored);
				npOnTime.setWrapSelectorWheel(false);
				npOnTime.setOnValueChangedListener( new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOnStored = newVal;
					}
				});
				final NumberPicker npOffTime = (NumberPicker) d.findViewById(R.id.np_Alarm_off);
				npOffTime.setMaxValue(23);
				npOffTime.setMinValue(0);
				npOffTime.setValue(secAutoOffStored);
				npOffTime.setWrapSelectorWheel(false);
				npOffTime.setOnValueChangedListener( new NumberPicker.OnValueChangeListener() {
					@Override
					public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
						secAutoOffStored = newVal;
					}
				});
				cancelButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						// Reset changed values
						secAutoOnStored = orgOnTime;
						secAutoOffStored = orgOffTime;
						d.dismiss();
					}
				});
				okButton.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						String onTime = Integer.toString(secAutoOnStored);
						if (secAutoOnStored < 10) {
							onTime = "0" + onTime;
						}
						String offTime = Integer.toString(secAutoOffStored);
						if (secAutoOffStored < 10) {
							offTime = "0" + offTime;
						}
						new ESPcommunication().execute(SECURITY_URL_FRONT_1,
								"/?a=2," + onTime + "," + offTime,
								"","sec",
								"0");
						new ESPcommunication().execute(SECURITY_URL_FRONT_1,
								"/?s",
								"","sec",
								"0");
						d.dismiss();
					}
				});
				d.show();
				break;
			case R.id.ib_security:
				if (Utilities.isHomeWiFi(this)) {
					if (!initializeIsRunning) {
						comView.setVisibility(View.VISIBLE);
					}
					// Update of security values
					new ESPcommunication().execute(SECURITY_URL_FRONT_1, "/?s", "", "sec", Integer.toString(selDevice));
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_home), Toast.LENGTH_LONG).show();
				}

				// Make security menu items visible
				menuItem = abMenu.getItem(0); // Alarm sound menu entry
				menuItem.setVisible(true);
				menuItem = abMenu.getItem(1); // Solar alarm sound menu entry
				menuItem.setVisible(false);
				menuItem = abMenu.getItem(2); // Aircon location menu entry
				menuItem.setVisible(false);
				// Make security view visible
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				secView.setVisibility(View.VISIBLE);
				visibleView = 0;
				mPrefs.edit().putInt(prefsLastView,visibleView).apply();
				break;
			case R.id.ib_solar:
				if (!initializeIsRunning) {
					comView.setVisibility(View.VISIBLE);
				}
				// Update of solar panel values
				new ESPcommunication().execute(SOLAR_URL, "/data/get", "", "spm", Integer.toString(selDevice));

				// Make solar panel menu items visible
				menuItem = abMenu.getItem(0); // Alarm sound menu entry
				menuItem.setVisible(false);
				menuItem = abMenu.getItem(1); // Solar alarm sound menu entry
				menuItem.setVisible(true);
				menuItem = abMenu.getItem(2); // Aircon location menu entry
				menuItem.setVisible(false);
				secView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.VISIBLE);
				visibleView = 1;
				mPrefs.edit().putInt(prefsLastView,visibleView).apply();
				break;
			case R.id.ib_aircon:
				if (Utilities.isHomeWiFi(this)) {
					// Update of security values
					if (!initializeIsRunning) {
						comView.setVisibility(View.VISIBLE);
					}
					switch (selDevice) {
						case 0:
							new ESPcommunication().execute(AIRCON_URL_1,"/?s","","air",Integer.toString(selDevice));
							break;
						case 1:
							new ESPcommunication().execute(AIRCON_URL_2,"/?s","","air",Integer.toString(selDevice));
							break;
						case 2:
							new ESPcommunication().execute(AIRCON_URL_3,"/?s","","air",Integer.toString(selDevice));
							break;
					}
				} else {
					Toast.makeText(getApplicationContext(), getResources().getString(R.string.not_home), Toast.LENGTH_LONG).show();
				}

				// Make aircon menu items visible
				menuItem = abMenu.getItem(0); // Alarm sound menu entry
				menuItem.setVisible(false);
				menuItem = abMenu.getItem(1); // Solar alarm sound menu entry
				menuItem.setVisible(false);
				menuItem = abMenu.getItem(2); // Aircon location menu entry
				menuItem.setVisible(true);
				secView.setVisibility(View.INVISIBLE);
				solView.setVisibility(View.INVISIBLE);
				airView.setVisibility(View.VISIBLE);
				visibleView = 2;
				mPrefs.edit().putInt(prefsLastView,visibleView).apply();
				break;
		}
		if (!url.isEmpty()) {
			if (!initializeIsRunning) {
				comView.setVisibility(View.VISIBLE);
			}
			new ESPcommunication().execute(url,cmd,"",id,Integer.toString(selDevice));
			if (id.equalsIgnoreCase("air")) {
				new ESPcommunication().execute(url,"/?s","",id,Integer.toString(selDevice));
			}
		}
	}

	/**
	 * Broadcast receiver for notifications received over UDP or GCM
	 */
	private BroadcastReceiver activityReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			/** Message received over UDP or GCM or from */
			String message = intent.getStringExtra("message");

			/** Return values for onPostExecute */
			ESPcommResultWrapper result = new ESPcommResultWrapper();

			// Check if response is a JSON array
			if (Utilities.isJSONValid(message)) {
				result.comResult = message;
				JSONObject jsonResult;
				try {
					jsonResult = new JSONObject(message);
					try {
						if (jsonResult.has("device")) { // Message from security or aircon
							String broadCastDevice = jsonResult.getString("device");
							if (broadCastDevice.startsWith("sf")) { // Broadcast from security device
								result.comCmd = "/?s";
								securityViewUpdate(result);
							} else if (broadCastDevice.startsWith("fd")) { // Broadcast from aircon device 0
								result.comCmd = "/?s";
								result.deviceIndex = 0;
								airconViewUpdate(result);
							} else if (broadCastDevice.startsWith("ca")) { // Broadcast from aircon device 1
								result.comCmd = "/?s";
								result.deviceIndex = 1;
								airconViewUpdate(result);
							}
						} else { // Message from spMonitor update service
							solarViewUpdate(message);
						}
					} catch (JSONException ignore) {
					}
				} catch (JSONException e) {
					if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
				}
			}
		}
	};

	private class ESPcommunication extends AsyncTask<String, String, ESPcommResultWrapper> {

		@Override
		protected ESPcommResultWrapper doInBackground(String... params) {

			/** A HTTP client to access the ESP device */
			OkHttpClient client = new OkHttpClient();

			/** Return values for onPostExecute */
			ESPcommResultWrapper result = new ESPcommResultWrapper();

			result.httpURL = params[0];
			result.comCmd = params[1];
			result.comResult = params[2];
			result.callID = params[3];
			result.deviceIndex = Integer.parseInt(params[4]);

			if (result.callID.equalsIgnoreCase("spm")) {
				if (!Utilities.isHomeWiFi(getApplicationContext())) {
					// For solar panel monitor get data from web site if we are not home
					result.httpURL = "http://www.spmonitor.giesecke.tk/l.php";
					result.comCmd = "/l.php";
				}
			}
			/** URL to be called */
			String urlString = result.httpURL + result.comCmd; // URL to call

			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "callESP = " + urlString);

			/** Request to ESP device */
			Request request = new Request.Builder()
					.url(urlString)
					.build();

			if (request != null) {
				try {
					/** Response from ESP device */
					Response response = client.newCall(request).execute();
					if (response != null) {
						result.comResult = response.body().string();
					}
				} catch (IOException e) {
					result.comResult = e.getMessage();
					try {
						if (result.comResult.contains("EHOSTUNREACH")) {
							result.comResult = MyHomeControl.appContext.getString(R.string.err_esp);
						}
						if (result.comResult.equalsIgnoreCase("")) {
							result.comResult = MyHomeControl.appContext.getString(R.string.err_esp);
						}
						return result;
					} catch (NullPointerException en) {
						result.comResult = MyHomeControl.appContext.getString(R.string.err_no_esp);
						return result;
					}
				}
			}

			if (result.comResult.equalsIgnoreCase("")) {
				result.comResult = MyHomeControl.appContext.getString(R.string.err_esp);
			}
			return result;
		}

		protected void onPostExecute(ESPcommResultWrapper result) {
			switch (result.callID) {
				case "sec": // Caller is security view
					if (result.httpURL.equalsIgnoreCase(SECURITY_URL_FRONT_1)) {
						initSec1IsOn = false;
					} else {
						initSec2IsOn = false;
					}
					securityViewUpdate(result);
					break;
				case "air": // Caller is aircon view
					switch (result.deviceIndex) {
						case 0:
							initAir1IsOn = false;
							break;
						case 1:
							initAir2IsOn = false;
							break;
						case 2:
							initAir3IsOn = false;
							break;
					}
					airconViewUpdate(result);
					break;
				case "spm": // Caller is solar monitor view
					initSolIsOn = false;
					solarViewUpdate(result.comResult);
					break;
			}
		}
	}

	/**
	 * Update UI with values received from ESP device
	 *
	 * @param result
	 * 		result sent by onPostExecute
	 */
	private void securityViewUpdate(final ESPcommResultWrapper result) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				/** String used for temporary conversions */
				String tempString;
				if (Utilities.isJSONValid(result.comResult)) {
					/** JSON object to hold the result received from the ESP8266 */
					JSONObject jsonResult;
					try {
						jsonResult = new JSONObject(result.comResult);
						try {
							tempString = jsonResult.getString("device");
							if (!tempString.startsWith("sf")) { // Broadcast not from security
								return;
							}
						} catch (Exception ignore) {
						}

						if (result.comCmd.equalsIgnoreCase("/?s")) { // Status request
							/** String to hold complete status in viewable form */
							String message;
							// Get device status and light status and add it to viewable status
							message = Utilities.getDeviceStatus(jsonResult) + Utilities.getLightStatus(jsonResult);
							try {
								tempString = jsonResult.getString("ssid");
								message += "SSID: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("ip");
								message += "IP: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("mac");
								message += "MAC: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("sketch");
								message += "Sketch size: " + tempString + "\n";
							} catch (JSONException ignore) {
							}
							try {
								tempString = jsonResult.getString("freemem");
								message += "Free Memory: " + tempString + "\n";
							} catch (JSONException ignore) {
							}

							if (showDebug) {
								secStatus.setText(message);
							} else {
								secStatus.setText("");
							}
						} else { // Change of alarm status
							try {
								if (showDebug) {
									tempString = jsonResult.getString("result");
									secStatus.setText(tempString);
								} else {
									secStatus.setText("");
								}
							} catch (JSONException e) {
								secStatus.setText(getString(R.string.err_unknown));
							}

						}
					} catch (JSONException e) {
						if (BuildConfig.DEBUG)
							Log.d(DEBUG_LOG_TAG, "Create JSONObject from String failed " + e.getMessage());
					}
				}
				if (!initSolIsOn && !initSec1IsOn && !initSec2IsOn && !initAir1IsOn && !initAir2IsOn && !initAir3IsOn) {
					initializeIsRunning = false;
				}
				if (!initializeIsRunning) {
					comView.setVisibility(View.INVISIBLE);
				}
				// Update security widgets
				/** App widget manager for all widgets of this app */
				AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(appContext);
				/** Component name of this widget */
				ComponentName thisAppWidget = new ComponentName(appContext.getPackageName(),
						SecurityWidget.class.getName());
				/** List of all active widgets */
				int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidget);

				for (int appWidgetId : appWidgetIds) {
					SecurityWidget.updateAppWidget(appContext, appWidgetManager, appWidgetId, false);
				}
			}
		});
	}

	/**
	 * Parse JSON and show received status in UI
	 *
	 * @param result
	 *            ESPcommResultWrapper
	 *               isSearchDevice = flag that device search is active
	 *               deviceIndex = index of device that is investigated
	 *               reqCmd = command to be sent to the ESP device
	 *               comResult = return string as JSON from the ESP device
	 */
	private void airconViewUpdate(ESPcommResultWrapper result) {

		try {
			/** JSON object with the result from the ESP device */
			JSONObject deviceResult = new JSONObject(result.comResult);
			if (deviceResult.has("result")) {
				if (deviceResult.getString("result").equalsIgnoreCase("success")) {
					if (deviceResult.has("device")) {
						deviceName[result.deviceIndex] = deviceResult.getString("device");
						if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("fd")) {
							deviceType[result.deviceIndex] = FUJIDENZO;
						}
						if (deviceName[result.deviceIndex].substring(0, 2).equalsIgnoreCase("ca")) {
							deviceType[result.deviceIndex] = CARRIER;
						}
						// TODO here is the place to add more layout versions for air cons
					}
					if (deviceResult.has("power")) {
						powerStatus[result.deviceIndex] = deviceResult.getInt("power");
					}
					if (deviceResult.has("mode")) {
						modeStatus[result.deviceIndex] = deviceResult.getInt("mode");
					}
					if (deviceResult.has("speed")) {
						fanStatus[result.deviceIndex] = deviceResult.getInt("speed");
					}
					if (deviceResult.has("temp")) {
						coolStatus[result.deviceIndex] = deviceResult.getInt("temp");
					}
					if (deviceResult.has("cons")) {
						consStatus = deviceResult.getDouble("cons");
					}
					if (deviceResult.has("status")) {
						autoStatus = deviceResult.getInt("status");
					}
					if (deviceResult.has("auto")) {
						autoOnStatus[result.deviceIndex] = deviceResult.getInt("auto");
					}
					if (deviceResult.has("sweep")) {
						sweepStatus[result.deviceIndex] = deviceResult.getInt("sweep");
					}
					if (deviceResult.has("turbo")) {
						turboStatus[result.deviceIndex] = deviceResult.getInt("turbo");
					}
					if (deviceResult.has("ion")) {
						ionStatus[result.deviceIndex] = deviceResult.getInt("ion");
					}
					// TODO here is the place to add more status for other air cons

					// Update UI
					updateAirStatus(result.deviceIndex);
				} else {
					if (BuildConfig.DEBUG)
						Log.d(DEBUG_LOG_TAG, "Communication result = " + result.comResult);
				}
			}
		} catch (JSONException e) {
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Received invalid JSON = " + result.comResult);
		}
		if (showDebug) {
			airStatus.setText(result.comResult);
		} else {
			airStatus.setText("");
		}
		if (!initSolIsOn && !initSec1IsOn && !initSec2IsOn && !initAir1IsOn && !initAir2IsOn && !initAir3IsOn) {
			initializeIsRunning = false;
		}
		if (!initializeIsRunning) {
			comView.setVisibility(View.INVISIBLE);
		}
	}

	/**
	 * Update UI fields with the latest status of a device
	 *
	 * @param deviceIndex
	 *            Index of the device to be updated
	 */
	private static void updateAirStatus(int deviceIndex) {
		String consText = String.format("%.0f", consStatus) + "W";
		String tempText = Integer.toString(coolStatus[deviceIndex]) + "C";
		String statusText = Integer.toString(autoStatus);

		switch (deviceType[deviceIndex]) {
			case FUJIDENZO:
				btOnOffLightFD.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btCoolLightFD.setBackgroundColor(colorGrey);
						btDryLightFD.setBackgroundColor(colorGrey);
						btFanLightFD.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btCoolLightFD.setBackgroundColor(colorGrey);
						btDryLightFD.setBackgroundColor(colorRed);
						btFanLightFD.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btCoolLightFD.setBackgroundColor(colorRed);
						btDryLightFD.setBackgroundColor(colorGrey);
						btFanLightFD.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanHighLightFD.setBackgroundColor(colorGrey);
						btFanMedLightFD.setBackgroundColor(colorGrey);
						btFanLowLightFD.setBackgroundColor(colorRed);
						break;
					case 1: // Fan medium mode
						btFanHighLightFD.setBackgroundColor(colorGrey);
						btFanMedLightFD.setBackgroundColor(colorRed);
						btFanLowLightFD.setBackgroundColor(colorGrey);
						break;
					case 2: // Fan high mode
						btFanHighLightFD.setBackgroundColor(colorRed);
						btFanMedLightFD.setBackgroundColor(colorGrey);
						btFanLowLightFD.setBackgroundColor(colorGrey);
						break;
				}
				txtConsValFD.setText(consText);
				txtTempValFD.setText(tempText);
				txtAutoStatusValFD.setText(statusText);
				btAutoLightFD.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				break;
			case CARRIER:
				btOnOffLightCA.setBackgroundColor(
						(powerStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				switch (modeStatus[deviceIndex]) {
					case 0: // Fan mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorRed);
						break;
					case 1: // Dry mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorRed);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
					case 2: // Cool mode
						btAutomLightCA.setBackgroundColor(colorGrey);
						btCoolLightCA.setBackgroundColor(colorRed);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
					case 3: // Auto mode
						btAutomLightCA.setBackgroundColor(colorRed);
						btCoolLightCA.setBackgroundColor(colorGrey);
						btDryLightCA.setBackgroundColor(colorGrey);
						btFanLightCA.setBackgroundColor(colorGrey);
						break;
				}
				switch (fanStatus[deviceIndex]) {
					case 0: // Fan low mode
						btFanCA.setText(R.string.bt_txt_fan_low);
						break;
					case 1: // Fan medium mode
						btFanCA.setText(R.string.bt_txt_fan_med);
						break;
					case 2: // Fan high mode
						btFanCA.setText(R.string.bt_txt_fan_high);
						break;
				}
				btSweepLightCA.setBackgroundColor(
						(sweepStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btTurboLightCA.setBackgroundColor(
						(turboStatus[deviceIndex] == 1) ? colorRed : colorGrey);
				btIonLightCA.setBackgroundColor(
						(ionStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				txtConsValCA.setText(consText);
				txtTempValCA.setText(tempText);
				txtAutoStatusValCA.setText(statusText);
				btAutoLightCA.setBackgroundColor(
						(autoOnStatus[deviceIndex] == 1) ? colorRed : colorGrey);

				break;
			// TODO here is the place to add more layouts for other air cons
		}
	}

	/**
	 * Update UI with values received from spMonitor device (Arduino part)
	 *
	 * @param value
	 *        result sent by spMonitor
	 */
	private void solarViewUpdate(final String value) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				/** Pointer to text views to be updated */
				TextView valueFields;
				/* String with results received from spMonitor device */
				String result;

				if (value.length() != 0) {
					// decode JSON
					if (Utilities.isJSONValid(value)) {
						/** Flag for data from external server */
						boolean isFromLocal;
						/** JSON object containing the values */
						JSONObject jsonValues = null;
						try {
							jsonValues = new JSONObject(value.substring(1,value.length()-1));
							isFromLocal = false;
						} catch (JSONException ignore) {
							isFromLocal = true;
						}
						try {
							if (isFromLocal) {
								/** JSON object containing result from server */
								JSONObject jsonResult = new JSONObject(value);
								/** JSON object containing the values */
								jsonValues = jsonResult.getJSONObject("value");
							}

							try {
								solarPowerMin = isFromLocal?
										Float.parseFloat(jsonValues.getString("S")):
										Float.parseFloat(jsonValues.getString("s"));
								lastSolarPowerMin = solarPowerMin;
							} catch (Exception excError) {
								solarPowerMin = lastSolarPowerMin;
							}
							try {
								consPowerMin = isFromLocal?
										Float.parseFloat(jsonValues.getString("C")):
										Float.parseFloat(jsonValues.getString("c"));
								lastConsPowerMin = consPowerMin;
							} catch (Exception excError) {
								consPowerMin = lastConsPowerMin;
							}

							result = "S=" + String.valueOf(solarPowerMin) + "W ";
							result += "s=";
							try {
								result += jsonValues.getString("s");
							} catch (Exception ignore) {
								result += "---";
							}
							if (jsonValues.has("sv")) {
								result += "A sv=";
								try {
									result += jsonValues.getString("sv");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sr")) {
								result += "V sr=";
								try {
									result += jsonValues.getString("sr");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sa")) {
								result += "W sa=";
								try {
									result += jsonValues.getString("sa");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("sp")) {
								result += "W sp=";
								try {
									result += jsonValues.getString("sp");
								} catch (Exception excError) {
									result += "---";
								}
							}
							result += "\nC=" + String.valueOf(consPowerMin) + "W c=";
							try {
								result += jsonValues.getString("c");
							} catch (Exception excError) {
								result += "---";
							}
							if (jsonValues.has("cv")) {
								result += "A cv=";
								try {
									result += jsonValues.getString("cv");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("cr")) {
								result += "V cr=";
								try {
									result += jsonValues.getString("cr");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("ca")) {
								result += "W ca=";
								try {
									result += jsonValues.getString("ca");
								} catch (Exception excError) {
									result += "---";
								}
							}
							if (jsonValues.has("cp")) {
								result += "W cp=";
								try {
									result += jsonValues.getString("cp") + "\n";
								} catch (Exception excError) {
									result += "---" + "\n";
								}
							} else {
								result += "\n";
							}

							/** Double for the result of solar current and consumption used at 1min updates */
							double resultPowerMin = solarPowerMin + consPowerMin;

							valueFields = (TextView) findViewById(R.id.tv_solar_val);
							/** String for display */
							String displayTxt;
							displayTxt = String.format("%.0f", solarPowerMin) + "W";
							valueFields.setText(displayTxt);
							valueFields = (TextView) findViewById(R.id.tv_cons_val);
							displayTxt = String.format("%.0f", resultPowerMin) + "W";
							valueFields.setText(displayTxt);
							if (showDebug) {
								solStatus.setText(result);
							} else {
								solStatus.setText("");
							}

							valueFields = (TextView) findViewById(R.id.tv_result_txt);
							if (consPowerMin > 0.0d) {
								valueFields.setText(getString(R.string.tv_result_txt_im));
								valueFields = (TextView) findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_red_light));
							} else {
								valueFields.setText(getString(R.string.tv_result_txt_ex));
								valueFields = (TextView) findViewById(R.id.tv_result_val);
								valueFields.setTextColor(getResources()
										.getColor(android.R.color.holo_green_light));
							}
							displayTxt = String.format("%.0f", Math.abs(consPowerMin)) + "W";
							valueFields.setText(displayTxt);
						} catch (JSONException e) {
							e.printStackTrace();
							solStatus.setText(e.getMessage());
							if (!initializeIsRunning) {
								comView.setVisibility(View.INVISIBLE);
							}
							return;
						}

						if (showDebug) {
							solStatus.setText(result);
						} else {
							solStatus.setText("");
						}
					}
				} else {
					if (showDebug) {
						solStatus.setText(value);
					} else {
						solStatus.setText("");
					}
				}
				if (!initSolIsOn && !initSec1IsOn && !initSec2IsOn && !initAir1IsOn && !initAir2IsOn && !initAir3IsOn) {
					initializeIsRunning = false;
				}
				if (!initializeIsRunning) {
					comView.setVisibility(View.INVISIBLE);
				}
			}
		});
	}

	/**
	 * Check if UDP receiver service is running
	 *
	 * @param serviceClass
	 *              Service class we want to check if it is running
	 * @return <code>boolean</code>
	 *              True if service is running
	 *              False if service is not running
	 */
	private boolean isMyServiceRunning(Class<?> serviceClass) {
		/** Activity manager for services */
		ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
			if (serviceClass.getName().equals(service.service.getClassName())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Set all global variables used
	 */
	private void setGlobalVar() {
		// For security view:
		secStatus = (TextView) findViewById(R.id.security_status);
		ivAlarmStatus = (ImageView) findViewById(R.id.dot_alarm_status);
		ivLightStatus = (ImageView) findViewById(R.id.dot_light);
		ivAlarmOn = (ImageView) findViewById(R.id.dot_alarm_on);
		secAutoAlarm = (CheckBox) findViewById(R.id.cb_sec_auto_alarm);
		secChangeAlarm = (TextView) findViewById(R.id.tv_change_alarm);

		ivAlarmStatusBack = (ImageView) findViewById(R.id.dot_alarm_status_back);
		ivLightStatusBack = (ImageView) findViewById(R.id.dot_light_back);
		ivAlarmOnBack = (ImageView) findViewById(R.id.dot_alarm_on_back);

		animator = ValueAnimator.ofFloat(0f, 1f);
		animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				ivAlarmOn.setAlpha((Float) animation.getAnimatedValue());
			}
		});

		animator.setDuration(1500);
		animator.setRepeatMode(ValueAnimator.REVERSE);
		animator.setRepeatCount(-1);

		animatorBack = ValueAnimator.ofFloat(0f, 1f);
		animatorBack.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(ValueAnimator animation) {
				ivAlarmOnBack.setAlpha((Float) animation.getAnimatedValue());
			}
		});

		animatorBack.setDuration(1500);
		animatorBack.setRepeatMode(ValueAnimator.REVERSE);
		animatorBack.setRepeatCount(-1);

		// For solar view:
		solStatus = (TextView) findViewById(R.id.solar_status);

		// For aircon view:
		airFDView = (RelativeLayout) findViewById(R.id.fuji_denzo);
		airCAView = (RelativeLayout) findViewById(R.id.carrier);

		airStatus = (TextView) findViewById(R.id.aircon_status);
		btAutoLightFD = findViewById(R.id.bt_auto_hl_fd);
		btAutoLightCA = findViewById(R.id.bt_auto_hl_ca);
		btOnOffLightFD = findViewById(R.id.bt_on_off_hl_fd);
		btOnOffLightCA = findViewById(R.id.bt_on_off_hl_ca);
		btFanHighLightFD = findViewById(R.id.bt_fan_high_hl_fd);
		btFanMedLightFD = findViewById(R.id.bt_fan_med_hl_fd);
		btFanLowLightFD = findViewById(R.id.bt_fan_low_hl_fd);
		btCoolLightFD = findViewById(R.id.bt_cool_hl_fd);
		btCoolLightCA = findViewById(R.id.bt_cool_hl_ca);
		btDryLightFD = findViewById(R.id.bt_dry_hl_fd);
		btDryLightCA = findViewById(R.id.bt_dry_hl_ca);
		btFanLightFD = findViewById(R.id.bt_fan_hl_fd);
		btFanLightCA = findViewById(R.id.bt_fan_hl_ca);
		btSweepLightCA = findViewById(R.id.bt_sweep_hl_ca);
		btTurboLightCA = findViewById(R.id.bt_turbo_hl_ca);
		btIonLightCA = findViewById(R.id.bt_ion_hl_ca);
		btAutomLightCA = findViewById(R.id.bt_autom_hl_ca);

		btFanCA = (Button) findViewById(R.id.bt_fanspeed_ca);

		txtConsValFD = (TextView) findViewById(R.id.txt_cons_val_fd);
		txtTempValFD = (TextView) findViewById(R.id.txt_temp_val_fd);
		txtAutoStatusValFD = (TextView) findViewById(R.id.txt_auto_status_val_fd);
		txtConsValCA = (TextView) findViewById(R.id.txt_cons_val_ca);
		txtTempValCA = (TextView) findViewById(R.id.txt_temp_val_ca);
		txtAutoStatusValCA = (TextView) findViewById(R.id.txt_auto_status_val_ca);

		//noinspection deprecation
		colorRed = getResources().getColor(android.R.color.holo_red_light);
		//noinspection deprecation
		colorGrey = getResources().getColor(android.R.color.darker_gray);
	}

	/**
	 * Find all available devices
	 * Check if Google Cloud Messaging is registered
	 * Send status request to all available devices
	 */
	private class Initialize extends AsyncTask<String, String, Void> {

		@SuppressLint("CommitPrefEdits")
		@Override
		protected Void doInBackground(String... params) {

			handleTasks(8, "", "", "", "", null, null);
			SOLAR_URL = getApplicationContext().getResources().getString(R.string.SOLAR_URL); // = "http://192.168.xxx.xx0";
			SECURITY_URL_FRONT_1 = getApplicationContext().getResources().getString(R.string.SECURITY_URL_FRONT_1); // = "http://192.168.xxx.xx1";
			AIRCON_URL_1 = getApplicationContext().getResources().getString(R.string.AIRCON_URL_1); // = "http://192.168.xxx.xx2";
			AIRCON_URL_2 = getApplicationContext().getResources().getString(R.string.AIRCON_URL_2); // = "http://192.168.xxx.xx3";
			AIRCON_URL_3 = getApplicationContext().getResources().getString(R.string.AIRCON_URL_3); // = "http://192.168.xxx.xx3";

			// Get pointer to shared preferences
			mPrefs = getSharedPreferences(sharedPrefName,0);

			// Scan for available devices
			Utilities.findDevicesOnLAN(getApplicationContext());

			initAircons();
			initSecurity();
			initSPM();

			return null;
		}
	}

	private void initSPM() {
		if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of spMonitor");
		if (!deviceIsOn[spMonitorIndex]) { // spMonitor
			// Show message not on home WiFi
			handleTasks(1, getResources().getString(R.string.err_spMonitor), "", "", "", null, null);
		}
		// Update of solar panel values
		handleTasks(3, "/data/get", SOLAR_URL, "spm", Integer.toString(selDevice), null, null);
	}

	private void initAircons() {
		if (deviceIsOn[aircon1Index]) { // Aircon 1 - Office
			// Get initial status from Aircon 1
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 1");
			initAir1IsOn = true;
			// Update aircon status
			handleTasks(3, "/?s", AIRCON_URL_1, "air", "0", null, null);
			if (mPrefs.contains(prefsLocationName + "0")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "0", "");
			}
			// Update aircon 1 location name
			handleTasks(4, locationName[0], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "0")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 1 icon
			//noinspection deprecation
			handleTasks(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[0]]));
		}
		if (deviceIsOn[aircon2Index]) { // Aircon 2 - Living room
			// Get initial status from Aircon 2
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 2");
			initAir2IsOn = true;
			// Update aircon 2 status
			handleTasks(3, "/?s", AIRCON_URL_2, "air", "1", null, null);
			if (mPrefs.contains(prefsLocationName + "1")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "1", "");
			}
			// Update aircon 2 location name
			handleTasks(5, locationName[1], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "1")) {
				deviceIcon[1] = mPrefs.getInt(prefsDeviceIcon + "0", 99);
			}
			// Update aircon 2 icon
			//noinspection deprecation
			handleTasks(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_ca),
					getResources().getDrawable(iconIDs[deviceIcon[1]]));
		}
		if (deviceIsOn[aircon3Index]) { // Aircon 3 - Bedroom
			// Get initial status from Aircon 3
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of Aircon 3");
			initAir3IsOn = true;
			// Update aircon 3 status
			handleTasks(3, "/?s", AIRCON_URL_3, "air", "2", null, null);
			if (mPrefs.contains(prefsLocationName + "2")) {
				locationName[0] = mPrefs.getString(prefsLocationName + "2", "");
			}
			// Update aircon 3 location name
			handleTasks(6, locationName[2], "", "", "", null, null);
			if (mPrefs.contains(prefsDeviceIcon + "2")) {
				deviceIcon[0] = mPrefs.getInt(prefsDeviceIcon + "2", 99);
			}
			// Update aircon 3 icon
			//noinspection deprecation
			handleTasks(7, "", "", "", "",
					(ImageView) findViewById(R.id.im_icon_fd),
					getResources().getDrawable(iconIDs[deviceIcon[2]]));
		}
		if (!deviceIsOn[aircon1Index] && !deviceIsOn[aircon2Index] && !deviceIsOn[aircon3Index]) {
			// Show message no aircons found
			handleTasks(2, getResources().getString(R.string.err_aircon),"","","", null, null);
		}
	}

	private void initSecurity() {
		if (deviceIsOn[secFrontIndex]) { // Security front
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of front Security");
			initSec1IsOn = true;
			// Update security status front sensor
			handleTasks(3, "/?s", SECURITY_URL_FRONT_1, "sec", Integer.toString(selDevice), null, null);
		} else {
			// Show message not on home WiFi
			handleTasks(0, getResources().getString(R.string.err_security), "", "", "", null, null);
		}
		if (deviceIsOn[secRearIndex]) { // Security back
			// Get initial status from Security
			if (BuildConfig.DEBUG) Log.d(DEBUG_LOG_TAG, "Get status of rear Security");
			initSec2IsOn = true;
			// Update security status back sensor
			handleTasks(3, "/?s", SECURITY_URL_BACK_1, "sec", Integer.toString(selDevice), null, null);
			handleTasks(10, "", "", "", "", null, null);
		} else {
			// Show message not on home WiFi
			handleTasks(0, getResources().getString(R.string.err_security), "", "", "", null, null);
		}
	}

	/**
	 * Handle UI tasks of async task initialize
	 *
	 * @param task
	 * 		task to take care of
	 * 	        0: Security screen status update
	 * 	        1: spMonitor screen status update
	 * 	        2: Aircon screen status update
	 * 	        3: start communication with ESP8266
	 * 	        4: Aircon 1 location text
	 * 	        5: Aircon 2 location text
	 * 	        6: Aircon 3 location text
	 * 	        7: Aircon location icon
	 * 	        8: Show communication progress spinner
	 * 	        9: Start background sync of database
	 * 	        10: Show backyard alarm status
	 * @param message
	 *      Update string for task 0, 1 and 2
	 *      Location text for task 4, 5 and 6
	 *      Command used in task 3
	 *      Database name in task 9
	 * @param url
	 *      URL to be used in task 3
	 * @param deviceID
	 *      Device ID to be used in task 3
	 * @param airconID
	 *      Aircon ID to be used in task 3
	 * @param iconImage
	 *      Image view of icon for task 7
	 * @param iconDrawable
	 *      Drawable of icon for task 7
	 */
	private void handleTasks(final int task,
							 final String message,
							 final String url,
							 final String deviceID,
							 final String airconID,
							 final ImageView iconImage,
							 final Drawable iconDrawable) {
		runOnUiThread(new Runnable() {
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				/** Text view to show location name */
				TextView locationText;
				switch (task) {
					case 0: // Security screen status update
						if (showDebug) {
							secStatus.setText(message);
						} else {
							secStatus.setText("");
						}
						break;
					case 1: // spMonitor screen status update
						solStatus.setText(message);
						break;
					case 2: // Aircon screen status update
						if (showDebug) {
							airStatus.setText(message);
						} else {
							airStatus.setText("");
						}
						break;
					case 3: // start communication with ESP8266
						new ESPcommunication().execute(url, message, "", deviceID, airconID);
						break;
					case 4: // Aircon 1 location text
						locationText = (TextView) findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						break;
					case 5: // Aircon 2 location text
						locationText = (TextView) findViewById(R.id.txt_device_ca);
						locationText.setText(message);
						break;
					case 6: // Aircon 3 location text
						locationText = (TextView) findViewById(R.id.txt_device_fd);
						locationText.setText(message);
						break;
					case 7: // Aircon location icon
						iconImage.setImageDrawable(iconDrawable);
						break;
					case 8: // Show communication spinner
						comView.setVisibility(View.VISIBLE);
						break;
					case 10:
						TableLayout backYardDots = (TableLayout) findViewById(R.id.tl_alarm_back);
						backYardDots.setVisibility(View.VISIBLE);
						break;
				}
			}
		});
	}
}

