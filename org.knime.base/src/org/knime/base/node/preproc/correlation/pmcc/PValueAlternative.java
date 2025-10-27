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
 *   Sep 5, 2019 (benjamin): created
 */
package org.knime.base.node.preproc.correlation.pmcc;

import java.util.Arrays;

import org.knime.node.parameters.widget.choices.Label;

/**
 * @author Benjamin Wilhelm, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public enum PValueAlternative {

        /** two sided p-value */
        @Label(value = "two-sided")
        TWO_SIDED("two-sided"),

        /** Positive association: One-sided (right) */
        @Label(value = "one-sided (right)")
        GREATER("one-sided (right)"),

        /** Negative association: One-sided (left) */
        @Label(value = "one-sided (left)")
        LESS("one-sided (left)");

    private final String m_desc;

    private PValueAlternative(final String desc) {
        m_desc = desc;
    }

    @Override
    public String toString() {
        return m_desc;
    }

    /**
     * @return the names of all possible values (in the same order as {@link #descriptions()})
     */
    public static String[] names() {
        return Arrays.stream(PValueAlternative.values()).map(PValueAlternative::name).toArray(String[]::new);
    }

    /**
     * @return the descriptions of all possible values (in the same order as {@link #names()})
     */
    public static String[] descriptions() {
        return Arrays.stream(PValueAlternative.values()).map(PValueAlternative::toString).toArray(String[]::new);
    }

}
