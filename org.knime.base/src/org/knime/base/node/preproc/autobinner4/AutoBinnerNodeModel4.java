/*
 * ------------------------------------------------------------------------
 *
 *  Copyright by KNIME AG, Zurich, Switzerland
 *  Website: http://www.knime.com; Email: contact@knime.com
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME AG herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * ---------------------------------------------------------------------
 *
 * History
 *   Apr 16, 2025 (david): created
 */
package org.knime.base.node.preproc.autobinner4;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.BinNames;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.BinningType;
import org.knime.base.node.preproc.autobinner4.AutoBinnerNodeSettings.MatchType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.sort.BufferedDataTableSorter;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.util.binning.numeric.Bin;
import org.knime.core.util.binning.numeric.NumericBin;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 *
 *         For David's future
 */
@SuppressWarnings("restriction")
final class AutoBinnerNodeModel4 extends WebUINodeModel<AutoBinnerNodeSettings> {

    AutoBinnerNodeModel4(final WebUINodeConfiguration configuration) {
        super(configuration, AutoBinnerNodeSettings.class);
    }

    static void assertSorted(final double[] values) {
        for (int i = 1; i < values.length; ++i) {
            if (values[i] < values[i - 1]) {
                throw new IllegalArgumentException("Values must be sorted in ascending order.");
            }
        }
    }

