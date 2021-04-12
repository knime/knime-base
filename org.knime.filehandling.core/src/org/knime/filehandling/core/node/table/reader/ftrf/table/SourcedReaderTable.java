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
package org.knime.filehandling.core.node.table.reader.ftrf.table;

import java.io.IOException;

import org.knime.core.columnar.ColumnarSchema;
import org.knime.filehandling.core.node.table.reader.ftrf.requapi.Cursor;
import org.knime.filehandling.core.node.table.reader.ftrf.requapi.RowAccessible;
import org.knime.filehandling.core.node.table.reader.ftrf.requapi.RowReadAccess;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SourcedReaderTable<T> {

    private final RowAccessible m_batchReadable;

    private final TypedReaderTableSpec<T> m_spec;

    public SourcedReaderTable(final TypedReaderTableSpec<T> spec, final RowAccessible batchReadable, final String source) {
        m_batchReadable = new SourceAwareRandomAccessible(batchReadable, source);
        m_spec = spec;
    }

    public RowAccessible getRowAccessible() {
        return m_batchReadable;
    }

    public TypedReaderTableSpec<T> getSpec() {
        return m_spec;
    }

    interface SourceStringProvider {
        String getSource();
    }

    static final class SourceIOException extends IOException implements SourceStringProvider {

        private static final long serialVersionUID = 1L;

        private final String m_source;

        SourceIOException(final String message, final String source, final Throwable cause) {
            super(message, cause);
            m_source = source;
        }

        @Override
        public String getSource() {
            return m_source;
        }

    }

    static final class SourceRuntimeException extends RuntimeException implements SourceStringProvider {

        private static final long serialVersionUID = 1L;

        private final String m_source;

        SourceRuntimeException(final String message, final String source, final Throwable cause) {
            super(message, cause);
            m_source = source;
        }

        @Override
        public String getSource() {
            return m_source;
        }
    }

    private static final class SourceAwareRandomAccessible implements RowAccessible {

        private final RowAccessible m_delegate;

        private final String m_source;

        SourceAwareRandomAccessible(final RowAccessible delegate, final String source) {
            m_delegate = delegate;
            m_source = source;
        }

        @Override
        public ColumnarSchema getSchema() {
            return m_delegate.getSchema();
        }

        @Override
        public void close() throws Exception { // TODO handle as well?
            try {
                m_delegate.close();
            } catch (IOException ex) {
                throw wrapIOException(ex);
            } catch (RuntimeException ex) {
                throw wrapRuntimeException(ex);
            }
        }

        private SourceIOException wrapIOException(final IOException ex) {
            return new SourceIOException(ex.getMessage(), m_source, ex);
        }

        private SourceRuntimeException wrapRuntimeException(final RuntimeException ex) {
            return new SourceRuntimeException(ex.getMessage(), m_source, ex);
        }


        private final class SourceAwareRowReadAccessCursor implements Cursor<RowReadAccess> {

            private final Cursor<RowReadAccess> m_delegateReader;

            SourceAwareRowReadAccessCursor(final Cursor<RowReadAccess> delegateCursor) {
                m_delegateReader = delegateCursor;
            }

            @Override
            public void close() {
                try {
                    m_delegateReader.close();
                } catch (RuntimeException ex) {
                    throw wrapRuntimeException(ex);
                }
            }

            @Override
            public RowReadAccess forward() {
                try {
                    return m_delegateReader.forward();
                } catch (RuntimeException ex) {
                    throw wrapRuntimeException(ex);
                }
            }

        }

        @SuppressWarnings("resource")// the delegate cursor is closed by the SourceAwareRowReadAccessCursor
        @Override
        public Cursor<RowReadAccess> cursor() {
            return new SourceAwareRowReadAccessCursor(m_delegate.cursor());
        }

    }

}
