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

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.FileSystemManagedByPortMessage;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionParameters;
import org.knime.base.node.io.filehandling.webui.reader2.ReaderLayout;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.DefaultFileChooserFilters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionMode;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.MultiFileSelectionWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.field.NodeParametersPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Node parameters for Line Reader.
 *
 * @author Rupert Ettrich, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class LineReaderNodeFactory2Parameters implements NodeParameters {

    // ===== Layout definitions =====

    @Section(title = "File")
    interface FileSection {
    }

    @Section(title = "Options for multiple files")
    @After(FileSection.class)
    interface MultipleFilesSection {
    }

    @Section(title = "Row Header")
    @After(MultipleFilesSection.class)
    interface RowHeaderSection {
    }

    @Section(title = "Column Header")
    @After(RowHeaderSection.class)
    interface ColumnHeaderSection {
    }

    @Section(title = "Empty Lines", advanced = true)
    @After(ColumnHeaderSection.class)
    interface EmptyLinesSection {
    }

    @Section(title = "Regular Expression", advanced = true)
    @After(EmptyLinesSection.class)
    interface RegexSection {
    }

    @Section(title = "Limit Rows", advanced = true)
    @After(RegexSection.class)
    interface LimitRowsSection {
    }

    @Section(title = "Path Column", advanced = true)
    @After(LimitRowsSection.class)
    interface PathColumnSection {
    }

    @Section(title = "Encoding", advanced = true)
    @After(PathColumnSection.class)
    interface EncodingSection {
    }

    // ===== File Selection =====

    static final class FileSelectionRef
        extends ReferenceStateProvider<MultiFileSelection<DefaultFileChooserFilters>>
        implements Modification.Reference {
    }

    static final class SetFileReaderExtensions implements Modification.Modifier {
        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(FileSelectionRef.class).modifyAnnotation(FileReaderWidget.class)
                .withProperty("fileExtensions", new String[]{"txt", "log"}).modify();
        }
    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    @Layout(FileSection.class)
    Void m_fileSystemFromPortNotAvailableMessage;

    @ValueReference(FileSelectionRef.class)
    @Layout(FileSection.class)
    @Modification.WidgetReference(FileSelectionRef.class)
    @Modification(SetFileReaderExtensions.class)
    @FileReaderWidget()
    @MultiFileSelectionWidget({MultiFileSelectionMode.FILE, MultiFileSelectionMode.FILES_IN_FOLDERS})
    MultiFileSelection<DefaultFileChooserFilters> m_source =
        new MultiFileSelection<>(MultiFileSelectionMode.FILE, new DefaultFileChooserFilters());

    // ===== Multiple Files Options =====

    static final class MultiFileSelectionIsActive implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            if (i.isMissing(FileSelectionRef.class)) {
                return i.getConstant(in -> true);
            }
            return i.getMultiFileSelection(FileSelectionRef.class).isMultiFileSelection();
        }
    }

    @Widget(title = "Fail on differing specs", description = """
            If enabled and the specs of the files are different, the node will fail. This option is only
            relevant when reading multiple files in a folder.
            """)
    @Layout(MultipleFilesSection.class)
    @Effect(predicate = MultiFileSelectionIsActive.class, type = EffectType.SHOW)
    @Persist(configKey = "fail_on_different_specs", configPaths = "settings")
    boolean m_failOnDifferingSpecs = true;

    // ===== Row Header =====

    @Widget(title = "Row header prefix", description = """
            Specifies the prefix of the row header. The default is "Row" and the resulting row headers are
            "Row0", "Row1", and so on.
            """)
    @Layout(RowHeaderSection.class)
    @Persist(configKey = "row_header_prefix", configPaths = "settings")
    String m_rowHeaderPrefix = "Row";

    // ===== Column Header =====

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
    @Persist(configKey = "custom_col_header", configPaths = "settings")
    String m_columnHeaderName = "Column";

    // ===== Empty Lines =====

    /**
     * Options for handling empty lines. Corresponds to {@link EmptyLineMode}.
     */
    enum EmptyLineModeOption {
            @Label(value = "Skip empty lines", description = "Empty lines will be skipped and are not added to the output.")
            SKIP_EMPTY,

            @Label(value = "Replace empty lines with missing cells", description = "Empty lines will be replaced by a missing cell.")
            REPLACE_BY_MISSING,

            @Label(value = "Replace empty lines with custom text", description = "Empty lines will be replaced by the text entered in the text field below.")
            REPLACE_EMPTY;

        static EmptyLineModeOption fromEmptyLineMode(final EmptyLineMode mode) {
            return switch (mode) {
                case SKIP_EMPTY -> SKIP_EMPTY;
                case REPLACE_BY_MISSING -> REPLACE_BY_MISSING;
                case REPLACE_EMPTY -> REPLACE_EMPTY;
            };
        }

        EmptyLineMode toEmptyLineMode() {
            return switch (this) {
                case SKIP_EMPTY -> EmptyLineMode.SKIP_EMPTY;
                case REPLACE_BY_MISSING -> EmptyLineMode.REPLACE_BY_MISSING;
                case REPLACE_EMPTY -> EmptyLineMode.REPLACE_EMPTY;
            };
        }
    }

    static final class EmptyLineModeRef extends ReferenceStateProvider<EmptyLineModeOption> {
    }

    @Widget(title = "Empty line handling", description = """
            Select how empty lines should be handled.
            """)
    @ValueReference(EmptyLineModeRef.class)
    @Layout(EmptyLinesSection.class)
    @RadioButtonsWidget
    @Migrate
    @Persistor(EmptyLineModePersistor.class)
    EmptyLineModeOption m_emptyLineMode = EmptyLineModeOption.REPLACE_BY_MISSING;

    static final class IsReplaceEmptyWithText implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(EmptyLineModeRef.class).isOneOf(EmptyLineModeOption.REPLACE_EMPTY);
        }
    }

    @Widget(title = "Replacement text", description = """
            The text to use as replacement for empty lines.
            """)
    @Layout(EmptyLinesSection.class)
    @Effect(predicate = IsReplaceEmptyWithText.class, type = EffectType.SHOW)
    @Persist(configKey = "empty_line_replacement", configPaths = "advanced_settings")
    String m_emptyLineReplacement = "";

    // ===== Regular Expression =====

    static final class UseRegexRef extends ReferenceStateProvider<Boolean> {
    }

    @Widget(title = "Match input against regex", description = """
            If enabled, every line will be checked against the regular expression below.
            Only lines matching the pattern will be included in the output.
            """)
    @ValueReference(UseRegexRef.class)
    @Layout(RegexSection.class)
    @Persist(configKey = "use_regex", configPaths = "advanced_settings")
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
    @Persist(configKey = "regex", configPaths = "advanced_settings")
    String m_regex = ".*";

    // ===== Limit Rows =====

    static final class MaxRowsDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 1000L;
        }
    }

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

    // ===== Encoding =====

    /**
     * Options for file encoding.
     */
    enum FileEncodingOption {
            @Label(value = "OS default", description = "Uses the default decoding set by the operating system.")
            DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"),

            @Label(value = "ISO-8859-1", description = "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.")
            ISO_8859_1("ISO-8859-1"),

            @Label(value = "US-ASCII", description = "Seven-bit ASCII, also referred to as US-ASCII.")
            US_ASCII("US-ASCII"),

            @Label(value = "UTF-8", description = "Eight-bit UCS Transformation Format.")
            UTF_8("UTF-8"),

            @Label(value = "UTF-16", description = """
                    Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark in the file.
                    """)
            UTF_16("UTF-16"),

            @Label(value = "UTF-16BE", description = "Sixteen-bit UCS Transformation Format, big-endian byte order.")
            UTF_16BE("UTF-16BE"),

            @Label(value = "UTF-16LE", description = "Sixteen-bit UCS Transformation Format, little-endian byte order.")
            UTF_16LE("UTF-16LE"),

            @Label(value = "Other", description = "Enter a valid charset name supported by the Java Virtual Machine.")
            OTHER("");

        final String m_charsetName;

        final String m_nonConstantDisplayText;

        FileEncodingOption(final String charsetName) {
            this(charsetName, null);
        }

        FileEncodingOption(final String charsetName, final String nonConstantDisplayText) {
            m_charsetName = charsetName;
            m_nonConstantDisplayText = nonConstantDisplayText;
        }

        static FileEncodingOption fromCharsetName(final String charsetName) {
            return Arrays.stream(FileEncodingOption.values())
                .filter(encoding -> Objects.equals(encoding.m_charsetName, charsetName)).findFirst().orElse(OTHER);
        }

        EnumChoice<FileEncodingOption> toEnumChoice() {
            if (m_nonConstantDisplayText == null) {
                return EnumChoice.fromEnumConst(this);
            }
            return new EnumChoice<>(this, m_nonConstantDisplayText);
        }
    }

    static final class EncodingChoicesProvider implements EnumChoicesProvider<FileEncodingOption> {
        @Override
        public List<EnumChoice<FileEncodingOption>> computeState(final NodeParametersInput context) {
            return Arrays.stream(FileEncodingOption.values()).map(FileEncodingOption::toEnumChoice).toList();
        }
    }

    static final class FileEncodingRef extends ReferenceStateProvider<FileEncodingOption> {
    }

    @Widget(title = "File encoding", description = """
            Defines the character set used to read a file that contains characters in a different encoding.
            You can choose from a list of character encodings (UTF-8, UTF-16, etc.), or specify any other
            encoding supported by your Java Virtual Machine (VM). The default value uses the default encoding
            of the Java VM, which may depend on the locale or the Java property "file.encoding".
            """)
    @ValueReference(FileEncodingRef.class)
    @Layout(EncodingSection.class)
    @ChoicesProvider(EncodingChoicesProvider.class)
    @Migrate
    @Persistor(FileEncodingPersistor.class)
    FileEncodingOption m_fileEncoding = FileEncodingOption.DEFAULT;

    static final class IsOtherEncoding implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileEncodingRef.class).isOneOf(FileEncodingOption.OTHER);
        }
    }

    @Widget(title = "Custom encoding", description = """
            A custom character set name to use for reading the file.
            """)
    @Layout(EncodingSection.class)
    @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    @Migrate
    @Persistor(CustomEncodingPersistor.class)
    String m_customEncoding = "";

    // ===== Custom Persistors =====

    /**
     * Persistor for the column header mode. Maps the boolean CFG_HAS_COL_HEADER to the enum.
     */
    static final class ColumnHeaderModePersistor implements NodeParametersPersistor<ColumnHeaderMode> {
        private static final String CFG_HAS_COL_HEADER = "use_column_header";
        private static final String CFG_SETTINGS = "settings";

        @Override
        public ColumnHeaderMode load(final org.knime.core.node.NodeSettingsRO settings)
            throws InvalidSettingsException {
            var settingsTab = settings.getNodeSettings(CFG_SETTINGS);
            boolean useFirstLine = settingsTab.getBoolean(CFG_HAS_COL_HEADER);
            return useFirstLine ? ColumnHeaderMode.FIRST_LINE_AS_HEADER : ColumnHeaderMode.FIX_COLUMN_HEADER;
        }

        @Override
        public void save(final ColumnHeaderMode value, final org.knime.core.node.NodeSettingsWO settings) {
            var settingsTab = settings.addNodeSettings(CFG_SETTINGS);
            settingsTab.addBoolean(CFG_HAS_COL_HEADER, value == ColumnHeaderMode.FIRST_LINE_AS_HEADER);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_SETTINGS, CFG_HAS_COL_HEADER}};
        }
    }

    /**
     * Persistor for the empty line mode enum.
     */
    static final class EmptyLineModePersistor implements NodeParametersPersistor<EmptyLineModeOption> {
        private static final String CFG_EMPTY_LINE_MODE = "empty_line_mode";
        private static final String CFG_ADVANCED_SETTINGS = "advanced_settings";

        @Override
        public EmptyLineModeOption load(final org.knime.core.node.NodeSettingsRO settings)
            throws InvalidSettingsException {
            var advSettings = settings.getNodeSettings(CFG_ADVANCED_SETTINGS);
            String modeStr = advSettings.getString(CFG_EMPTY_LINE_MODE);
            EmptyLineMode mode = EmptyLineMode.valueOf(modeStr);
            return EmptyLineModeOption.fromEmptyLineMode(mode);
        }

        @Override
        public void save(final EmptyLineModeOption value, final org.knime.core.node.NodeSettingsWO settings) {
            var advSettings = settings.addNodeSettings(CFG_ADVANCED_SETTINGS);
            advSettings.addString(CFG_EMPTY_LINE_MODE, value.toEmptyLineMode().name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ADVANCED_SETTINGS, CFG_EMPTY_LINE_MODE}};
        }
    }

    /**
     * Persistor for limit rows with optional handling.
     */
    static final class LimitRowsPersistor implements NodeParametersPersistor<Optional<Long>> {
        private static final String CFG_LIMIT_DATA_ROWS = "limit_data_rows";
        private static final String CFG_MAX_ROWS = "max_rows";
        private static final String CFG_ADVANCED_SETTINGS = "advanced_settings";

        @Override
        public Optional<Long> load(final org.knime.core.node.NodeSettingsRO settings) throws InvalidSettingsException {
            var advSettings = settings.getNodeSettings(CFG_ADVANCED_SETTINGS);
            boolean limitRows = advSettings.getBoolean(CFG_LIMIT_DATA_ROWS);
            if (limitRows) {
                return Optional.of(advSettings.getLong(CFG_MAX_ROWS));
            }
            return Optional.empty();
        }

        @Override
        public void save(final Optional<Long> value, final org.knime.core.node.NodeSettingsWO settings) {
            var advSettings = settings.addNodeSettings(CFG_ADVANCED_SETTINGS);
            advSettings.addBoolean(CFG_LIMIT_DATA_ROWS, value.isPresent());
            advSettings.addLong(CFG_MAX_ROWS, value.orElse(1000L));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ADVANCED_SETTINGS, CFG_LIMIT_DATA_ROWS},
                {CFG_ADVANCED_SETTINGS, CFG_MAX_ROWS}};
        }
    }

    /**
     * Persistor for path column with optional handling.
     */
    static final class PathColumnPersistor implements NodeParametersPersistor<Optional<String>> {
        private static final String CFG_APPEND_PATH_COLUMN = "append_path_column#$internals#";
        private static final String CFG_PATH_COLUMN_NAME = "path_column_name#$internals#";
        private static final String CFG_ADVANCED_SETTINGS = "advanced_settings";

        @Override
        public Optional<String> load(final org.knime.core.node.NodeSettingsRO settings)
            throws InvalidSettingsException {
            var advSettings = settings.getNodeSettings(CFG_ADVANCED_SETTINGS);
            // Handle case where keys might not exist (added in 4.4.0)
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
        public void save(final Optional<String> value, final org.knime.core.node.NodeSettingsWO settings) {
            var advSettings = settings.addNodeSettings(CFG_ADVANCED_SETTINGS);
            advSettings.addBoolean(CFG_APPEND_PATH_COLUMN, value.isPresent());
            advSettings.addString(CFG_PATH_COLUMN_NAME, value.orElse(""));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ADVANCED_SETTINGS, CFG_APPEND_PATH_COLUMN},
                {CFG_ADVANCED_SETTINGS, CFG_PATH_COLUMN_NAME}};
        }
    }

    /**
     * Persistor for file encoding.
     */
    static final class FileEncodingPersistor implements NodeParametersPersistor<FileEncodingOption> {
        private static final String CFG_CHARSET = "charset";
        private static final String CFG_ENCODING = "encoding";

        @Override
        public FileEncodingOption load(final org.knime.core.node.NodeSettingsRO settings)
            throws InvalidSettingsException {
            var encodingSettings = settings.getNodeSettings(CFG_ENCODING);
            String charsetName = encodingSettings.getString(CFG_CHARSET);
            return FileEncodingOption.fromCharsetName(charsetName);
        }

        @Override
        public void save(final FileEncodingOption value, final org.knime.core.node.NodeSettingsWO settings) {
            // Note: actual saving is handled by CustomEncodingPersistor for the combined charset value
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{CFG_ENCODING, CFG_CHARSET}};
        }
    }

    /**
     * Persistor for custom encoding - handles the actual charset string.
     */
    static final class CustomEncodingPersistor implements NodeParametersPersistor<String> {
        private static final String CFG_CHARSET = "charset";
        private static final String CFG_ENCODING = "encoding";

        @Override
        public String load(final org.knime.core.node.NodeSettingsRO settings) throws InvalidSettingsException {
            var encodingSettings = settings.getNodeSettings(CFG_ENCODING);
            String charsetName = encodingSettings.getString(CFG_CHARSET);
            // If it's a known encoding, return empty string (the encoding dropdown handles it)
            if (FileEncodingOption.fromCharsetName(charsetName) != FileEncodingOption.OTHER) {
                return "";
            }
            return charsetName != null ? charsetName : "";
        }

        @Override
        public void save(final String value, final org.knime.core.node.NodeSettingsWO settings) {
            // Actual charset saving is handled by the combined save in validate/save sequence
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{};
        }
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_source.validate();
        if (m_columnHeaderMode == ColumnHeaderMode.FIX_COLUMN_HEADER && m_columnHeaderName.isBlank()) {
            throw new InvalidSettingsException("Column header name must not be empty.");
        }
        if (m_useRegex && m_regex.isBlank()) {
            throw new InvalidSettingsException("Regular expression must not be empty when enabled.");
        }
        if (m_limitRows.isPresent() && m_limitRows.get() < 0) {
            throw new InvalidSettingsException("The maximum number of rows must be non-negative.");
        }
        if (m_fileEncoding == FileEncodingOption.OTHER && m_customEncoding.isBlank()) {
            throw new InvalidSettingsException("Custom encoding must not be empty when 'Other' is selected.");
        }
    }
}
