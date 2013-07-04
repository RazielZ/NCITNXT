package com.ncit.nxt;
import java.util.ArrayList;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLayoutChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.RelativeLayout.LayoutParams;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener, DrawModeCallBackInterface {

	private static final int REQUEST_ENABLE_BT = 1;
	private static final int REQUEST_CONNECT_DEVICE = 2;
	private static final int REQUEST_SETTINGS = 3;

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
	private Button testConnectButton;
	private Button switchButton;

	//Buton selectare mod scriere: 0 - Vertical, 1 - Orizontal etc.
	private Button bSwitchDrawMode;

	//Variabila pentru modul de desenare selectat
	private int drawMode = 0;
	//Variabila pentru nr total de moduri
	private int noDrawingModes = 10;

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
	private Button bVerticalLine;
	//putere pentru desenare
	private byte drawPower[] = new byte[3];
	private Letters letters;

	private TabHost mTabHost;
	private TabSpec tab1, tab2, tab3;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

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
		bSwitchDrawMode = (Button) findViewById(R.id.bSwitchDrawMode);
		bSwitchDrawMode.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				drawMode = (drawMode + 1) % noDrawingModes;

				if (drawMode == 0) {
					bVerticalLine.setText("0");
				}
				if (drawMode == 1) {
					bVerticalLine.setText("1");
				}
				if (drawMode == 2) {
					bVerticalLine.setText("2");
				}
				if (drawMode == 3) {
					bVerticalLine.setText("3");
				}
				if (drawMode == 4) {
					bVerticalLine.setText("4");
				}
				if (drawMode == 5) {
					bVerticalLine.setText("5");
				}
				if (drawMode == 6) {
					bVerticalLine.setText("6");
				}
				if (drawMode == 7) {
					bVerticalLine.setText("7");
				}
				if (drawMode == 8) {
					bVerticalLine.setText("8");
				}
				if (drawMode == 9) {
					bVerticalLine.setText("9");
				}
			}
		});

		//Afisare viteze motoare conform seekBars
		tvShowSpeeds[0] = (TextView) findViewById(R.id.tvShowSpeed1);
		tvShowSpeeds[1] = (TextView) findViewById(R.id.tvShowSpeed2);
		tvShowSpeeds[2] = (TextView) findViewById(R.id.tvShowSpeed3);

		//Buton pentru desenare linie verticala
		bVerticalLine = (Button) findViewById(R.id.bVerticalLine);		
		bVerticalLine.setOnClickListener(new DrawLettersOnClickListener(letters,this));

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

		testConnectButton = (Button) findViewById(R.id.testconnectbutton);
		testConnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				findBrick();
			}
		});

		switchButton = (Button)findViewById(R.id.modeswitch);
		switchButton.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				sensorMode = !sensorMode;
				mNXTTalker.motor(NXTTalker.MOTOR1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				mNXTTalker.motor(NXTTalker.MOTOR2, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				mNXTTalker.motor(NXTTalker.MOTOR3, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				if (sensorMode) {
					firstOrientation = true;
					testStatusTextView.setText(stateText+" (Sensor Mode)");
				} else {
					testStatusTextView.setText(stateText+" (Button Mode)");
				}
			}
		});
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
				//Toast.makeText(this, address, Toast.LENGTH_LONG).show();
				mDeviceAddress = address;
				mNXTTalker.connect(device);
			}
			break;
		case REQUEST_SETTINGS:
			//XXX?
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

		private double motormod;
		private int power;
		private int sign;
		private byte motorB;
		private int motorBi;

		public MotorButtonListener(int motor, int dir, double powerMod, int ppower) {
			motormod = powerMod;
			power = dir;
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
					Log.d("MMOD", "Motormod: "+motormod);
					Log.d("cPWR", "Current Power: "+power);
					Log.d("fPWR", "Final Power: "+fPower);

					Log.d ("power1", "action= motor: " + motorB + " fpower: " + fPower);
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
		// TODO Auto-generated method stub

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

				if (Math.abs(linear_acceleration[0])>5) {
					byte power = (byte) (20*Math.signum(linear_acceleration[0]));
					mNXTTalker.motor(mNXTTalker.MOTOR1, power, mRegulateSpeed, mSynchronizeMotors);
				} else {
					mNXTTalker.motor(mNXTTalker.MOTOR1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}
				if (Math.abs(linear_acceleration[1])>4) {
					byte power = (byte) (20*Math.signum(linear_acceleration[1]));
					mNXTTalker.motor(mNXTTalker.MOTOR2, power, mRegulateSpeed, mSynchronizeMotors);
				} else {
					mNXTTalker.motor(mNXTTalker.MOTOR2, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}
			} else {
				Log.d("ORI1", "Orientation value 1: "+event.values[0]);
				if (firstOrientation) {
					initialOri = event.values[0];
					firstOrientation = false;
				} else {
					double currentOri = event.values[0];
					currentOri -= initialOri;
					if (currentOri<0) {
						currentOri = 360 + currentOri;
					}
					if (currentOri<180&&currentOri>30) {
						byte power = (byte) (12);
						mNXTTalker.motor(mNXTTalker.MOTOR3, power, mRegulateSpeed, mSynchronizeMotors);
					} else if (currentOri<330&&currentOri>180) {
						byte power = (byte) (-12);
						mNXTTalker.motor(mNXTTalker.MOTOR3, power, mRegulateSpeed, mSynchronizeMotors);
					} else {
						mNXTTalker.motor(mNXTTalker.MOTOR3, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
					}
				}
			}
		}
	}


	@Override
	public int getDrawMode() {
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
}

