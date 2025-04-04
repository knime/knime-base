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
 *   Mar 21, 2025 (paulbaernreuther): created
 */
package org.knime.base.node.preproc.filter.rowref;

import static org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice.ROW_ID;

import java.util.List;
import java.util.stream.Stream;

import org.knime.core.node.defaultnodesettings.SettingsModelColumnName;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migrate;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelColumnNameMigration;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.RowIDChoice;
import org.knime.core.webui.node.dialog.defaultdialog.setting.singleselection.StringOrEnum;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;

/**
 * {@link DefaultNodeSettings} implementation for the Reference Row Filter to auto-generate a Web-UI based dialog. Note
 * that this class is only used for the dialog generation and not by the node model.
 *
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
class AbstractRowFilterRefNodeSettings implements DefaultNodeSettings {

    @Before(UpdateDomainsLayout.class)
    interface ColumnsLayout {
    }

    interface UpdateDomainsLayout {
    }

    private interface DataColumn extends Modification.Reference {
    }

    static class AdjustDataColumnDescriptionsForSplitter implements Modifier {

        @Override
        public void modify(final WidgetGroupModifier group) {
            group.find(DataColumn.class).modifyAnnotation(Widget.class).withProperty("description",
                "The column from the table to be split that should be used for comparison.").modify();
        }

    }

    @Widget(title = "Data column (in first input)",
        description = "The column from the table to be filtered that should be used for comparison.")
    @ChoicesProvider(DataColumnChoices.class)
    @Modification.WidgetReference(DataColumn.class)
    @Migration(DataColumnMigration.class)
    @Persist(configKey = "dataTableColumnV2")
    @Layout(ColumnsLayout.class)
    StringOrEnum<RowIDChoice> m_dataColumn = new StringOrEnum<>(ROW_ID);

    static final class DataColumnChoices extends AllColumnsProvider {

    }

    static final class DataColumnMigration extends ColumnMigration {
        DataColumnMigration() {
            super("dataTableColumn");
        }

    }

    @Widget(title = "Reference column (in second input)",
        description = "The column from the filter table that should be used for comparison.")
    @ChoicesProvider(ReferenceColumnChoices.class)
    @Migration(ReferenceColumnMigration.class)
    @Persist(configKey = "referenceTableColumnV2")
    @Layout(ColumnsLayout.class)
    StringOrEnum<RowIDChoice> m_referenceColumn = new StringOrEnum<>(ROW_ID);

    static final class ReferenceColumnChoices extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    static final class ReferenceColumnMigration extends ColumnMigration {
        ReferenceColumnMigration() {
            super("referenceTableColumn");
        }

    }

    @Widget( //
        title = "Update domains of all columns", //
        description = "Advanced setting to enable recomputation of the domains of all columns in the output table " //
            + "such that the domains' bounds exactly match the bounds of the data in the output table.", //
        advanced = true)
    @Migrate(loadDefaultIfAbsent = true)
    @Layout(UpdateDomainsLayout.class)
    boolean m_updateDomains;

    /**
     * Loads columns from either the time these were saved as {@link SettingsModelColumnName} or were not present at
     * all.
     */
    static abstract class ColumnMigration extends SettingsModelColumnNameMigration {
        /**
         * @param legacyConfigKey the config key by which the setting was saved as a {@link SettingsModelColumnName}
         *            before
         */
        protected ColumnMigration(final String legacyConfigKey) {
            super(legacyConfigKey);
        }

        @Override
        public List<ConfigMigration<StringOrEnum<RowIDChoice>>> getConfigMigrations() {
            return Stream.concat(//
                super.getConfigMigrations().stream(),
                //  Setting has been introduces with KNIME 2.0, before that the row id was used
                Stream.of(ConfigMigration.builder((settings) -> new StringOrEnum<>(ROW_ID)).build())//
            ).toList();
        }

    }

}
