/**
 * Copyright (C) 2012 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.web.analytics;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.opengamma.engine.ComputationTarget;
import com.opengamma.engine.ComputationTargetResolver;
import com.opengamma.engine.ComputationTargetSpecification;
import com.opengamma.engine.target.ComputationTargetType;
import com.opengamma.engine.target.ComputationTargetTypeMap;
import com.opengamma.engine.value.ValueProperties;
import com.opengamma.engine.value.ValueSpecification;
import com.opengamma.engine.view.AggregatedExecutionLog;
import com.opengamma.id.UniqueId;
import com.opengamma.id.VersionCorrection;
import com.opengamma.util.ArgumentChecker;
import com.opengamma.util.tuple.Pair;
import com.opengamma.web.analytics.formatting.TypeFormatter;

/**
 * Row and column structure for a grid that displays the dependency graph used when calculating a value.
 * Each row contains one calcuated value from the results, all other columns in the row contain metadata about
 * the value.
 */
public class DependencyGraphGridStructure implements GridStructure {

  /** Index of the target column */
  private static final int TARGET_COL = 0;
  /** Index of the target type column */
  private static final int TARGET_TYPE_COL = 1;
  /** Index of the value name column */
  private static final int VALUE_NAME_COL = 2;
  /** Index of the value column */
  private static final int VALUE_COL = 3;
  /** Index of the function name column */
  private static final int FUNCTION_NAME_COL = 4;
  /** Index of the value properties column */
  private static final int PROPERTIES_COL = 5;
  /** Map of target types to displayable names. */
  private static final ComputationTargetTypeMap<String> TARGET_TYPE_NAMES = createTargetTypeNames();

  /** {@link ValueSpecification}s for all rows in the grid in row index order. */
  private final List<ValueSpecification> _valueSpecs;
  /** Function names for all rows in the grid in row index order. */
  private final List<String> _fnNames;
  /** For looking up calculation targets using their specification. */
  private final ComputationTargetResolver _computationTargetResolver;
  /** The root node of the tree structure representing the rows. */
  private final AnalyticsNode _root;
  /** The calculation configuration name. */
  private final String _calcConfigName;
  /** The columns in the grid. */
  private final GridColumnGroups _columnGroups;
  /** Row name of the root cell of the dependency graph in the parent grid. */
  private final String _rootRowName;
  /** Column name of the root cell of the dependency graph in the parent grid */
  private final String _rootColumnName;

  /* package */ DependencyGraphGridStructure(AnalyticsNode root,
                                             String calcConfigName,
                                             List<ValueSpecification> valueSpecs,
                                             List<String> fnNames,
                                             ComputationTargetResolver targetResolver,
                                             String rootRowName,
                                             String rootColumnName) {
    ArgumentChecker.notNull(valueSpecs, "valueSpecs");
    ArgumentChecker.notNull(fnNames, "fnNames");
    ArgumentChecker.notNull(targetResolver, "targetResolver");
    ArgumentChecker.notNull(rootRowName, "rootRowName");
    ArgumentChecker.notNull(rootColumnName, "rootColumnName");
    _rootColumnName = rootColumnName;
    _rootRowName = rootRowName;
    _root = root;
    _calcConfigName = calcConfigName;
    _valueSpecs = Collections.unmodifiableList(valueSpecs);
    _fnNames = Collections.unmodifiableList(fnNames);
    _computationTargetResolver = targetResolver;
    _columnGroups = new GridColumnGroups(ImmutableList.of(
        // fixed column group with one column for the row label
        new GridColumnGroup("", ImmutableList.<GridColumn>of(
            column("Target", 0)), false),
        // non-fixed columns
        new GridColumnGroup("", ImmutableList.<GridColumn>of(
            column("Type", 1),
            column("Value Name", 2),
            column("Value", null, 3),
            column("Function", 4),
            column("Properties", ValueProperties.class, 5)),
        false)));
  }

  /**
   * Returns the value specifications used to calculate the values in the grid.
   * @return The value specifications used to calculate the values
   */
  /* package */ List<ValueSpecification> getValueSpecifications() {
    return _valueSpecs;
  }

  /**
   * Builds the results for a viewport.
   *
   * @param viewportDefinition Defines the viewport
   * @param cache Cache of results for the grid
   * @param previousResults The results before the latest calculation cycle, possibly null
   * @return The results for the cells in the viewport and the new viewport state
   */
  /* package */ Pair<ViewportResults, Viewport.State> createResults(ViewportDefinition viewportDefinition,
                                                                    ResultsCache cache,
                                                                    ViewportResults previousResults) {
    List<ResultsCell> results = Lists.newArrayList();
    for (GridCell cell : viewportDefinition) {
      GridColumn column = _columnGroups.getColumn(cell.getColumn());
      results.add(column.buildResults(cell.getRow(), cell.getFormat(), cache));
    }
    ViewportResults newResults = new ViewportResults(results,
                                                     viewportDefinition,
                                                     _columnGroups,
                                                     cache.getLastCalculationDuration(), cache.getValuationTime());
    Viewport.State state;
    if (previousResults != null && results.equals(previousResults.getResults())) {
      state = Viewport.State.STALE_DATA;
    } else {
      state = Viewport.State.FRESH_DATA;
    }
    return Pair.of(newResults, state);
  }

  /**
   *
   * @param header The column header string
   * @param colIndex The column index
   * @return A column for displaying a string value
   */
  private GridColumn column(String header, int colIndex) {
    return column(header, String.class, colIndex);
  }

