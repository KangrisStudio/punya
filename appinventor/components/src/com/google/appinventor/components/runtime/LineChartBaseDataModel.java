// -*- mode: java; c-basic-offset: 2; -*-
// Copyright 2019-2020 MIT, All rights reserved
// Released under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package com.google.appinventor.components.runtime;

import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.EntryXComparator;
import com.google.appinventor.components.common.ComponentConstants;
import com.google.appinventor.components.runtime.util.YailList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Handles the data operations & model-specific styling for line-based
 * Chart data (i.e. Line and Area charts) for the Chart component.
 *
 * @param <V> Line Chart Base View type parameter. To be specialized by
 *            extending subclasses.
 * @see com.google.appinventor.components.runtime.ChartDataModel
 */
public abstract class LineChartBaseDataModel<V extends LineChartViewBase>
    extends PointChartDataModel<LineDataSet, LineData, V> {
  /**
   * Initializes a new LineChartBaseDataModel object instance.
   *
   * @param data Line Chart Data object instance
   * @param view Chart View to link model to
   */
  protected LineChartBaseDataModel(LineData data, V view) {
    super(data, view);
    dataset = new LineDataSet(new ArrayList<Entry>(), "");
    this.data.addDataSet(dataset); // Safe add
    setDefaultStylingProperties();
  }

  @Override
  public void addEntryFromTuple(YailList tuple) {
    Entry entry = getEntryFromTuple(tuple);

    if (entry != null) {
            /* TODO: The commented out line should be used, however, it breaks in certain cases.
               When this is fixed in MPAndroidChart, this method should use the commented method instead
               of the current implementation.
               See: https://github.com/PhilJay/MPAndroidChart/issues/4616
            */
      // getDataset().addEntryOrdered(entry);


      // In Line Chart based data series, the data is already pre-sorted.
      // We can thus run binary search by comparing with the x value, and
      // using an x+1 value to find the insertion point
      int index = Collections.binarySearch(entries, // Use the list of entries
          entry, // Search for the same x value as the entry to be added
          new EntryXComparator()); // Compare by x value

      // Value not found: insertion point can be derived from it
      if (index < 0) {
        // result is (-(insertion point) - 1)
        index = -index - 1;
      } else {
        // Get the entry count of the Data Set
        int entryCount = entries.size();

        // Iterate until an entry with a differing (higher) x value is found (this
        // is where the value should be inserted)
        // The reason for a loop is to pass through all the duplicate entries.
        while (index < entryCount && entries.get(index).getX() == entry.getX()) {
          index++;
        }
      }

      entries.add(index, entry);
    }
  }

  @Override
  public void setColor(int argb) {
    super.setColor(argb);
    getDataset().setCircleColor(argb); // Also update the circle color
  }

  @Override
  public void setColors(List<Integer> colors) {
    super.setColors(colors);
    getDataset().setCircleColors(colors); // Also update the circle colors
  }

  @Override
  protected void setDefaultStylingProperties() {
    getDataset().setDrawCircleHole(false); // Draw full circle instead of a hollow one
  }

  /**
   * Changes the Line Type of the Line Chart Data Series.
   *
   * @param type one of {@link ComponentConstants#CHART_LINE_TYPE_LINEAR},
   *             {@link ComponentConstants#CHART_LINE_TYPE_CURVED} or
   *             {@link ComponentConstants#CHART_LINE_TYPE_STEPPED}
   */
  public void setLineType(int type) {
    switch (type) {
      case ComponentConstants.CHART_LINE_TYPE_LINEAR:
        dataset.setMode(LineDataSet.Mode.LINEAR);
        break;

      case ComponentConstants.CHART_LINE_TYPE_CURVED:
        dataset.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        break;

      case ComponentConstants.CHART_LINE_TYPE_STEPPED:
        dataset.setMode(LineDataSet.Mode.STEPPED);
        break;
    }
  }
}
