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
 *   Sep 17, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.UseExistingRowId;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.LegacyReaderFilerChooserPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"javadoc", "restriction"})
public final class CommonReaderNodeSettings {

    public static class Settings implements WidgetGroup, PersistableSettings {

        public static final class FileChooserRef extends ReferenceStateProvider<FileChooser>
            implements Modification.Reference {
        }

        public static abstract class SetFileReaderWidgetExtensions implements WidgetGroup.Modifier {
            @Override
            public void modify(final WidgetGroupModifier group) {
                group.find(FileChooserRef.class).modifyAnnotation(FileReaderWidget.class)
                    .withProperty("fileExtensions", getExtensions()).modify();
            }

            protected abstract String[] getExtensions();
        }

        @Widget(title = "Source", description = CommonReaderLayout.File.Source.DESCRIPTION)
        @ValueReference(FileChooserRef.class)
        @Layout(CommonReaderLayout.File.Source.class)
        @Persist(configKey = "file_selection", customPersistor = LegacyReaderFilerChooserPersistor.class)
        @Modification.WidgetReference(FileChooserRef.class)
        @FileReaderWidget()
        public FileChooser m_source = new FileChooser(); // TODO should not be public

        @Persist(configKey = "file_selection", hidden = true)
        FileSelectionInternal m_fileSelectionInternal = new FileSelectionInternal();

        static class FileSelectionInternal implements WidgetGroup, PersistableSettings {
            @Persist(configKey = "SettingsModelID")
            String m_settingsModelID = "SMID_ReaderFileChooser";

            @Persist(configKey = "EnabledStatus")
            boolean m_enabledStatus = true;
        }

        public static class FirstColumnContainsRowIdsRef extends ReferenceStateProvider<Boolean> {
        }

        public static class UseExistingRowIdWidgetRef implements Modification.Reference {
        }

        @Widget(title = "Use existing RowID", description = UseExistingRowId.DESCRIPTION)
        @ValueReference(FirstColumnContainsRowIdsRef.class)
        @Layout(UseExistingRowId.class)
        @Persist(configKey = "has_row_id")
        @Modification.WidgetReference(UseExistingRowIdWidgetRef.class)
        public boolean m_firstColumnContainsRowIds;
    }

    public static class AdvancedSettings implements WidgetGroup, PersistableSettings {
        public enum HowToCombineColumnsOption {
                @Label(value = "Fail if different",
                    description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_FAIL)
                FAIL(ColumnFilterMode.UNION),

                @Label(value = "Union",
                    description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_UNION)
                UNION(ColumnFilterMode.UNION),

                @Label(value = "Intersection",
                    description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_INTERSECTION)
                INTERSECTION(ColumnFilterMode.INTERSECTION);

            private final ColumnFilterMode m_columnFilterMode;

            HowToCombineColumnsOption(final ColumnFilterMode columnFilterMode) {
                m_columnFilterMode = columnFilterMode;
            }

            /**
             * @return the corresponding ColumnFilterMode
             */
            public ColumnFilterMode toColumnFilterMode() {
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

        public static class HowToCombineColumnsOptionRef implements Reference<HowToCombineColumnsOption> {
        }

        @Widget(title = "How to combine columns",
            description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION)
        @ValueSwitchWidget
        @ValueReference(HowToCombineColumnsOptionRef.class)
        @Layout(CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.class)
        @Persist(customPersistor = HowToCombineColumnsOptionPersistor.class)
        public HowToCombineColumnsOption m_howToCombineColumns = HowToCombineColumnsOption.FAIL;
        // TODO this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

        public static class AppendPathColumnRef extends ReferenceStateProvider<Boolean> {
        }

        static final class AppendPathColumn implements PredicateProvider {

            @Override
            public Predicate init(final PredicateInitializer i) {
                return i.getBoolean(AppendPathColumnRef.class).isTrue();
            }

        }

        @Widget(title = "Append file path column",
            description = CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.DESCRIPTION)
        @ValueReference(AppendPathColumnRef.class)
        @Layout(CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.class)
        @Persist(configKey = "append_path_column", hidden = true)
        boolean m_appendPathColumn;

        public static class FilePathColumnNameRef extends ReferenceStateProvider<String> {
        }

        @Widget(title = "File path column name",
            description = CommonReaderLayout.MultipleFileHandling.FilePathColumnName.DESCRIPTION)
        @ValueReference(FilePathColumnNameRef.class)
        @Layout(CommonReaderLayout.MultipleFileHandling.FilePathColumnName.class)
        @Effect(predicate = AppendPathColumn.class, type = EffectType.SHOW)
        @Persist(configKey = "path_column_name", hidden = true)
        String m_filePathColumnName = "File Path";

        enum IfSchemaChangesOption {
                @Label(value = "Fail",
                    description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_FAIL) //
                FAIL, //
                @Label(value = "Use new schema",
                    description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_USE_NEW_SCHEMA) //
                USE_NEW_SCHEMA, //
                @Label(value = "Ignore (deprecated)",
                    description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_IGNORE,
                    disabled = true)
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

        @Widget(title = "If schema changes",
            description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION)
        @RadioButtonsWidget
        @Layout(CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.class)
        @Persist(customPersistor = IfSchemaChangesPersistor.class)
        @ValueReference(IfSchemaChangesOptionRef.class)
        IfSchemaChangesOption m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;
    }

    public static final class SkipFirstDataRowsPersistor implements FieldNodeSettingsPersistor<Long> {

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

    public interface LimitNumberOfRowsRef extends Reference<Boolean> {
    }

    public static final class LimitNumberOfRowsPredicate implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getBoolean(LimitNumberOfRowsRef.class).isTrue();
        }
    }

}