  /**
   *
   * @param header The column header string
   * @param type The type of value the column contains
   * @param colIndex The column index
   * @return A column for displaying values of the specified type
   */
  private GridColumn column(String header, Class<?> type, int colIndex) {
    DependencyGraphCellRenderer renderer = new DependencyGraphCellRenderer(colIndex,
                                                                           _valueSpecs,
                                                                           _fnNames,
                                                                           _computationTargetResolver,
                                                                           _calcConfigName);
    return new GridColumn(header, header, type, renderer);
  }

  @Override
  public int getRowCount() {
    return _valueSpecs.size();
  }

  @Override
  public int getColumnCount() {
    return _columnGroups.getColumnCount();
  }

  @Override
  public GridColumnGroups getColumnStructure() {
    return _columnGroups;
  }

  @Override
  public Pair<String, ValueSpecification> getTargetForCell(int row, int col) {
    if (_calcConfigName == null || col != VALUE_COL) {
      return null;
    }
    ValueSpecification valueSpec = _valueSpecs.get(row);
    return valueSpec != null ? Pair.of(_calcConfigName, valueSpec) : null;
  }

  /**
   * @return The root of the node structure representing the dependency graph, possibly null
   */
  public AnalyticsNode getRootNode() {
    return _root;
  }

  /**
   * @return Row name of the root of the dependency graph in the parent grid.
   */
  public String getRootRowName() {
    return _rootRowName;
  }

  /**
   * @return Column name of the root of the dependency graph in the parent grid
   */
  public String getRootColumnName() {
    return _rootColumnName;
  }

  private static ComputationTargetTypeMap<String> createTargetTypeNames() {
    ComputationTargetTypeMap<String> map = new ComputationTargetTypeMap<>();
    map.put(ComputationTargetType.PORTFOLIO_NODE, "Agg");
    map.put(ComputationTargetType.POSITION, "Pos");
    map.put(ComputationTargetType.SECURITY, "Sec");
    map.put(ComputationTargetType.ANYTHING, "Prim");
    map.put(ComputationTargetType.NULL, "Prim");
    map.put(ComputationTargetType.TRADE, "Trade");
    return map;
  }

  /**
   * Renderer for cells in the dependency graph grid.
   */
  private static final class DependencyGraphCellRenderer implements GridColumn.CellRenderer {

    /** Index of the renderer's column. */
    private final int _colIndex;
    /** {@link ValueSpecification}s for each row in the grid. */
    private final List<ValueSpecification> _valueSpecs;
    /** Names of the functions used to calculate each row in the grid. */
    private final List<String> _fnNames;
    /** For looking up calculation targets using their specification. */
    private final ComputationTargetResolver _computationTargetResolver;
    /** The calculation configuration name. */
    private final String _calcConfigName;


    private DependencyGraphCellRenderer(int colIndex,
                                        List<ValueSpecification> valueSpecs,
                                        List<String> fnNames,
                                        ComputationTargetResolver computationTargetResolver,
                                        String calcConfigName) {
      ArgumentChecker.notNull(valueSpecs, "valueSpecs");
      ArgumentChecker.notNull(fnNames, "fnNames");
      ArgumentChecker.notNull(computationTargetResolver, "computationTargetResolver");
      ArgumentChecker.notNull(calcConfigName, "calcConfigName");
      _calcConfigName = calcConfigName;
      _computationTargetResolver = computationTargetResolver;
      _colIndex = colIndex;
      _valueSpecs = valueSpecs;
      _fnNames = fnNames;
    }

    @Override
    public ResultsCell getResults(int rowIndex,
                                  TypeFormatter.Format format,
                                  ResultsCache cache,
                                  Class<?> columnType,
                                  Object inlineKey) {
      ValueSpecification valueSpec = _valueSpecs.get(rowIndex);
      switch (_colIndex) {
        case TARGET_COL:
          return ResultsCell.forStaticValue(getTargetName(valueSpec.getTargetSpecification()), columnType, format);
        case TARGET_TYPE_COL:
          return ResultsCell.forStaticValue(TARGET_TYPE_NAMES.get(valueSpec.getTargetSpecification().getType()), columnType, format);
        case VALUE_NAME_COL:
          return ResultsCell.forStaticValue(valueSpec.getValueName(), columnType, format);
        case VALUE_COL:
          ResultsCache.Result cacheResult = cache.getResult(_calcConfigName, valueSpec, null);
          Collection<Object> history = cacheResult.getHistory();
          Object value = cacheResult.getValue();
          AggregatedExecutionLog executionLog = cacheResult.getAggregatedExecutionLog();
          return ResultsCell.forCalculatedValue(value, valueSpec, history, executionLog, cacheResult.isUpdated(), columnType, format);
        case FUNCTION_NAME_COL:
          String fnName = _fnNames.get(rowIndex);
          return ResultsCell.forStaticValue(fnName, columnType, format);
        case PROPERTIES_COL:
          return ResultsCell.forStaticValue(valueSpec.getProperties(), columnType, format);
        default: // never happen
          throw new IllegalArgumentException("Column index " + _colIndex + " is invalid");
      }
    }

    /**
     * @param targetSpec Specification of the target for a grid row
     * @return The name of the target
     */
    private String getTargetName(final ComputationTargetSpecification targetSpec) {
      // TODO I don't think LATEST will do long term. resolution time available on the result model
      ComputationTarget target = _computationTargetResolver.resolve(targetSpec, VersionCorrection.LATEST);
      if (target != null) {
        return target.getName();
      } else {
        UniqueId uid = targetSpec.getUniqueId();
        if (uid != null) {
          return uid.toString();
        } else {
          return targetSpec.getType().toString();
        }
      }
    }
  }
}
