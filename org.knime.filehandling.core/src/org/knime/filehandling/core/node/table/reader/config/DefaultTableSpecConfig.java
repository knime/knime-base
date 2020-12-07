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
 *   Nov 13, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.config;

import static java.util.stream.Collectors.joining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
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
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
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

/**
 * Configuration storing all the information needed to create a {@link DataTableSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultTableSpecConfig implements TableSpecConfig {

    private final String m_rootItem;

    private final Map<String, ReaderTableSpec<?>> m_individualSpecs;

    private final boolean m_enforceTypes;

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
     * Constructor.
     *
     * @param rootItem the root item
     * @param outputSpec the output {@link DataTableSpec}
     * @param individualSpecs the individual input specs
     * @param productionPaths the {@link ProductionPath}s
     * @param originalNames the original column names
     * @param positionalMapping the position map
     * @param keep flag if the column should be kept
     * @param newColPosition the column position
     * @param columnFilterMode the {@link ColumnFilterMode}
     * @param includeUnknownColumns if unknown columns should be included
     * @param enforceTypes indicates whether configured KNIME types should be enforced even if the external type changes
     *
     */
    public <I> DefaultTableSpecConfig(final String rootItem, final DataTableSpec outputSpec,
        final Map<I, ? extends ReaderTableSpec<?>> individualSpecs, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positionalMapping, final boolean[] keep, final int newColPosition,
        final ColumnFilterMode columnFilterMode, final boolean includeUnknownColumns, final boolean enforceTypes) {
        // check for nulls
        CheckUtils.checkNotNull(rootItem, "The rootPath cannot be null");
        CheckUtils.checkNotNull(individualSpecs, "The individual specs cannot be null");
        CheckUtils.checkNotNull(outputSpec, "The outputSpec cannot be null");
        CheckUtils.checkNotNull(productionPaths, "The paths cannot be null");
        CheckUtils.checkNotNull(originalNames, "The originalNames cannot be null");
        CheckUtils.checkNotNull(positionalMapping, "The positionalMapping cannot be null");

        // check for size
        CheckUtils.checkArgument(!rootItem.trim().isEmpty(), "The rootPath cannot be empty");
        CheckUtils.checkArgument(!individualSpecs.isEmpty(), "The individual specs cannot be empty");
        CheckUtils.checkArgument(originalNames.length == outputSpec.getNumColumns(),
            "The originalNames must have as many elements as outputSpec has columns");

        m_rootItem = rootItem;
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
        m_enforceTypes = enforceTypes;
    }

    /**
     * Constructor.
     *
     * @param rootItem if it represents a folder then all <b>paths<b> must be contained in this folder, otherwise the
     *            <b>rootPath</b> equals the <b>paths[0]<b> and <b>paths<b> contains only a single element.
     * @param outputSpec the {@link DataTableSpec} resulting from merging the <b>individualSpecs</b> and applying the
     *            TypeMapping
     * @param items the string representation of the items associated with each individual spec
     * @param individualSpecs the individual input specs
     * @param productionPaths the {@link ProductionPath}s
     * @param originalNames the original column names
     * @param positionalMapping the position map
     * @param keep flag if the column should be kept
     * @param newColPosition the column position
     * @param columnFilterMode the {@link ColumnFilterMode}
     * @param includeUnknownColumns if unknown columns should be included
     * @param enforceTypes indicates whether configured KNIME types should be enforced even if the external type changes
     */
    protected DefaultTableSpecConfig(final String rootItem, final DataTableSpec outputSpec, final String[] items,
        final ReaderTableSpec<?>[] individualSpecs, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positionalMapping, final boolean[] keep, final int newColPosition,
        final ColumnFilterMode columnFilterMode, final boolean includeUnknownColumns, final boolean enforceTypes) {
        m_rootItem = rootItem;
        m_dataTableSpec = outputSpec;
        m_individualSpecs = IntStream.range(0, items.length)//
            .boxed()//
            .collect(Collectors.toMap(//
                i -> items[i], //
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
        m_enforceTypes = enforceTypes;
    }

    /**
     * Creates a {@link DefaultTableSpecConfig} that corresponds to the provided parameters.
     *
     * @param <T> the type used to identify external types
     * @param rootItem the root item
     * @param individualSpecs a map from the path/file to its individual {@link ReaderTableSpec}
     * @param tableTransformation defines the transformation (type-mapping, filtering, renaming and reordering) of the
     *            output spec
     * @return a {@link DefaultTableSpecConfig} for the provided parameters
     */
    public static <I, T> TableSpecConfig createFromTransformationModel(final String rootItem,
        final Map<I, ? extends ReaderTableSpec<?>> individualSpecs, final TableTransformation<T> tableTransformation) {
        final TypedReaderTableSpec<T> rawSpec = tableTransformation.getRawSpec().getUnion();
        final int unionSize = rawSpec.size();
        final List<DataColumnSpec> columns = new ArrayList<>(unionSize);
        final List<ProductionPath> productionPaths = new ArrayList<>(unionSize);
        final List<String> originalNames = new ArrayList<>(unionSize);
        final int[] positions = new int[unionSize];
        final boolean[] keep = new boolean[unionSize];
        int idx = 0;
        for (TypedReaderColumnSpec<T> column : rawSpec) {
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
        return new DefaultTableSpecConfig(rootItem, new DataTableSpec(columns.toArray(new DataColumnSpec[0])),
            individualSpecs, productionPaths.toArray(new ProductionPath[0]), originalNames.toArray(new String[0]),
            positions, keep, tableTransformation.getPositionForUnknownColumns(),
            tableTransformation.getColumnFilterMode(), tableTransformation.keepUnknownColumns(),
            tableTransformation.enforceTypes());
    }

    private static <T extends ReaderColumnSpec> Set<String> extractNameSet(final ReaderTableSpec<T> spec) {
        return spec.stream()//
            .map(MultiTableUtils::getNameAfterInit)//
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Returns the raw {@link TypedReaderTableSpec} before type mapping, filtering, reordering or renaming.
     *
     * @param <T> the type used to identify external types
     * @return the raw spec
     */
    @Override
    public <T> RawSpec<T> getRawSpec() {
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

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @param pathLoader the {@link ProductionPathLoader}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static void validate(final NodeSettingsRO settings, final ProductionPathLoader pathLoader)
        throws InvalidSettingsException {
        new DefaultTableSpecConfigSerializer(pathLoader, null).validate(settings);
    }

    /**
     * Checks that this configuration can be loaded from the provided settings.
     *
     * @param settings to validate
     * @param registry the {@link ProducerRegistry} used to restore the {@link ProductionPath ProductionPaths}
     * @throws InvalidSettingsException if the settings are invalid
     */
    public static void validate(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry)
        throws InvalidSettingsException {
        new DefaultTableSpecConfigSerializer(registry, null).validate(settings);
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
            m_unknownColPosition, m_enforceTypes);
    }

    private <T> ColumnTransformation<T> createTransformation(final TypedReaderColumnSpec<T> colSpec, final int idx) {
        final String name = m_dataTableSpec.getColumnSpec(idx).getName();
        final int position = m_positions[idx];
        final boolean keep = m_keep[idx];
        final ProductionPath prodPath = m_prodPaths[idx];
        return new ImmutableColumnTransformation<>(colSpec, prodPath, keep, position, name);
    }

    @Override
    public boolean isConfiguredWith(final SourceGroup<String> sourceGroup) {
        return isConfiguredWith(sourceGroup.getID()) && m_individualSpecs.size() == sourceGroup.size() //
            && sourceGroup.stream()//
                .allMatch(m_individualSpecs::containsKey);
    }

    @Override
    public boolean isConfiguredWith(final String rootItem) {
        return m_rootItem.equals(rootItem);
    }

    @Override
    public DataTableSpec getDataTableSpec() {
        return TableTransformationUtils.toDataTableSpec(getTransformationModel());
    }

    @Override
    public List<String> getItems() {
        return Collections.unmodifiableList(new ArrayList<>(m_individualSpecs.keySet()));
    }

    @Override
    public ReaderTableSpec<?> getSpec(final String item) {
        return m_individualSpecs.get(item);
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

    @Override
    public void save(final NodeSettingsWO settings) {
        DefaultTableSpecConfigSerializer.save(this, settings);
    }

    /**
     * De-serializes the {@link DefaultTableSpecConfig} previously written to the given settings.
     *
     * @param settings containing the serialized {@link DefaultTableSpecConfig}
     * @param pathLoader the {@link ProductionPathLoader}
     * @param mostGenericExternalType used as default type for columns that were previously (4.2) filtered out
     * @param specMergeModeOld for workflows stored with 4.2, should be {@code null} for workflows stored with 4.3 and
     *            later
     * @return the de-serialized {@link DefaultTableSpecConfig}
     * @throws InvalidSettingsException - if the settings do not exists / cannot be loaded
     */
    public static DefaultTableSpecConfig load(final Object mostGenericExternalType,
        final NodeSettingsRO settings, final ProductionPathLoader pathLoader,
        @SuppressWarnings("deprecation") final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        return new DefaultTableSpecConfigSerializer(pathLoader, mostGenericExternalType).load(settings, specMergeModeOld);
    }

    /**
    * De-serializes the {@link DefaultTableSpecConfig} previously written to the given settings.
    *
    * @param settings containing the serialized {@link DefaultTableSpecConfig}
     * @param registry the {@link ProducerRegistry} for restoring {@link ProductionPath ProductionPaths}
    * @param mostGenericExternalType used as default type for columns that were previously (4.2) filtered out
    * @param specMergeModeOld for workflows stored with 4.2, should be {@code null} for workflows stored with 4.3 and
    *            later
    * @return the de-serialized {@link DefaultTableSpecConfig}
    * @throws InvalidSettingsException - if the settings do not exists / cannot be loaded
    */
    public static DefaultTableSpecConfig load(final NodeSettingsRO settings,
        final ProducerRegistry<?, ?> registry, final Object mostGenericExternalType,
        @SuppressWarnings("deprecation") final SpecMergeMode specMergeModeOld) throws InvalidSettingsException {
        return new DefaultTableSpecConfigSerializer(registry, mostGenericExternalType).load(settings, specMergeModeOld);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((m_dataTableSpec == null) ? 0 : m_dataTableSpec.hashCode());
        result = prime * result + ((m_individualSpecs == null) ? 0 : m_individualSpecs.hashCode());
        result = prime * result + Arrays.hashCode(m_prodPaths);
        result = prime * result + ((m_rootItem == null) ? 0 : m_rootItem.hashCode());
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
                && m_rootItem.equals(other.m_rootItem)//
                && Arrays.equals(m_originalNames, other.m_originalNames)//
                && Arrays.equals(m_positions, other.m_positions)//
                && Arrays.equals(m_keep, other.m_keep);
        }
        return false;
    }

    @Override
    public String toString() {
        return new StringBuilder("[")//
            .append("Root item: ")//
            .append(m_rootItem)//
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


 // Getters for DefaultTableSpecConfigSerializer

    String getRootItem() {
        return m_rootItem;
    }

    DataTableSpec getFullDataSpec() {
        return m_dataTableSpec;
    }

    String[] getOriginalNames() {
        return m_originalNames;
    }

    int[] getPositions() {
        return m_positions;
    }

    boolean[] getKeep() {
        return m_keep;
    }

    int getUnknownColumnPosition() {
        return m_unknownColPosition;
    }

    boolean includeUnknownColumns() {
        return m_includeUnknownColumns;
    }

    boolean enforceTypes() {
        return m_enforceTypes;
    }

    ProductionPath[] getAllProductionPaths() {
        return m_prodPaths;
    }

    Collection<ReaderTableSpec<?>> getIndividualSpecs() {
        return m_individualSpecs.values();
    }

}