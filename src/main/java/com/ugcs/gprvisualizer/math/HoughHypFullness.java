package com.ugcs.gprvisualizer.math;

public class HoughHypFullness {

	private int horizontalSize;
	private int searchIndex;
	private int searchEdge;
	
	int[] pointCount;
	
	public HoughHypFullness(int horizontalSize, int searchIndex, int searchEdge) {
		this.horizontalSize = horizontalSize;
		this.searchIndex = searchIndex;
		this.searchEdge = searchEdge;
		
		pointCount = new int[horizontalSize * 2];
	}
	
	public void add(int tr, int xfd1, int xfd2, int edge) {
		boolean inside = 
				xfd1 <= searchIndex && xfd2 >= searchIndex 
				|| xfd2 <= searchIndex && xfd1 >= searchIndex;
				
		if (edge == searchEdge && inside) {
			
			
			int index = horizontalSize + tr;
			if (index < 0 || index >= pointCount.length) {
				return;
			}
			
			pointCount[index]++;
		}
	}
	
	/**
	 * Max zero group in % of all length.
	 * @return
	 */
	public double getMaxGap() {
		
		int maxZeroGroup = getMaxZeroGroup();
		
		return (double) maxZeroGroup / (double) horizontalSize;
	}

	/**
	 * except start and finish gaps.
	 * @return
	 */
	private int getMaxZeroGroup() {
		int result = 0;
		int group = 0;
		boolean started = true;
		
		for (int point : pointCount) {
			if (point == 0) {
				if (started) {
					group++;
				}
			} else {
				started = true;
				if (group > result) {
					result = group;
					group = 0;
				}				
			}			
		}
		if (group > result) {
			result = group;
		}				
		
		return result;
	}
	
}