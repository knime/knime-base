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
 *   28 Jun 2021 Moditha Hewasinghage: created
 */
package org.knime.filehandling.core.example.node;

import java.io.IOException;

import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.node.table.reader.TableReader;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec.TypedReaderTableSpecBuilder;

/**
 * Reader for the example CSV reader node.
 * 
 * Here we can manipulate the spec of the reader. In this case we read the first
 * row of the CSV to count the number of columns.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Germany, Germany
 */
final class ExampleCSVReader implements TableReader<ExampleCSVReaderConfig, Class<?>, String> {

    @Override
    public ExampleCSVRead read(final FSPath path, final TableReadConfig<ExampleCSVReaderConfig> config)
            throws IOException {
        return new ExampleCSVRead(path, config);
    }

    /**
     * Column names are generated with Column prefix All the columns have the type
     * String
     */
    @Override
    public TypedReaderTableSpec<Class<?>> readSpec(final FSPath path,
            final TableReadConfig<ExampleCSVReaderConfig> config, final ExecutionMonitor exec) throws IOException {
        try (final ExampleCSVRead read = new ExampleCSVRead(path, config)) {
            TypedReaderTableSpecBuilder<Class<?>> specBuilder = new TypedReaderTableSpecBuilder<>();
            final RandomAccessible<String> columns = read.next();
            for (int i = 0; i < columns.size(); i++) {
                specBuilder.addColumn(
                        String.format("%s%x", config.getReaderSpecificConfig().getColumnHeaderPrefix(), i),
                        String.class, true);
            }
            return specBuilder.build();
        }

    }

}
