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

package org.knime.base.node.io.filehandling.filereader;

import java.net.URL;

import org.knime.base.node.io.filehandling.webui.reader2.MaxNumberOfRowsParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.LegacyReaderFileSelectionPersistor;
import org.knime.filehandling.core.connections.FSLocationUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotEmptyValidation;

/**
 * Node parameters for File Reader (Complex Format).
 *
 * @author Tim Crundall, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class FileReaderNodeParameters implements NodeParameters {

    FileReaderNodeParameters(final NodeParametersInput input) {
        this(input.getURLConfiguration());
    }

    FileReaderNodeParameters(final NodeCreationConfiguration nodeCreationConfig) {
        this(nodeCreationConfig.getURLConfig());
    }

    private FileReaderNodeParameters(final java.util.Optional<? extends URLConfiguration> urlConfig) { // NOSONAR
        if (urlConfig.isPresent()) {
            final URL url = urlConfig.get().getUrl();
            m_fileSelection = new FileSelection(FSLocationUtil.createFromURL(url.toString()));
        }
    }

    FileReaderNodeParameters() {
        // default constructor
    }

    // ========== File Selection ==========

    /**
     * File selection using LegacyReaderFileSelectionPersistor to persist in the same format as
     * SettingsModelReaderFileChooser (under "file_selection" config key with "path" subkey).
     */
    static class FileSelectionPersistor extends LegacyReaderFileSelectionPersistor {
        FileSelectionPersistor() {
            super("file_selection");
        }
    }

    @Widget(title = "File", description = "Select the file to read.")
    @Persistor(FileSelectionPersistor.class)
    FileSelection m_fileSelection = new FileSelection();

    // NOTE: "Preserve user settings" checkbox is a UI-only behavior feature and is not migrated
    // as it controls dialog behavior, not node execution settings.

    // ========== Tokenizer Settings (Required by TokenizerSettings base class) ==========

    /**
     * Tokenizer settings group that handles the required Delimiters, Quotes, Comments, and WhiteSpaces
     * config structures needed by TokenizerSettings.
     */
    static class TokenizerConfigGroup implements NodeParameters {
        // These fields are not shown in UI but are persisted for compatibility
        String m_delimiter = ",";
        boolean m_ignoreSpacesAndTabs = false;
        boolean m_javaStyleComments = false;
        String m_singleLineComment = "";
        long m_skipFirstLines = 0;
    }

    /**
     * Custom persistor for tokenizer settings that writes the required config structure
     * (Delimiters, Quotes, Comments, WhiteSpaces) expected by TokenizerSettings.
     */
    static class TokenizerConfigPersistor implements NodeParametersPersistor<TokenizerConfigGroup> {
        
        @Override
        public TokenizerConfigGroup load(final NodeSettingsRO settings) throws InvalidSettingsException {
            // Load from the tokenizer config structure
            var config = new TokenizerConfigGroup();
            
            // Load SkipFirstLines if present
            if (settings.containsKey("SkipFirstLines")) {
                config.m_skipFirstLines = settings.getLong("SkipFirstLines");
            }
            
            // TODO: Load delimiter, whitespaces, comments from their respective config structures
            // For now, use defaults
            
            return config;
        }

        @Override
        public void save(final TokenizerConfigGroup obj, final NodeSettingsWO settings) {
            // Write the required tokenizer config structure
            
            // 1. Delimiters config
            var delimsConfig = settings.addNodeSettings("Delimiters");
            var delim0 = delimsConfig.addNodeSettings("Delim0");
            delim0.addString("pattern", obj.m_delimiter);
            delim0.addBoolean("combineMultiple", false);
            delim0.addBoolean("returnAsToken", false);
            delim0.addBoolean("includeInToken", false);
            
            // 2. Quotes config (empty by default)
            settings.addNodeSettings("Quotes");
            
            // 3. Comments config
            var commentsConfig = settings.addNodeSettings("Comments");
            if (obj.m_javaStyleComments) {
                // Block comment: /* ... */
                var comment0 = commentsConfig.addNodeSettings("Comment0");
                comment0.addString("begin", "/*");
                comment0.addString("end", "*/");
                comment0.addString("EscChar", "");
                comment0.addBoolean("DontRem", false);
                
                // Line comment: // ... newline
                var comment1 = commentsConfig.addNodeSettings("Comment1");
                comment1.addString("begin", "//");
                comment1.addString("end", "%%00010"); // newline
                comment1.addString("EscChar", "");
                comment1.addBoolean("DontRem", false);
            }
            if (obj.m_singleLineComment != null && !obj.m_singleLineComment.isEmpty()) {
                var commentKey = obj.m_javaStyleComments ? "Comment2" : "Comment0";
                var comment = commentsConfig.addNodeSettings(commentKey);
                comment.addString("begin", obj.m_singleLineComment);
                comment.addString("end", "%%00010"); // newline
                comment.addString("EscChar", "");
                comment.addBoolean("DontRem", false);
            }
            
            // 4. WhiteSpaces config
            var whiteSpacesConfig = settings.addNodeSettings("WhiteSpaces");
            if (obj.m_ignoreSpacesAndTabs) {
                // Add space (%%00032) and tab (%%00009)
                var ws0 = whiteSpacesConfig.addNodeSettings("WhiteSpace0");
                ws0.addString("pattern", "%%00032"); // space
                var ws1 = whiteSpacesConfig.addNodeSettings("WhiteSpace1");
                ws1.addString("pattern", "%%00009"); // tab
            }
            
            // 5. Other tokenizer settings
            settings.addBoolean("CombineMultDelims", false);
            settings.addLong("SkipFirstLines", obj.m_skipFirstLines);
            settings.addBoolean("NewLineInQuotes", false);
            
            // 6. Row Delimiters config (required by FileReaderSettings)
            var rowDelimsConfig = settings.addNodeSettings("RowDelims");
            // Add newline (LF)
            var rdelim0 = rowDelimsConfig.addNodeSettings("RDelim0");
            rdelim0.addString("pattern", "%%00010"); // newline
            rdelim0.addBoolean("SkipEmptyLine", false);
            // Add carriage return (CR)  
            var rdelim1 = rowDelimsConfig.addNodeSettings("RDelim1");
            rdelim1.addString("pattern", "%%00013"); // carriage return
            rdelim1.addBoolean("SkipEmptyLine", false);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{
                {"Delimiters"},
                {"Quotes"},
                {"Comments"},
                {"WhiteSpaces"},
                {"RowDelims"},
                {"SkipFirstLines"}
            };
        }
    }

    @Persistor(TokenizerConfigPersistor.class)
    TokenizerConfigGroup m_tokenizerConfig = new TokenizerConfigGroup();

    // ========== Basic Settings ==========

    @Widget(title = "Read column headers",
        description = "If checked, the items in the first line of the file are used as column names. "
            + "Otherwise default column names are created.")
    boolean m_hasColHdr = true;

    @Widget(title = "Read RowIDs",
        description = "If checked, the first column in the file is used as RowIDs. "
            + "If not checked, default row headers are created.")
    boolean m_hasRowHdr = false;

    // ========== Advanced Settings ==========

    // TODO: Implement FileEncodingParameters (reuse from CSV reader)
    // - Map to FileReaderSettings.getCharsetName()
    // - Config key: CFGKEY_CHARSETNAME

    // TODO: Implement quote support parameter
    // - Map to FileReaderSettings quote-related methods
    // - From QuotePanel in Advanced dialog

    // TODO: Implement decimal separator parameter
    // - Map to FileReaderSettings.getDecimalSeparator()
    // - Config key: CFGKEY_DECIMALSEP = "DecimalSeparator"
    // - From DecSepPanel in Advanced dialog
    // - Default value: '.'
    // - In settings.xml: <entry key="DecimalSeparator" type="xchar" value="."/>

    @Widget(title = "Ignore empty lines",
        description = "If checked, empty lines in the file are ignored and skipped.")
    boolean m_ignoreEmptyLines = true;

    @Widget(title = "Row header prefix",
        description = "Prefix used when generating row IDs (if not reading from file). Default is 'Row'.")
    @TextInputWidget
    String m_rowPrefix = "Row";

    // Note: Character encoding - from settings.xml: <entry key="CharsetName" type="xstring" value="UTF-8"/>
    // Default persistence would be "charsetName", but config uses "CharsetName" with capitals
    @Widget(title = "Character encoding",
        description = "The character encoding used to read the file. Common values: UTF-8, ISO-8859-1, US-ASCII.")
    @TextInputWidget
    @Persist(configKey = "CharsetName")
    String m_charsetName = "UTF-8";

    @Widget(title = "Ignore delimiters at end of row",
        description = "If checked, extra delimiters at the end of rows are ignored.")
    @Persist(configKey = "ignEmtpyTokensAtEOR")
    boolean m_ignoreDelimitersAtEOR = false;

    @Widget(title = "Support short lines",
        description = "If checked, rows with too few data items are filled with missing values.")
    boolean m_acceptShortLines = false;

    @Widget(title = "Make RowIDs unique",
        description = "If checked, duplicate RowIDs are made unique by appending a suffix. "
            + "Not recommended for huge files as it requires storing all RowIDs in memory.")
    boolean m_uniquifyRowID = true;

    // NOTE: MaxNumberOfRowsParameters handles its own persistence with configKey = "MaxNumOfRows"
    MaxNumberOfRowsParameters m_maxNumberOfRowsParams = new MaxNumberOfRowsParameters();

    @Widget(title = "Missing value pattern",
        description = "Specify a pattern that will be interpreted as a missing value for all string columns. "
            + "Leave empty to disable. Individual column missing value patterns can be set in the column properties.")
    @TextInputWidget
    @Persist(configKey = "globalMissPattern")
    String m_missingValuePattern = "";

    // TODO: Implement SkipFirstLinesOfFileParameters (if it exists as a reusable component)
    // - Need to verify if this exists and can be reused

    // ========== NOT Migrated (UI-only features) ==========
    // - "Rescan" button - UI action only
    // - Preview table - UI feature only
    // - Column properties dialog (click column header) - transformation/spec inference handled separately
    // - Analysis/Quick Scan features - UI behavior only

    // ========== Persistence Notes ==========
    /*
     * COMPLEX PERSISTENCE STRUCTURES (require custom persistors):
     *
     * 1. DELIMITERS (stored in "Delimiters" config):
     *    - Multiple entries: Delim0, Delim1, Delim2, etc.
     *    - Each has: pattern, combineMultiple, includeInToken, returnAsToken
     *    - Special encoding for special chars (e.g., %%00010 for newline, %%00013 for carriage return)
     *    - Our m_columnDelimiter needs to be converted to/from this structure
     *
     * 2. WHITESPACES (stored in "WhiteSpaces" config):
     *    - Multiple entries: WhiteSpace0, WhiteSpace1, etc.
     *    - Our m_ignoreSpacesAndTabs boolean needs to control whether space (%%00032) and tab (%%00009) are in this list
     *
     * 3. COMMENTS (stored in "Comments" config):
     *    - Multiple entries: Comment0, Comment1, etc.
     *    - Each has: begin, end, EscChar, DontRem
     *    - Java-style means: Comment0 with begin="/*" end="*\/" AND Comment1 with begin="//" end=newline
     *    - Our m_javaStyleComments and m_singleLineComment need custom persistence logic
     *
     * 4. QUOTES (stored in "Quotes" config):
     *    - Multiple entries: Quote0, Quote1, etc.
     *    - Each has: left, right, EscChar, DontRem
     *    - Example: Quote0 with left="\"" right="\"" EscChar="\"
     *    - Needs custom persistor (TODO)
     *
     * 5. ROW DELIMITERS (stored in "RowDelims" config):
     *    - Multiple entries: RDelim0, RDelim1, etc.
     *    - Each has: pattern and SkipEmptyLine flag
     *    - Typically includes newline (%%00010) and carriage return (%%00013)
     *
     * 6. COLUMN PROPERTIES (stored in "ColumnProperties" config):
     *    - Per-column settings: names, types, missing value patterns, skip flags
     *    - This is transformation/spec inference - may not be part of basic parameters migration
     *
     * For now, simple boolean and string fields use @Persist with configKey.
     * Complex structures will need custom NodeParametersPersistor implementations.
     */

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_fileSelection == null || m_fileSelection.m_path == null || m_fileSelection.m_path.getPath().isBlank()) {
            throw new InvalidSettingsException("Please select a file to read.");
        }
        m_maxNumberOfRowsParams.validate();

        // TODO: Add validation for other parameters once implemented
    }

    // TODO: Implement saveToConfig methods to persist to FileReaderNodeSettings
    // void saveToConfig(final FileReaderNodeSettings settings) {
    //     // Save other parameters
    // }

}
