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
 *   23.05.2006 (gabriel): created
 */
package org.knime.base.node.viz.property.color;

import java.io.File;
import java.io.IOException;

import org.knime.base.node.viz.property.color.ColorManager2NodeModel.OutputSpecification;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.property.ColorHandler;
import org.knime.core.data.property.ColorModel;
import org.knime.core.data.property.ColorModelNominal;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.viewproperty.ColorHandlerPortObject;
import org.knime.core.node.port.viewproperty.ViewPropertyPortObject;
import org.knime.core.node.util.CheckUtils;

/**
 * Node model to append color settings to a column selected in the dialog.
 *
 * @author Thomas Gabriel, University of Konstanz
 */
final class ColorAppender2NodeModel extends NodeModel {

    private final SettingsModelBoolean m_applyToColumnNamesModel = createApplyToColumnNamesModel();
    private final SettingsModelString m_columnModel = createColumnModel(m_applyToColumnNamesModel);

    /**
     * Create a new color appender model.
     */
    ColorAppender2NodeModel(final boolean withModelOutput) {
        super(new PortType[]{ColorHandlerPortObject.TYPE, BufferedDataTable.TYPE},
            withModelOutput ? new PortType[]{BufferedDataTable.TYPE, ColorHandlerPortObject.TYPE}
                : new PortType[]{BufferedDataTable.TYPE});
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs)
            throws InvalidSettingsException {
        final var modelSpec = (DataTableSpec)inSpecs[0];
        final var dataSpec = (DataTableSpec)inSpecs[1];
        final var outputSpecification = createOutputSpec(modelSpec, dataSpec);
        if (getNrOutPorts() == 1) { // deprecated node, as of 5.1
            return new DataTableSpec[]{outputSpecification.dataSpec()};
        } else {
            return new PortObjectSpec[]{outputSpecification.dataSpec(), outputSpecification.modelSpec()};
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inData,
            final ExecutionContext exec) throws Exception {
        final var modelSpec = ((ViewPropertyPortObject)inData[0]).getSpec();
        final var dataSpec = ((BufferedDataTable)inData[1]).getSpec();
        final var outputSpecification = createOutputSpec(modelSpec, dataSpec);
        BufferedDataTable table =
            exec.createSpecReplacerTable((BufferedDataTable)inData[1], outputSpecification.dataSpec());
        if (getNrOutPorts() == 1) { // deprecated node, as of 5.1
            return new BufferedDataTable[]{table};
        } else {
            return new PortObject[]{table, new ColorHandlerPortObject(outputSpecification.modelSpec(),
                "Coloring on \"" + m_columnModel.getStringValue() + "\"")};
        }
    }

    private OutputSpecification createOutputSpec(final DataTableSpec modelSpec,
            final DataTableSpec dataSpec) throws InvalidSettingsException {
        CheckUtils.checkSetting(modelSpec.getNumColumns() == 1, "No color information in input");
        final var columnSpecPerModel = modelSpec.getColumnSpec(0);
        final var inColorHandler = columnSpecPerModel.getColorHandler();
        final var applyToColumnNames = m_applyToColumnNamesModel.getBooleanValue();
        final var inColorModel = inColorHandler.getColorModel();
        // for nominal columns we will apply the palette to unknown values in the data:
        // e.g. if the model has definitions for colors A and B but the data now sees values for C and D
        // we will apply the palette to these new values, while still leaving A and B assignments untouched.
        final ColorModel colorModelOutput;
        final String columnNameInOutput;
        if (applyToColumnNames) {
            final var columnNames = ColorManager2NodeModel.columnNamesToDataCellSet(dataSpec);
            CheckUtils.checkSetting(inColorModel instanceof ColorModelNominal, "Node is configured to apply coloring "
                + "to column names but the input model does not represent a model on nominal values.");
            colorModelOutput = ((ColorModelNominal)inColorModel).applyToNewValues(columnNames);
            columnNameInOutput = ColorManager2NodeModel.COLUMNS_CONTAINED_IN_TABLE_IDENTIFIER;
        } else {
            var column = m_columnModel.getStringValue();
            if (column == null) { // auto-configuration/guessing
                // TODO check for nominal/range coloring and guess last
                // suitable column (while setting a warning message)
                if (dataSpec.containsName(columnSpecPerModel.getName())) { // NOSONAR (if merges)
                    column = columnSpecPerModel.getName();
                }
            }
            CheckUtils.checkSettingNotNull(column, "Node is not properly configured.");
            final var columnSpec = dataSpec.getColumnSpec(column);
            CheckUtils.checkSettingNotNull(columnSpec, "Column \"%s\" not available.", column);
            if (inColorModel instanceof ColorModelNominal nominalModel) {
                final var possibleValues = columnSpec.getDomain().getValues();
                CheckUtils.checkSettingNotNull(possibleValues, "Column \"%s\" does not have posssible values set in "
                        + "its domain - execute predecessor or apply Domain Calculator", column);
                colorModelOutput = nominalModel.applyToNewValues(possibleValues);
            } else {
                colorModelOutput = inColorModel;
            }
            columnNameInOutput = column;
        }
        return ColorManager2NodeModel.getOutputDataSpecification(dataSpec, columnNameInOutput,
            new ColorHandler(colorModelOutput));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        try {
            m_applyToColumnNamesModel.loadSettingsFrom(settings);
        } catch (InvalidSettingsException ise) {
            m_applyToColumnNamesModel.setBooleanValue(false); // added in 5.1
        }
        m_columnModel.loadSettingsFrom(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir,
            final ExecutionMonitor exec)
            throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_applyToColumnNamesModel.saveSettingsTo(settings);
        m_columnModel.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings)
            throws InvalidSettingsException {
        m_columnModel.validateSettings(settings);
    }

    static SettingsModelBoolean createApplyToColumnNamesModel() {
        return new SettingsModelBoolean("apply_to_column_names", false);
    }

    /**
     * @return settings model for column selection
     */
    static SettingsModelString createColumnModel(final SettingsModelBoolean applyToColumnNamesModel) {
        final var result = new SettingsModelString("selected_column", null);
        applyToColumnNamesModel.addChangeListener(e -> result.setEnabled(!applyToColumnNamesModel.getBooleanValue()));
        result.setEnabled(!applyToColumnNamesModel.getBooleanValue());
        return result;
    }

}
