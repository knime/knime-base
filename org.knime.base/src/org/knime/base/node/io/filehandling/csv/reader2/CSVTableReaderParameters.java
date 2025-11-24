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
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.NodeParameters;

/**
 * CSV-specific parameters for the CSV Table Reader Node.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public class CSVTableReaderParameters implements NodeParameters {

    CSVTableReaderParameters(final URL url) {
        m_csvFormatParams = new CSVFormatParameters(url);
    }

    CSVTableReaderParameters() {
        // default constructor
    }

    public FileEncodingParameters m_fileEncodingParams = new FileEncodingParameters();

    public SkipFirstLinesOfFileParameters m_skipFirstLinesParams = new SkipFirstLinesOfFileParameters();

    public CSVFormatParameters m_csvFormatParams = new CSVFormatParameters();

    public AutoDetectCSVFormatParameters m_autoDetectParams = new AutoDetectCSVFormatParameters();

    public FirstRowContainsColumnNamesParameters m_firstRowContainsColumnNamesParams =
        new FirstRowContainsColumnNamesParameters();

    public FirstColumnContainsRowIdsParameters m_firstColumnContainsRowIdsParams =
        new FirstColumnContainsRowIdsParameters();

    public IfRowHasFewerColumnsParameters m_ifRowHasFewerColumnsParams = new IfRowHasFewerColumnsParameters();

    public QuotedStringsParameters m_quotedStringsParams = new QuotedStringsParameters();

    public LimitScannedRowsParameters m_limitScannedRowsParams = new LimitScannedRowsParameters();

    public MaximumNumberOfColumnsParameters m_maxColumnsParams = new MaximumNumberOfColumnsParameters();

    public LimitMemoryPerColumnParameters m_limitMemoryParams = new LimitMemoryPerColumnParameters();

    public PrependFileIndexToRowIdParameters m_prependFileIndexParams = new PrependFileIndexToRowIdParameters();

    void saveToConfig(final CSVMultiTableReadConfig config) {
        final var tableReadConfig = config.getTableReadConfig();
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
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_fileEncodingParams.validate();
        m_skipFirstLinesParams.validate();
        m_csvFormatParams.validate();
        m_autoDetectParams.validate();
        m_limitScannedRowsParams.validate();
        m_maxColumnsParams.validate();
    }

}
