package com.brunovianna.Ampulheta;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.lang.String;

import com.brunovianna.Ampulheta.R;
import com.brunovianna.Ampulheta.R.id;
import com.brunovianna.Ampulheta.R.layout;
import com.brunovianna.Ampulheta.R.menu;
import com.brunovianna.Ampulheta.R.array;



//import com.example.android.apis.R;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.view.Surface;
import android.widget.TextView;

public class Ampulheta extends Activity implements Button.OnClickListener  {

	private SensorManager myManager;
	private List<Sensor> sensors;
	private Sensor accSensor, magSensor, orientationSensor;
	private AmpulhetaView ampulhetaView;
	private float accFactor;
	private float  thisX, thisY, thisZ;
	private float oldX, oldY, oldZ = 0f;
	float[] RotationMatrix, InclinationMatrix, accels, mags, outR, values;
	private Display display;
	private PowerManager.WakeLock wl;


	/** Called when the activity is first created. */
	@SuppressWarnings("deprecation")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		display = getWindowManager().getDefaultDisplay(); 


		setContentView(R.layout.buttons);

		Button b = (Button) findViewById(R.id.button1);
		//
		b.setOnClickListener(this);
		//		
		// Set Sensor + Manager
		myManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
		sensors = myManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
		if(sensors.size() > 0)
		{
			accSensor = sensors.get(0);
		}
		sensors = myManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD);
		if(sensors.size() > 0)
		{
			magSensor = sensors.get(0);
		}
		sensors = myManager.getSensorList(Sensor.TYPE_ORIENTATION);
		if(sensors.size() > 0)
		{
			orientationSensor = sensors.get(0);
		}

		int matrix_size = 16; 
		// matrices for letting SensorManager do its magic 
		RotationMatrix = new float[matrix_size]; 
		InclinationMatrix = new float[matrix_size]; 

		accels = new float[3];
		mags = new float[3];

		// an output matrix, that will hold a rotation matrix that 
		// can be used in openGl as a modelview matrix 
		outR = new float[matrix_size]; 
		// the orientation rotation array 
		values = new float[3]; 

		accFactor = 0.01f;

		//		mHandler.removeCallbacks(updateAcc);
		//		mHandler.postDelayed(updateAcc, period);		

		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, getClass().getName());
		//wl.acquire();

	}

	/*    	private Runnable updatePos = new Runnable() {
		@SuppressWarnings("deprecation")
		public void run() {
			if (drop==true) {
			}

			mHandler.postDelayed(this, period);

		}
	};*/



	public void onClick (View bb) {
		ampulhetaView  = new AmpulhetaView (this, display.getWidth(), display.getHeight());

		setContentView(ampulhetaView);

	}

	private void updateTV(float x, float y, float z)
	{
		thisX = x - oldX * 10;
		thisY = y - oldY * 10;
		thisZ = z - oldZ * 10;	

		//		ampulhetaView.ax = thisX * accFactor;
		//		ampulhetaView.ay = thisY * accFactor;

		ampulhetaView.ax = x * accFactor;
		ampulhetaView.ay = y * accFactor;



		oldX = x;
		oldY = y;
		oldZ = z;
	}

	private final SensorEventListener mySensorListener = new SensorEventListener()
	{
		public void onSensorChanged(SensorEvent event)
		{		

			Sensor sensor = event.sensor; 
			int type = sensor.getType(); 
			switch (type) { 
			case Sensor.TYPE_MAGNETIC_FIELD: 
				mags[0] = event.values[0]; 
				mags[1] = event.values[1]; 
				mags[2] = event.values[2]; 
				break; 
			case Sensor.TYPE_ACCELEROMETER: 
				accels[0] = event.values[0]; 
				accels[1] = event.values[1]; 
				accels[2] = event.values[2]; 
				break; 
			case Sensor.TYPE_ORIENTATION: 
				/****************************** 
				 *these are the orientation values in degrees - one of them is the 
				 * magnetic heading.... 
				 */ 
				// values = event.values.clone(); 
				break; 
			} 
			//this is key to getting your heading, it fills out the matrices 
			//which are needed to calculate the 
			//heading.  It is important to not that the acceleration data is 
			//linked to the magnetic data, in that the 
			//physical tilt/yaw of the phone affects the mags vector. Atleast 
			//this is my understanding. 
			SensorManager.getRotationMatrix(RotationMatrix, InclinationMatrix, 
					accels, mags); 
			// this is only necessary for my AR opengl purposes 
			//SensorManager.remapCoordinateSystem(RotationMatrix,  SensorManager.AXIS_Y, SensorManager.AXIS_MINUS_X, outR); 
			// This is the orientation that i need 
			// values[0] = compass in radians 
			// values[1] and values[2] are the rotations about the x and y axis 
			SensorManager.getOrientation(RotationMatrix, values); 
			// I have not used this function but it may give you the magnetic 
			// heading 
			// directly, in radians of course 
			//float magHeading = SensorManager.getInclination(InclinationMatrix);

			if (type == Sensor.TYPE_ACCELEROMETER) {
				switch (display.getRotation()) {
				case Surface.ROTATION_0:
					thisX = accels[0];
					thisY = accels[1];
					break;
				case Surface.ROTATION_90:
					thisX = -accels[1];
					thisY = accels[0];
					break;
				case Surface.ROTATION_180:
					thisX = -accels[0];
					thisY = -accels[1];
					break;
				case Surface.ROTATION_270:
					thisX = accels[1];
					thisY = -accels[0];
					break;
					//thisX = accels[0] - oldX * 10;
					//thisY = accels[1] - oldY * 10;
				}

				if (ampulhetaView != null) { 
					ampulhetaView.ax = thisX * accFactor;
					ampulhetaView.ay = thisY * accFactor;
				}
			}


			//ampulhetaView.ax = x * accFactor;
			//ampulhetaView.ay = y * accFactor;

			if (ampulhetaView != null)
				if (accels[1]>0)
					ampulhetaView.angle = -180f * values[2] / (float)Math.PI;
				else
					ampulhetaView.angle = 180f + 180f * values[2] / (float)Math.PI;
			oldX = accels[0];
			oldY = accels[1];
		}

		public void onAccuracyChanged(Sensor sensor, int accuracy) {}
	};

	@Override
	protected void onResume()
	{
		super.onResume();
		myManager.registerListener(mySensorListener, orientationSensor, SensorManager.SENSOR_DELAY_GAME); 	
		myManager.registerListener(mySensorListener, accSensor, SensorManager.SENSOR_DELAY_GAME); 	
		myManager.registerListener(mySensorListener, magSensor, SensorManager.SENSOR_DELAY_GAME); 	
		wl.acquire();
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		wl.release();
	}
	@Override
	protected void onStop()
	{    	
		myManager.unregisterListener(mySensorListener);
		//mHandler.removeCallbacks(updatePos);
		//wl.release();
		super.onStop();
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.aboutmenu, menu);
		return true;
	}



}