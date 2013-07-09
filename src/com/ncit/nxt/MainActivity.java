package com.ncit.nxt;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.speech.RecognizerIntent;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements SensorEventListener, DrawModeCallBackInterface {

	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_CONNECT_DEVICE = 2;
	//private static final int REQUEST_SETTINGS = 3;
	private static final int RESULT_SPEECH = 4;

	public static final int MESSAGE_TOAST = 1;
	public static final int MESSAGE_STATE_CHANGE = 2;

	public static final String TOAST = "toast";

	private BluetoothAdapter mBluetoothAdapter;
	private PowerManager mPowerManager;
	private PowerManager.WakeLock mWakeLock;
	private NXTTalker mNXTTalker;

	private int mState = NXTTalker.STATE_NONE;
	private int mSavedState = NXTTalker.STATE_NONE;
	private boolean mNewLaunch = true;
	private String mDeviceAddress = null;

	private ArrayList<ImageButton> motorButtons = new ArrayList<ImageButton>();

	//Buton selectare mod scriere: 0 - Vertical, 1 - Orizontal etc.
	private Button bDraw;

	//Variabila pentru modul de desenare selectat
	private int drawMode = 0;

	//Status conexiune
	private TextView testStatusTextView; 
	//Afisare viteze motoare
	private TextView tvShowSpeeds[] = new TextView[3];


	private boolean mRegulateSpeed = true;
	private boolean mSynchronizeMotors = false;
	private boolean sensorMode = false;

	private SensorManager mSensorManager;
	private Sensor mSensor;
	private Sensor mOrientation;
	private String stateText = "";

	private int speedMotors[] = new int[3];	
	private SeekBar seekBars[] = new SeekBar[3];	
	//putere pentru desenare
	private Letters letters;

	private TabHost mTabHost;
	private TabSpec tab1, tab2, tab3;

	private ArrayList<ImageView> sensorImages = new ArrayList<ImageView>();

	//drawmode
	private TextView number;
	private ImageButton decrement;
	private ImageButton increment;
	private String[] numbers = {"0","1","2","3","4","5","6","7","8","9"};

	//Voice Recognition
	private Button voiceControl;
	
	//Don't show this again var:
	private boolean hideHints;
	SharedPreferences shPrefs;
	SharedPreferences.Editor shPrefsEditor;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		// Don't show hints again
		shPrefs = getSharedPreferences("show_hints", MODE_PRIVATE);
		
		mTabHost=(TabHost)findViewById(R.id.tabHost);
		mTabHost.setup();

		tab1 = mTabHost.newTabSpec("First");
		tab1.setContent(R.id.tab1);
		tab1.setIndicator("Button Mode");
		mTabHost.addTab(tab1);

		tab2 = mTabHost.newTabSpec("Second");
		tab2.setContent(R.id.tab2);
		tab2.setIndicator("Sensor Mode");
		mTabHost.addTab(tab2);

		tab3 = mTabHost.newTabSpec("Third");
		tab3.setContent(R.id.tab3);
		tab3.setIndicator("Draw Mode");
		mTabHost.addTab(tab3);

		
		mTabHost.setOnTabChangedListener(new OnTabChangeListener() {

			@Override
			public void onTabChanged(String tabId) {
				
				letters.stopAllMotors();
				if (tabId.equals("Second")) {
					sensorMode = true;
					firstOrientation = true;
					
					//MotionHint Dialog
					FragmentManager fm = getSupportFragmentManager();
					SensorHintsDialog sensorHintsDialog = new SensorHintsDialog();
					
					hideHints = shPrefs.getBoolean("hide_sensor_hints", false);
					if (!hideHints) {
						sensorHintsDialog.show(fm, "Motion Hint Dialog");
					}
					
				} else {
					sensorMode = false;
					if (tabId.equals("Third")) {
						//MotionHint Dialog
						Log.d("DERP", "Starting draw hint dialog");
						FragmentManager fm = getSupportFragmentManager();
						DrawHintsDialog drawHintsDialog = new DrawHintsDialog();
						hideHints = shPrefs.getBoolean("hide_draw_hints", false);
						if (!hideHints) {
							drawHintsDialog.show(fm, "Draw Hint Dialog");
						}
					}
				}
			}
		});


		number = (TextView) findViewById(R.id.number);
		number.setText("0");
		
		decrement = (ImageButton) findViewById(R.id.bDecrement);
		increment = (ImageButton) findViewById(R.id.bIncrement);

		decrement.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String i = number.getText().toString();
				if(Integer.parseInt(i) > 0){
					number.setText(numbers[Integer.parseInt(i)-1]);
				}else{
					number.setText("9");
				}
			}
		} );

		increment.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				String i = number.getText().toString();
				if(Integer.parseInt(i) < numbers.length -1){
					number.setText(numbers[Integer.parseInt(i)+1]);
				}else{
					number.setText("0");
				}
			}
		});

		for (int i = 0; i < mTabHost.getTabWidget().getChildCount(); i++) {
			TextView tView = (TextView) mTabHost.getTabWidget().getChildAt(i).findViewById(android.R.id.title);
			tView.setTextColor(Color.WHITE);
		}

		if (savedInstanceState != null) {
			mNewLaunch = false;
			mDeviceAddress = savedInstanceState.getString("device_address");
			if (mDeviceAddress != null) {
				mSavedState = NXTTalker.STATE_CONNECTED;
			}
		}

		mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
		mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "NXT Remote Control");

		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

		if (mBluetoothAdapter == null) {
			Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mOrientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);


		mNXTTalker = new NXTTalker(mHandler);

		allocateMemory();
		setupUI();


	}

	private void allocateMemory () {

		letters = new Letters (mNXTTalker);

	}

	@Override
	protected void onStart() {
		super.onStart();
		if (!mBluetoothAdapter.isEnabled()) {
			Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
		} else {
			if (mSavedState == NXTTalker.STATE_CONNECTED) {
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(mDeviceAddress);
				mNXTTalker.connect(device);
			} else {
				if (mNewLaunch) {
					mNewLaunch = false;
					findBrick();
				}
			}
		}

	}

	@SuppressLint("NewApi")
	private void setupUI() {
		
		voiceControl = (Button) findViewById(R.id.VoiceControl);
		voiceControl.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(
						RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
				intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, "en-US");
				intent.putExtra(RecognizerIntent.EXTRA_PROMPT,
						"Only numbers from 0 to 9.");
				intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);
				try {
					startActivityForResult(intent, RESULT_SPEECH);
				} catch (ActivityNotFoundException a) {
					Toast t = Toast.makeText(getApplicationContext(),
							"Opps! Your device doesn't support Speech to Text",
							Toast.LENGTH_SHORT);
					t.show();
				}
			}
		});


		//Imagini animatie sensor
		sensorImages.add((ImageView) findViewById(R.id.inactive_l20));
		sensorImages.add((ImageView) findViewById(R.id.inactive_u0));
		sensorImages.add((ImageView) findViewById(R.id.inactive_u1));
		sensorImages.add((ImageView) findViewById(R.id.inactive_r20));
		sensorImages.add((ImageView) findViewById(R.id.inactive_r21));
		sensorImages.add((ImageView) findViewById(R.id.inactive_d1));
		sensorImages.add((ImageView) findViewById(R.id.inactive_d0));
		sensorImages.add((ImageView) findViewById(R.id.inactive_l21));

		//Assign motor buttons
		motorButtons.add((ImageButton) findViewById(R.id.m1b1));
		motorButtons.add((ImageButton) findViewById(R.id.m1b2));
		motorButtons.add((ImageButton) findViewById(R.id.m2b1));
		motorButtons.add((ImageButton) findViewById(R.id.m2b2));
		motorButtons.add((ImageButton) findViewById(R.id.m3b1));
		motorButtons.add((ImageButton) findViewById(R.id.m3b2));


		//SeekBars:
		seekBars[0] = (SeekBar) findViewById(R.id.speedM1);
		seekBars[1] = (SeekBar) findViewById(R.id.speedM2);
		seekBars[2] = (SeekBar) findViewById(R.id.speedM3);

		//Buton selectare mod desenare
		bDraw = (Button) findViewById(R.id.bDraw);
		bDraw.setOnClickListener(new DrawLettersOnClickListener(letters,this));

		//Afisare viteze motoare conform seekBars
		tvShowSpeeds[0] = (TextView) findViewById(R.id.tvShowSpeed1);
		tvShowSpeeds[1] = (TextView) findViewById(R.id.tvShowSpeed2);
		tvShowSpeeds[2] = (TextView) findViewById(R.id.tvShowSpeed3);

		//Seteaza valoarea initiala pentru seekBars;
		for (int i = 0; i < speedMotors.length; i++) {
			speedMotors[i] = seekBars[i].getProgress();
			tvShowSpeeds[i].addOnLayoutChangeListener(new LayoutChangeForText());
			tvShowSpeeds[i].setText(Integer.toString(speedMotors[i])+"%");
		}

		//Adaugare eveniment seekBar listener
		for (SeekBar s : seekBars) {
			s.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {

				@Override
				public void onStopTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onStartTrackingTouch(SeekBar seekBar) {
				}

				@Override
				public void onProgressChanged(SeekBar seekBar, int progress,
						boolean fromUser) {
					if (seekBar == seekBars[0]) {
						speedMotors[0] = progress;
						if (speedMotors[0]>50) {
							tvShowSpeeds[0].setTextColor(Color.rgb(255, 255-(speedMotors[0]-50)*5, 255-(speedMotors[0]-50)*5));
							tvShowSpeeds[0].setText(progress + " %");
						} else {
							tvShowSpeeds[0].setTextColor(Color.WHITE);
							tvShowSpeeds[0].setText(progress + " %");
						}
					}
					if (seekBar == seekBars[1]) {
						speedMotors[1] = progress;
						if (speedMotors[1]>50) {
							tvShowSpeeds[1].setTextColor(Color.rgb(255, 255-(speedMotors[1]-50)*5, 255-(speedMotors[1]-50)*5));
							tvShowSpeeds[1].setText(progress + " %");
						} else {
							tvShowSpeeds[1].setTextColor(Color.WHITE);
							tvShowSpeeds[1].setText(progress + " %");
						}
					}
					if (seekBar == seekBars[2]) {
						speedMotors[2] = progress;
						if (speedMotors[2]>50) {
							tvShowSpeeds[2].setTextColor(Color.rgb(255, 255-(speedMotors[2]-50)*5, 255-(speedMotors[2]-50)*5));
							tvShowSpeeds[2].setText(progress + " %");
						} else {
							tvShowSpeeds[2].setTextColor(Color.WHITE);
							tvShowSpeeds[2].setText(progress + " %");
						}
					}
				}
			});
		}

		testStatusTextView = (TextView) findViewById(R.id.testtext);
		testStatusTextView.setText(stateText+" (Button Mode)");
		for (int i = 0; i<6; i++) {
			switch (i) {
			case 0: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(0,-1,1.0d,15)); break;
			case 1: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(0,1,1.0d,15)); break;
			case 2: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(1,1,1.0d,15)); break;
			case 3: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(1,-1,1.0d,15)); break;
			case 4: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(2,1,1.0d,8)); break;
			case 5: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(2,-1,1.0d,8)); break;
			default: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(i/2,1,1.0d,15)); break;
			}
		}

		displayState();
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		switch (requestCode) {
		case REQUEST_ENABLE_BT:
			if (resultCode == Activity.RESULT_OK) {
				findBrick();
			} else {
				Toast.makeText(this, "Bluetooth not enabled, exiting.", Toast.LENGTH_LONG).show();
				finish();
			}
			break;
		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				String address = data.getExtras().getString(DeviceChooser.EXTRA_DEVICE_ADDRESS);
				BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
				mDeviceAddress = address;
				mNXTTalker.connect(device);
			}
			break;
		case RESULT_SPEECH:
			if (resultCode == RESULT_OK && data != null) {
				ArrayList<String> text = data
						.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
				
				recognizeAndDraw(text.get(0));
			}
			break;
		}

	}

	private void displayState() {
		int color = 0;
		switch (mState){ 
		case NXTTalker.STATE_NONE:
			stateText = "Not connected";
			color = 0xffff0000;
			setProgressBarIndeterminateVisibility(false);
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
			}
			break;
		case NXTTalker.STATE_CONNECTING:
			stateText = "Connecting...";
			color = 0xffffff00;
			setProgressBarIndeterminateVisibility(true);
			if (!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
			break;
		case NXTTalker.STATE_CONNECTED:
			stateText = "Connected";
			color = 0xff00ff00;
			setProgressBarIndeterminateVisibility(false);
			if (!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
			break;
		}
		testStatusTextView.setText(stateText);
		testStatusTextView.setTextColor(color);
	}

	private void findBrick() {
		Intent intent = new Intent(this, DeviceChooser.class);
		startActivityForResult(intent, REQUEST_CONNECT_DEVICE);
	}

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
				break;
			case MESSAGE_STATE_CHANGE:
				mState = msg.arg1;
				displayState();
				break;
			}
		}
	};

	private class MotorButtonListener implements OnTouchListener {

		private int sign;
		private byte motorB;
		private int motorBi;

		public MotorButtonListener(int motor, int dir, double powerMod, int ppower) {
			sign = dir;
			Log.d("power1", "PowerInitial: "+ppower);
			switch (motor){ 
			case 0: motorB = NXTTalker.MOTOR1; motorBi = 0; break;
			case 1: motorB = NXTTalker.MOTOR2; motorBi = 1; break;
			case 2: motorB = NXTTalker.MOTOR3; motorBi = 2; break;
			default: motorB = NXTTalker.MOTOR1; break;
			}
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			int action = event.getAction();

			Log.d ("power1", "speedMotors: " + speedMotors[0] + " " + speedMotors[1] + " " + speedMotors[2]);
			if (!sensorMode) {
				if (action == MotionEvent.ACTION_DOWN) {
					v.setPressed(true);
					byte fPower = (byte) (speedMotors[motorBi] * sign);
					mNXTTalker.motor(motorB, fPower, mRegulateSpeed, mSynchronizeMotors);
				} else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
					v.setPressed(false);
					mNXTTalker.motor(motorB, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}
			}
			return true;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {

	}

	private float lastValues[] = new float[3];
	private double initialOri;
	boolean firstOrientation = true;

	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}

	@Override
	protected void onResume() {
		super.onResume();
		mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
		mSensorManager.registerListener(this, mOrientation, SensorManager.SENSOR_DELAY_NORMAL);
	}



	@Override
	public void onSensorChanged(SensorEvent event) {
		if (sensorMode == true) {
			if (event.sensor == mSensor) {
				float linear_acceleration[] = new float[3];
				// Remove the gravity contribution with the high-pass filter.
				linear_acceleration[0] = event.values[0];
				linear_acceleration[1] = event.values[1];
				linear_acceleration[2] = event.values[2];

				//overr last
				lastValues[0] = linear_acceleration[0];
				lastValues[1] = linear_acceleration[1];
				lastValues[2] = linear_acceleration[2];

				//Motor 0:
				if (Math.abs(linear_acceleration[0])>5) {
					byte power = (byte) (20*Math.signum(linear_acceleration[0]));

					//Schimbare imagine sensor in functie de orientare, pentru motorul 0
					//Motor activ
					if (linear_acceleration[0] > 0) {
						sensorImages.get(6).setImageResource(R.drawable.active_d0);
						sensorImages.get(1).setImageResource(R.drawable.inactive_u0);
					} else {
						sensorImages.get(1).setImageResource(R.drawable.active_u0);
						sensorImages.get(6).setImageResource(R.drawable.inactive_d0);
					}

					//Activare motor
					mNXTTalker.motor(NXTTalker.MOTOR1, power, mRegulateSpeed, mSynchronizeMotors);

				} else {
					//Motor inactiv
					sensorImages.get(1).setImageResource(R.drawable.inactive_u0);
					sensorImages.get(6).setImageResource(R.drawable.inactive_d0);

					//Dezactivare motor
					mNXTTalker.motor(NXTTalker.MOTOR1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}

				//Motor 1:
				if (Math.abs(linear_acceleration[1])>4) {
					byte power = (byte) (20*Math.signum(linear_acceleration[1]));

					//Schimbare imagine sensor in functie de orientare, pentru motorul 0
					//Motor activ					
					if (linear_acceleration[1] > 0) {
						sensorImages.get(2).setImageResource(R.drawable.active_u1);
						sensorImages.get(5).setImageResource(R.drawable.inactive_d1);
					} else {
						sensorImages.get(5).setImageResource(R.drawable.active_d1);
						sensorImages.get(2).setImageResource(R.drawable.inactive_u1);
					}

					//Activare motor:
					mNXTTalker.motor(NXTTalker.MOTOR2, power, mRegulateSpeed, mSynchronizeMotors);
				} else {
					//Motor inactiv
					sensorImages.get(2).setImageResource(R.drawable.inactive_u1);
					sensorImages.get(5).setImageResource(R.drawable.inactive_d1);

					//Dezactivare motor
					mNXTTalker.motor(NXTTalker.MOTOR2, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}
			} else {
				//Motor 2:
				if (firstOrientation) {
					initialOri = event.values[0];
					firstOrientation = false;
				} else {
					double currentOri = event.values[0];
					currentOri -= initialOri;
					if (currentOri<0) {
						currentOri = 360 + currentOri;
					}
					//Activare motor (la dreapta)
					if (currentOri<180&&currentOri>30) {
						byte power = (byte) (12);

						//Schimbare imagine (motor activ)
						sensorImages.get(3).setImageResource(R.drawable.active_r20);
						sensorImages.get(4).setImageResource(R.drawable.active_r21);
						sensorImages.get(0).setImageResource(R.drawable.inactive_l20);
						sensorImages.get(7).setImageResource(R.drawable.inactive_l21);

						//Activare motor:
						mNXTTalker.motor(NXTTalker.MOTOR3, power, mRegulateSpeed, mSynchronizeMotors);

						//Activare motor (la stanga)
					} else if (currentOri<330&&currentOri>180) {
						byte power = (byte) (-12);

						//Schimbare imagine (motor activ)
						sensorImages.get(0).setImageResource(R.drawable.active_l20);
						sensorImages.get(7).setImageResource(R.drawable.active_l21);
						sensorImages.get(3).setImageResource(R.drawable.inactive_r20);
						sensorImages.get(4).setImageResource(R.drawable.inactive_r21);

						//Activare motor:
						mNXTTalker.motor(NXTTalker.MOTOR3, power, mRegulateSpeed, mSynchronizeMotors);

						//Oprire motor
					} else {

						//Schimbare imagini (motor inactiv)
						sensorImages.get(0).setImageResource(R.drawable.inactive_l20);
						sensorImages.get(7).setImageResource(R.drawable.inactive_l21);
						sensorImages.get(3).setImageResource(R.drawable.inactive_r20);
						sensorImages.get(4).setImageResource(R.drawable.inactive_r21);

						//Oprire motor
						mNXTTalker.motor(NXTTalker.MOTOR3, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
					}
				}
			}
		}
	}


	@Override
	public int getDrawMode() {
		drawMode = Integer.parseInt(number.getText().toString());
		return drawMode;
	}

	@SuppressLint("NewApi")
	private class LayoutChangeForText implements OnLayoutChangeListener  {

		@Override
		public void onLayoutChange(View v, int left, int top, int right,
				int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
			if (left == 0 && top == 0 && right == 0 && bottom == 0) {
				return;
			}
			for (int i = 0; i < speedMotors.length; i++) {
				if (motorButtons.get(i*2).getHeight()!=0&&tvShowSpeeds[i].getHeight()!=0) {
					RelativeLayout.LayoutParams lParams;
					Log.d("TXH","Height: "+tvShowSpeeds[i].getHeight());
					lParams = (LayoutParams) tvShowSpeeds[i].getLayoutParams();
					lParams.topMargin = motorButtons.get(i*2).getHeight()/2-tvShowSpeeds[i].getHeight()/2;
					tvShowSpeeds[i].setLayoutParams(lParams);
				}
			}
		}

	}

	@Override
	public boolean onOptionsItemSelected (MenuItem item){

		// Connect
		if (item.getItemId() == R.id.connectItem) {
			findBrick();
			return true;
		}

		// Activare Motion Hints
		if (item.getItemId() == R.id.hintsItem) {

			//MotionHint Dialog
			FragmentManager fm = getSupportFragmentManager();
			SensorHintsDialog sensorHintsDialog = new SensorHintsDialog();
			sensorHintsDialog.show(fm, "Motion Hint Dialog");
			return true;
		}
		
		// Activare Draw Hints
		if (item.getItemId() == R.id.drawHintsItem) {

			//MotionHint Dialog
			FragmentManager fm = getSupportFragmentManager();
			DrawHintsDialog drawHintsDialog = new DrawHintsDialog();
			drawHintsDialog.show(fm, "Draw Hints Dialog");
			return true;
		}

		return false;
	}
	
	
	public void recognizeAndDraw(String type) {
		
		int drawMode;
		
		try {
			drawMode = Integer.parseInt(type);
			if (drawMode >= 0 && drawMode <= 9) {
				Toast.makeText(this, "Drawing: " + drawMode, Toast.LENGTH_LONG).show();
			}
			
			switch (drawMode){
			
			case 0: 
				letters.drawZero();
				break;
	
			case 1:
				letters.drawOne();
				break;
	
			case 2:
				letters.drawTwo();
				break;
	
			case 3:
				letters.drawThree();
				break;
	
			case 4:
				letters.drawFour();
				break;
	
			case 5:
				letters.drawFive();
				break;
	
			case 6:
				letters.drawSix();
				break;
	
			case 7:
				letters.drawSeven();
				break;
	
			case 8:
				letters.drawEight();
				break;
	
			case 9:
				letters.drawNine();
				break;
	
			default: 
				letters.stopAllMotors();
				break;
			}
		} catch (NumberFormatException e){
			e.printStackTrace();
			Toast.makeText(this, "Only number support!", Toast.LENGTH_LONG).show();
		}
	}

}

