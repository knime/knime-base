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
 *   Jul 24, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row2.operator;

import java.util.Objects;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.panel.PatternMatchPanel;
import org.knime.base.util.WildcardMatcher;
import org.knime.core.data.DataCell;
import org.knime.core.node.util.CheckUtils;

/**
 * {@link KnimeOperatorFunction} for the {@link PatternMatchPanel}.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
enum KnimePatternMatchOperatorFunction implements KnimeOperatorFunction {
    INSTANCE;

    @Override
    public RowPredicateFactory apply(final OperatorParameters parameters) {
        Objects.requireNonNull(parameters, "parameters");
        return new DefaultRowPredicateFactory(createCellPredicate(parameters), parameters.getColumnSpec());
    }

    private static Predicate<DataCell> createCellPredicate(final OperatorParameters parameters) {
        final String expression = parameters.getValues()[0];
        final boolean isCaseSensitive = Boolean.parseBoolean(parameters.getValues()[1]);
        final boolean isWildcard = Boolean.parseBoolean(parameters.getValues()[2]);
        final boolean isRegularExpression = Boolean.parseBoolean(parameters.getValues()[3]);
        if (expression == null) {
            throw new NullPointerException("Pattern to match cannot be null.");
        }
        CheckUtils.checkArgument(!(isWildcard && isRegularExpression),
            "Wildcard and Regular Expression cannot be true at the same time.");
        if (isWildcard || isRegularExpression) {
            final String matcherExpression = isWildcard ? WildcardMatcher.wildcardToRegex(expression) : expression;
            return createMatcherPredicate(matcherExpression, isCaseSensitive);
        } else {
            return createEqualityPredicate(expression, isCaseSensitive);
        }
    }

    private static Predicate<DataCell> createMatcherPredicate(final String matcherExpression,
        final boolean isCaseSensitive) {
        final int flags = getFlags(isCaseSensitive);
        final Pattern regExpression = Pattern.compile(matcherExpression, flags);
        return c -> regExpression.matcher(c.toString()).matches();
    }

    private static int getFlags(final boolean isCaseSensitive) {
        int flags = Pattern.DOTALL | Pattern.MULTILINE;
        if (!isCaseSensitive) {
            flags |= Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
        }
        return flags;
    }

    private static Predicate<DataCell> createEqualityPredicate(final String expression, final boolean caseSensitive) {
        if (caseSensitive) {
            return c -> expression.equals(c.toString());
        } else {
            return c -> expression.equalsIgnoreCase(c.toString());
        }
    }

}
