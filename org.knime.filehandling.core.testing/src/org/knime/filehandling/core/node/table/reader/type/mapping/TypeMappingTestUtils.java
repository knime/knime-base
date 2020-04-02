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
 *   Mar 30, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.type.mapping;

import java.util.Arrays;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.map.CellValueProducer;
import org.knime.core.data.convert.map.CellValueProducerFactory;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;

/**
 * Contains utility methods as well as necessary mock implementations for testing classes in the typemapping package.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class TypeMappingTestUtils {

    private TypeMappingTestUtils() {
        // static utility class
    }

    static ProductionPath[] mockProductionPaths(final String... externalTypes) {
        return Arrays.stream(externalTypes)//
            .map(TypeMappingTestUtils::mockProductionPath)//
            .toArray(ProductionPath[]::new);

    }

    static ProductionPath mockProductionPath(final String externalType) {
        return new ProductionPath(new TestCellValueProducerFactory(externalType),
            new TestJavaToDataCellConverterFactory());
    }

    static class TestReadAdapter extends ReadAdapter<String, String> {

    }

    static class TestCellValueProducerFactory
        implements CellValueProducerFactory<TestReadAdapter, String, String, ReadAdapterParams<TestReadAdapter>> {

        private final String m_sourceType;

        TestCellValueProducerFactory(final String sourceType) {
            m_sourceType = sourceType;
        }

        @Override
        public Class<?> getDestinationType() {
            return String.class;
        }

        @Override
        public String getSourceType() {
            return m_sourceType;
        }

        @Override
        public String getIdentifier() {
            return m_sourceType + " converter factory";
        }

        @Override
        public CellValueProducer<TestReadAdapter, String, ReadAdapterParams<TestReadAdapter>> create() {
            return (s, p) -> s.get(p);
        }

    }

    static class TestJavaToDataCellConverterFactory implements JavaToDataCellConverterFactory<String> {

        @Override
        public DataType getDestinationType() {
            return StringCell.TYPE;
        }

        @Override
        public Class<?> getSourceType() {
            return String.class;
        }

        @Override
        public String getIdentifier() {
            return "test converter";
        }

        @Override
        public JavaToDataCellConverter<String> create(final FileStoreFactory fileStoreFactory) {
            return StringCell::new;
        }

    }
}
