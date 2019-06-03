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
 *   May 29, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util;

import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.container.ColumnRearranger;

/**
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class TableSpecUtil {

    private TableSpecUtil() {
        // static utility class
    }


    /**
     * @param spec1 {@link DataTableSpec}
     * @param spec2 {@link DataTableSpec}
     * @return a {@link DataTableSpec} that contains all columns from <b>spec1</b> that are not contained in <b>spec2</b>
     */
    public static DataTableSpec difference(final DataTableSpec spec1, final DataTableSpec spec2) {
        return keepOnly(spec1, c -> !spec2.containsName(c.getName()));
    }

    /**
     * Removes those columns from {@link DataTableSpec spec} that don't fulfill {@link Predicate predicate}.
     *
     * @param spec the {@link DataTableSpec} to filter
     * @param predicate the predicate all columns contained in the returned {@link DataTableSpec} must fulfill
     * @return the filtered {@link DataTableSpec}
     */
    public static DataTableSpec keepOnly(final DataTableSpec spec, final Predicate<DataColumnSpec> predicate) {
        final ColumnRearranger cr = new ColumnRearranger(spec);
        for (DataColumnSpec colSpec : spec) {
            if (!predicate.test(colSpec)) {
                cr.remove(colSpec.getName());
            }
        }
        return cr.createSpec();
    }
}
