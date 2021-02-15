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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.ImmutableTableTransformation;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigIDSerializer.EmptyConfigID;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;

/**
 * Utility class for configs stored before KNIME AP 4.4.0.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class Pre44Loader {

    /** Individual spec config key. */
    private static final String CFG_INDIVIDUAL_SPECS = "individual_specs" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_INDIVIDUAL_SPEC = "individual_spec_";

    /** Root path/item config key. */
    private static final String CFG_ROOT_PATH = "root_path" + SettingsModel.CFGKEY_INTERNAL;

    /** File path config key. */
    private static final String CFG_FILE_PATHS = "file_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATHS = "production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_NUM_PRODUCTION_PATHS = "num_production_paths" + SettingsModel.CFGKEY_INTERNAL;

    private static final String CFG_PRODUCTION_PATH = "production_path_";

    /** Table spec config key. */
    private static final String CFG_DATATABLE_SPEC = "datatable_spec" + SettingsModel.CFGKEY_INTERNAL;

    private final String m_sourceGroupID;

    private final String[] m_items;

    private final List<ReaderTableSpec<?>> m_individualSpecs;

    private final DataTableSpec m_loadedKnimeSpec;

    private final Set<String> m_allColumnNames;

    private final ProductionPath[] m_productionPaths;

    Pre44Loader(final NodeSettingsRO settings, final ProductionPathSerializer prodPathLoader)
        throws InvalidSettingsException {
        m_sourceGroupID = settings.getString(CFG_ROOT_PATH);
        m_items = settings.getStringArray(CFG_FILE_PATHS);
        m_individualSpecs = loadIndividualSpecs(settings.getNodeSettings(CFG_INDIVIDUAL_SPECS), m_items.length);
        m_loadedKnimeSpec = DataTableSpec.load(settings.getConfig(CFG_DATATABLE_SPEC));
        m_allColumnNames = union(m_individualSpecs);
        m_productionPaths =
            loadProductionPathsFromSettings(settings.getNodeSettings(CFG_PRODUCTION_PATHS), prodPathLoader);
    }

    private static ProductionPath[] loadProductionPathsFromSettings(final NodeSettingsRO settings,
        final ProductionPathSerializer loader) throws InvalidSettingsException {
        final ProductionPath[] prodPaths = new ProductionPath[settings.getInt(CFG_NUM_PRODUCTION_PATHS)];
        for (int i = 0; i < prodPaths.length; i++) {
            prodPaths[i] = loader.loadProductionPath(settings, CFG_PRODUCTION_PATH + i);
        }
        return prodPaths;
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

    DataTableSpec getLoadedKnimeSpec() {
        return m_loadedKnimeSpec;
    }

    Set<String> getAllColumnNames() {
        return m_allColumnNames;
    }

    ProductionPath[] getProductionPaths() {
        return m_productionPaths;
    }

    <T> DefaultTableSpecConfig<T> createTableSpecConfig(final ImmutableTableTransformation<T> tableTransformation) {
        Collection<TypedReaderTableSpec<T>> typedIndividualSpecs = getTypedIndividualSpecs(tableTransformation);
        return new DefaultTableSpecConfig<>(m_sourceGroupID, EmptyConfigID.INSTANCE, m_items, typedIndividualSpecs,
            tableTransformation);
    }

    private <T> Collection<TypedReaderTableSpec<T>>
        getTypedIndividualSpecs(final TableTransformation<T> transformation) {
        final TypeAssigner<T> typeAssigner = new TypeAssigner<>(transformation);
        return m_individualSpecs.stream()//
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

    <T> RawSpec<T> createRawSpec(final DataTableSpec rawKnimeSpec, final ProductionPath[] productionPaths,
        final String[] originalNames) {
        assert rawKnimeSpec
            .getNumColumns() == productionPaths.length : "Number of production paths doesn't match the number of columns.";
        final List<TypedReaderColumnSpec<T>> specs = new ArrayList<>(rawKnimeSpec.getNumColumns());
        for (int i = 0; i < rawKnimeSpec.getNumColumns(); i++) {
            final ProductionPath productionPath = productionPaths[i];
            @SuppressWarnings("unchecked") // the production path stores the source type of type T
            final T type = (T)productionPath.getProducerFactory().getSourceType();
            // hasType was always assumed to be true before 4.4.0
            specs.add(TypedReaderColumnSpec.createWithName(originalNames[i], type, true));
        }
        final TypedReaderTableSpec<T> union = new TypedReaderTableSpec<>(specs);
        final TypedReaderTableSpec<T> intersection = findIntersection(union, m_individualSpecs);
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

    static <T> List<ImmutableColumnTransformation<T>> createColumnTransformations(final DataTableSpec fullKnimeSpec,
        final ProductionPath[] productionPaths, final String[] originalNames, final int[] positions,
        final boolean[] keeps) {
        final int unionSize = fullKnimeSpec.getNumColumns();
        final List<ImmutableColumnTransformation<T>> transformations = new ArrayList<>(unionSize);
        for (int i = 0; i < unionSize; i++) {
            final ProductionPath productionPath = productionPaths[i];
            @SuppressWarnings("unchecked")
            final T externalType = (T)productionPath.getProducerFactory().getSourceType();
            final TypedReaderColumnSpec<T> externalSpec =
                // hasType was assumed to be true before 4.4.0
                TypedReaderColumnSpec.createWithName(originalNames[i], externalType, true);
            final int position = positions[i];
            final String outputName = fullKnimeSpec.getColumnSpec(i).getName();
            final boolean keep = keeps[i];
            final ImmutableColumnTransformation<T> trans =
                new ImmutableColumnTransformation<>(externalSpec, productionPath, keep, position, outputName);
            transformations.add(trans);
        }
        return transformations;
    }

}
