package com.ncit.nxt;
import java.util.ArrayList;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
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
import android.view.View.OnTouchListener;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements SensorEventListener {

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

	private ArrayList<Button> motorButtons = new ArrayList<Button>();
	private Button testConnectButton;
	private Button switchButton;
	
	private Button bCalibration;
	
	//Buton selectare mod scriere: 0 - Vertical, 1 - Orizontal etc.
	private Button bSwitchDrawMode;
	
	//Variabila pentru modul de desenare selectat
	private int drawMode = 0;
	//Variabila pentru nr total de moduri
	private int noDrawingModes = 5;
	
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
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);

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

		setupUI();

		mNXTTalker = new NXTTalker(mHandler);
		
	}

	@Override
	protected void onStart() {
		super.onStart();
		//Log.i("NXT", "NXTRemoteControl.onStart()");
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

	private void setupUI() {
		
		//Calibration Button
		bCalibration = (Button) findViewById (R.id.bCalibration);
		bCalibration.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				calibration();
			}
		});
		
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
				//Linie verticala sus-jos
				if (drawMode == 0) {
					bVerticalLine.setText("VLine");
					speedMotors[0] = 5;
					speedMotors[1] = 8;
					speedMotors[2] = 6;
					seekBars[0].setProgress(5);
					seekBars[1].setProgress(8);
					seekBars[2].setProgress(6);
				}
				//Linie orizontala stanga-dreapta
				if (drawMode == 1) {
					bVerticalLine.setText("HLine");
					speedMotors[0] = 3;
					speedMotors[1] = 2;
					speedMotors[2] = 6;
					seekBars[0].setProgress(3);
					seekBars[1].setProgress(2);
					seekBars[2].setProgress(6);
				}
				//Linie verticala jos-sus
				if (drawMode == 2) {
					bVerticalLine.setText("-VLine");
					speedMotors[0] = 4;
					speedMotors[1] = 8;
					speedMotors[2] = 6;
					seekBars[0].setProgress(4);
					seekBars[1].setProgress(8);
					seekBars[2].setProgress(6);
				}
				//Linie orizontala dreapta-stanga
				if (drawMode == 3) {
					bVerticalLine.setText("-HLine");
					speedMotors[0] = 2;
					speedMotors[1] = 0;
					speedMotors[2] = 5;
					seekBars[0].setProgress(2);
					seekBars[1].setProgress(0);
					seekBars[2].setProgress(5);
				}
				//Deseneaza 7
				if (drawMode == 4) {
					bVerticalLine.setText("7");
				}
			}
		});
		
		//Afisare viteze motoare conform seekBars
		tvShowSpeeds[0] = (TextView) findViewById(R.id.tvShowSpeed1);
		tvShowSpeeds[1] = (TextView) findViewById(R.id.tvShowSpeed2);
		tvShowSpeeds[2] = (TextView) findViewById(R.id.tvShowSpeed3);
		
		//Buton pentru desenare linie verticala
		bVerticalLine = (Button) findViewById(R.id.bVerticalLine);		
		bVerticalLine.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.d("Case", "OnTouch");
				Thread control = new Thread(new Runnable() {
					int sleepTime = 500;
					@Override
					public void run() {
						
						switch (drawMode) {
						
						case 0: 
							Log.d ("Case", "Case 0");
							drawVerticalLine();
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopAllMotors();
							break;
								
						case 1:
							Log.d ("Case", "Case 1");
							drawHorizontalLine();
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopAllMotors();
							break;
						
						case 2:
							Log.d ("Case", "Case 2");
							drawVerticalLine2(); //adica in sens invers
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopAllMotors();
							break;
							
						case 3:
							Log.d ("Case", "Case 3");
							drawHorizontalLine2(); // adica in sens invers
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopAllMotors();
							break;
							
						case 4: //Deseneaza cifra 7
							Log.d ("Case", "Case 4");
							speedMotors[0] = 4;
							speedMotors[1] = 8;
							speedMotors[2] = 6;
							drawVerticalLine2 ();
							
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							speedMotors[0] = 2;
							speedMotors[1] = 0;
							speedMotors[2] = 5;
							drawHorizontalLine2 ();
							try {
								Thread.sleep(sleepTime);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							stopAllMotors();
							break;
							
						default: stopAllMotors(); break;
						}
					}
				});
				control.start();
				try {
					control.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		
//		for (int i = 0; i < speedMotors.length; i++) {
//			speedMotors[i] = seekBars[i].getProgress();
//		}
		
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
						tvShowSpeeds[0].setText(progress + " %");
					}
					if (seekBar == seekBars[1]) {
						Log.d ("Progress", "sM= " + speedMotors[1] + " progress: " + progress);
						speedMotors[1] = progress;
						tvShowSpeeds[1].setText(progress + " %");
					}
					if (seekBar == seekBars[2]) {
						speedMotors[2] = progress;
						tvShowSpeeds[2].setText(progress + " %");
					}
					
					Log.d("SpeedBars", "Speed 1, 2, 3: " + speedMotors[0] + " " + speedMotors[1] + " " + speedMotors[2]);
					
				}
			});
		}
		
		motorButtons.add((Button) findViewById(R.id.m1b1));
		motorButtons.add((Button) findViewById(R.id.m1b2));
		motorButtons.add((Button) findViewById(R.id.m2b1));
		motorButtons.add((Button) findViewById(R.id.m2b2));
		motorButtons.add((Button) findViewById(R.id.m3b1));
		motorButtons.add((Button) findViewById(R.id.m3b2));

		testStatusTextView = (TextView) findViewById(R.id.testtext);
		testStatusTextView.setText(stateText+" (Button Mode)");
		for (int i = 0; i<motorButtons.size(); i++) {
			switch (i) {
			case 0: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(0,true,1.0d,15)); break;
			case 1: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(0,false,1.0d,15)); break;
			case 2: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(1,true,1.0d,15)); break;
			case 3: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(1,false,1.0d,15)); break;
			case 4: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(2,true,1.0d,8)); break;
			case 5: 
				motorButtons.get(i).setOnTouchListener(new MotorButtonListener(2,false,1.0d,8)); break;
			default: motorButtons.get(i).setOnTouchListener(new MotorButtonListener(i/2,false,1.0d,15)); break;
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
				mNXTTalker.motor(mNXTTalker.MOTOR1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				mNXTTalker.motor(mNXTTalker.MOTOR2, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				mNXTTalker.motor(mNXTTalker.MOTOR3, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
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
			//	            mConnectButton.setVisibility(View.VISIBLE);
			//	            mDisconnectButton.setVisibility(View.GONE);
			setProgressBarIndeterminateVisibility(false);
			if (mWakeLock.isHeld()) {
				mWakeLock.release();
			}
			break;
		case NXTTalker.STATE_CONNECTING:
			stateText = "Connecting...";
			color = 0xffffff00;
			//	            mConnectButton.setVisibility(View.GONE);
			//	            mDisconnectButton.setVisibility(View.GONE);
			setProgressBarIndeterminateVisibility(true);
			if (!mWakeLock.isHeld()) {
				mWakeLock.acquire();
			}
			break;
		case NXTTalker.STATE_CONNECTED:
			stateText = "Connected";
			color = 0xff00ff00;
			//	            mConnectButton.setVisibility(View.GONE);
			//	            mDisconnectButton.setVisibility(View.VISIBLE);
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
		private float power = 1;
		private byte motorB;

		public MotorButtonListener(int motor, boolean dir, double powerMod, int ppower) {
			motormod = powerMod;
			this.power = ppower;
			if (!dir){
				if (motor!=0) {
					this.power *= -1;
				}
			} else {
				if (motor==0) {
					this.power *= -1;
				}
			}
			Log.d("power1", "PowerInitial: "+ppower);
			switch (motor){ 
				case 0: motorB = NXTTalker.MOTOR1; break;
				case 1: motorB = NXTTalker.MOTOR2; break;
				case 2: motorB = NXTTalker.MOTOR3; break;
				default: motorB = NXTTalker.MOTOR1; break;
			}
		}

		@Override
		public boolean onTouch(View v, MotionEvent event) {
			float signum = (float) power;
			int action = event.getAction();
			
			switch (motorB) {
			
			case NXTTalker.MOTOR1: 
				Log.d("power1", "PowerSignum0: " + (int) Math.signum(signum));
				power = speedMotors[0] * (int) Math.signum(signum); 
				Log.d("power1", "pw0= " + power + " " + speedMotors[0]); break;

			case NXTTalker.MOTOR2: 
				Log.d("power1", "PowerSignum1: " + (int) Math.signum(signum) + " " + signum);
				power = speedMotors[1] * (int) Math.signum(signum); 
				Log.d("power1", "pw1= " + power + " " + speedMotors[1]); break;

			case NXTTalker.MOTOR3: 
				Log.d("power1", "PowerSignum2: " + (int) Math.signum(signum));
				power = speedMotors[2] * (int) Math.signum(signum); 
				Log.d("power1", "pw2= " + power + " " + speedMotors[2]); break;

			default: 
				power = speedMotors[0] * Math.round(Math.signum(signum)); break;
			}
			
			if (!sensorMode) {
				if (action == MotionEvent.ACTION_DOWN) {
					byte fPower = (byte) (power*motormod);
					Log.d("MMOD", "Motormod: "+motormod);
					Log.d("cPWR", "Current Power: "+power);
					Log.d("fPWR", "Final Power: "+fPower);
					
					mNXTTalker.motor(motorB, fPower, mRegulateSpeed, mSynchronizeMotors);
				} else if ((action == MotionEvent.ACTION_UP) || (action == MotionEvent.ACTION_CANCEL)) {
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

				//DO STUFF WITH CURRENT-LAST

				//overr last
				lastValues[0] = linear_acceleration[0];
				lastValues[1] = linear_acceleration[1];
				lastValues[2] = linear_acceleration[2];

				//	Log.d("LA", "Linear X: "+linear_acceleration[0]);
				if (Math.abs(linear_acceleration[0])>5) {
					byte power = (byte) (20*Math.signum(linear_acceleration[0]));
					//		Log.d("ACPWR","Power :"+power);
					mNXTTalker.motor(mNXTTalker.MOTOR1, power, mRegulateSpeed, mSynchronizeMotors);
				} else {
					mNXTTalker.motor(mNXTTalker.MOTOR1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}
				if (Math.abs(linear_acceleration[1])>4) {
					byte power = (byte) (20*Math.signum(linear_acceleration[1]));
					//	Log.d("ACPWR","Power :"+power);
					mNXTTalker.motor(mNXTTalker.MOTOR2, power, mRegulateSpeed, mSynchronizeMotors);
				} else {
					mNXTTalker.motor(mNXTTalker.MOTOR2, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
				}
			} else {
				//Log.d("ORI", "Orientation: "+event.values.toString());
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
	
	public void drawVerticalLine () {
		
		//Good default powers: 3,10,6
		drawPower[0] = (byte) (speedMotors[0]);
		drawPower[1] = (byte) (speedMotors[1]);
		
		mNXTTalker.motor(NXTTalker.MOTOR1, drawPower[0], mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR2, drawPower[1], mRegulateSpeed, mSynchronizeMotors);
	}
	
	public void drawHorizontalLine () {
		
		//Good defaul powers: 3,2,6 %
		drawPower[0] = (byte) (speedMotors[0]);
		drawPower[1] = (byte) ((-1) * speedMotors[1]);
		drawPower[2] = (byte) (speedMotors[2]);
				
		mNXTTalker.motor(NXTTalker.MOTOR1, drawPower[0], mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR2, drawPower[1], mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR3, drawPower[2], mRegulateSpeed, mSynchronizeMotors);
	}
	
	public void drawVerticalLine2 () {
		//Good default powers: 3,10,6
		drawPower[0] = (byte) ((-1) * speedMotors[0]);
		drawPower[1] = (byte) ((-1) * speedMotors[1]);
		
		mNXTTalker.motor(NXTTalker.MOTOR1, drawPower[0], mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR2, drawPower[1], mRegulateSpeed, mSynchronizeMotors);
	}
	
	public void drawHorizontalLine2 () {
		
		//Good defaul powers: 3,2,6 %
		drawPower[0] = (byte) ((-1) * speedMotors[0]);
		drawPower[1] = (byte) (speedMotors[1]);
		drawPower[2] = (byte) ((-1) * speedMotors[2]);
				
		mNXTTalker.motor(NXTTalker.MOTOR1, drawPower[0], mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR2, drawPower[1], mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR3, drawPower[2], mRegulateSpeed, mSynchronizeMotors);
	}
	
	public void stopAllMotors () {
		
		mNXTTalker.motor(NXTTalker.MOTOR1, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR2, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
		mNXTTalker.motor(NXTTalker.MOTOR3, (byte) 0, mRegulateSpeed, mSynchronizeMotors);
	}
	
	public void calibration () {
		new Thread(new Runnable() {
			int sleepTime = 1340;
			@Override
			public void run() {
				
				drawPower[1] = (byte) -20;
				mNXTTalker.motor(NXTTalker.MOTOR2, drawPower[1], mRegulateSpeed, mSynchronizeMotors);
				
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}						
				stopAllMotors();
			}
		}).start();
	}
	
}

