package com.brunovianna.ampulheta;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.BitmapFactory.Options;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.TextView;

class AmpulhetaView extends TextView implements SensorEventListener {
	// diameter of the balls in meters
	private static final float sBallDiameter = 0.004f;
	private static final float sBallDiameter2 = sBallDiameter * sBallDiameter;

	// friction of the virtual table and air
	private static final float sFriction = 0.1f;

	private Sensor mAccelerometer;
	private long mLastT;
	private float mLastDeltaT;

	private float mXDpi;
	private float mYDpi;
	private float mMetersToPixelsX;
	private float mMetersToPixelsY;
	private float mPixelsToMetersX;
	private float mPixelsToMetersY;
	private Bitmap mBitmap;
	private Bitmap mAmpulheta, mResizedAmpulheta = null;
	private float mXOrigin;
	private float mYOrigin;
	private float mSensorX;
	private float mSensorY;
	private long mSensorTimeStamp;
	private long mCpuTimeStamp;
	private float mHorizontalBound;
	private float mVerticalBound;
	private float mWinWidth;
	private float mWinHeight;
	private float letterCompensationWidth;
	private float letterCompensationHeight;

	private SensorManager mSensorManager;
	private WindowManager mWindowManager;
	private Display mDisplay;
	private ParticleSystem mParticleSystem;
	//private final LetterSystem mParticleSystem = new LetterSystem();

	private Paint paint, redPaint;

	private CharSequence[] poemUp, poemDown;
	private int numLetters = 0;
	private boolean isPoemUp;

	/*
	 * Each of our particle holds its previous and current position, its
	 * acceleration. for added realism each particle has its own friction
	 * coefficient.
	 */
	class Particle {
		private float mPosX;
		private float mPosY;
		private float mAccelX;
		private float mAccelY;
		private float mLastPosX;
		private float mLastPosY;
		private float mOneMinusFriction;
		public String mLetter, mLetterDown, mLetterUp;
		private float mHeight, mWidth, mWidthDown, mHeightDown, mWidthUp, mHeightUp;
		private boolean mIsUp;
		public float mFinalX, mFinalY;

		Particle() {
			// make each particle a bit different by randomizing its
			// coefficient of friction
			final float r = ((float) Math.random() - 0.5f) * 0.2f;
			mOneMinusFriction = 1.0f - sFriction + r;
		}

		public void setLetterUp (String l, Paint p) {
			mLetterUp = l;
			Rect r = new Rect();
			p.getTextBounds(mLetterUp, 0, 1, r);
			mWidthUp = (float)r.width() * mPixelsToMetersX;
			mHeightUp = (float)r.height()* mPixelsToMetersY;
			//			mHalfWidth = mWidth * 0.5f ;
			//			mHalfHeight = mHeight * 0.5f ;

			// position balls in the upper half
			mPosY = 100 * mPixelsToMetersY;

		}

		public void setLetterDown (String l, Paint p) {
			mLetterDown = l;
			Rect r = new Rect();
			p.getTextBounds(mLetterDown, 0, 1, r);
			mWidthDown = (float)r.width() * mPixelsToMetersX;
			mHeightDown = (float)r.height()* mPixelsToMetersY;
			//			mHalfWidth = mWidth * 0.5f ;
			//			mHalfHeight = mHeight * 0.5f ;

			// position balls in the upper half
			mPosY = 100 * mPixelsToMetersY;

		}

		public void setIsUp (boolean b) {
			mIsUp = b;
			if (b){
				mLetter = mLetterUp;
				mHeight = mHeightUp;
				mWidth = mWidthUp;
			} else {
				mLetter = mLetterDown;
				mHeight = mHeightDown;
				mWidth = mWidthDown;

			}
		}

		protected void updatePosition(float sx, float sy, long timestamp, float deltastamp) {
			final float dT = deltastamp;
			if (mLastT != 0) {
				if (mLastDeltaT != 0) {
					final float dTC = dT / mLastDeltaT;
						computePhysics(sx, sy, dT, dTC);
				}
			}
		}
		
