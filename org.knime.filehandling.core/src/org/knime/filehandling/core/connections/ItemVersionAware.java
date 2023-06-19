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
 *   Jun 13, 2023 (Zkriya Rakhimberdiyev): created
 */
package org.knime.filehandling.core.connections;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * This interface provides methods for working with Hub repository item versions. Some file system providers can
 * implement this interface to allow interacting with item versions.
 *
 * @author Zkriya Rakhimberdiyev, Redfield SE
 */
public interface ItemVersionAware {

    /**
     * Wrapper object for Item version details.
     */
    class RepositoryItemVersion {

        private final long m_version;

        private final String m_title;

        private final String m_description;

        private final String m_author;

        private final Instant m_createdOn;

        /**
         * Constructor.
         *
         * @param version
         * @param title
         * @param description
         * @param author
         * @param createdOn
         */
        public RepositoryItemVersion(final long version, final String title, final String description, final String author,
            final Instant createdOn) {

            m_version = version;
            m_title = title;
            m_description = description;
            m_author = author;
            m_createdOn = createdOn;
        }

        /**
         * @return the numeric version
         */
        public long getVersion() {
            return m_version;
        }

        /**
         * @return the title
         */
        public String getTitle() {
            return m_title;
        }

        /**
         * @return the description
         */
        public String getDescription() {
            return m_description;
        }

        /**
         * @return the author account name
         */
        public String getAuthor() {
            return m_author;
        }

        /**
         * @return the createdOn
         */
        public Instant getCreatedOn() {
            return m_createdOn;
        }
    }

    /**
     * Lists all versions of the given Hub repository item.
     *
     * @param id The repository item id.
     * @return a {@link List} of {@link RepositoryItemVersion}s for the given repository item.
     * @throws IOException
     */
    List<RepositoryItemVersion> getRepositoryItemVersions(String id) throws IOException;
}
