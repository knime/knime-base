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
 *   Jan 22, 2024 (marcel): created
 */
package org.knime.base.node.preproc.rowkey2;

import java.util.List;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.configmapping.ConfigsDeprecation;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.DefaultPersistorWithDeprecations;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.BooleanReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 * Settings for {@link RowKeyNodeModel2}.
 *
 * @author Marcel Wiedenmann, KNIME GmbH, Berlin, Germany
 * @since 5.3
 */
@SuppressWarnings("restriction")
public final class RowKeyNodeSettings implements DefaultNodeSettings {

    private static final String LEGACY_NEW_ROW_KEY_COLUMN_CONFIG_KEY = "newRowKeyColumnName";

    static final class ReplaceRowKey implements BooleanReference {

    }

    @Widget(title = "Replace RowIDs", description = "If selected the RowIDs will be replaced")
    @Layout(ReplaceRowIdsSection.class)
    @ValueReference(ReplaceRowKey.class)
    boolean m_replaceRowKey = true;

    private static final class ReplacementModeRef implements Reference<ReplacementMode> {

    }

    @Persist(customPersistor = ReplacementModePersistor.class)
    @ValueSwitchWidget
    @Widget(title = "Replacement mode",
        description = "Replace the RowID by a newly generated one or the values of a column.")
    @ValueReference(ReplacementModeRef.class)
    @Effect(predicate = ReplaceRowKey.class, type = EffectType.SHOW)
    @Layout(ReplaceRowIdsSection.class)
    ReplacementMode m_replaceRowKeyMode = ReplacementMode.GENERATE_NEW;

