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
package org.knime.filehandling.core.node.table.reader;

import java.io.IOException;
import java.util.Map;

import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.util.MultiTableRead;
import org.knime.filehandling.core.node.table.reader.util.StagedMultiTableRead;

/**
 * Creates {@link MultiTableRead MultiTableReads} given a {@link Map} of {@link TypedReaderTableSpec} representing
 * tables that should be read together, or based on a stored {@link TableSpecConfig}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <I> the item type to read from
 * @param <C> the type of {@link ReaderSpecificConfig}
 * @param <T> the type representing external data types
 * @noimplement non-public API
 * @noreference non-public API
 */
public interface MultiTableReadFactory<I, C extends ReaderSpecificConfig<C>, T> {

    /**
     * Creates a {@link StagedMultiTableRead} for the provided parameters.</br>
     * Note that a {@link TableSpecConfig} stored in {@link MultiTableReadConfig} will be ignored i.e. the table spec is
     * always calculated.
     *
     * @param sourceGroup the {@link SourceGroup} to read from
     * @param config contains the user configuration
     * @param exec used to monitor the spec creation
     * @return a {@link StagedMultiTableRead} for the provided parameters
     * @throws IOException if an {@link IOException} occurs while creating the table spec
     */
    StagedMultiTableRead<I, T> create(SourceGroup<I> sourceGroup, MultiTableReadConfig<C, T> config,
        ExecutionMonitor exec) throws IOException;

    /**
     * Creates a {@link StagedMultiTableRead} for the given {@link SourceGroup} with the {@link TableSpecConfig} stored
     * in the provided {@link MultiTableReadConfig}. This means that {@link MultiTableReadConfig config} MUST have been
     * configured for {@link SourceGroup sourceGroup}! The specs aren't recalculated and the configured
     * {@link TableTransformation} is used by default.
     *
     * @param sourceGroup the {@link SourceGroup} to read from
     * @param config user provided {@link MultiTableReadConfig}
     * @return a {@link MultiTableRead} for reading the tables from the given items
     */
    StagedMultiTableRead<I, T> createFromConfig(SourceGroup<I> sourceGroup, MultiTableReadConfig<C, T> config);

}