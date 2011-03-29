package com.brunovianna.Ampulheta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

public class PoemLine {
	public ArrayList<Letter> letters;

	public float aX, aY, maxAx, maxAy;

	public String name;

	private final int bottom = 52;

	public PoemLine (CharSequence cs, float angle, float friction, String n) {

		letters = new ArrayList<Letter>();

		name = n;

		for (int i = 0; i < cs.length(); i++) {
			Letter l = new Letter(String.valueOf(cs.charAt(i)));
			l.angle = angle;
			l.ay = aY;
			l.ax = aX;
			l.friction = friction;
			letters.add(l);
		}

	}

	public void distributeLine(int w, int h, Bitmap map, Paint paint) {

		Random randomGenerator = new Random();

		Iterator<Letter> il = letters.iterator();

		while (il.hasNext()) {

			Letter nl = il.next();

			Rect rect = new Rect();

			paint.getTextBounds(nl.text, 0, 1, rect);

			// avoid negative pixel pos 
			int randomX = randomGenerator.nextInt(w-rect.width()*2)+rect.width();
			int randomY = randomGenerator.nextInt(h-rect.height()*2)+rect.height();


			while ((map.getPixel(randomX, randomY)!=Color.WHITE)
					|| (map.getPixel(randomX, randomY-rect.height())!=Color.WHITE)
					|| (map.getPixel(randomX+rect.width(), randomY)!=Color.WHITE)
					|| (map.getPixel(randomX+rect.width(), randomY-rect.height())!=Color.WHITE)) {
				randomX = randomGenerator.nextInt(w-rect.width()*2)+rect.width();
				randomY = randomGenerator.nextInt(h-rect.height()*2)+rect.height();
			}
			nl.x = (float)randomX;
			nl.y = (float)randomY;
		}
	}


	public void writeLine(int dw, int dh, int x, int y, Paint paint, boolean direction) {

		Iterator<Letter> il = letters.iterator();

		while (il.hasNext()) {

			Letter nl = il.next();

			nl.x = nl.finalX = x;
			nl.y = nl.finalY = y;

			Rect rect = new Rect();
			String measure = nl.text;

			if (nl.text.compareTo(" ") == 0)
				measure = "|";

			paint.getTextBounds(measure, 0, 1, rect);

			if (direction)
				x +=  rect.width();
			else
				x -=  rect.width();
		}
	}

	public void writeCenteredLine (int dw, int dh, int y, Paint paint, boolean direction) {
		Iterator<Letter> il = letters.iterator();

		int w = 0;
		if (direction == true) {
			while (il.hasNext()) {
				Rect rect = new Rect();
				Letter nl = il.next();
				paint.getTextBounds(nl.text, 0, 1, rect);
				w += rect.width();
			}
			writeLine (dw, dh, dw/2 - w/2, y, paint, direction);
		}else{
			while (il.hasNext()) {
				Rect rect = new Rect();
				Letter nl = il.next();
				paint.getTextBounds(nl.text, 0, 1, rect);
				w += rect.width();
			}
			writeLine (dw, dh, dw/2 + w/2, y, paint, direction);

		}
	}

	public void drawLine(Canvas canvas, Paint paint, Bitmap map) {

		Iterator<Letter> il = letters.iterator();

		Paint redPaint = new Paint();
		redPaint.setColor(Color.RED);

		while (il.hasNext()) {
			Letter l = il.next();
			String s = String.valueOf(l.text);
			Rect r = new Rect();
			paint.getTextBounds(l.text, 0, 1, r);

			canvas.save();
			canvas.rotate(l.angle, l.x , l.y);
			canvas.drawText(s, l.x, l.y, paint);
			canvas.restore();
			//canvas.drawCircle(l.x, l.y, 2, redPaint);
			//canvas.drawCircle(l.x+r.width(), l.y-r.height(), 2,redPaint);

		}


	}

	public void updateAcc(float ax, float ay) {
		Iterator<Letter> il = letters.iterator();
		while (il.hasNext()) {
			Letter l = il.next();
			l.ax = ax;
			l.ay = ay;
		}

	}

	public boolean checkDropped (Bitmap map) {
		Iterator<Letter> il = letters.iterator();
		boolean dropped = true;
		while (il.hasNext()) {
			Letter l = il.next();
			if (l.y < (float)(map.getHeight() / 2 + 50)) {
			//if (l.y < (float)(map.getHeight() - bottom)) {
				dropped = false;
				break;
			}
		}
		return dropped;
	}

	public boolean checkFinal () {
		Iterator<Letter> il = letters.iterator();
		boolean f = true;
		while (il.hasNext()) {
			Letter l = il.next();
			if ((Math.round(l.y) != (int)l.finalY)||(Math.round(l.x)!=(int)l.finalX)) {
				f = false;
				break;
			}
		}
		return f;
	}

	
	public void copyPos (ArrayList<PoemLine> poem) {
		Iterator<Letter> il = letters.iterator();


	}

