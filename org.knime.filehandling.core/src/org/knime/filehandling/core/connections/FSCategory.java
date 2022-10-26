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
 *   Jun 17, 2020 (bjoern): created
 */
package org.knime.filehandling.core.connections;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * Enum to model file system categories.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 * @noreference non-public API
 */
public enum FSCategory {

        /** Category for local "convenience" file system(s) */
        LOCAL("Local File System"),

        /** Category for relative "convenience" file systems */
        RELATIVE("Relative to"),

        /** Category for "convenience" file systems that access mountpoints */
        MOUNTPOINT("Mountpoint"),

        /** Category for "convenience" file systems that access Hub Spaces */
        HUB_SPACE("Hub Space"),

        /** Category for "convenience" file system(s) that acces only URLs */
        CUSTOM_URL("Custom/KNIME URL"),

        /** Category for file systems that need to be connected via input port. */
        CONNECTED("");

    private static final Set<FSCategory> STANDARD_CATEGORIES =
        Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(CONNECTED)));

    private static final Set<FSCategory> STANDARD_NONTRIVIAL_CATEGORIES =
        Collections.unmodifiableSet(EnumSet.complementOf(EnumSet.of(CONNECTED, CUSTOM_URL)));

    private final String m_label;

    private FSCategory(final String label) {
        m_label = label;
    }

    /**
     * @return a human-readable label for the file system category, to be used for display purposes.
     */
    public String getLabel() {
        return m_label;
    }

    /**
     * @return the set of all standard (aka convenience aka unconnected) {@link FSCategory} values.
     */
    public static Set<FSCategory> getStandardFSCategories() {
        return STANDARD_CATEGORIES;
    }

    /**
     * Some file systems (such as the one behind the {@value #CUSTOM_URL} category, are incapable of "non-trivial"
     * operations, like browsing or listing files. However, some nodes require these capabilities, hence they can only
     * be used with file system that support these.
     *
     * @return the set of all standard (aka convenience aka unconnected) {@link FSCategory} values, where the file systems support more
     *         browsing, listing, etc.
     */
    public static Set<FSCategory> getStandardNonTrivialFSCategories() {
        return STANDARD_NONTRIVIAL_CATEGORIES;
    }
}