    private static final class ReplaceByColumn implements PredicateProvider {

        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getPredicate(ReplaceRowKey.class)
                .and(i.getEnum(ReplacementModeRef.class).isOneOf(ReplacementMode.USE_COLUMN));
        }
    }

    @Persist(customPersistor = NewRowKeyColumnPersistor.class)
    @Widget(title = "ID column", description = "The column to replace the current RowID.")
    @ChoicesWidget(choices = AllColumns.class)
    @Effect(predicate = ReplaceByColumn.class, type = EffectType.SHOW)
    @Layout(ReplaceRowIdsSection.class)
    String m_newRowKeyColumnV2;

    /**
     * Optional, as this setting is not available in older releases.
     */
    @Persist(configKey = "removeRowKeyCol", optional = true)
    @Widget(title = "Remove selected ID column",
        description = "If selected, the column replacing the current RowID is removed from the table.")
    @Effect(predicate = ReplaceByColumn.class, type = EffectType.SHOW)
    @Layout(ReplaceRowIdsSection.class)
    boolean m_removeRowKeyColumn;

    @Persist(customPersistor = HandleMissingValuesModePersistor.class)
    @ValueSwitchWidget
    @Widget(title = "If ID column contains missing values",
        description = "Fail if encountering missing values, or replace them.")
    @Effect(predicate = ReplaceByColumn.class, type = EffectType.SHOW)
    @Layout(ReplaceRowIdsSection.class)
    HandleMissingValuesMode m_handleMissingsMode = HandleMissingValuesMode.FAIL;

    @Persist(customPersistor = HandleDuplicateValuesModePersistor.class)
    @ValueSwitchWidget
    @Widget(title = "If ID column contains duplicates",
        description = "Fail if encountering duplicate values, or make them unique.")
    @Effect(predicate = ReplaceByColumn.class, type = EffectType.SHOW)
    @Layout(ReplaceRowIdsSection.class)
    HandleDuplicateValuesMode m_handleDuplicatesMode = HandleDuplicateValuesMode.FAIL;

    /**
     * Optional, as this option is not available in releases prior to 2.0.3.
     */
    @Persist(configKey = "enableHilite", optional = true)
    @Widget(title = "Enable hiliting", description = """
            If selected, a map is maintained joining the old with the new RowID. Depending on the number of rows,
            enabling this feature might consume a lot of memory.""", advanced = true)
    @Effect(predicate = ReplaceRowKey.class, type = EffectType.SHOW)
    @Layout(ReplaceRowIdsSection.class)
    boolean m_enableHilite;

    static final class AppendRowKey implements BooleanReference {

    }

    @Persist(configKey = "appendRowKeyCol")
    @Widget(title = "Append column with RowID values",
        description = "If selected, a new column with the values of the current RowID is appended to the table.")
    @ValueReference(AppendRowKey.class)
    @Layout(ExtractRowIdsSection.class)
    boolean m_appendRowKey;

    @Persist(configKey = "newColumnName4RowKeyValues")
    @Widget(title = "Column Name", description = "The name of the column to append to the table.")
    @Effect(predicate = AppendRowKey.class, type = EffectType.SHOW)
    @Layout(ExtractRowIdsSection.class)
    String m_appendedColumnName = "Old RowID";

    /**
     * How to replace the RowID column, if at all.
     */
    public enum ReplacementMode {
            /**
             * Generate a new RowID.
             */
            @Label(value = "Generate new",
                description = "If selected, a new RowID is generated with the format: Row0, Row1, Row2, ...")
            GENERATE_NEW,
            /**
             * Replace the RowID by a column.
             */
            @Label(value = "Use column", description = "If selected, the RowID is replaced by the selected column.")
            USE_COLUMN
    }

    /**
     * How to handle missing values in the designated RowID column.
     */
    public enum HandleMissingValuesMode {
            /**
             * Fail node execution.
             */
            @Label(value = "Fail",
                description = "If selected, the node fails if a missing value is encountered in the selected column.")
            FAIL,
            /**
             * Replace missing values.
             */
            @Label(value = "Replace by \"?\"", description = "If selected, missing values are replaced with \"?\"."
                + " We recommend also enabling the\"Append counter\" option to handle any duplicate missing values.")
            REPLACE
    }

    /**
     * How to handle duplicate values in the designated RowID column.
     */
    public enum HandleDuplicateValuesMode {
            /**
             * Fail node execution.
             */
            @Label(value = "Fail",
                description = "If selected, the node fails if a duplicate value is encountered in the selected column.")
            FAIL,
            /**
             * Append a counter to duplicates to make them unique.
             */
            @Label(value = "Append counter",
                description = "If selected, uniqueness is ensured by appending an incrementing number to duplicates.")
            APPEND_COUNTER
    }

    private static final class NewRowKeyColumnPersistor implements DefaultPersistorWithDeprecations<String> {

        @Override
        public List<ConfigsDeprecation<String>> getConfigsDeprecations() {
            return List.of(ConfigsDeprecation.builder(NewRowKeyColumnPersistor::loadLegazy) //
                .withDeprecatedConfigPath(LEGACY_NEW_ROW_KEY_COLUMN_CONFIG_KEY).build());
        }

        private static String loadLegazy(final NodeSettingsRO settings) throws InvalidSettingsException {
            return settings.getString(LEGACY_NEW_ROW_KEY_COLUMN_CONFIG_KEY);
        }
    }

    private static final class ReplacementModePersistor implements DefaultPersistorWithDeprecations<ReplacementMode> {

        @Override
        public List<ConfigsDeprecation<ReplacementMode>> getConfigsDeprecations() {
            return List.of(ConfigsDeprecation.builder(ReplacementModePersistor::loadLegazy)
                .withDeprecatedConfigPath(LEGACY_NEW_ROW_KEY_COLUMN_CONFIG_KEY).build());
        }

        private static ReplacementMode loadLegazy(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getString(LEGACY_NEW_ROW_KEY_COLUMN_CONFIG_KEY) == null) {
                return ReplacementMode.GENERATE_NEW;
            } else {
                return ReplacementMode.USE_COLUMN;
            }
        }

    }

    private static final class HandleMissingValuesModePersistor
        implements FieldNodeSettingsPersistor<HandleMissingValuesMode> {

        private static final String CONFIG_KEY = "replaceMissingValues";

        @Override
        public String[] getConfigKeys() {
            return new String[]{CONFIG_KEY};
        }

        @Override
        public HandleMissingValuesMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(CONFIG_KEY)) {
                return HandleMissingValuesMode.REPLACE;
            } else {
                return HandleMissingValuesMode.FAIL;
            }
        }

        @Override
        public void save(final HandleMissingValuesMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(CONFIG_KEY, obj == HandleMissingValuesMode.REPLACE);
        }
    }

    private static final class HandleDuplicateValuesModePersistor
        implements FieldNodeSettingsPersistor<HandleDuplicateValuesMode> {

        private static final String CONFIG_KEY = "ensureUniqueness";

        @Override
        public String[] getConfigKeys() {
            return new String[]{CONFIG_KEY};
        }

        @Override
        public HandleDuplicateValuesMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.getBoolean(CONFIG_KEY)) {
                return HandleDuplicateValuesMode.APPEND_COUNTER;
            } else {
                return HandleDuplicateValuesMode.FAIL;
            }
        }

        @Override
        public void save(final HandleDuplicateValuesMode obj, final NodeSettingsWO settings) {
            settings.addBoolean(CONFIG_KEY, obj == HandleDuplicateValuesMode.APPEND_COUNTER);
        }
    }

    private static final class AllColumns implements ChoicesProvider {

        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(RowKeyNodeModel2.DATA_IN_PORT) //
                .map(DataTableSpec::getColumnNames) //
                .orElseGet(() -> new String[0]);
        }
    }

    @Section(title = "Replace RowIDs")
    private interface ReplaceRowIdsSection {
    }

    @Section(title = "Extract RowIDs")
    @After(ReplaceRowIdsSection.class)
    private interface ExtractRowIdsSection {
    }
}
