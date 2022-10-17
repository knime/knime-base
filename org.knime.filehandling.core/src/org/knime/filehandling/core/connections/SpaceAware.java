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
 *   Apr 22, 2022 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.util.List;

/**
 * An interface providing methods for handling spaces.
 *
 * @author Zkriya Rakhimberdiyev
 */
public interface SpaceAware {

    /**
     * Wrapper object for space details.
     *
     * @author Zkriya Rakhimberdiyev
     */
    public static class Space {

        private final String m_name;

        private final String m_owner;

        private final boolean m_isPrivate;

        private final String m_spaceId;

        private final String m_path;

        /**
         * Space constructor.
         *
         * @param name Name of the Space.
         * @param owner Name of the account that owns the Space.
         * @param isPrivate True when this is a private Space, false if it is public.
         * @param spaceId space id Repository item ID (aka KNIME ID) of the Space.
         * @param path Path of the space within the file tree of the Hub, e.g. /Users/joe/Private. For shared spaces
         *            this is the actual path of the Space.
         */
        public Space(final String name, //
            final String owner, //
            final boolean isPrivate, //
            final String spaceId,
            final String path) {

            m_name = name;
            m_owner = owner;
            m_isPrivate = isPrivate;
            m_spaceId = spaceId;
            m_path = path;
        }

        /**
         * @return the name of the Space.
         */
        public String getName() {
            return m_name;
        }

        /**
         * @return the name of the owner account.
         */
        public String getOwner() {
            return m_owner;
        }

        /**
         * @return true if this is a private Space, false if it is a public Space.
         */
        public boolean isPrivate() {
            return m_isPrivate;
        }

        /**
         * @return the repository item ID (aka KNIME ID) of the Space.
         */
        public String getSpaceId() {
            return m_spaceId;
        }

        /**
         * @return path of the space within the file tree of the Hub, e.g. /Users/joe/Private. For shared spaces this is
         *         the actual path of the Space.
         */
        public String getPath() {
            return m_path;
        }
    }

    /**
     * Gets all the Hub Spaces accessible to the user.
     *
     * @return list of {@link Space}s.
     * @throws IOException
     */
    List<Space> getSpaces() throws IOException;

    /**
     * Gets the space by it's id.
     *
     * @param id The space id.
     * @return The space.
     * @throws IOException
     */
    Space getSpace(String id) throws IOException;
}
