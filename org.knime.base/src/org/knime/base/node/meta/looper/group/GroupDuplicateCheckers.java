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
 *   Aug 18, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.looper.group;

import java.io.IOException;

import org.knime.core.util.DuplicateChecker;
import org.knime.core.util.DuplicateKeyException;

/**
 * Utility class for dealing with duplicate checking.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class GroupDuplicateCheckers {

    static GroupDuplicateChecker createGroupDuplicateChecker(final boolean checkForDuplicates) {
        if (checkForDuplicates) {
            return new RealGroupDuplicateChecker();
        } else {
            return NoopGroupDuplicateChecker.INSTANCE;
        }
    }

    interface GroupDuplicateChecker extends AutoCloseable {

        @Override
        void close();

        void addGroup(final String groupIdentifier) throws DuplicateKeyException, IOException;

        void checkForDuplicates() throws IOException;
    }

    private static final class RealGroupDuplicateChecker implements GroupDuplicateChecker {

        private final DuplicateChecker m_duplicateChecker;

        RealGroupDuplicateChecker() {
            m_duplicateChecker = new DuplicateChecker();
        }

        @Override
        public void close() {
            m_duplicateChecker.clear();
        }

        @Override
        public void addGroup(final String groupIdentifier) throws DuplicateKeyException, IOException {
            try {
                m_duplicateChecker.addKey(groupIdentifier);
            } catch (DuplicateKeyException e) {
                var toThrow = new DuplicateKeyException(
                    "Input table was not sorted, found duplicate (group identifier:" + groupIdentifier + ")");
                toThrow.initCause(e);
                throw toThrow;
            }
        }

        @Override
        public void checkForDuplicates() throws IOException {
            try {
                m_duplicateChecker.checkForDuplicates();
            } catch (DuplicateKeyException e) {
                var toThrow = new DuplicateKeyException(
                    "Input table was not sorted, found duplicate group identifier " + e.getKey());
                toThrow.initCause(e);
                throw toThrow;
            }
        }

    }

    private enum NoopGroupDuplicateChecker implements GroupDuplicateChecker {
            INSTANCE;

        @Override
        public void close() {
            // noop
        }

        @Override
        public void addGroup(final String groupIdentifier) {
            // noop
        }

        @Override
        public void checkForDuplicates() {
            // noop
        }

    }
}
