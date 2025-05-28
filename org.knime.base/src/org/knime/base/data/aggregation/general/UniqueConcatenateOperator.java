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
 */

package org.knime.base.data.aggregation.general;

import java.util.Set;

import javax.swing.JPanel;

import org.knime.base.data.aggregation.AggregationOperator;
import org.knime.base.data.aggregation.GlobalSettings;
import org.knime.base.data.aggregation.OperatorColumnSettings;
import org.knime.base.data.aggregation.OperatorData;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.SettingsModelDouble;

/**
 * Returns the concatenation of all different values per group.
 *
 * @author Tobias Koetter, University of Konstanz
 */
public class UniqueConcatenateOperator extends SetCellOperator {

    private UniqueConcatenateSettingsPanel m_settingsPanel;

    private final UniqueConcatenateFuntionSettings m_settings = new UniqueConcatenateFuntionSettings();

    private final DataType m_type = StringCell.TYPE;

    /**Constructor for class Concatenate.
     * @param globalSettings the global settings
     * @param opColSettings the operator column specific settings
     */
    public UniqueConcatenateOperator(final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        super(new OperatorData("uniqueConcatenate_V2.4", "Unique concatenate",
                "Unique concatenate", true, false,
                DataValue.class, true), globalSettings, opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataType getDataType(final DataType origType) {
        return m_type;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AggregationOperator createInstance(
            final GlobalSettings globalSettings,
            final OperatorColumnSettings opColSettings) {
        return new UniqueConcatenateOperator(globalSettings,
                opColSettings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataCell getResultInternal() {
        final Set<DataCell> groupMembers = getGroupMembers();
        if (groupMembers.isEmpty()) {
            return DataType.getMissingCell();
        }
        final StringBuilder buf = new StringBuilder();
        boolean first = true;
        for (final DataCell val : groupMembers) {
            if (first) {
                first = false;
            } else {
                buf.append(getValueDelimiter());
            }
            buf.append(val.toString());
        }
        return new StringCell(buf.toString());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDescription() {
        return "Concatenates each member only once per group.";
    }

    /**
     * Override this method and return <code>true</code> if the operator requires additional settings.
     *
     * {@inheritDoc}
     */
    @Override
    public boolean hasOptionalSettings() {
        return true;
    }

    /**
     * Override this method if the operator requires additional settings. {@inheritDoc}
     * @since 5.5
     *
     */
    @Override
    public UniqueConcatenateSettingsPanel getSettingsPanel() {
        if (m_settingsPanel == null) {
            m_settingsPanel = new UniqueConcatenateSettingsPanel(m_settings);
        }
        return m_settingsPanel;
    }

    @Override
    public void loadValidatedSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsFrom(settings);
    }

    @Override
    public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
        throws NotConfigurableException {
        getSettingsPanel().loadSettingsFrom(settings, spec);
    }

    @Override
    public void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettingsTo(settings);
    }

    @Override
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.validateSettings(settings);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_settings.validate();
    }

    /**
     * {@link JPanel} that allows the user to specify layout mapping settings.
     *
     * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
     */
    private class UniqueConcatenateSettingsPanel extends JPanel {

        private static final long serialVersionUID = 1;

        private DialogComponentNumber m_functionComponent;

        /**
         * @param settings the {@link UniqueConcatenateFuntionSettings} to use
         */
        public UniqueConcatenateSettingsPanel(final UniqueConcatenateFuntionSettings settings) {

            final SettingsModelDouble functionModel = settings.getFunctionModel();

            m_functionComponent = new DialogComponentNumber(functionModel, "Percentile: ", 10);
            m_functionComponent.getComponentPanel();
            add(m_functionComponent.getComponentPanel());

        }

        /**
         * Read value(s) of this dialog component from the configuration object. This method will be called by the
         * dialog pane only.
         *
         * @param settings the <code>NodeSettings</code> to read from
         * @param spec the input {@link DataTableSpec}
         * @throws NotConfigurableException If there is no chance for the dialog component to be valid (i.e. the
         *             settings are valid), e.g. if the given spec lacks some important columns or column types.
         */
        public void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec spec)
            throws NotConfigurableException {
            final DataTableSpec[] specs = new DataTableSpec[]{spec};
            m_functionComponent.loadSettingsFrom(settings, specs);
        }
    }

    /**
     * Class that save the settings of the {@link UniqueConcatenateSettingsPanel}.
     *
     * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
     */
    private class UniqueConcatenateFuntionSettings {

        private static final String CFG_CUSTOM_PERCENTILE = "customPercentile";

        private static final double MIN_VALUE = 0;

        private static final double MAX_VALUE = 100;

        /** The default custom function. */
        public static final double DEFAULT_PERCENTILE = 50;

        private final SettingsModelDouble m_function;

        /**
         * Constructor.
         */
        public UniqueConcatenateFuntionSettings() {
            this(DEFAULT_PERCENTILE);
        }

        /**
         * @throws InvalidSettingsException
         *
         */
        public void validate() throws InvalidSettingsException {
            final double val = m_function.getDoubleValue();
            checkBoundary(val);
        }

        /**
         * @param val
         * @throws InvalidSettingsException
         */
        private void checkBoundary(final double val) throws InvalidSettingsException {
            if (val <= MIN_VALUE || val >= MAX_VALUE) {
                throw new InvalidSettingsException("Percentile must be greater than 0 and less than 100");
            }
        }

        /**
         * @param percentile
         */
        public void setPercentile(final double percentile) {
            m_function.setDoubleValue(percentile);
        }

        private UniqueConcatenateFuntionSettings(final double percentile) {
            m_function = new SettingsModelDouble(CFG_CUSTOM_PERCENTILE, percentile);
        }

        /**
         * @return the separator model
         */
        SettingsModelDouble getFunctionModel() {
            return m_function;
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            final double val =
                ((SettingsModelDouble)m_function.createCloneWithValidatedValue(settings)).getDoubleValue();
            checkBoundary(val);
        }

        /**
         * @param settings the {@link NodeSettingsRO} to read the settings from
         * @throws InvalidSettingsException if the settings are invalid
         */
        public void loadSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
            m_function.loadSettingsFrom(settings);
        }

        /**
         * @param settings the {@link NodeSettingsWO} to write to
         */
        public void saveSettingsTo(final NodeSettingsWO settings) {
            m_function.saveSettingsTo(settings);
        }

    }
}
