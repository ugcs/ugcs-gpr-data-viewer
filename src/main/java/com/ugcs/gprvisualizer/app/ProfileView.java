package com.ugcs.gprvisualizer.app;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.github.thecoldwine.sigrun.common.ext.ResourceImageHolder;
import com.github.thecoldwine.sigrun.common.ext.SgyFile;
import com.github.thecoldwine.sigrun.common.ext.Trace;
import com.github.thecoldwine.sigrun.common.ext.TraceSample;
import com.github.thecoldwine.sigrun.common.ext.VerticalCutPart;
import com.github.thecoldwine.sigrun.common.ext.ProfileField;
import com.ugcs.gprvisualizer.app.auxcontrol.BaseObject;
import com.ugcs.gprvisualizer.app.auxcontrol.ClickPlace;
import com.ugcs.gprvisualizer.app.auxcontrol.FoundPlace;
import com.ugcs.gprvisualizer.draw.Change;
import com.ugcs.gprvisualizer.draw.PrismDrawer;
import com.ugcs.gprvisualizer.draw.SmthChangeListener;
import com.ugcs.gprvisualizer.draw.WhatChanged;
import com.ugcs.gprvisualizer.gpr.Model;
import com.ugcs.gprvisualizer.gpr.RecalculationController;
import com.ugcs.gprvisualizer.gpr.RecalculationLevel;
import com.ugcs.gprvisualizer.gpr.Settings;
import com.ugcs.gprvisualizer.math.HyperFinder;
import com.ugcs.gprvisualizer.ui.BaseSlider;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Orientation;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseDragEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class ProfileView implements SmthChangeListener, ModeFactory {
	public static Stroke AMP_STROKE = new BasicStroke(1.0f);
	public static Stroke LEVEL_STROKE = new BasicStroke(2.0f);
	
	protected PrismDrawer prismDrawer;	
	protected Model model;
	protected ImageView imageView = new ImageView();
	protected VBox vbox = new VBox();
	protected Pane topPane = new Pane();
	
	protected BufferedImage img;
	protected Image i ;
	protected int width;
	protected int height;
	
	protected double contrast = 50;	
	
	private ContrastSlider contrastSlider;
	private HyperbolaSlider hyperbolaSlider;
	private HyperGoodSizeSlider hyperGoodSizeSlider;
	private MiddleAmplitudeSlider middleAmplitudeSlider;
	
	private ToggleButton auxModeBtn = new ToggleButton("aux");
	ToolBar toolBar = new ToolBar();
	private Button zoomInBtn = new Button("", ResourceImageHolder.getImageView("zoom-in_20.png" ));
	private Button zoomOutBtn = new Button("", ResourceImageHolder.getImageView("zoom-out_20.png"));
	private ToggleButton showGreenLineBtn = new ToggleButton("", ResourceImageHolder.getImageView("level.png"));
	
	private MouseHandler selectedMouseHandler;   
	private MouseHandler scrollHandler;
	private AuxElementEditHandler auxEditHandler;
	
	private HyperFinder hyperFinder; 
	public ProfileScroll profileScroll = new ProfileScroll();
	
	private ChangeListener<Number> sliderListener = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {			
			repaintEvent();
		}
	};
	private ChangeListener<Number> aspectSliderListener = new ChangeListener<Number>() {
		@Override
		public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
			//updateAspect();
			updateScroll();
			repaintEvent();
		}
	};
	
	public ProfileView(Model model) {
		this.model = model;
		
		zoomInBtn.setTooltip(new Tooltip("Zoom in flight profile"));
		zoomOutBtn.setTooltip(new Tooltip("Zoom out flight profile"));
		
		hyperFinder = new HyperFinder(model);
		prismDrawer = new PrismDrawer(model);
		contrastSlider = new ContrastSlider(model.getSettings(), sliderListener);
		hyperbolaSlider = new HyperbolaSlider(model.getSettings(), aspectSliderListener);
		hyperGoodSizeSlider = new HyperGoodSizeSlider(model.getSettings(), sliderListener);
		middleAmplitudeSlider = new MiddleAmplitudeSlider(model.getSettings(), sliderListener);
		initImageView();
		
		profileScroll.setChangeListener(new ChangeListener<Number>() {
            public void changed(ObservableValue<? extends Number> ov,
                    Number old_val, Number new_val) {
                repaintEvent();                
            }
        });

		scrollHandler = new CleverViewScrollHandler(this);
		auxEditHandler = new AuxElementEditHandler(this);
		
		prepareToolbar();
		
		profileScroll.recalc();
		vbox.getChildren().addAll(toolBar, profileScroll, imageView/*,  scrollBar*/);
		
		profileScroll.widthProperty().bind(
				topPane.widthProperty());
		//profileScroll.heightProperty().bind(
         //       stackPane.heightProperty());		
		
		
		
		zoomInBtn.setOnAction(e -> {
			zoom(1, width/2, height/2);

		});
		zoomOutBtn.setOnAction(e -> {
			zoom(-1, width/2, height/2);
		});
		
		
		showGreenLineBtn.setTooltip(new Tooltip("Show/hide anomaly probability chart"));
		showGreenLineBtn.setOnAction(e -> {
			model.getSettings().showGreenLine = showGreenLineBtn.isSelected();
			AppContext.notifyAll(new WhatChanged(Change.justdraw));
		});
		
		AppContext.smthListener.add(this);
	}

	public void prepareToolbar() {
		toolBar.setDisable(true);
		toolBar.getItems().addAll(auxEditHandler.getRightPanelTools());
		toolBar.getItems().add(getSpacer());
		
		toolBar.getItems().addAll(AppContext.navigator.getToolNodes());
		
		toolBar.getItems().add(getSpacer());
		toolBar.getItems().add(zoomInBtn);
		toolBar.getItems().add(zoomOutBtn);
		toolBar.getItems().add(getSpacer());
		
		
		toolBar.getItems().add(showGreenLineBtn);
		
		//toolBar.getItems().add(hyperLiveViewBtn);
	}
	
	protected BufferedImage draw(int width,	int height) {
		if(width <= 0 || height <= 0 || !model.isActive()) {
			return null;
		}		
		
		List<Trace> traces = model.getFileManager().getTraces();

		ProfileField field = new ProfileField(getField());
		
		BufferedImage bi ;
		if(img != null && img.getWidth() == width && img.getHeight() == height) {
			bi = img;
		}else {
			bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		}
		int[] buffer = ((DataBufferInt)bi.getRaster().getDataBuffer()).getData() ;
		
		Graphics2D g2 = (Graphics2D)bi.getGraphics();
		
		RenderingHints rh = new RenderingHints(
	             RenderingHints.KEY_TEXT_ANTIALIASING,
	             RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
	    g2.setRenderingHints(rh);		
		
	    
	    
	    
		clearBitmap(bi, g2, field);
		
		new VerticalRulerDrawer(model).draw(g2, field);
		
		g2.translate(field.getMainRect().x + field.getMainRect().width/2, 0);

		
		double contr = Math.pow(1.08, 140-contrast);

		prismDrawer.draw(width, field, g2, buffer, contr);
		
		int startTrace = field.getFirstVisibleTrace();
		int finishTrace = field.getLastVisibleTrace();		
		
		if(model.getFileManager().levelCalculated) {
			g2.setColor(new Color(210,105,30));
			g2.setStroke(LEVEL_STROKE);
			drawGroundLevel(field, g2, traces,  startTrace, finishTrace, false);
			
		}
		if(model.getSettings().showGreenLine) {
			
			g2.setColor(Color.GREEN);
			g2.setStroke(AMP_STROKE);
			drawGroundLevel(field, g2, traces,  startTrace, finishTrace, true);
			
		}
		
		drawFileNames(height, field, g2);

		drawAmplitudeMapLevels(field, g2);
		
		drawAuxElements(field, g2);
		
		if(model.getSettings().hyperliveview) {
			hyperFinder.drawHyperbolaLine(g2, field);
		}
		
		///
		return bi;
	}

	final static float dash1[] = {5.0f};
	final static BasicStroke dashed =
	        new BasicStroke(1.0f,
	                        BasicStroke.CAP_BUTT,
	                        BasicStroke.JOIN_MITER,
	                        10.0f, dash1, 0.0f);
	
	private void drawAmplitudeMapLevels(ProfileField field, Graphics2D g2) {
		if(model.getSettings().isRadarMapVisible) {
		
			g2.setColor(Color.MAGENTA);
			g2.setStroke(dashed);			
			
			int y = field.traceSampleToScreen(new TraceSample(0, model.getSettings().layer)).y;
			g2.drawLine(-width/2, y, width/2, y);

			int y2 = field.traceSampleToScreen(new TraceSample(0, model.getSettings().layer + model.getSettings().hpage)).y;
			g2.drawLine(-width/2, y2, width/2, y2);

		}		
	}
	
	private void drawAuxElements(ProfileField field, Graphics2D g2) {
		for(BaseObject bo : model.getAuxElements()) {
			bo.drawOnCut(g2, field);
		}
		if(model.getControls() != null) {
			for(BaseObject bo : model.getControls()) {
				bo.drawOnCut(g2, field);
			}
		}
	}

	private void clearBitmap(BufferedImage bi, Graphics2D g2, ProfileField field) {
		
		
		Rectangle mainRectRect = field.getMainRect();
		Rectangle topRuleRect = field.getTopRuleRect();
		Rectangle leftRuleRect = field.getLeftRuleRect();
		
		g2.setPaint ( Color.DARK_GRAY );
		g2.fillRect(mainRectRect.x, mainRectRect.y, mainRectRect.width, mainRectRect.height);
		
		g2.setPaint (new Color(45, 60, 100));
		g2.fillRect(topRuleRect.x, topRuleRect.y, topRuleRect.width, topRuleRect.height);
		g2.setPaint (Color.white);
		g2.drawLine(topRuleRect.x, topRuleRect.y+topRuleRect.height, topRuleRect.x + topRuleRect.width, topRuleRect.y+topRuleRect.height);
		
		g2.setPaint (new Color(45, 60, 100));
		g2.fillRect(leftRuleRect.x, leftRuleRect.y, leftRuleRect.width, leftRuleRect.height);
		g2.setPaint (Color.white);
		g2.drawLine(leftRuleRect.x+leftRuleRect.width, leftRuleRect.y, 
				leftRuleRect.x+leftRuleRect.width, leftRuleRect.y+leftRuleRect.height);
		
		
	}

	private void drawGroundLevel(ProfileField field, Graphics2D g2, List<Trace> traces, int startTrace, int finishTrace, boolean m2) {
		
		
		Trace trace1 = traces.get(startTrace);
		Point p1 = field.traceSampleToScreenCenter(new TraceSample(startTrace,  m2 ? trace1.maxindex2 : trace1.maxindex));
		int max2 = 0;
		
		for(int i=startTrace+1; i<finishTrace; i++) {
			//Trace trace1 = traces.get(i-1);
			
			Trace trace2 = traces.get(i);
			
			max2 = Math.max(max2, m2 ? trace2.maxindex2 : trace2.maxindex);
			
			Point p2 = field.traceSampleToScreenCenter(new TraceSample(i,  max2));
			if(p2.x - p1.x > 2) {
				g2.drawLine(p1.x, p1.y, p2.x, p2.y);
				trace1 = trace2;
				p1 = p2;
				
				max2 = 0;
			}
			
		}
	}

	private void drawFileNames(int height, ProfileField field, Graphics2D g2) {
		g2.setColor(Color.WHITE);
		for(SgyFile fl : model.getFileManager().getFiles()) {
			Point p = field.traceSampleToScreen(new TraceSample(fl.getTraces().get(0).indexInSet, 0));
			
			g2.drawLine(p.x, 0, p.x, height);
			g2.drawString(fl.getFile().getName(), p.x + 7, 11);
		}
	}

	@Override
	public void show() {
	
		updateScroll();
		repaintEvent();
	}

	@Override
	public Node getCenter() {
		
		
		ChangeListener<Number> sp2SizeListener = (observable, oldValue, newValue) -> {
			this.setSize((int) (topPane.getWidth()), (int) (topPane.getHeight()));
		};
		topPane.widthProperty().addListener(sp2SizeListener);
		topPane.heightProperty().addListener(sp2SizeListener);
		
		topPane.getChildren().add(vbox);
		
		//sp2.getChildren().add(toolBar);
		
		return topPane;
	}

	
	@Override
	public List<Node> getRight() {
		
		return Arrays.asList(
				//new HBox( zoomInBtn, zoomOutBtn),
				contrastSlider.produce()  
			);
	}

	public List<Node> getRightSearch() {
		
		return Arrays.asList(
				hyperbolaSlider.produce(),
				hyperGoodSizeSlider.produce(),
				middleAmplitudeSlider.produce()
			);
	}

	int z = 0;
	protected void initImageView() {
		imageView.setOnScroll(event -> {
	    	//model.getField().setZoom( .getZoom() + (event.getDeltaY() > 0 ? 1 : -1 ) );
			int ch = (event.getDeltaY() > 0 ? 1 : -1 );
			
			double ex = event.getSceneX();
			double ey = event.getSceneY();
			
			zoom(ch, ex, ey);
	    } );
		
		imageView.setOnMousePressed(mousePressHandler);
		imageView.setOnMouseReleased(mouseReleaseHandler);
		imageView.setOnMouseMoved(mouseMoveHandler);
		imageView.setOnMouseClicked(mouseClickHandler);
		imageView.addEventFilter(MouseEvent.DRAG_DETECTED, dragDetectedHandler);
		imageView.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseMoveHandler);
		imageView.addEventFilter(MouseDragEvent.MOUSE_DRAG_RELEASED, dragReleaseHandler);		
	}

	private void zoom(int ch, double ex, double ey) {
		Point t = getLocalCoords(ex, ey);
		
		TraceSample ts = getField().screenToTraceSample(t);
		
		z = z + ch;
		
		getField().setZoom(getField().getZoom()+ch);
		
		Point t2 = getLocalCoords(ex, ey);
		TraceSample ts2 = getField().screenToTraceSample(t2);
		
		getField().setSelectedTrace(getField().getSelectedTrace() - (ts2.getTrace() - ts.getTrace()));
		
		
		int starts = getField().getStartSample() - (ts2.getSample() - ts.getSample());
		getField().setStartSample(starts);
		
		
		updateScroll();
		repaintEvent();
	
	}

	protected EventHandler dragDetectedHandler = new EventHandler<MouseEvent>() {
	    @Override
	    public void handle(MouseEvent mouseEvent) {

	    	imageView.startFullDrag();
	    	
	    	imageView.setCursor(Cursor.CLOSED_HAND);
	    }
	};
	
	protected EventHandler dragReleaseHandler = new EventHandler<MouseDragEvent>() {
        @Override
        public void handle(MouseDragEvent event) {

        	Point p = getLocalCoords(event);
        	
        	if(selectedMouseHandler != null) {
        		selectedMouseHandler.mouseReleaseHandle(p, getField());
        		selectedMouseHandler = null;
        	}
        	
        	imageView.setCursor(Cursor.DEFAULT);
        	
        	event.consume();
        }
	};
	
	protected EventHandler<MouseEvent> mouseMoveHandler = new EventHandler<MouseEvent>() {
        
		@Override
        public void handle(MouseEvent event) {
			
        	Point p = getLocalCoords(event);
        	
        	if(model.getSettings().hyperliveview) {
        		TraceSample ts = getField().screenToTraceSample(p);
        		hyperFinder.setPoint(ts);        	
        		repaintEvent();
        	}else {
        		
        		if(selectedMouseHandler != null) {

        			selectedMouseHandler.mouseMoveHandle(p, getField());
        		}else{
	        		if(!auxEditHandler.mouseMoveHandle(p, getField())) {

	        		}
        		}
        	}
        }
	};
	
	private Point getLocalCoords(MouseEvent event) {
		
		return getLocalCoords(event.getSceneX(), event.getSceneY());
	
	}
	protected Point getLocalCoords(double x, double y) {
		javafx.geometry.Point2D sceneCoords  = new javafx.geometry.Point2D(x, y);
    	javafx.geometry.Point2D imgCoord = imageView.sceneToLocal(sceneCoords );        	
    	Point p = new Point(
    			(int)(imgCoord.getX() - getField().getMainRect().x - getField().getMainRect().width/2), 
    			(int)(imgCoord.getY() ));
		return p;
	}

	protected EventHandler<MouseEvent> mouseClickHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {

        	if(event.getClickCount() == 2) {
        		//add tmp flag
        		Point p = getLocalCoords(event);
        		
        		int trace = getField().screenToTraceSample(p).getTrace();
        		
        		if(trace>=0 && trace < model.getTracesCount()) {
        		
        			//	select in MapView
        			model.getField().setSceneCenter(model.getFileManager().getTraces().get(trace).getLatLon());
        		
        			createTempPlace(model, trace);
        			
        			AppContext.notifyAll(new WhatChanged(Change.mapscroll));
        		}
        	}
        }
     };

	public static void createTempPlace(Model model, int trace) {
		
		ClickPlace fp = new ClickPlace(trace);
		fp.setSelected(true);
		model.setControls(Arrays.asList(fp));
	}
     
	protected EventHandler<MouseEvent> mousePressHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {
        	
        	
        	Point p = getLocalCoords(event);
        	if(auxEditHandler.mousePressHandle(p, getField())) {
        		selectedMouseHandler = auxEditHandler; 
        	}else if(scrollHandler.mousePressHandle(p, getField())) {
        		selectedMouseHandler = scrollHandler;        		
        	}else {
        		selectedMouseHandler = null;
        	}
        	
        	imageView.setCursor(Cursor.CLOSED_HAND);
        }
	};

	protected EventHandler<MouseEvent> mouseReleaseHandler = new EventHandler<MouseEvent>() {
        @Override
        public void handle(MouseEvent event) {        	
        	
        	Point p = getLocalCoords(event);
        	
        	if(selectedMouseHandler != null) {
        		selectedMouseHandler.mouseReleaseHandle(p, getField());
        		
        		selectedMouseHandler = null;
        	}        	
        }
	};
	
	protected void repaintEvent() {
		if(!model.isLoading()) {
			controller.render(null);
		}
	}
	
	protected void repaint() {

		img = draw(width, height);
		if(img != null) {
			i = SwingFXUtils.toFXImage(img, null);
		}else {
			i = null;
		}
		updateWindow();
	}
	
	protected void updateWindow() {
		Platform.runLater(new Runnable() {
            @Override
            public void run() {
//            	if(i == null) {
//            		return;
//            	}
			    
			    imageView.setImage(i);
            }
          });
	}

	@Override
	public void somethingChanged(WhatChanged changed) {

		if(changed.isFileopened()) {
			profileScroll.setVisible(model.isActive());
			toolBar.setDisable(!model.isActive());
		}
		
		if(changed.isAuxOnMapSelected()) {

		}
		
		repaintEvent();
		updateScroll();
	}

	private void updateScroll() {
		if(!model.isActive()) {
			return;
		}
		
		profileScroll.recalc();
		
//		scrollBar.setMin(0);
//		scrollBar.setMax(model.getFileManager().getTraces().size());
//		
//		int am = getField().getVisibleNumberOfTrace(width);
//		scrollBar.setVisibleAmount(am);
//		scrollBar.setUnitIncrement(am/4);
//		scrollBar.setBlockIncrement(am);
//		scrollBar.setValue(getField().getSelectedTrace());
	}
	

	private RecalculationController controller = new RecalculationController(new Consumer<RecalculationLevel>() {

		@Override
		public void accept(RecalculationLevel level) {

			repaint();
			
		}
		
	});
	
	public class ContrastSlider extends BaseSlider {
		
		public ContrastSlider(Settings settings, ChangeListener<Number> listenerExt) {
			super(settings, listenerExt);
			name = "Contrast";
			units = "";
			tickUnits = 25;
		}

		public void updateUI() {
			slider.setMax(100);
			slider.setMin(0);
			slider.setValue(contrast);
		}
		
		public int updateModel() {
			contrast = (int)slider.getValue();
			return (int)contrast;
		}
	}

	public class AspectSlider extends BaseSlider {
		
		public AspectSlider(Settings settings, ChangeListener<Number> listenerExt) {
			super(settings, listenerExt);
			name = "Aspect";
			units = "";
			tickUnits = 10;
		}

		public void updateUI() {
			slider.setMax(30);
			slider.setMin(-30);
			slider.setValue(getField().getAspect());
		}
		
		public int updateModel() {
			getField().setAspect((int)slider.getValue());
			return (int)getField().getAspect();
		}
	}
	public class HyperbolaSlider extends BaseSlider {
		
		public HyperbolaSlider(Settings settings, ChangeListener<Number> listenerExt) {
			super(settings, listenerExt);
			name = "Hyperbola";
			units = "";
			tickUnits = 200;
		}

		public void updateUI() {
			slider.setMax(400);
			slider.setMin(2);
			slider.setValue(settings.hyperkfc);
		}
		
		public int updateModel() {
			settings.hyperkfc = (int)slider.getValue();
			return (int)settings.hyperkfc;
		}
	}

	public class HyperGoodSizeSlider extends BaseSlider {
		
		public HyperGoodSizeSlider(Settings settings, ChangeListener<Number> listenerExt) {
			super(settings, listenerExt);
			name = "Sensitivity";
			units = "";
			tickUnits = 10;
		}

		public void updateUI() {
			slider.setMax(100);
			slider.setMin(0);
			slider.setValue(settings.hyperSensitivity.intValue());
		}
		
		public int updateModel() {
			settings.hyperSensitivity.setValue((int)slider.getValue());
			return (int)settings.hyperSensitivity.intValue();
		}
	}

	public class MiddleAmplitudeSlider extends BaseSlider {
		
		public MiddleAmplitudeSlider(Settings settings, ChangeListener<Number> listenerExt) {
			super(settings, listenerExt);
			name = "Middle amp";
			units = "";
			tickUnits = 200;
		}

		public void updateUI() {
			slider.setMax(1000);
			slider.setMin(-1000);
			slider.setValue(settings.hypermiddleamp);
		}
		
		public int updateModel() {
			settings.hypermiddleamp = (int)slider.getValue();
			return (int)settings.hypermiddleamp;
		}
	}
	
	public void setSize(int width, int height) {
		
		this.width = width;
		this.height = height;
		getField().setViewDimension(new Dimension(this.width, this.height));
		
		
		repaintEvent();		
	}

	MouseHandler getMouseHandler() {
		if(auxModeBtn.isSelected()) {
			return auxEditHandler;
		}else {
			return scrollHandler;
		}
	}

	void setScrollHandler(MouseHandler scrollHandler) {
		this.scrollHandler = scrollHandler;
	}

	protected ProfileField getField() {
		return model.getVField();
	}

	private Region getSpacer() {
		Region r3 = new Region();
		r3.setPrefWidth(7);
		return r3;
	}

}
