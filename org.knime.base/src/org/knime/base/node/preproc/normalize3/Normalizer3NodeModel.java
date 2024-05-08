/*
 * ------------------------------------------------------------------------
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
 * -------------------------------------------------------------------
 *
 * History
 *   19.04.2005 (cebron): created
 */
package org.knime.base.node.preproc.normalize3;

import java.util.Arrays;
import java.util.HashSet;

import org.knime.base.data.filter.column.FilterColumnTable;
import org.knime.base.data.normalize.Normalizer2;
import org.knime.base.data.normalize.NormalizerPortObject;
import org.knime.base.node.preproc.normalize3.NormalizerNodeSettings.NormalizerMode;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUINodeModel;

/**
 * The Normalizer3NodeModel uses the Normalizer to normalize the input DataTable.
 *
 * @see Normalizer2
 * @author Nicolas Cebron, University of Konstanz
 * @author Marcel Hanser, University of Konstanz
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class Normalizer3NodeModel extends WebUINodeModel<NormalizerNodeSettings> {
    private static final int MAX_UNKNOWN_COLS = 3;

    Normalizer3NodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, NormalizerNodeSettings.class);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs, final NormalizerNodeSettings modelSettings)
        throws InvalidSettingsException {
        final var spec = (DataTableSpec)inSpecs[0];
        final var columns = getIncludedColumns(spec, modelSettings);
        final var modelSpec = FilterColumnTable.createFilterTableSpec(spec, columns);
        return new PortObjectSpec[]{Normalizer2.generateNewSpec(spec, columns), modelSpec};
    }

    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec,
        final NormalizerNodeSettings modelSettings) throws CanceledExecutionException {
        final var inTable = (BufferedDataTable)inObjects[0];
        final var inSpec = inTable.getSpec();
        final var includedColumns = getIncludedColumns(inSpec, modelSettings);
        final var ntable = new Normalizer2(inTable, includedColumns);
        if (ntable.getErrorMessage() != null) {
            setWarningMessage(ntable.getErrorMessage());
        }

        final var prepareExec = exec.createSubExecutionContext(.3);
        final var outTable = switch (modelSettings.m_mode) {
            case MINMAX -> ntable.doMinMaxNorm(modelSettings.m_max, modelSettings.m_min, prepareExec);
            case Z_SCORE -> ntable.doZScoreNorm(prepareExec);
            case DECIMALSCALING -> ntable.doDecimalScaling(prepareExec);
        };
        if (outTable.getErrorMessage() != null) {
            throw new IllegalStateException(outTable.getErrorMessage());
        }

        var spec = outTable.getDataTableSpec();
        // fix the domain to min/max in case of MINMAX_MODE; fixes bug #1187
        // ideally this goes into the AffineTransConfiguration/AffineTransTable,
        // but that will not work with the applier node (which will apply
        // the same transformation, which is not guaranteed to snap to min/max)
        if (modelSettings.m_mode == NormalizerMode.MINMAX) {
            final var includedColumnsSet = new HashSet<>(Arrays.asList(includedColumns));
            final var newColSpecs = spec.stream()
                .map(colSpec -> includedColumnsSet.contains(colSpec.getName())
                    ? updateMinMax(colSpec, modelSettings.m_min, modelSettings.m_max) : colSpec)
                .toArray(DataColumnSpec[]::new);
            spec = new DataTableSpec(spec.getName(), newColSpecs);
        }

        final var normalizerPO = new NormalizerPortObject(
            FilterColumnTable.createFilterTableSpec(inSpec, includedColumns), outTable.getConfiguration());
        final var bufferedOutTable = exec.createBufferedDataTable(outTable, exec.createSubProgress(.7));
        final var specReplacerOutTable = exec.createSpecReplacerTable(bufferedOutTable, spec);
        return new PortObject[]{specReplacerOutTable, normalizerPO};
    }

    private String[] getIncludedColumns(final DataTableSpec spec, final NormalizerNodeSettings modelSettings) {
        final var numericCols = spec.stream()//
            .filter(colSpec -> colSpec.getType().isCompatible(DoubleValue.class)).map(DataColumnSpec::getName)//
            .toArray(String[]::new);
        final var nonMissingSelected = modelSettings.m_dataColumnFilterConfig.getNonMissingSelected(numericCols, spec);

        if (nonMissingSelected.length == 0) {
            final var warnings = new StringBuilder("No columns included - input stays unchanged.");
            final var selected = modelSettings.m_dataColumnFilterConfig.getSelected(numericCols, spec);
            if (selected.length > 0) {
                warnings.append("\nThe following columns were included before but no longer exist:\n");
                warnings.append(ConvenienceMethods.getShortStringFrom(Arrays.asList(selected), MAX_UNKNOWN_COLS));
            }
            setWarningMessage(warnings.toString());
        }

        return nonMissingSelected;
    }

    private static DataColumnSpec updateMinMax(final DataColumnSpec colSpec, final double min, final double max) {
        final var creator = new DataColumnSpecCreator(colSpec);
        final var domCreator = new DataColumnDomainCreator(colSpec.getDomain());
        domCreator.setLowerBound(new DoubleCell(min));
        domCreator.setUpperBound(new DoubleCell(max));
        creator.setDomain(domCreator.createDomain());
        return creator.createSpec();
    }
}
