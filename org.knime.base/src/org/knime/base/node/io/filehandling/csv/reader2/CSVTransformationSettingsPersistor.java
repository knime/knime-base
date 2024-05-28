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
 *   May 15, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.ClassTypeSerializer;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Encoding.Charset.FileEncodingOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeSettings.Settings.RowDelimiterOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.ColumnFilterModeOption;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElement;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDLoader;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec.TypedReaderTableSpecBuilder;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
final class CSVTransformationSettingsPersistor extends NodeSettingsPersistorWithConfigKey<CSVTransformationSettings> {

    private enum MultiTableReadConfigIdLoader implements ConfigIDLoader {
            ID_LOADER;

        @Override
        public ConfigID createFromSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new NodeSettingsConfigID(settings.getNodeSettings("multi_table_read"));
        }
    }

    private static final NodeLogger LOGGER = NodeLogger.getLogger(CSVTransformationSettingsPersistor.class);

    private final ProducerRegistry<Class<?>, ?> m_producerRegistry =
        StringReadAdapterFactory.INSTANCE.getProducerRegistry();

    @Override
    public CSVTransformationSettings load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var transformationSettings = new CSVTransformationSettings();
        if (settings.containsKey(getConfigKey())) {
            // We do not need to load the sourceId, configId and specs here since they are not needed in the frontend

            final var tableSpecConfigSerializer = TableSpecConfigSerializer.createStartingV42(m_producerRegistry,
                MultiTableReadConfigIdLoader.ID_LOADER, ClassTypeSerializer.SERIALIZER, String.class);
            final var tableSpecConfig = tableSpecConfigSerializer.load(settings.getNodeSettings(getConfigKey()));
            transformationSettings.m_columnTransformation =
                tableSpecConfig.getTableTransformation().stream()
                    .sorted((t1, t2) -> t1.getPosition() - t2.getPosition())
                    .map(t -> new TransformationElement(t.getOriginalName(), t.keep(), t.getName(),
                        t.getProductionPath().getConverterFactory().getIdentifier()))
                    .toArray(TransformationElement[]::new);

            transformationSettings.m_takeColumnsFrom =
                ColumnFilterModeOption.valueOf(settings.getNodeSettings(getConfigKey())
                    .getNodeSettings("table_transformation").getString("column_filter_mode"));
        }
        return transformationSettings;
    }

    @Override
    public void save(final CSVTransformationSettings transformationSettings, final NodeSettingsWO settings) {
        final var config = new CSVMultiTableReadConfig();
        final var csvConfig = config.getReaderSpecificConfig();
        final var tc = config.getTableReadConfig();
        // basic settings
        tc.setUseColumnHeaderIdx(transformationSettings.m_configId.m_firstRowContainsColumnNames);
        tc.setUseRowIDIdx(transformationSettings.m_configId.m_firstColumnContainsRowIds);

        csvConfig.setComment(transformationSettings.m_configId.m_commentLineCharacter);
        csvConfig.setDelimiter(transformationSettings.m_configId.m_columnDelimiter);
        csvConfig.setQuote(transformationSettings.m_configId.m_quoteCharacter);
        csvConfig.setQuoteEscape(transformationSettings.m_configId.m_quoteEscapeCharacter);
        csvConfig.setLineSeparator(transformationSettings.m_configId.m_customRowDelimiter);
        csvConfig.useLineBreakRowDelimiter(
            transformationSettings.m_configId.m_rowDelimiterOption == RowDelimiterOption.LINE_BREAK);

        // advanced settings
        csvConfig.setQuoteOption(QuoteOption.valueOf(transformationSettings.m_configId.m_quotedStringsOption.name()));
        csvConfig
            .setReplaceEmptyWithMissing(transformationSettings.m_configId.m_replaceEmptyQuotedStringsByMissingValues);
        tc.setLimitRows(transformationSettings.m_configId.m_limitScannedRows);
        tc.setMaxRows(transformationSettings.m_configId.m_maxDataRowsScanned);
        csvConfig.setThousandsSeparator(transformationSettings.m_configId.m_thousandsSeparator);
        csvConfig.setDecimalSeparator(transformationSettings.m_configId.m_decimalSeparator);

        // encoding
        final var encoding = transformationSettings.m_configId.m_fileEncoding;
        csvConfig.setCharSetName(encoding == FileEncodingOption.OTHER
            ? transformationSettings.m_configId.m_customEncoding : encoding.m_persistId);

        // limit rows
        csvConfig.setSkipLines(transformationSettings.m_configId.m_skipFirstLines > 0);
        csvConfig.setNumLinesToSkip(transformationSettings.m_configId.m_skipFirstLines);
        tc.setSkipRows(transformationSettings.m_configId.m_skipFirstDataRows > 0);
        tc.setNumRowsToSkip(transformationSettings.m_configId.m_skipFirstDataRows);

        final var individualSpecs = toSpecMap(transformationSettings.m_specs);
        final var rawSpec = CSVTransformationElementsProvider.toRawSpec(individualSpecs);

        final var transformationIndexByColumnName =
            IntStream.range(0, transformationSettings.m_columnTransformation.length).boxed().collect(Collectors
                .toMap(i -> transformationSettings.m_columnTransformation[i].m_columnName, Function.identity()));
        final var transformations = new ArrayList<ColumnTransformation<Class<?>>>();
        for (var column : rawSpec.getUnion()) {

            final var columnName = column.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present
            final var position = transformationIndexByColumnName.get(columnName);

            final var transformation = transformationSettings.m_columnTransformation[position];
            final var externalType = column.getType();
            final var productionPath =
                CSVTransformationSettings.PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(externalType).stream()
                    .filter(p -> p.getConverterFactory().getIdentifier().equals(transformation.m_type)).findFirst()
                    .orElseGet(() -> {
                        LOGGER.error(
                            String.format("The column '%s' can't be converted to the configured data type.", column));
                        return CSVTransformationSettings.PRODUCTION_PATH_PROVIDER
                            .getDefaultProductionPath(externalType);
                    });

            transformations.add(new ImmutableColumnTransformation<>(column, productionPath,
                transformation.m_includeInOutput, position, transformation.m_columnRename));
        }
        // TODO NOSONAR UIEXT-1914 parameters should be dependent on <any unknown new column> transformation
        final var unknownColsTrans = new ImmutableUnknownColumnsTransformation(1, true, false, null);

        final var configID = config.getConfigID();
        final var tableTransformation = new DefaultTableTransformation<Class<?>>(rawSpec, transformations,
            transformationSettings.m_takeColumnsFrom.toColumnFilterMode(), unknownColsTrans, true,
            config.skipEmptyColumns());
        // TODO if path column is appended, this should be non-null, see
        // DefaultMultiTableReadFactory.createItemIdentifierColumn
        final DataColumnSpec itemIdentifierColumnSpec = null;

        final var tableSpecConfigSerializer = TableSpecConfigSerializer.createStartingV42(m_producerRegistry,
            MultiTableReadConfigIdLoader.ID_LOADER, ClassTypeSerializer.SERIALIZER, String.class);
        final var tableSpecConfig =
            DefaultTableSpecConfig.createFromTransformationModel(transformationSettings.m_sourceId, configID,
                individualSpecs, tableTransformation, itemIdentifierColumnSpec);
        tableSpecConfigSerializer.save(tableSpecConfig, settings.addNodeSettings(getConfigKey()));
    }

    final static Map<String, TypedReaderTableSpec<Class<?>>> toSpecMap(final CSVTableSpec[] specs) {
        final var individualSpecs = new LinkedHashMap<String, TypedReaderTableSpec<Class<?>>>();
        for (final var tableSpec : specs) {
            final TypedReaderTableSpecBuilder<Class<?>> specBuilder = TypedReaderTableSpec.builder();
            for (final var colSpec : tableSpec.m_spec) {
                specBuilder.addColumn(colSpec.m_name, colSpec.m_type, true);
            }
            final var spec = specBuilder.build();
            individualSpecs.put(tableSpec.m_sourceId, spec);
        }
        return individualSpecs;
    }
}
