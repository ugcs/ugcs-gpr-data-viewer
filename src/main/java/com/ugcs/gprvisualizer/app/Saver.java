package com.ugcs.gprvisualizer.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.github.thecoldwine.sigrun.common.ext.ByteBufferHolder;
import com.github.thecoldwine.sigrun.common.ext.ByteBufferProducer;
import com.github.thecoldwine.sigrun.common.ext.SgyFile;
import com.github.thecoldwine.sigrun.common.ext.Trace;
import com.ugcs.gprvisualizer.draw.ToolProducer;
import com.ugcs.gprvisualizer.gpr.Model;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Button;

public class Saver implements ToolProducer{

	private Button buttonSave = new Button("Save");
	private Button buttonSaveReload = new Button("Save&Reload");
	private Model model;
	
	{
		buttonSave.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	buttonSave.setDisable(true);
		    	
		    	Cursor cursor = buttonSave.getCursor();
		    	buttonSave.setCursor(Cursor.WAIT);
		    	List<File> newfiles = save();
		    	
		    	buttonSave.setDisable(false);
		    	buttonSave.setCursor(cursor);
		    }
		});
		
		buttonSaveReload.setOnAction(new EventHandler<ActionEvent>() {
		    @Override public void handle(ActionEvent e) {
		    	buttonSaveReload.setDisable(true);
		    	
		    	Cursor cursor = buttonSaveReload.getCursor();
		    	buttonSaveReload.setCursor(Cursor.WAIT);
		    	List<File> newfiles = save();
		    	AppContext.loader.load(newfiles);
		    	
		    	buttonSaveReload.setDisable(false);
		    	buttonSaveReload.setCursor(cursor);
		    }
		});
	}
	
	public Saver(Model model) {
		this.model = model;
	}
	
	@Override
	public List<Node> getToolNodes() {
		
		return Arrays.asList(buttonSave, buttonSaveReload);
	}

	private List<File> save() {
		List<File> newfiles = new ArrayList<>();
		
		
		File folder = createFolder();
		for(SgyFile file : model.getFileManager().getFiles()) {
			int part = 1;
			List<Trace> sublist = new ArrayList<>();
			for(Trace trace : file.getTraces()) {
				
				if(trace.isActive()) {
					sublist.add(trace);
				}else {
					if(!sublist.isEmpty()){					
						newfiles.add(savePart(file, part++, sublist, folder));
						sublist.clear();
					}		
				}
			}
			//for last
			if(!sublist.isEmpty()){					
				newfiles.add(savePart(file, part++, sublist, folder));
				sublist.clear();
			}		
		}
		
		return newfiles;
	}

	private File createFolder() {
		File someFile = model.getFileManager().getFiles().get(0).getFile();
		File nfolder;
		int cnt=0;
		do {
			cnt++;
			String name = String.format("processed_%03d", cnt);
			nfolder = new File(someFile.getParentFile(), name);
		}while(nfolder.exists());
		
		nfolder.mkdir();
		return nfolder;
	}


	private File savePart(SgyFile file, int part, List<Trace> sublist, File nfolder) {
		List<ByteBufferProducer> blocks = getBlocks(sublist); 
		File nfile = null;
		try {
			String name = file.getFile().getName();
			int pos = name.lastIndexOf(".");
			String onlyname = name.substring(0, pos);
			nfile = new File(nfolder, onlyname + "_" + part + name.substring(pos));
			
			
			file.savePart(nfile.getAbsolutePath(), blocks);
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		return nfile;
	}

	private List<ByteBufferProducer> getBlocks(List<Trace> sublist) {
		
		List<ByteBufferProducer> blocks = new ArrayList<>();
		for(Trace trace : sublist) {
			blocks.add(trace.getHeaderBlock());
			//blocks.add(trace.getDataBlock());
			blocks.add(new ByteBufferHolder(trace));
		}
		
		return blocks;
	}
	
	
	
}