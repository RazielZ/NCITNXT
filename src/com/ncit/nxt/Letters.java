package com.ncit.nxt;

public class Letters {
	
	NXTTalker mNXTTalker;
	byte drawPower[];
	
	public Letters (NXTTalker mNXTTalker) {
		this.mNXTTalker = mNXTTalker;
		drawPower = new byte[3];
	}
	
	public void stopAllMotors () {
		
		mNXTTalker.motor(NXTTalker.MOTOR1, (byte) 0, true, false);
		mNXTTalker.motor(NXTTalker.MOTOR2, (byte) 0, true, false);
		mNXTTalker.motor(NXTTalker.MOTOR3, (byte) 0, true, false);
	}
	
	public void pennUp () {
		mNXTTalker.motor(NXTTalker.MOTOR2, (byte) 30, true, false);
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		stopAllMotors();
	}
	
	public void drawOne () {
		Thread draw = new Thread (new Runnable() {

			@Override
			public void run() {
				try {
					//Up
					activateAllMotors (-4, -8, 0);
					Thread.sleep(600);
					
					//Left-Down (Diagonal)
					activateAllMotors (8, 5, -10);
					Thread.sleep(300);
					stopAllMotors();
					Thread.sleep(400);
					
					//Arm Up
					activateAllMotors(-20, 30, 0);
					Thread.sleep(400);
					stopAllMotors();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void activateAllMotors (int speedMotor0, int speedMotor1, int speedMotor2) {
		
		drawPower[0] = (byte) speedMotor0;
		drawPower[1] = (byte) speedMotor1;
		drawPower[2] = (byte) speedMotor2;
		
		mNXTTalker.motor(NXTTalker.MOTOR1, drawPower[0], true, false);
		mNXTTalker.motor(NXTTalker.MOTOR2, drawPower[1], true, false);
		mNXTTalker.motor(NXTTalker.MOTOR3, drawPower[2], true, false);
	}
	
	public void drawTwo () {

		Thread draw = new Thread (new Runnable() {
			
			@Override
			public void run() {

				try {
					//Left
					activateAllMotors (0, 0, -7);
					Thread.sleep(450);
					stopAllMotors();
					
					Thread.sleep(200);
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Right
					activateAllMotors (2, 0, 8);
					Thread.sleep(400);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, -2);
					Thread.sleep(450);
					stopAllMotors();
					
					//Left
					activateAllMotors (-2, 0, -7);
					Thread.sleep(450);
					stopAllMotors();
					
					Thread.sleep(300);
					pennUp();
					
					
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
	}
	
	public void drawSeven () {
		Thread draw = new Thread (new Runnable() {

			@Override
			public void run() {
				try {
					//Up
					activateAllMotors (-4, -8, 0);
					Thread.sleep(600);
					
					//Left
					activateAllMotors(-2, 0, -5);
					Thread.sleep(500);
					stopAllMotors();
					
					//Down
					activateAllMotors(4, 1, 0);
					Thread.sleep(300);
					stopAllMotors();
					Thread.sleep(300);
					
					pennUp ();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void drawThree () {
		Thread draw = new Thread (new Runnable() {

			@Override
			public void run() {
				try {
					//Right
					activateAllMotors (0, 0, 8);
					Thread.sleep(400);
					stopAllMotors();
					Thread.sleep(300);
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Left
					activateAllMotors (0, 0, -8);
					Thread.sleep(450);
					stopAllMotors();
					Thread.sleep(500);
					
					//Right
					activateAllMotors (0, 0, 8);
					Thread.sleep(400);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();

					//Left
					activateAllMotors (0, 0, -7);
					Thread.sleep(500);
					stopAllMotors();

					pennUp();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void drawFour () {
		Thread draw = new Thread (new Runnable() {

			@Override
			public void run() {
				try {
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(350);
					stopAllMotors();
					
					//Down
					activateAllMotors (5, 12, 0);
					Thread.sleep(500);
					stopAllMotors();
					
					//Left
					activateAllMotors (0, 0, -8);
					Thread.sleep(400);
					stopAllMotors();
					Thread.sleep(500);
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();

					pennUp();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void drawFive () {
		
		Thread draw = new Thread (new Runnable() {
			
			@Override
			public void run() {
				try {
					//Right
					activateAllMotors (0, 0, 8);
					Thread.sleep(400);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();
					
					//Left
					activateAllMotors (0, 3, -8);
					Thread.sleep(400);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Right
					activateAllMotors (0, 4, 7);
					Thread.sleep(500);
					stopAllMotors();
					Thread.sleep(400);
					
					pennUp();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void drawSix () {
		Thread draw = new Thread (new Runnable() {
			
			@Override
			public void run() {
				try {
					
					//Right
					activateAllMotors (0, 0, 7);
					Thread.sleep(400);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();
					
					//Left
					activateAllMotors (0, 3, -8);
					Thread.sleep(450);
					stopAllMotors();
					
					//Down
					activateAllMotors (5, 10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(450);
					stopAllMotors();
					
					//Right
					activateAllMotors (0, 4, 7);
					Thread.sleep(500);
					stopAllMotors();
					Thread.sleep(400);
					
					pennUp();
					
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void drawEight() {
		Thread draw = new Thread (new Runnable() {
			
			@Override
			public void run() {
				try {
					
					//Right
					activateAllMotors (0, 0, 8);
					Thread.sleep(390);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Left
					activateAllMotors (0, 3, -8);
					Thread.sleep(450);
					stopAllMotors();

					//Down
					activateAllMotors (5, 10, 0);
					Thread.sleep(500);
					stopAllMotors();

					//Down
					activateAllMotors (5, 10, 0);
					Thread.sleep(500);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -15, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Right
					activateAllMotors (0, 0, 10);
					Thread.sleep(400);
					stopAllMotors();



					Thread.sleep(400);
					pennUp();




				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void drawNine () {

		Thread draw = new Thread (new Runnable() {

			@Override
			public void run() {
				try {
					//Right
					activateAllMotors (0, 0, 8);
					Thread.sleep(390);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Left
					activateAllMotors (0, 3, -8);
					Thread.sleep(450);
					stopAllMotors();

					//Down
					activateAllMotors (5, 10, 0);
					Thread.sleep(500);
					stopAllMotors();

					//Right
					activateAllMotors (0, 4, 7);
					Thread.sleep(500);
					stopAllMotors();

					Thread.sleep(400);
					pennUp();

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	public void drawZero() {
		Thread draw = new Thread (new Runnable() {

			@Override
			public void run() {
				try {
					//Right
					activateAllMotors (0, 0, 8);
					Thread.sleep(390);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Up
					activateAllMotors (-5, -10, 0);
					Thread.sleep(400);
					stopAllMotors();

					//Left
					activateAllMotors (0, 3, -8);
					Thread.sleep(450);
					stopAllMotors();

					//Down
					activateAllMotors (5, 10, 0);
					Thread.sleep(500);
					stopAllMotors();

					//Down
					activateAllMotors (5, 10, 0);
					Thread.sleep(500);
					stopAllMotors();

					Thread.sleep(400);
					pennUp();

				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		draw.start();
		try {
			draw.join();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
}
