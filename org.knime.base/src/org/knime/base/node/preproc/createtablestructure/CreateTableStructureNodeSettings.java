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
 *   Jan 29, 2025 (Martin Sillye, TNG Technology Consulting GmbH): created
 */
package org.knime.base.node.preproc.createtablestructure;

import java.util.List;
import java.util.function.Supplier;

import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell.DoubleCellFactory;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.webui.node.dialog.configmapping.ConfigMigration;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Migration;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsMigration;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.StateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * The settings for the "Table Structure Creator" node.
 *
 * @author Martin Sillye, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
final class CreateTableStructureNodeSettings implements DefaultNodeSettings {

    @Widget(title = "Columns", description = "")
    @ArrayWidget(addButtonText = "Add new column", elementTitle = "Column", showSortButtons = true,
        elementDefaultValueProvider = DefaultColumnSettingsProvider.class)
    @Migration(value = ColumnSettingsMigration.class)
    @ValueReference(Ref.class)
    ColumnSettings[] m_columnSettings = new ColumnSettings[]{new ColumnSettings("Column 1", StringCellFactory.TYPE)};

    static final class ColumnSettings implements DefaultNodeSettings {

        ColumnSettings() {
        }

        ColumnSettings(final String columnName, final DataType colType) {
            this.m_columnName = columnName;
            this.m_colType = colType;
        }

        @Widget(title = "Column name ", description = "Name of the created column")
        @TextInputWidget(minLength = 1)
        String m_columnName = "Column ";

        @Widget(title = "Column type", description = "Type of the created column")
        DataType m_colType = StringCellFactory.TYPE;
    }

    static final class DefaultColumnSettingsProvider implements StateProvider<ColumnSettings> {

        private Supplier<ColumnSettings[]> m_settings;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_settings = initializer.computeFromValueSupplier(Ref.class);
        }

        @Override
        public ColumnSettings computeState(final DefaultNodeSettingsContext context) {
            final String name = "Column " + (this.m_settings.get().length + 1);
            return new ColumnSettings(name, StringCellFactory.TYPE);
        }

    }

    interface Ref extends Reference<ColumnSettings[]> {

    }

    static final class ColumnSettingsMigration implements NodeSettingsMigration<ColumnSettings[]> {

        private static final String COUNT_KEY = "colCount";

        private static final String PREFIX_KEY = "colPrefix";

        private static final String TYPE_KEY = "colType";

        private static DataType createDataTypeFromName(final String name) {
            return switch (name) {
                case "String" -> StringCellFactory.TYPE;
                case "Integer" -> IntCellFactory.TYPE;
                case "Double" -> DoubleCellFactory.TYPE;
                default -> StringCellFactory.TYPE;
            };
        }

        static ColumnSettings[] load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var colCount = settings.getInt(COUNT_KEY);
            CheckUtils.checkSetting(colCount >= 0, "Invalid (negativ) column count");
            final var colPrefix = settings.getString(PREFIX_KEY);
            CheckUtils.checkSetting(colPrefix != null && !colPrefix.trim().isEmpty(), "Invalid (empty) prefix");
            final var colType = settings.getString(TYPE_KEY);
            CheckUtils.checkSetting("String".equals(colType) || "Integer".equals(colType) || "Double".equals(colType),
                "Invalid type: '" + colType + "'");
            final var columns = new ColumnSettings[colCount];
            for (int i = 0; i < colCount; i++) {
                columns[i] = new ColumnSettings(colPrefix + i, createDataTypeFromName(colType));
            }
            return columns;
        }

        @Override
        public List<ConfigMigration<ColumnSettings[]>> getConfigMigrations() {
            return List.of(//
                ConfigMigration.builder(ColumnSettingsMigration::load).withDeprecatedConfigPath(COUNT_KEY)//
                    .withDeprecatedConfigPath(PREFIX_KEY)//
                    .withDeprecatedConfigPath(TYPE_KEY)//
                    .build());
        }

    }

}
