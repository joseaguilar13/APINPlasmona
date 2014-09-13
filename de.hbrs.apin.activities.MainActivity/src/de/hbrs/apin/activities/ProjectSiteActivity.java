/*
 * Copyright Ç©È Dec 23, 2011, Paul Woelfel, Email: frig@frig.at 
 * Created on November, 2012
 * Author : Jose Aguilar
 * Email: jose.aguilar@smail.inf.h-brs.de
 */
package de.hbrs.apin.activities;

import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PointF;
import android.graphics.drawable.AnimationDrawable;
import android.net.wifi.ScanResult;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.InputType;
import android.text.format.Time;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import at.fhstp.wificompass.exceptions.SiteNotFoundException;
import at.fhstp.wificompass.exceptions.WifiException;
import at.fhstp.wificompass.model.AccessPoint;
import at.fhstp.wificompass.model.BluetoothBeacon;
import at.fhstp.wificompass.model.Bssid;
import at.fhstp.wificompass.model.BssidResult;
import at.fhstp.wificompass.model.Location;
import at.fhstp.wificompass.model.ProjectSite;
import at.fhstp.wificompass.model.WifiScanResult;
import at.fhstp.wificompass.model.helper.DatabaseHelper;
import at.fhstp.wificompass.model.helper.SelectBssdidsExpandableListAdapter;
import at.fhstp.wificompass.trilateration.WeightedCentroidTrilateration;
import at.fhstp.wificompass.userlocation.LocationChangeListener;
import at.fhstp.wificompass.userlocation.LocationServiceFactory;
import at.fhstp.wificompass.userlocation.StepDetectionProvider;
import at.fhstp.wificompass.view.AccessPointDrawable;
import at.fhstp.wificompass.view.BluetoothBeaconDrawable;
import at.fhstp.wificompass.view.MeasuringPointDrawable;
import at.fhstp.wificompass.view.MultiTouchDrawable;
import at.fhstp.wificompass.view.MultiTouchView;
import at.fhstp.wificompass.view.NorthDrawable;
import at.fhstp.wificompass.view.OkCallback;
import at.fhstp.wificompass.view.RefreshableView;
import at.fhstp.wificompass.view.ScaleLineDrawable;
import at.fhstp.wificompass.view.SiteMapDrawable;
import at.fhstp.wificompass.view.UserDrawable;
import at.fhstp.wificompass.view.UserWiFiDrawable;
import at.fhstp.wificompass.wifi.WifiResultCallback;
import at.fhstp.wificompass.wifi.WifiScanner;
import at.woelfel.philip.filebrowser.FileBrowser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;


import de.hbrs.apin.Logger;
import de.hbrs.apin.R;
import de.hbrs.apin.ToolBox;

/**
 * @author Jose Aguilar jose.aguilar@smail.inf.hbrs.de
 * @source Paul Woelfel (paul@woelfel.at)
 */
public class ProjectSiteActivity extends Activity implements OnClickListener, WifiResultCallback, RefreshableView, LocationChangeListener {

	/**
	 * @uml.property name="log"
	 * @uml.associationEnd
	 */
	protected Logger log = new Logger(ProjectSiteActivity.class);

	public static final String SITE_KEY = "SITE";

	public static final String PROJECT_KEY = "PROJECT";

	public static final String SCAN_INTERVAL = "scan_interval";

	// public static final int START_NEW = 1, START_LOAD = 2;

	protected static final int DIALOG_TITLE = 1, DIALOG_SCANNING = 2, DIALOG_CHANGE_SIZE = 3, DIALOG_SET_BACKGROUND = 4, DIALOG_SET_SCALE_OF_MAP = 5,
			DIALOG_ADD_KNOWN_AP = 6, DIALOG_SELECT_BSSIDS = 7, DIALOG_FRESH_SITE = 8, DIALOG_ASK_CHANGE_SCALE = 9, DIALOG_ASK_FOR_NORTH = 10,
			DIALOG_CHANGE_SCAN_INTERVAL = 11;

	protected static final int MESSAGE_REFRESH = 1, MESSAGE_START_WIFISCAN = 2, MESSAGE_PERSIST_RESULT = 3;

	protected static final int FILEBROWSER_REQUEST = 1;

	/**
	 * how often should we start a wifi scan
	 */
	protected int schedulerTime = 10;

	/**
	 * @uml.property name="multiTouchView"
	 * @uml.associationEnd
	 */
	protected MultiTouchView multiTouchView;

	/**
	 * @uml.property name="map"
	 * @uml.associationEnd
	 */
	protected SiteMapDrawable map;

	/**
	 * @uml.property name="site"
	 * @uml.associationEnd
	 */
	protected ProjectSite site;

	/**
	 * @uml.property name="databaseHelper"
	 * @uml.associationEnd
	 */
	protected DatabaseHelper databaseHelper = null;

	protected Dao<ProjectSite, Integer> projectSiteDao = null;

	protected AlertDialog scanAlertDialog;

	protected ImageView scanningImageView;

	protected boolean ignoreWifiResults = false;

	protected BroadcastReceiver wifiBroadcastReceiver;

	/**
	 * @uml.property name="user"
	 * @uml.associationEnd
	 */
	protected UserDrawable user;
	
	//protected UserWiFiDrawable userWiFi;

	protected MeasuringPointDrawable mp; 
	
	/**
	 * @uml.property name="scaler"
	 * @uml.associationEnd
	 */
	protected ScaleLineDrawable scaler = null;

	protected final Context context = this;

	protected TextView backgroundPathTextView;

	protected float scalerDistance;
	
	PointF WiFiPos = null;
	
	/**
	 * @uml.property name="triangulationTask"
	 * @uml.associationEnd
	 */
	protected TriangulationTask triangulationTask = null;

	/**
	 * @uml.property name="stepDetectionProvider"
	 * @uml.associationEnd
	 */
	protected StepDetectionProvider stepDetectionProvider = null;

	/**
	 * @uml.property name="northDrawable"
	 * @uml.associationEnd
	 */
	protected NorthDrawable northDrawable = null;

	protected Handler messageHandler;

	protected final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

	protected Runnable wifiRunnable;

	protected ScheduledFuture<?> scheduledTask = null;

	protected ArrayList<WifiScanResult> unsavedScanResults;
	
	protected ArrayList<AccessPoint> addedAP;
	
	protected ArrayList<BluetoothBeacon> addedBT;

	protected boolean walkingAndScanning = false;

	protected boolean freshSite = false;
	
	protected boolean flagWF = false; 
	
	protected boolean flagDR = false;
	
	protected boolean flagBT = false;
	
	protected boolean flagQuickScan = false;

	protected boolean flagLogText = false;
	
	protected boolean AddKnownFlagBT = false;
	
	protected boolean trackSteps= true; 
	
	protected boolean flagKnownAP = false;
    
	protected int testv=0; 
	
	protected String floorID = "0";
	
	protected int DRAccStep = 0;
	
	Location FinalPos = null;
	
	protected float errorWIFI = 10;
	
	protected float errorDR = 0;
	
	protected float errorBT = 30;
	
    protected int rounds=0;

    protected int refPoint=0;
    
    protected float heading = 0;
    
    Location DRFinalPos = null;

    Location WiFiFinalPos = null;
    
    protected boolean newObservation = false;

    protected int newWifiPos = 0;
    
    protected int stepsGlobal = 0;
	
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	
	
	// For the bluetooth comm
	ArrayAdapter<String> listAdapter;
	ListView listView;
	BluetoothAdapter btAdapter;
	Set<BluetoothDevice> devicesArray;
	ArrayList<String> pairedDevices;
	ArrayList<BluetoothDevice> devices;
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	protected static final int SUCCESS_CONNECT = 0;
	protected static final int MESSAGE_READ = 1;


