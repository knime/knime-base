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
 *   May 12, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import static java.util.stream.Collectors.joining;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformationUtils;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

import com.google.common.collect.Iterators;

/**
 * Configuration storing all the information needed to create a {@link DataTableSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultTableSpecConfig implements TableSpecConfig {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(DefaultTableSpecConfig.class);

    private static final String CFG_INCLUDE_UNKNOWN = "include_unknown_columns" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NEW_COLUMN_POSITION = "unknown_column_position" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_KEEP = "keep" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_INDIVIDUAL_SPECS = "individual_specs" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_INDIVIDUAL_SPEC = "individual_spec_";

    private static final String CFG_ROOT_PATH = "root_path" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_FILE_PATHS = "file_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATHS = "production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NUM_PRODUCTION_PATHS = "num_production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_DATATABLE_SPEC = "datatable_spec" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATH = "production_path_";

    private static final String CFG_ORIGINAL_NAMES = "original_names" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_POSITIONAL_MAPPING = "positional_mapping" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_COLUMN_FILTER_MODE = "column_filter_mode" + SettingsModel.CFGKEY_INTERNAL;

    private final String m_rootPath;

    private final Map<String, ReaderTableSpec<?>> m_individualSpecs;

    /**
     * The production paths for all columns in original order (no filtering).
     */
    private final ProductionPath[] m_prodPaths;

    /**
     * Added with 4.3.</br>
     * Stores for each column in m_dataTableSpec the name in the raw spec.
     */
    private final String[] m_originalNames;

    /**
     * Added with 4.3</br>
     * Stores the position of the columns in the final output. The first column in the output is
     * {@code m_dataTableSpec.getColumnSpec(m_positionalMapping[0])} and so on. If there are fewer columns in the
     * positional mapping, then those indices not contained are filtered out.
     */
    private final int[] m_positions;

    /**
     * Stores for each column in the raw spec whether it's kept or not.
     */
    private final boolean[] m_keep;

    /**
     * Indicates whether new unknown columns should be in- or excluded.
     */
    private final boolean m_includeUnknownColumns;

    /**
     * The position at which new columns are to be inserted during execution (if they are inserted at all).
     */
    private final int m_unknownColPosition;

    /**
     * Specifies how to deal with new columns. NOTE: This field is not stored by {@link #save(NodeSettingsWO)} for
     * backwards compatibility (in 4.2 an equivalent setting was stored as part of the MultiTableReadConfig).
     * Consequently, the {@link #load(NodeSettingsRO, ProducerRegistry, Object, ColumnFilterMode)} function receives it
     * as parameter.
     */
    private final ColumnFilterMode m_columnFilterMode;

    /**
     * Contains all columns in original order (i.e. no filtering or reordering)
     */
    private final DataTableSpec m_dataTableSpec;

    /**
     * Creates a {@link DefaultTableSpecConfig} that corresponds to the provided parameters.
     *
     * @param <T> the type used to identify external types
     * @param rootPath if it represents a folder then all keys in the <b>individualSpecs<b> must be contained in this
     *            folder, otherwise the <b>rootPath</b> equals the {@link Path#toString()} version of the
     *            <b>individualSpecs<b> key and <b>individualSpecs<b> contains only a single element.
     * @param individualSpecs a map from the path/file to its individual {@link ReaderTableSpec}
     * @param transformationModel defines the transformation (type-mapping, filtering, renaming and reordering) of the
     *            output spec
     * @return a {@link DefaultTableSpecConfig} for the provided parameters
     */
    public static <T> TableSpecConfig createFromTransformationModel(final String rootPath,
        final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs,
        final TableTransformation<T> transformationModel) {
        final TypedReaderTableSpec<T> rawSpec = transformationModel.getRawSpec().getUnion();
        final int unionSize = rawSpec.size();
        final List<DataColumnSpec> columns = new ArrayList<>(unionSize);
        final List<ProductionPath> productionPaths = new ArrayList<>(unionSize);
        final List<String> originalNames = new ArrayList<>(unionSize);
        final int[] positions = new int[unionSize];
        final boolean[] keep = new boolean[unionSize];
        int idx = 0;
        for (TypedReaderColumnSpec<T> column : rawSpec) {
            final ColumnTransformation<T> transformation = transformationModel.getTransformation(column);
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
        return new DefaultTableSpecConfig(rootPath, new DataTableSpec(columns.toArray(new DataColumnSpec[0])),
            individualSpecs, productionPaths.toArray(new ProductionPath[0]), originalNames.toArray(new String[0]),
            positions, keep, transformationModel.getPositionForUnknownColumns(),
            transformationModel.getColumnFilterMode(), transformationModel.keepUnknownColumns());
    }

    /**
     * Constructor for testing.</br>
     * Clients should use {@link #createFromTransformationModel(String, Map, TableTransformation)}.
     *
     * @param rootPath if it represents a folder then all keys in the <b>individualSpecs<b> must be contained in this
     *            folder, otherwise the <b>rootPath</b> equals the {@link Path#toString()} version of the
     *            <b>individualSpecs<b> key and <b>individualSpecs<b> contains only a single element.
     * @param outputSpec the {@link DataTableSpec} resulting from merging the <b>individualSpecs</b> and applying the
     *            {@link TypeMapping}
     * @param individualSpecs a map from the path/file to its individual {@link ReaderTableSpec}
     * @param productionPaths the production paths
     * @param originalNames the original names i.e. before renaming
     * @param positionalMapping contains for each column in the final output the position in {@link DataTableSpec
     *            outputSpec}
     */
    DefaultTableSpecConfig(final String rootPath, final DataTableSpec outputSpec,
        final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positionalMapping, final boolean[] keep, final int newColPosition,
        final ColumnFilterMode columnFilterMode, final boolean includeUnknownColumns) {
        // check for nulls
        CheckUtils.checkNotNull(rootPath, "The rootPath cannot be null");
        CheckUtils.checkNotNull(individualSpecs, "The individual specs cannot be null");
        CheckUtils.checkNotNull(outputSpec, "The outputSpec cannot be null");
        CheckUtils.checkNotNull(productionPaths, "The paths cannot be null");
        CheckUtils.checkNotNull(originalNames, "The originalNames cannot be null");
        CheckUtils.checkNotNull(positionalMapping, "The positionalMapping cannot be null");

        // check for size
        CheckUtils.checkArgument(!rootPath.trim().isEmpty(), "The rootPath cannot be empty");
        CheckUtils.checkArgument(!individualSpecs.isEmpty(), "The individual specs cannot be empty");
        CheckUtils.checkArgument(originalNames.length == outputSpec.getNumColumns(),
            "The originalNames must have as many elements as outputSpec has columns");

        m_rootPath = rootPath;
        m_dataTableSpec = outputSpec;
        m_individualSpecs = individualSpecs.entrySet().stream()//
            .collect(Collectors.toMap(//
                e -> e.getKey().toString()//
                , Map.Entry::getValue//
                , (x, y) -> y//
                , LinkedHashMap::new));
        m_prodPaths = productionPaths.clone();
        m_originalNames = originalNames.clone();
        m_positions = positionalMapping.clone();
        m_keep = keep.clone();
        m_unknownColPosition = newColPosition;
        m_columnFilterMode = columnFilterMode;
        m_includeUnknownColumns = includeUnknownColumns;
    }

    /**
     * Constructor.
     *
     * @param rootPath if it represents a folder then all <b>paths<b> must be contained in this folder, otherwise the
     *            <b>rootPath</b> equals the <b>paths[0]<b> and <b>paths<b> contains only a single element.
     * @param outputSpec the {@link DataTableSpec} resulting from merging the <b>individualSpecs</b> and applying the
     *            {@link TypeMapping}
     * @param paths the string representation of the paths associated with each individual spec
     * @param individualSpecs the individual {@link ReaderTableSpec ReaderTableSpecs}
     * @param productionPaths the {@link ProductionPath ProductionPaths}
     */
    private DefaultTableSpecConfig(final String rootPath, final DataTableSpec outputSpec, final String[] paths,
        final ReaderTableSpec<?>[] individualSpecs, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positionalMapping, final boolean[] keep, final int newColPosition,
        final ColumnFilterMode columnFilterMode, final boolean includeUnknownColumns) {
        m_rootPath = rootPath;
        m_dataTableSpec = outputSpec;
        m_individualSpecs = IntStream.range(0, paths.length)//
            .boxed()//
            .collect(Collectors.toMap(//
                i -> paths[i], //
                i -> individualSpecs[i], //
                (x, y) -> y, //
                LinkedHashMap::new));
        m_prodPaths = productionPaths;
        m_originalNames = originalNames;
        m_positions = positionalMapping;
        m_keep = keep;
        m_unknownColPosition = newColPosition;
        m_columnFilterMode = columnFilterMode;
        m_includeUnknownColumns = includeUnknownColumns;
    }

    /**
     * Returns the raw {@link TypedReaderTableSpec} before type mapping, filtering, reordering or renaming.
     *
     * @param <T> the type used to identify external types
     * @return the raw spec
     */
    private <T> RawSpec<T> getRawSpec() {
        final DataTableSpec rawKnimeSpec = m_dataTableSpec;
        final ProductionPath[] productionPaths = m_prodPaths;
        assert rawKnimeSpec
            .getNumColumns() == productionPaths.length : "Number of production paths doesn't match the number of columns.";
        final List<TypedReaderColumnSpec<T>> specs = new ArrayList<>(rawKnimeSpec.getNumColumns());
        for (int i = 0; i < rawKnimeSpec.getNumColumns(); i++) {
            final ProductionPath productionPath = productionPaths[i];
            @SuppressWarnings("unchecked") // the production path stores the source type of type T
            final T type = (T)productionPath.getProducerFactory().getSourceType();
            specs.add(TypedReaderColumnSpec.createWithName(m_originalNames[i], type, true));
        }
        final TypedReaderTableSpec<T> union = new TypedReaderTableSpec<>(specs);
        final TypedReaderTableSpec<T> intersection = findIntersection(union);
        return new RawSpec<>(union, intersection);
    }

    private <T> TypedReaderTableSpec<T> findIntersection(final TypedReaderTableSpec<T> union) {
        final Set<String> commonNames = extractNameSet(union);
        for (ReaderTableSpec<?> individualSpec : m_individualSpecs.values()) {
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

    private static <T extends ReaderColumnSpec> Set<String> extractNameSet(final ReaderTableSpec<T> spec) {
        return spec.stream()//
            .map(MultiTableUtils::getNameAfterInit)//
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Override
    public <T> TableTransformation<T> getTransformationModel() {
        final RawSpec<T> rawSpec = getRawSpec();
        final TypedReaderTableSpec<T> union = rawSpec.getUnion();

        final List<ColumnTransformation<T>> transformations = new ArrayList<>(m_originalNames.length);
        for (int i = 0; i < m_originalNames.length; i++) {
            transformations.add(createTransformation(union.getColumnSpec(i), i));
        }
        return new DefaultTableTransformation<>(rawSpec, transformations, m_columnFilterMode, m_includeUnknownColumns,
            m_unknownColPosition);
    }

    private <T> ColumnTransformation<T> createTransformation(final TypedReaderColumnSpec<T> colSpec, final int idx) {
        final String name = m_dataTableSpec.getColumnSpec(idx).getName();
        final int position = m_positions[idx];
        final boolean keep = m_keep[idx];
        final ProductionPath prodPath = m_prodPaths[idx];
        return new ImmutableColumnTransformation<>(colSpec, prodPath, keep, position, name);
    }

    @Override
    public boolean isConfiguredWith(final String rootPath, final List<Path> paths) {
        return isConfiguredWith(rootPath) && isConfiguredWith(paths);
    }

    @Override
    public boolean isConfiguredWith(final String rootPath) {
        return m_rootPath.equals(rootPath);
    }

    @Override
    public boolean isConfiguredWith(final List<Path> paths) {
        return m_individualSpecs.size() == paths.size() //
            && paths.stream()//
                .map(Path::toString)//
                .allMatch(m_individualSpecs::containsKey);
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return TableTransformationUtils.toDataTableSpec(getTransformationModel());
    }

    @Override
    public List<String> getPaths() {
        return Collections.unmodifiableList(new ArrayList<>(m_individualSpecs.keySet()));
    }

    @Override
    public ReaderTableSpec<?> getSpec(final String path) {
        return m_individualSpecs.get(path);
    }

    @Override
    public ProductionPath[] getProductionPaths() {
        return IntStream.range(0, m_prodPaths.length)//
            .filter(i -> m_keep[i])//
            .boxed()//
            .sorted((i, j) -> Integer.compare(m_positions[i], m_positions[j]))//
            .map(i -> m_prodPaths[i])//
            .toArray(ProductionPath[]::new);
    }

    @Override
    public ColumnFilterMode getColumnFilterMode() {
        return m_columnFilterMode;
    }

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @param registry the {@link ProducerRegistry}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static void validate(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        settings.getString(CFG_ROOT_PATH);
        DataTableSpec.load(settings.getNodeSettings(CFG_DATATABLE_SPEC));
        final int numIndividualPaths = settings.getStringArray(CFG_FILE_PATHS).length;
        validateIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), numIndividualPaths);
        validateProductionPaths(settings.getNodeSettings(CFG_PRODUCTION_PATHS), registry);
    }

    private static void validateIndividualSpecs(final NodeSettingsRO settings, final int numIndividualPaths)
        throws InvalidSettingsException {
        for (int i = 0; i < numIndividualPaths; i++) {
            settings.getStringArray(CFG_INDIVIDUAL_SPEC + i);
        }
    }

    private static void validateProductionPaths(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        final int numProductionPaths = settings.getInt(CFG_NUM_PRODUCTION_PATHS);
        for (int i = 0; i < numProductionPaths; i++) {
            SerializeUtil.loadProductionPath(settings, registry, CFG_PRODUCTION_PATH + i);
        }
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFG_ROOT_PATH, m_rootPath);
        m_dataTableSpec.save(settings.addNodeSettings(CFG_DATATABLE_SPEC));
        settings.addStringArray(CFG_FILE_PATHS, //
            m_individualSpecs.keySet().stream()//
                .toArray(String[]::new));
        saveIndividualSpecs(settings.addNodeSettings(CFG_INDIVIDUAL_SPECS));
        saveProductionPaths(settings.addNodeSettings(CFG_PRODUCTION_PATHS));
        settings.addStringArray(CFG_ORIGINAL_NAMES, m_originalNames);
        settings.addIntArray(CFG_POSITIONAL_MAPPING, m_positions);
        settings.addBooleanArray(CFG_KEEP, m_keep);
        settings.addInt(CFG_NEW_COLUMN_POSITION, m_unknownColPosition);
        settings.addBoolean(CFG_INCLUDE_UNKNOWN, m_includeUnknownColumns);
        settings.addString(CFG_COLUMN_FILTER_MODE, m_columnFilterMode.name());
    }

    private void saveProductionPaths(final NodeSettingsWO settings) {
        int i = 0;
        for (final ProductionPath pP : m_prodPaths) {
            SerializeUtil.storeProductionPath(pP, settings, CFG_PRODUCTION_PATH + i);
            i++;
        }
        settings.addInt(CFG_NUM_PRODUCTION_PATHS, i);
    }

    private void saveIndividualSpecs(final NodeSettingsWO settings) {
        int i = 0;
        for (final ReaderTableSpec<? extends ReaderColumnSpec> readerTableSpec : m_individualSpecs.values()) {
            settings.addStringArray(CFG_INDIVIDUAL_SPEC + i//
                , readerTableSpec.stream()//
                    .map(MultiTableUtils::getNameAfterInit)//
                    .toArray(String[]::new)//
            );
            i++;
        }
    }

    /**
     * De-serializes the {@link DefaultTableSpecConfig} previously written to the given settings.
     *
     * @param settings containing the serialized {@link DefaultTableSpecConfig}
     * @param registry the {@link ProducerRegistry}
     * @param mostGenericExternalType used as default type for columns that were previously (4.2) filtered out
     * @param specMergeModeOld for workflows stored with 4.2, should be {@code null} for workflows stored with 4.3 and
     *            later
     * @return the de-serialized {@link DefaultTableSpecConfig}
     * @throws InvalidSettingsException - if the settings do not exists / cannot be loaded
     */
    public static TableSpecConfig load(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry,
        final Object mostGenericExternalType, final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        final String rootPath = settings.getString(CFG_ROOT_PATH);
        final String[] paths = settings.getStringArray(CFG_FILE_PATHS);
        final ReaderTableSpec<?>[] individualSpecs =
            loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), paths.length);
        final Set<String> allColumns = union(individualSpecs);

        final boolean includeUnknownColumns = settings.getBoolean(CFG_INCLUDE_UNKNOWN, true);

        // For old workflows (created with 4.2), the spec might not contain all columns contained in union if
        // SpecMergeMode#INTERSECTION was used to create the final spec
        final DataTableSpec loadedKnimeSpec = DataTableSpec.load(settings.getConfig(CFG_DATATABLE_SPEC));
        final DataTableSpec fullKnimeSpec = constructFullKnimeSpec(allColumns, loadedKnimeSpec);

        ProductionPath[] allProdPaths =
            loadProductionPaths(settings, registry, mostGenericExternalType, allColumns, loadedKnimeSpec);

        final String[] originalNames = loadOriginalNames(fullKnimeSpec, settings);
        final int[] positionalMapping = loadPositionalMapping(fullKnimeSpec.getNumColumns(), settings);
        final boolean[] keep = loadKeep(loadedKnimeSpec, allColumns, settings);
        final int newColPosition = settings.getInt(CFG_NEW_COLUMN_POSITION, allProdPaths.length);
        final ColumnFilterMode columnFilterMode = loadColumnFilterMode(settings, specMergeModeOld);

        return new DefaultTableSpecConfig(rootPath, fullKnimeSpec, paths, individualSpecs, allProdPaths, originalNames,
            positionalMapping, keep, newColPosition, columnFilterMode, includeUnknownColumns);
    }

    private static ColumnFilterMode loadColumnFilterMode(final NodeSettingsRO settings,
        final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        try {
            return ColumnFilterMode.valueOf(settings.getString(CFG_COLUMN_FILTER_MODE));
        } catch (InvalidSettingsException ise) {
            LOGGER.debug("The settings contained no ColumnFilterMode.", ise);
            CheckUtils.checkSetting(specMergeModeOld != null,
                "The settings are missing both the SpecMergeMode (4.2) and the ColumnFilterMode (4.3 and later).");
            @SuppressWarnings("null") // checked above
            final ColumnFilterMode columnFilterMode = specMergeModeOld.getColumnFilterMode();
            return columnFilterMode;
        }
    }

    private static Set<String> union(final ReaderTableSpec<?>[] individualSpecs) {
        final Set<String> allColumns = new LinkedHashSet<>();
        for (ReaderTableSpec<?> ts : individualSpecs) {
            for (ReaderColumnSpec col : ts) {
                allColumns.add(MultiTableUtils.getNameAfterInit(col));
            }
        }
        return allColumns;
    }

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

    private static ProductionPath[] loadProductionPaths(final NodeSettingsRO settings,
        final ProducerRegistry<?, ?> registry, final Object mostGenericExternalType, final Set<String> allColumns,
        final DataTableSpec dataTableSpec) throws InvalidSettingsException {
        final ProductionPath[] prodPaths =
            loadProductionPathsFromSettings(settings.getNodeSettings(CFG_PRODUCTION_PATHS), registry);
        if (allColumns.size() == dataTableSpec.getNumColumns()) {
            return prodPaths;
        } else {
            return reconstructProdPathsFor42Intersection(registry, mostGenericExternalType, allColumns, dataTableSpec,
                prodPaths);
        }
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

    private static String[] loadOriginalNames(final DataTableSpec spec, final NodeSettingsRO settings) {
        final String[] originalNames = settings.getStringArray(CFG_ORIGINAL_NAMES, (String[])null);
        if (originalNames == null) {
            // Settings stored before 4.3 didn't have this setting, which means that all columns kept their original name
            return spec.stream().map(DataColumnSpec::getName).toArray(String[]::new);
        } else {
            return originalNames;
        }
    }

    private static int[] loadPositionalMapping(final int numColumns, final NodeSettingsRO settings) {
        final int[] positionalMapping = settings.getIntArray(CFG_POSITIONAL_MAPPING, (int[])null);
        if (positionalMapping == null) {
            // Settings stored before 4.3 didn't have this setting, which means that there was no reordering or filtering
            return IntStream.range(0, numColumns).toArray();
        } else {
            return positionalMapping;
        }
    }

    private static ReaderTableSpec<?>[] loadIndividualSpecs(final NodeSettingsRO nodeSettings,
        final int numIndividualPaths) throws InvalidSettingsException {
        final ReaderTableSpec<?>[] individualSpecs = new ReaderTableSpec[numIndividualPaths];
        for (int i = 0; i < numIndividualPaths; i++) {
            individualSpecs[i] = ReaderTableSpec
                .createReaderTableSpec(Arrays.asList(nodeSettings.getStringArray(CFG_INDIVIDUAL_SPEC + i)));
        }
        return individualSpecs;
    }

    private static ProductionPath[] loadProductionPathsFromSettings(final NodeSettingsRO settings,
        final ProducerRegistry<?, ?> registry) throws InvalidSettingsException {
        final ProductionPath[] prodPaths = new ProductionPath[settings.getInt(CFG_NUM_PRODUCTION_PATHS)];
        for (int i = 0; i < prodPaths.length; i++) {
            final int idx = i;
            prodPaths[i] = SerializeUtil.loadProductionPath(settings, registry, CFG_PRODUCTION_PATH + i)
                .orElseThrow(() -> new InvalidSettingsException(
                    String.format("No production path associated with key <%s>", CFG_PRODUCTION_PATH + idx)));
        }
        return prodPaths;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_dataTableSpec == null) ? 0 : m_dataTableSpec.hashCode());
        result = prime * result + ((m_individualSpecs == null) ? 0 : m_individualSpecs.hashCode());
        result = prime * result + Arrays.hashCode(m_prodPaths);
        result = prime * result + ((m_rootPath == null) ? 0 : m_rootPath.hashCode());
        result = prime * result + Arrays.hashCode(m_originalNames);
        result = prime * result + Arrays.hashCode(m_positions);
        result = prime * result + Arrays.hashCode(m_keep);
        result = prime * result + Integer.hashCode(m_unknownColPosition);
        result = prime * result + Boolean.hashCode(m_includeUnknownColumns);
        result = prime * result + m_columnFilterMode.hashCode();
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj != null && getClass() == obj.getClass()) {
            DefaultTableSpecConfig other = (DefaultTableSpecConfig)obj;
            return m_includeUnknownColumns == other.m_includeUnknownColumns//
                && m_unknownColPosition == other.m_unknownColPosition//
                && m_columnFilterMode == other.m_columnFilterMode //
                && m_dataTableSpec.equals(other.m_dataTableSpec)//
                && m_individualSpecs.equals(other.m_individualSpecs)//
                && Arrays.equals(m_prodPaths, other.m_prodPaths)//
                && m_rootPath.equals(other.m_rootPath)//
                && Arrays.equals(m_originalNames, other.m_originalNames)//
                && Arrays.equals(m_positions, other.m_positions)//
                && Arrays.equals(m_keep, other.m_keep);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder("[")//
            .append("Root path: ")//
            .append(m_rootPath)//
            .append("\n DataTableSpec: ")//
            .append(m_dataTableSpec)//
            .append("\n Individual specs: ")//
            .append(m_individualSpecs.entrySet().stream()//
                .map(e -> e.getKey() + ": " + e.getValue())//
                .collect(joining(", ", "[", "]")))//
            .append("\n ProductionPaths: ")//
            .append(Arrays.stream(m_prodPaths)//
                .map(ProductionPath::toString)//
                .collect(joining(", ", "[", "]")))//
            .append("\n OriginalNames: ")//
            .append(Arrays.stream(m_originalNames)//
                .collect(joining(", ", "[", "]")))//
            .append("\n Positions: ")//
            .append(Arrays.stream(m_positions)//
                .mapToObj(Integer::toString)//
                .collect(joining(", ", "[", "]")))//
            .append("\n Keep: ")//
            .append(Arrays.toString(m_keep))//
            .append("\n Keep unknown: ")//
            .append(m_includeUnknownColumns)//
            .append("\n Position for unknown columns: ")//
            .append(m_unknownColPosition)//
            .append("\n ColumnFilterMode: ")//
            .append(m_columnFilterMode)//
            .append("]\n").toString();
    }

}
