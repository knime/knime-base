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
 *   Sep 20, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.fromDataTypeId;
import static org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.getDataTypeId;
import static org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.toSpecMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ConfigIdSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.PersistorSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.TypeChoicesProvider;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ConfigAndReader;
import org.knime.base.node.io.filehandling.webui.reader.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPersistorWithConfigKey;
import org.knime.filehandling.core.data.location.FSLocationValueMetaData;
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDLoader;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.NodeSettingsConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfigSerializer;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.UnknownColumnsTransformation;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"javadoc", "restriction"})
// TODO Javadoc
public abstract class CommonReaderTransformationSettingsPersistor<C extends ReaderSpecificConfig<C>, I extends ConfigIdSettings<C>, S, T, R extends CommonReaderTransformationSettings<I, S>>
    extends NodeSettingsPersistorWithConfigKey<R> implements ReaderSpecific.ProductionPathProviderAndTypeHierarchy<T>,
    ExternalDataTypeSerializer<S, T>, ConfigAndReader<C, T> {

    protected abstract R createDefaultTransformationSettings();

    protected abstract TableSpecConfigSerializer<T> createTableSpecConfigSerializer(ConfigIDLoader configIdLoader);

    private TableSpecConfigSerializer<T> createTableSpecConfigSerializer() {
        final ConfigIDLoader configIdLoader =
            s -> new NodeSettingsConfigID(s.getNodeSettings(getConfigIdSettingsKey()));
        return createTableSpecConfigSerializer(configIdLoader);
    }

    protected abstract String getConfigIdSettingsKey();

    @Override
    public R load(final NodeSettingsRO settings) throws InvalidSettingsException {
        final var transformationSettings = createDefaultTransformationSettings();
        if (settings.containsKey(getConfigKey())) {
            // TODO NOSONAR We do not need to load the PersistorSettings here since they are not needed in the frontend.
            // This will change once we tackle UIEXT-1740 and this code will be used by the node model.

            final var tableSpecConfigSerializer = createTableSpecConfigSerializer();
            final var tableSpecConfig = tableSpecConfigSerializer.load(settings.getNodeSettings(getConfigKey()));

            final var transformationElements =
                tableSpecConfig.getTableTransformation().stream().map(this::getTransformationElement).toList();
            final var unknownTransformationElement =
                getTransformationElement(tableSpecConfig.getTableTransformation().getTransformationForUnknownColumns());
            transformationSettings.m_columnTransformation =
                Stream.concat(transformationElements.stream(), Stream.of(unknownTransformationElement))
                    .sorted((t1, t2) -> t1.getFirst() - t2.getFirst()).map(Pair::getSecond)
                    .toArray(TransformationElementSettings[]::new);
            transformationSettings.m_persistorSettings.m_takeColumnsFrom =
                ColumnFilterMode.valueOf(settings.getNodeSettings(getConfigKey())
                    .getNodeSettings("table_transformation").getString("column_filter_mode"));
            transformationSettings.m_enforceTypes = settings.getNodeSettings(getConfigKey())
                .getNodeSettings("table_transformation").getBoolean("enforce_types", true);
        }
        return transformationSettings;
    }

    private Pair<Integer, TransformationElementSettings>
        getTransformationElement(final ColumnTransformation<T> knownTransformation) {
        return new Pair<>(knownTransformation.getPosition(), toTransformationElementSettings(knownTransformation));
    }

    private static Pair<Integer, TransformationElementSettings>
        getTransformationElement(final UnknownColumnsTransformation unknownColumnsTansformation) {
        return new Pair<>(unknownColumnsTansformation.getPosition(),
            toTransformationElementSettings(unknownColumnsTansformation));
    }

    private TransformationElementSettings toTransformationElementSettings(final ColumnTransformation<T> t) {
        final var defaultProductionPath =
            getProductionPathProvider().getDefaultProductionPath(t.getExternalSpec().getType());
        return new TransformationElementSettings( //
            t.getOriginalName(), //
            t.keep(), //
            t.getName(), //
            t.getProductionPath().getConverterFactory().getIdentifier(), //
            defaultProductionPath.getConverterFactory().getIdentifier(), //
            defaultProductionPath.getDestinationType().toPrettyString());
    }

    private static TransformationElementSettings toTransformationElementSettings(final UnknownColumnsTransformation t) {
        return new TransformationElementSettings(null, t.keep(), null, getForcedType(t),
            TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID, TypeChoicesProvider.DEFAULT_COLUMNTYPE_TEXT);
    }

    private static String getForcedType(final UnknownColumnsTransformation t) {
        if (t.forceType()) {
            return getDataTypeId(t.getForcedType());
        }
        return TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID;
    }

    @Override
    public void save(final R transformationSettings, final NodeSettingsWO settings) {
        final var persistorSettings = transformationSettings.m_persistorSettings;

        final var individualSpecs = toSpecMap(this, persistorSettings.m_specs);
        final var rawSpec = toRawSpec(individualSpecs);
        final var transformations = determineTransformations(transformationSettings, rawSpec);
        final var tableTransformation = new DefaultTableTransformation<T>(rawSpec, transformations.getFirst(),
            transformationSettings.m_persistorSettings.m_takeColumnsFrom, transformations.getSecond(),
            transformationSettings.m_enforceTypes, false);

        DataColumnSpec itemIdentifierColumnSpec = determineAppendPathColumnSpec(persistorSettings);

        final var config = getMultiTableReadConfig();
        persistorSettings.m_configId.applyToConfig(config.getTableReadConfig());
        final var tableSpecConfigSerializer = createTableSpecConfigSerializer();
        final var tableSpecConfig = DefaultTableSpecConfig.createFromTransformationModel(persistorSettings.m_sourceId,
            config.getConfigID(), individualSpecs, tableTransformation, itemIdentifierColumnSpec);
        tableSpecConfigSerializer.save(tableSpecConfig, settings.addNodeSettings(getConfigKey()));
    }

    private Pair<ArrayList<ColumnTransformation<T>>, UnknownColumnsTransformation>
        determineTransformations(final R transformationSettings, final RawSpec<T> rawSpec) {
        final Map<String, Integer> transformationIndexByColumnName = new HashMap<>();
        int unknownColumnsTransformationPosition = -1;
        for (int i = 0; i < transformationSettings.m_columnTransformation.length; i++) {
            final var columnName = transformationSettings.m_columnTransformation[i].m_columnName;
            if (columnName == null) {
                unknownColumnsTransformationPosition = i;
            } else {
                transformationIndexByColumnName.put(columnName, i);
            }
        }
        final var transformations = new ArrayList<ColumnTransformation<T>>();
        for (var column : rawSpec.getUnion()) {

            final var columnName = column.getName().get(); // NOSONAR in the TypedReaderTableSpecsProvider we make sure that names are always present
            if (columnName == null) {
                continue;
            }
            final var position = transformationIndexByColumnName.get(columnName);
            final var transformation = transformationSettings.m_columnTransformation[position];
            final var externalType = column.getType();
            final var productionPath = getProductionPathProvider().getAvailableProductionPaths(externalType).stream()
                .filter(p -> p.getConverterFactory().getIdentifier().equals(transformation.m_type)).findFirst()
                .orElseGet(() -> {
                    LOGGER.error(
                        String.format("The column '%s' can't be converted to the configured data type.", column));
                    return getProductionPathProvider().getDefaultProductionPath(externalType);
                });

            transformations.add(new ImmutableColumnTransformation<>(column, productionPath,
                transformation.m_includeInOutput, position, transformation.m_columnRename));
        }
        final var unknownColumnsTransformation = determineUnknownTransformation(
            transformationSettings.m_columnTransformation[unknownColumnsTransformationPosition],
            unknownColumnsTransformationPosition);
        return new Pair<>(transformations, unknownColumnsTransformation);
    }

    private static ImmutableUnknownColumnsTransformation determineUnknownTransformation(
        final TransformationElementSettings unknownTransformation, final int unknownColumnsTransformationPosition) {
        DataType forcedUnknownType = null;
        if (!unknownTransformation.m_type.equals(TypeChoicesProvider.DEFAULT_COLUMNTYPE_ID)) {
            forcedUnknownType = fromDataTypeId(unknownTransformation.m_type);
        }
        return new ImmutableUnknownColumnsTransformation(unknownColumnsTransformationPosition,
            unknownTransformation.m_includeInOutput, forcedUnknownType != null, forcedUnknownType);
    }

    // TODO NOSONAR De-duplicate this code when migrating the node model in UIEXT-1740
    private static DataColumnSpec determineAppendPathColumnSpec(final PersistorSettings<?, ?> persistorSettings) {
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
}
