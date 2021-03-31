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
 *   Mar 29, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf;

import static org.knime.filehandling.core.node.table.reader.util.MultiTableUtils.transformToString;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import org.knime.core.columnar.batch.SequentialBatchReadable;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.util.CheckUtils;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformationFactory;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class FtrfMultiTableReadFactory<I, C extends ReaderSpecificConfig<C>, T>
    implements MultiTableReadFactory<I, C, T> {

    private final RawSpecFactory<T> m_rawSpecFactory;

    private final GenericTableReader<I, C, T, ?> m_reader;

    private final DefaultTableTransformationFactory<T> m_tableTransformationFactory;

    /**
     *
     */
    public FtrfMultiTableReadFactory(final GenericTableReader<I, C, T, ?> reader,
        final TypeHierarchy<T, T> typeHierarchy, final ProductionPathProvider<T> productionPathProvider) {
        m_rawSpecFactory = new RawSpecFactory<>(typeHierarchy);
        m_reader = reader;
        m_tableTransformationFactory = new DefaultTableTransformationFactory<>(productionPathProvider);
    }

    @Override
    public StagedMultiTableRead<I, T> create(final SourceGroup<I> sourceGroup, final MultiTableReadConfig<C, T> config,
        final ExecutionMonitor exec) throws IOException {
        final Map<I, TypedReaderTableSpec<T>> specs =
            readIndividualSpecs(sourceGroup, config.getTableReadConfig(), exec);
        final RawSpec<T> rawSpec = m_rawSpecFactory.create(specs.values());
        return create(sourceGroup, specs, rawSpec, config);
    }

    private Map<I, TypedReaderTableSpec<T>> readIndividualSpecs(final SourceGroup<I> sourceGroup,
        final TableReadConfig<C> tableReadConfig, final ExecutionMonitor exec) throws IOException {
        final Map<I, TypedReaderTableSpec<T>> sourceTuples = new LinkedHashMap<>(sourceGroup.size());
        for (I item : sourceGroup) {
            final TypedReaderTableSpec<T> spec = m_reader.readSpec(item, tableReadConfig, exec);
            sourceTuples.put(item, spec);
        }
        return sourceTuples;
    }

    private StagedMultiTableRead<I, T> create(final SourceGroup<I> sourceGroup,
        final Map<I, TypedReaderTableSpec<T>> specs, final RawSpec<T> rawSpec,
        final MultiTableReadConfig<C, T> config) {
        final List<ReaderTable<T>> tables = toTables(specs, config.getTableReadConfig());
        final Supplier<TableTransformation<T>> defaultTransformationSupplier =
            createDefaultTransformationSupplier(sourceGroup, config, rawSpec);
        return new FtrfStagedMultiTableRead<>(rawSpec, tables, t -> DefaultTableSpecConfig
            .createFromTransformationModel(sourceGroup.getID(), config.getConfigID(), specs, t),
            defaultTransformationSupplier);
    }

    private List<ReaderTable<T>> toTables(final Map<I, TypedReaderTableSpec<T>> specs,
        final TableReadConfig<C> config) {
        // TODO virtually align tables so that all have the same columns (except for type) in the same order as union
        // TODO This will make it easy to do pushdown of the column selection later on
        final List<ReaderTable<T>> tables = new ArrayList<>();
        for (Entry<I, TypedReaderTableSpec<T>> entry : specs.entrySet()) {
            final TypedReaderTableSpec<T> spec = entry.getValue();
            final I item = entry.getKey();
            final SequentialBatchReadable readable = m_reader.readContent(item, config, spec);
            tables.add(new ReaderTable<>(spec, readable, item.toString()));
        }
        return tables;
    }

    private Supplier<TableTransformation<T>> createDefaultTransformationSupplier(final SourceGroup<I> sourceGroup,
        final MultiTableReadConfig<C, T> config, final RawSpec<T> rawSpec) {
        if (config.hasTableSpecConfig()) {
            final TableSpecConfig<T> tableSpecConfig = config.getTableSpecConfig();
            final TableTransformation<T> configuredTransformation = tableSpecConfig.getTableTransformation();
            if (tableSpecConfig.isConfiguredWith(config.getConfigID(), transformToString(sourceGroup))) {
                return () -> configuredTransformation;
            } else {
                return () -> m_tableTransformationFactory.createFromExisting(rawSpec, config, configuredTransformation);
            }
        } else {
            return () -> m_tableTransformationFactory.createNew(rawSpec, config);
        }
    }

    @Override
    public StagedMultiTableRead<I, T> createFromConfig(final SourceGroup<I> sourceGroup,
        final MultiTableReadConfig<C, T> config) {
        CheckUtils.checkArgument(config.hasTableSpecConfig(), "No TableSpecConfig available");
        CheckUtils.checkArgument(config.isConfiguredWith(MultiTableUtils.transformToString(sourceGroup)),
            "The config has not been created with the provided SourceGroup.");
        final TableSpecConfig<T> tableSpecConfig = config.getTableSpecConfig();
        final Map<I, TypedReaderTableSpec<T>> specs = extractItemToSpecMap(sourceGroup, tableSpecConfig);
        return create(sourceGroup, specs, tableSpecConfig.getRawSpec(), config);
    }

    private final Map<I, TypedReaderTableSpec<T>> extractItemToSpecMap(final SourceGroup<I> sourceGroup,
        final TableSpecConfig<T> tableSpecConfig) {
        final Map<I, TypedReaderTableSpec<T>> specs = new LinkedHashMap<>(sourceGroup.size());
        for (I item : sourceGroup) {
            final TypedReaderTableSpec<T> spec = tableSpecConfig.getSpec(item.toString());
            specs.put(item, spec);
        }
        return specs;
    }

}
