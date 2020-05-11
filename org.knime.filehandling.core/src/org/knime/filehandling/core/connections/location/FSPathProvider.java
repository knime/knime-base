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
 *   Apr 24, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections.location;

import java.io.IOException;

import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.connections.FSPath;

/**
 * Interface that provides a {@link FSPath} object. Since a {@link FSPath} object is always linked to a file system and
 * file system provider, which may block system resources (open streams, sockets, etc), this interface is also an
 * {@link AutoCloseable}. Invoking {@link #close()} will release any such blocked resources.
 *
 * <p>
 * The main use case of this interface are corner cases when you need to create a lot of {@link FSPath} instances, where
 * each one is linked to ad-hoc file system. In this case, this interface provides a way to close the ad-hoc file
 * system. The more common case however, is that all {@link FSPathProvider}s from the same factory share the same file
 * system instance. Note that the concrete behavior is an implementation detail of the concrete
 * {@link FSPathProviderFactory}.
 * </p>
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @see FSPathProviderFactory
 * @since 4.2
 */
public interface FSPathProvider extends AutoCloseable {

    /**
     * Provides an {@link FSPath} that corresponds to the {@link FSLocation} that was used
     * to create this {@link FSPathProvider} instance.
     *
     * @return the path instance for the underlying {@link FSLocation}.
     * @see FSPathProviderFactory#create(FSLocation)
     */
    FSPath getPath();

    @Override
    void close() throws IOException;
}