		public void computePhysics(float sx, float sy, float dT, float dTC) {
			// Force of gravity applied to our virtual object
			final float m = 1000.0f; // mass of our virtual object
			final float gx = -sx * m;
			final float gy = -sy * m;

			/*
			 * �F = mA <=> A = �F / m We could simplify the code by
			 * completely eliminating "m" (the mass) from all the equations,
			 * but it would hide the concepts from this sample code.
			 */
			final float invm = 1.0f / m;
			final float ax = gx * invm;
			final float ay = gy * invm;

			/*
			 * Time-corrected Verlet integration The position Verlet
			 * integrator is defined as x(t+�t) = x(t) + x(t) - x(t-�t) +
			 * a(t)�t�2 However, the above equation doesn't handle variable
			 * �t very well, a time-corrected version is needed: x(t+�t) =
			 * x(t) + (x(t) - x(t-�t)) * (�t/�t_prev) + a(t)�t�2 We also add
			 * a simple friction term (f) to the equation: x(t+�t) = x(t) +
			 * (1-f) * (x(t) - x(t-�t)) * (�t/�t_prev) + a(t)�t�2
			 */
			final float dTdT = dT * dT;
			final float x = mPosX + mOneMinusFriction * dTC * (mPosX - mLastPosX) + mAccelX
			* dTdT;
			final float y = mPosY + mOneMinusFriction * dTC * (mPosY - mLastPosY) + mAccelY
			* dTdT;
			mLastPosX = mPosX;
			mLastPosY = mPosY;
			mPosX = x;
			mPosY = y;
			mAccelX = ax;
			mAccelY = ay;
		}

		/*
		 * Resolving constraints and collisions with the Verlet integrator
		 * can be very simple, we simply need to move a colliding or
		 * constrained particle in such way that the constraint is
		 * satisfied.
		 */
		public void resolveCollisionWithBounds() {
			final float xmax = mHorizontalBound;
			final float ymax = mVerticalBound;
			final float x = mPosX;
			final float y = mPosY;
			if (x + mWidth > xmax) {
				mPosX = xmax - mWidth;
			} else if (x < -xmax) {
				mPosX = -xmax;
			}
			if (y + mHeight > ymax) {
				mPosY = ymax -mHeight;
			} else if (y < -ymax) {
				mPosY = -ymax;
			}		
		}

		public void resolveCollisionWithSandclock() {
			// TODO Auto-generated method stub

			int x = (int) ((mPosX * mMetersToPixelsX) + mXOrigin);
			int y = (int) (- (mPosY * mMetersToPixelsY) + mYOrigin);

			int intWidth = (int) (mWidth * mMetersToPixelsX);
			int intHeight = (int) (mHeight * mMetersToPixelsY);

			if (mPosX<0) {
				if (mPosY > 0) {
					//above left
					while (findColorUnder(x,y) == Color.BLACK) 	{
						mPosX = mPosX + mPixelsToMetersX;
						x = (int) ((mPosX * mMetersToPixelsX) + mXOrigin);
					}
				} else {
					//under left
					while (findColorUnder(x, y - intHeight) == Color.BLACK) {
						mPosX = mPosX + mPixelsToMetersX;					
						x = (int) ((mPosX * mMetersToPixelsX) + mXOrigin);
					}
				}
			} else {
				if (mPosY > 0) {
					//above right
					while (findColorUnder(x + intWidth, y) == Color.BLACK) { 	
						mPosX = mPosX - mPixelsToMetersX;					
						x = (int) ((mPosX * mMetersToPixelsX) + mXOrigin);
					}
				} else {
					while (findColorUnder(x + intWidth, y - intHeight) == Color.BLACK) { 	
						mPosX = mPosX - mPixelsToMetersX;					
						x = (int) ((mPosX * mMetersToPixelsX) + mXOrigin);
					}
				}
			}


		}

