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
 * ---------------------------------------------------------------------
 *
 * Created on Nov 28, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
 */
package org.knime.base.node.meta.looper;

import java.util.function.Supplier;

import org.knime.base.node.meta.looper.AbstractLoopEndNodeSettings.RowKeyPolicy;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;

/**
 * Settings for the web UI dialog of the Loop End (Dynamic) node.
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author Carsten Haubold, KNIME GmbH, Konstanz, Germany
 * @since 4.5
 */
final class LoopEndDynamicNodeWebUISettings implements NodeParameters {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    LoopEndDynamicNodeWebUISettings() {
    }

    /**
     * Constructor that receives context to set up defaults.
     */
    LoopEndDynamicNodeWebUISettings(final NodeParametersInput context) {
        // Initialize table settings array based on number of input ports
        final int numberOfPorts = context.getInPortTypes().length;
        m_tableSettings = new TableSettings[numberOfPorts];
        for (int i = 0; i < numberOfPorts; i++) {
            m_tableSettings[i] = new TableSettings();
        }
    }

    @Section(title = "General Settings")
    interface GeneralSection {
    }

    @Section(title = "Table Settings")
    @After(GeneralSection.class)
    interface TableSection {
    }

    @Persistor(RowKeyPolicyPersistor.class)
    @Widget(title = "Row ID policy", description = "Specifies how to deal with the RowIDs for each table.")
    @RadioButtonsWidget
    RowKeyPolicyOption m_rowKeyPolicy = RowKeyPolicyOption.APPEND_SUFFIX;

    enum RowKeyPolicyOption {
            @Label(value = "Generate new RowIDs", description = "RowIDs are newly generated (Row0, Row1, ...)")
            GENERATE_NEW,

            @Label(value = "Unique RowIDs by appending a suffix", description = """
                    The iteration number is added to each RowID from the incoming table,
                    thus making the RowIDs unique over all iterations""")
            APPEND_SUFFIX,

            @Label(value = "Leave RowIDs unmodified", description = """
                    The incoming RowIDs are not altered. In this case you have
                    to make sure that there are not duplicate RowIDs in different iterations.
                    Otherwise an error occurs.
                    """)
            UNMODIFIED;
    }

    @Layout(GeneralSection.class)
    @Widget(title = "Add iteration column",
        description = "Allows you to add a column containing the iteration number to the output tables.")
    boolean m_addIterationColumn = true;

    @Layout(GeneralSection.class)
    @Widget(title = "Propagate modified loop variables",
        description = "If checked, variables whose values are modified within the loop are exported by this node. "
            + "These variables must be declared outside the loop, i.e. injected into the loop from a side-branch "
            + "or be available upstream of the corresponding loop start node. For the latter, any modification of "
            + "a variable is passed back to the start node in subsequent iterations (e.g. moving sum calculation). "
            + "Note that variables defined by the loop start node itself are excluded as these usually represent "
            + "loop controls (e.g. \"currentIteration\").")
    boolean m_propagateLoopVariables;

    @Layout(TableSection.class)
    @Widget(title = "Table Settings",
        description = "Settings for each input table port. Configure how to handle empty tables, column types, "
            + "and changing table specifications for each port.")
    @ArrayWidget(addButtonText = "Add Port", elementTitle = "Port", showSortButtons = false, hasFixedSize = true)
    @Persistor(TableSettingsPersistor.class)
    @ValueReference(TableSettingsReference.class)
    @ValueProvider(TableSettingsProvider.class)
    TableSettings[] m_tableSettings = new TableSettings[0];

    static final class TableSettings implements NodeParameters {
        @Widget(title = "Ignore empty input tables",
            description = "If this option is checked, empty input tables and their structures are ignored "
                + "and will not cause the node to fail.")
        boolean m_ignoreEmptyTables = true;

        @Widget(title = "Allow variable column types",
            description = "If checked, the loop does not fail when the column types between different table "
                + "iterations change. The resulting column will have the common super type of the different "
                + "column types.")
        boolean m_tolerateColumnTypes;

        @Widget(title = "Allow changing table specifications",
            description = "If checked, the table specifications between iterations can differ. If columns have "
                + "been added or removed between iterations, missing values are inserted accordingly in the "
                + "result table. If not checked and the table specifications differ, the node will fail.")
        boolean m_tolerateChangingSpecs;
    }

    private static final class RowKeyPolicyPersistor implements NodeParametersPersistor<RowKeyPolicyOption> {

        @Override
        public RowKeyPolicyOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // Load from legacy AbstractLoopEndNodeSettings format
            final String policyName = settings.getString("rowKeyPolicy", RowKeyPolicy.APPEND_SUFFIX.name());
            final RowKeyPolicy policy = RowKeyPolicy.valueOf(policyName);

