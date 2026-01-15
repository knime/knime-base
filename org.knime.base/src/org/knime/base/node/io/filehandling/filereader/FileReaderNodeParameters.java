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

    // ========== Basic Settings ==========

    @Widget(title = "Read column headers",
        description = "If checked, the items in the first line of the file are used as column names. "
            + "Otherwise default column names are created.")
    // @Persist(configKey = "hasColHdr")
    boolean m_hasColHdr = true;

    @Widget(title = "Read RowIDs",
        description = "If checked, the first column in the file is used as RowIDs. "
            + "If not checked, default row headers are created.")
    @Persist(configKey = "hasRowHdr")
    boolean m_hasRowHdr = false;

    // NOTE: Column delimiter persistence is complex - it's stored in "Delimiters" config with multiple entries
    // For now, we'll handle this with a custom persistor (TODO)
    @Widget(title = "Column delimiter", description = """
            Enter the character(s) that separate the data tokens in the file. Use '\\t' for tab character.
            Common delimiters are comma (,), semicolon (;), tab (\\t), or space.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    String m_columnDelimiter = ",";

    // NOTE: Whitespaces are stored in "WhiteSpaces" config with multiple entries (WhiteSpace0, WhiteSpace1, etc.)
    // For now, we'll handle this with a custom persistor (TODO)
    @Widget(title = "Ignore spaces and tabs",
        description = "If checked, spaces and the TAB characters are ignored (not in quoted strings though).")
    boolean m_ignoreSpacesAndTabs = false;

    // ========== Comment Settings ==========

    // NOTE: Comments are stored in "Comments" config with multiple entries (Comment0, Comment1, etc.)
    // Each has "begin", "end", "EscChar", and "DontRem" properties
    // Java-style comments = "/*" to "*/" (block) and "//" to newline (single line)
    // For now, we'll handle this with a custom persistor (TODO)
    @Widget(title = "Java-style comments",
        description = "Everything between '/*' and '*/' is ignored. Also everything after '//' until the end of the line.")
    boolean m_javaStyleComments = false;

    @Widget(title = "Single line comment",
        description = "Enter one or more characters that will indicate the start of a comment (ended by a new line).")
    @TextInputWidget
    String m_singleLineComment = "";

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
    @Persist(configKey = "ignoreEmptyLines")
    boolean m_ignoreEmptyLines = true;

    @Widget(title = "Row header prefix",
        description = "Prefix used when generating row IDs (if not reading from file). Default is 'Row'.")
    @TextInputWidget
    @Persist(configKey = "rowPrefix")
    String m_rowPrefix = "Row";

    @Widget(title = "Skip first lines",
        description = "Number of lines to skip at the beginning of the file before starting to read data.")
    @Persist(configKey = "SkipFirstLines")
    long m_skipFirstLines = 0;

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
    boolean m_ignEmtpyTokensAtEOR = false;

    @Widget(title = "Support short lines",
        description = "If checked, rows with too few data items are filled with missing values.")
    @Persist(configKey = "acceptShortLines")
    boolean m_acceptShortLines = false;

    @Widget(title = "Make RowIDs unique",
        description = "If checked, duplicate RowIDs are made unique by appending a suffix. "
            + "Not recommended for huge files as it requires storing all RowIDs in memory.")
    @Persist(configKey = "uniquifyRowID")
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
