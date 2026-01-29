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
import org.knime.base.node.io.filehandling.webui.FileEncodingParameters.FileEncodingOption;
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

    @Section(title = "Input File")
    interface FileSection {
    }

    @Section(title = "Row Header")
    @After(FileSection.class)
    interface RowHeaderSection {
    }

    @Section(title = "Column Header")
    @After(RowHeaderSection.class)
    interface ColumnHeaderSection {
    }

    @Section(title = "Empty Lines")
    @After(ColumnHeaderSection.class)
    @Advanced
    interface EmptyLinesSection {
    }

    @Section(title = "Regular Expression")
    @After(EmptyLinesSection.class)
    @Advanced
    interface RegexSection {
    }

    @Section(title = "Limit Rows")
    @After(RegexSection.class)
    @Advanced
    interface LimitRowsSection {
    }

    @Section(title = "Path Column")
    @After(LimitRowsSection.class)
    @Advanced
    interface PathColumnSection {
    }

    interface FileSelectionRef extends ParameterReference<LegacyMultiFileSelection> {
    }

    static final class MultiFileSelectionIsActive implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return ((LegacyPredicateInitializer)i).getLegacyMultiFileSelection(FileSelectionRef.class)
                .getSelectionMode().isOneOf(MultiFileSelectionMode.FILES_IN_FOLDERS, MultiFileSelectionMode.FOLDER);
        }
    }

    @Layout(FileSection.class)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = "file_selection")
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
    @ValueReference(FileSelectionRef.class)
    LegacyMultiFileSelection m_fileSelection = new LegacyMultiFileSelection(MultiFileSelectionMode.FILE);

    @Widget(title = "File Encoding", description = """
            To read files with a different encoding, you can select a character set \
            (UTF-8, UTF-16, etc.) or specify any other encoding supported by your Java VM. By default, the \
            node uses the system default.\
            """)
    @Persistor(FileEncodingPersistor.class)
    @Advanced
    @Layout(FileSection.class)
    @Migrate
    FileEncodingParameters m_charSet = new FileEncodingParameters();

    @Widget(title = "Fail on differing specs", description = """
            If enabled and the specs of the files are different, the node will fail. This option is only
            relevant when reading multiple files in a folder.
            """)
    @Layout(FileSection.class)
    @Effect(predicate = MultiFileSelectionIsActive.class, type = EffectType.SHOW)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = CFG_FAIL_DIFFERING_SPECS)
    boolean m_failOnDifferingSpecs = true;

    @Widget(title = "Row header prefix", description = """
            Specifies the prefix of the row header. The default is "Row" and the resulting row headers are
            "Row0", "Row1", and so on.
            """)
    @Layout(RowHeaderSection.class)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = CFG_ROW_HEADER_PREFIX)
    String m_rowHeaderPrefix = "Row";

    /**
     * Options for column header configuration.
     */
    enum ColumnHeaderMode {
            @Label(value = "Use fix column header", description = """
                    The name of the output column can be set manually in the text field below. \
                    The default name is 'Column'.
                    """)
            FIX_COLUMN_HEADER,

            @Label(value = "Use first line as column header", description = """
                    The name of the output column equals the first row of the read file.
                    """)
            FIRST_LINE_AS_HEADER;
    }

    static final class ColumnHeaderModeRef extends ReferenceStateProvider<ColumnHeaderMode> {
    }

    @Widget(title = "Column header", description = """
            Select how the column header should be determined.
            """)
    @ValueReference(ColumnHeaderModeRef.class)
    @Layout(ColumnHeaderSection.class)
    @RadioButtonsWidget
    @Migrate
    @Persistor(ColumnHeaderModePersistor.class)
    ColumnHeaderMode m_columnHeaderMode = ColumnHeaderMode.FIX_COLUMN_HEADER;

    static final class IsFixColumnHeader implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnHeaderModeRef.class).isOneOf(ColumnHeaderMode.FIX_COLUMN_HEADER);
        }
    }

    @Widget(title = "Column header name", description = """
            The name of the output column. The default is "Column".
            """)
    @Layout(ColumnHeaderSection.class)
    @Effect(predicate = IsFixColumnHeader.class, type = EffectType.SHOW)
    @PersistWithin(value = {SettingsUtils.CFG_SETTINGS_TAB})
    @Persist(configKey = CFG_CUSTOM_COL_HEADER)
    String m_columnHeaderName = "Column";

    static final class EmptyLineModeRef extends ReferenceStateProvider<EmptyLineMode> {
    }

    @Widget(title = "Empty line handling", description = """
            Select how empty lines should be handled.
            """)
    @ValueReference(EmptyLineModeRef.class)
    @Layout(EmptyLinesSection.class)
    @RadioButtonsWidget
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persist(configKey = CFG_EMPTY_LINE_MODE)
    EmptyLineMode m_emptyLineMode = EmptyLineMode.REPLACE_BY_MISSING;

    static final class IsReplaceEmptyWithText implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(EmptyLineModeRef.class).isOneOf(EmptyLineMode.REPLACE_EMPTY);
        }
    }

    @Widget(title = "Replacement text", description = """
            The text to use as replacement for empty lines.
            """)
    @Layout(EmptyLinesSection.class)
    @Effect(predicate = IsReplaceEmptyWithText.class, type = EffectType.SHOW)
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persist(configKey = CFG_EMPTY_REPLACEMENT)
    String m_emptyLineReplacement = "";

    static final class UseRegexRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Match input against regex", description = """
            If enabled, every line will be checked against the regular expression below.
            Only lines matching the pattern will be included in the output.
            """)
    @ValueReference(UseRegexRef.class)
    @Layout(RegexSection.class)
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persist(configKey = "use_regex")
    boolean m_useRegex = false;

    static final class IsUseRegex implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseRegexRef.class).isTrue();
        }
    }

    @Widget(title = "Regular expression", description = """
            The regular expression to match lines against. For example '12.*' will only match lines starting
            with 12 and have zero or more characters afterwards. The default value is '.*' which matches everything.
            """)
    @Layout(RegexSection.class)
    @Effect(predicate = IsUseRegex.class, type = EffectType.SHOW)
    @PersistWithin(value = {CFG_ADVANCED_SETTINGS_TAB})
    @Persist(configKey = "regex")
    String m_regex = ".*";

    static final class MaxRowsDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 1000L;
        }
    }

    // UIEXT-3203 framework support for optional
    @Widget(title = "Limit data rows", description = """
            If enabled, the number of rows which will be read and output by the node is limited
            to the specified value.
            """)
    @Layout(LimitRowsSection.class)
    @OptionalWidget(defaultProvider = MaxRowsDefaultProvider.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Migrate
    @Persistor(LimitRowsPersistor.class)
    Optional<Long> m_limitRows = Optional.empty();

    // ===== Path Column =====

    static final class PathColumnDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput context) {
            return "Path";
        }
    }

    // UIEXT-3203 framework support for optional
    @Widget(title = "Append path column", description = """
            If enabled, the node will append a path column with the provided name to the output table.
            This column contains for each row which file it was read from. The node will fail if adding
            the column with the provided name causes a name collision with any of the columns in the
            read table.
            """)
    @Layout(PathColumnSection.class)
    @OptionalWidget(defaultProvider = PathColumnDefaultProvider.class)
    @Migrate
    @Persistor(PathColumnPersistor.class)
    Optional<String> m_appendPathColumn = Optional.empty();

    // ===== Custom Persistors =====

    /**
     * Persistor for the column header mode. Maps the boolean CFG_HAS_COL_HEADER to the enum.
     */
    static final class ColumnHeaderModePersistor implements NodeParametersPersistor<ColumnHeaderMode> {

        @Override
        public ColumnHeaderMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var settingsTab = settings.getNodeSettings(SettingsUtils.CFG_SETTINGS_TAB);
            boolean useFirstLine = settingsTab.getBoolean(CFG_HAS_COL_HEADER);
            return useFirstLine ? ColumnHeaderMode.FIRST_LINE_AS_HEADER : ColumnHeaderMode.FIX_COLUMN_HEADER;
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

    /**
     * Persistor for limit rows with optional handling.
     */
    static final class LimitRowsPersistor implements NodeParametersPersistor<Optional<Long>> {
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

    /**
     * Persistor for path column with optional handling.
     */
    static final class PathColumnPersistor implements NodeParametersPersistor<Optional<String>> {
        @Override
        public Optional<String> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var advSettings = settings.getNodeSettings(CFG_ADVANCED_SETTINGS_TAB);
            if (!advSettings.containsKey(CFG_APPEND_PATH_COLUMN)) {
                return Optional.empty();
            }
            boolean appendColumn = advSettings.getBoolean(CFG_APPEND_PATH_COLUMN);
            if (appendColumn) {
                return Optional.of(advSettings.getString(CFG_PATH_COLUMN_NAME));
            }
            return Optional.empty();
        }

        @Override
        public void save(final Optional<String> value, final NodeSettingsWO settings) {
            var advSettings = SettingsUtils.getOrAdd(settings, CFG_ADVANCED_SETTINGS_TAB);
            advSettings.addBoolean(CFG_APPEND_PATH_COLUMN, value.isPresent());
            advSettings.addString(CFG_PATH_COLUMN_NAME, value.orElse(""));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ADVANCED_SETTINGS_TAB, CFG_APPEND_PATH_COLUMN},
                {CFG_ADVANCED_SETTINGS_TAB, CFG_PATH_COLUMN_NAME}};
        }
    }

    static class FileEncodingPersistor implements NodeParametersPersistor<FileEncodingParameters> {

        /**
         * {@inheritDoc}
         */
        @Override
        public FileEncodingParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var encodingSettings = settings.getNodeSettings(CFG_ENCODING_TAB);
            final var currentCharsetName =
                encodingSettings.getString(CFG_CHARSET, FileEncodingOption.DEFAULT.getCharsetName());
            final var currentEncoding = FileEncodingOption.fromCharsetName(currentCharsetName);
            final var fileEncodingParameters = new FileEncodingParameters(currentEncoding,
                currentEncoding == FileEncodingOption.OTHER ? currentCharsetName : null);
            return fileEncodingParameters;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void save(final FileEncodingParameters param, final NodeSettingsWO settings) {
            final var fileEncoding = param.getFileEncoding();
            final var customEncoding = param.getCustomEncoding();
            final var charsetName =
                fileEncoding == FileEncodingOption.OTHER ? customEncoding : fileEncoding.getCharsetName();
            final var encodingSettings = SettingsUtils.getOrAdd(settings, CFG_ENCODING_TAB);
            encodingSettings.addString(CFG_CHARSET, charsetName);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ENCODING_TAB, CFG_CHARSET}};
        }

    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_columnHeaderMode == ColumnHeaderMode.FIX_COLUMN_HEADER && m_columnHeaderName.isBlank()) {
            throw new InvalidSettingsException("Column header name must not be empty.");
        }
        if (m_useRegex && m_regex.isBlank()) {
            throw new InvalidSettingsException("Regular expression must not be empty when enabled.");
        }
        if (m_limitRows.isPresent() && m_limitRows.get() < 0) {
            throw new InvalidSettingsException("The maximum number of rows must be non-negative.");
        }
        if (m_charSet.getFileEncoding() == FileEncodingOption.OTHER && m_charSet.getCustomEncoding().isBlank()) {
            throw new InvalidSettingsException("Custom encoding must not be empty when 'Other' is selected.");
        }
    }
}
