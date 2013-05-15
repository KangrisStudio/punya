package com.google.appinventor.components.runtime;

import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Environment; 
import android.os.IBinder;
import android.util.Log;
 

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ErrorMessages;
import com.google.appinventor.components.runtime.util.SensorDbUtil;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gwt.dev.util.collect.HashMap;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule;
import edu.mit.media.funf.pipeline.Pipeline;
import edu.mit.media.funf.storage.DatabaseService;
import edu.mit.media.funf.storage.NameValueDatabaseService;

/* 
 * This class provide interface for App inventor program to configure an SensorDBPipeline that will execute schedule tasks.
 * The schedule tasks include sensing , archive, export db, clear backup.
 * 
 * This component can be killed by Android but the scheduled task will continue in the background. This introduce a new practice, 
 * in App Inventor's app that is to query for current states when the app is created. The component provides interface to query
 * what are the current sensor collection tasks and current pipeline tasks 
 * 
 * Also each component (App) will have and control exactly one SensorDBPipeline, however, it's true that two App Inventor apps
 * are using same FunfManager
 * 
 *  
 * 

 */
@DesignerComponent(version = YaVersion.WIFISENSOR_COMPONENT_VERSION, description = "Non-visible component that provides method      s to backup and upload "
		+ "sensor db on the phone.", category = ComponentCategory.BASIC, nonVisible = true, iconName = "images/tinyDB.png")
@SimpleObject
@UsesPermissions(permissionNames = "android.permission.WRITE_EXTERNAL_STORAGE, "
		+ "android.permission.ACCESS_NETWORK_STATE, "
		+ "android.permission.WAKE_LOCK, "
		+ "android.permission.READ_LOGS, "
		+ "android.permission.INTERNET")
