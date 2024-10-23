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
 *   21 Jan 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.IntValue;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.datacell.SimpleJavaToDataCellConverterFactory;

/**
 * Extension that takes care of registering the dummy data cell for the Row Filter tests.
 * Use it in a test class by annotating the class with {@code @ExtendWith(FilterDummyDataCellExtension.class)}.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class FilterDummyDataCellExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(final ExtensionContext context) throws Exception {
        // other data types are supported by the Row Filter, if the input widget supports it
        // (which is currently done via type mapping from String -> cell)
        final var registry = JavaToDataCellConverterRegistry.getInstance();
        registry.register(
            new SimpleJavaToDataCellConverterFactory<>(String.class, FilterDummyCell.TYPE, FilterDummyCell::new));
    }


    /**
     * Dummy cell for testing "generic" data cell handling in the Row Filter components.
     */
    @SuppressWarnings("serial")
    public static final class FilterDummyCell extends DataCell implements BoundedValue, IntValue {

        /**
         * Type annotation.
         */
        public static final DataType TYPE = DataType.getType(FilterDummyCell.class);

        /**
         * Constructor.
         *
         * @param serializedValue parsed but not stored value
         */
        public FilterDummyCell(final String serializedValue) {
            Integer.parseInt(serializedValue);
        }

        @Override
        public int getIntValue() {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

        @Override
        public int hashCode() {
            throw new UnsupportedOperationException("Not expected to be called during test.");
        }

    }

}
