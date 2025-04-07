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
 * ------------------------------------------------------------------------
 *
 */
package org.knime.base.node.preproc.columnmerge;

import static org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationUtils.validateColumnName;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import org.knime.base.node.preproc.columnmerge.ColumnMergerNodeSettings.OutputPlacement;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationUtils.InvalidColumnNameState;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;

/**
 * Model to column merger.
 *
 * @author Bernd Wiswedel, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction")
final class ColumnMergerNodeModel extends WebUISimpleStreamableFunctionNodeModel<ColumnMergerNodeSettings> {

    /**
     * @param configuration
     */
    protected ColumnMergerNodeModel(final WebUINodeConfiguration configuration) {
        super(configuration, ColumnMergerNodeSettings.class);
    }

    /**
     * Creates column rearranger doing all the work.
     *
     * @param spec The input spec.
     * @return The rearranger creating the output table/spec.
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ColumnMergerNodeSettings modelSettings) throws InvalidSettingsException {
        final int primColIndex = spec.findColumnIndex(modelSettings.m_primaryColumn);
        if (primColIndex < 0) {
            throw new InvalidSettingsException("The selected primary column \"" + modelSettings.m_primaryColumn
                + "\" does not exist in the input table.");
        }

        final int secColIndex = spec.findColumnIndex(modelSettings.m_secondaryColumn);
        if (secColIndex < 0) {
            throw new InvalidSettingsException("The selected secondary column \"" + modelSettings.m_secondaryColumn
                + "\" does not exist in the input table.");
        }
        DataColumnSpec c1 = spec.getColumnSpec(primColIndex);
        DataColumnSpec c2 = spec.getColumnSpec(secColIndex);
        DataType commonType = DataType.getCommonSuperType(c1.getType(), c2.getType());
        String name = getNewColumnName(spec, modelSettings, c1, c2);

        DataColumnSpec outColSpec = new DataColumnSpecCreator(name, commonType).createSpec();
        SingleCellFactory fac = new SingleCellFactory(outColSpec) {
            /** {@inheritDoc} */
            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell1 = row.getCell(primColIndex);
                DataCell cell2 = row.getCell(secColIndex);
                return !cell1.isMissing() ? cell1 : cell2;
            }
        };

        return getResult(spec, modelSettings, fac, primColIndex, secColIndex);
    }

    private static String getNewColumnName(final DataTableSpec spec, final ColumnMergerNodeSettings modelSettings,
        final DataColumnSpec c1, final DataColumnSpec c2) throws InvalidSettingsException {
        switch (modelSettings.m_outputPlacement) {
            case ReplacePrimary:

            case ReplaceBoth:
                return c1.getName();
            case ReplaceSecondary:
                return c2.getName();
            case AppendAsNewColumn:
                return DataTableSpec.getUniqueColumnName(spec, modelSettings.m_outputName);
            default:
                throw new InvalidSettingsException(
                    String.format("Coding problem: Unrecognized option \"%s\" for output placement selection.",
                        modelSettings.m_outputPlacement));
        }
    }

    private static ColumnRearranger getResult(final DataTableSpec spec, final ColumnMergerNodeSettings modelSettings,
        final SingleCellFactory fac, final int primColIndex, final int secColIndex) throws InvalidSettingsException {

        ColumnRearranger result = new ColumnRearranger(spec);
        switch (modelSettings.m_outputPlacement) {
            case ReplacePrimary:
                result.replace(fac, primColIndex);
                return result;
            case ReplaceBoth:
                result.replace(fac, primColIndex);
                result.remove(secColIndex);
                return result;
            case ReplaceSecondary:
                result.replace(fac, secColIndex);
                return result;
            case AppendAsNewColumn:
                result.append(fac);
                return result;
            default:
                throw new InvalidSettingsException(
                    String.format("Coding problem: Unrecognized option \"%s\" for output placement selection.",
                        modelSettings.m_outputPlacement));
        }
    }

    private static final Function<InvalidColumnNameState, String> INVALID_COL_NAME_TO_ERROR_MSG =
            new ColumnNameValidationMessageBuilder("new column name").build();

    @Override
    protected void validateSettings(final ColumnMergerNodeSettings settings) throws InvalidSettingsException {
        if (settings.m_outputPlacement == OutputPlacement.AppendAsNewColumn) {
            if (settings.m_doNotAllowBlankOrPaddedColumnName) {
                validateColumnName(settings.m_outputName, INVALID_COL_NAME_TO_ERROR_MSG);
            } else if (settings.m_outputName == null || settings.m_outputName.length() == 0) {
                throw new InvalidSettingsException(
                    "Output column name must not be empty if 'Append as new column' is selected.");
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

    /** {@inheritDoc} */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // no internals
    }

}
