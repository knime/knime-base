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
 *   Mar 27, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import java.nio.file.Path;
import java.util.Map;

import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 * Creates {@link MultiTableRead MultiTableReads} given a {@link Map} of {@link TypedReaderTableSpec} representing
 * tables that should be read together.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <T> the type representing external data types
 * @param <V> the type representing values
 */
@FunctionalInterface
public interface MultiTableReadFactory<T, V> {

    /**
     * Creates a {@link MultiTableRead} from the provided {@link TypedReaderTableSpec individualSpecs} and
     * {@link MultiTableReadConfig config}.
     *
     * @param rootPath the root directory of all {@link Path Paths} in the <b>individualSpecs</b>
     * @param individualSpecs a {@link Map} from {@link Path} to {@link TypedReaderTableSpec} where each
     *            {@link TypedReaderTableSpec} corresponds to the table stored in its corresponding {@link Path}
     * @param config user provided {@link MultiTableReadConfig}
     * @return a {@link MultiTableRead} for reading the tables stored in the keys of <b>individualSpecs</b>
     */
    MultiTableRead<V> create(final String rootPath, Map<Path, TypedReaderTableSpec<T>> individualSpecs,
        MultiTableReadConfig<?> config);

}