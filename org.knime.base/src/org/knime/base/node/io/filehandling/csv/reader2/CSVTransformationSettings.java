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

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.AppendPathColumnRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.DecimalSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.FilePathColumnNameRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.LimitScannedRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.MaxDataRowsScannedRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.QuotedStringsOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.ReplaceEmptyQuotedStringsByMissingValuesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.AdvancedSettings.ThousandsSeparatorRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.CustomEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstDataRowsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.LimitRows.SkipFirstLinesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.ColumnDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CommentStartRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.CustomRowDelimiterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FirstColumnContainsRowIdsRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.FirstRowContainsColumnNamesRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.QuoteCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.QuoteEscapeCharacterRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOptionRef;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TableSpecSettingsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TransformationElementSettingsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.FSLocationsProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.SourceIdProvider;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ArrayWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class CSVTransformationSettings implements WidgetGroup, PersistableSettings {

    static final ProductionPathProvider<Class<?>> PRODUCTION_PATH_PROVIDER =
        StringReadAdapterFactory.INSTANCE.createProductionPathProvider();

    static final TreeTypeHierarchy<Class<?>, Class<?>> TYPE_HIERARCHY = StringReadAdapterFactory.TYPE_HIERARCHY;

    static final class ConfigIdSettings implements WidgetGroup, PersistableSettings {

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

        @ValueProvider(LimitScannedRowsRef.class)
        boolean m_limitScannedRows = true;

        @ValueProvider(MaxDataRowsScannedRef.class)
        long m_maxDataRowsScanned = 10000;

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

        void applyToConfig(final DefaultTableReadConfig<CSVTableReaderConfig> config) {
            config.setColumnHeaderIdx(0);
            config.setLimitRowsForSpec(m_limitScannedRows);
            config.setMaxRowsForSpec(m_maxDataRowsScanned);
            config.setSkipRows(m_skipFirstDataRows > 0);
            config.setNumRowsToSkip(m_skipFirstDataRows);
            config.setRowIDIdx(0);
            config.setUseColumnHeaderIdx(m_firstRowContainsColumnNames);
            config.setUseRowIDIdx(m_firstColumnContainsRowIds);

            final var csvConfig = config.getReaderSpecificConfig();
            csvConfig.setCharSetName(
                m_fileEncoding == FileEncodingOption.OTHER ? m_customEncoding : m_fileEncoding.m_persistId);
            csvConfig.setComment(m_commentLineCharacter);
            csvConfig.setDecimalSeparator(m_decimalSeparator);
            csvConfig.setDelimiter(m_columnDelimiter);
            csvConfig.setLineSeparator(m_customRowDelimiter);
            csvConfig.setSkipLines(m_skipFirstLines > 0);
            csvConfig.setNumLinesToSkip(m_skipFirstLines);
            csvConfig.setQuote(m_quoteCharacter);
            csvConfig.setQuoteEscape(m_quoteEscapeCharacter);
            csvConfig.setQuoteOption(QuoteOption.valueOf(m_quotedStringsOption.name()));
            csvConfig.setReplaceEmptyWithMissing(m_replaceEmptyQuotedStringsByMissingValues);
            csvConfig.setThousandsSeparator(m_thousandsSeparator);
            csvConfig.useLineBreakRowDelimiter(m_rowDelimiterOption == RowDelimiterOption.LINE_BREAK);
        }
    }

    static final class ColumnSpecSettings implements WidgetGroup, PersistableSettings {

        String m_name;

        Class<?> m_type;

        ColumnSpecSettings(final String name, final Class<?> type) {
            m_name = name;
            m_type = type;
        }

        ColumnSpecSettings() {
        }
    }

    static final class TableSpecSettings implements WidgetGroup, PersistableSettings {

        String m_sourceId;

        ColumnSpecSettings[] m_spec;

        TableSpecSettings(final String sourceId, final ColumnSpecSettings[] spec) {
            m_sourceId = sourceId;
            m_spec = spec;
        }

        TableSpecSettings() {
        }
    }

    /**
     * TODO NOSONAR UIEXT-1946 These settings are sent to the frontend where they are not needed. They are merely held
     * here to be used in the CSVTransformationSettingsPersistor. We should look for an alternative mechanism to provide
     * these settings to the persistor.
     */
    static final class PersistorSettings implements WidgetGroup, PersistableSettings {

        static class ConfigIdReference implements Reference<ConfigIdSettings> {
        }

        @ValueReference(ConfigIdReference.class)
        ConfigIdSettings m_configId = new ConfigIdSettings();

        @ValueProvider(SourceIdProvider.class)
        String m_sourceId = "";

        @ValueProvider(FSLocationsProvider.class)
        FSLocation[] m_fsLocations = new FSLocation[0];

        @ValueProvider(TableSpecSettingsProvider.class)
        TableSpecSettings[] m_specs = new TableSpecSettings[0];

        @ValueProvider(AppendPathColumnRef.class)
        boolean m_appendPathColumn;

        @ValueProvider(FilePathColumnNameRef.class)
        String m_filePathColumnName = "File Path";
    }

    PersistorSettings m_persistorSettings = new PersistorSettings();

    enum ColumnFilterModeOption {
            @Label(value = "Union", description = """
                    Any column that is part of any input file is considered. If a file is missing a column, it's filled
                    up with missing values.
                    """)
            UNION(ColumnFilterMode.UNION),

            @Label(value = "Intersection",
                description = "Only columns that appear in all files are considered for the output table.") //
            INTERSECTION(ColumnFilterMode.INTERSECTION);

        private final ColumnFilterMode m_columnFilterMode;

        ColumnFilterModeOption(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
        }

        ColumnFilterMode toColumnFilterMode() {
            return m_columnFilterMode;
        }
    }

    @Widget(title = "Take columns from", description = """
            Only enabled in "Files in folder" mode. Specifies which set of columns are considered for the output table.
            """)
    @ValueSwitchWidget
    // TODO NOSONAR UIEXT-1800 merge with CSVTableReaderNoderSettings.m_failOnDifferingSpecs
    ColumnFilterModeOption m_takeColumnsFrom = ColumnFilterModeOption.UNION;

    static class TransformationElementSettings implements WidgetGroup, PersistableSettings {

        static class ColumnNameRef implements Reference<String> {
        }

        @ValueReference(ColumnNameRef.class)
        String m_columnName;

        @Widget(title = "Include in output", description = "") // TODO NOSONAR UIEXT-1901 add description
        boolean m_includeInOutput;

        @Widget(title = "Column name", description = "", hideTitle = true) // TODO NOSONAR UIEXT-1901 add description
        String m_columnRename;

        @Widget(title = "Column type", description = "") // TODO NOSONAR UIEXT-1901 add description
        @ChoicesWidget(choicesProvider = TypeChoicesProvider.class)
        String m_type;

        TransformationElementSettings() {
        }

        TransformationElementSettings(final String columnName, final boolean includeInOutput, final String columnRename,
            final String type) {
            m_columnName = columnName;
            m_includeInOutput = includeInOutput;
            m_columnRename = columnRename;
            m_type = type;
        }
    }

    @Widget(title = "Transformations", description = """
            Use this option to modify the structure of the table. You can deselect each column to filter it out of the
            output table, use the arrows to reorder the columns, or change the column name or column type of each
            column. Note that the positions of columns are reset in the dialog if a new file or folder is selected.
            Whether and where to add unknown columns during execution is specified via the special row &lt;any unknown
            new column&gt;. It is also possible to select the type new columns should be converted to. Note that the
            node will fail if this conversion is not possible e.g. if the selected type is Integer but the new column is
            of type Double.
            """)
    // TODO NOSONAR UIEXT-1901 this description is currently not shown
    @ArrayWidget(elementTitle = "Column", showSortButtons = true, hasFixedSize = true)
    @ValueProvider(TransformationElementSettingsProvider.class)
    // TODO NOSONAR UIEXT-1914 the <any unknown new column> is not implemented yet
    TransformationElementSettings[] m_columnTransformation = new TransformationElementSettings[0];
}
