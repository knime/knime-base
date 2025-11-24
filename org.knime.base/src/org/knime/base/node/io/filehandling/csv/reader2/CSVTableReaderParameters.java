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
 *   Oct 16, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.net.URL;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.webui.reader2.IfSchemaChangesParameters;
import org.knime.base.node.io.filehandling.webui.reader2.MaxNumberOfRowsParameters;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileReaderParameters;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionParameters;
import org.knime.base.node.io.filehandling.webui.reader2.MultiFileSelectionPath;
import org.knime.base.node.io.filehandling.webui.reader2.SkipFirstDataRowsParameters;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.node.parameters.NodeParameters;

/**
 * CSV-specific parameters for the CSV Table Reader Node.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
class CSVTableReaderParameters implements NodeParameters {

    CSVTableReaderParameters(final URL url) {
        m_multiFileSelectionParams = new MultiFileSelectionParameters(url);
        m_csvFormatParams = new CSVFormatParameters(url);
    }

    CSVTableReaderParameters() {
        // default constructor
    }
    // Common parameters

    static final class SetCSVExtensions extends MultiFileSelectionParameters.SetFileReaderWidgetExtensions {
        @Override
        protected String[] getExtensions() {
            return new String[]{"csv", "tsv", "txt", "gz"};
        }
    }

    @Modification(SetCSVExtensions.class)
    MultiFileSelectionParameters m_multiFileSelectionParams = new MultiFileSelectionParameters();

    SkipFirstDataRowsParameters m_skipFirstDataRowsParams = new SkipFirstDataRowsParameters();

    MaxNumberOfRowsParameters m_maxNumberOfRowsParams = new MaxNumberOfRowsParameters();

    IfSchemaChangesParameters m_ifSchemaChangesParams = new IfSchemaChangesParameters();

    MultiFileReaderParameters m_multiFileReaderParams = new MultiFileReaderParameters();

    // CSV-specific parameters

    FileEncodingParameters m_fileEncodingParams = new FileEncodingParameters();

    SkipFirstLinesOfFileParameters m_skipFirstLinesParams = new SkipFirstLinesOfFileParameters();

    CSVFormatParameters m_csvFormatParams = new CSVFormatParameters();

    AutoDetectCSVFormatParameters m_autoDetectParams = new AutoDetectCSVFormatParameters();

    FirstRowContainsColumnNamesParameters m_firstRowContainsColumnNamesParams =
        new FirstRowContainsColumnNamesParameters();

    FirstColumnContainsRowIdsParameters m_firstColumnContainsRowIdsParams = new FirstColumnContainsRowIdsParameters();

    IfRowHasFewerColumnsParameters m_ifRowHasFewerColumnsParams = new IfRowHasFewerColumnsParameters();

    QuotedStringsParameters m_quotedStringsParams = new QuotedStringsParameters();

    LimitScannedRowsParameters m_limitScannedRowsParams = new LimitScannedRowsParameters();

    MaximumNumberOfColumnsParameters m_maxColumnsParams = new MaximumNumberOfColumnsParameters();

    LimitMemoryPerColumnParameters m_limitMemoryParams = new LimitMemoryPerColumnParameters();

    PrependFileIndexToRowIdParameters m_prependFileIndexParams = new PrependFileIndexToRowIdParameters();

    ConfigID saveToConfig(final CSVMultiTableReadConfig config) {
        final var tableReadConfig = config.getTableReadConfig();
        m_skipFirstDataRowsParams.saveToConfig(tableReadConfig);
        m_maxNumberOfRowsParams.saveToConfig(tableReadConfig);
        m_ifSchemaChangesParams.saveToConfig(config);
        m_multiFileReaderParams.saveToConfig(config);
        final var csvConfig = tableReadConfig.getReaderSpecificConfig();
        m_fileEncodingParams.saveToConfig(csvConfig);
        m_skipFirstLinesParams.saveToConfig(csvConfig);
        m_csvFormatParams.saveToConfig(csvConfig);
        m_autoDetectParams.saveToConfig(csvConfig);
        m_firstRowContainsColumnNamesParams.saveToConfig(tableReadConfig);
        m_firstColumnContainsRowIdsParams.saveToConfig(tableReadConfig);
        m_ifRowHasFewerColumnsParams.saveToConfig(tableReadConfig);
        m_quotedStringsParams.saveToConfig(csvConfig);
        m_limitScannedRowsParams.saveToConfig(tableReadConfig);
        m_maxColumnsParams.saveToConfig(csvConfig);
        m_limitMemoryParams.saveToConfig(csvConfig);
        m_prependFileIndexParams.saveToConfig(tableReadConfig);
        return config.getConfigID();
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_multiFileSelectionParams.validate();
        m_skipFirstDataRowsParams.validate();
        m_maxNumberOfRowsParams.validate();
        m_multiFileReaderParams.validate();

        m_fileEncodingParams.validate();
        m_skipFirstLinesParams.validate();
        m_csvFormatParams.validate();
        m_autoDetectParams.validate();
        m_limitScannedRowsParams.validate();
        m_maxColumnsParams.validate();
    }

    void saveToSource(final MultiFileSelectionPath sourceSettings) {
        m_multiFileSelectionParams.saveToSource(sourceSettings);
    }

    String getSourcePath() {
        return m_multiFileSelectionParams.m_source.getFSLocation().getPath();
    }

    MultiFileReaderParameters getMultiFileReaderParameters() {
        return m_multiFileReaderParams;
    }

}
