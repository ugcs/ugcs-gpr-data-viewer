package com.ugcs.gprvisualizer.draw;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.thecoldwine.sigrun.common.ext.ResourceImageHolder;
import com.github.thecoldwine.sigrun.common.ext.SgyFile;
import com.github.thecoldwine.sigrun.common.ext.Trace;
import com.ugcs.gprvisualizer.gpr.Model;
import com.ugcs.gprvisualizer.gpr.Scan;
import com.ugcs.gprvisualizer.gpr.Settings;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;

public class GpsTrack extends BaseLayer{

	private RepaintListener listener;
	
	private EventHandler<ActionEvent> showMapListener = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent event) {
			setActive(showLayerCheckbox.isSelected());
			
			listener.repaint();				
		}
	};
	
	private ToggleButton showLayerCheckbox = new ToggleButton("", ResourceImageHolder.getImageView("path_20.png"));
	{
		showLayerCheckbox.setSelected(true);
		showLayerCheckbox.setOnAction(showMapListener);
	}
	
	
	
	public GpsTrack(Dimension parentDimension, Model model, RepaintListener listener) {
		
		super(parentDimension, model);
		
		this.listener = listener;		
	}
	
	@Override
	public void draw(Graphics2D g2) {
		if(model.getField().getSceneCenter() == null) {
			return;
		}
		
		drawGPSPath(g2);
	}

	private void drawGPSPath(Graphics2D g2) {
		if(!isActive()) {
			return;
		}
		
		g2.setStroke(new BasicStroke(1.0f));		
		
		
		double sumdist = 0;
		double threshold =
				model.getField().getSceneCenter().getDistance(
				model.getField().screenTolatLon(new Point2D.Double(0, 5)));// meter
		
		//
		g2.setColor(Color.RED);
		
		for(SgyFile sgyFile : model.getFileManager().getFiles()) {
			Point2D pPrev = null;
			List<Trace> traces = sgyFile.getTraces();
	
			for (int tr=0; tr< traces.size(); tr++) {
				Trace trace =  traces.get(tr);
				
				if(pPrev == null) {
					
					pPrev = model.getField().latLonToScreen(trace.getLatLon());
					sumdist = 0;
					
				}else{//prev point exists
					
						sumdist += trace.getPrevDist();
						
						if(sumdist >= threshold && !trace.isEnd()) {
							
							Point2D pNext = model.getField().latLonToScreen(trace.getLatLon());
									
							g2.drawLine((int)pPrev.getX(), (int)pPrev.getY(), (int)pNext.getX(), (int)pNext.getY());
							
							pPrev = pNext;
							sumdist = 0;
						}
				}
				
			}
		}
	}
	
	@Override
	public boolean isReady() {
		
		return true;
	}

	@Override
	public void somethingChanged(WhatChanged changed) {
		
		if(changed.isFileopened() || changed.isZoom() || changed.isAdjusting()) {
			
		}		
	}

	@Override
	public boolean mousePressed(Point2D point) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseRelease(Point2D point) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean mouseMove(Point2D point) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public List<Node> getToolNodes() {
		
		return Arrays.asList(showLayerCheckbox);
	}
	
}
