package com.brunovianna.ampulheta;

public class Letter {
	public float x, y, vx, vy, ax, ay, angle, friction, finalX, finalY;
	public String text;
	
	public Letter (String itext) {
		
		x = finalX = 0f;
		y = finalY = 0f;
		
		text = itext;
		
		vx = 0f;
		vy = 0f;
		
		ax = 0f;
		ay = 0f;
		
		angle = 0f;
		friction = 0f;
		
	}
	
}
