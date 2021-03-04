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
 *   Feb 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.config.tablespec;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataTableSpecCreator;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;

import com.google.common.collect.Iterators;

/**
 * {@link TableSpecConfigSerializer} for configs created and stored in KNIME 4.2.x.<br>
 * Only supports loading and throws an {@link IllegalStateException} when
 * {@link TableSpecConfigSerializer#save(TableSpecConfig, NodeSettingsWO)} is called because configs should always be
 * saved with the latest serializer.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class V42TableSpecConfigSerializer<T> implements TableSpecConfigSerializer<T> {

    private final ProductionPathSerializer m_prodPathLoader;

    private final ProductionPath m_defaultPath;

    V42TableSpecConfigSerializer(final ProducerRegistry<T, ?> producerRegistry, final T mostGenericType) {
        m_prodPathLoader = new DefaultProductionPathSerializer(producerRegistry);
        m_defaultPath = findDefaultProdPath(producerRegistry, mostGenericType);
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

    @Override
    public void save(final TableSpecConfig<T> object, final NodeSettingsWO settings) {
        throw new IllegalStateException(
            "This is a compatibility serializer used for loading old settings stored with KNIME AP 4.2.x. "
                + "Use the latest serializer for saving.");
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings, final AdditionalParameters additionalParameters)
        throws InvalidSettingsException {
        final ColumnFilterMode colFilterMode = additionalParameters.getColumnFilterMode()
            .orElseThrow(V42TableSpecConfigSerializer::noColumnFilterModeProvided);
        final Pre44Loader loader = new Pre44Loader(settings, m_prodPathLoader);
        final ImmutableTableTransformation<T> tableTransformation = loadTableTransformation(loader, colFilterMode);
        return loader.createTableSpecConfig(tableTransformation);
    }

    private static IllegalStateException noColumnFilterModeProvided() {
        return new IllegalStateException(
            "Coding error. In KNIME AP 4.2.x the ColumnFilterMode was stored outside of the TableSpecConfig "
                + "and must therefore be provided as additional parameter.");
    }

    @Override
    public TableSpecConfig<T> load(final NodeSettingsRO settings) {
        throw noColumnFilterModeProvided();
    }

    /**
     * @param allColumns all column names
     * @param loadedKnimeSpec {@link DataTableSpec}
     * @return reconstructed KNIME {@link DataTableSpec}
     */
    private static DataTableSpec constructFullKnimeSpec(final Pre44Loader loader) {
        final Set<String> allColumns = loader.getAllColumnNames();
        final DataTableSpec loadedKnimeSpec = loader.getLoadedKnimeSpec();
        if (loader.getAllColumnNames().size() == loadedKnimeSpec.getNumColumns()) {
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

    private ImmutableTableTransformation<T> loadTableTransformation(final Pre44Loader loader,
        final ColumnFilterMode columnFilterMode) {
        final DataTableSpec fullKnimeSpec = constructFullKnimeSpec(loader);
        final int unionSize = fullKnimeSpec.getNumColumns();
        final ProductionPath[] productionPaths = reconstructProdPathsFor42Intersection(loader);
        final String[] originalNames = fullKnimeSpec.stream()//
            .map(DataColumnSpec::getName)//
            .toArray(String[]::new);
        final int[] positions = IntStream.range(0, unionSize).toArray();
        final boolean[] keeps = createKeepForOldWorkflows(loader);
        final List<ImmutableColumnTransformation<T>> columnTransformations =
            Pre44Loader.createColumnTransformations(fullKnimeSpec, productionPaths, originalNames, positions, keeps);
        final RawSpec<T> rawSpec = loader.createRawSpec(fullKnimeSpec, productionPaths, originalNames);
        final boolean enforceTypes = false;
        final boolean includeUnknownColumns = true;
        final boolean skipEmptyColumns = false;
        final int newColPosition = unionSize;
        return new ImmutableTableTransformation<>(columnTransformations, rawSpec, columnFilterMode, newColPosition,
            includeUnknownColumns, enforceTypes, skipEmptyColumns);
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
    private ProductionPath[] reconstructProdPathsFor42Intersection(final Pre44Loader loader) {
        final Set<String> allColumns = loader.getAllColumnNames();
        final DataTableSpec loadedKnimeSpec = loader.getLoadedKnimeSpec();
        if (allColumns.size() == loadedKnimeSpec.getNumColumns()) {
            // |intersection| == |union| therefore we have production paths for all columns
            return loader.getProductionPaths();
        }
        final List<ProductionPath> allProdPaths = new ArrayList<>(allColumns.size());
        final Iterator<ProductionPath> loadedProdPaths = Iterators.forArray(loader.getProductionPaths());
        for (String col : allColumns) {
            if (loadedKnimeSpec.containsName(col)) {
                allProdPaths.add(loadedProdPaths.next());
            } else {
                allProdPaths.add(m_defaultPath);
            }
        }
        return allProdPaths.toArray(new ProductionPath[0]);
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
    private static boolean[] createKeepForOldWorkflows(final Pre44Loader loader) {
        final Set<String> allColumns = loader.getAllColumnNames();
        final DataTableSpec spec = loader.getLoadedKnimeSpec();
        final boolean[] keep = new boolean[allColumns.size()];
        int i = 0;
        for (String colName : allColumns) {
            keep[i] = spec.containsName(colName);
            i++;
        }
        return keep;
    }

}