		//find pixel color under
		public int findColorUnder(int x, int y) {


			//			if ((x<0)||(y<0)||(x>=mResizedAmpulheta.getWidth()||y>=mResizedAmpulheta.getHeight())) {
			//				mPosY = 150 / mMetersToPixelsX;
			//				mPosX = 0;
			//				return Color.WHITE;
			//				
			//			}

			try {
				int color =mResizedAmpulheta.getPixel(x, y);
				return color;
			} catch (IllegalArgumentException e) {
				return Color.WHITE;	
			}			
		}
	}

	/*
	 * A particle system is just a collection of particles
	 */
	class ParticleSystem {
		private Particle mBalls[];

		ParticleSystem(int num) {

			mBalls = new Particle[num];

			// temporary
			Paint p = new Paint();
			p.setColor(Color.BLACK);
			p.setTextSize(16);
			p.setFakeBoldText(true);
			p.setAntiAlias(true);


			/*
			 * Initially our particles have no speed or acceleration
			 */
			for (int i = 0; i < mBalls.length; i++) {
				mBalls[i] = new Particle();
			}

			int ballCount = 0;
			for (int i=0; i<poemUp.length;i++)
				for (int j=0;j<poemUp[i].length();j++) {
					mBalls[ballCount].setLetterUp(String.valueOf(poemUp[i].charAt(j)), p);
					ballCount ++;
				}

			ballCount = 0;
			for (int i=0; i<poemDown.length;i++)
				for (int j=0;j<poemDown[i].length();j++) {
					mBalls[ballCount].setLetterDown(String.valueOf(poemDown[i].charAt(j)), p);
					ballCount ++;
				}	
			//down poem is shorter
			while (ballCount < mBalls.length ) {
				mBalls[ballCount].setLetterDown(" ",p);
				ballCount++;
			}


			for (int i = 0; i < mBalls.length; i++) {
				mBalls[i].setIsUp(true);
			}

			//routines to find the initial and final position - build the sentences
			ballCount = 0;
			float x = 0;
			float y = 200 * mPixelsToMetersY;
			Rect r = new Rect();
			for (int i=0; i<poemUp.length; i++) {
				p.getTextBounds(poemUp[i].toString(), 0, poemUp[i].length()-1, r);
				x = - r.width() * 0.5f * mPixelsToMetersX;
				for (int j= 0; j<poemUp[i].length(); j++) {
					mBalls[ballCount].mPosX = x;
					mBalls[ballCount].mPosY = y;
					mBalls[ballCount].mLastPosX = x;
					mBalls[ballCount].mLastPosY = y;
					String measure = mBalls[ballCount].mLetterUp;
					if (measure.compareTo(" ")==0)
						measure = "|";
					p.getTextBounds(measure, 0, 1, r);
					x = x + r.width() * mPixelsToMetersX;
					ballCount ++;
				}
				y = y - r.height() * mPixelsToMetersY * 1.2f;
			}

			ballCount = 0;
			y = 100 * mPixelsToMetersY;
			for (int i=0; i<poemDown.length; i++) {
				p.getTextBounds(poemDown[i].toString(), 0, poemDown[i].length()-1, r);
				x = r.width() * 0.5f * mPixelsToMetersX;
				for (int j= 0; j<poemDown[i].length(); j++) {
					mBalls[ballCount].mFinalX = x;
					mBalls[ballCount].mFinalY = y;
					String measure = mBalls[ballCount].mLetterDown;
					if (measure.compareTo(" ")==0)
						measure = "|";
					p.getTextBounds(measure, 0, 1, r);
					x = x - r.width() * mPixelsToMetersX;
					ballCount ++;
				}
				y = y + r.height() * mPixelsToMetersY * 1.2f;
			}

			//down poem is shorter
			while (ballCount < mBalls.length ) {
				mBalls[ballCount].mFinalX = -200;
				mBalls[ballCount].mFinalY = -200;
				ballCount++;
			}

		}

