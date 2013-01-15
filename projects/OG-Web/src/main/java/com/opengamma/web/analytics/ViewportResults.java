/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.analytics;

import java.util.Collection;
import java.util.List;

import javax.time.Duration;

import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.AggregatedExecutionLog;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.web.analytics.formatting.TypeFormatter;

/**
 * Set of calculation results for displaying in the viewport of a grid of analytics data.
 */
public class ViewportResults {

  /** The result values by row. */
  private final List<ResultsCell> _allResults;
  /** The grid columns. */
  private final AnalyticsColumnGroups _columns;
  /** Definition of the viewport. */
  private final ViewportDefinition _viewportDefinition;
  /** Duration of the last calculation cycle. */
  private final Duration _calculationDuration;
  private final Viewport.State _state;

  /**
   * @param allResults Cells in the viewport containing the data, history and the value specification. The outer
   * list contains the data by rows and the inner lists contain the data for each row
   * @param viewportDefinition Definition of the rows and columns in the viewport
   * @param columns The columns in the viewport's grid
   * @param state
   */
  /* package */ ViewportResults(List<ResultsCell> allResults,
                                ViewportDefinition viewportDefinition,
                                AnalyticsColumnGroups columns,
                                Duration calculationDuration,
                                Viewport.State state) {
    ArgumentChecker.notNull(allResults, "allResults");
    ArgumentChecker.notNull(columns, "columns");
    ArgumentChecker.notNull(viewportDefinition, "viewportDefinition");
    ArgumentChecker.notNull(calculationDuration, "calculationDuration");
    ArgumentChecker.notNull(state, "state");
    _allResults = allResults;
    _viewportDefinition = viewportDefinition;
    _columns = columns;
    _calculationDuration = calculationDuration;
    _state = state;
  }

  /**
   * @return Cells in the viewport containing the data, history and the value specification. The outer
   * list contains the data by rows and the inner lists contain the data for each row
   */
  /* package */ List<ResultsCell> getResults() {
    return _allResults;
  }

  /**
   *
   * @return Whether the data is a summary or the full data. Summary data fits in a single grid cell whereas
   * the full data might need more space. e.g. displaying matrix data in a window that pops up over the main grid.
   */
  /* package */ TypeFormatter.Format getFormat() {
    return _viewportDefinition.getFormat();
  }

  /**
   * @return The version of the viewport used when creating the results, allows the client to that a set of results
   * correspond to the current viewport state.
   */
  /* package */ long getVersion() {
    return _viewportDefinition.getVersion();
  }

  /**
   * @param colIndex The column index in the grid (zero based)
   * @return The type of the specified column
   */
  /* package */ Class<?> getColumnType(int colIndex) {
    return _columns.getColumn(colIndex).getType();
  }

  /**
   * @return The duration of the last calculation cycle.
   */
  /* package */ Duration getCalculationDuration() {
    return _calculationDuration;
  }

  /**
   * Factory method that creates a grid cell for displaying a string value.
   * @param value The cell's value
   * @return A cell for displaying the value
   */
  /* package */ static ResultsCell objectCell(Object value, int column) {
    ArgumentChecker.notNull(value, "value");
    return new ResultsCell(value, null, null, column, null, false);
  }

  /**
   * Factory method that creates a grid cell for displaying a calculated value.
   *
   * @param value The value
   * @param valueSpecification The value's specification
   * @param history The value's history
   * @param updated true if the value was updated in the last calculation cycle
   * @return A cell for displaying the value
   */
  /* package */ static ResultsCell valueCell(Object value,
                                             ValueSpecification valueSpecification,
                                             Collection<Object> history,
                                             AggregatedExecutionLog executionLog,
                                             int column,
                                             boolean updated) {
    return new ResultsCell(value, valueSpecification, history, column, executionLog, updated);
  }

  /**
   * Factory method that returns a grid cell with no value.
   * @return An empty cell
   * @param emptyHistory Empty history appropriate for the cell's type. For types that support history it should
   * be an empty collection, for types that don't it should be null.
   * @param colIndex Index of the cell's grid column
   */
  /* package */ static ResultsCell emptyCell(Collection<Object> emptyHistory, int colIndex) {
    return new ResultsCell(null, null, emptyHistory, colIndex, null, false);
  }

  /* package */ Viewport.State getState() {
    return _state;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ViewportResults that = (ViewportResults) o;

    if (!_columns.equals(that._columns)) {
      return false;
    }
    if (!_viewportDefinition.equals(that._viewportDefinition)) {
      return false;
    }
    if (!_allResults.equals(that._allResults)) {
      return false;
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = _allResults.hashCode();
    result = 31 * result + _columns.hashCode();
    result = 31 * result + _viewportDefinition.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "ViewportResults [" +
        "_allResults=" + _allResults +
        ", _columns=" + _columns +
        ", _viewportDefinition=" + _viewportDefinition +
        "]";
  }
}