	public void updatePos(Paint paint, Bitmap map, float aX, float aY, float angle) {
		ArrayList<Letter> tempLetters= (ArrayList<Letter>) letters.clone();
		Iterator<Letter> il;
		Iterator<Letter> til = tempLetters.iterator();
		Letter maxLetter;
		float maxY = 0.0f;

		while (til.hasNext()) {
			maxLetter = null;
			maxY = 0f;
			il =  tempLetters.iterator();

			//start with lowest letter - might be useful when piling
			while (il.hasNext()) {
				Letter tempLetter = il.next();
				if (tempLetter.y >= maxY) {
					maxLetter = tempLetter;
					maxY = tempLetter.y;
				}
			}

			Rect r = new Rect();
			float tx, ty;
			paint.getTextBounds(maxLetter.text, 0, 1, r);

			maxLetter.ax = aX;
			maxLetter.ay = aY;

			maxLetter.vx = maxLetter.vx + maxLetter.ax;
			maxLetter.vy = maxLetter.vy + maxLetter.ay;			

			tx = maxLetter.x + maxLetter.vx;
			ty = maxLetter.y + maxLetter.vy;

			if (tx + r.width() > (float)map.getWidth()) {
				tx = (float)(map.getWidth() - r.width() - 1);
				maxLetter.vx = 0;
			} else if (tx < 0) {
				maxLetter.vx = 0;
				tx = 0;
			}
			//if (ty  >= canvasHeight) {
			//	ty = canvasHeight - 1f;
			if (ty  >= (float)(map.getHeight() - bottom)) {
				ty = (float)(map.getHeight() - bottom);
				maxLetter.vy = 0;
				maxLetter.vx = maxLetter.vx * maxLetter.friction;
			} else if (ty - (float)r.height() < 0) {
				ty = (float)r.height();
				maxLetter.vy = 0;
			}

			//store current x to calculate speed later on
			float oldTx = tx;

			// above the middle or under?

			if (ty < (float)(map.getHeight() /2)) {
				//above
				if (this.name.compareTo("poem-down")==0) {
					//poem-down

					//undo acceleration
					maxLetter.vx = maxLetter.vx - maxLetter.ax;
					maxLetter.vy = maxLetter.vy - maxLetter.ay;			

					//undo position
					tx = maxLetter.x;
					ty = maxLetter.y;


					if (tx < maxLetter.finalX)
						maxLetter.vx = (maxLetter.finalX - tx) / 10;
					else if (tx > maxLetter.finalX)
						maxLetter.vx = (maxLetter.finalX - tx) / 10;
					else
						tx = maxLetter.finalX;

					if (ty < maxLetter.finalY)
						maxLetter.vy = (maxLetter.finalY - ty) / 10;
					else if (ty > maxLetter.finalY)
						maxLetter.vy = (maxLetter.finalY - ty) / 10;
					else
						ty = maxLetter.finalY;

					//					maxLetter.ax *= 0.2f;
					//					maxLetter.ay *= 0.2f;

					//					maxLetter.vx = maxLetter.vx + maxLetter.ax;
					//					maxLetter.vy = maxLetter.vy + maxLetter.ay;			

					tx = maxLetter.x + maxLetter.vx;
					ty = maxLetter.y + maxLetter.vy;

				}


				//left or right of the map?

				try {
					if (tx + (float) r.width() / 2f < (float) map.getWidth() / 2f ) {

						if ((map.getPixel((int)tx, (int)ty)!=Color.WHITE)&&(tx > 4)) {
							while (map.getPixel((int)tx, (int)ty)!=Color.WHITE && tx < (float) map.getWidth() - 2f) {
								tx = tx + 1f;
							}
						}
					} else {

						if ((map.getPixel((int)tx+r.width(), (int)(ty))!=Color.WHITE)&&(tx > 4)) {
							maxLetter.vx =  - 1.0f;	
							while (map.getPixel((int)tx+r.width(), (int)ty)!=Color.WHITE && tx > 1f) {
								tx = tx - 1f;
							}
						}
					}
				} catch (IllegalArgumentException e) {


				}
				//check if out of screen
				if (tx<0) tx =0;
				if (tx >= map.getWidth()) tx = map.getWidth()-1;
				if (ty<0) ty =0;
				if (ty >= map.getHeight()) ty = map.getHeight()-1;

				maxLetter.vx = tx - oldTx;

			} else {
				//under
				//left or right of the map?
				if (tx + (float) r.width() / 2f < (float) map.getWidth() / 2f ) {
					//left
					if (map.getPixel((int)tx, (int)ty-r.height())!=Color.WHITE) {
						maxLetter.vx = 1.0f;
						while (map.getPixel((int)tx, (int)ty-r.height())!=Color.WHITE && tx < (float) map.getWidth() - 2f) {
							tx = tx + 1f;
						}
						maxLetter.vx = tx - oldTx;
					}
				} else {
					//right
					if (map.getPixel((int)tx+r.width(), (int)(ty)-r.height())!=Color.WHITE) {
						maxLetter.vx =  - 1.0f;	
						while (map.getPixel((int)tx-r.width(), (int)ty-r.height())!=Color.WHITE && tx > 1f) {
							tx = tx - 1f;
						}
						maxLetter.vx = tx - oldTx;
					}
				}

			}

			maxLetter.x = tx;
			maxLetter.y = ty;
			

			maxLetter.angle = angle;
			
			tempLetters.remove(maxLetter);
			til = tempLetters.iterator();


		}

	}

}