		/*
		 * Update the position of each particle in the system using the
		 * Verlet integrator.
		 */
		protected void updatePositions(float sx, float sy, long timestamp) {
			final long t = timestamp;
			if (mLastT != 0) {
				final float dT = (float) (t - mLastT) * (1.0f / 1000000000.0f);
				if (mLastDeltaT != 0) {
					final float dTC = dT / mLastDeltaT;
					final int count = mBalls.length;
					for (int i = 0; i < count; i++) {
						Particle ball = mBalls[i];
						ball.computePhysics(sx, sy, dT, dTC);
					}
				}
				mLastDeltaT = dT;
			}
			mLastT = t;
		}

		
		/*
		 * Performs one iteration of the simulation. First updating the
		 * position of all the particles and resolving the constraints and
		 * collisions.
		 */
		public void update(float sx, float sy, long now) {
			
			final int count = mBalls.length;
			final float dT = (float) (now - mLastT) * (1.0f / 1000000000.0f);
			for (int i = 0; i < count; i++) {
				Particle curr = mBalls[i];
				curr.updatePosition(sx, sy, now, dT);
			}	
			mLastT = now;
			mLastDeltaT = dT;
			
			boolean allDown = true;
			for (int i = 0; i < count; i++) {
				Particle curr = mBalls[i];
				// update the system's positions
				curr.resolveCollisionWithBounds();
				curr.resolveCollisionWithSandclock();

				//start changing the poem
				if (curr.mPosY < 0)
					curr.setIsUp(false);
				else
					allDown = false;
			}
			if (allDown)
				isPoemUp = false;
		}

		public int getParticleCount() {
			return mBalls.length;
		}

		public float getPosX(int i) {
			return mBalls[i].mPosX;
		}

		public float getPosY(int i) {
			return mBalls[i].mPosY;
		}

		public String getString(int i) {
			if (isPoemUp)
				return mBalls[i].mLetter;
			else
				return mBalls[i].mLetter;
		}

	}	

	public void setSensorManager(SensorManager sm) {
		mSensorManager = sm;
		mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

	}

	public void setWindowManager(WindowManager wm) {

		DisplayMetrics metrics = new DisplayMetrics();

		mWindowManager = wm;
		mDisplay = wm.getDefaultDisplay();
		mDisplay.getMetrics(metrics);
		mXDpi = metrics.xdpi;
		mYDpi = metrics.ydpi;
		mMetersToPixelsX = mXDpi / 0.0254f;
		mMetersToPixelsY = mYDpi / 0.0254f;
		mPixelsToMetersX = 1 / mMetersToPixelsX;
		mPixelsToMetersY = 1 / mMetersToPixelsY;

		// rescale the ball so it's about 0.5 cm on screen
		Bitmap ball = BitmapFactory.decodeResource(getResources(), R.drawable.ball);
		final int dstWidth = (int) (sBallDiameter * mMetersToPixelsX + 0.5f);
		final int dstHeight = (int) (sBallDiameter * mMetersToPixelsY + 0.5f);
		mBitmap = Bitmap.createScaledBitmap(ball, dstWidth, dstHeight, true);

		Options opts = new Options();
		opts.inDither = true;
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		mAmpulheta = BitmapFactory.decodeResource(getResources(), R.drawable.ampulheta, opts);

		mParticleSystem = new ParticleSystem(numLetters);


	}

