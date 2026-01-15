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

import org.knime.base.node.io.filehandling.webui.reader2.SingleFileSelectionParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.context.url.URLConfiguration;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;

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

    // TODO: Implement FirstRowContainsColumnNamesParameters (reuse from CSV reader)
    // - Use @Widget with title "Read column headers" and description from dialog
    // - Map to FileReaderSettings.getFileHasColumnHeaders()
    // - Config key: CFGKEY_HASCOL = "hasColHdr"

    // TODO: Implement FirstColumnContainsRowIdsParameters (reuse from CSV reader)
    // - Use @Widget with title "Read RowIDs" and description from dialog
    // - Map to FileReaderSettings.getFileHasRowHeaders()
    // - Config key: CFGKEY_HASROW = "hasRowHdr"

    // TODO: Implement column delimiter parameter
    // - Use TextInputWidget with common presets (comma, tab, semicolon, space, etc.)
    // - Or use a combo-style widget if available
    // - Map to FileReaderSettings.getAllDelimiters() (excluding row delimiters)
    // - Description: "Enter the character(s) that separate the data tokens in the file, or select a delimiter from the list."

    // TODO: Implement "Ignore spaces and tabs" parameter
    // - Use boolean/checkbox widget
    // - Map to FileReaderSettings.getAllWhiteSpaces()
    // - Description: "If checked, spaces and the TAB characters are ignored (not in quoted strings though)."

    // ========== Comment Settings ==========

    // TODO: Implement "Java-style comments" parameter
    // - Use boolean/checkbox widget
    // - Map to checking for "/*" "*/" and "//" in FileReaderSettings.getAllComments()
    // - Description: "Everything between '/*' and '*/' is ignored. Also everything after '//' until the end of the line."

    // TODO: Implement "Single line comment" parameter
    // - Use TextInputWidget
    // - Map to single-line comment patterns in FileReaderSettings.getAllComments()
    // - Description: "Enter one or more characters that will indicate the start of a comment (ended by a new line)."

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

    // TODO: Implement "Ignore delimiters at end of row" parameter
    // - Map to FileReaderSettings.ignoreEmptyTokensAtEndOfRow()
    // - Config key: CFGKEY_IGNOREATEOR = "ignEmtpyTokensAtEOR"
    // - From IgnoreDelimsPanel in Advanced dialog

    // TODO: Implement "Support short lines" parameter (allow rows with fewer columns)
    // - Map to FileReaderSettings.isSupportShortLines()
    // - Config key: CFGKEY_SHORTLINES = "acceptShortLines"
    // - From ShortLinesPanel in Advanced dialog
    // - Description: "If checked, rows with too few data items are filled with missing values."

    // TODO: Implement "Uniquify RowIDs" parameter
    // - Map to FileReaderSettings.isUniquifyRowIDs()
    // - Config key: CFGKEY_UNIQUIFYID = "uniquifyRowID"
    // - From UniquifyPanel in Advanced dialog
    // - Description: "If checked, makes RowIDs unique (not recommended for huge files)."

    // TODO: Implement MaxNumberOfRowsParameters (reuse from CSV reader - LimitRowsPanel)
    // - Map to FileReaderSettings.getMaxNumberOfRowsToRead()
    // - Config key: CFGKEY_MAXROWS = "MaxNumOfRows"

    // TODO: Implement "Missing value pattern" parameter
    // - Map to FileReaderSettings.getGlobalMissPatternStrCols()
    // - Config key: CFGKEY_GLOBALMISSPATTERN = "globalMissPattern"
    // - From MissingValuePanel in Advanced dialog
    // - Description: "Specify a missing value pattern for string columns."

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

        // TODO: Add validation for other parameters once implemented
    }

    // TODO: Implement saveToConfig methods to persist to FileReaderNodeSettings
    // void saveToConfig(final FileReaderNodeSettings settings) {
    //     m_singleFileSelectionParams.saveToSource(...);
    //     // Save other parameters
    // }

}