    @Override
    protected void validateSettings(final AutoBinnerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_binningType == BinningType.CUSTOM_CUTOFFS) {
            if (settings.m_customCutoffs.length < 2) {
                throw new InvalidSettingsException("At least two custom cutoffs are required.");
            }

            assertSorted(Arrays.stream(settings.m_customCutoffs) //
                .mapToDouble(value -> value.m_cutoff) //
                .toArray());
        } else if (settings.m_binningType == BinningType.CUSTOM_QUANTILES) {
            if (settings.m_customQuantiles.length < 2) {
                throw new InvalidSettingsException("At least two custom quantiles are required.");
            }

            assertSorted(Arrays.stream(settings.m_customQuantiles) //
                .mapToDouble(value -> value.m_quantile) //
                .toArray());
        }
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final AutoBinnerNodeSettings modelSettings)
        throws InvalidSettingsException {

        return new PortObjectSpec[0];
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final AutoBinnerNodeSettings modelSettings) throws Exception {

        var sortedTable = BufferedDataTableSorter.sortTable( //
            (BufferedDataTable)inObjects[0], //
            0, // TODO
            exec //
        );

        return new PortObject[0];
    }

    // TODO: right now this probably doesn't work very nicely with missing values
    ColumnRearranger createColumnRearranger(final BufferedDataTable table, final AutoBinnerNodeSettings settings,
        final ExecutionContext exec) throws CanceledExecutionException {
        var selectedColumns = settings.m_selectedColumns.filterFromFullSpec(table.getDataTableSpec());
        var rearranger = new ColumnRearranger(table.getDataTableSpec());

        for (var column : selectedColumns) {
            var columnIndex = table.getDataTableSpec().findColumnIndex(column);
            var sortedTable = BufferedDataTableSorter.sortTable( //
                table, //
                columnIndex, //
                exec //
            );
            List<Double> values = new ArrayList<>(Math.toIntExact(sortedTable.size()));
            try (var iterator = sortedTable.iterator()) {
                while (iterator.hasNext()) {
                    var row = iterator.next();
                    var cell = row.getCell(columnIndex);

                    if (cell.isMissing()) {
                        continue;
                    }

                    values.add(((DoubleValue)cell).getDoubleValue());
                }
            }

            var bins = createBins( //
                values, //
                settings //
            );

            // having created the bins, we need to insert the bin names into the unsorted table. I really hope that sortTable has sorted
            // the index columns so we can do this, otherwise we have a problem
        }

        throw new UnsupportedOperationException("Not yet implemented: createColumnRearranger");
    }

    private static Bin[] createBins(final List<Double> sortedValues, final AutoBinnerNodeSettings settings) {
        var minValue = settings.m_fixLowerBound ? settings.m_fixedLowerBound
            : sortedValues.stream().min(Double::compare).orElse(0.0);
        var maxValue = settings.m_fixUpperBound ? settings.m_fixedUpperBound
            : sortedValues.stream().max(Double::compare).orElse(1.0);

        return switch (settings.m_binningType) {
            case EQUAL_WIDTH -> createBinsWithEqualWidth(minValue, maxValue, settings.m_numberOfBins,
                settings.m_binNames, settings.m_enforceIntegerCutoffs);
            case EQUAL_FREQUENCY -> createBinsWithEqualFrequency(sortedValues, settings.m_numberOfBins,
                settings.m_binNames, settings.m_enforceIntegerCutoffs);
            case CUSTOM_CUTOFFS -> complexBoundsToBins( //
                Arrays.stream(settings.m_customCutoffs).mapToDouble(value -> value.m_cutoff).toArray(), //
                Arrays.stream(settings.m_customCutoffs).map(value -> value.m_matchType).toArray(MatchType[]::new), //
                settings.m_binNames //
                );
            case CUSTOM_QUANTILES -> complexBoundsToBins( //
                Arrays.stream(settings.m_customQuantiles).mapToDouble(value -> value.m_quantile).toArray(), //
                Arrays.stream(settings.m_customQuantiles).map(value -> value.m_matchType).toArray(MatchType[]::new), //
                settings.m_binNames //
                );
        };
    }

    private static Bin[] createBinsWithEqualWidth(final double minValue, final double maxValue, final int numBins,
        final BinNames nameType, final boolean integerCutoffs) {
        var binWidth = (maxValue - minValue) / numBins;

        var binBoundaries = IntStream.range(0, numBins + 1) //
            .mapToDouble(i -> minValue + (i * binWidth)) //
            .toArray();
        if (integerCutoffs) {
            enforceIntegerBoundaries(binBoundaries);
        }

        return simpleBoundsToBins(binBoundaries, nameType);
    }

    /**
     * Create bins where each bin has approximately the same number of values.
     *
     * @param sortedValues
     * @param numBins
     * @param nameType
     * @param integerCutoffs
     * @return
     */
    private static Bin[] createBinsWithEqualFrequency(final List<Double> sortedValues, final int numBins,
        final BinNames nameType, final boolean integerCutoffs) {

        var numValues = sortedValues.size();
        var binSize = numValues / numBins;

        // fill each bin until we hit binSize number of values. Then, set the bin boundaries to the last element added.

        var binBoundaries = new double[numBins + 1];
        binBoundaries[0] = sortedValues.get(0);
        binBoundaries[numBins] = sortedValues.get(numValues - 1);

        for (int i = 0; i < numBins; ++i) {
            var binEndIndex = i * binSize;

            if (i == numBins - 1) {
                binEndIndex = numValues;
            }

            binBoundaries[i + 1] = sortedValues.get(binEndIndex - 1);
        }

        if (integerCutoffs) {
            enforceIntegerBoundaries(binBoundaries);
        }

        return simpleBoundsToBins(binBoundaries, nameType);
    }

    private static void enforceIntegerBoundaries(final double[] boundaries) {
        for (int i = 0; i < boundaries.length; i++) {
            boundaries[i] = Math.round(boundaries[i]);
        }
    }

    /**
     * Convert the boundaries into bins, in the simple case where all bins are open at the top and closed at the bottom
     * (except the last which is closed at the top).
     *
     * @param bounds the bin boundaries
     * @param nameType the type of name to use for the bins
     * @return the bins
     */
    private static Bin[] simpleBoundsToBins(final double[] bounds, final BinNames nameType) {
        return IntStream.range(0, bounds.length - 1) //
            .mapToObj(i -> {
                var lowerOpen = false; // false for all bins
                var upperOpen = i != bounds.length - 1; // true for all bins except last, which is closed at the top
                var lowerBound = bounds[i];
                var upperBound = bounds[i + 1];
                var name = nameType.computedName(i, lowerOpen, lowerBound, upperOpen, upperBound);

                return new NumericBin( //
                    name, //
                    lowerOpen, //
                    bounds[i], //
                    upperOpen, //
                    bounds[i + 1] //
                );
            }) //
            .toArray(Bin[]::new);
    }

    private static Bin[] complexBoundsToBins(final double[] bounds, final MatchType[] boundTypes,
        final BinNames nameType) {
        return IntStream.range(0, bounds.length - 1) //
            .mapToObj(i -> {
                // upper bound of top bin and lower bound of bottom bin are always closed
                var lowerOpen = boundTypes[i] == MatchType.TO_LOWER_BIN && i != 0;
                var upperOpen = boundTypes[i + 1] == MatchType.TO_UPPER_BIN && i != bounds.length - 1;
                var lowerBound = bounds[i];
                var upperBound = bounds[i + 1];
                var name = nameType.computedName(i, lowerOpen, lowerBound, upperOpen, upperBound);

                return new NumericBin( //
                    name, //
                    lowerOpen, //
                    bounds[i], //
                    upperOpen, //
                    bounds[i + 1] //
                );
            }) //
            .toArray(Bin[]::new);
    }
}