	public void startSimulation() {
		/*
		 * It is not necessary to get accelerometer events at a very high
		 * rate, by using a slower rate (SENSOR_DELAY_UI), we get an
		 * automatic low-pass filter, which "extracts" the gravity component
		 * of the acceleration. As an added benefit, we use less power and
		 * CPU resources.
		 */
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_UI);
	}

	public void stopSimulation() {
		mSensorManager.unregisterListener(this);
	}

	public AmpulhetaView(Context context, AttributeSet attrs) {
		super(context, attrs);

		// set the color and font size
		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setTextSize(16);
		paint.setFakeBoldText(true);
		paint.setAntiAlias(true);

		// set the color and font size
		redPaint = new Paint();
		redPaint.setColor(Color.RED);

		Resources res = getResources();

		poemUp = res.getTextArray(R.array.poem_up);
		poemDown = res.getTextArray(R.array.poem_down);

		//count letters
		int poemUpCount = 0;
		for (int i = 0; i<poemUp.length; i++) {
			poemUpCount += poemUp[i].length();
		}
		//count letters
		int poemDownCount = 0;
		for (int i = 0; i<poemDown.length; i++) {
			poemDownCount += poemDown[i].length();
		}

		if (poemUpCount > poemDownCount) 
			numLetters = poemUpCount;
		else 
			numLetters = poemDownCount;

	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		// compute the origin of the screen relative to the origin of
		// the bitmap
		mXOrigin = (w /*- mBitmap.getWidth()*/) * 0.5f;
		mYOrigin = (h /*- mBitmap.getHeight()*/) * 0.5f;
		mHorizontalBound = ((w * mPixelsToMetersX ) * 0.5f);
		mVerticalBound = ((h* mPixelsToMetersY ) * 0.5f);

		mResizedAmpulheta = Bitmap.createScaledBitmap(mAmpulheta, w, h, true);
		mWinWidth = w;
		mWinHeight = h;
	}

	@Override
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;
		/*
		 * record the accelerometer data, the event's timestamp as well as
		 * the current time. The latter is needed so we can calculate the
		 * "present" time during rendering. In this application, we need to
		 * take into account how the screen is rotated with respect to the
		 * sensors (which always return data in a coordinate space aligned
		 * to with the screen in its native orientation).
		 */

		switch (mDisplay.getRotation()) {
		case Surface.ROTATION_0:
			mSensorX = event.values[0];
			mSensorY = event.values[1];
			break;
		case Surface.ROTATION_90:
			mSensorX = -event.values[1];
			mSensorY = event.values[0];
			break;
		case Surface.ROTATION_180:
			mSensorX = -event.values[0];
			mSensorY = -event.values[1];
			break;
		case Surface.ROTATION_270:
			mSensorX = event.values[1];
			mSensorY = -event.values[0];
			break;
		}

		mSensorTimeStamp = event.timestamp;
		mCpuTimeStamp = System.nanoTime();
	}

	@Override
	protected void onDraw(Canvas canvas) {
		//super.onDraw(canvas);
		/*
		 * draw the background
		 */
		if (mResizedAmpulheta != null){

			canvas.drawBitmap(mResizedAmpulheta, 0, 0, null);

			/*
			 * compute the new position of our object, based on accelerometer
			 * data and present time.
			 */

			final ParticleSystem particleSystem = mParticleSystem;
			final long now = mSensorTimeStamp + (System.nanoTime() - mCpuTimeStamp);
			final float sx = 0f;//mSensorX;
			final float sy = 0.05f;//mSensorY;



			particleSystem.update(sx, sy, now);

			final float xc = mXOrigin;
			final float yc = mYOrigin;
			final float xs = mMetersToPixelsX;
			final float ys = mMetersToPixelsY;
			final Bitmap bitmap = mBitmap;
			final int count = particleSystem.getParticleCount();
			for (int i = 0; i < count; i++) {
				/*
				 * We transform the canvas so that the coordinate system matches
				 * the sensors coordinate system with the origin in the center
				 * of the screen and the unit is the meter.
				 */


				final float x = xc + particleSystem.getPosX(i) * xs;
				final float y = yc - particleSystem.getPosY(i) * ys;

				canvas.drawText(particleSystem.getString(i), x, y, paint);
				canvas.drawPoint(x, y, redPaint);
				//canvas.drawRect(x, y, x+letterCompensationWidth, y+letterCompensationHeight, paint);
			}

			// and make sure to redraw asap
			invalidate();
		}

	}
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {   }
}
