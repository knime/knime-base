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
 *   Dec 4, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

import com.google.common.collect.Iterators;

/**
 * Handles loading, saving and validation of {@link DefaultTableSpecConfig}
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type used to identify external types
 */
public final class DefaultTableSpecConfigSerializer<T> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTableSpecConfigSerializer.class);

    private static final String CFG_CONFIG_ID = "config_id";

    /** Enforce types key. */
    private static final String CFG_ENFORCE_TYPES = "enforce_types";

    /** Include unknown column config key. */
    private static final String CFG_INCLUDE_UNKNOWN = "include_unknown_columns" + SettingsModel.CFGKEY_INTERNAL;

    /** New column position config key. */
    private static final String CFG_NEW_COLUMN_POSITION = "unknown_column_position" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_KEEP = "keep" + SettingsModel.CFGKEY_INTERNAL;

    /** Individual spec config key. */
    private static final String CFG_INDIVIDUAL_SPECS = "individual_specs" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_INDIVIDUAL_SPEC = "individual_spec_";

    private static final String CFG_HAS_TYPE = "has_type" + SettingsModel.CFGKEY_INTERNAL;

    /** Root path/item config key. */
    private static final String CFG_ROOT_PATH = "root_path" + SettingsModel.CFGKEY_INTERNAL;

    /** File path config key. */
    private static final String CFG_FILE_PATHS = "file_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATHS = "production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NUM_PRODUCTION_PATHS = "num_production_paths" + SettingsModel.CFGKEY_INTERNAL;

    /** Table spec config key. */
    private static final String CFG_DATATABLE_SPEC = "datatable_spec" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATH = "production_path_";

    private static final String CFG_ORIGINAL_NAMES = "original_names" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_POSITIONAL_MAPPING = "positional_mapping" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_COLUMN_FILTER_MODE = "column_filter_mode" + SettingsModel.CFGKEY_INTERNAL;

    private final ProductionPathLoader m_productionPathLoader;

    private final T m_mostGenericType;

    private final ConfigIDLoader m_configIDLoader;

    /**
     * Constructor.
     *
     * @param productionPathLoader the {@link ProductionPathLoader} to use for loading {@link ProductionPath
     *            ProductionPaths}
     * @param mostGenericType the most generic type in the hierarchy (used only for workflows created in 4.2)
     * @param configIDLoader loads the {@link ConfigID}
     */
    public DefaultTableSpecConfigSerializer(final ProductionPathLoader productionPathLoader, final T mostGenericType,
        final ConfigIDLoader configIDLoader) {
        m_productionPathLoader = productionPathLoader;
        m_mostGenericType = mostGenericType;
        m_configIDLoader = configIDLoader;
    }

    /**
     * Constructor.
     *
     * @param producerRegistry the {@link ProducerRegistry} to use for loading {@link ProductionPath ProductionPaths}
     * @param mostGenericType the most generic type in the hierarchy (used only for workflows created in 4.2)
     * @param configIDLoader loads the {@link ConfigID}
     */
    public DefaultTableSpecConfigSerializer(final ProducerRegistry<?, ?> producerRegistry, final T mostGenericType,
        final ConfigIDLoader configIDLoader) {
        this(new DefaultProductionPathLoader(producerRegistry), mostGenericType, configIDLoader);
    }

    /**
     * De-serializes the {@link DefaultTableSpecConfig} previously written to the given settings.
     *
     * @param settings containing the serialized {@link DefaultTableSpecConfig}
     * @param externalConfig the external config
     * @return the de-serialized {@link DefaultTableSpecConfig}
     * @throws InvalidSettingsException - if the settings do not exists / cannot be loaded
     */
    public DefaultTableSpecConfig<T> load(final NodeSettingsRO settings, final ExternalConfig externalConfig)
        throws InvalidSettingsException {
        final ConfigID configID = loadID(settings);
        final String sourceGroupID = settings.getString(CFG_ROOT_PATH);
        final String[] items = settings.getStringArray(CFG_FILE_PATHS);
        final List<ReaderTableSpec<?>> individualSpecs =
            loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), items.length);
        final Set<String> allColumns = union(individualSpecs);
        final ImmutableTableTransformation<T> tableTransformation =
            loadTableTransformation(settings, allColumns, externalConfig, individualSpecs);
        final Collection<TypedReaderTableSpec<T>> typedIndividualSpecs =
            assignTypes(individualSpecs, tableTransformation);
        return new DefaultTableSpecConfig<>(sourceGroupID, configID, items, typedIndividualSpecs, tableTransformation);
    }

    private ConfigID loadID(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (settings.containsKey(CFG_CONFIG_ID)) {
            return m_configIDLoader.createFromSettings(settings.getNodeSettings(CFG_CONFIG_ID));
        } else {
            // the node was created in 4.3 -> no config id is available therefore we return the empty config id
            return EmptyConfigID.INSTANCE;
        }
    }

    private Collection<TypedReaderTableSpec<T>> assignTypes(final Collection<ReaderTableSpec<?>> individualSpecs,
        final TableTransformation<T> transformation) {
        final TypeAssigner<T> typeAssigner = new TypeAssigner<>(transformation);
        return individualSpecs.stream()//
            .map(typeAssigner::assignType)//
            .collect(toList());
    }

    private static class TypeAssigner<T> {
        private final Map<String, TypedReaderColumnSpec<T>> m_nameToSpec;

        TypeAssigner(final TableTransformation<T> transformation) {
            m_nameToSpec = transformation.stream().map(ColumnTransformation::getExternalSpec)
                .collect(toMap(MultiTableUtils::getNameAfterInit, Function.identity()));
        }

        TypedReaderTableSpec<T> assignType(final ReaderTableSpec<? extends ReaderColumnSpec> untyped) {
            return new TypedReaderTableSpec<>(untyped.stream()//
                .map(MultiTableUtils::getNameAfterInit)//
                .map(m_nameToSpec::get)//
                .collect(toList()));
        }
    }

    private ImmutableTableTransformation<T> loadTableTransformation(final NodeSettingsRO settings,
        final Set<String> allColumns, final ExternalConfig externalConfig,
        final Collection<ReaderTableSpec<?>> individualSpecs) throws InvalidSettingsException {

        final boolean includeUnknownColumns = settings.getBoolean(CFG_INCLUDE_UNKNOWN, true);
        final boolean enforceTypes = settings.getBoolean(CFG_ENFORCE_TYPES, false);
        final ColumnFilterMode columnFilterMode = loadColumnFilterMode(settings, externalConfig.getSpecMergeMode());

        // For old workflows (created with 4.2), the spec might not contain all columns contained in union if
        // SpecMergeMode#INTERSECTION was used to create the final spec
        final DataTableSpec loadedKnimeSpec = DataTableSpec.load(settings.getConfig(CFG_DATATABLE_SPEC));
        final DataTableSpec fullKnimeSpec = constructFullKnimeSpec(allColumns, loadedKnimeSpec);

        ProductionPath[] allProdPaths = loadProductionPaths(settings, allColumns, loadedKnimeSpec);
        final int newColPosition = settings.getInt(CFG_NEW_COLUMN_POSITION, allProdPaths.length);

        final String[] originalNames = loadOriginalNames(fullKnimeSpec, settings);
        final int[] positions = loadPositionalMapping(fullKnimeSpec.getNumColumns(), settings);
        final boolean[] keeps = loadKeep(loadedKnimeSpec, allColumns, settings);
        final boolean[] hasType = loadHasType(allColumns, settings);

        final List<ImmutableColumnTransformation<T>> transformations = new ArrayList<>(allProdPaths.length);
        for (int i = 0; i < allProdPaths.length; i++) {
            final ProductionPath productionPath = allProdPaths[i];
            @SuppressWarnings("unchecked")
            final T externalType = (T)productionPath.getProducerFactory().getSourceType();
            final TypedReaderColumnSpec<T> externalSpec =
                TypedReaderColumnSpec.createWithName(originalNames[i], externalType, hasType[i]);
            final int position = positions[i];
            final String outputName = fullKnimeSpec.getColumnSpec(i).getName();
            final boolean keep = keeps[i];
            final ImmutableColumnTransformation<T> trans =
                new ImmutableColumnTransformation<>(externalSpec, productionPath, keep, position, outputName);
            transformations.add(trans);
        }
        final RawSpec<T> rawSpec = createRawSpec(fullKnimeSpec, allProdPaths, originalNames, individualSpecs, hasType);

        return new ImmutableTableTransformation<>(transformations, rawSpec, columnFilterMode, newColPosition,
            includeUnknownColumns, enforceTypes, externalConfig.skipEmptyColumns());
    }

    private static <T> RawSpec<T> createRawSpec(final DataTableSpec rawKnimeSpec,
        final ProductionPath[] productionPaths, final String[] originalNames,
        final Collection<ReaderTableSpec<?>> individualSpecs, final boolean[] hasType) {
        assert rawKnimeSpec
            .getNumColumns() == productionPaths.length : "Number of production paths doesn't match the number of columns.";
        final List<TypedReaderColumnSpec<T>> specs = new ArrayList<>(rawKnimeSpec.getNumColumns());
        for (int i = 0; i < rawKnimeSpec.getNumColumns(); i++) {
            final ProductionPath productionPath = productionPaths[i];
            @SuppressWarnings("unchecked") // the production path stores the source type of type T
            final T type = (T)productionPath.getProducerFactory().getSourceType();
            specs.add(TypedReaderColumnSpec.createWithName(originalNames[i], type, hasType[i]));
        }
        final TypedReaderTableSpec<T> union = new TypedReaderTableSpec<>(specs);
        final TypedReaderTableSpec<T> intersection = findIntersection(union, individualSpecs);
        return new RawSpec<>(union, intersection);
    }

    private static <T> TypedReaderTableSpec<T> findIntersection(final TypedReaderTableSpec<T> union,
        final Collection<ReaderTableSpec<?>> individualSpecs) {
        final Set<String> commonNames = union(individualSpecs);
        for (ReaderTableSpec<?> individualSpec : individualSpecs) {
            final Set<String> currentCommonNames = new HashSet<>(commonNames);
            for (ReaderColumnSpec column : individualSpec) {
                currentCommonNames.remove(MultiTableUtils.getNameAfterInit(column));
            }
            currentCommonNames.forEach(commonNames::remove);
        }
        return new TypedReaderTableSpec<>(union.stream()//
            .filter(c -> commonNames.contains(MultiTableUtils.getNameAfterInit(c)))//
            .collect(Collectors.toList()));
    }

    /**
     * @param settings {@link NodeSettingsRO} to read from
     * @param specMergeModeOld the old {@link SpecMergeMode}
     * @return the {@link ColumnFilterMode} to use
     * @throws InvalidSettingsException
     */
    private static ColumnFilterMode loadColumnFilterMode(final NodeSettingsRO settings,
        @SuppressWarnings("deprecation") final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        try {
            return ColumnFilterMode.valueOf(settings.getString(CFG_COLUMN_FILTER_MODE));
        } catch (InvalidSettingsException ise) {
            LOGGER.debug("The settings contained no ColumnFilterMode.", ise);
            CheckUtils.checkSetting(specMergeModeOld != null,
                "The settings are missing both the SpecMergeMode (4.2) and the ColumnFilterMode (4.3 and later).");
            @SuppressWarnings({"null", "deprecation"}) // checked above
            final ColumnFilterMode columnFilterMode = specMergeModeOld.getColumnFilterMode();
            return columnFilterMode;
        }
    }

    /**
     * @param spec {@link DataTableSpec}
     * @param allColumns all column names
     * @param settings {@link NodeSettingsRO} to read from
     * @return the keep flag array
     */
    private static boolean[] loadKeep(final DataTableSpec spec, final Set<String> allColumns,
        final NodeSettingsRO settings) {
        final boolean[] keep = settings.getBooleanArray(CFG_KEEP, (boolean[])null);
        if (keep == null) {
            /**
             * Settings stored before 4.3 didn't have this settings, so we need to reconstruct it. Before 4.3
             * potentially not all columns were contained in spec in case the user selected the SpecMergeMode
             * intersection. Hence we reconstruct this behavior by checking which columns of the individual specs were
             * contained in the output and which were not.
             */
            return createKeepForOldWorkflows(spec, allColumns);
        } else {
            return keep;
        }
    }

    private static boolean[] loadHasType(final Set<String> allColumns, final NodeSettingsRO settings) {
        final boolean[] defaultVal = new boolean[allColumns.size()];
        // we did not store this information with 4.3 (and earlier) and always assumed true
        Arrays.fill(defaultVal, true);
        return settings.getBooleanArray(CFG_HAS_TYPE, defaultVal);
    }

    /**
     * Before 4.3 potentially not all columns were contained in spec in case the user selected the SpecMergeMode
     * intersection.
     *
     * @param spec the output {@link DataTableSpec} which might not contain all columns contained in
     *            <b>individualSpecs</b>
     * @param individualSpecs the column names contained in the individual files
     * @return the reconstructed keep array
     */
    private static boolean[] createKeepForOldWorkflows(final DataTableSpec spec, final Set<String> allColumns) {

        final boolean[] keep = new boolean[allColumns.size()];
        int i = 0;
        for (String colName : allColumns) {
            keep[i] = spec.containsName(colName);
            i++;
        }
        return keep;
    }

    /**
     * @param allColumns all column names
     * @param loadedKnimeSpec {@link DataTableSpec}
     * @return reconstructed KNIME {@link DataTableSpec}
     */
    private static DataTableSpec constructFullKnimeSpec(final Set<String> allColumns,
        final DataTableSpec loadedKnimeSpec) {
        if (allColumns.size() == loadedKnimeSpec.getNumColumns()) {
            return loadedKnimeSpec;
        } else {
            return reconstructFullKnimeSpec(allColumns, loadedKnimeSpec);
        }
    }

    private static DataTableSpec reconstructFullKnimeSpec(final Set<String> allColumns,
        final DataTableSpec loadedKnimeSpec) {
        final DataTableSpecCreator fullKnimeSpecCreator = new DataTableSpecCreator();
        for (String col : allColumns) {
            if (loadedKnimeSpec.containsName(col)) {
                fullKnimeSpecCreator.addColumns(loadedKnimeSpec.getColumnSpec(col));
            } else {
                fullKnimeSpecCreator.addColumns(new DataColumnSpecCreator(col, StringCell.TYPE).createSpec());
            }
        }
        return fullKnimeSpecCreator.createSpec();
    }

    /**
     * @param individualSpecs individual {@link ReaderTableSpec}s
     * @return the union of the column names
     */
    private static Set<String> union(final Collection<ReaderTableSpec<?>> individualSpecs) {
        final Set<String> allColumns = new LinkedHashSet<>();
        for (ReaderTableSpec<?> ts : individualSpecs) {
            for (ReaderColumnSpec col : ts) {
                allColumns.add(MultiTableUtils.getNameAfterInit(col));
            }
        }
        return allColumns;
    }

    /**
     * @param spec {@link DataTableSpec}
     * @param settings {@link NodeSettingsRO} to read from
     * @return the original column names
     */
    private static String[] loadOriginalNames(final DataTableSpec spec, final NodeSettingsRO settings) {
        final String[] originalNames = settings.getStringArray(CFG_ORIGINAL_NAMES, (String[])null);
        if (originalNames == null) {
            // Settings stored before 4.3 didn't have this setting, which means that all columns kept their original name
            return spec.stream().map(DataColumnSpec::getName).toArray(String[]::new);
        } else {
            return originalNames;
        }
    }

    /**
     * @param numColumns number of columns
     * @param settings {@link NodeSettingsRO} to read from
     * @return positional mapping
     */
    private static int[] loadPositionalMapping(final int numColumns, final NodeSettingsRO settings) {
        final int[] positionalMapping = settings.getIntArray(CFG_POSITIONAL_MAPPING, (int[])null);
        if (positionalMapping == null) {
            // Settings stored before 4.3 didn't have this setting, which means that there was no reordering or filtering
            return IntStream.range(0, numColumns).toArray();
        } else {
            return positionalMapping;
        }
    }

    /**
     * @param nodeSettings to read from
     * @param numIndividualPaths number of paths to read
     * @return {@link ReaderTableSpec}
     * @throws InvalidSettingsException
     */
    private static List<ReaderTableSpec<?>> loadIndividualSpecs(final NodeSettingsRO nodeSettings,
        final int numIndividualPaths) throws InvalidSettingsException {
        final List<ReaderTableSpec<?>> individualSpecs = new ArrayList<>();
        for (int i = 0; i < numIndividualPaths; i++) {
            individualSpecs.add(ReaderTableSpec
                .createReaderTableSpec(Arrays.asList(nodeSettings.getStringArray(CFG_INDIVIDUAL_SPEC + i))));
        }
        return individualSpecs;
    }

    /**
     * @param settings {@link NodeSettingsRO}
     * @param pathLoader {@link ProductionPathLoader}
     * @param mostGenericExternalType the most generic external type
     * @param allColumns all column names
     * @param dataTableSpec {@link DataTableSpec}
     * @return {@link ProductionPath}s
     * @throws InvalidSettingsException
     */
    private ProductionPath[] loadProductionPaths(final NodeSettingsRO settings, final Set<String> allColumns,
        final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        final ProductionPath[] prodPaths =
            loadProductionPathsFromSettings(settings.getNodeSettings(CFG_PRODUCTION_PATHS));
        if (allColumns.size() == dataTableSpec.getNumColumns()) {
            return prodPaths;
        } else {
            return reconstructProdPathsFor42Intersection(m_productionPathLoader.getProducerRegistry(),
                m_mostGenericType, allColumns, dataTableSpec, prodPaths);
        }
    }

    private ProductionPath[] loadProductionPathsFromSettings(final NodeSettingsRO settings)
        throws InvalidSettingsException {
        final ProductionPath[] prodPaths = new ProductionPath[settings.getInt(CFG_NUM_PRODUCTION_PATHS)];
        for (int i = 0; i < prodPaths.length; i++) {
            final int idx = i;
            prodPaths[i] = m_productionPathLoader.loadProductionPath(settings, CFG_PRODUCTION_PATH + i)
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("No production path associated with key <%s>", CFG_PRODUCTION_PATH + idx)));
        }
        return prodPaths;
    }

    /**
     * In KAP 4.2 we only stored the {@link ProductionPath ProductionPaths} for the columns that were in the KNIME
     * output spec. If the user read in multiple files and selected intersection as spec merge mode, this meant that we
     * didn't store the ProductionPath for those columns that were not part of the intersection.</br>
     * In KAP 4.3, we introduce the Transformation tab which allows to manipulate all columns of the union of the read
     * files, so we need ProductionPaths for the left-out columns as well. To this end we will assume that those columns
     * had the most generic type (typically String) and use the default ProductionPath to convert them into a String
     * column.
     *
     * @param registry {@link ProducerRegistry}
     * @param mostGenericExternalType typically String
     * @param allColumns the {@link Set} of all columns
     * @param dataTableSpec the loaded spec (i.e. potentially the intersection)
     * @param prodPaths the ProductionPaths corresponding to <b>dataTableSpec</b>
     * @return the ProductionPath array for the complete spec (i.e. union of all specs)
     */
    private static ProductionPath[] reconstructProdPathsFor42Intersection(final ProducerRegistry<?, ?> registry,
        final Object mostGenericExternalType, final Set<String> allColumns, final DataTableSpec dataTableSpec,
        final ProductionPath[] prodPaths) {
        final ProductionPath defaultProdPath = findDefaultProdPath(registry, mostGenericExternalType);
        final List<ProductionPath> allProdPaths = new ArrayList<>(allColumns.size());
        final Iterator<ProductionPath> loadedProdPaths = Iterators.forArray(prodPaths);
        for (String col : allColumns) {
            if (dataTableSpec.containsName(col)) {
                allProdPaths.add(loadedProdPaths.next());
            } else {
                allProdPaths.add(defaultProdPath);
            }
        }
        return allProdPaths.toArray(new ProductionPath[0]);
    }

    private static ProductionPath findDefaultProdPath(final ProducerRegistry<?, ?> registry,
        final Object mostGenericExternalType) {
        return registry.getAvailableProductionPaths().stream()//
            .filter(p -> p.getProducerFactory().getSourceType().equals(mostGenericExternalType))//
            .filter(p -> p.getConverterFactory().getDestinationType() == StringCell.TYPE)//
            .findFirst()//
            .orElseThrow(() -> new IllegalStateException(
                "No string converter available for the supposedly most generic external type: "
                    + mostGenericExternalType));
    }

    static <T> void save(final DefaultTableSpecConfig<T> config, final NodeSettingsWO settings) {

        final TableTransformation<T> tableTransformation = config.getTransformationModel();

        final TypedReaderTableSpec<T> rawSpec = tableTransformation.getRawSpec().getUnion();
        final int unionSize = rawSpec.size();
        final List<DataColumnSpec> columns = new ArrayList<>(unionSize);
        final List<ProductionPath> productionPaths = new ArrayList<>(unionSize);
        final List<String> originalNames = new ArrayList<>(unionSize);
        final int[] positions = new int[unionSize];
        final boolean[] keep = new boolean[unionSize];
        final boolean[] hasType = new boolean[unionSize];
        int idx = 0;
        for (TypedReaderColumnSpec<T> column : rawSpec) {
            hasType[idx] = column.hasType();
            final ColumnTransformation<T> transformation = tableTransformation.getTransformation(column);
            final ProductionPath productionPath = transformation.getProductionPath();
            productionPaths.add(productionPath);
            originalNames.add(MultiTableUtils.getNameAfterInit(column));
            keep[idx] = transformation.keep();
            final int idxInOutput = transformation.getPosition();
            positions[idx] = idxInOutput;
            final DataType knimeType = productionPath.getConverterFactory().getDestinationType();
            final String name = transformation.getName();
            CheckUtils.checkArgument(!name.isEmpty(), "Empty column names are not permitted.");
            columns.add(new DataColumnSpecCreator(name, knimeType).createSpec());
            idx++;
        }

        final DataTableSpec fullDataSpec = new DataTableSpec(columns.toArray(new DataColumnSpec[0]));

        settings.addString(CFG_ROOT_PATH, config.getSourceGroupID());
        saveID(config.getConfigID(), settings);

        fullDataSpec.save(settings.addNodeSettings(CFG_DATATABLE_SPEC));
        settings.addStringArray(CFG_FILE_PATHS, config.getItems().toArray(new String[0]));
        saveIndividualSpecs(config.getIndividualSpecs(), settings.addNodeSettings(CFG_INDIVIDUAL_SPECS));
        saveProductionPaths(productionPaths, settings.addNodeSettings(CFG_PRODUCTION_PATHS));
        settings.addStringArray(CFG_ORIGINAL_NAMES, originalNames.toArray(new String[0]));
        settings.addIntArray(CFG_POSITIONAL_MAPPING, positions);
        settings.addBooleanArray(CFG_KEEP, keep);
        settings.addBooleanArray(CFG_HAS_TYPE, hasType);
        settings.addInt(CFG_NEW_COLUMN_POSITION, tableTransformation.getPositionForUnknownColumns());
        settings.addBoolean(CFG_INCLUDE_UNKNOWN, tableTransformation.keepUnknownColumns());
        settings.addBoolean(CFG_ENFORCE_TYPES, tableTransformation.enforceTypes());
        settings.addString(CFG_COLUMN_FILTER_MODE, config.getColumnFilterMode().name());
    }

    private static void saveID(final ConfigID id, final NodeSettingsWO topLevelSettings) {
        if (id == EmptyConfigID.INSTANCE) {
            // the TableSpecConfig being serialized was loaded from an old workflow and therefore has no ConfigID
            // this is done for backwards compatibility
        } else {
            id.save(topLevelSettings.addNodeSettings(CFG_CONFIG_ID));
        }
    }

    private static void saveProductionPaths(final List<ProductionPath> prodPaths, final NodeSettingsWO settings) {
        int i = 0;
        for (final ProductionPath pP : prodPaths) {
            SerializeUtil.storeProductionPath(pP, settings, CFG_PRODUCTION_PATH + i);
            i++;
        }
        settings.addInt(CFG_NUM_PRODUCTION_PATHS, i);
    }

    private static <T> void saveIndividualSpecs(final Collection<TypedReaderTableSpec<T>> individualSpecs,
        final NodeSettingsWO settings) {
        int i = 0;
        for (final ReaderTableSpec<? extends ReaderColumnSpec> readerTableSpec : individualSpecs) {
            settings.addStringArray(CFG_INDIVIDUAL_SPEC + i//
                , readerTableSpec.stream()//
                    .map(MultiTableUtils::getNameAfterInit)//
                    .toArray(String[]::new)//
            );
            i++;
        }
    }

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @throws InvalidSettingsException if the settings are invalid
     */
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFG_ROOT_PATH);
        validateConfigconfigID(settings);
        DataTableSpec.load(settings.getNodeSettings(CFG_DATATABLE_SPEC));
        final int numIndividualPaths = settings.getStringArray(CFG_FILE_PATHS).length;
        validateIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), numIndividualPaths);
        //try to load the production paths
        loadProductionPathsFromSettings(settings.getNodeSettings(CFG_PRODUCTION_PATHS));
    }

    private void validateConfigconfigID(final NodeSettingsRO topLevelSettings) throws InvalidSettingsException {
        if (topLevelSettings.containsKey(CFG_CONFIG_ID)) {
            m_configIDLoader.createFromSettings(topLevelSettings.getNodeSettings(CFG_CONFIG_ID));
        }
    }

    private static void validateIndividualSpecs(final NodeSettingsRO settings, final int numIndividualPaths)
        throws InvalidSettingsException {
        for (int i = 0; i < numIndividualPaths; i++) {
            settings.getStringArray(CFG_INDIVIDUAL_SPEC + i);
        }
    }

    private static final class DefaultProductionPathLoader implements ProductionPathLoader {

        private ProducerRegistry<?, ?> m_registry;

        private DefaultProductionPathLoader(final ProducerRegistry<?, ?> registry) {
            m_registry = registry;
        }

        @Override
        public Optional<ProductionPath> loadProductionPath(final NodeSettingsRO config, final String key)
            throws InvalidSettingsException {
            return SerializeUtil.loadProductionPath(config, getProducerRegistry(), key);
        }

        @Override
        public ProducerRegistry<?, ?> getProducerRegistry() {
            return m_registry;
        }
    }

    /**
     * A class holding external configurations for the table spec that are saved by nodes directly and can be controlled
     * via flow variables.
     *
     * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
     */
    public static final class ExternalConfig {

        @SuppressWarnings("deprecation")
        private final SpecMergeMode m_specMergeMode;

        private final boolean m_skipEmptyColumns;

        /**
         * @param specMergeMode for workflows stored with 4.2, should be {@code null} for workflows stored with 4.3 and
         *            later
         * @param skipEmptyColumns whether empty columns should be skipped, most likely defaults to {@code false}
         */
        public ExternalConfig(@SuppressWarnings("deprecation") final SpecMergeMode specMergeMode,
            final boolean skipEmptyColumns) {
            m_specMergeMode = specMergeMode;
            m_skipEmptyColumns = skipEmptyColumns;
        }

        /**
         * @return the specMergeMode, can be {@code null}
         */
        @SuppressWarnings("deprecation")
        public SpecMergeMode getSpecMergeMode() {
            return m_specMergeMode;
        }

        /**
         * @return the skipEmptyColumns
         */
        public boolean skipEmptyColumns() {
            return m_skipEmptyColumns;
        }
    }

    /**
     * Special configID for loading old (pre 4.4) settings.<br>
     * Package private for testing purposes, DON'T USE this class FOR ANYTHING ELSE.
     *
     * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
     */
    enum EmptyConfigID implements ConfigID {

            INSTANCE;

        @Override
        public void save(final NodeSettingsWO settings) {
            throw new IllegalStateException(
                "EmptyConfigID only exist for backwards compatibility and should be handled differently.");
        }

    }
}
