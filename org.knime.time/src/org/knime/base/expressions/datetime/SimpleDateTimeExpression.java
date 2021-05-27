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
 */
package org.knime.base.expressions.datetime;

import org.knime.expressions.core.AbstractExpression;

/**
 * Class to easily create new date & time functions.
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
public class SimpleDateTimeExpression extends AbstractExpression implements DateTimeExpression {

    private final String[] m_argDescs;

    private final String m_displayName;

    private final Class<?> m_returnType;

    /**
     * Constructor.
     * @param name function name
     * @param description function description
     * @param returnType function return type
     */
    SimpleDateTimeExpression(final String name, final String description, final Class<?> returnType) {
        this(name, description, returnType, new String[]{});
    }

    /**
     * Constructor.
     * @param name function name
     * @param description function description
     * @param returnType function return type
     * @param argDescs input argument description
     */
    SimpleDateTimeExpression(final String name, final String description, final Class<?> returnType,
        final String[] argDescs) {
        super(name, createDescription(name, description, returnType, argDescs),
            createScript(DateTimeExpressionJavaMethodProvider.ID, name, argDescs.length));
        m_returnType = returnType;
        m_argDescs = argDescs;
        m_displayName = name + "(" + getArgDescs(m_argDescs) + ")";
    }

    private static String createDescription(final String name, final String description, final Class<?> returnType,
        final String... argDescs) {
        final String returnTypeName = returnType.getSimpleName();
        return String.format("%s<br/>Result Type: %s<br/><br/><strong>Example:</strong> %s = %s(%s)", description,
            returnTypeName, returnTypeName, name, concatArgs(argDescs));
    }

    private static String createScript(final String providerID, final String name, final int nrArgs) {
        return String.format("function %s(){\n"//forced line break
            + "return %s.%s(%s);\n"//forced line break
            + "}", //forced line break
            name, providerID, name, createArgs(nrArgs));
    }

    private static String concatArgs(final String[] argDescs) {
        if (argDescs == null || argDescs.length == 0) {
            return "";
        }
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < argDescs.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(argDescs[i]);
        }
        return buf.toString();
    }

    private static String createArgs(final int nrArgs) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < nrArgs; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append("arguments[");
            buf.append(i);
            buf.append("]");
        }
        return buf.toString();
    }

    @Override
    public int getNrArgs() {
        return m_argDescs.length;
    }

    @Override
    public String getDisplayName() {
        return m_displayName;
    }

    private static String getArgDescs(final String[] argDescs) {
        final StringBuilder buf = new StringBuilder();
        for (int i = 0; i < argDescs.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(argDescs[i]);
        }
        return buf.toString();
    }

    @Override
    public Class<?> getReturnType() {
        return m_returnType;
    }

    @Override
    public boolean usesVarArgs() {
        return false;
    }
}