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
 *   26 Nov 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.knime.core.data.BoundedValue;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.DataValueComparator;
import org.knime.core.data.ExtensibleUtilityFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterFactory;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.java.DataCellToJavaConverter;
import org.knime.core.data.convert.java.DataCellToJavaConverterFactory;
import org.knime.core.data.convert.java.DataCellToJavaConverterRegistry;
import org.knime.core.data.filestore.FileStoreFactory;
import org.knime.core.node.InvalidSettingsException;

/**
 * Tests for the equality operators derived by default if a data type does not register their own operators for equals,
 * not equals, etc.
 *
 * Note: it is easier to set up this unit test than to rely on testflows using data types that happen to fit our use
 * case.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
final class DefaultEqualityOperatorsTest {

    // define Dummy data type with comparator but do not register any operators

    // must be public
    public interface DummyBoundedValue extends DataValue, BoundedValue {

        UtilityFactory UTILITY = new DummyUtilityFactory();

        final class DummyUtilityFactory extends ExtensibleUtilityFactory {
            private DummyUtilityFactory() {
                super(DummyBoundedValue.class);
            }

            @Override
            public String getName() {
                return "Dummy Bounded Value";
            }

            @Override
            protected DataValueComparator getComparator() {
                return new DataValueComparator() {
                    @Override
                    protected int compareDataValues(final DataValue v1, final DataValue v2) {
                        return Integer.compare(((DummyBoundedValue)v1).getDummyValue(),
                            ((DummyBoundedValue)v2).getDummyValue());
                    }
                };
            }
        }

        int getDummyValue();
    }

    static final class DummyCell extends DataCell implements DummyBoundedValue {

        static final DataType TYPE = DataType.getType(DummyCell.class);

        private final int m_value;

        DummyCell(final int value) {
            m_value = value;
        }

        @Override
        public int getDummyValue() {
            return m_value;
        }

        @Override
        public String toString() {
            return String.valueOf(m_value);
        }

        @Override
        protected boolean equalsDataCell(final DataCell dc) {
            if (!(dc instanceof DummyCell)) {
                return false;
            }
            return m_value == ((DummyCell)dc).m_value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(m_value);
        }
    }

    static final class ToDataCellFactory implements JavaToDataCellConverterFactory<String> {

        @Override
        public DataType getDestinationType() {
            return DummyCell.TYPE;
        }

        @Override
        public Class<?> getSourceType() {
            return String.class;
        }

        @Override
        public String getIdentifier() {
            return "dummy-string-to-datacell";
        }

        @Override
        public JavaToDataCellConverter<String> create(final FileStoreFactory fileStoreFactory) {
            return source -> new DummyCell(Integer.parseInt(source));
        }
    }

    static final class FromDataCellFactory implements DataCellToJavaConverterFactory<DummyBoundedValue, String> {

        @Override
        public Class<?> getDestinationType() {
            return String.class;
        }

        @Override
        public Class<? extends DataValue> getSourceType() {
            return DummyBoundedValue.class;
        }

        @Override
        public String getIdentifier() {
            return "dummy-datacell-to-string";
        }

        @Override
        public DataCellToJavaConverter<DummyBoundedValue, String> create() {
            return value -> String.valueOf(value.getDummyValue());
        }

    }

    @SuppressWarnings("static-method")
    @Test
    void testEqualityOperators() throws InvalidSettingsException {
        assertFalse(DefaultEqualityOperators.isApplicable(DummyCell.TYPE),
            "Expected equality operators to not be applicable for the dummy type, "
                + "since it is not yet registered with type mapping");
        JavaToDataCellConverterRegistry.getInstance().register(new ToDataCellFactory());
        DataCellToJavaConverterRegistry.getInstance().register(new FromDataCellFactory());
        assertTrue(DefaultEqualityOperators.isApplicable(DummyCell.TYPE),
            "Expected equality operators to be applicable for the dummy type, "
                + "after it has been registered with type mapping");

        final var runtimeColumnSpec = new DataColumnSpecCreator("DummyCol", DummyCell.TYPE).createSpec();
        testEqualsOperator(runtimeColumnSpec);
        testNotEqualsOperator(runtimeColumnSpec);
        testNotEqualsNorMissingOperator(runtimeColumnSpec);
    }

    private static void testEqualsOperator(final org.knime.core.data.DataColumnSpec runtimeColumnSpec)
        throws InvalidSettingsException {
        final var equalsOp = new DefaultEqualityOperators.EqualDefault();

        final var one = new SingleStringParameters("1");
        final var pred = equalsOp.createPredicate(runtimeColumnSpec, DummyCell.TYPE, one);

        assertFalse(pred.test(new DummyCell(0)), "expected 0 != 1");
        assertTrue(pred.test(new DummyCell(1)), "expected 1 == 1");
        assertFalse(pred.test(new DummyCell(2)), "expected 2 != 1");
        assertFalse(pred.test(DataType.getMissingCell()), "expected missing cell != 1");
        assertFalse(equalsOp.mapMissingTo(), "expected operator to map missing cells to false");
    }

    private static void testNotEqualsOperator(final org.knime.core.data.DataColumnSpec runtimeColumnSpec)
        throws InvalidSettingsException {
        final var notEqualsOp = new DefaultEqualityOperators.NotEqualDefault();

        final var one = new SingleStringParameters("1");
        final var pred = notEqualsOp.createPredicate(runtimeColumnSpec, DummyCell.TYPE, one);

        assertTrue(pred.test(new DummyCell(0)), "expected 0 != 1");
        assertFalse(pred.test(new DummyCell(1)), "expected 1 == 1, so not (1 != 1)");
        assertTrue(pred.test(new DummyCell(2)), "expected 2 != 1");
        assertTrue(notEqualsOp.mapMissingTo(), "expected operator to map missing cells to true");
    }

    private static void testNotEqualsNorMissingOperator(final org.knime.core.data.DataColumnSpec runtimeColumnSpec)
        throws InvalidSettingsException {
        final var notEqualsNorMissingOp = new DefaultEqualityOperators.NotEqualNorMissingDefault();

        final var one = new SingleStringParameters("1");
        final var pred = notEqualsNorMissingOp.createPredicate(runtimeColumnSpec, DummyCell.TYPE, one);

        assertTrue(pred.test(new DummyCell(0)), "expected 0 != 1");
        assertFalse(pred.test(new DummyCell(1)), "expected 1 == 1, so not (1 != 1)");
        assertTrue(pred.test(new DummyCell(2)), "expected 2 != 1");
        assertFalse(notEqualsNorMissingOp.mapMissingTo(), "expected operator to map missing cells to false");
    }
}