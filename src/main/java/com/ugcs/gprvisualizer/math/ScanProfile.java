package com.ugcs.gprvisualizer.math;

import java.util.Arrays;

public class ScanProfile {

	
	public double [] intensity;
	public int[] radius = null;
	public double maxVal;
	
	public ScanProfile(int size) {
		this(size, false);		
	}
	
	public ScanProfile(int size, boolean initRadius) {
		
		intensity = new double[size];
		
		if (initRadius) {
			radius = new int[size];
		}
		
	}

	public void finish() {
		maxVal = Arrays.stream(intensity).max().getAsDouble();		
	}
	
}
