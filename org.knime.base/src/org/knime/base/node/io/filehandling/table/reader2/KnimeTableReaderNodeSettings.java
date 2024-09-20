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
 *   Sep 9, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.PrependTableIndexToRowId;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeSettings.Settings.SetTableReaderExtensions;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.WidgetModification;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class KnimeTableReaderNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = "settings")
    Settings m_settings = new Settings();

    @Persist(configKey = "advanced_settings")
    AdvancedSettings m_advancedSettings = new AdvancedSettings();

    @Persist(configKey = "table_spec_config", hidden = true,
        customPersistor = KnimeTableReaderTransformationSettingsPersistor.class)
    @Layout(CommonReaderLayout.Transformation.class)
    KnimeTableReaderTransformationSettings m_tableSpecConfig = new KnimeTableReaderTransformationSettings();

    @WidgetModification(SetTableReaderExtensions.class)
    static class Settings extends CommonReaderNodeSettings.Settings {

        static final class SetTableReaderExtensions
            extends CommonReaderNodeSettings.Settings.SetFileReaderWidgetExtensions {

            @Override
            protected String[] getExtensions() {
                return new String[]{"table"};
            }
        }

        @Widget(title = "Prepend table index to RowID", description = PrependTableIndexToRowId.DESCRIPTION)
        @Layout(PrependTableIndexToRowId.class)
        @Persist(configKey = "prepend_table_index_to_row_id") // ??? this key is different to the CSV reader
        boolean m_prependTableIndexToRowId;
        // TODO this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    }

    static class AdvancedSettings extends CommonReaderNodeSettings.AdvancedSettings {

        static final class SkipFirstDataRowsPersistor implements FieldNodeSettingsPersistor<Long> {

            private static final String CFG_SKIP_DATA_ROWS = "skip_data_rows";

            private static final String CFG_NUMBER_OF_DATA_ROWS_TO_SKIP = "number_of_rows_to_skip";

            @Override
            public Long load(final NodeSettingsRO settings) throws InvalidSettingsException {
                return settings.getBoolean(CFG_SKIP_DATA_ROWS) ? settings.getLong(CFG_NUMBER_OF_DATA_ROWS_TO_SKIP) : 0;
            }

            @Override
            public void save(final Long skipFirstDataRows, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_SKIP_DATA_ROWS, skipFirstDataRows > 0);
                settings.addLong(CFG_NUMBER_OF_DATA_ROWS_TO_SKIP, skipFirstDataRows);
            }

            @Override
            public String[] getConfigKeys() {
                return new String[]{CFG_SKIP_DATA_ROWS, CFG_NUMBER_OF_DATA_ROWS_TO_SKIP};
            }
        }

        static class SkipFirstDataRowsRef extends ReferenceStateProvider<Long> {
        }

        @Widget(title = "Skip first data rows", description = CommonReaderLayout.DataArea.SkipFirstDataRows.DESCRIPTION)
        @ValueReference(SkipFirstDataRowsRef.class)
        @NumberInputWidget(min = 0)
        @Layout(CommonReaderLayout.DataArea.SkipFirstDataRows.class)
        @Persist(customPersistor = SkipFirstDataRowsPersistor.class)
        long m_skipFirstDataRows;

        interface LimitNumberOfRowsRef extends Reference<Boolean> {
        }

        static final class LimitNumberOfRowsPredicate implements PredicateProvider { // TODO: Deduplicate at least these predicates and persistors.
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getBoolean(LimitNumberOfRowsRef.class).isTrue();
            }
        }

        @Widget(title = "Limit number of rows", description = CommonReaderLayout.DataArea.LimitNumberOfRows.DESCRIPTION,
            advanced = true)
        @Layout(CommonReaderLayout.DataArea.LimitNumberOfRows.class)
        @ValueReference(LimitNumberOfRowsRef.class)
        @Persist(configKey = "limit_data_rows")
        boolean m_limitNumberOfRows;
        // TODO merge into a single widget with UIEXT-1742

        @Widget(title = "Maximum number of rows",
            description = CommonReaderLayout.DataArea.MaximumNumberOfRows.DESCRIPTION)
        @NumberInputWidget(min = 0)
        @Layout(CommonReaderLayout.DataArea.MaximumNumberOfRows.class)
        @Effect(predicate = LimitNumberOfRowsPredicate.class, type = EffectType.SHOW)
        @Persist(configKey = "max_rows")
        long m_maximumNumberOfRows = 50;

    }
}
