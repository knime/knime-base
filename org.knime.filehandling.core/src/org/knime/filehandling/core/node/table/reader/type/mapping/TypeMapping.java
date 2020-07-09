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
package org.knime.filehandling.core.node.table.reader.type.mapping;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.spec.ReaderTableSpec;

/**
 * Represents a mapping from external types to KNIME types.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of values
 * @noreference non-public API
 * @noimplement non-public API
 */
public interface TypeMapping<V> {

    /**
     * Creates a {@link TypeMapper} for mapping from external types to KNIME types.
     *
     * @param fsFactory used to create DataCells backed by file stores
     * @return a {@link TypeMapper} that performs the actual mapping
     */
    TypeMapper<V> createTypeMapper(FileStoreFactory fsFactory);

    /**
     * Creates the {@link DataTableSpec} that corresponds to {@link ReaderTableSpec spec} according to this
     * {@link TypeMapping}.
     *
     * @param spec the {@link ReaderTableSpec} to map to a {@link DataTableSpec}
     * @return the {@link DataTableSpec} corresponding to {@link ReaderTableSpec spec} and this {@link TypeMapping}
     */
    DataTableSpec map(ReaderTableSpec<?> spec);

    /**
     * Returns the {@link ProductionPath ProductionPaths} used for the type mapping.
     *
     * @return the type mapping {@link ProductionPath ProductionPaths}
     */
    ProductionPath[] getProductionPaths();

}