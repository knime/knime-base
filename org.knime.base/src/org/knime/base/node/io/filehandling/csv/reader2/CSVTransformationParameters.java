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
 *   May 8, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.Optional;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.EscapeUtils;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.ColumnDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.CustomEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.CustomRowDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.DecimalSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.FileEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.FirstRowContainsColumnNamesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.LimitMemoryPerColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.MaxDataRowsScannedRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.MaximumNumberOfColumnsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.QuoteCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.QuoteEscapeCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.QuotedStringsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.QuotedStringsOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.ReplaceEmptyQuotedStringsByMissingValuesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.RowDelimiterOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeParameters.ThousandsSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonReaderTransformationParameters;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonTableReaderNodeParameters.FirstColumnContainsRowIdsRef;
import org.knime.base.node.io.filehandling.csv.reader2.common.CommonTableReaderNodeParameters.SkipFirstDataRowsRef;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.node.parameters.updates.ValueProvider;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
@Modification(CSVTransformationParametersStateProviders.TransformationSettingsWidgetModification.class)
final class CSVTransformationParameters
    extends CommonReaderTransformationParameters<CSVTransformationParameters.ConfigIdSettings, Class<?>> {

    CSVTransformationParameters() {
        super(new ConfigIdSettings());
    }

    static final class ConfigIdSettings
        extends CommonReaderTransformationParameters.ConfigIdSettings<CSVTableReaderConfig> {

        @ValueProvider(FirstRowContainsColumnNamesRef.class)
        boolean m_firstRowContainsColumnNames = true;

        @ValueProvider(FirstColumnContainsRowIdsRef.class)
        boolean m_firstColumnContainsRowIds;

        @ValueProvider(CommentStartRef.class)
        String m_commentLineCharacter = "#";

        @ValueProvider(ColumnDelimiterRef.class)
        String m_columnDelimiter = ",";

        @ValueProvider(QuoteCharacterRef.class)
        String m_quoteCharacter = "\"";

        @ValueProvider(QuoteEscapeCharacterRef.class)
        String m_quoteEscapeCharacter = "\"";

        @ValueProvider(RowDelimiterOptionRef.class)
        RowDelimiterOption m_rowDelimiterOption = RowDelimiterOption.LINE_BREAK;

        @ValueProvider(CustomRowDelimiterRef.class)
        String m_customRowDelimiter = "\n";

        @ValueProvider(QuotedStringsOptionRef.class)
        QuotedStringsOption m_quotedStringsOption = QuotedStringsOption.REMOVE_QUOTES_AND_TRIM;

        @ValueProvider(ReplaceEmptyQuotedStringsByMissingValuesRef.class)
        boolean m_replaceEmptyQuotedStringsByMissingValues = true;

        @ValueProvider(MaxDataRowsScannedRef.class)
        Optional<Long> m_maxDataRowsScanned = Optional.of(10000L);

        @ValueProvider(ThousandsSeparatorRef.class)
        String m_thousandsSeparator = "";

        @ValueProvider(DecimalSeparatorRef.class)
        String m_decimalSeparator = ".";

        @ValueProvider(FileEncodingRef.class)
        FileEncodingOption m_fileEncoding = FileEncodingOption.DEFAULT;

        @ValueProvider(CustomEncodingRef.class)
        String m_customEncoding = "";

        @ValueProvider(SkipFirstLinesRef.class)
        long m_skipFirstLines;

        @ValueProvider(SkipFirstDataRowsRef.class)
        long m_skipFirstDataRows;

        @ValueProvider(LimitMemoryPerColumnRef.class)
        boolean m_limitMemoryPerColumn = true;

        @ValueProvider(MaximumNumberOfColumnsRef.class)
        int m_maximumNumberOfColumns = 8192;

        @Override
        protected void applyToConfig(final DefaultTableReadConfig<CSVTableReaderConfig> config) {
            config.setColumnHeaderIdx(0);
//            config.setLimitRowsForSpec(m_limitScannedRows);
//            config.setMaxRowsForSpec(m_maxDataRowsScanned); TODO
            config.setSkipRows(m_skipFirstDataRows > 0);
            config.setNumRowsToSkip(m_skipFirstDataRows);
            config.setRowIDIdx(0);
            config.setUseColumnHeaderIdx(m_firstRowContainsColumnNames);
            config.setUseRowIDIdx(m_firstColumnContainsRowIds);

            final var csvConfig = config.getReaderSpecificConfig();
//            csvConfig.setCharSetName(
//                m_fileEncoding == FileEncodingOption.OTHER ? m_customEncoding : m_fileEncoding.m_persistId); TODO
            csvConfig.setComment(m_commentLineCharacter);
            csvConfig.setDecimalSeparator(m_decimalSeparator);
            csvConfig.setDelimiter(EscapeUtils.unescape(m_columnDelimiter));
            csvConfig.setLineSeparator(EscapeUtils.unescape(m_customRowDelimiter));
            csvConfig.setSkipLines(m_skipFirstLines > 0);
            csvConfig.setNumLinesToSkip(m_skipFirstLines);
            csvConfig.setQuote(m_quoteCharacter);
            csvConfig.setQuoteEscape(m_quoteEscapeCharacter);
            csvConfig.setQuoteOption(QuoteOption.valueOf(m_quotedStringsOption.name()));
            csvConfig.setReplaceEmptyWithMissing(m_replaceEmptyQuotedStringsByMissingValues);
            csvConfig.setThousandsSeparator(m_thousandsSeparator);
            csvConfig.useLineBreakRowDelimiter(m_rowDelimiterOption == RowDelimiterOption.LINE_BREAK);

            csvConfig.limitCharsPerColumn(m_limitMemoryPerColumn);
            csvConfig.setMaxColumns(m_maximumNumberOfColumns);
        }
    }

}
