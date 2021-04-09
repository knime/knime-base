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
 *   Apr 9, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf.adapter.cursor;

import java.util.function.Function;

import org.knime.core.data.DataCell;
import org.knime.core.data.def.BooleanCell.BooleanCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.v2.access.BooleanAccess.BooleanReadAccess;
import org.knime.core.data.v2.access.DoubleAccess.DoubleReadAccess;
import org.knime.core.data.v2.access.IntAccess.IntReadAccess;
import org.knime.core.data.v2.access.LongAccess.LongReadAccess;
import org.knime.core.data.v2.access.ObjectAccess.ObjectReadAccess;
import org.knime.core.data.v2.access.ReadAccess;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public interface ReadAccessAdapter<V> extends ReadAccess {

    void setValue(final V value);

    interface DefaultedDoubleReadAccess extends DoubleReadAccess {
        @Override
        default double getCenterOfGravity() {
            return getDoubleValue();
        }

        @Override
        default double getImaginaryValue() {
            return 0;
        }

        @Override
        default double getCore() {
            return getDoubleValue();
        }

        @Override
        default double getMaxCore() {
            return getDoubleValue();
        }

        @Override
        default double getMaxSupport() {
            return getDoubleValue();
        }

        @Override
        default double getMinCore() {
            return getDoubleValue();
        }

        @Override
        default double getRealValue() {
            return getDoubleValue();
        }

        @Override
        default double getMinSupport() {
            return getDoubleValue();
        }

        @Override
        default DataCell getDataCell() {
            return new DoubleCell(getDoubleValue());
        }
    }

    interface DoubleReadAccessAdapter<V> extends ReadAccessAdapter<V>, DefaultedDoubleReadAccess {

    }

    interface IntReadAccessAdapter<V> extends ReadAccessAdapter<V>, DefaultedDoubleReadAccess, IntReadAccess {
        @Override
        default double getDoubleValue() {
            return getIntValue();
        }

        @Override
        default DataCell getDataCell() {
            return new IntCell(getIntValue());
        }
    }

    interface LongReadAccessAdapter<V> extends ReadAccessAdapter<V>, DefaultedDoubleReadAccess, LongReadAccess {

        @Override
        default double getDoubleValue() {
            return getLongValue();
        }

        @Override
        default DataCell getDataCell() {
            return new LongCell(getLongValue());
        }
    }

    interface BooleanReadAccessAdapter<V> extends ReadAccessAdapter<V>, DefaultedDoubleReadAccess, BooleanReadAccess {

        @Override
        default double getDoubleValue() {
            return getIntValue();
        }

        @Override
        default int getIntValue() {
            return getBooleanValue() ? 1 : 0;
        }

        @Override
        default long getLongValue() {
            return getIntValue();
        }

        @Override
        default DataCell getDataCell() {
            return BooleanCellFactory.create(getBooleanValue());
        }

    }

    interface ObjectReadAccessAdapter<V, O> extends ReadAccessAdapter<V>, ObjectReadAccess<O> {

    }

    public abstract class AbstractReadAccessAdapter<V> implements ReadAccessAdapter<V> {
        private V m_value;

        @Override
        public void setValue(final V value) {
            m_value = value;
        }

        @Override
        public boolean isMissing() {
            return m_value == null;
        }

        /**
         * @return the currently adapted value
         */
        public V getValue() {
            return m_value;
        }
    }

    public final class DefaultObjectReadAccessAdapter<V, O> extends AbstractReadAccessAdapter<V>
        implements ObjectReadAccessAdapter<V, O> {

        private final Function<V, O> m_converter;

        public DefaultObjectReadAccessAdapter(final Function<V, O> converter) {
            m_converter = converter;
        }

        @Override
        public O getObject() {
            return m_converter.apply(getValue());
        }

    }
}
