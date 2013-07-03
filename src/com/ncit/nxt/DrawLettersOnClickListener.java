package com.ncit.nxt;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

public class DrawLettersOnClickListener implements OnClickListener {
	
	Letters letters;
	private DrawModeCallBackInterface drawModeCall;
	
	public DrawLettersOnClickListener (Letters letters, DrawModeCallBackInterface toCall) {
		this.letters = letters;
		drawModeCall= toCall;
	}
	
	@Override
	public void onClick(View v) {

		Thread control = new Thread(new Runnable() {
			int sleepTime = 500;
			int drawMode = drawModeCall.getDrawMode();
			@Override
			public void run() {
				try {
					switch (drawMode) {

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
				} catch (Exception e) {
					e.printStackTrace();
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
}
