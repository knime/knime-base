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
import org.knime.base.node.io.filehandling.webui.reader2.SingleFileSelectionParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
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
            m_singleFileSelectionParams = new SingleFileSelectionParameters(url);
        }
    }

    FileReaderNodeParameters() {
        // default constructor
    }

    // ========== File Selection ==========

    /**
     * File selection parameters using SingleFileSelectionParameters (single file, not multi-file like CSV reader).
     */
    SingleFileSelectionParameters m_singleFileSelectionParams = new SingleFileSelectionParameters();

    // NOTE: "Preserve user settings" checkbox is a UI-only behavior feature and is not migrated
    // as it controls dialog behavior, not node execution settings.

    // ========== Basic Settings ==========

    @Widget(title = "Read column headers",
        description = "If checked, the items in the first line of the file are used as column names. "
            + "Otherwise default column names are created.")
    boolean m_readColumnHeaders = true;

    @Widget(title = "Read RowIDs",
        description = "If checked, the first column in the file is used as RowIDs. "
            + "If not checked, default row headers are created.")
    boolean m_readRowHeaders = false;

    @Widget(title = "Column delimiter", description = """
            Enter the character(s) that separate the data tokens in the file. Use '\\t' for tab character.
            Common delimiters are comma (,), semicolon (;), tab (\\t), or space.
            """)
    @TextInputWidget(minLengthValidation = IsNotEmptyValidation.class)
    String m_columnDelimiter = ",";

    @Widget(title = "Ignore spaces and tabs",
        description = "If checked, spaces and the TAB characters are ignored (not in quoted strings though).")
    boolean m_ignoreSpacesAndTabs = false;

    // ========== Comment Settings ==========

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

    @Widget(title = "Ignore delimiters at end of row",
        description = "If checked, extra delimiters at the end of rows are ignored.")
    boolean m_ignoreDelimitersAtEndOfRow = false;

    @Widget(title = "Support short lines",
        description = "If checked, rows with too few data items are filled with missing values.")
    boolean m_supportShortLines = false;

    @Widget(title = "Make RowIDs unique",
        description = "If checked, duplicate RowIDs are made unique by appending a suffix. "
            + "Not recommended for huge files as it requires storing all RowIDs in memory.")
    boolean m_uniquifyRowIds = true;

    MaxNumberOfRowsParameters m_maxNumberOfRowsParams = new MaxNumberOfRowsParameters();

    @Widget(title = "Missing value pattern",
        description = "Specify a pattern that will be interpreted as a missing value for all string columns. "
            + "Leave empty to disable. Individual column missing value patterns can be set in the column properties.")
    @TextInputWidget
    String m_missingValuePattern = "";

    // TODO: Implement SkipFirstLinesOfFileParameters (if it exists as a reusable component)
    // - Need to verify if this exists and can be reused

    // ========== NOT Migrated (UI-only features) ==========
    // - "Rescan" button - UI action only
    // - Preview table - UI feature only
    // - Column properties dialog (click column header) - transformation/spec inference handled separately
    // - Analysis/Quick Scan features - UI behavior only

    @Override
    public void validate() throws InvalidSettingsException {
        m_singleFileSelectionParams.validate();
        m_maxNumberOfRowsParams.validate();

        // TODO: Add validation for other parameters once implemented
    }

    // TODO: Implement saveToConfig methods to persist to FileReaderNodeSettings
    // void saveToConfig(final FileReaderNodeSettings settings) {
    //     m_singleFileSelectionParams.saveToSource(...);
    //     // Save other parameters
    // }

}
