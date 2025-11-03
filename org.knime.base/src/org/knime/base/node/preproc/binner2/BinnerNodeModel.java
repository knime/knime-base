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
package org.knime.base.node.preproc.binner2;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

import org.dmg.pmml.DerivedFieldDocument.DerivedField;
import org.knime.base.node.preproc.binner2.BinnerNodeSettings.ReplaceOrAppend;
import org.knime.base.node.preproc.binner2.BinnerNodeSettingsEnums.BinningType;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.preproc.DerivedFieldMapper;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.util.binning.BinningPMMLApplyUtil;
import org.knime.core.util.binning.BinningSettings.BinBoundary;
import org.knime.core.util.binning.BinningSettings.BinNamingScheme;
import org.knime.core.util.binning.BinningSettings.BinNamingUtils;
import org.knime.core.util.binning.BinningSettings.BinNamingUtils.BinNamingNumberFormatter;
import org.knime.core.util.binning.BinningSettings.BinningMethod;
import org.knime.core.util.binning.BinningSettings.DataBounds;
import org.knime.core.util.binning.BinningUtil;
import org.knime.core.util.binning.numeric.Bin;
import org.knime.core.util.binning.numeric.NumericBin;
import org.knime.core.util.binning.numeric.PMMLBinningTranslator;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

import com.google.common.collect.Comparators;

