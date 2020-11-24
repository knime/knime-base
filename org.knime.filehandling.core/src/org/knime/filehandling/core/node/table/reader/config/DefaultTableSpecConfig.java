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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.util.SerializeUtil;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Configuration storing all the information needed to create a {@link DataTableSpec}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DefaultTableSpecConfig extends GenericDefaultTableSpecConfig<Path> implements TableSpecConfig {

    static final private class DefaultProductionPathLoader implements ProductionPathLoader {

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
     * Constructor for testing.</br>
     * Clients should use {@link #createFromTransformationModel(String, Map, TableTransformation)}.
     *
     * @param rootItem root item
     * @param outputSpec the output {@link DataTableSpec}
     * @param individualSpecs the individual input specs
     * @param productionPaths the {@link ProductionPath}s to use
     * @param originalNames the original names
     * @param positionalMapping the positional map
     * @param keep keep column flag
     * @param newColPosition new column position
     * @param columnFilterMode {@link ColumnFilterMode}
     * @param includeUnknownColumns flag if unknown columns should be included
     * @param enforceTypes indicates whether configured KNIME types should be enforced even if the external type changes
     */
    public DefaultTableSpecConfig(final String rootItem, final DataTableSpec outputSpec,
        final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positionalMapping, final boolean[] keep, final int newColPosition,
        final ColumnFilterMode columnFilterMode, final boolean includeUnknownColumns, final boolean enforceTypes) {
        super(rootItem, outputSpec, individualSpecs, productionPaths, originalNames, positionalMapping, keep,
            newColPosition, columnFilterMode, includeUnknownColumns, enforceTypes);
    }

    /**
     * @param rootItem root item
     * @param outputSpec the output {@link DataTableSpec}
     * @param items string representation of the inputs to read from
     * @param individualSpecs the individual input specs
     * @param productionPaths the {@link ProductionPath}s to use
     * @param originalNames the original names
     * @param positionalMapping the positional map
     * @param keep keep column flag
     * @param newColPosition new column position
     * @param columnFilterMode {@link ColumnFilterMode}
     * @param includeUnknownColumns flag if unknown columns should be included
     * @param enforceTypes indicates whether configured KNIME types should be enforced even if the external type changes
     */
    public DefaultTableSpecConfig(final String rootItem, final DataTableSpec outputSpec, final String[] items,
        final ReaderTableSpec<?>[] individualSpecs, final ProductionPath[] productionPaths,
        final String[] originalNames, final int[] positionalMapping, final boolean[] keep, final int newColPosition,
        final ColumnFilterMode columnFilterMode, final boolean includeUnknownColumns, final boolean enforceTypes) {
        super(rootItem, outputSpec, items, individualSpecs, productionPaths, originalNames, positionalMapping, keep,
            newColPosition, columnFilterMode, includeUnknownColumns, enforceTypes);
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
    public static DefaultTableSpecConfig load(final NodeSettingsRO settings, final ProducerRegistry<?, ?> registry,
        final Object mostGenericExternalType, @SuppressWarnings("deprecation") final SpecMergeMode specMergeModeOld)
        throws InvalidSettingsException {
        final String rootPath = settings.getString(CFG_ROOT_PATH);
        final String[] paths = settings.getStringArray(CFG_FILE_PATHS);
        final ReaderTableSpec<?>[] individualSpecs =
            loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), paths.length);
        final Set<String> allColumns = union(individualSpecs);

        final boolean includeUnknownColumns = settings.getBoolean(CFG_INCLUDE_UNKNOWN, true);

        final boolean enforceTypes = settings.getBoolean(CFG_ENFORCE_TYPES, false);

        // For old workflows (created with 4.2), the spec might not contain all columns contained in union if
        // SpecMergeMode#INTERSECTION was used to create the final spec
        final DataTableSpec loadedKnimeSpec = DataTableSpec.load(settings.getConfig(CFG_DATATABLE_SPEC));
        final DataTableSpec fullKnimeSpec = constructFullKnimeSpec(allColumns, loadedKnimeSpec);

        ProductionPath[] allProdPaths = loadProductionPaths(settings, new DefaultProductionPathLoader(registry),
            mostGenericExternalType, allColumns, loadedKnimeSpec);

        final String[] originalNames = loadOriginalNames(fullKnimeSpec, settings);
        final int[] positionalMapping = loadPositionalMapping(fullKnimeSpec.getNumColumns(), settings);
        final boolean[] keep = loadKeep(loadedKnimeSpec, allColumns, settings);
        final int newColPosition = settings.getInt(CFG_NEW_COLUMN_POSITION, allProdPaths.length);
        final ColumnFilterMode columnFilterMode = loadColumnFilterMode(settings, specMergeModeOld);

        return new DefaultTableSpecConfig(rootPath, fullKnimeSpec, paths, individualSpecs, allProdPaths, originalNames,
            positionalMapping, keep, newColPosition, columnFilterMode, includeUnknownColumns, enforceTypes);
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
        GenericDefaultTableSpecConfig.validate(settings, new DefaultProductionPathLoader(registry));
    }

    /**
     * Creates a {@link DefaultTableSpecConfig} that corresponds to the provided parameters.
     *
     * @param <T> the type used to identify external types
     * @param rootPath if it represents a folder then all keys in the <b>individualSpecs<b> must be contained in this
     *            folder, otherwise the <b>rootPath</b> equals the {@link Path#toString()} version of the
     *            <b>individualSpecs<b> key and <b>individualSpecs<b> contains only a single element.
     * @param individualSpecs a map from the path/file to its individual {@link ReaderTableSpec}
     * @param tableTransformation defines the transformation (type-mapping, filtering, renaming and reordering) of the
     *            output spec
     * @return a {@link DefaultTableSpecConfig} for the provided parameters
     */
    public static <T> DefaultTableSpecConfig createFromTransformationModel(final String rootPath,
        final Map<Path, ? extends ReaderTableSpec<?>> individualSpecs,
        final TableTransformation<T> tableTransformation) {
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
        return new DefaultTableSpecConfig(rootPath, new DataTableSpec(columns.toArray(new DataColumnSpec[0])),
            individualSpecs, productionPaths.toArray(new ProductionPath[0]), originalNames.toArray(new String[0]),
            positions, keep, tableTransformation.getPositionForUnknownColumns(),
            tableTransformation.getColumnFilterMode(), tableTransformation.keepUnknownColumns(),
            tableTransformation.enforceTypes());
    }
}
