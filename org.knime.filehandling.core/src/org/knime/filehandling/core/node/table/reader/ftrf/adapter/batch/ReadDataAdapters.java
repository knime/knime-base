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
 *   Apr 6, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.ftrf.adapter.batch;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.List;

import org.knime.core.columnar.data.BooleanData.BooleanReadData;
import org.knime.core.columnar.data.DoubleData.DoubleReadData;
import org.knime.core.columnar.data.IntData.IntReadData;
import org.knime.core.columnar.data.LocalDateData.LocalDateReadData;
import org.knime.core.columnar.data.LocalDateTimeData.LocalDateTimeReadData;
import org.knime.core.columnar.data.LocalTimeData.LocalTimeReadData;
import org.knime.core.columnar.data.LongData.LongReadData;
import org.knime.core.columnar.data.NullableReadData;
import org.knime.core.columnar.data.ObjectData.ObjectReadData;
import org.knime.core.columnar.data.StringData.StringReadData;
import org.knime.core.columnar.data.ZonedDateTimeData.ZonedDateTimeReadData;
import org.knime.filehandling.core.node.table.reader.ftrf.adapter.batch.ValueAccess.BooleanAccess;
import org.knime.filehandling.core.node.table.reader.ftrf.adapter.batch.ValueAccess.DoubleAccess;
import org.knime.filehandling.core.node.table.reader.ftrf.adapter.batch.ValueAccess.IntAccess;
import org.knime.filehandling.core.node.table.reader.ftrf.adapter.batch.ValueAccess.LongAccess;
import org.knime.filehandling.core.node.table.reader.ftrf.adapter.batch.ValueAccess.ObjectAccess;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ReadDataAdapters {

    private ReadDataAdapters() {
        // utility class
    }

    static <V> NullableReadData createAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
        final ValueAccess valueAccess) {
        if (valueAccess instanceof IntAccess) {
            return new IntReadDataAdapter<V>(batch, columnIndex, (IntAccess)valueAccess);
        } else if (valueAccess instanceof LongAccess) {
            return new LongReadDataAdapter<V>(batch, columnIndex, (LongAccess)valueAccess);
        } else if (valueAccess instanceof DoubleAccess) {
            return new DoubleReadDataAdapter<V>(batch, columnIndex, (DoubleAccess)valueAccess);
        } else if (valueAccess instanceof BooleanAccess) {
            return new BooleanReadDataAdapter<V>(batch, columnIndex, (BooleanAccess)valueAccess);
        } else {
            final Class<?> returnType = valueAccess.getReturnType();
            if (returnType == String.class) {
                return new StringReadDataAdapter<V>(batch, columnIndex, (ObjectAccess<String, V>)valueAccess);
            } else if (returnType == LocalDateTime.class) {
                return new LocalDateTimeReadDataAdapter<V>(batch, columnIndex,
                    (ObjectAccess<LocalDateTime, V>)valueAccess);
            } else if (returnType == LocalDate.class) {
                return new LocalDateReadDataAdapter<V>(batch, columnIndex, (ObjectAccess<LocalDate, V>)valueAccess);
            } else if (returnType == ZonedDateTime.class) {
                return new ZonedDateTimeReadDataAdapter<V>(batch, columnIndex,
                    (ObjectAccess<ZonedDateTime, V>)valueAccess);
            } else if (returnType == LocalTime.class) {
                return new LocalTimeReadDataAdapter<V>(batch, columnIndex, (ObjectAccess<LocalTime, V>)valueAccess);
            } else {
                throw new IllegalArgumentException("Unsupported ValueAccess: " + valueAccess);
            }
        }
    }

    private abstract static class AbstractReadDataAdapter<V> implements NullableReadData {

        private final List<RandomAccessible<V>> m_batch;

        private final int m_columnIndex;

        AbstractReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex) {
            m_batch = batch;
            m_columnIndex = columnIndex;
        }

        protected final V getValue(final int rowIndex) {
            return m_batch.get(rowIndex).get(m_columnIndex);
        }

        @Override
        public final boolean isMissing(final int index) {
            return getValue(index) == null;
        }

        @Override
        public void retain() {
        }

        @Override
        public void release() {
        }

        @Override
        public long sizeOf() {
            return 0;
        }

        @Override
        public int length() {
            return m_batch.size();
        }
    }

    private static final class IntReadDataAdapter<V> extends AbstractReadDataAdapter<V> implements IntReadData {

        private final IntAccess<V> m_access;

        IntReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex, final IntAccess<V> access) {
            super(batch, columnIndex);
            m_access = access;
        }

        @Override
        public int getInt(final int index) {
            return m_access.getInt(getValue(index));
        }

    }

    private static final class LongReadDataAdapter<V> extends AbstractReadDataAdapter<V> implements LongReadData {

        private final LongAccess<V> m_access;

        LongReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex, final LongAccess<V> access) {
            super(batch, columnIndex);
            m_access = access;
        }

        @Override
        public long getLong(final int index) {
            return m_access.getLong(getValue(index));
        }

    }

    private static final class DoubleReadDataAdapter<V> extends AbstractReadDataAdapter<V> implements DoubleReadData {

        private final DoubleAccess<V> m_access;

        DoubleReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final DoubleAccess<V> access) {
            super(batch, columnIndex);
            m_access = access;
        }

        @Override
        public double getDouble(final int index) {
            return m_access.getDouble(getValue(index));
        }
    }

    private static final class BooleanReadDataAdapter<V> extends AbstractReadDataAdapter<V> implements BooleanReadData {

        private final BooleanAccess<V> m_access;

        BooleanReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final BooleanAccess<V> access) {
            super(batch, columnIndex);
            m_access = access;
        }

        @Override
        public boolean getBoolean(final int index) {
            return m_access.getBoolean(getValue(index));
        }

    }

    private abstract static class ObjectReadDataAdapter<O, V> extends AbstractReadDataAdapter<V>
        implements ObjectReadData<O> {

        protected final ObjectAccess<O, V> m_access;

        ObjectReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final ObjectAccess<O, V> access) {
            super(batch, columnIndex);
            m_access = access;
        }

        @Override
        public final O getObject(final int index) {
            return m_access.getObject(getValue(index));
        }

    }

    private static final class StringReadDataAdapter<V> extends ObjectReadDataAdapter<String, V>
        implements StringReadData {

        StringReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final ObjectAccess<String, V> stringAccess) {
            super(batch, columnIndex, stringAccess);
        }

        @Override
        public String getString(final int index) {
            return getObject(index);
        }

    }

    private static final class LocalDateTimeReadDataAdapter<V> extends ObjectReadDataAdapter<LocalDateTime, V>
        implements LocalDateTimeReadData {

        LocalDateTimeReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final ObjectAccess<LocalDateTime, V> access) {
            super(batch, columnIndex, access);
            // TODO Auto-generated constructor stub
        }

        @Override
        public LocalDateTime getLocalDateTime(final int index) {
            return getObject(index);
        }
    }

    private static final class LocalDateReadDataAdapter<V> extends ObjectReadDataAdapter<LocalDate, V>
        implements LocalDateReadData {

        LocalDateReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final ObjectAccess<LocalDate, V> access) {
            super(batch, columnIndex, access);
            // TODO Auto-generated constructor stub
        }

        @Override
        public LocalDate getLocalDate(final int index) {
            return getObject(index);
        }
    }

    private static final class ZonedDateTimeReadDataAdapter<V> extends ObjectReadDataAdapter<ZonedDateTime, V>
        implements ZonedDateTimeReadData {

        ZonedDateTimeReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex,
            final ObjectAccess<ZonedDateTime, V> access) {
            super(batch, columnIndex, access);
            // TODO Auto-generated constructor stub
        }

        @Override
        public ZonedDateTime getZonedDateTime(final int index) {
            return getObject(index);
        }

    }

    private static final class LocalTimeReadDataAdapter<V> extends ObjectReadDataAdapter<LocalTime, V> implements LocalTimeReadData {

        LocalTimeReadDataAdapter(final List<RandomAccessible<V>> batch, final int columnIndex, final ObjectAccess<LocalTime, V> access) {
            super(batch, columnIndex, access);
        }

        @Override
        public LocalTime getLocalTime(final int index) {
            return getObject(index);
        }
    }

}