/**
 * Node model for the Auto Binner node, web UI version.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class BinnerNodeModel extends WebUINodeModel<BinnerNodeSettings> {

    BinnerNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, BinnerNodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final BinnerNodeSettings modelSettings)
        throws InvalidSettingsException {
        var inSpec = (DataTableSpec)inSpecs[0];

        final var binnableColumns = inSpec.stream() //
            .filter(BinningUtil::columnCanBeBinned) //
            .toList();

        final var missingColumns = modelSettings.m_selectedColumns.getMissingSelected(binnableColumns);
        if (missingColumns.length > 0) {
            throw new InvalidSettingsException("Input table does not contain the following selected column(s): "
                + ConvenienceMethods.getShortStringFrom(new HashSet<>(Arrays.asList(missingColumns)), 3));
        }

        var selectedColumns = modelSettings.m_selectedColumns.filter(binnableColumns);

        if (selectedColumns.length == 0) {
            throw new InvalidSettingsException("No columns are selected for binning.");
        }

        var outputColumns = getOutputColumns(modelSettings, selectedColumns);
        var outputTableSpec = createOutputSpec(inSpec, outputColumns);
        var pmmlSpec = BinningPMMLApplyUtil.createPMMLOutSpec(inSpec, Arrays.asList(selectedColumns));

        return new PortObjectSpec[]{outputTableSpec, pmmlSpec};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inData, final ExecutionContext exec,
        final BinnerNodeSettings modelSettings) throws Exception {

        var inTable = (BufferedDataTable)inData[0];
        var inSpec = inTable.getDataTableSpec();

        var selectedColumns = modelSettings.m_selectedColumns.filter( //
            inSpec.stream() //
                .filter(BinningUtil::columnCanBeBinned) //
                .toList() //
        );
        final var outputColumns = getOutputColumns(modelSettings, selectedColumns);

        final var inSpecWithAppendedCols = createInputSpecWithAppendedColumns(inSpec, outputColumns);

        final var outPMMLSpec =
            BinningPMMLApplyUtil.createPMMLOutSpec(inSpecWithAppendedCols, Arrays.asList(selectedColumns));
        final var outPMMLPortObject = new PMMLPortObject(outPMMLSpec, null, inSpec);

        final var translator = createTranslator(inSpec, inTable, exec, modelSettings, selectedColumns, outputColumns);
        outPMMLPortObject.addGlobalTransformations(translator.exportToTransDict());

        final var columnRearranger = BinningPMMLApplyUtil.createColumnRearranger(inSpec, outPMMLPortObject);
        final var outputTable = exec.createColumnRearrangeTable((BufferedDataTable)inData[0], columnRearranger, exec);

        return new PortObject[]{outputTable, outPMMLPortObject};

    }

    private static PMMLBinningTranslator createTranslator(final DataTableSpec inSpec, //
        final BufferedDataTable inTable, //
        final ExecutionContext exec, //
        final BinnerNodeSettings modelSettings, //
        final String[] selectedColumns, //
        final List<String> outputColumns //
    ) throws CanceledExecutionException, KNIMEException {
        var extractedData = BinningUtil.extractDataFromTableAndSort(inTable, exec, selectedColumns);

        final var binningMethod = getBinningMethod(modelSettings);

        Map<String, String> inColToOutCol = new HashMap<>();
        Map<String, Bin[]> binsByColumnName = new HashMap<>();

        for (int i = 0; i < selectedColumns.length; i++) {
            final var inputColName = selectedColumns[i];
            final var inputColSpec = inSpec.getColumnSpec(inputColName);
            final var inputColDomain = inputColSpec.getDomain();
            final var lowerDomainBound = inputColDomain.hasLowerBound() //
                ? OptionalDouble.of(((DoubleValue)inputColDomain.getLowerBound()).getDoubleValue()) //
                : OptionalDouble.empty();
            final var upperDomainBound = inputColDomain.hasUpperBound() //
                ? OptionalDouble.of(((DoubleValue)inputColDomain.getUpperBound()).getDoubleValue()) //
                : OptionalDouble.empty();
            final var domainBounds = new DataBounds( //
                lowerDomainBound, //
                upperDomainBound //
            );
            final var dataBoundsSettings = new DataBounds( //
                modelSettings.m_fixLowerBound ? OptionalDouble.of(modelSettings.m_fixedLowerBound)
                    : OptionalDouble.empty(),
                modelSettings.m_fixUpperBound ? OptionalDouble.of(modelSettings.m_fixedUpperBound)
                    : OptionalDouble.empty());
            final var sortedValues = extractedData.get(inputColName);
            final var binNamingScheme = createBinNamingScheme(modelSettings, inputColSpec);

            List<NumericBin> bins = null;

            try {
                bins = BinningUtil.createBinsFromSortedValues(//
                    sortedValues, //
                    exec, //
                    binningMethod, //
                    binNamingScheme, //
                    dataBoundsSettings, //
                    domainBounds //
                );
            } catch (IllegalArgumentException ex) {
                throw new KNIMEException(
                    String.format("Error while computing bins for column %s: %s", inputColName, ex.getMessage()), ex);
            }

            inColToOutCol.put(inputColName,
                modelSettings.m_replaceOrAppend == ReplaceOrAppend.REPLACE ? null : outputColumns.get(i));
            binsByColumnName.put(inputColName, bins.stream().toArray(Bin[]::new));

        }

        return new PMMLBinningTranslator( //
            binsByColumnName, //
            inColToOutCol, //
            new DerivedFieldMapper((DerivedField[])null) //
        );
    }

    @Override
    protected void validateSettings(final BinnerNodeSettings settings) throws InvalidSettingsException { // NOSONAR complexity is fine
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

        if (settings.m_replaceOrAppend == ReplaceOrAppend.APPEND && settings.m_suffix.isBlank()) {
            throw new InvalidSettingsException("Suffix must not be empty when appending columns.");
        }
    }

    private static List<String> getOutputColumns(final BinnerNodeSettings modelSettings,
        final String[] selectedColumns) {
        return Arrays.stream(selectedColumns) //
            .map(inputColName -> modelSettings.m_replaceOrAppend == ReplaceOrAppend.REPLACE //
                ? inputColName //
                : (inputColName + modelSettings.m_suffix)) //
            .toList();
    }

    private static DataTableSpec createOutputSpec(final DataTableSpec inSpec, final List<String> outputColumns) {
        var specCreator = new DataTableSpecCreator(inSpec);
        for (String outputColumn : outputColumns) {
            if (inSpec.containsName(outputColumn)) {
                specCreator.replaceColumn( //
                    inSpec.findColumnIndex(outputColumn), //
                    new DataColumnSpecCreator(outputColumn, StringCellFactory.TYPE).createSpec() //
                );
            } else {
                specCreator.addColumns( //
                    new DataColumnSpecCreator(outputColumn, StringCellFactory.TYPE).createSpec() //
                );
            }
        }
        return specCreator.createSpec();
    }

    private static DataTableSpec createInputSpecWithAppendedColumns(final DataTableSpec inSpec,
        final List<String> outputColumns) {
        var specCreator = new DataTableSpecCreator(inSpec);
        for (String outputColumn : outputColumns) {
            if (!inSpec.containsName(outputColumn)) {
                specCreator.addColumns( //
                    new DataColumnSpecCreator(outputColumn, StringCellFactory.TYPE).createSpec() //
                );
            }
        }
        return specCreator.createSpec();
    }

    private static BinningMethod getBinningMethod(final BinnerNodeSettings settings) {
        return switch (settings.m_binningType) {
            case CUSTOM_CUTOFFS -> new BinningMethod.FixedBoundaries( //
                Arrays.stream(settings.m_customCutoffs) //
                    .map(c -> new BinBoundary(c.m_cutoff, c.m_matchType.m_baseValue)) //
                    .toArray(BinBoundary[]::new) //
                );
            case CUSTOM_QUANTILES -> new BinningMethod.FixedQuantiles( //
                Arrays.stream(settings.m_customQuantiles) //
                    .map(q -> new BinBoundary(q.m_quantile, q.m_matchType.m_baseValue)) //
                    .toArray(BinBoundary[]::new), //
                settings.m_enforceIntegerCutoffs);
            case EQUAL_WIDTH -> new BinningMethod.EqualWidth(settings.m_numberOfBins, settings.m_enforceIntegerCutoffs);
            case EQUAL_FREQUENCY -> new BinningMethod.EqualCount(settings.m_numberOfBins,
                settings.m_enforceIntegerCutoffs);
        };
    }

    private static BinNamingScheme createBinNamingScheme(final BinnerNodeSettings settings,
        final DataColumnSpec colSpec) {
        var binNaming = switch (settings.m_binNames) {
            case NUMBERED -> BinNamingUtils.getNumberedBinNaming(settings.m_usePrefix ? settings.m_prefix : "Bin ");
            case BORDERS -> BinNamingUtils.getBordersBinNaming(getNumberFormatting(colSpec));
            case MIDPOINTS -> BinNamingUtils.getMidpointsBinNaming(getNumberFormatting(colSpec));
        };
        return new BinNamingScheme(binNaming, settings.m_lowerOutlierValue, settings.m_upperOutlierValue);
    }

    private static BinNamingNumberFormatter getNumberFormatting(final DataColumnSpec colSpec) {
        return BinNamingUtils.BinNamingNumberFormatterUtils.getDefaultNumberFormatterForColumn(colSpec);
    }
}
