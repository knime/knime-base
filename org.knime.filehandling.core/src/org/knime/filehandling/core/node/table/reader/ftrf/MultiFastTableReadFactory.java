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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.GenericTableReader;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.RawSpecFactory;
import org.knime.filehandling.core.node.table.reader.SourceGroup;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.util.MultiTableUtils;
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
        final List<FtrfSourceTuple<T>> sourceTuples = readIndividualSpecs(sourceGroup, config, exec);
        final RawSpec<T> rawSpec =
            m_rawSpecFactory.create(sourceTuples.stream().map(FtrfSourceTuple::getSpec).collect(toList()));
        return new StagedMultiFastTableRead<>(rawSpec, sourceTuples);
    }

    private List<FtrfSourceTuple<T>> readIndividualSpecs(final SourceGroup<I> sourceGroup,
        final MultiTableReadConfig<C, T> config, final ExecutionMonitor exec) throws IOException {
        final List<FtrfSourceTuple<T>> sourceTuples = new ArrayList<>(sourceGroup.size());
        for (I item : sourceGroup) {
            TableReadConfig<C> tableReadConfig = config.getTableReadConfig();
            final TypedReaderTableSpec<T> spec = MultiTableUtils.assignNamesIfMissing(
                m_reader.readSpec(item, tableReadConfig, exec.createSubProgress(1.0 / sourceGroup.size())));
            // TODO this feels like something the reader could give us directly
            final FtrfSourceTuple<T> sourceTuple =
                new FtrfSourceTuple<>(m_reader.readContent(item, tableReadConfig, spec), spec);
            sourceTuples.add(sourceTuple);
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