@UsesLibraries(libraries = "funf.jar")
public class SensorDB extends AndroidNonvisibleComponent implements
OnDestroyListener{
	/*
	 * Binding to FunfMananger service
	 */

  protected FunfManager mBoundFunfManager = null;
  protected boolean mIsBound = false;
  private SensorDBPipeline mPipeline = null;
  private JsonParser parser;
  private Gson gson;

	private final String TAG = "SensorDB";
//	public static final String PIPE_NAME = "SensorDB"; // this is used as the
	
	private final String pipelineName; 
 
	protected static Activity mainUIThreadActivity;
	private String exportPath;
	protected static final String ACTION_ARCHIVE_DATA = "ARCHIVE_DATA";
  protected static final String ACTION_EXPORT_DATA = "EXPORT_DATA";
  protected static final String ACTION_CLEAR_BACKUP= "CLEAR_BACKUP";
  private static final long ARCHIVE_PERIOD = 3600; // archive database 60*60*24 (archive every 24 hour)
  private static final long EXPORT_PERIOD = 7200;//
//  public static final String DATABASE_NAME = "SensorData";
  
  private Map<String, String> sensorMapping = SensorDbUtil.sensorMap;

  private boolean scheduleArchiveEnabled;
  private boolean scheduleExportEnabled;
  private long archive_period;
  private long export_period;
  
  /*
   *TODO: consider using reflection? 
   * a list of current possible funf sensors for data collection
   * unfortunately, we need to update this list if there are new sensors been added
   * edu.mit.media.funf.probe.builtin.ActivityProbe (ActivitySensor)
   * edu.mit.media.funf.probe.builtin.BatteryProbe (BatterySensor)
   * edu.mit.media.funf.probe.builtin.CallLogProbe (CallLogHistory)
   * edu.mit.media.funf.probe.builtin.CellTowerProbe (CellTowerProbeSensor)
   * edu.mit.media.funf.probe.builtin.LightSensorProbe (LightSensor)
   * edu.mit.media.funf.probe.builtin.SimpleLocationProbe (LocationProbeSensor)
   * edu.mit.media.funf.probe.builtin.PedometerProbe (PedometerSensor)
   * edu.mit.media.funf.probe.builtin.ProximitySensorProbe (ProximitySensor)
   * edu.mit.media.funf.probe.builtin.ScreenProbe (ScreenStatus)
   * edu.mit.media.funf.probe.builtin.SmsProbe (SmsHistory)
   * edu.mit.media.funf.probe.builtin.BluetoothProbe (SocialProximitySensor.java)
   * edu.mit.media.funf.probe.builtin.WifiProbe (WifiSensor)
   * 
   * 
  */
  //Mapping between Funf class and App Inventor sensor name
  //TODO: move to util class?



  // Note that if use both archive and export, the period of export needs to be shorter than archive, because when 
  // archive is called, it will delete the original sqlite db file. 
  // ** Archive should be used together with the upload db function provided by other uploading component, such as 
  //    Dropbox.UploadDB(); 
  // ** Export should be used together with upload folder function provided by other uploading component, such as in 
  //    Dropbox.Upload();
  // We will provide archive folder path after archive
  
  

	private String exportFormat;
  

	public SensorDB(ComponentContainer container) {
		super(container.$form());
		// TODO Auto-generated constructor stub
		// Set up listeners
		form.registerForOnDestroy(this);
		mainUIThreadActivity = container.$context();
		
    //set upload and archive periods
    archive_period = ARCHIVE_PERIOD;
    export_period = EXPORT_PERIOD;
    scheduleArchiveEnabled = false;
    scheduleExportEnabled = false;
    //we use dbName as pipelineName, each app (package) has their unique dbName, which is packagename + "__SENSOR_DB__"
    pipelineName = SensorDbUtil.getPipelineName(mainUIThreadActivity);
    
    exportPath =  new File(Environment.getExternalStorageDirectory(), form.getPackageName()) + 
    		File.separator + "export";
    exportFormat = NameValueDatabaseService.EXPORT_CSV; // set the exporting format as csv by default
   
    Intent i = new Intent(mainUIThreadActivity, FunfManager.class);
    mainUIThreadActivity.startService(i);

    // bind to FunfManger (in case the user wants to set up the schedule)
    doBindService();
    
	}
	
	
	
	
	/*
	 * create a pipleline by giving a json configuration to FunfManger, 
	 * and get back the handle
	 */
  private Pipeline getOrCreatePipeline() {
    // TODO Auto-generated method stub
    // try to get the pipeline, if not create a pipeline configuration here
    //<string name="mainPipelineConfig">{"@type":"edu.mit.dig.funftest.MainPipeline"}</string>
    // try 
    
    Pipeline pipeline = mBoundFunfManager.getRegisteredPipeline(pipelineName);
    
    if(pipeline == null){
      String pipeLineStr = "{\"@type\":\"com.google.appinventor.components.runtime.SensorDBPipeline\"}";
      JsonElement pipelineConfig = parser.parse(pipeLineStr);
      Pipeline newPipeline = gson.fromJson(pipelineConfig, Pipeline.class);
      
      // add to funfManager   

      mBoundFunfManager.registerPipeline(pipelineName, newPipeline);
      return mBoundFunfManager.getRegisteredPipeline(pipelineName);
      
    }
 
    return pipeline;
    
  }
  
  
  @SimpleFunction(description = "Return available names of the avaiable sesnors for data collection")
  public YailList getAvailableSensors(){
    return null;
  
    
    
  }
  
  @SimpleFunction(description = "")
  public void AddSensorCollection(String sensorName, Integer period) {
    // add to the list

    if (mPipeline != null) {
      // mapp the sensor to some probe's className
      if (sensorMapping.containsKey(sensorName)) {
        mPipeline.addSensorCollection(sensorName, period);

      } else {
        // TODO: throw an exception, saying the sensor does not exist, please
        // check the sensorMap
        form.dispatchErrorOccurredEvent(SensorDB.this, "AddSensorCollection",
            ErrorMessages.ERROR_SENSORDB_NOTAVAILABLE, sensorName);
      }
    } else {
      Log.v(TAG, "AddSensorCollection, should not be here...");
      //this should not happen..because we already bind to Funf 
    }

  }

  @SimpleFunction(description ="")
  public void UpdateSensorCollection(String sensorName, Integer period){
    //remove if existed, and add a new one with different configuration
    
    if(mPipeline != null){
      RemoveSensorCollection(sensorName);
      AddSensorCollection(sensorName, period);
    } else {
      Log.v(TAG, "UpdateSensorCollection, should not be here...");
      //this should not happen..because we already bind to Funf 
    }
    
  }
  
  @SimpleFunction(description = "")
  public void RemoveSensorCollection(String sensorName) {
    if (mPipeline != null) {
      if(!sensorMapping.containsKey(sensorName)){
        form.dispatchErrorOccurredEvent(SensorDB.this, "AddSensorCollection",
            ErrorMessages.ERROR_SENSORDB_NOTAVAILABLE, sensorName);
      }
      
      if (mPipeline.getActiveSensor().containsKey(sensorName)) {
        mPipeline.removeSensorCollection(sensorName);
      } else {
        // TODO: throw an exception saying the sensor is not active
        form.dispatchErrorOccurredEvent(SensorDB.this, "AddSensorCollection",
            ErrorMessages.ERROR_SENSORDB_NOTACTIVE, sensorName);
      }
    } else{
      Log.v(TAG, "RemoveSensorCollection, should not be here...");
      //this should not happen..because we already bind to Funf 
    }

  }
  
  /**
   * Returns the active sensors.
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR,
      description = "The active sensor collections, as a list of two-element sublists. The first element " +
      "of each sublist represents the sensor name. The second element of each " +
      "sublist represents the schedule period of that sensor")
  public YailList CurrentActiveSensors(){
    YailList list = new YailList();
    for (Entry<String, Integer> entry: mPipeline.getActiveSensor().entrySet()){
      YailList entryList = new YailList();
      entryList.add(entry.getKey());
      entryList.add(entry.getValue());
      list.add(entryList);
    }
    return list; 
  }
 
  
  /*
   * Return available sensors
   */
  @SimpleProperty(category = PropertyCategory.BEHAVIOR, 
      description = "Returning available sensor names for sensor data collection, " +
      		"as a list of string for all sensor names")
  public YailList AvailableActiveSensors(){
    return YailList.makeList(SensorDbUtil.sensorMap.keySet());
    
  }


  /**
   * Export the Sensor Database (SensorData as the name for the sqlite db on Android) as
   * csv file(s) or JSON file(s). Each type of sensor data in the database 
   * will be export it as one file. 
   * The export path is under SDcard/packageName/export/
   */
  @SimpleFunction(description = "Export Sensor Database as CSV files or JSON files. " +
      "Input \"csv\" or \"json\" for exporting format") 
  public void Export(String format){
    Log.i(TAG, "Exporting DB as CSV files");
    this.exportFormat = format;

    
  }

  
  
  @SimpleFunction(description = "Stop the schedule service for Archiving db")
  
	
	 @SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public String FolderPath(){
		return this.exportPath;
		
		
	}
	 
	@SimpleProperty(category = PropertyCategory.BEHAVIOR)
	public String DBName(){
		return SensorDbUtil.DB_NAME; 
		
	}
	
	
	@SimpleFunction(description = "Enable archive schedule task with specified period")
	public void SetScheduleArchive(int period){
 
		this.scheduleArchiveEnabled = true;
		this.archive_period = period;

      // register pipeline action with bound FunfManger
    	
      Schedule archivePeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.archive_period), BigDecimal.ZERO, false, false);
      //because we have the Pipeline handle, so we pass it to the FunfManager
    	mBoundFunfManager.registerPipelineAction(mPipeline, ACTION_ARCHIVE_DATA, archivePeriod);

	}
	
	@SimpleFunction(description = "Discable archive scheduled task")
	public void StopScheduleArchive(){
		
		this.scheduleArchiveEnabled = false;
		mBoundFunfManager.unregisterPipelineAction(mPipeline, ACTION_ARCHIVE_DATA);
		
	}
	
	@SimpleProperty(description = "Indicates whether the schedule archive task is current" +
			"enabled.", category = PropertyCategory.BEHAVIOR)
	public boolean ScheduleArchiveEnabled(){
		return this.scheduleArchiveEnabled; 
		
	}
	
	// schedule export
	
	@SimpleFunction(description = "Enable export db schedule task with specified period")
	public void ScheduleExport(long period){
		this.scheduleExportEnabled = true;
		this.export_period = period;
 

      // register pipeline action with bound FunfManger
    	
      Schedule exportPeriod = new Schedule.BasicSchedule(
          BigDecimal.valueOf(this.export_period), BigDecimal.ZERO, false, false);

    	mBoundFunfManager.registerPipelineAction(mPipeline, ACTION_EXPORT_DATA, exportPeriod);
	}
	
	@SimpleFunction(description = "Discable export scheduled task")
	public void StopExportArchive(){
		
		this.scheduleExportEnabled = false;
		mBoundFunfManager.unregisterPipelineAction(mPipeline, ACTION_EXPORT_DATA);
		
	}
	
	@SimpleProperty(description = "Indicates whether the scheduled export task is current" +
			"enabled.", category = PropertyCategory.BEHAVIOR)
	public boolean ScheduleExportEnabled(){
		return this.scheduleExportEnabled; 
		
	}
	

	// try local binding to FunfManager
	private ServiceConnection mConnection = new ServiceConnection() {
		public void onServiceConnected(ComponentName className, IBinder service) {
			mBoundFunfManager = ((FunfManager.LocalBinder) service)
					.getManager();

//			registerSelfToFunfManager();
			//once we bind to the existing FunfManager service, we will createPipeline
			mPipeline = (SensorDBPipeline)getOrCreatePipeline();
		  mIsBound = true;
			Log.i(TAG, "Bound to FunfManager");

		}

		public void onServiceDisconnected(ComponentName className) {
			mBoundFunfManager = null;

			Log.i(TAG, "Unbind FunfManager");

		}
	};
	

  

	

	void doBindService() {
		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		mainUIThreadActivity.bindService(new Intent(mainUIThreadActivity,
				FunfManager.class), mConnection, Context.BIND_AUTO_CREATE);

		Log.i(TAG,
				"FunfManager is bound, and now we could have register dataRequests");

	}

	void doUnbindService() {
		if (mIsBound) {
 
			mainUIThreadActivity.unbindService(mConnection);
			mIsBound = false;
		}
	}




	@Override
	public void onDestroy() {
		// TODO Auto-generated method stub
    doUnbindService();
	}
	
	
	
	

}
