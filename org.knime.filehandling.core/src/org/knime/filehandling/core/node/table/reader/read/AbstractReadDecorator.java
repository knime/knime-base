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
 *   Nov 15, 2020 (Tobias): created
 */
package org.knime.filehandling.core.node.table.reader.read;

import java.io.IOException;
import java.util.OptionalLong;

import org.knime.core.node.util.CheckUtils;


/**
 * An abstract implementation of a decorator for {@link Read} objects.</br>
 * Handles methods like {@link Read#getMaxProgress()}, {@link Read#getProgress()} and {@link Read#close()} by
 * delegating to the underlying read object.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 * @param <V> the type of values returned by this read
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractReadDecorator<V> implements Read<V> {

    private final Read<V> m_source;

    /**
     * Constructor.
     *
     * @param source the {@link Read} to decorate
     */
    protected AbstractReadDecorator(final Read<V> source) {
        m_source = CheckUtils.checkArgumentNotNull(source, "The source must not be null.");
    }

    /**
     * Returns the decorated {@link Read}.
     *
     * @return the decorated {@link Read}
     */
    protected final Read<V> getSource() {
        return m_source;
    }

    @Override
    public OptionalLong getMaxProgress() {
        return m_source.getMaxProgress();
    }

    @Override
    public long getProgress() {
        return m_source.getProgress();
    }

    @Override
    public void close() throws IOException {
        m_source.close();
    }

    @Override
    public boolean needsDecoration() {
        return m_source.needsDecoration();
    }

}