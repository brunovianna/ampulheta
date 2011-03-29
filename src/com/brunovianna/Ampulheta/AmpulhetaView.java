package com.brunovianna.Ampulheta;

import java.util.ArrayList;
import java.util.Iterator;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.NinePatch;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Handler;
import android.os.Message;
import android.view.View;


public class AmpulhetaView extends View implements View.OnClickListener {

	private ArrayList<PoemLine> poem, poem_up, poem_down;
	private Bitmap ampulheta, resizedAmpulheta;
	private Resources res;
	private Paint paint, buttonPaint;
	private boolean drop;
	public float ax, ay, maxAx,maxAy, angle;
	public int dWidth, dHeight, canvasWidth, canvasHeight;

	private float changePoemPointer, changePoemDelta;
	private int changePoem_i, changePoem_j = 0;
	private Iterator <PoemLine> changePoemIterator;
	private PoemLine changePoemLine;
	private Iterator <Letter> changePoemLetters;
	private boolean allDropped = true, finalPos = true;


	private NinePatchDrawable npd;
	private NinePatch np;
	private String reiniciar = "reiniciar";
	private Rect buttonRect, reiniciarRect;

	private RefreshHandler mRedrawHandler = new RefreshHandler();

	private long mLastMove = System.currentTimeMillis();;

	private long mRefresh = 60;

