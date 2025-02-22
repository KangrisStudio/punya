// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.YaVersion;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

/**
 * Physical world component that can measure the light level.
 *
 * @internaldoc
 * It is implemented using
 * android.hardware.SensorListener
 * (http://developer.android.com/reference/android/hardware/SensorListener.html).
 */
@DesignerComponent(version = YaVersion.LIGHTSENSOR_COMPONENT_VERSION,
    description = "A sensor component that can measure the light level.",
    category = ComponentCategory.SENSORS,
    nonVisible = true,
    iconName = "images/lightsensor.png")
@SimpleObject
public class LightSensor extends BufferedSingleValueSensor {
  private static final int BUFFER_SIZE = 10;
  private volatile long timestamp;

  //default settings for schedule
  private final int SCHEDULE_INTERVAL = 1800; //read illuminances every 1800 seconds (30 minutes)
  private final int SCHEDULE_DURATION = 15; //scan for 15 seconds everytime

  /**
   * Creates a new LightSensor component.
   *
   * @param container  ignored (because this is a non-visible component)
   */
  public LightSensor(ComponentContainer container) {
    super(container.$form(), Sensor.TYPE_LIGHT, BUFFER_SIZE);
  }

  @Override
  protected void onValueChanged(float value) {
    timestamp = System.currentTimeMillis();
    LightChanged(value);
  }
  
  /**
   * Indicates the light level changed.
   *
   * @param lux the new light level in lux
   */
  @SimpleEvent(description = "Called when a change is detected in the light level.")
  public void LightChanged(float lux) {
    EventDispatcher.dispatchEvent(this, "LightChanged", lux);
  }

  /**
   * Returns the last measured brightness in lux.
   * The sensor must be enabled and available to return meaningful values.
   *
   * @return lux
   */
  @SimpleProperty(description = "The most recent light level, in lux, if the sensor is available " +
       "and enabled.")
   public float Lux() {
    return getValue();
  }

  /**
   * Returns the brightness in lux by averaging the previous 10 measured values.
   * The sensor must be enabled and available to return meaningful values.
   *
   * @return lux
   */
  @SimpleProperty(description = "The average of the 10 most recent light levels measured, in lux.")
   public float AverageLux() {
    return getAverageValue();
  }

  /**
   * Returns the timestamp of latest reading.
   */
  @SimpleProperty(description = "The timestamp of this sensor event.")
  public float Timestamp() {
    return timestamp;
  }
	
  /**
   * Returns the default interval between each scan for this probe.
   */
  @SimpleProperty(description = "The default interval (in seconds) between each scan for this probe")
  @Deprecated
  public float DefaultInterval() {
    return SCHEDULE_INTERVAL;
  }
	
  /**
   * Returns the default duration of each scan for this probe
   */
  @SimpleProperty(description = "The default duration (in seconds) of each scan for this probe")
  @Deprecated
  public float DefaultDuration() {
    return SCHEDULE_DURATION;
  }
}
