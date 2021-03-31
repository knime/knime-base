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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class MultiFastTableReadFactory<I, C extends ReaderSpecificConfig<C>, T>
    implements MultiTableReadFactory<I, C, T> {

    private final RawSpecFactory<T> m_rawSpecFactory;

    private final GenericTableReader<I, C, T, ?> m_reader;

    /**
     *
     */
    public MultiFastTableReadFactory(final GenericTableReader<I, C, T, ?> reader,
        final TypeHierarchy<T, T> typeHierarchy) {
        m_rawSpecFactory = new RawSpecFactory<>(typeHierarchy);
        m_reader = reader;
    }

    @Override
    public StagedMultiTableRead<I, T> create(final SourceGroup<I> sourceGroup, final MultiTableReadConfig<C, T> config,
        final ExecutionMonitor exec) throws IOException {
        final Map<I, FtrfBatchReadable<T>> batchReadables = readIndividualSpecs(sourceGroup, config, exec);
        final Map<I, TypedReaderTableSpec<T>> specs = batchReadables.entrySet().stream().collect(//
            toMap(//
                Entry::getKey, e -> e.getValue().getSpec(), //
                (i, j) -> i, //
                LinkedHashMap::new)//
            );
        final RawSpec<T> rawSpec = m_rawSpecFactory.create(specs.values());
        return new StagedMultiFastTableRead<>(rawSpec, align(batchReadables.values(), rawSpec),
            t -> DefaultTableSpecConfig.createFromTransformationModel(sourceGroup.getID(), config.getConfigID(), specs,
                t));
    }

    private static <T> List<FtrfBatchReadable<T>> align(final Collection<FtrfBatchReadable<T>> batchReadables,
        final RawSpec<T> rawSpec) {
        final FtrfBatchReadableAligner<T> unifier = new FtrfBatchReadableAligner<>(rawSpec.getUnion());
        return batchReadables.stream()//
            .map(unifier::align)//
            .collect(toList());
    }

    private Map<I, FtrfBatchReadable<T>> readIndividualSpecs(final SourceGroup<I> sourceGroup,
        final MultiTableReadConfig<C, T> config, final ExecutionMonitor exec) throws IOException {
        final Map<I, FtrfBatchReadable<T>> sourceTuples = new LinkedHashMap<>(sourceGroup.size());
        for (I item : sourceGroup) {
            TableReadConfig<C> tableReadConfig = config.getTableReadConfig();
            @SuppressWarnings("resource") // the readables are managed by the downstream reads
            FtrfBatchReadable<T> batchReadable = m_reader.readContent(item, tableReadConfig, exec);
            sourceTuples.put(item, batchReadable);
        }
        return sourceTuples;
    }

    @Override
    public StagedMultiTableRead<I, T> createFromConfig(final SourceGroup<I> sourceGroup,
        final MultiTableReadConfig<C, T> config) {
        // TODO Auto-generated method stub
        return null;
    }

}
