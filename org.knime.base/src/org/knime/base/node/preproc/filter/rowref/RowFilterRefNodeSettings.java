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
 *   Jan 9, 2023 (benjamin): created
 */
package org.knime.base.node.preproc.filter.rowref;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * {@link DefaultNodeSettings} implementation for the Reference Row Filter to auto-generate a Web-UI based dialog. Note
 * that this class is only used for the dialog generation and not by the node model.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class RowFilterRefNodeSettings implements DefaultNodeSettings {

    // TODO: UIEXT-1007 migrate String to ColumnSelection

    @Persist(settingsModel = SettingsModelColumnName.class, configKey = "dataTableColumn")
    @Widget(title = "Data column",
        description = "The column from the table to be filtered that should be used for comparison.")
    @ChoicesWidget(choices = DataColumnChoices.class, showRowKeys = true)
    String m_dataColumn = "<row-keys>";

    @Persist(settingsModel = SettingsModelColumnName.class, configKey = "referenceTableColumn")
    @Widget(title = "Reference column",
        description = "The column from the filter table that should be used for comparison.")
    @ChoicesWidget(choices = ReferenceColumnChoices.class, showRowKeys = true)
    String m_referenceColumn = "<row-keys>";

    @Persist(customPersistor = IncludeOrExcludeRowsPersistor.class)
    @Widget(title = "Include or exclude rows from the reference table",
        description = "Includes or excludes all rows from the reference table in the resulting table from the first "
            + "input.")
    @ValueSwitchWidget
    IncludeOrExcludeRows m_inexclude = IncludeOrExcludeRows.INCLUDE;

    @Persist(settingsModel = SettingsModelBoolean.class)
    @Widget( //
        title = "Update domains of all columns", //
        description = "Advanced setting to enable recomputation of the domains of all columns in the output table " //
            + "such that the domains' bounds exactly match the bounds of the data in the output table.", //
        advanced = true)
    boolean m_updateDomains;

    enum IncludeOrExcludeRows {
            @Label("Include")
            INCLUDE,

            @Label("Exclude")
            EXCLUDE;
    }

    private abstract static class AllColumnChoices implements ChoicesProvider {

        private final int m_portIdx;

        public AllColumnChoices(final int portIdx) {
            m_portIdx = portIdx;
        }

        @Override
        public String[] choices(final SettingsCreationContext context) {
            final DataTableSpec specs = context.getDataTableSpecs()[m_portIdx];
            return specs != null ? specs.getColumnNames() : new String[0];
        }
    }

    private static final class DataColumnChoices extends AllColumnChoices {
        public DataColumnChoices() {
            super(0);
        }
    }

    private static final class ReferenceColumnChoices extends AllColumnChoices {
        public ReferenceColumnChoices() {
            super(1);
        }
    }

    private static final class IncludeOrExcludeRowsPersistor
        implements FieldNodeSettingsPersistor<IncludeOrExcludeRows> {

        private static final String KEY_INCLUDE_EXCLUDE = "inexclude";

        @Override
        public IncludeOrExcludeRows load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return RowFilterRefNodeDialogPane.INCLUDE.equals(settings.getString(KEY_INCLUDE_EXCLUDE)) //
                ? IncludeOrExcludeRows.INCLUDE //
                : IncludeOrExcludeRows.EXCLUDE;
        }

        @Override
        public void save(final IncludeOrExcludeRows obj, final NodeSettingsWO settings) {
            settings.addString(KEY_INCLUDE_EXCLUDE, //
                obj == IncludeOrExcludeRows.INCLUDE //
                    ? RowFilterRefNodeDialogPane.INCLUDE //
                    : RowFilterRefNodeDialogPane.EXCLUDE //
            );
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{KEY_INCLUDE_EXCLUDE};
        }
    }
}
