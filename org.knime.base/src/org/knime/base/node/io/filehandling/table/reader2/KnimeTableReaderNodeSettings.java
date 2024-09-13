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

import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.ColumnAndDataTypeDetection.IfSchemaChanges;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.DataArea.FirstColumnContainsRowIds;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.DataArea.LimitNumberOfRows;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.DataArea.MaximumNumberOfRows;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.DataArea.SkipFirstDataRows;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.File.Source;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.MultipleFileHandling.AppendFilePathColumn;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.MultipleFileHandling.FilePathColumnName;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.MultipleFileHandling.HowToCombineColumns;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.MultipleFileHandling.PrependFileIndexToRowId;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeLayout.Transformation;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;

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
    @Layout(Transformation.class)
    KnimeTableReaderTransformationSettings m_tableSpecConfig = new KnimeTableReaderTransformationSettings();

    static class Settings implements WidgetGroup, PersistableSettings {

        static final class FileChooserRef extends ReferenceStateProvider<FileChooser> {
        }

        @Widget(title = "Source", description = Source.DESCRIPTION)
        @ValueReference(FileChooserRef.class)
        @Layout(Source.class)
        @Persist(configKey = "file_selection", settingsModel = SettingsModelReaderFileChooser.class)
        @FileReaderWidget(fileExtensions = {"table"}) // ??? different to CSV reader
        FileChooser m_source = new FileChooser();

        @Persist(configKey = "file_selection", hidden = true)
        FileSelectionInternal m_fileSelectionInternal = new FileSelectionInternal();

        static class FileSelectionInternal implements WidgetGroup, PersistableSettings {
            @Persist(configKey = "SettingsModelID")
            String m_settingsModelID = "SMID_ReaderFileChooser";

            @Persist(configKey = "EnabledStatus")
            boolean m_enabledStatus = true;
        }

        static class FirstColumnContainsRowIdsRef extends ReferenceStateProvider<Boolean> {
        }

        @Widget(title = "First column contains RowIDs", description = FirstColumnContainsRowIds.DESCRIPTION)
        @ValueReference(FirstColumnContainsRowIdsRef.class)
        @Layout(FirstColumnContainsRowIds.class)
        @Persist(configKey = "has_row_id")
        boolean m_firstColumnContainsRowIds;

        @Widget(title = "Prepend file index to RowID", description = PrependFileIndexToRowId.DESCRIPTION)
        @Layout(PrependFileIndexToRowId.class)
        @Persist(configKey = "prepend_table_index_to_row_id") // ??? this key is different to the CSV reader
        boolean m_prependFileIndexToRowId;
        // TODO this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    }

    static class AdvancedSettings implements WidgetGroup, PersistableSettings {

        enum HowToCombineColumnsOption {
                @Label(value = "Fail if different", description = HowToCombineColumns.DESCRIPTION_FAIL)
                FAIL(ColumnFilterMode.UNION),

                @Label(value = "Union", description = HowToCombineColumns.DESCRIPTION_UNION)
                UNION(ColumnFilterMode.UNION),

                @Label(value = "Intersection", description = HowToCombineColumns.DESCRIPTION_INTERSECTION)
                INTERSECTION(ColumnFilterMode.INTERSECTION);

            private final ColumnFilterMode m_columnFilterMode;

            HowToCombineColumnsOption(final ColumnFilterMode columnFilterMode) {
                m_columnFilterMode = columnFilterMode;
            }

            ColumnFilterMode toColumnFilterMode() {
                return m_columnFilterMode;
            }
        }

        static final class HowToCombineColumnsOptionPersistor
            implements FieldNodeSettingsPersistor<HowToCombineColumnsOption> {

            private static final String CFG_FAIL_ON_DIFFERING_SPECS = "fail_on_differing_specs";

            private static final String CFG_SPEC_MERGE_MODE = "spec_merge_mode";

            @Override
            public HowToCombineColumnsOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                if (settings.getBoolean(CFG_FAIL_ON_DIFFERING_SPECS, true)) {
                    return HowToCombineColumnsOption.FAIL;
                }
                if (settings.getString(CFG_SPEC_MERGE_MODE, "UNION").equals("UNION")) {
                    return HowToCombineColumnsOption.UNION;
                }
                return HowToCombineColumnsOption.INTERSECTION;
            }

            @Override
            public void save(final HowToCombineColumnsOption howToCombineColumnsOption, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_FAIL_ON_DIFFERING_SPECS,
                    howToCombineColumnsOption == HowToCombineColumnsOption.FAIL);
                settings.addString(CFG_SPEC_MERGE_MODE, howToCombineColumnsOption.toColumnFilterMode().name());
            }

            @Override
            public String[] getConfigKeys() {
                return new String[]{CFG_FAIL_ON_DIFFERING_SPECS, CFG_SPEC_MERGE_MODE};
            }
        }

        static class HowToCombineColumnsOptionRef implements Reference<HowToCombineColumnsOption> {
        }

        @Widget(title = "How to combine columns", description = HowToCombineColumns.DESCRIPTION)
        @ValueSwitchWidget
        @ValueReference(HowToCombineColumnsOptionRef.class)
        @Layout(HowToCombineColumns.class)
        @Persist(customPersistor = HowToCombineColumnsOptionPersistor.class)
        HowToCombineColumnsOption m_howToCombineColumns = HowToCombineColumnsOption.FAIL;
        // TODO this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

        static class AppendPathColumnRef extends ReferenceStateProvider<Boolean> {
        }

        static final class AppendPathColumn implements PredicateProvider {

            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getBoolean(AppendPathColumnRef.class).isTrue();
            }

        }

        @Widget(title = "Append file path column", description = AppendFilePathColumn.DESCRIPTION)
        @ValueReference(AppendPathColumnRef.class)
        @Layout(AppendFilePathColumn.class)
        @Persist(configKey = "append_path_column", hidden = true)
        boolean m_appendPathColumn;

        static class FilePathColumnNameRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "File path column name", description = FilePathColumnName.DESCRIPTION)
        @ValueReference(FilePathColumnNameRef.class)
        @Layout(FilePathColumnName.class)
        @Effect(predicate = AppendPathColumn.class, type = EffectType.SHOW)
        @Persist(configKey = "path_column_name", hidden = true)
        String m_filePathColumnName = "File Path";

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

        @Widget(title = "Skip first data rows", description = SkipFirstDataRows.DESCRIPTION) // ??? these widgets live in LimitRows in the CSV reader
        @ValueReference(SkipFirstDataRowsRef.class)
        @NumberInputWidget(min = 0)
        @Layout(SkipFirstDataRows.class)
        @Persist(customPersistor = SkipFirstDataRowsPersistor.class)
        long m_skipFirstDataRows;

        interface LimitNumberOfRowsRef extends Reference<Boolean> {
        }

        static final class LimitNumberOfRowsPredicate implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getBoolean(LimitNumberOfRowsRef.class).isTrue();
            }
        }

        @Widget(title = "Limit number of rows", description = LimitNumberOfRows.DESCRIPTION, advanced = true)
        @Layout(LimitNumberOfRows.class)
        @ValueReference(LimitNumberOfRowsRef.class)
        @Persist(configKey = "limit_data_rows")
        boolean m_limitNumberOfRows;
        // TODO merge into a single widget with UIEXT-1742

        @Widget(title = "Maximum number of rows", description = MaximumNumberOfRows.DESCRIPTION)
        @NumberInputWidget(min = 0)
        @Layout(MaximumNumberOfRows.class)
        @Effect(predicate = LimitNumberOfRowsPredicate.class, type = EffectType.SHOW)
        @Persist(configKey = "max_rows")
        long m_maximumNumberOfRows = 50;

        enum IfSchemaChangesOption {
                @Label(value = "Fail", description = IfSchemaChanges.DESCRIPTION_FAIL) //
                FAIL, //
                @Label(value = "Use new schema", description = IfSchemaChanges.DESCRIPTION_USE_NEW_SCHEMA) //
                USE_NEW_SCHEMA, //
                @Label(value = "Ignore (deprecated)", description = IfSchemaChanges.DESCRIPTION_IGNORE, disabled = true)
                IGNORE; //
        }

        static final class IfSchemaChangesPersistor implements FieldNodeSettingsPersistor<IfSchemaChangesOption> {

            // ??? in the csv reader, this is not hidden / internal
            private static final String CFG_SAVE_TABLE_SPEC_CONFIG =
                "save_table_spec_config" + SettingsModel.CFGKEY_INTERNAL;

            private static final String CFG_CHECK_TABLE_SPEC = "check_table_spec";

            @Override
            public IfSchemaChangesOption load(final NodeSettingsRO settings) throws InvalidSettingsException {
                final var saveTableSpecConfig = settings.getBoolean(CFG_SAVE_TABLE_SPEC_CONFIG, true);
                if (saveTableSpecConfig) {
                    if (settings.getBoolean(CFG_CHECK_TABLE_SPEC, false)) {
                        return IfSchemaChangesOption.FAIL;
                    } else {
                        return IfSchemaChangesOption.IGNORE;
                    }
                }
                return IfSchemaChangesOption.USE_NEW_SCHEMA;
            }

            @Override
            public void save(final IfSchemaChangesOption ifSchemaChangesOption, final NodeSettingsWO settings) {
                settings.addBoolean(CFG_SAVE_TABLE_SPEC_CONFIG,
                    ifSchemaChangesOption != IfSchemaChangesOption.USE_NEW_SCHEMA);
                settings.addBoolean(CFG_CHECK_TABLE_SPEC, ifSchemaChangesOption == IfSchemaChangesOption.FAIL);
            }

            @Override
            public String[] getConfigKeys() {
                return new String[]{CFG_SAVE_TABLE_SPEC_CONFIG, CFG_CHECK_TABLE_SPEC};
            }
        }

        static final class IfSchemaChangesOptionRef implements Reference<IfSchemaChangesOption> {
        }

        static final class UseNewSchema implements PredicateProvider {
            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getEnum(IfSchemaChangesOptionRef.class).isOneOf(IfSchemaChangesOption.USE_NEW_SCHEMA);
            }
        }

        @Widget(title = "If schema changes", description = IfSchemaChanges.DESCRIPTION)
        @RadioButtonsWidget
        @Layout(IfSchemaChanges.class)
        @Persist(customPersistor = IfSchemaChangesPersistor.class)
        @ValueReference(IfSchemaChangesOptionRef.class)
        IfSchemaChangesOption m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;
    }
}
