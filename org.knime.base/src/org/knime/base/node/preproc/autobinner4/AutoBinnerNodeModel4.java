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

import java.util.Arrays;
import java.util.Comparator;

import org.knime.base.node.util.binning.AutoBinningSettings;
import org.knime.base.node.util.binning.AutoBinningSettings.BinBoundary;
import org.knime.base.node.util.binning.AutoBinningSettings.BinNamingSettings;
import org.knime.base.node.util.binning.AutoBinningSettings.BinningSettings;
import org.knime.base.node.util.binning.AutoBinningSettings.BinningType;
import org.knime.base.node.util.binning.AutoBinningSettings.ColumnOutputNamingSettings;
import org.knime.base.node.util.binning.AutoBinningSettings.DataBoundsSettings.BoundSetting.FixedBound;
import org.knime.base.node.util.binning.AutoBinningSettings.DataBoundsSettings.BoundSetting.NoBound;
import org.knime.base.node.util.binning.AutoBinningSettings.NumberFormattingSettings;
import org.knime.base.node.util.binning.AutoBinningUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

import com.google.common.collect.Comparators;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class AutoBinnerNodeModel4 extends WebUINodeModel<AutoBinnerNodeSettings> {

    AutoBinnerNodeModel4(final WebUINodeConfiguration configuration) {
        super(configuration, AutoBinnerNodeSettings.class);
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final AutoBinnerNodeSettings modelSettings) throws Exception {

        var inTable = (BufferedDataTable)inData[0];
        var autobinningSettings = createSettingsForAutobinning(modelSettings, inTable.getSpec());
        var autoBinner = new AutoBinningUtils.AutoBinner(autobinningSettings, inTable.getSpec());

        return autoBinner.createNodeOutput(inTable, exec);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final AutoBinnerNodeSettings modelSettings)
        throws InvalidSettingsException {

        var inSpec = (DataTableSpec)inSpecs[0];
        var autobinningSettings = createSettingsForAutobinning(modelSettings, inSpec);
        var autoBinner = new AutoBinningUtils.AutoBinner(autobinningSettings, inSpec);

        return autoBinner.createOutputSpec(inSpec);
    }

    @Override
    protected void validateSettings(final AutoBinnerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_binningType == BinningType.CUSTOM_CUTOFFS && settings.m_customCutoffs.length < 2) {
            throw new InvalidSettingsException("At least two custom cutoffs are required.");
        } else if (settings.m_binningType == BinningType.CUSTOM_QUANTILES && settings.m_customQuantiles.length < 2) {
            throw new InvalidSettingsException("At least two custom quantiles are required.");
        } else if (settings.m_binningType == BinningType.EQUAL_WIDTH && settings.m_numberOfBins < 1) {
            throw new InvalidSettingsException("Number of bins must be at least 1 for equal width binning.");
        } else if (settings.m_binningType == BinningType.EQUAL_FREQUENCY && settings.m_numberOfBins < 1) {
            throw new InvalidSettingsException("Number of bins must be at least 1 for equal frequency binning.");
        }

        // check that quantiles or cutoffs are sorted (if applicable)
        if (settings.m_binningType == BinningType.CUSTOM_CUTOFFS) {
            var isSorted = Comparators.isInStrictOrder(Arrays.asList(settings.m_customCutoffs),
                Comparator.comparingDouble(c -> c.m_cutoff));

            if (!isSorted) {
                throw new InvalidSettingsException("Custom cutoffs must be sorted in ascending order.");
            }
        } else if (settings.m_binningType == BinningType.CUSTOM_QUANTILES) {
            var isSorted = Comparators.isInStrictOrder(Arrays.asList(settings.m_customQuantiles),
                Comparator.comparingDouble(q -> q.m_quantile));

            if (!isSorted) {
                throw new InvalidSettingsException("Custom quantiles must be sorted in ascending order.");
            }
        }

        // if boundaries are provided, the lower bound must be less than the upper bound
        if (settings.m_fixLowerBound && settings.m_fixUpperBound
            && settings.m_fixedLowerBound >= settings.m_fixedUpperBound) {
            throw new InvalidSettingsException("Fixed lower bound must be less than fixed upper bound.");
        }
    }

    private static AutoBinningSettings createSettingsForAutobinning(final AutoBinnerNodeSettings settings,
        final DataTableSpec inSpec) {
        var binningSettings = switch (settings.m_binningType) {
            case CUSTOM_CUTOFFS -> new BinningSettings.FixedBoundaries( //
                Arrays.stream(settings.m_customCutoffs) //
                    .map(c -> new BinBoundary(c.m_cutoff, c.m_matchType)) //
                    .toArray(BinBoundary[]::new) //
                );
            case CUSTOM_QUANTILES -> new BinningSettings.FixedQuantiles( //
                Arrays.stream(settings.m_customQuantiles) //
                    .map(q -> new BinBoundary(q.m_quantile, q.m_matchType)) //
                    .toArray(BinBoundary[]::new) //
                );
            case EQUAL_WIDTH -> new BinningSettings.EqualWidth(settings.m_numberOfBins);
            case EQUAL_FREQUENCY -> new BinningSettings.EqualCount(settings.m_numberOfBins);
        };

        var columnOutputNaming = switch (settings.m_replaceOrAppend) {
            case REPLACE -> new ColumnOutputNamingSettings.ReplaceColumn();
            case APPEND -> new ColumnOutputNamingSettings.AppendSuffix(settings.m_suffix);
        };

        var numberFormatting = switch (settings.m_numberFormat) {
            case COLUMN_FORMAT -> new NumberFormattingSettings.ColumnFormat();
            case CUSTOM -> new NumberFormattingSettings.CustomFormat(settings.m_numberFormatSettings);
        };

        var selectedColumns = Arrays.asList( //
            settings.m_selectedColumns.filter(inSpec.stream().filter(AutoBinningUtils::columnCanBeBinned).toList()) //
        );

        var boundsSettings = new AutoBinningSettings.DataBoundsSettings( //
            settings.m_fixLowerBound //
                ? new FixedBound(settings.m_fixedLowerBound, settings.m_lowerOutlierValue) //
                : new NoBound(settings.m_lowerOutlierValue), //
            settings.m_fixUpperBound //
                ? new FixedBound(settings.m_fixedUpperBound, settings.m_upperOutlierValue) //
                : new NoBound(settings.m_upperOutlierValue) //
        );

        return new AutoBinningSettings( //
            selectedColumns, //
            binningSettings, //
            settings.m_enforceIntegerCutoffs, //
            new BinNamingSettings(settings.m_binNames, numberFormatting), //
            boundsSettings, //
            columnOutputNaming //
        );
    }
}
