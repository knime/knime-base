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
 *   Mar 2, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.knime.core.node.util.CheckUtils;

/**
 * Keeps track of the names of the different transformations and allows to detect duplicates.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NameChecker {

    private final HashMap<String, AtomicInteger> m_nameCounts = new HashMap<>();

    void clear() {
        m_nameCounts.clear();
    }

    void add(final String name) {
        getCount(name).incrementAndGet();
    }

    private AtomicInteger getCount(final String name) {
        return m_nameCounts.computeIfAbsent(name, n -> new AtomicInteger());
    }

    Affected rename(final String oldName, final String newName) {
        CheckUtils.checkArgument(isTaken(oldName), "Can't rename an unknown value '%s'.", oldName);
        int newCountOfOldName = getCount(oldName).decrementAndGet();
        final Affected affectedByRemove = newCountOfOldName == 1 ? Affected.ALL : Affected.THIS;
        if (newCountOfOldName == 0) {
            m_nameCounts.remove(oldName);
        }
        final Affected affectedByAddition = getCount(newName).incrementAndGet() > 1 ? Affected.ALL : Affected.THIS;
        return affectedByRemove.and(affectedByAddition);
    }

    boolean isDuplicated(final String name) {
        return isTaken(name) && getCount(name).get() > 1;
    }

    boolean isTaken(final String name) {
        return m_nameCounts.containsKey(name);
    }

}
