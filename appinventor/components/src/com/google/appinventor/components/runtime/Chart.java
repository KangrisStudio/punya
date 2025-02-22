// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import android.app.Activity;
import android.view.View;
import android.widget.RelativeLayout;
import com.google.appinventor.components.annotations.*;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.common.YaVersion;
import com.google.appinventor.components.runtime.util.ElementsUtil;
import com.google.appinventor.components.runtime.util.OnInitializeListener;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * The Chart component plots data originating from it's attached Data components. Five different
 * Chart types are available, including Line, Area, Scatter, Bar and Pie, which can be changed by
 * the {@link #Type(int)} property.
 * The Chart component itself has various other properties that change the appearance
 * of the Chart, such as {{@link #Description(String)}, {@link #GridEnabled(boolean)},
 * @link #Labels(YailList)} and {@link #LegendEnabled(boolean)}.
 *
 * @see com.google.appinventor.components.runtime.ChartDataModel
 * @see com.google.appinventor.components.runtime.ChartView
 */
@SimpleObject
@DesignerComponent(version = YaVersion.CHART_COMPONENT_VERSION,
    category = ComponentCategory.CHARTS,
    description = "A component that allows visualizing data")
@UsesLibraries(libraries = "mpandroidchart.jar")
public class Chart extends AndroidViewComponent implements ComponentContainer<ChartDataBase>, OnInitializeListener {
  // Root layout of the Chart view. This is used to make Chart
  // dynamic removal & adding easier.
  private RelativeLayout view;

  // Underlying Chart view
  private ChartView chartView;

  // Properties
  private int type;
  private int backgroundColor;
  private String description;
  private int pieRadius;
  private boolean legendEnabled;
  private boolean gridEnabled;
  private YailList labels;

  // Synced t value across all Data Series (used for real-time entries)
  // Start the value from 1 (in contrast to starting from 0 as in Chart
  // Data Base) to lessen off-by-one offsets for multiple Chart Data Series.
  private int t = 1;

  // Attached Data components
  private ArrayList<ChartDataBase> dataComponents;

  /**
   * Creates a new Chart component.
   *
   * @param container container, component will be placed in
   */
  public Chart(ComponentContainer container) {
    super(container);

    view = new RelativeLayout(container.$context());

    // Adds the view to the designated container
    container.$add(this);

    dataComponents = new ArrayList<ChartDataBase>();

    // Set default values
    Type(ComponentConstants.CHART_TYPE_LINE);
    Width(ComponentConstants.VIDEOPLAYER_PREFERRED_WIDTH);
    Height(ComponentConstants.VIDEOPLAYER_PREFERRED_HEIGHT);
    BackgroundColor(Component.COLOR_DEFAULT);
    Description("");
    PieRadius(100);
    LegendEnabled(true);
    GridEnabled(true);
    Labels(new YailList());

    // Register onInitialize event of the Chart
    $form().registerForOnInitialize(this);
  }

  @Override
  public View getView() {
    return view;
  }

  @Override
  public Activity $context() {
    return container.$context();
  }

  @Override
  public Form $form() {
    return container.$form();
  }

  @Override
  public void $add(AndroidViewComponent component) {
    throw new UnsupportedOperationException("ChartBase.$add() called");
  }

  @Override
  public void setChildWidth(AndroidViewComponent component, int width) {
    throw new UnsupportedOperationException("ChartBase.setChildWidth called");
  }

  @Override
  public void setChildHeight(AndroidViewComponent component, int height) {
    throw new UnsupportedOperationException("ChartBase.setChildHeight called");
  }

  /**
   * Returns the type of the Chart.
   *
   * @return one of {@link ComponentConstants#CHART_TYPE_LINE},
   * {@link ComponentConstants#CHART_TYPE_SCATTER},
   * {@link ComponentConstants#CHART_TYPE_AREA},
   * {@link ComponentConstants#CHART_TYPE_BAR} or
   * {@link ComponentConstants#CHART_TYPE_PIE}
   */
  @SimpleProperty(
      category = PropertyCategory.BEHAVIOR,
      userVisible = false)
  public int Type() {
    return type;
  }

  /**
   * Specifies the type of the Chart, which determines how to visualize the data.
   *
   * @param type one of {@link ComponentConstants#CHART_TYPE_LINE},
   *             {@link ComponentConstants#CHART_TYPE_SCATTER},
   *             {@link ComponentConstants#CHART_TYPE_AREA},
   *             {@link ComponentConstants#CHART_TYPE_BAR} or
   *             {@link ComponentConstants#CHART_TYPE_PIE}
   * @throws IllegalArgumentException if shape is not a legal value.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHART_TYPE,
      defaultValue = ComponentConstants.CHART_TYPE_LINE + "")
  @SimpleProperty(description = "Specifies the chart's type (area, bar, " +
      "pie, scatter), which determines how to visualize the data.",
      userVisible = false)
  public void Type(int type) {
    if (type >= 0 && type < ComponentConstants.CHART_TYPES) {
      this.type = type;

      // Keep track whether a ChartView already exists,
      // in which case it will have to be reinitialized.
      boolean chartViewExists = (chartView != null);

      // ChartView currently exists in root layout. Remove it.
      if (chartViewExists) {
        view.removeView(chartView.getView());
      }

      // Create a new Chart view based on the supplied type
      chartView = createChartViewFromType(type);

      // Add the new Chart view as the first child of the root RelativeLayout
      view.addView(chartView.getView(), 0);

      // If a ChartView already existed, then the Chart
      // has to be reinitialized.
      if (chartViewExists) {
        reinitializeChart();
      }
    }
  }

  /**
   * Creates and returns a ChartView object based on the type
   * (integer) provided.
   *
   * @param type one of {@link ComponentConstants#CHART_TYPE_LINE},
   *             {@link ComponentConstants#CHART_TYPE_SCATTER},
   *             {@link ComponentConstants#CHART_TYPE_AREA},
   *             {@link ComponentConstants#CHART_TYPE_BAR} or
   *             {@link ComponentConstants#CHART_TYPE_PIE}
   * @return new ChartView instance
   */
  private ChartView createChartViewFromType(int type) {
    switch (type) {
      case ComponentConstants.CHART_TYPE_LINE:
        return new LineChartView(this);
      case ComponentConstants.CHART_TYPE_SCATTER:
        return new ScatterChartView(this);
      case ComponentConstants.CHART_TYPE_AREA:
        return new AreaChartView(this);
      case ComponentConstants.CHART_TYPE_BAR:
        return new BarChartView(this);
      case ComponentConstants.CHART_TYPE_PIE:
        return new PieChartView(this);
      default:
        // Invalid argument
        throw new IllegalArgumentException("Invalid Chart type specified: " + type);
    }
  }

  /**
   * Reinitializes the Chart view by reattaching all the Data
   * components and setting back all the properties.
   */
  private void reinitializeChart() {
    // Iterate through all attached Data Components and reinitialize them.
    // This is needed since the Type property is registered only after all
    // the Data components are attached to the Chart.
    // This has no effect when the Type property is default (0), since
    // the Data components are not attached yet, making the List empty.
    for (ChartDataBase dataComponent : dataComponents) {
      dataComponent.initChartData();
    }

    Description(description);
    BackgroundColor(backgroundColor);
    LegendEnabled(legendEnabled);
    GridEnabled(gridEnabled);
    Labels(labels);
  }

  /**
   * Returns the description label text of the Chart.
   *
   * @return description label
   */
  @SimpleProperty(
      category = PropertyCategory.APPEARANCE)
  public String Description() {
    return description;
  }

  /**
   * Specifies the text displayed by the description label inside the Chart.
   * Specifying an empty string ("") will not display any label.
   *
   * @param text description
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty
  public void Description(String text) {
    this.description = text;
    chartView.setDescription(description);
  }

  /**
   * Returns the chart's background color as an alpha-red-green-blue
   * integer.
   *
   * @return background RGB color with alpha
   */
  @SimpleProperty(
      category = PropertyCategory.APPEARANCE)
  public int BackgroundColor() {
    return backgroundColor;
  }

  /**
   * Specifies the chart's background color as an alpha-red-green-blue
   * integer.
   *
   * @param argb background RGB color with alpha
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_COLOR,
      defaultValue = Component.DEFAULT_VALUE_COLOR_NONE)
  @SimpleProperty
  public void BackgroundColor(int argb) {
    backgroundColor = argb;
    chartView.setBackgroundColor(argb);
  }

  /**
   * Sets the Pie Radius of the Chart. If the current type is
   * not the Pie Chart, the value has no effect.
   * <p>
   * The value is hidden in the blocks due to it being applicable
   * to a single Chart only. TODO: Might be better to change this in the future
   * <p>
   * TODO: Make this an enum selection in the future? (Donut, Full Pie, Small Donut, etc.)
   *
   * @param percent Percentage of the Pie Chart radius to fill
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_CHART_PIE_RADIUS,
      defaultValue = "100")
  @SimpleProperty(description = "Sets the Pie Radius of a Pie Chart from 0% to 100%, where the percentage " +
      "indicates the percentage of the hole fill. 100% means that a full Pie Chart " +
      "is drawn, while values closer to 0% correspond to hollow Pie Charts.", userVisible = false,
      category = PropertyCategory.APPEARANCE)
  public void PieRadius(int percent) {
    this.pieRadius = percent;

    // Only set the value if the Chart View is a
    // Pie Chart View; Otherwise take no action.
    if (chartView instanceof PieChartView) {
      ((PieChartView) chartView).setPieRadius(percent);
    }
  }

  /**
   * Returns a boolean indicating whether the legend is enabled
   * on the Chart.
   *
   * @return True if legend is enabled, false otherwise
   */
  @SimpleProperty
  public boolean LegendEnabled() {
    return this.legendEnabled;
  }

  /**
   * Changes the visibility of the Chart's Legend.
   *
   * @param enabled indicates whether the Chart should be enabled.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "True")
  @SimpleProperty(category = PropertyCategory.APPEARANCE)
  public void LegendEnabled(boolean enabled) {
    this.legendEnabled = enabled;
    chartView.setLegendEnabled(enabled);
  }

  /**
   * Returns a boolean indicating whether the grid is enabled
   * on the Chart.
   *
   * @return True if grid is enabled, false otherwise
   */
  @SimpleProperty
  public boolean GridEnabled() {
    return this.gridEnabled;
  }

  /**
   * Changes the visibility of the Chart's grid, if the
   * Chart Type is set to a Chart with an Axis (applies for Area, Bar, Line,
   * Scatter Chart types)
   *
   * @param enabled indicates whether the Chart's grid should be enabled.
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_BOOLEAN,
      defaultValue = "True")
  @SimpleProperty(category = PropertyCategory.APPEARANCE)
  public void GridEnabled(boolean enabled) {
    this.gridEnabled = enabled;

    // Only change grid visibility if the Chart View is an
    // Axis Chart View, since non-axis Charts do not have
    // grids.
    if (chartView instanceof AxisChartView) {
      ((AxisChartView) chartView).setGridEnabled(enabled);
    }
  }

  /**
   * Returns a List of Labels set to the X Axis.
   *
   * @return List of Labels used for the X Axis
   */
  @SimpleProperty
  public YailList Labels() {
    return labels;
  }

  /**
   * Changes the Chart's X axis labels to the specified List,
   * if the Chart's Type is set to a Chart with an Axis.
   * <p>
   * The first entry of the List corresponds to the minimum x value of the data,
   * the second to the min x value + 1, and so on.
   * <p>
   * If a label is not specified for an x value, a default value
   * is used (the x value of the axis tick at that location)
   *
   * @param labels List of labels to set to the X Axis of the Chart
   */
  @SimpleProperty(description = "Changes the Chart's X axis labels to the specified List of Strings, " +
      " provided that the Chart Type is set to a Chart with an Axis (applies to Area, Bar, Line, Scatter Charts)." +
      "The labels are applied in order, starting from the smallest x value on the Chart, and continuing in order." +
      "If a label is not specified for an x value, a default value is used (the x value of the axis tick " +
      "at that location)")
  public void Labels(YailList labels) {
    this.labels = labels;

    // Only change the labels if the Chart View is
    // an Axis Chart View, since Charts without an
    // axis will not have an X Axis.
    if (chartView instanceof AxisChartView) {
      List<String> stringLabels = new ArrayList<String>();

      for (int i = 0; i < labels.size(); ++i) {
        String label = labels.getString(i);
        stringLabels.add(label);
      }

      ((AxisChartView) chartView).setLabels(stringLabels);
    }
  }


  /**
   * Specifies the labels to set to the Chart's X Axis, provided the current
   * view is a Chart with an X Axis. The labels are specified as a single comma-separated
   * values String (meaning each value is separated by a comma). See {@link #Labels(YailList)}
   * for more details on how the Labels are applied to the Chart.
   *
   * @param labels Comma-separated values, where each value represents a label (in order)
   * @see #Labels(YailList)
   */
  @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING,
      defaultValue = "")
  @SimpleProperty(userVisible = false, category = PropertyCategory.APPEARANCE)
  public void LabelsFromString(String labels) {
    // Retrieve the elements from the CSV-formatted String
    YailList labelsList = ElementsUtil.elementsFromString(labels);
    Labels(labelsList); // Set the Labels from the retrieved elements List
  }

  /**
   * Creates a new instance of a ChartDataModel, corresponding
   * to the current Chart type.
   *
   * @return new ChartDataModel object instance
   */
  public ChartDataModel createChartModel() {
    return chartView.createChartModel();
  }

  /**
   * Refreshes the Chart View.
   */
  public void refresh() {
    chartView.Refresh();
  }

  /**
   * Returns the underlying Chart View object.
   *
   * @return Chart View object
   */
  public ChartView getChartView() {
    return chartView;
  }

  /**
   * Attach a Data Component to the Chart.
   *
   * @param dataComponent Data component object instance to add
   */
  public void addDataComponent(ChartDataBase dataComponent) {
    dataComponents.add(dataComponent);
  }

  @Override
  public void onInitialize() {
    // If the Chart View is of type PieChartView, the
    // radius of the Chart has to be set after initialization
    // due to the method relying on retrieving width and height
    // via getWidth() and getHeight(), which only return non-zero
    // values after the Screen is initialized.
    if (chartView instanceof PieChartView) {
      ((PieChartView) chartView).setPieRadius(pieRadius);
      chartView.Refresh();
    }
  }

  /**
   * Returns the t value to use for time entries for a
   * Data Series of this Chart component.
   * <p>
   * Takes in the t value of a Data Series as an argument
   * to determine a value tailored to the Data Series, while
   * updating the synced t value of the Chart component.
   * <p>
   * This method primarily takes care of syncing t values
   * across all the Data Series of the Chart for consistency.
   *
   * @param dataSeriesT t value of a Data Series
   * @return t value to use for the next time entry based on the specified parameter
   */
  public int getSyncedTValue(int dataSeriesT) {
    int returnValue;

    // If the difference between the global t and the Data Series' t
    // value is more than one, that means the Data Series' t value
    // is out of sync and must be updated.
    if (t - dataSeriesT > 1) {
      returnValue = t;
    } else {
      returnValue = dataSeriesT;
    }

    // Since the returnValue is either bigger or equal to t,
    // the new synchronized t value should be 1 higher than
    // the return value (since immediately after getting the
    // t value, the value will be incremented either way)
    t = returnValue + 1;

    // Return the calculated t value
    return returnValue;
  }

  @Override
  public Iterator<ChartDataBase> iterator() {
    return dataComponents.iterator();
  }
}
