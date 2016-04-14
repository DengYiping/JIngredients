package sarf.jingredients.util;

public class Timer {
	
	private long t;
	
	public Timer() {
		t = System.currentTimeMillis();
	}
	
	public void printTime(String message) {
		System.out.println(message + (System.currentTimeMillis() - t));
		t = System.currentTimeMillis();
	}

}