	IntentFilter filter;
	BroadcastReceiver receiver;
	String tag = "debugging";
	Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg) {
			// TODO Auto-generated method stub
			Log.i(tag, "in handler");
			super.handleMessage(msg);
			switch(msg.what){
			case SUCCESS_CONNECT:
				// DO something
				ConnectedThread connectedThread = new ConnectedThread((BluetoothSocket)msg.obj);
				Toast.makeText(getApplicationContext(), "CONNECT", 0).show();
				String s = "successfully connected";
				connectedThread.write(s.getBytes());
				Log.i(tag, "connected");
				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[])msg.obj;
				String string = new String(readBuf);
				Toast.makeText(getApplicationContext(), string, 0).show();
				break;
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		try {
			this.setContentView(R.layout.project_site);
			super.onCreate(savedInstanceState);
			Intent intent = this.getIntent();

			int siteId = intent.getExtras().getInt(SITE_KEY, -1);
			if (siteId == -1) {
				throw new SiteNotFoundException("ProjectSiteActivity called without a correct site ID!");
			}
			
		

			databaseHelper = OpenHelperManager.getHelper(this, DatabaseHelper.class);
			projectSiteDao = databaseHelper.getDao(ProjectSite.class);
			site = projectSiteDao.queryForId(siteId);

			if (site == null) {
				throw new SiteNotFoundException("The ProjectSite Id could not be found in the database!");
			}

			MultiTouchDrawable.setGridSpacing(site.getGridSpacingX(), site.getGridSpacingY());

			map = new SiteMapDrawable(this, this);
			map.setAngleAdjustment(site.getNorth());

			if (site.getWidth() == 0 || site.getHeight() == 0) {
				// the site has never been loaded
				freshSite = true;
				site.setSize(map.getWidth(), map.getHeight());
			} else {
				map.setSize(site.getWidth(), site.getHeight());
			}
			if (site.getBackgroundBitmap() != null) {
				map.setBackgroundImage(site.getBackgroundBitmap());
			}

			for (AccessPoint ap : site.getAccessPoints()) {
				new AccessPointDrawable(this, map, ap);
			}
			
//			for (BluetoothBeacon bt : site.getBluetoothBeacons()) {
//				new BluetoothBeaconDrawable(this, map, bt);
//			}

			for (WifiScanResult wsr : site.getScanResults()) {
				 new MeasuringPointDrawable(this, map, wsr);
			}
			

			user = new UserDrawable(this, map);
			
			//userWiFi = new UserWiFiDrawable(this, map);

			
			if (site.getLastLocation() != null) {
				user.setRelativePosition(site.getLastLocation().getX(), site.getLastLocation().getY());
				//userWiFi.setRelativePosition(site.getLastLocation().getX(), site.getLastLocation().getY());
				//userWiFi.setCenterX(site.getLastLocation().getX());
				//userWiFi.setCenterY(site.getLastLocation().getY());
				

			} else {
				user.setRelativePosition(map.getWidth() / 2, map.getHeight() / 2);
				//userWiFi.setRelativePosition(map.getWidth() / 2, map.getHeight() / 2);
				//userWiFi.setCenterX(map.getWidth() / 2);
				//userWiFi.setCenterY(map.getHeight() / 2);

			}

			LocationServiceFactory.getLocationService().setRelativeNorth(site.getNorth());
			LocationServiceFactory.getLocationService().setGridSpacing(site.getGridSpacingX(), site.getGridSpacingY());
			stepDetectionProvider = new StepDetectionProvider(this);
			stepDetectionProvider.setLocationChangeListener(this);

			messageHandler = new Handler() {
				@Override
				public void handleMessage(Message msg) {
					switch (msg.what) {
					case MESSAGE_REFRESH:
						/* Refresh UI */
						if (multiTouchView != null)
							multiTouchView.invalidate();
						break;
					case MESSAGE_START_WIFISCAN:
						
						// start a wifiscan
						if(flagWF){
						startWifiBackgroundScan();
						}
						
						if(flagBT){
							//for the BluetoothComm
							
							//super.onCreate(savedInstanceState);
					        //setContentView(R.layout.main);
					        initBluetooth();
					        if(btAdapter==null){
					        	Toast.makeText(getApplicationContext(), "No bluetooth detected", 0).show();
					        	finish();
					        }
					        else{
					        	if(!btAdapter.isEnabled()){
					        		turnOnBT();
					        	}
					        	
					        	getPairedDevices();
					        	startDiscovery();
					        } // end of BluetoothComm   
							
						}
						
						
						break;

					case MESSAGE_PERSIST_RESULT:

						if (msg.arg1 == RESULT_OK) {
							if (msg.getData().getInt(WifiScanResultPersistTask.RESULT_COUNT) > 0)
								Toast.makeText(context, R.string.project_site_scanresults_persisted, Toast.LENGTH_SHORT).show();
						} else {
							Toast.makeText(
									context,
									context.getString(R.string.project_site_scanresults_not_persisted,
											msg.getData().getString(WifiScanResultPersistTask.RESULT_MESSAGE)), Toast.LENGTH_LONG).show();
						}

						break;
					}
				}
			};

			wifiRunnable = new Runnable() {

				@Override
				public void run() {
					messageHandler.sendEmptyMessage(MESSAGE_START_WIFISCAN);
				}

			};

			unsavedScanResults = new ArrayList<WifiScanResult>();
			
			addedAP = new ArrayList<AccessPoint>();
			
			addedBT = new ArrayList<BluetoothBeacon>();


			schedulerTime = this.getPreferences(Activity.MODE_PRIVATE).getInt(SCAN_INTERVAL, schedulerTime);

			initUI();

		} catch (Exception ex) {
			log.error("Failed to create ProjectSiteActivity: " + ex.getMessage(), ex);
			Toast.makeText(this, R.string.project_site_load_failed, Toast.LENGTH_LONG).show();
			this.finish();
		}
	
	
//		//for the BluetoothComm
//		
//		//super.onCreate(savedInstanceState);
//        //setContentView(R.layout.main);
//        initBluetooth();
//        if(btAdapter==null){
//        	Toast.makeText(getApplicationContext(), "No bluetooth detected", 0).show();
//        	finish();
//        }
//        else{
//        	if(!btAdapter.isEnabled()){
//        		turnOnBT();
//        	}
//        	
//        	getPairedDevices();
//        	startDiscovery();
//        } // end of BluetoothComm        
	
	//initialize Location PosFinal
	FinalPos = new Location();
	
	
	} // end of onCreate
	
	
	//Functions for BluetoothComm
	
	private void startDiscovery() {
		// TODO Auto-generated method stub
		btAdapter.cancelDiscovery();
		btAdapter.startDiscovery();
		
	}
	private void turnOnBT() {
		// TODO Auto-generated method stub
		Intent intent =new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
		startActivityForResult(intent, 1);
	}
	private void getPairedDevices() {
		// TODO Auto-generated method stub
		devicesArray = btAdapter.getBondedDevices();
		if(devicesArray.size()>0){
			for(BluetoothDevice device:devicesArray){
				pairedDevices.add(device.getName());
				
			}
		}
	}
	private void initBluetooth() {
		// TODO Auto-generated method stub
		listView=(ListView)findViewById(R.id.listView);
		//listView.setOnItemClickListener(this);
		listAdapter= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,0);
		listView.setAdapter(listAdapter);
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		pairedDevices = new ArrayList<String>();
		filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		devices = new ArrayList<BluetoothDevice>();
		listAdapter.clear();
		receiver = new BroadcastReceiver(){
			@Override
			public void onReceive(Context context, Intent intent) {
				// TODO Auto-generated method stub
				String action = intent.getAction();
				
				if(BluetoothDevice.ACTION_FOUND.equals(action)){
					BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
					devices.add(device);
					String s = "";
					for(int a = 0; a < pairedDevices.size(); a++){
						if(device.getName().equals(pairedDevices.get(a))){
							//append 
							s = "(Paired)";
							break;
						}
					}
			
					//listAdapter.add(device.getName()+" "+s+" "+"\n"+device.getAddress());
				
						Location FinalPosBT = BTLocalization(device);
						user.setRelativePosition(FinalPosBT.getX(), FinalPosBT.getY());
						
						if(flagLogText){ //Log BT Pos
						writeToTextFile(FinalPos.getX()+","+FinalPos.getY()+","+FinalPos.getAccurancy()+","+FinalPos.getProvider()+","+FinalPos.getTimestampmilis(),FinalPos.getProvider());
						}
					
				}
				
				else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){
					// run some code
					Toast.makeText(getApplicationContext(), "Discovery started ", Toast.LENGTH_SHORT).show();

				}
				else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){
					// run some code
					Toast.makeText(getApplicationContext(), "Discovery finished ", Toast.LENGTH_SHORT).show();					
//					for(BluetoothDevice device:devices){
//					BTLocalization(device);
//					user.setRelativePosition(FinalPos.getX(), FinalPos.getY());
//					writeToTextFile(FinalPos.getX()+","+FinalPos.getY()+","+FinalPos.getAccurancy()+","+FinalPos.getProvider()+","+FinalPos.getTimestampmilis(),FinalPos.getProvider());
//					}
				
				}
				else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
					if(btAdapter.getState() == btAdapter.STATE_OFF){
						turnOnBT();
					}
				}
		  
			}

			private Location BTLocalization(BluetoothDevice device) {
				// TODO Auto-generated method stub
				//new MeasuringPointDrawable(this, map, wr);
				
				for (int i = 0; i < map.getSubDrawables().size(); i++) {
					MultiTouchDrawable d = map.getSubDrawables().get(i);
					if (d instanceof BluetoothBeaconDrawable) {
						
						//Toast.makeText(this, "AP positions: " + d.getRelativeX() + " , " +d.getRelativeY() + " , " +  ((AccessPointDrawable) d).getAccessPoint().getBssid() + " , " + ((AccessPointDrawable) d).getAccessPoint().getLocation(), Toast.LENGTH_LONG).show();
						addedBT.add(((BluetoothBeaconDrawable) d).getAccessPoint());
					}
				}
				
				
				float xf = 0;
				float yf = 0;
								
						for(BluetoothBeacon tmpBT : addedBT){					
							if(tmpBT.getBssid().equals(device.getAddress())){
									//if(result.getLevel() > -70){
											xf=tmpBT.getLocation().getX();
											yf=tmpBT.getLocation().getY();
											floorID = tmpBT.getCapabilities();
										}
							
						 }
			
				//WeightedCentroidTrilateration wc = new WeightedCentroidTrilateration(context, site);
				//BTPos = wc.calculateUserPositionWiFi(wr, addedAP);
				
				 //xf = (float) BTPos.x;
				// yf = (float) BTPos.y;
				
				 if(flagQuickScan){
				 user.setRelativePosition(xf, yf);
				//create final position/location object
					FinalPos.setAccurancy(errorBT);
					FinalPos.setX(xf);
					FinalPos.setY(yf);
					Date currentDate = new Date(System.currentTimeMillis());
					FinalPos.setTimestamp(currentDate);
					
					String posProvider=null;
					if(flagDR && !flagWF && !flagBT){
						posProvider="Dead Reckoning";
						
					}else if(!flagDR && flagWF && !flagBT){
						posProvider="WiFi AP positioning";
					}else if(!flagDR && !flagWF && flagBT){
						posProvider="Bluetooth Beacon Positioning";
					}else if(flagDR && flagWF && !flagBT){
						posProvider="DR + WiF+Error10";
					}else if(flagDR && !flagWF && flagBT){
						posProvider="DR + Bluetooth";
					}else if(!flagDR && flagWF && flagBT){
						posProvider="WiFi + Bluetooth";
					}else if(flagDR && flagWF && flagBT){
						posProvider="DR + WiFi + Bluetooth";
					}
					
					FinalPos.setProvider(posProvider);
					
				 }else if((flagBT) && (xf != 0) && (yf != 0) &&(!flagQuickScan)){ //  && (xf != 0) && (yf != 0) 
					 
					 
					 float errorPrediction = 1;
					 if(flagDR){
						 errorPrediction = errorDR;
					 }else{
						 errorPrediction = errorBT;
					 }
				
				
					 sensorFusionFilter(xf, yf, "kalman", errorPrediction, errorBT);
				//user.setRelativePosition(FinalPos.getX(), FinalPos.getY());
				DRAccStep=0; //we initialize the DR steps count to check the DR accuracy (5% of travelled distance)
	
				}
				
				flagQuickScan = false;
						
				Toast.makeText(getApplicationContext(), "BTPos Float X: " + xf, Toast.LENGTH_SHORT).show();
				Toast.makeText(getApplicationContext(), "BTPos Float Y: " + yf, Toast.LENGTH_SHORT).show();
				  
				
				user.bringToFront();

				if(floorID.equals("0")){
					
					RadioButton bfloor1 = (RadioButton) findViewById(R.id.floor1);
		            bfloor1.setChecked(true);
					
				}else if(floorID.equals("1")){
					
					RadioButton bfloor2 = (RadioButton) findViewById(R.id.floor2);
		            bfloor2.setChecked(true);
					
				}else if(floorID.equals("2")){
					
					RadioButton bfloor3 = (RadioButton) findViewById(R.id.floor3);
		            bfloor3.setChecked(true);
					
				}
				return FinalPos;
				
			} // BTLocalization
		};
		
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
		registerReceiver(receiver, filter);
		 filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
		registerReceiver(receiver, filter);
	}

	protected void initUI() {

		// ((Button) findViewById(R.id.project_site_reset_zoom_button)).setOnClickListener(this);

		// ((Button) findViewById(R.id.project_site_snap_user_button)).setOnClickListener(this);

		((Button) findViewById(R.id.project_site_start_wifiscan_button)).setOnClickListener(this);

		// ((Button) findViewById(R.id.project_site_calculate_ap_positions_button)).setOnClickListener(this);

		// ((Button) findViewById(R.id.project_site_add_known_ap)).setOnClickListener(this);

		((Button) findViewById(R.id.project_site_step_detect)).setOnClickListener(this);
		
		((Button) findViewById(R.id.project_reset_dr)).setOnClickListener(this);
		
		//((Button) findViewById(R.id.project_test)).setOnClickListener(this);
		
		((ToggleButton) findViewById(R.id.project_site_toggle_autorotate)).setOnClickListener(this);
		
		((RadioButton) findViewById(R.id.floor1)).setOnClickListener(this);
		
		((RadioButton) findViewById(R.id.floor2)).setOnClickListener(this);

		((RadioButton) findViewById(R.id.floor3)).setOnClickListener(this);

		((CheckBox) findViewById(R.id.checkWIFI)).setOnClickListener(this);
		
		((CheckBox) findViewById(R.id.checkBT)).setOnClickListener(this);

		((CheckBox) findViewById(R.id.checkDR)).setOnClickListener(this);
		
		((CheckBox) findViewById(R.id.checkLogText)).setOnClickListener(this);
		
		//((CheckBox) findViewById(R.id.addKnownCheckBT)).setOnClickListener(this);

		
		//((CheckBox) findViewById(R.id.addKnownCheckBT)).setOnClickListener(R.layout.project_site_dialog_add_known_ap);

		multiTouchView = ((MultiTouchView) findViewById(R.id.project_site_resultview));
		multiTouchView.setRearrangable(false);

		multiTouchView.addDrawable(map);

		if (site.getTitle().equals(ProjectSite.UNTITLED)) {
			showDialog(DIALOG_TITLE);
		} else {
			if (freshSite) {
				// start configuration dialog
				showDialog(DIALOG_FRESH_SITE);
			}
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (databaseHelper != null) {
			OpenHelperManager.releaseHelper();
			databaseHelper = null;
		}
		WifiScanner.stopScanning(this);
		
		try{
			unregisterReceiver(receiver); //unregister Bluetooth receiver
			}catch(Exception e){
				
			}

	}

	@Override
	protected void onResume() {
		super.onResume();
		log.debug("setting context");

		multiTouchView.loadImages(this);
		map.load();
		// stepDetectionProvider.start();

		if (walkingAndScanning) {
			setWalkingAndScanning(true,true);
		}
	}

	@Override
	public void onClick(View v) {
		switch (v.getId()) {

		case R.id.project_site_start_wifiscan_button:
//			Logger.d("start a wifiscan");
//			
//			flagQuickScan = true;
//		
//			if(flagWF){
//				try {
//					startWifiScan();
//					showDialog(DIALOG_SCANNING);
//
//				} catch (WifiException e) {
//					Logger.e("could not start wifi scan!", e);
//					Toast.makeText(this, R.string.project_site_wifiscan_start_failed, Toast.LENGTH_LONG).show();
//				}
//			}
//			
//			if(flagBT){
//				//for the BluetoothComm
//				
//				//super.onCreate(savedInstanceState);
//		        //setContentView(R.layout.main);
//		        initBluetooth();
//		        if(btAdapter==null){
//		        	Toast.makeText(getApplicationContext(), "No bluetooth detected", 0).show();
//		        	finish();
//		        }
//		        else{
//		        	if(!btAdapter.isEnabled()){
//		        		turnOnBT();
//		        	}
//		        	
//		        	getPairedDevices();
//		        	startDiscovery();
//		        } // end of BluetoothComm   
//			}
			

			break;

		// case R.id.project_site_calculate_ap_positions_button:
		//
		// final ProgressDialog triangulationProgress = new ProgressDialog(this);
		// triangulationProgress.setTitle(R.string.project_site_triangulation_progress_title);
		// triangulationProgress.setMessage(getString(R.string.project_site_triangulation_progress_message));
		// triangulationProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		// triangulationProgress.setButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
		// public void onClick(DialogInterface dialog, int whichButton) {
		// // Canceled.
		// if (triangulationTask != null) {
		// triangulationTask.cancel(true);
		// }
		// }
		// });
		//
		// triangulationTask = new TriangulationTask(this, triangulationProgress);
		//
		// triangulationProgress.show();
		// triangulationTask.execute();
		// break;

		case R.id.project_site_add_known_ap:
			showDialog(DIALOG_ADD_KNOWN_AP);
			break;

		case R.id.project_test:
			if(testv == 0){
				
				Toast.makeText(this, "gridX:  " + site.getGridSpacingX(), Toast.LENGTH_LONG).show();
				Toast.makeText(this, "gridY:  " + site.getGridSpacingY(), Toast.LENGTH_LONG).show();
   testv=0;				
				
				
//				for (int i = 0; i < map.getSubDrawables().size(); i++) {
//					MultiTouchDrawable d = map.getSubDrawables().get(i);
//					if (d instanceof AccessPointDrawable) {
//						
//						Toast.makeText(this, "AP positions: " + d.getRelativeX() + " , " +d.getRelativeY() + " , " +  ((AccessPointDrawable) d).getAccessPoint().getBssid() + " , " + ((AccessPointDrawable) d).getAccessPoint().getLocation(), Toast.LENGTH_LONG).show();
//						
//					}
//				}
				
//			try {
//				site.deleteScanResults();
//			} catch (SQLException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
			//user.setRelativePosition(602, 668);
			//user.setRelativePosition(80*site.getGridSpacingX(), 92*site.getGridSpacingY());
			
			writeToTextFile("Esta es una prueba del textFile", "prueba");
			Toast.makeText(this, "Se escribi— el archivo de texto en sdcard/log.file ", Toast.LENGTH_SHORT).show();
			
			testv = testv +1;
			}else if(testv == 1){
            Toast.makeText(this, "AddedAP size is: " + (int)addedAP.size(), Toast.LENGTH_SHORT).show();
			testv = testv + 1;	
			}else if(testv == 2){
			user.setRelativePosition(555, 992);
			testv ++;	
			}else if(testv == 3){
			Toast.makeText(this, "WiFi is: " + user.getRelativeX() + " , " + user.getRelativeY(), Toast.LENGTH_SHORT).show();
			testv = 0;	
			}
			break;
			
			
			
		case R.id.project_site_step_detect:

			setWalkingAndScanning(!walkingAndScanning,true);
			walkingAndScanning = !walkingAndScanning;
		
			
			break;
			
			case R.id.project_reset_dr:
				
		        rounds++;
                refPoint = 1;
                stepsGlobal = 0;	
			map.deleteAllSteps();
			//ScanResults.delete;
			
		    try {
				site.deleteScanResults();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				log.error("couldn't delete the scan results");
				//e.printStackTrace();
			}
//			multiTouchView.unloadImages();
//			map.unload();
			
			// delete all old messurements
			for (int i = 0; i < map.getSubDrawables().size(); i++) {
				MultiTouchDrawable d = map.getSubDrawables().get(i);
				if (d instanceof MeasuringPointDrawable) {
					map.removeSubDrawable(d);
					i--;
				}
			}
			
			break;	

		case R.id.project_site_toggle_autorotate:

			ToggleButton button = (ToggleButton) findViewById(R.id.project_site_toggle_autorotate);
           
			
			if (button.isChecked()) {
				map.startAutoRotate();
				Logger.d("Started autorotate.");
			} else {
				map.stopAutoRotate();
				Logger.d("Stopped autorotate.");
			}

			break;
		case R.id.floor1:
            floorID = "0";
            RadioButton bfloor1 = (RadioButton) findViewById(R.id.floor1);
            bfloor1.setChecked(true);
			Toast.makeText(this, " Currently in floor " + floorID, Toast.LENGTH_SHORT).show();
	        break;
		case R.id.floor2:
			floorID = "1";
			RadioButton bfloor2 = (RadioButton) findViewById(R.id.floor2);
            bfloor2.setChecked(true);
			Toast.makeText(this, " Currently in floor " + floorID, Toast.LENGTH_SHORT).show();
	        break;
		case R.id.floor3:
			floorID = "2";
			RadioButton bfloor3 = (RadioButton) findViewById(R.id.floor3);
            bfloor3.setChecked(true);
			Toast.makeText(this, " Currently in floor " + floorID, Toast.LENGTH_SHORT).show();
	        break;
		case R.id.checkWIFI:

            CheckBox checkButtonWIFI = (CheckBox) findViewById(R.id.checkWIFI);
            
            
			if (checkButtonWIFI.isChecked()) {
				flagWF = true;
				Toast.makeText(this, " WIFI localization is on", Toast.LENGTH_SHORT).show();
				Logger.d("WIFI localization is on.");
			} else {
				flagWF = false;
				Toast.makeText(this, " WIFI localization is off", Toast.LENGTH_SHORT).show();
				Logger.d("WIFI localization is off");
			}
			
			
			
	        break;
		case R.id.checkBT:
			
		    CheckBox checkButtonBT = (CheckBox) findViewById(R.id.checkBT);
			
 		    
					if (checkButtonBT.isChecked()) {
						flagBT = true;
						Toast.makeText(this, " Bluetooth localization is on", Toast.LENGTH_SHORT).show();
						Logger.d("Bluetooth localization is on.");
					} else {
						flagBT = false;
						Toast.makeText(this, " Bluetooth localization is off", Toast.LENGTH_SHORT).show();
						Logger.d("Bluetooth localization is off");
					}
					
	        break;
		case R.id.checkDR:

			   CheckBox checkButtonDR = (CheckBox) findViewById(R.id.checkDR);
				
				if (checkButtonDR.isChecked()) {
					flagDR = true;
					Toast.makeText(this, "DR localization is on", Toast.LENGTH_SHORT).show();
					Logger.d("DR localization is on.");
				} else {
					flagDR = false;
					Toast.makeText(this, "DR localization is off", Toast.LENGTH_SHORT).show();
					Logger.d("DR localization is off");
				}
			
	        break;
	        
		case R.id.checkLogText:

			   CheckBox checkButtonLogText = (CheckBox) findViewById(R.id.checkLogText);
				
				if (checkButtonLogText.isChecked()) {
					flagLogText = true;
					Toast.makeText(this, "LogText is on", Toast.LENGTH_SHORT).show();
					Logger.d("LogText is on.");
				} else {
					flagLogText = false;
					Toast.makeText(this, "LogText is off", Toast.LENGTH_SHORT).show();
					Logger.d("LogText is off");
				}
			
	        break;
	        
		}
	}

	private void writeToTextFile(String text, String textFileName) {
		// TODO Auto-generated method stub
		Time today = new Time(Time.getCurrentTimezone());
		today.setToNow();
		String myDate = Integer.toString(today.year)+"-"+Integer.toString(today.month)+"-"+Integer.toString(today.monthDay)+"-"+Integer.toString(today.hour);
		//String myDate = java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
   
		File logFile = new File("sdcard/"+textFileName+"Final"+".file");
		   if (!logFile.exists())
		   {
		      try
		      {
		         logFile.createNewFile();
		      } 
		      catch (IOException e)
		      {
		         // TODO Auto-generated catch block
		         e.printStackTrace();
		      }
		   }
		   try
		   {
		      //BufferedWriter for performance, true to set append to file flag
		      BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true)); 
		      buf.append(text);
		      buf.newLine();
		      buf.close();
		   }
		   catch (IOException e)
		   {
		      // TODO Auto-generated catch block
		      e.printStackTrace();
		   }
		
		
	}


	protected void setWalkingAndScanning(boolean shouldRun,boolean ui) {
		if (!shouldRun) {
			// stop!

			if (stepDetectionProvider.isRunning())
				stepDetectionProvider.stop();
			if (scheduledTask != null) {
				scheduledTask.cancel(false);
				scheduledTask = null;
			}
			stopWifiScan();

			if(ui)
			((Button) findViewById(R.id.project_site_step_detect)).setText(R.string.project_site_start_step_detect);

			persistScanResults(ui);

		} else {
			// start
			unsavedScanResults = new ArrayList<WifiScanResult>();

			if (!stepDetectionProvider.isRunning()) {
				stepDetectionProvider.start();
			}

			if (scheduledTask == null) {
				scheduledTask = scheduler.scheduleWithFixedDelay(wifiRunnable, 0, (schedulerTime<=0?1:schedulerTime), TimeUnit.SECONDS);
			}
			if(ui)
			((Button) findViewById(R.id.project_site_step_detect)).setText(R.string.project_site_stop_step_detect);
			
			
			
		}
	}

	/**
	 * 
	 */
	protected void persistScanResults(boolean dialog) {
		if (dialog) {
			final ProgressDialog persistProgress = new ProgressDialog(this);

			final WifiScanResultPersistTask persistTask = new WifiScanResultPersistTask(this, persistProgress);

			// create dialog and run asynctask
			persistProgress.setTitle(R.string.project_site_scanresults_persisting_title);
			persistProgress.setMessage(getString(R.string.project_site_scanresults_persisting_message));
			persistProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);

			persistProgress.setButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					if (persistTask != null) {
						persistTask.cancel(true);
					}
				}
			});

			persistProgress.show();
			persistTask.execute();
		} else {
			WifiScanResultPersistTask persistTask = new WifiScanResultPersistTask(this, null);
			persistTask.execute();
		}
	}

	protected void addKnownAP(String bssid, String ssid, String floorID) {
		Location curLocation = LocationServiceFactory.getLocationService().getLocation();
		
		
		AccessPoint ap = new AccessPoint();
	
		BluetoothBeacon bt = new BluetoothBeacon();
		
		if(!AddKnownFlagBT){		
		ap.setBssid(bssid);
		ap.setSsid(ssid);
		ap.setLocation(curLocation);
		ap.setCapabilities("");
		ap.setCalculated(false);
		ap.setProjectSite(site);
		ap.setCapabilities(floorID);
		
		new AccessPointDrawable(this, map, ap);
		
	//addedAP.add(ap);
		
		try {
			databaseHelper.getDao(Location.class).create(curLocation);
			databaseHelper.getDao(AccessPoint.class).create(ap);
		} catch (SQLException e) {
			Logger.e("could not create ap", e);

		}
		
	}else if(AddKnownFlagBT){
		
		bt.setBssid(bssid);
		bt.setSsid(ssid);
		bt.setLocation(curLocation);
		bt.setCapabilities("");
		bt.setCalculated(false);
		bt.setProjectSite(site);
		bt.setCapabilities(floorID);
		
		new BluetoothBeaconDrawable(this, map, bt);
		
	//addedAP.add(ap);
		
		try {
			databaseHelper.getDao(Location.class).create(curLocation);
			databaseHelper.getDao(BluetoothBeacon.class).create(bt);
		} catch (SQLException e) {
			Logger.e("could not create bt", e);

		}
		
		
	} // end for the if addKnownFlag BT to know if add bluetooth or WiFi Drawable
		
		
	
		

		multiTouchView.invalidate();

	}

	protected void setCalculatedAccessPoints(Vector<AccessPointDrawable> aps) {
		// delete all old messurements
		for (int i = 0; i < map.getSubDrawables().size(); i++) {
			MultiTouchDrawable d = map.getSubDrawables().get(i);
			if (d instanceof AccessPointDrawable && ((AccessPointDrawable) d).isCalculated()) {
				map.removeSubDrawable(d);
				addedAP = null;
				i--;
			}
		}

		try {
			Dao<AccessPoint, Integer> apDao = databaseHelper.getDao(AccessPoint.class);
			Dao<Location, Integer> locDao = databaseHelper.getDao(Location.class);

			for (AccessPoint ap : site.getAccessPoints()) {
				try {
					if (ap.isCalculated())
						apDao.delete(ap);
				} catch (Exception e) {

				}
			}

			for (AccessPointDrawable ap : aps) {

				locDao.createIfNotExists(ap.getAccessPoint().getLocation());
				ap.getAccessPoint().setProjectSite(site);
				apDao.createOrUpdate(ap.getAccessPoint());
			}

			projectSiteDao.refresh(site);

		} catch (SQLException e) {
			Logger.e("could not delete old or create new ap results", e);
		}

		for (AccessPointDrawable ap : aps) {
			map.addSubDrawable(ap);
			map.recalculatePositions();

		}
		multiTouchView.invalidate();
	}

	protected void setScaleOfMap(float scale) {
		float mapScale = scalerDistance / scale;
		site.setGridSpacingX(mapScale);
		site.setGridSpacingY(mapScale);
		LocationServiceFactory.getLocationService().setGridSpacing(site.getGridSpacingX(), site.getGridSpacingY());
		MultiTouchDrawable.setGridSpacing(mapScale, mapScale);
		multiTouchView.invalidate();
		Toast.makeText(this, getString(R.string.project_site_mapscale_changed, mapScale), Toast.LENGTH_SHORT).show();

		if (freshSite) {
			showDialog(DIALOG_ASK_FOR_NORTH);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onCreateDialog(int)
	 */
	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case DIALOG_TITLE:
			AlertDialog.Builder titleAlert = new AlertDialog.Builder(this);

			titleAlert.setTitle(R.string.project_site_dialog_title_title);
			titleAlert.setMessage(R.string.project_site_dialog_title_message);

			// Set an EditText view to get user input
			final EditText input = new EditText(this);
			input.setSingleLine(true);
			titleAlert.setView(input);

			titleAlert.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					setSiteTitle(input.getText().toString());
					if (freshSite) {
						showDialog(DIALOG_FRESH_SITE);
					}
				}
			});

			titleAlert.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			return titleAlert.create();

		case DIALOG_SCANNING:

			AlertDialog.Builder scanAlert = new AlertDialog.Builder(this);

			scanAlert.setTitle(R.string.project_site_dialog_scanning_title);
			scanAlert.setMessage(R.string.project_site_dialog_scanning_message);

			scanningImageView = new ImageView(this);
			scanningImageView.setImageResource(R.drawable.loading);

			scanAlert.setView(scanningImageView);

			scanAlert.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					stopWifiScan();
				}
			});

			scanAlertDialog = scanAlert.create();

			scanAlertDialog.setOnShowListener(new OnShowListener() {

				@Override
				public void onShow(DialogInterface paramDialogInterface) {
					if (scanningImageView != null) {
						((AnimationDrawable) scanningImageView.getDrawable()).start();
					} else {
						Logger.w("why is scanningImageView null????");
					}
				}
			});

			return scanAlertDialog;

		case DIALOG_CHANGE_SIZE:
			AlertDialog.Builder sizeAlert = new AlertDialog.Builder(this);

			sizeAlert.setTitle(R.string.project_site_dialog_size_title);
			sizeAlert.setMessage(R.string.project_site_dialog_size_message);

			sizeAlert.setView(getLayoutInflater().inflate(R.layout.project_site_dialog_change_size, (ViewGroup) getCurrentFocus()));

			sizeAlert.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					try {
						int w = Integer.parseInt(((EditText) ((AlertDialog) dialog).findViewById(R.id.project_site_dialog_change_size_width))
								.getText().toString()), h = Integer.parseInt(((EditText) ((AlertDialog) dialog)
								.findViewById(R.id.project_site_dialog_change_size_height)).getText().toString());
						if (w <= 0) {
							throw new NumberFormatException("width has to be larger than 0");
						}
						if (h <= 0) {
							throw new NumberFormatException("height has to be larger than 0");
						}

						map.setSize(w, h);
						site.setSize(w, h);

						multiTouchView.invalidate();
						Toast.makeText(context, context.getString(R.string.project_site_dialog_size_finished, w, h), Toast.LENGTH_SHORT).show();

						saveProjectSite();
					} catch (NumberFormatException e) {
						Logger.w("change size width or height not a number ", e);
						Toast.makeText(context, context.getString(R.string.project_site_dialog_size_nan), Toast.LENGTH_LONG).show();
					}

				}
			});

			sizeAlert.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			return sizeAlert.create();

		case DIALOG_SET_BACKGROUND:

			AlertDialog.Builder bckgAlert = new AlertDialog.Builder(this);
			bckgAlert.setTitle(R.string.project_site_dialog_background_title);
			bckgAlert.setMessage(R.string.project_site_dialog_background_message);

			LinearLayout bckgLayout = new LinearLayout(this);
			bckgLayout.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
			bckgLayout.setGravity(Gravity.CENTER);
			bckgLayout.setOrientation(LinearLayout.VERTICAL);
			bckgLayout.setPadding(5, 5, 5, 5);

			final TextView pathTextView = new TextView(this);
			backgroundPathTextView = pathTextView;
			pathTextView.setText(R.string.project_site_dialog_background_default_path);
			pathTextView.setPadding(10, 0, 10, 10);

			bckgLayout.addView(pathTextView);

			Button pathButton = new Button(this);
			pathButton.setText(R.string.project_site_dialog_background_path_button);
			pathButton.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View v) {
					Intent i = new Intent(context, FileBrowser.class);
					i.putExtra(FileBrowser.EXTRA_MODE, FileBrowser.MODE_LOAD);
					i.putExtra(FileBrowser.EXTRA_ALLOWED_EXTENSIONS, "jpg,png,gif,jpeg,bmp");
					startActivityForResult(i, FILEBROWSER_REQUEST);
				}

			});

			bckgLayout.addView(pathButton);

			bckgAlert.setView(bckgLayout);

			bckgAlert.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					setBackgroundImage(pathTextView.getText().toString());
					if (freshSite) {
						showDialog(DIALOG_ASK_CHANGE_SCALE);
					}
				}
			});

			bckgAlert.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					freshSite = false;
				}
			});

			Dialog bckgDialog = bckgAlert.create();
			bckgDialog.setCanceledOnTouchOutside(true);

			return bckgDialog;

		case DIALOG_SET_SCALE_OF_MAP:
			AlertDialog.Builder scaleOfMapDialog = new AlertDialog.Builder(this);

			scaleOfMapDialog.setTitle(R.string.project_site_dialog_scale_of_map_title);
			scaleOfMapDialog.setMessage(R.string.project_site_dialog_scale_of_map_message);

			// Set an EditText view to get user input
			final EditText scaleInput = new EditText(this);
			scaleInput.setSingleLine(true);
			scaleInput.setRawInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
			scaleOfMapDialog.setView(scaleInput);

			scaleOfMapDialog.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					try {
						float value = Float.parseFloat(scaleInput.getText().toString());
						setScaleOfMap(value);
					} catch (NumberFormatException nfe) {
						Logger.w("Wrong number format format!");
						Toast.makeText(context, getString(R.string.not_a_number, scaleInput.getText()), Toast.LENGTH_SHORT).show();
					}
				}
			});

			scaleOfMapDialog.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			return scaleOfMapDialog.create();

		case DIALOG_ADD_KNOWN_AP:

			AlertDialog.Builder addAPAlert = new AlertDialog.Builder(this);

			addAPAlert.setTitle(R.string.project_site_dialog_add_known_ap_title);
			addAPAlert.setMessage(R.string.project_site_dialog_add_known_ap_message);

			addAPAlert.setView(getLayoutInflater().inflate(R.layout.project_site_dialog_add_known_ap, (ViewGroup) getCurrentFocus()));

			addAPAlert.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {

					String ssid = ((EditText) ((AlertDialog) dialog).findViewById(R.id.project_site_dialog_add_known_ap_ssid)).getText().toString();
					String bssid = ((EditText) ((AlertDialog) dialog).findViewById(R.id.project_site_dialog_add_known_ap_bssid)).getText().toString();
					String floor = ((EditText) ((AlertDialog) dialog).findViewById(R.id.project_site_dialog_add_known_ap_floor)).getText().toString();
					String Bluetooth = ((EditText) ((AlertDialog) dialog).findViewById(R.id.project_site_dialog_add_known_ap_bt)).getText().toString();
					String LoadIniAP = ((EditText) ((AlertDialog) dialog).findViewById(R.id.project_site_dialog_add_known_ap_IAP)).getText().toString();

					
					if (Bluetooth.equals("y") || Bluetooth.equals("Y")) {
						AddKnownFlagBT = true;
						Logger.d("Add known ap BT ckecked.");
					} else {
						AddKnownFlagBT = false;
						Logger.d("Bluetooth known ap BT not checked");
					}
					
					if (LoadIniAP.equals("y") || LoadIniAP.equals("Y")) {
						flagKnownAP = true;
						Logger.d("Yes, load the inicial APs.");
					} else {
						flagKnownAP = false;
						Logger.d("Don't load the inicial APs");
					}
					
//					 CheckBox checkAddButtonBT = (CheckBox)Dialog.findViewById(R.id.addKnownCheckBT);
//			 		    
//						if (checkAddButtonBT.isChecked()) {
//							AddKnownFlagBT = true;
//							Logger.d("Add known ap BT ckecked.");
//						} else {
//							AddKnownFlagBT = false;
//							Logger.d("Bluetooth known ap BT not checked");
//						}
					
					addKnownAP(bssid, ssid, floor);
					
					float tmpX = user.getRelativeX(); 
					float tmpY = user.getRelativeY();
					
					if(flagKnownAP){
					
					if(!AddKnownFlagBT){
					//Add the known AP from the data collected
					if(floorID.equals("0")){
	
						
						
						
						
						
//						user.setRelativePosition(555, 992);	
//					addKnownAP("00:12:43:4e:2b:d0", "WLANFB02", "0");
//					addKnownAP("00:12:43:4e:2b:d1", "WIPS", "0");
//					addKnownAP("00:12:43:4e:2b:d2", "WIPSecure", "0");
//					
//					user.setRelativePosition(621, 822);
//					addKnownAP("00:11:20:68:83:a0", "WLANFB02", "0");
//					addKnownAP("00:11:20:68:83:a1", "WIPS", "0");
//					addKnownAP("00:11:20:68:83:a2", "WIPSecure", "0");
//					
//					user.setRelativePosition(602, 668);
//					addKnownAP("00:11:20:2a:4d:e0", "WLANFB02", "0");
//					addKnownAP("00:11:20:2a:4d:e1", "WIPS", "0");
//					addKnownAP("00:11:20:2a:4d:e2", "WIPSecure", "0");
//					
//					user.setRelativePosition(523, 603);
//					addKnownAP("00:11:20:2a:56:70", "WLANFB02", "0");
//					addKnownAP("00:11:20:2a:56:71", "WIPS", "0");
//					addKnownAP("00:11:20:2a:56:72", "WIPSecure", "0");
//					
//					user.setRelativePosition(377, 663);
//					addKnownAP("00:11:20:2a:58:10", "WLANFB02", "0");
//					addKnownAP("00:11:20:2a:58:11", "WIPS", "0");
//					addKnownAP("00:11:20:2a:58:12", "WIPSecure", "0");
//					
//					user.setRelativePosition(401, 923);
//					addKnownAP("00:11:20:2a:55:80", "WLANFB02", "0");
//					addKnownAP("00:11:20:2a:55:81", "WIPS", "0");
//					addKnownAP("00:11:20:2a:55:82", "WIPSecure", "0");
				
				          user.setRelativePosition(100*map.getGridSpacingX(),
				        		  156*map.getGridSpacingY());
				        		                                                  addKnownAP("d4:ca:6d:4f:91:5e", "robocup-lab", "0");

				        		                                                  user.setRelativePosition(100*map.getGridSpacingX(),
				        		  150*map.getGridSpacingY());
				        		                                                  addKnownAP("bc:05:43:b5:5b:fd", "care-o-bot-developers", "0");

				        		                                                  user.setRelativePosition(98*map.getGridSpacingX(),
				        		  126*map.getGridSpacingY());
				        		                                                  addKnownAP("00:0e:a6:5b:52:8a", "IVC", "0");

				        		                                                  user.setRelativePosition(98*map.getGridSpacingX(),
				        		  112*map.getGridSpacingY());
				        		                                                  addKnownAP("00:14:bf:a5:73:6a", "C055", "0");

				        		                                                  user.setRelativePosition(98*map.getGridSpacingX(),
				        		  114*map.getGridSpacingY());
				        		                                                  addKnownAP("00:0b:6b:b0:7e:02", "C055-N", "0");

				        		                                                  user.setRelativePosition(56*map.getGridSpacingX(),
				        		  96*map.getGridSpacingY());
				        		                                                  addKnownAP("c8:d7:19:d4:6a:e5", "AStAWLan", "0");

				        		                                                  user.setRelativePosition(56*map.getGridSpacingX(),
				        		  130*map.getGridSpacingY());
				        		                                                  addKnownAP("00:0b:6b:b0:7e:5d", "C015-N", "0");

				        		                                                  user.setRelativePosition(65*map.getGridSpacingX(),
				        		  145*map.getGridSpacingY());
				        		                                                  addKnownAP("5c:d9:98:08:7b:3f", "adgj", "0");

				        		                                                  user.setRelativePosition(65*map.getGridSpacingX(),
				        		  155*map.getGridSpacingY());
				        		                                                  addKnownAP("24:65:11:7e:94:71", "youbot-developers", "0");	
						
						
					
					}else if (floorID.equals("1")){
					
						user.setRelativePosition(555, 992);	
						addKnownAP("00:12:43:4e:2b:d0", "WLANFB02", "0");
						addKnownAP("00:12:43:4e:2b:d1", "WIPS", "0");
						addKnownAP("00:12:43:4e:2b:d2", "WIPSecure", "0");
						
						user.setRelativePosition(621, 822);
						addKnownAP("00:11:20:68:83:a0", "WLANFB02", "0");
						addKnownAP("00:11:20:68:83:a1", "WIPS", "0");
						addKnownAP("00:11:20:68:83:a2", "WIPSecure", "0");
						
						user.setRelativePosition(602, 668);
						addKnownAP("00:11:20:2a:4d:e0", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:4d:e1", "WIPS", "0");
						addKnownAP("00:11:20:2a:4d:e2", "WIPSecure", "0");
						
						user.setRelativePosition(523, 603);
						addKnownAP("00:11:20:2a:56:70", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:56:71", "WIPS", "0");
						addKnownAP("00:11:20:2a:56:72", "WIPSecure", "0");
						
						user.setRelativePosition(377, 663);
						addKnownAP("00:11:20:2a:58:10", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:58:11", "WIPS", "0");
						addKnownAP("00:11:20:2a:58:12", "WIPSecure", "0");
						
						user.setRelativePosition(401, 923);
						addKnownAP("00:11:20:2a:55:80", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:55:81", "WIPS", "0");
						addKnownAP("00:11:20:2a:55:82", "WIPSecure", "0");	
						
					}else if (floorID.equals("2")){
					
						user.setRelativePosition(555, 992);	
						addKnownAP("00:12:43:4e:2b:d0", "WLANFB02", "0");
						addKnownAP("00:12:43:4e:2b:d1", "WIPS", "0");
						addKnownAP("00:12:43:4e:2b:d2", "WIPSecure", "0");
						
						user.setRelativePosition(621, 822);
						addKnownAP("00:11:20:68:83:a0", "WLANFB02", "0");
						addKnownAP("00:11:20:68:83:a1", "WIPS", "0");
						addKnownAP("00:11:20:68:83:a2", "WIPSecure", "0");
						
						user.setRelativePosition(602, 668);
						addKnownAP("00:11:20:2a:4d:e0", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:4d:e1", "WIPS", "0");
						addKnownAP("00:11:20:2a:4d:e2", "WIPSecure", "0");
						
						user.setRelativePosition(523, 603);
						addKnownAP("00:11:20:2a:56:70", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:56:71", "WIPS", "0");
						addKnownAP("00:11:20:2a:56:72", "WIPSecure", "0");
						
						
						addKnownAP("00:11:20:2a:58:10", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:58:11", "WIPS", "0");
						addKnownAP("00:11:20:2a:58:12", "WIPSecure", "0");
						
						user.setRelativePosition(401, 923);
						addKnownAP("00:11:20:2a:55:80", "WLANFB02", "0");
						addKnownAP("00:11:20:2a:55:81", "WIPS", "0");
						addKnownAP("00:11:20:2a:55:82", "WIPSecure", "0");
						
					 }
					}else if(AddKnownFlagBT){
						user.setRelativePosition(68*map.getGridSpacingX(), 101*map.getGridSpacingY());
						addKnownAP("00:17:CA:98:5C:13","Dell Streak", "0");
						user.setRelativePosition(85*map.getGridSpacingX(), 100*map.getGridSpacingY());
						addKnownAP("00:17:CA:98:01:9F","Dell Streak 3", "0");
						//user.setRelativePosition(602, 698);
						//addKnownAP("00:21:D2:FA:67:E3","SGH-i900", "0");
						user.setRelativePosition(92*map.getGridSpacingX(), 98*map.getGridSpacingY());
						addKnownAP("40:2C:F4:55:FF:51","ubuntu-0", "0");
						user.setRelativePosition(81*map.getGridSpacingX(), 102*map.getGridSpacingY());
						addKnownAP("58:1F:AA:B9:77:0B","caramelo", "0");
						user.setRelativePosition(98*map.getGridSpacingX(), 137*map.getGridSpacingY());
						addKnownAP("00:06:66:06:1D:47","FireflyBP-1D47", "0");
						user.setRelativePosition(96*map.getGridSpacingX(), 130*map.getGridSpacingY());
						addKnownAP("60:FB:42:82:E6:B8","Joses MacBookPro", "0");
						user.setRelativePosition(86*map.getGridSpacingX(), 108*map.getGridSpacingY());
						addKnownAP("00:1B:10:00:08:D1","adrastea-0", "0");
						user.setRelativePosition(100*map.getGridSpacingX(), 109*map.getGridSpacingY());
						addKnownAP("00:01:95:0C:94:95","CV-FIVIS-02", "0");
						user.setRelativePosition(95*map.getGridSpacingX(), 110*map.getGridSpacingY());
						addKnownAP("00:0F:B3:18:AE:58","CG-Tablet-0", "0");
						user.setRelativePosition(95*map.getGridSpacingX(), 125*map.getGridSpacingY());
						addKnownAP("10:93:E9:0A:90:31","MacBookAir", "0");
						user.setRelativePosition(87*map.getGridSpacingX(), 140*map.getGridSpacingY());
						addKnownAP("B8:FF:61:69:5C:B7","Valle", "0");
						user.setRelativePosition(87*map.getGridSpacingX(), 115*map.getGridSpacingY());
						addKnownAP("00:1B:10:00:17:E6","elara-0", "0");
						user.setRelativePosition(87*map.getGridSpacingX(), 122*map.getGridSpacingY());
						addKnownAP("00:1B:10:00:17:88","koopa", "0");
						user.setRelativePosition(98*map.getGridSpacingX(), 153*map.getGridSpacingY());
						addKnownAP("00:DB:DF:04:0F:AF","eliza-0", "0");
					        
					}
					
					
					flagKnownAP = false;
					AddKnownFlagBT = false;
					
					} //add the knownAP for first time if
					
					user.setRelativePosition(tmpX, tmpY);


				}
			});

			addAPAlert.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			return addAPAlert.create();

		case DIALOG_SELECT_BSSIDS:

			AlertDialog.Builder selectBssidsDialog = new AlertDialog.Builder(this);

			LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
			View layout = inflater.inflate(R.layout.project_site_dialog_select_bssids,
					(ViewGroup) findViewById(R.id.project_site_dialog_select_bssids_root_layout));
			selectBssidsDialog.setView(layout);

			final SelectBssdidsExpandableListAdapter adapter = new SelectBssdidsExpandableListAdapter();

			selectBssidsDialog.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					site.setUnselectedBssids(adapter.getSelectedBssids(false));
					try {
						site.update();
					} catch (SQLException e) {
						Logger.e("Could not update project site", e);
					}
				}
			});
			selectBssidsDialog.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
				}
			});

			AlertDialog dialog = selectBssidsDialog.create();

			ExpandableListView listView = (ExpandableListView) layout.findViewById(R.id.project_site_dialog_select_bssids_list_view);
			adapter.initialize(dialog.getContext(), new ArrayList<String>(), new ArrayList<ArrayList<Bssid>>());

			Button selectAll = (Button) layout.findViewById(R.id.project_site_dialog_select_bssids_select_all_button);
			Button deselectAll = (Button) layout.findViewById(R.id.project_site_dialog_select_bssids_deselect_all_button);

			OnClickListener selectAllListener = new OnClickListener() {

				@Override
				public void onClick(View v) {
					boolean state = true;

					if (v.getId() == R.id.project_site_dialog_select_bssids_select_all_button)
						state = true;
					else
						state = false;

					adapter.selectAllChildren(state);
				}

			};

			selectAll.setOnClickListener(selectAllListener);
			deselectAll.setOnClickListener(selectAllListener);

			// Set this blank adapter to the list view
			listView.setAdapter(adapter);

			ForeignCollection<WifiScanResult> scanResults = site.getScanResults();
			ArrayList<Bssid> bssids = new ArrayList<Bssid>();

			for (WifiScanResult scanResult : scanResults) {
				Collection<BssidResult> bssidResults = scanResult.getBssids();

				for (BssidResult bssidResult : bssidResults) {
					Bssid bssid = new Bssid(bssidResult.getBssid(), bssidResult.getSsid());

					boolean alreadyAdded = false;

					for (Bssid tmpBssid : bssids) {
						if (tmpBssid.getBssid().equals(bssid.getBssid()) && tmpBssid.getSsid().equals(bssid.getSsid()))
							alreadyAdded = true;
					}

					if (!alreadyAdded) {
						bssid.setSelected(site.isBssidSelected(bssid.getBssid()));
						bssids.add(bssid);
					}
				}
			}

			adapter.addItems(bssids);

			return dialog;

		case DIALOG_FRESH_SITE:

			AlertDialog.Builder freshBuilder = new Builder(context);
			freshBuilder.setTitle(R.string.project_site_dialog_fresh_site_title);
			freshBuilder.setMessage(R.string.project_site_dialog_fresh_site_message);

			freshBuilder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					showDialog(DIALOG_SET_BACKGROUND);
				}

			});

			freshBuilder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					freshSite = false;
				}
			});

			return freshBuilder.create();

		case DIALOG_ASK_CHANGE_SCALE:

			AlertDialog.Builder askScaleBuilder = new Builder(context);
			askScaleBuilder.setTitle(R.string.project_site_dialog_ask_change_scale_title);
			askScaleBuilder.setMessage(R.string.project_site_dialog_ask_change_scale_message);

			askScaleBuilder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					scaleOfMap();
				}

			});

			askScaleBuilder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					freshSite = false;
				}
			});

			return askScaleBuilder.create();

		case DIALOG_ASK_FOR_NORTH:

			AlertDialog.Builder askNorthBuilder = new Builder(context);
			askNorthBuilder.setTitle(R.string.project_site_dialog_ask_north_title);
			askNorthBuilder.setMessage(R.string.project_site_dialog_ask_north_message);

			askNorthBuilder.setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					setMapNorth();
					freshSite = false;
				}

			});

			askNorthBuilder.setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					freshSite = false;
				}
			});

			return askNorthBuilder.create();

		case DIALOG_CHANGE_SCAN_INTERVAL:
			AlertDialog.Builder changeScanIntervalBuilder = new Builder(context);
			changeScanIntervalBuilder.setTitle(R.string.project_site_dialog_change_scan_interval_title);
			changeScanIntervalBuilder.setMessage(getString(R.string.project_site_dialog_change_scan_interval_message, schedulerTime));

			final SeekBar sb = new SeekBar(this);
			sb.setMax(60);
			sb.setProgress(schedulerTime);

			changeScanIntervalBuilder.setView(sb);

			changeScanIntervalBuilder.setPositiveButton(getString(R.string.button_ok), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					schedulerTime = sb.getProgress();
					if(schedulerTime==0) schedulerTime=1; // schedulerTime must not be 0
					getPreferences(MODE_PRIVATE).edit().putInt(SCAN_INTERVAL, schedulerTime).commit();
					if (walkingAndScanning) {
						// timer must be updated
						setWalkingAndScanning(false,true);
						setWalkingAndScanning(true,true);
					}
				}

			});

			changeScanIntervalBuilder.setNegativeButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.

				}
			});

			final AlertDialog changeScanIntervalDialog = changeScanIntervalBuilder.create();

			sb.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
					changeScanIntervalDialog.setMessage(context.getString(R.string.project_site_dialog_change_scan_interval_message, progress));
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

			});

			return changeScanIntervalDialog;

		default:
			return super.onCreateDialog(id);
		}
	}

	protected void setSiteTitle(String title) {
		site.setTitle(title);
		saveProjectSite();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.project_site, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		case R.id.project_site_menu_change_name:
			showDialog(DIALOG_TITLE);

			return false;

		case R.id.project_site_menu_save:
			saveProjectSite();
			return false;

		case R.id.project_site_menu_change_size:
			showDialog(DIALOG_CHANGE_SIZE);
			return false;

		case R.id.project_site_menu_set_background:
			showDialog(DIALOG_SET_BACKGROUND);
			return false;

		case R.id.project_site_menu_set_scale_of_map:

			if (scaler == null) {
				scaleOfMap();
			} else {
				// just hide the scalers, don't change the scaleing
				scaler.removeScaleSliders();
				map.removeSubDrawable(scaler);
				scaler = null;
				invalidate();
			}

			return false;

		case R.id.project_site_menu_set_north:

			setMapNorth();
			return false;

		case R.id.project_site_menu_select_bssids:

			this.showDialog(DIALOG_SELECT_BSSIDS);

			return false;

		case R.id.project_site_reset_zoom_button:
			Logger.d("resetting Zoom");
			multiTouchView.resetAllScale();
			multiTouchView.resetAllXY();
			multiTouchView.resetAllAngle();
			multiTouchView.recalculateDrawablePositions();
			multiTouchView.invalidate();
			break;

		case R.id.project_site_snap_user_button:
			Logger.d("Snapping user to grid");
			user.snapPositionToGrid();
			//userWiFi.snapPositionToGrid();
			multiTouchView.invalidate();
			break;

		case R.id.project_site_calculate_ap_positions_option:

			final ProgressDialog triangulationProgress = new ProgressDialog(this);
			triangulationProgress.setTitle(R.string.project_site_triangulation_progress_title);
			triangulationProgress.setMessage(getString(R.string.project_site_triangulation_progress_message));
			triangulationProgress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
			triangulationProgress.setButton(getString(R.string.button_cancel), new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					// Canceled.
					if (triangulationTask != null) {
						triangulationTask.cancel(true);
					}
				}
			});

			triangulationTask = new TriangulationTask(this, triangulationProgress);

			triangulationProgress.show();
			triangulationTask.execute();
			break;

		case R.id.project_site_add_known_ap:
			showDialog(DIALOG_ADD_KNOWN_AP);
			break;

		case R.id.project_site_menu_change_scan_interval:
			showDialog(DIALOG_CHANGE_SCAN_INTERVAL);
			break;
			
		case R.id.project_site_menu_track_steps:
			trackSteps=!trackSteps;
			if(trackSteps==false){
				// tracking disabled
				map.deleteAllSteps();
			}
			break;

		default:
			return super.onOptionsItemSelected(item);
		}

		return false;
	}

	/**
	 * 
	 */
	protected void setMapNorth() {
		if (northDrawable == null) {
			// Stop auto-rotate when map north is set
			((ToggleButton) findViewById(R.id.project_site_toggle_autorotate)).setChecked(false);
			map.stopAutoRotate();

			// create the icon the set the north
			northDrawable = new NorthDrawable(this, map, site) {

				/*
				 * (non-Javadoc)
				 * 
				 * @see at.fhstp.wificompass.view.NorthDrawable#onOk()
				 */
				@Override
				public void onOk() {
					super.onOk();
					northDrawable = null;
					site.setNorth(ToolBox.normalizeAngle(adjustmentAngle));
					map.setAngleAdjustment(site.getNorth());
					
					LocationServiceFactory.getLocationService().setRelativeNorth(site.getNorth());
					Logger.d("set adjustment angle of map to "+site.getNorth());
					Toast.makeText(ctx, R.string.project_site_nort_set, Toast.LENGTH_SHORT).show();
					saveProjectSite();
				}

			};
			northDrawable.setRelativePosition(site.getWidth() / 2, site.getHeight() / 2);
			northDrawable.setAngle(map.getAngle() + site.getNorth());

		} else {
			map.removeSubDrawable(northDrawable);
			// do not set the angle, if the menu option is clicked
			// site.setNorth(northDrawable.getAngle());
			// LocationServiceFactory.getLocationService().setRelativeNorth(site.getNorth());
			northDrawable = null;
		}

		multiTouchView.invalidate();

	}

	/**
	 * 
	 */
	protected void scaleOfMap() {
		if (scaler == null) {
			scaler = new ScaleLineDrawable(context, map, new OkCallback() {

				@Override
				public void onOk() {
					onMapScaleSelected();
				}
			});
			scaler.getSlider(1).setRelativePosition(user.getRelativeX() - 80, user.getRelativeY());
			scaler.getSlider(2).setRelativePosition(user.getRelativeX() + 80, user.getRelativeY());
			multiTouchView.invalidate();
		} else {
			onMapScaleSelected();
		}
	}

	protected void onMapScaleSelected() {
		scalerDistance = scaler.getSliderDistance();
		scaler.removeScaleSliders();
		map.removeSubDrawable(scaler);
		scaler = null;
		invalidate();
		showDialog(DIALOG_SET_SCALE_OF_MAP);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onPause()
	 */
	@Override
	protected void onPause() {
		super.onPause();
		multiTouchView.unloadImages();
		map.unload();
		
		try{
		unregisterReceiver(receiver); //unregister Bluetooth receiver
		}catch(Exception e){
			
		}

		setWalkingAndScanning(false,false);

		saveProjectSite();
		
	}

	/**
	 * save current project site
	 */
	protected void saveProjectSite() {
		log.debug("saving project site");

		try {

			Location curLocation = new Location(LocationServiceFactory.getLocationService().getLocation()), lastLocation = site.getLastLocation();

			if (lastLocation == null || (curLocation.getX() != lastLocation.getX() && curLocation.getY() != lastLocation.getY())) {
				site.setLastLocation(curLocation);

				Dao<Location, Integer> locDao = databaseHelper.getDao(Location.class);

				if (lastLocation != null) {
					// delete old location
					locDao.delete(lastLocation);
				}
				// and create new one
				locDao.create(curLocation);

			}

			for (MultiTouchDrawable d : map.getSubDrawables()) {

				if (d instanceof AccessPointDrawable) {
					AccessPoint ap = ((AccessPointDrawable) d).getAccessPoint();
					// id is not 0, so this location was never saved
					if (!ap.isCalculated() && ap.getLocation() != null) {
						try {
							databaseHelper.getDao(Location.class).create(ap.getLocation());
							databaseHelper.getDao(AccessPoint.class).update(ap);
						} catch (SQLException e) {
							log.error("could not save location data for an ap: " + ap.toString(), e);
						}
					}
				}
			}

			int changed = projectSiteDao.update(site);

			if (changed > 0) {
				Toast.makeText(this, R.string.project_site_saved, Toast.LENGTH_SHORT).show();
			}

			projectSiteDao.refresh(site);
		} catch (SQLException e) {
			log.error("could not save or refresh project site", e);
			Toast.makeText(this, R.string.project_site_save_failed, Toast.LENGTH_LONG).show();
		}

	}

	protected void deleteProjectSite() {
		log.debug("saveing project site");

		try {
			int rows = site.delete();

			if (rows > 0) {
				Toast.makeText(this, R.string.project_site_deleted, Toast.LENGTH_SHORT).show();
			} else {
				Logger.w("Tried to delete a project site, but it did not exist?!?");

			}
			finish();

		} catch (SQLException e) {
			log.error("could not delete project site", e);
			Toast.makeText(this, getString(R.string.project_site_delete_failed, e.getMessage()), Toast.LENGTH_LONG).show();
		}

	}

	@Override
	public synchronized void onScanFinished(WifiScanResult wr){
		hideWifiScanDialog();
		if (!ignoreWifiResults) {
			try {

				Logger.d("received a wifi scan result!");
				ignoreWifiResults = true;

				wr.setProjectLocation(site);

				if (walkingAndScanning) {
					//unsavedScanResults.add(wr);
				} else {
					//wr.save(databaseHelper);
					site.getScanResults().refreshCollection();
				}

				// Dao<WifiScanResult, Integer> scanResultDao = databaseHelper.getDao(WifiScanResult.class);
				// scanResultDao.update(wr);

				// projectSiteDao.refresh(site);
				
				
				//new MeasuringPointDrawable(this, map, wr);
				
				for (int i = 0; i < map.getSubDrawables().size(); i++) {
					MultiTouchDrawable d = map.getSubDrawables().get(i);
					if (d instanceof AccessPointDrawable) {
						
						//Toast.makeText(this, "AP positions: " + d.getRelativeX() + " , " +d.getRelativeY() + " , " +  ((AccessPointDrawable) d).getAccessPoint().getBssid() + " , " + ((AccessPointDrawable) d).getAccessPoint().getLocation(), Toast.LENGTH_LONG).show();
						addedAP.add(((AccessPointDrawable) d).getAccessPoint());
					}
				}
				
				
				float xf = 0;
				float yf = 0;
				

				// StringBuffer sb = new StringBuffer();
				HashMap<String, Integer> ssids = new HashMap<String, Integer>();
				if(wr.getBssids()!=null)
					for (BssidResult result : wr.getBssids()) {
						ssids.put(result.getSsid(), (ssids.get(result.getSsid()) == null ? 1 : ssids.get(result.getSsid()) + 1));
						// BssidResult result = it.next();
						// Logger.d("ScanResult: " + result.toString());
						// sb.append(result.toString());
						// sb.append("\n");
						//Toast.makeText(this, "ScanResult: " + result.getLevel(), Toast.LENGTH_SHORT).show();
					
//						if(result.getBssid().equals("00:12:43:4e:2b:d0")){ // 00:24:fe:c0:83:de
//							Toast.makeText(this, "A huevo ", Toast.LENGTH_SHORT).show();
//
//						}else{
//							Toast.makeText(this, "Los Strings no son iguales ", Toast.LENGTH_SHORT).show();
//						}
						
						
						for(AccessPoint tmpAP : addedAP){
							//for(int i=0; i<addedAP.size(); i++){					
							if(tmpAP.getBssid().equals(result.getBssid())){	
							//if(addedAP.get(i).getBssid().compareTo(result.getBssid()) == 0){
								
									//if(lastxf != xf && lastyf != yf){
										if(result.getLevel() > -70){
											xf=tmpAP.getLocation().getX();
											yf=tmpAP.getLocation().getY();
											floorID = tmpAP.getCapabilities();
										}
									//}
							}
						}
						
						
//						try{
//						if(result.getBssid().equals("00:12:43:4e:2b:d0") || result.getBssid().equals("00:12:43:4e:2b:d1") || result.getBssid().equals("00:12:43:4e:2b:d2") ){
//								if(result.getLevel() > -80){
//									xf= 555; //86;
//									yf= 992; //152;
//								}
//							}else if(result.getBssid().equals("00:11:20:68:83:a0") || result.getBssid().equals("00:11:20:68:83:a1") || result.getBssid().equals("00:11:20:68:83:a2")){
//								if(result.getLevel() > -80){
//									xf= 621; //95;
//									yf= 822;// 126;
//								}
//							}else if(result.getBssid().equals("00:11:20:2a:4d:e0") || result.getBssid().equals("00:11:20:2a:4d:e1") || result.getBssid().equals("00:11:20:2a:4d:e2")){
//								if(result.getLevel() > -80){
//									xf= 602; // 92;
//									yf= 668; //102;
//								}
//							}else if(result.getBssid().equals("00:11:20:2a:56:70") || result.getBssid().equals("00:11:20:2a:56:71") || result.getBssid().equals("00:11:20:2a:56:72")){
//								if(result.getLevel() > -80){
//									xf= 523; //80;
//									yf= 603; //92;
//								}
//							}else if(result.getBssid().equals("00:11:20:2a:58:10") || result.getBssid().equals("00:11:20:2a:58:11") || result.getBssid().equals("00:11:20:2a:58:12")){
//								if(result.getLevel() > -80){
//									xf= 377; //58;
//									yf= 663; //102;
//								}
//							}else if(result.getBssid().equals("00:11:20:2a:55:80") || result.getBssid().equals("00:11:20:2a:55:81") || result.getBssid().equals("00:11:20:2a:55:82")){
//								if(result.getLevel() > -80){
//								xf= 401; //61;
//								yf= 923; //141;
//								}
//							}
//						}
//						 catch(Exception e){
//							 Logger.e("could not start wifi scan", e);
//							 }
						 
					}
				
	
				WeightedCentroidTrilateration wc = new WeightedCentroidTrilateration(context, site);
				WiFiPos = wc.calculateUserPositionWiFi(wr, addedAP);
				
				newObservation = true;
				
				 xf = (float) WiFiPos.x;
				 yf = (float) WiFiPos.y;
				
				 if(flagQuickScan){
				 user.setRelativePosition(xf, yf);
				//create final position/location object
					FinalPos.setAccurancy(errorWIFI);
					FinalPos.setX(xf);
					FinalPos.setY(yf);
					Date currentDate = new Date(System.currentTimeMillis());
					FinalPos.setTimestamp(currentDate);
					
					String posProvider=null;
					
					if(flagDR && !flagWF && !flagBT){
						posProvider="DR";
						
					}else if(!flagDR && flagWF && !flagBT){
						posProvider="WiFi";
					}else if(!flagDR && !flagWF && flagBT){
						posProvider="Bluetooth";
					}else if(flagDR && flagWF && !flagBT){
						posProvider="DR + WiFi+Erro10";
					}else if(flagDR && !flagWF && flagBT){
						posProvider="DR + Bluetooth";
					}else if(!flagDR && flagWF && flagBT){
						posProvider="WiFi + Bluetooth";
					}else if(flagDR && flagWF && flagBT){
						posProvider="DR + WiFi + Bluetooth";
					}
					
					FinalPos.setProvider(posProvider);
				 }else if((flagWF) && (xf != 0) && (yf != 0) &&(!flagQuickScan)){ //  && (xf != 0) && (yf != 0) 
				
					 float errorPrediction = 1;
					 if(flagDR){
						 errorPrediction = errorDR;
					 }else{
						 errorPrediction = errorWIFI;
					 }
					 
					 sensorFusionFilter(xf, yf, "kalman", errorPrediction, errorWIFI);
					 
				
				DRAccStep=0; //we initialize the DR steps count to check the DR accuracy (5% of travelled distance)

				}
				 
			       if(newObservation){
                       newWifiPos = 1;
               } else{
                   newWifiPos = 0;
               }
				 
			       //Date currentDate = new Date(System.currentTimeMillis());
			       
			       Time today = new Time(Time.getCurrentTimezone());
					today.setToNow();
			       
			       if(flagLogText){ //Log WiFi Pos
				 //writeToTextFile(FinalPos.getX()+","+FinalPos.getY()+","+FinalPos.getAccurancy()+","+FinalPos.getProvider()+","+FinalPos.getTimestampmilis(),FinalPos.getProvider());
			    		
			    	   String ErrorDRString = Float.toString(errorDR);
			    	   String WiFiPosXString = Float.toString(WiFiPos.x);
			    	   String WiFiPosYString = Float.toString(WiFiPos.y);
			    	   String ErrorWiFiString = Float.toString(errorWIFI);
			    	   String roundsString = Integer.toString(rounds);
			    	   String dayString = Integer.toString(today.monthDay);
			    	   String hourString = Integer.toString(today.hour);
			    	   String secondString = Integer.toString(today.second);
			    	   
			    	   
			    	   
			    	   writeToTextFile(stepsGlobal + "," + newWifiPos + "," + refPoint +
					  				 "," + heading + ","+ ErrorDRString +","+ WiFiPosXString +","+
					  				 WiFiPosYString +","+ ErrorWiFiString +","+
					  				 FinalPos.getX()+","+FinalPos.getY()+","+FinalPos.getAccurancy()+","+FinalPos.getProvider()+","+dayString+","+hourString+","+secondString,
					  				 roundsString);

				}
				 
				flagQuickScan = false;
						
				Toast.makeText(this, "WiFiPos Float X: " + xf, Toast.LENGTH_SHORT).show();
				Toast.makeText(this, "WiFiPos Float Y: " + yf, Toast.LENGTH_SHORT).show();
								
				user.bringToFront();
				//userWiFi.bringToFront();

				if(floorID.equals("0")){
					
					RadioButton bfloor1 = (RadioButton) findViewById(R.id.floor1);
		            bfloor1.setChecked(true);
					
				}else if(floorID.equals("1")){
					
					RadioButton bfloor2 = (RadioButton) findViewById(R.id.floor2);
		            bfloor2.setChecked(true);
					
				}else if(floorID.equals("2")){
					
					RadioButton bfloor3 = (RadioButton) findViewById(R.id.floor3);
		            bfloor3.setChecked(true);
					
				}

				multiTouchView.invalidate();

				// it's not necessary to show the result as Toast, but we can show a summary
				// Toast.makeText(this, this.getString(R.string.project_site_wifiscan_finished, sb.toString()), Toast.LENGTH_SHORT).show();
				Toast.makeText(this, this.getString(R.string.project_site_wifiscan_finished, ssids.size(), wr.getBssids().size()), Toast.LENGTH_SHORT)
						.show();

				// if(stepDetectionProvider.isRunning()){
				// // we are walking and finished a scan, why don't we start a new one
				// startWifiBackgroundScan();
				//
				// }
				
			} catch (SQLException e) {
				Logger.e("could not update wifiscanresult!", e);
				Toast.makeText(this, this.getString(R.string.project_site_wifiscan_failed, e.getMessage()), Toast.LENGTH_LONG).show();
			}

		}
	}

	private void sensorFusionFilter(float xf, float yf, String filterType, float errorPrediction, float errorObservation) {
		// TODO Auto-generated method stub
	    //errorDR = (float) (((0.70 * DRAccStep)*0.05)+1); //DR positioning error in meters based on the travelled distance
	    //errorWIFI = (float) 10; //WiFi positioning error in meters
		float k1 = (float) (errorPrediction/(errorPrediction+errorObservation));
		float k2 = (float) (errorObservation/(errorPrediction+errorObservation));
		float PosError = (float) ((k2*errorPrediction) + (k1*errorObservation));
		float XFin=0;
		float YFin=0;
	    
	    //float k1 = (float) 0.5;
	    //float k2 = (float) 0.5;
		
		if(filterType.equals("kalman")){
			
			XFin=user.getRelativeX()*k2 + xf*k1;
			YFin=user.getRelativeY()*k2 + yf*k1;
			user.setRelativePosition(user.getRelativeX()*k2 + xf*k1, user.getRelativeY()*k2 + yf*k1);
			
		}else if(filterType.equals("particle")){
			
			XFin=user.getRelativeX()*k2 + xf*k1;
			YFin=user.getRelativeY()*k2 + yf*k1;
			user.setRelativePosition(user.getRelativeX()*k2 + xf*k1, user.getRelativeY()*k2 + yf*k1);
			
		}else if(filterType.equals("weighted")){
			
			XFin=user.getRelativeX()*k2 + xf*k1;
			YFin=user.getRelativeY()*k2 + yf*k1;
			user.setRelativePosition(user.getRelativeX()*k2 + xf*k1, user.getRelativeY()*k2 + yf*k1);
			
		}
		
		//create final position/location object
		FinalPos.setAccurancy(PosError);
		FinalPos.setX(XFin);
		FinalPos.setY(YFin);
		Date currentDate = new Date(System.currentTimeMillis());
		FinalPos.setTimestamp(currentDate);
		
		String posProvider=null;
		if(flagDR && !flagWF && !flagBT){
			posProvider="Dead Reckoning";
			
		}else if(!flagDR && flagWF && !flagBT){
			posProvider="WiFi AP positioning";
		}else if(!flagDR && !flagWF && flagBT){
			posProvider="Bluetooth Beacon Positioning";
		}else if(flagDR && flagWF && !flagBT){
			posProvider="DR + WiFi+Error10";
		}else if(flagDR && !flagWF && flagBT){
			posProvider="DR + Bluetooth";
		}else if(!flagDR && flagWF && flagBT){
			posProvider="WiFi + Bluetooth";
		}else if(flagDR && flagWF && flagBT){
			posProvider="DR + WiFi + Bluetooth";
		}
		
		FinalPos.setProvider(posProvider+"+"+filterType);
	}

	@Override
	public void onScanFailed(Exception ex) {
		hideWifiScanDialog();
		if (!ignoreWifiResults) {

			Logger.e("Wifi scan failed!", ex);
			Toast.makeText(this, this.getString(R.string.project_site_wifiscan_failed, ex.getMessage()), Toast.LENGTH_LONG).show();

		}

	}

	protected void startWifiScan() throws WifiException {
		log.debug("starting WiFi Scan");

		wifiBroadcastReceiver = WifiScanner.startScan(this, this);
		ignoreWifiResults = false;
	}

	protected void startWifiBackgroundScan() {
		
			try {
				// we first stop the old receiver, so we wont receive duplicate results
//				stopWifiScan();
				
				if(wifiBroadcastReceiver!=null){
//					wifiBroadcastReceiver.
				}
				
				startWifiScan();
				// Toast.makeText(this, R.string.project_site_wifiscan_started, Toast.LENGTH_SHORT).show();
			} catch (WifiException e) {
				Logger.e("could not start wifi scan", e);
				Toast.makeText(this, getString(R.string.project_site_wifiscan_start_failed, e.getMessage()), Toast.LENGTH_LONG).show();
			}
		
	}

	/**
	 * stop the wifi scan, if in progress
	 */
	protected void stopWifiScan() {
		hideWifiScanDialog();

		if (wifiBroadcastReceiver != null) {

			WifiScanner.stopScanner(this, wifiBroadcastReceiver);
			wifiBroadcastReceiver = null;

		}
		// stop scan
		// oh, wait, we can't stop the scan, it's asynchronous!
		// we just have to ignore the result!
		ignoreWifiResults = true;

	}

	/**
	 * hide the wifi scan dialog if shown
	 */
	protected void hideWifiScanDialog() {
		if (scanningImageView != null) {
			((AnimationDrawable) scanningImageView.getDrawable()).stop();
			// scanningImageView = null;
		}

		if (scanAlertDialog != null) {
			scanAlertDialog.cancel();
			// scanAlertDialog = null;
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onWindowFocusChanged(boolean)
	 */
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);

		// if(scanAlertDialog!=null && scanningImageView!=null){
		// ((AnimationDrawable) scanningImageView.getDrawable()).start();
		// }
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		Logger.d("Activity result of " + requestCode + " " + resultCode + " " + (data != null ? data.toString() : ""));

		switch (requestCode) {
		case FILEBROWSER_REQUEST:

			if (resultCode == Activity.RESULT_OK && data != null) {
				String path = data.getExtras().getString(FileBrowser.EXTRA_PATH);

				if (backgroundPathTextView != null) {
					backgroundPathTextView.setText(path);
				} else {
					Logger.w("the background image dialog textview should not be null?!?");
				}
			}
			break;

		default:
			super.onActivityResult(requestCode, resultCode, data);
			break;
		}

	}

	protected void setBackgroundImage(String path) {

		try {
			Bitmap bmp = BitmapFactory.decodeFile(path);
			site.setBackgroundBitmap(bmp);
			map.setBackgroundImage(bmp);
			site.setSize(bmp.getWidth(), bmp.getHeight());
			map.setSize(bmp.getWidth(), bmp.getHeight());
			user.setRelativePosition(bmp.getWidth() / 2, bmp.getHeight() / 2);
			//userWiFi.setRelativePosition(bmp.getWidth() / 2, bmp.getHeight() / 2);
			//userWiFi.setCenterX(bmp.getWidth());
			//userWiFi.setCenterY(bmp.getHeight() / 2);

			multiTouchView.invalidate();
			Toast.makeText(context, "set " + path + " as new background image!", Toast.LENGTH_LONG).show();
			saveProjectSite();

		} catch (Exception e) {
			Logger.e("could not set background", e);
			Toast.makeText(context, getString(R.string.project_site_set_background_failed, e.getMessage()), Toast.LENGTH_LONG).show();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.app.Activity#onConfigurationChanged(android.content.res.Configuration )
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		this.setContentView(R.layout.project_site);
		initUI();
	}

	@Override
	public void invalidate() {
		if (multiTouchView != null) {
			multiTouchView.invalidate();
		}
	}

	/**
	 * @author Thomas Konrad (is101503@fhstp.ac.at)
	 */
	protected class TriangulationTask extends AsyncTask<Void, Integer, Vector<AccessPointDrawable>> {

		/**
		 * @uml.property name="parent"
		 * @uml.associationEnd
		 */
		private final ProjectSiteActivity parent;

		private final ProgressDialog progress;

		public TriangulationTask(final ProjectSiteActivity parent, final ProgressDialog progress) {
			this.parent = parent;
			this.progress = progress;
		}

		@Override
		protected Vector<AccessPointDrawable> doInBackground(Void... params) {
			WeightedCentroidTrilateration wc = new WeightedCentroidTrilateration(context, site, progress);
			return wc.calculateAllAndGetDrawables();
		}

		@Override
		protected void onPostExecute(final Vector<AccessPointDrawable> result) {
			progress.dismiss();
			parent.setCalculatedAccessPoints(result);
		}
	}

	@Override
	public void onLocationChange(Location loc) {
		// info from StepDetectionProvider, that the location changed.
		
		heading=loc.getAccurancy();
		
		if(flagDR){
		user.setRelativePosition(loc.getX(), loc.getY());
		map.addStep(new PointF(loc.getX(),loc.getY()));
		}
		messageHandler.sendEmptyMessage(MESSAGE_REFRESH);
		DRAccStep++;
		stepsGlobal++;
		
	    errorDR = (float) (((0.70 * DRAccStep)*0.05)+1); //DR positioning error in meters based on the travelled distance
		//create final position/location object
		FinalPos.setAccurancy(errorDR);
		FinalPos.setX(loc.getX());
		FinalPos.setY(loc.getY());
		Date currentDate = new Date(System.currentTimeMillis());
	
		  Time today = new Time(Time.getCurrentTimezone());
			today.setToNow();
			FinalPos.setTimestamp(currentDate);
			
		
		String posProvider=null;
		
     		if(flagDR && !flagWF && !flagBT){
				posProvider="DR";
				
			}else if(!flagDR && flagWF && !flagBT){
				posProvider="WiFi";
			}else if(!flagDR && !flagWF && flagBT){
				posProvider="Bluetooth";
			}else if(flagDR && flagWF && !flagBT){
				posProvider="DR + WiFi+Error10";
			}else if(flagDR && !flagWF && flagBT){
				posProvider="DR + Bluetooth";
			}else if(!flagDR && flagWF && flagBT){
				posProvider="WiFi + Bluetooth";
			}else if(flagDR && flagWF && flagBT){
				posProvider="DR + WiFi + Bluetooth";
			}
		
		FinalPos.setProvider(posProvider);
		
	    if(newObservation){
            newWifiPos = 1;
    } else{
        newWifiPos = 0;
    }
		
		if(flagLogText){ //LogDR pos
		//writeToTextFile(FinalPos.getX()+","+FinalPos.getY()+","+FinalPos.getAccurancy()+","+FinalPos.getProvider()+","+FinalPos.getTimestampmilis(),FinalPos.getProvider());

			
		   	   String ErrorDRString = Float.toString(errorDR);
	    	   String WiFiPosXString = Float.toString(WiFiPos.x);
	    	   String WiFiPosYString = Float.toString(WiFiPos.y);
	    	   String ErrorWiFiString = Float.toString(errorWIFI);
	    	   String roundsString = Integer.toString(rounds);
	    	   String dayString = Integer.toString(today.monthDay);
	    	   String hourString = Integer.toString(today.hour);
	    	   String secondString = Integer.toString(today.second);
	    	   
	    	   
	    	   
	    	   writeToTextFile(stepsGlobal + "," + newWifiPos + "," + refPoint +
			  				 "," + heading + ","+ ErrorDRString +","+ WiFiPosXString +","+
			  				 WiFiPosYString +","+ ErrorWiFiString +","+
			  				 FinalPos.getX()+","+FinalPos.getY()+","+FinalPos.getAccurancy()+","+FinalPos.getProvider()+","+dayString+","+hourString+","+secondString,
			  				 roundsString);
		
		}
		
	}

	protected class WifiScanResultPersistTask extends AsyncTask<Void, Integer, Bundle> {

		protected ProjectSiteActivity parent;

		protected ProgressDialog progressDialog;

		protected boolean running = true;

		protected DatabaseHelper databaseHelper;

		static final String RESULT_CODE = "result", RESULT_MESSAGE = "message", RESULT_COUNT = "count";

		public WifiScanResultPersistTask(ProjectSiteActivity parent, ProgressDialog progress) {
			this.parent = parent;
			this.progressDialog = progress;
			databaseHelper = OpenHelperManager.getHelper(parent, DatabaseHelper.class);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#doInBackground(Params[])
		 */
		@Override
		protected Bundle doInBackground(Void... params) {
			Bundle result = new Bundle();
			result.putInt(RESULT_CODE, RESULT_CANCELED);

			int progress = 0;

			synchronized (unsavedScanResults) {

				try{
				this.progressDialog.setMax(unsavedScanResults.size());
				}catch (Exception ex){}

				// save all wifiscan results
				try {

					for (WifiScanResult sr : unsavedScanResults) {
						if (!running) {
							break;
						}
						sr.save(databaseHelper);
						this.publishProgress(++progress);
					}

					result.putInt(RESULT_COUNT, progress);
					if (running) {
						Logger.d("saved " + unsavedScanResults.size() + " WiFi scan results");
						unsavedScanResults = new ArrayList<WifiScanResult>();
						result.putInt(RESULT_CODE, RESULT_OK);
					} else {
						// remove the saved results

						while (progress > 0) {
							unsavedScanResults.remove(0);
							progress--;
						}
					}
				} catch (SQLException e) {
					Logger.e("Could not save temporary results", e);
					result.putInt(RESULT_CODE, RESULT_CANCELED);
					result.putString(RESULT_MESSAGE, RESULT_MESSAGE);
				}
			}

			return result;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
		 */
		@Override
		protected void onPostExecute(Bundle result) {
			if (progressDialog != null)
				progressDialog.dismiss();
			OpenHelperManager.releaseHelper();
			if (running && messageHandler != null) {
				Message msg = new Message();
				msg.what = MESSAGE_PERSIST_RESULT;
				msg.arg1 = result.getInt(RESULT_CODE);
				msg.setData(result);
				messageHandler.sendMessage(msg);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onProgressUpdate(Progress[])
		 */
		@Override
		protected void onProgressUpdate(Integer... values) {
			if (progressDialog != null)
				progressDialog.setProgress(values[0]);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see android.os.AsyncTask#onCancelled()
		 */
		@Override
		protected void onCancelled() {
			running = false;
			super.onCancelled();
		}

	}
	
	//BluetoothComm connected thread class
	
	private class ConnectThread extends Thread {
		
		private final BluetoothSocket mmSocket;
	    private final BluetoothDevice mmDevice;
	 
	    public ConnectThread(BluetoothDevice device) {
	        // Use a temporary object that is later assigned to mmSocket,
	        // because mmSocket is final
	        BluetoothSocket tmp = null;
	        mmDevice = device;
	        Log.i(tag, "construct");
	        // Get a BluetoothSocket to connect with the given BluetoothDevice
	        try {
	            // MY_UUID is the app's UUID string, also used by the server code
	            tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
	        } catch (IOException e) { 
	        	Log.i(tag, "get socket failed");
	        	
	        }
	        mmSocket = tmp;
	    }
	 
	    public void run() {
	        // Cancel discovery because it will slow down the connection
	        btAdapter.cancelDiscovery();
	        Log.i(tag, "connect - run");
	        try {
	            // Connect the device through the socket. This will block
	            // until it succeeds or throws an exception
	            mmSocket.connect();
	            Log.i(tag, "connect - succeeded");
	        } catch (IOException connectException) {	Log.i(tag, "connect failed");
	            // Unable to connect; close the socket and get out
	            try {
	                mmSocket.close();
	            } catch (IOException closeException) { }
	            return;
	        }
	 
	        // Do work to manage the connection (in a separate thread)
	   
	        mHandler.obtainMessage(SUCCESS_CONNECT, mmSocket).sendToTarget();
	    }
	 


		/** Will cancel an in-progress connection, and close the socket */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}

	private class ConnectedThread extends Thread {
	    private final BluetoothSocket mmSocket;
	    private final InputStream mmInStream;
	    private final OutputStream mmOutStream;
	 
	    public ConnectedThread(BluetoothSocket socket) {
	        mmSocket = socket;
	        InputStream tmpIn = null;
	        OutputStream tmpOut = null;
	 
	        // Get the input and output streams, using temp objects because
	        // member streams are final
	        try {
	            tmpIn = socket.getInputStream();
	            tmpOut = socket.getOutputStream();
	        } catch (IOException e) { }
	 
	        mmInStream = tmpIn;
	        mmOutStream = tmpOut;
	    }
	 
	    public void run() {
	        byte[] buffer;  // buffer store for the stream
	        int bytes; // bytes returned from read()

	        // Keep listening to the InputStream until an exception occurs
	        while (true) {
	            try {
	                // Read from the InputStream
	            	buffer = new byte[1024];
	                bytes = mmInStream.read(buffer);
	                // Send the obtained bytes to the UI activity
	                mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
	                        .sendToTarget();
	               
	            } catch (IOException e) {
	                break;
	            }
	        }
	    }
	 
	    /* Call this from the main activity to send data to the remote device */
	    public void write(byte[] bytes) {
	        try {
	            mmOutStream.write(bytes);
	        } catch (IOException e) { }
	    }
	 
	    /* Call this from the main activity to shutdown the connection */
	    public void cancel() {
	        try {
	            mmSocket.close();
	        } catch (IOException e) { }
	    }
	}
	
	
}