	class RefreshHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			AmpulhetaView.this.update();
			AmpulhetaView.this.invalidate();
		}

		public void sleep(long delayMillis) {
			this.removeMessages(0);
			sendMessageDelayed(obtainMessage(0), delayMillis);
		}
	};


	public AmpulhetaView(Context context, int cWidth, int cHeight) {
		super(context);
		
		drop = false;
		ax = 0f;
		ay = 1.0f;

		maxAx = maxAy = 10f;
		float friction = 0.9f;
		float angle = 0f;

		dWidth = cWidth;
		dHeight = cHeight;

		res = getResources();
		

		this.setOnClickListener(this);

		npd = (NinePatchDrawable) res.getDrawable(R.drawable.btn_default_normal);

		reiniciarRect = new Rect();

		buttonPaint = new Paint();
		buttonPaint.setColor(Color.BLACK);
		buttonPaint.setTextSize(16);
		buttonPaint.setFakeBoldText(true);
		buttonPaint.setAntiAlias(true);

		buttonPaint.getTextBounds(reiniciar, 0, reiniciar.length()-1, reiniciarRect);
		buttonRect = new Rect(reiniciarRect);
		buttonRect.inset(-12, -12);
		//buttonRect.offsetTo(100, 100);
		buttonRect.offsetTo((dWidth-buttonRect.width())/2, (dHeight-buttonRect.height())/2);
		npd.setBounds(buttonRect);

		//ampulheta bg
		ampulheta = BitmapFactory.decodeResource(getResources(), R.drawable.ampulheta);

		// recreate the new Bitmap
		resizedAmpulheta = Bitmap.createScaledBitmap(ampulheta, dWidth, dHeight, false);


		// set the color and font size
		paint = new Paint();
		paint.setColor(Color.BLACK);
		paint.setTextSize(16);
		paint.setFakeBoldText(true);
		paint.setAntiAlias(true);

		//read poem and distribute in display
		poem = new ArrayList<PoemLine>();
		poem_up = new ArrayList<PoemLine>();
		poem_down = new ArrayList<PoemLine>();

		CharSequence[] acs = res.getTextArray(R.array.poem_up);

		changePoemPointer = cHeight / 2;

		int letterCount = 0;

		for (int j = 0; j < acs.length; j++) {
			CharSequence cs = acs[j];
			letterCount =+ cs.length();
			PoemLine pl = new PoemLine(cs, angle, friction, "poem-up");
			pl.aX = ax;
			pl.aY = ay;
			pl.maxAx = maxAx;
			pl.maxAy = maxAy;
			//pl.distributeLine(dWidth, (int) ((float)dHeight / 3.0f), resizedAmpulheta, paint);
			poem_up.add(pl);	
		}

		changePoemDelta = ((cHeight/2)-10)/letterCount;
		changePoemIterator = null;

		acs = res.getTextArray(R.array.poem_down);
		for (int j = 0; j < acs.length; j++) {
			CharSequence cs = acs[j];
			PoemLine pl = new PoemLine(cs, angle, friction, "poem-down");
			pl.aX = ax;
			pl.aY = ay;
			pl.maxAx = maxAx;
			pl.maxAy = maxAy;
			//pl.distributeLine(dWidth, (int) ((float)dHeight / 3.0f), resizedAmpulheta, paint);
			poem_down.add(pl);	
		}

		poem = poem_down;

		//never gets drawn until the end
		writePoemCentered(dWidth, dHeight, 130, 14, paint, false);

		poem = poem_up;

		writePoemCentered(dWidth, dHeight, 20, 14, paint, true);

		mRedrawHandler.sleep(60);		

		//		Button b = new Button(context);
		//		
		//		ArrayList <View> bbb = new ArrayList<View>();
		//
		//		b.setVisibility(VISIBLE);
		//		b.setEnabled(true);
		//		b.setWidth(200);
		//		
		//		RelativeLayout rl = new RelativeLayout(context); 
		//		rl.addView(b);
		//
		//		bbb.add(rl);
		//		this.addTouchables(bbb);

	}

	public void writePoemCentered (int w, int h, int y, int dy, Paint paint, boolean direction){
		Iterator<PoemLine> it = poem.iterator();
		if (direction == true) {
			//normal
			while (it.hasNext()){
				PoemLine pl;
				pl = it.next();
				pl.writeCenteredLine (w, h, y, paint, direction);
				y += dy;
			}
		} else {
			//upside-down
			while (it.hasNext()){
				PoemLine pl;
				pl = it.next();
				pl.writeCenteredLine (w, h, y, paint, direction);
				y -= dy;
			}
		}
	}

	public void onDraw (Canvas mCanvas) {
		super.onDraw(mCanvas);

		mCanvas.drawBitmap(resizedAmpulheta, 0, 0, null);		

		Iterator<PoemLine> it = poem.iterator();

		allDropped = true;
		finalPos = true;

		while (it.hasNext()) {
			PoemLine pl = it.next();
			if (pl.checkDropped(resizedAmpulheta)==false) 
				allDropped = false;

			if (pl.checkFinal()==false) 
				finalPos = false;
		}

		if ((finalPos)&&(poem==poem_down)) {
			npd.draw(mCanvas);

			mCanvas.save();
			mCanvas.rotate(180, dWidth/2, dHeight/2);
			mCanvas.drawText(reiniciar, (dWidth-reiniciarRect.width())/2-4, (dHeight)/2+6, buttonPaint);
			mCanvas.restore();

		}

		if ((allDropped == true)&&(poem!=poem_down))
		{

			if (changePoemIterator == null) { 
				changePoemIterator = poem.iterator();
				changePoemLine = changePoemIterator.next();
				changePoemLetters = changePoemLine.letters.iterator();
			}

			Letter l = null;

			//first letter
			float currentHeight = poem.get(0).letters.get(0).y;

			if (currentHeight > changePoemPointer) {
				changePoemPointer =+ changePoemDelta;
				if (!changePoemLetters.hasNext()) 
					if (changePoemIterator.hasNext()) {
						changePoemLine = changePoemIterator.next();
						changePoemLetters = changePoemLine.letters.iterator();
					} else {
						//use letters from the beginning
						changePoemIterator = poem_down.iterator();
						changePoemLine = changePoemIterator.next();
						changePoemLetters = changePoemLine.letters.iterator();
					}
				l = changePoemLetters.next();
				l.text = poem_down.get(changePoem_i).letters.get(changePoem_j).text;

				poem_down.get(changePoem_i).letters.get(changePoem_j).x = l.x;
				poem_down.get(changePoem_i).letters.get(changePoem_j).y = l.y;
				poem_down.get(changePoem_i).letters.get(changePoem_j).vx = l.vx;
				poem_down.get(changePoem_i).letters.get(changePoem_j).vy = l.vy;
				poem_down.get(changePoem_i).letters.get(changePoem_j).ax = l.ax;
				poem_down.get(changePoem_i).letters.get(changePoem_j).ay = l.ay;

				if (changePoem_j<poem_down.get(changePoem_i).letters.size()-2) {
					changePoem_j++;
				} else {
					if (changePoem_i<poem_down.size()-2) {
						changePoem_i++;
						changePoem_j = 0;
					}	else {
						changePoem_i = 0;
						changePoem_j = 0;
						poem = poem_down;
					}
				}
			}

		
		}
	
//		for (int i=0; i<poem.size();i++){
//			PoemLine pl = poem.get(i);
//			pl.drawLine(mCanvas, paint, resizedAmpulheta);
//		}	
	
		it = poem.iterator();
		while (it.hasNext()) {
			PoemLine pl = it.next();
			pl.drawLine(mCanvas, paint, resizedAmpulheta);
		}

		
	}

	public void onClick(View v) {
		drop = true;
		if (finalPos) {
			//drop = false;
			poem = poem_up;
			writePoemCentered(dWidth, dHeight, 20, 14, paint, true);
			Iterator<PoemLine> it = poem.iterator();
			while (it.hasNext()) {
				it.next().updatePos(paint, resizedAmpulheta, ax, ay, angle);
			}
		}

	}

	public void update() {

		if (drop) {
			long now = System.currentTimeMillis();

			if (now - mLastMove > mRefresh) {
				Iterator<PoemLine> it = poem.iterator();
				while (it.hasNext()) {
					it.next().updatePos(paint, resizedAmpulheta, ax, ay, angle);
				}

				mLastMove = now;
			}
		}
		mRedrawHandler.sleep(mRefresh);

	}

}
