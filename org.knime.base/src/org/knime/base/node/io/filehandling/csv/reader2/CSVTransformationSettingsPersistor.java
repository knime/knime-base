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

import static org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PRODUCTION_PATH_PROVIDER;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader.ClassTypeSerializer;
import org.knime.base.node.io.filehandling.csv.reader.api.StringReadAdapterFactory;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.PersistorSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TableSpecSettings;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTransformationSettings.TransformationElementSettings;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDLoader;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
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

    private static final ProducerRegistry<Class<?>, ?> PRODUCER_REGISTRY =
        StringReadAdapterFactory.INSTANCE.getProducerRegistry();

    @Override
    public CSVTransformationSettings load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var transformationSettings = new CSVTransformationSettings();
        if (settings.containsKey(getConfigKey())) {
            // TODO NOSONAR We do not need to load the PersistorSettings here since they are not needed in the frontend.
            // This will change once we tackle UIEXT-1740 and this code will be used by the node model.

            final var tableSpecConfigSerializer = TableSpecConfigSerializer.createStartingV42(PRODUCER_REGISTRY,
                MultiTableReadConfigIdLoader.ID_LOADER, ClassTypeSerializer.SERIALIZER, String.class);
            final var tableSpecConfig = tableSpecConfigSerializer.load(settings.getNodeSettings(getConfigKey()));
            transformationSettings.m_columnTransformation = tableSpecConfig.getTableTransformation().stream()
                .sorted((t1, t2) -> t1.getPosition() - t2.getPosition()).map(t -> {
                    final var defaultProductionPath =
                        PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(t.getExternalSpec().getType());
                    return new TransformationElementSettings( //
                        t.getOriginalName(), //
                        t.keep(), //
                        t.getName(), //
                        t.getProductionPath().getConverterFactory().getIdentifier(), //
                        defaultProductionPath.getConverterFactory().getIdentifier(), //
                        defaultProductionPath.getDestinationType().toPrettyString() //
                    );
                }).toArray(TransformationElementSettings[]::new);

            transformationSettings.m_takeColumnsFrom = settings.getNodeSettings(getConfigKey())
                .getNodeSettings("table_transformation").getString("column_filter_mode");
            transformationSettings.m_enforceTypes = settings.getNodeSettings(getConfigKey())
                .getNodeSettings("table_transformation").getBoolean("enforce_types", true);
        }
        return transformationSettings;
    }

    @Override
    public void save(final CSVTransformationSettings transformationSettings, final NodeSettingsWO settings) {
        final var persistorSettings = transformationSettings.m_persistorSettings;

        final var individualSpecs = toSpecMap(persistorSettings.m_specs);
        final var rawSpec = CSVTransformationSettingsStateProviders.toRawSpec(individualSpecs);

        final var transformations = determineTransformations(transformationSettings, rawSpec);
        // TODO NOSONAR UIEXT-1914 parameters should be dependent on <any unknown new column> transformation
        final var unknownColsTrans = new ImmutableUnknownColumnsTransformation(1, true, false, null);

        final var tableTransformation = new DefaultTableTransformation<Class<?>>(rawSpec, transformations,
            ColumnFilterMode.valueOf(transformationSettings.m_takeColumnsFrom), unknownColsTrans,
            transformationSettings.m_enforceTypes, false);

        DataColumnSpec itemIdentifierColumnSpec = determineAPpendPathColumnSpec(persistorSettings);

        final var config = new CSVMultiTableReadConfig();
        persistorSettings.m_configId.applyToConfig(config.getTableReadConfig());
        final var tableSpecConfigSerializer = TableSpecConfigSerializer.createStartingV42(PRODUCER_REGISTRY,
            MultiTableReadConfigIdLoader.ID_LOADER, ClassTypeSerializer.SERIALIZER, String.class);
        final var tableSpecConfig = DefaultTableSpecConfig.createFromTransformationModel(persistorSettings.m_sourceId,
            config.getConfigID(), individualSpecs, tableTransformation, itemIdentifierColumnSpec);
        tableSpecConfigSerializer.save(tableSpecConfig, settings.addNodeSettings(getConfigKey()));
    }

    private static ArrayList<ColumnTransformation<Class<?>>> determineTransformations(
        final CSVTransformationSettings transformationSettings, final RawSpec<Class<?>> rawSpec) {
        final var transformationIndexByColumnName =
            IntStream.range(0, transformationSettings.m_columnTransformation.length).boxed().collect(Collectors
                .toMap(i -> transformationSettings.m_columnTransformation[i].m_columnName, Function.identity()));
        final var transformations = new ArrayList<ColumnTransformation<Class<?>>>();
        for (var column : rawSpec.getUnion()) {

            final var columnName = column.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present
            final var position = transformationIndexByColumnName.get(columnName);

            final var transformation = transformationSettings.m_columnTransformation[position];
            final var externalType = column.getType();
            final var productionPath = PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(externalType).stream()
                .filter(p -> p.getConverterFactory().getIdentifier().equals(transformation.m_type)).findFirst()
                .orElseGet(() -> {
                    LOGGER.error(
                        String.format("The column '%s' can't be converted to the configured data type.", column));
                    return PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(externalType);
                });

            transformations.add(new ImmutableColumnTransformation<>(column, productionPath,
                transformation.m_includeInOutput, position, transformation.m_columnRename));
        }
        return transformations;
    }

    // TODO NOSONAR De-duplicate this code when migrating the node model in UIEXT-1740
    private static DataColumnSpec determineAPpendPathColumnSpec(final PersistorSettings persistorSettings) {
        // code taken from DefaultMultiTableReadFactory.createItemIdentifierColumn
        if (persistorSettings.m_appendPathColumn) {
            DataColumnSpecCreator itemIdColCreator = null;
            for (var path : persistorSettings.m_fsLocations) {
                // code taken from TableReader.createIdentifierColumnSpec
                final DataColumnSpecCreator creator =
                    new DataColumnSpecCreator(persistorSettings.m_filePathColumnName, SimpleFSLocationCellFactory.TYPE);
                creator.addMetaData(new FSLocationValueMetaData(path.getFileSystemCategory(),
                    path.getFileSystemSpecifier().orElse(null)), true);
                final var itemIdCol = creator.createSpec();
                if (itemIdColCreator == null) {
                    itemIdColCreator = new DataColumnSpecCreator(itemIdCol);
                } else {
                    itemIdColCreator.merge(itemIdCol);
                }
            }
            if (itemIdColCreator != null) {
                return itemIdColCreator.createSpec();
            }
        }
        return null;
    }

    static Map<String, TypedReaderTableSpec<Class<?>>> toSpecMap(final TableSpecSettings[] specs) {
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