            return switch (policy) {
                case GENERATE_NEW -> RowKeyPolicyOption.GENERATE_NEW;
                case APPEND_SUFFIX -> RowKeyPolicyOption.APPEND_SUFFIX;
                case UNMODIFIED -> RowKeyPolicyOption.UNMODIFIED;
            };
        }

        @Override
        public void save(final RowKeyPolicyOption value, final NodeSettingsWO settings) {
            final RowKeyPolicy policy = switch (value) {
                case GENERATE_NEW -> RowKeyPolicy.GENERATE_NEW;
                case APPEND_SUFFIX -> RowKeyPolicy.APPEND_SUFFIX;
                case UNMODIFIED -> RowKeyPolicy.UNMODIFIED;
            };
            settings.addString("rowKeyPolicy", policy.name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"rowKeyPolicy"}};
        }
    }

    private static final class TableSettingsPersistor implements NodeParametersPersistor<TableSettings[]> {

        @Override
        public TableSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // Load from the existing dynamic settings format
            final boolean[] ignoreEmptyTables = settings.getBooleanArray("ignoreEmptyTables", new boolean[0]);
            final boolean[] tolerateColumnTypes = settings.getBooleanArray("tolerateColumnTypes", new boolean[0]);
            final boolean[] tolerateChangingSpecs = settings.getBooleanArray("tolerateChangingSpecs", new boolean[0]);

            // Determine the number of ports based on the longest array
            final int numberOfPorts =
                Math.max(Math.max(ignoreEmptyTables.length, tolerateColumnTypes.length), tolerateChangingSpecs.length);

            if (numberOfPorts == 0) {
                return new TableSettings[0];
            }

            final TableSettings[] tableSettings = new TableSettings[numberOfPorts];
            for (int i = 0; i < numberOfPorts; i++) {
                final TableSettings ts = new TableSettings();

                // Use the value if available, otherwise default values
                ts.m_ignoreEmptyTables = i >= ignoreEmptyTables.length || ignoreEmptyTables[i];
                ts.m_tolerateColumnTypes = i < tolerateColumnTypes.length && tolerateColumnTypes[i];
                ts.m_tolerateChangingSpecs = i < tolerateChangingSpecs.length && tolerateChangingSpecs[i];

                tableSettings[i] = ts;
            }

            return tableSettings;
        }

        @Override
        public void save(final TableSettings[] value, final NodeSettingsWO settings) {
            if (value == null || value.length == 0) {
                settings.addBooleanArray("ignoreEmptyTables", new boolean[0]);
                settings.addBooleanArray("tolerateColumnTypes", new boolean[0]);
                settings.addBooleanArray("tolerateChangingSpecs", new boolean[0]);
                return;
            }

            final boolean[] ignoreEmptyTables = new boolean[value.length];
            final boolean[] tolerateColumnTypes = new boolean[value.length];
            final boolean[] tolerateChangingSpecs = new boolean[value.length];

            for (int i = 0; i < value.length; i++) {
                final TableSettings tableSetting = value[i];
                ignoreEmptyTables[i] = tableSetting == null || tableSetting.m_ignoreEmptyTables;
                tolerateColumnTypes[i] = tableSetting != null && tableSetting.m_tolerateColumnTypes;
                tolerateChangingSpecs[i] = tableSetting != null && tableSetting.m_tolerateChangingSpecs;
            }

            settings.addBooleanArray("ignoreEmptyTables", ignoreEmptyTables);
            settings.addBooleanArray("tolerateColumnTypes", tolerateColumnTypes);
            settings.addBooleanArray("tolerateChangingSpecs", tolerateChangingSpecs);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{"ignoreEmptyTables"}, {"tolerateColumnTypes"}, {"tolerateChangingSpecs"}};
        }
    }

    static final class TableSettingsReference implements ParameterReference<TableSettings[]> {
    }

    static final class TableSettingsProvider implements StateProvider<TableSettings[]> {

        Supplier<TableSettings[]> m_supplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_supplier = initializer.getValueSupplier(TableSettingsReference.class);
        }

        @Override
        public TableSettings[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            var numInputPorts = parametersInput.getInPortSpecs().length;
            var currentSettings = m_supplier.get();

            if (currentSettings.length == numInputPorts) {
                return currentSettings;
            }

            var newSettings = new TableSettings[numInputPorts];

            var numCurrentSettings = Math.min(currentSettings.length, numInputPorts);
            System.arraycopy(currentSettings, 0, newSettings, 0, numCurrentSettings);

            for (int i = numCurrentSettings; i < numInputPorts; i++) {
                newSettings[i] = new TableSettings();
            }

            return newSettings;
        }

    }
}
