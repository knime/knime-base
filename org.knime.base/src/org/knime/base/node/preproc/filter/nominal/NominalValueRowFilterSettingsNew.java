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
 *   Mar 7, 2024 (jakob): created
 */
package org.knime.base.node.preproc.filter.nominal;

import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.NominalValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldBasedNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyNameFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.NameFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnselection.ColumnSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ColumnChoicesProviderUtil.CompatibleColumnChoicesProvider;

/**
 * WebUI settings class for the "Nomnal Value Row Filter" node.
 *
 * @author Jakob Sanowski, KNIME GmbH, Konstanz
 * @since 5.3
 */
@SuppressWarnings("restriction")
public class NominalValueRowFilterSettingsNew implements DefaultNodeSettings {

    /** Config key for the selected column. */
    static final String CFG_SELECTED_COL = "selected_column";

    @Widget(title = "Select column", description = "Select the (nominal) column which contains the nominal values to filter.")
    @ChoicesWidget(choices = NominalColumnChoicesProider.class)
    @Persist(configKey = CFG_SELECTED_COL, customPersistor = LegacyColumnSelectionPersistor.class)
    public ColumnSelection m_selectedColumn;

    /**
     * Provides columns of type {@link NominalValue} as choices.
     *
     * @author Jakob Sanowski, KNIME GmbH, Konstanz
     */
    private static final class NominalColumnChoicesProider extends CompatibleColumnChoicesProvider {
        /**
         *
         */
        public NominalColumnChoicesProider() {
            // TODO Auto-generated constructor stub
            super(NominalValue.class);
        }
    }

    public static final class LegacyColumnSelectionPersistor extends NodeSettingsPersistorWithConfigKey<ColumnSelection> {

        private final FieldBasedNodeSettingsPersistor<ColumnSelection> m_persistor;

        public LegacyColumnSelectionPersistor(final Class<ColumnSelection> settingsClass) {
            m_persistor = new FieldBasedNodeSettingsPersistor<ColumnSelection>(settingsClass);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public ColumnSelection load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var configKey = getConfigKey();
            if (!settings.containsKey(configKey)) {
                return new ColumnSelection();
            }

            try {
                final var fieldSettingsString = settings.getString(getConfigKey());
                return new ColumnSelection(fieldSettingsString, null);
            } catch (InvalidSettingsException ex) {
                return m_persistor.load(settings.getNodeSettings(getConfigKey()));
            }
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void save(final ColumnSelection obj, final NodeSettingsWO settings) {
            save(obj, settings, getConfigKey());
        }

        @Override
        public String getConfigKey() {
            return super.getConfigKey();
        }

        private void save(ColumnSelection columnSelection, final NodeSettingsWO settings, final String configKey) {
            if (columnSelection == null) {
                // TODO: Log columnSelection = null error.
                columnSelection = new ColumnSelection();
            }

            var columnSelectionSettings = settings.addNodeSettings(configKey);
            settings.addString(configKey, columnSelection.getSelected());
        }
    }

    @Widget(title = "Nominal value filter", description = "Select the nominal values to be in the output data, by moving them "
        + "from left (excluded) to right (included)")
    @ChoicesWidget(choices  = NominalValueChoicesProvider.class)
    @Persist(configKey = NominalValueRowFilterNodeDialog.CFG_CONFIGROOTNAME, customPersistor = LegacyNameFilterPersistor.class)
    public NameFilter m_nominalValueSelection;

    // Temporary. Will be replaced with the ChoicesStateProvider API.
    protected static final class NominalValueChoicesProvider implements ChoicesProvider {
        /**
         * {@inheritDoc}
         */
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            // TODO Auto-generated method stub
            final DataTableSpec specs = context.getDataTableSpec(0).orElse(new DataTableSpec());
            final var values = specs.getColumnSpec(0).getDomain().getValues();
            return values != null ? values.stream().map(c -> c.toString()).toArray(String[]::new) : new String[0];
        }

    }

    public NominalValueRowFilterSettingsNew() {
        this((DataTableSpec)null);
    }

    public NominalValueRowFilterSettingsNew(final DefaultNodeSettingsContext context) {
        this(context.getDataTableSpecs()[0]);
        m_nominalValueSelection = new NameFilter(context);
    }

    public NominalValueRowFilterSettingsNew(final DataTableSpec spec) {
        m_selectedColumn = getFirstCompatibleColumn(spec, NominalValue.class).map(ColumnSelection::new).orElse(new ColumnSelection());
    }

    // Should probably use the functionality of CompatibleColumnsService but this is currently not possible
    // due to it being part of org.knime.base.views. Adding it as dependency would create a dependency cycle.
    private static Optional<DataColumnSpec> getFirstCompatibleColumn(final DataTableSpec spec,
        final Class<? extends DataValue> valueClass) {
        if (spec == null) {
            return Optional.empty();
        }
        var compatibleColumns = spec.stream().filter(s -> s.getType().isCompatible(valueClass)).collect(Collectors.toUnmodifiableList());
        if (!compatibleColumns.isEmpty()) {
            return Optional.of(compatibleColumns.get(0));
        }
        return Optional.empty();
    }
}
