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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.io.filehandling.linereader;

import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_ADVANCED_SETTINGS_TAB;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_APPEND_PATH_COLUMN;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_CHARSET;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_CUSTOM_COL_HEADER;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_EMPTY_LINE_MODE;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_EMPTY_REPLACEMENT;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_ENCODING_TAB;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_FAIL_DIFFERING_SPECS;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_HAS_COL_HEADER;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_LIMIT_DATA_ROWS;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_MAX_ROWS;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_PATH_COLUMN_NAME;
import static org.knime.base.node.io.filehandling.linereader.LineReaderMultiTableReadConfigSerializer.CFG_ROW_HEADER_PREFIX;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.FileEncodingParameters;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.PersistWithin;
import org.knime.filehandling.core.util.SettingsUtils;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyMultiFileSelection;
import org.knime.node.parameters.persistence.legacy.OptionalStringPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.LegacyPredicateInitializer;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;

/**
 * Node parameters for Line Reader.
 *
 * @author Rupert Ettrich, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class LineReaderNodeParameters implements NodeParameters {

    private static final String CFG_USE_REGEX = "use_regex";
    private static final String CFG_REGEX = "regex";

    @Section(title = "Input File")
    interface FileSection {
    }

    @Section(title = "Headers")
    @After(FileSection.class)
    interface HeadersSection {
    }

    @Section(title = "Advanced")
    @After(HeadersSection.class)
    @Advanced
    interface AdvancedSection {
    }

    @Layout(FileSection.class)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = "file_selection")
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
    @ValueReference(FileSelectionRef.class)
    LegacyMultiFileSelection m_fileSelection = new LegacyMultiFileSelection(MultiFileSelectionMode.FILE);

    interface FileSelectionRef extends ParameterReference<LegacyMultiFileSelection> {
    }

    @Persistor(FileEncodingPersistor.class)
    @Advanced
    @PersistWithin(value = {CFG_ENCODING_TAB})
    @Layout(FileSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    FileEncodingParameters m_charSet = new FileEncodingParameters();

    static class FileEncodingPersistor extends FileEncodingParameters.AbstractFileEncodingPersistor {
        public FileEncodingPersistor() {
            super(CFG_CHARSET);
        }
    }

    @Widget(title = "Fail on differing specs", description = """
            If enabled and the specs of the files are different, the node will fail. This option is only
            relevant when reading multiple files in a folder.
            """)
    @Layout(FileSection.class)
    @Effect(predicate = MultiFileSelectionIsActive.class, type = EffectType.SHOW)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = CFG_FAIL_DIFFERING_SPECS)
    boolean m_failOnDifferingSpecs = true;

    private static final class MultiFileSelectionIsActive implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return ((LegacyPredicateInitializer)i).getLegacyMultiFileSelection(FileSelectionRef.class)
                .getSelectionMode().isOneOf(MultiFileSelectionMode.FILES_IN_FOLDERS, MultiFileSelectionMode.FOLDER);
        }
    }

    @Widget(title = "Row header prefix", description = """
            Specifies the prefix of the row header. The default is "Row" and the resulting row headers are
            "Row0", "Row1", and so on.
            """)
    @Layout(HeadersSection.class)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = CFG_ROW_HEADER_PREFIX)
    String m_rowHeaderPrefix = "Row";

    private enum ColumnHeaderMode {
            @Label(value = "Use first line as column header", description = """
                    The name of the output column equals the first row of the read file.
                    """)
            FIRST_LINE_AS_HEADER,
            @Label(value = "Set column header", description = """
                    The name of the output column can be set manually in the text field below. \
                    The default name is 'Column'.
                    """)
            SET_COLUMN_HEADER;
    }

    @Widget(title = "Column header", description = """
            Select how the column header should be determined.
            """)
    @ValueReference(ColumnHeaderModeRef.class)
    @Layout(HeadersSection.class)
    @RadioButtonsWidget
    @Migrate
    @Persistor(ColumnHeaderModePersistor.class)
    ColumnHeaderMode m_columnHeaderMode = ColumnHeaderMode.SET_COLUMN_HEADER;

    interface ColumnHeaderModeRef extends ParameterReference<ColumnHeaderMode> {
    }

    private static final class ColumnHeaderModePersistor implements NodeParametersPersistor<ColumnHeaderMode> {

        @Override
        public ColumnHeaderMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var settingsTab = settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB);
            boolean useFirstLine = settingsTab.getBoolean(CFG_HAS_COL_HEADER);
            return useFirstLine ? ColumnHeaderMode.FIRST_LINE_AS_HEADER : ColumnHeaderMode.SET_COLUMN_HEADER;
        }

        @Override
        public void save(final ColumnHeaderMode value, final NodeSettingsWO settings) {
            var settingsTab = SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB);
            settingsTab.addBoolean(CFG_HAS_COL_HEADER, value == ColumnHeaderMode.FIRST_LINE_AS_HEADER);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{SettingsUtils.CFG_SETTINGS_TAB, CFG_HAS_COL_HEADER}};
        }
    }

    @Widget(title = "Column header name", description = """
            The name of the output column. The default is "Column".
            """)
    @Layout(HeadersSection.class)
    @Effect(predicate = IsFixColumnHeader.class, type = EffectType.SHOW)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = CFG_CUSTOM_COL_HEADER)
    String m_columnHeaderName = "Column";

    private static final class IsFixColumnHeader implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnHeaderModeRef.class).isOneOf(ColumnHeaderMode.SET_COLUMN_HEADER);
        }
    }

    @Widget(title = "Empty line handling", description = """
            Select how empty lines should be handled.
            """)
    @ValueReference(EmptyLineModeRef.class)
    @Layout(AdvancedSection.class)
    @RadioButtonsWidget
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persist(configKey = CFG_EMPTY_LINE_MODE)
    EmptyLineMode m_emptyLineMode = EmptyLineMode.REPLACE_BY_MISSING;

    static final class EmptyLineModeRef extends ReferenceStateProvider<EmptyLineMode> {
    }

    @Widget(title = "Replacement text", description = """
            The text to use as replacement for empty lines.
            """)
    @Layout(AdvancedSection.class)
    @Effect(predicate = IsReplaceEmptyWithText.class, type = EffectType.SHOW)
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persist(configKey = CFG_EMPTY_REPLACEMENT)
    String m_emptyLineReplacement = "";

    private static final class IsReplaceEmptyWithText implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(EmptyLineModeRef.class).isOneOf(EmptyLineMode.REPLACE_EMPTY);
        }
    }
    @Widget(title = "Filter with regular expression", description = """
            Include in the output only lines matching the provided regular expression pattern. For example '12.*' will \
            only match lines starting with 12 and have zero or more characters afterwards. The default value is '.*' \
            which matches everything.\
            """)
    @Layout(AdvancedSection.class)
    @OptionalWidget(defaultProvider = RegexDefaultProvider.class)
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persistor(RegexPersistor.class)
    Optional<String> m_regex = Optional.empty();

    private static final class RegexPersistor extends OptionalStringPersistor {
        public RegexPersistor() {
            super(CFG_USE_REGEX, CFG_REGEX);
        }
    }

    private static final class RegexDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return ".*";
        }
    }

    @Widget(title = "Append path column", description = """
            If enabled, the node will append a path column with the provided name to the output table.
            This column contains for each row which file it was read from. The node will fail if adding
            the column with the provided name causes a name collision with any of the columns in the
            read table.
            """)
    @Layout(AdvancedSection.class)
    @OptionalWidget(defaultProvider = PathColumnDefaultProvider.class)
    @Migrate(loadDefaultIfAbsent = true)
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persistor(PathColumnPersistor.class)
    Optional<String> m_appendPathColumn = Optional.empty();

    private static final class PathColumnDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return "Path";
        }
    }

    private static final class PathColumnPersistor extends OptionalStringPersistor {
        public PathColumnPersistor() {
            super(CFG_APPEND_PATH_COLUMN, CFG_PATH_COLUMN_NAME);
        }
    }

    @Widget(title = "Limit data rows", description = """
            If enabled, the number of rows which will be read and output by the node is limited
            to the specified value.
            """)
    @Layout(AdvancedSection.class)
    @OptionalWidget(defaultProvider = MaxRowsDefaultProvider.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, stepSize = 1000)
    @Migrate(loadDefaultIfAbsent = true)
    @Persistor(LimitRowsPersistor.class)
    Optional<Long> m_limitRows = Optional.empty();

    private static final class MaxRowsDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 50L;
        }
    }

    private static final class LimitRowsPersistor implements NodeParametersPersistor<Optional<Long>> {
        @Override
        public Optional<Long> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var advSettings = settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB);
            boolean limitRows = advSettings.getBoolean(CFG_LIMIT_DATA_ROWS);
            if (limitRows) {
                return Optional.of(advSettings.getLong(CFG_MAX_ROWS));
            }
            return Optional.empty();
        }

        @Override
        public void save(final Optional<Long> value, final NodeSettingsWO settings) {
            var advSettings = SettingsUtils.getOrAdd(settings, CFG_ADVANCED_SETTINGS_TAB);
            advSettings.addBoolean(CFG_LIMIT_DATA_ROWS, value.isPresent());
            advSettings.addLong(CFG_MAX_ROWS, value.orElse(1000L));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ADVANCED_SETTINGS_TAB, CFG_LIMIT_DATA_ROWS},
                {CFG_ADVANCED_SETTINGS_TAB, CFG_MAX_ROWS}};
        }
    }

}
