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
 *   Sep 23, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.filter.row3.operators.pattern;

import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.base.node.preproc.filter.row3.operators.legacy.StringPredicate;
import org.knime.core.data.BooleanValue;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.IntValue;
import org.knime.core.data.LongValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValidationUtil;

/**
 * Utility class for creating pattern filter predicates that test whether a string representation of a value matches a
 * pattern (regex or wildcard).
 *
 * @author Paul Bärnreuther
 */
@SuppressWarnings("restriction") // webui
public final class PatternFilterUtils {

    private PatternFilterUtils() {
        // Utility
    }

    /**
     * For types for which {@link #isSupported(DataType)} returns true, returns a function that maps DataValues of the
     * given type to their string representation for pattern matching.
     *
     * @param runtimeColumnSpec the column spec for which to create the function
     * @return a function mapping DataValues to strings
     * @throws InvalidSettingsException if the data type is not supported
     */
    static Function<DataValue, String> toStringFunction(final DataColumnSpec runtimeColumnSpec)
        throws InvalidSettingsException {
        throwIfNotSupported(runtimeColumnSpec);
        final var columnDataType = runtimeColumnSpec.getType();
        // IntCell implements LongValue, so we have to check the smaller type first
        if (columnDataType.isCompatible(IntValue.class)) {
            return value -> Integer.toString(((IntValue)value).getIntValue());
        } else if (columnDataType.isCompatible(LongValue.class)) {
            return value -> Long.toString(((LongValue)value).getLongValue());
        }
        // throwIfNotSupported(..) ensures that only StringValue is left
        return value -> ((StringValue)value).getStringValue();
    }

    /**
     * Checks if the given data type is supported for (default) pattern filtering.
     *
     * @param type the data type to check
     * @return true if the type is supported, false otherwise
     */
    public static boolean isSupported(final DataType type) {
        final var preferredValueClass = type.getPreferredValueClass();
        return !BooleanValue.class.equals(preferredValueClass) // booleans have only IS_TRUE and IS_FALSE operators
            && (type.isCompatible(StringValue.class) //
                || type.isCompatible(IntValue.class) || type.isCompatible(LongValue.class));
    }

    /**
     * Throws an informative {@link InvalidSettingsException} if the given column spec's data type is not supported
     * for pattern filtering.
     *
     * @param runtimeColumnSpec the column spec to check
     * @throws InvalidSettingsException if the column spec's data type is not supported
     */
    static void throwIfNotSupported(final DataColumnSpec runtimeColumnSpec) throws InvalidSettingsException {
        DataType[] supportedTypes = {StringCell.TYPE, IntCell.TYPE, LongCell.TYPE};
        final var columnDataType = runtimeColumnSpec.getType();
        if (!isSupported(columnDataType)) {
            throw FilterValidationUtil.createInvalidSettingsException(builder -> builder
                .withSummary("Pattern matching for column \"%s\" is not supported for type \"%s\"."
                    .formatted(runtimeColumnSpec.getName(), columnDataType.getName()))
                .addResolutions(
                    FilterValidationUtil
                        .appendElements(new StringBuilder("Convert the input column to a compatible type, e.g. "),
                            supportedTypes)
                        .toString(),
                    "Please select a different operator that supports the column's data type \"%s\"."
                        .formatted(columnDataType.getName())));
        }
    }

    /**
     * Throws an informative {@link InvalidSettingsException} if the given column spec's data type is not compatible
     * with the given supported data type.
     *
     * @param <D> data value type
     * @param supportedValueType supported data value type
     * @param configureColumnType the data type at configuration time
     * @param runtimeColumnSpec the column spec to check
     * @throws InvalidSettingsException if the column spec's data type is not compatible
     */
    static <D extends DataValue> void throwIfNotCompatible(final Class<D> supportedValueType,
        final DataType configureColumnType, final DataColumnSpec runtimeColumnSpec) throws InvalidSettingsException {
        if (!runtimeColumnSpec.getType().isCompatible(supportedValueType)) {
            throwNotCompatible(configureColumnType, runtimeColumnSpec);
        }
    }

    /**
     * Throws an informative {@link InvalidSettingsException} indicating that the pattern matching is not compatible
     * with the given column type.
     *
     * @param configureColumnType the data type at configuration time
     * @param runtimeColumnSpec the column spec to check
     * @throws InvalidSettingsException always
     */
    static void throwNotCompatible(final DataType configureColumnType, final DataColumnSpec runtimeColumnSpec)
        throws InvalidSettingsException {
        throw FilterValidationUtil.createInvalidSettingsException(builder -> builder
            .withSummary("Pattern matching for column \"%s\" was configured for type \"%s\" "
                .formatted(runtimeColumnSpec.getName(), configureColumnType.toPrettyString())
                + "but now the column type has changed to \"%s\"."
                    .formatted(runtimeColumnSpec.getType().toPrettyString()))
            .addResolutions("Confirm that the pattern should be applied with the new column type "
                + "by re-applying the node dialog."));
    }

    /**
     * Creates a predicate that tests DataValues against a pattern.
     *
     * @param pattern the pattern to match
     * @param isRegex true if the pattern is a regex, false if it's a wildcard
     * @param isCaseSensitive true if the matching should be case-sensitive
     * @param toStringFunction function mapping the data value to a string for pattern matching
     * @return a predicate that tests DataValues
     *
     * @param <D> the data value type
     */
    public static <D extends DataValue> Predicate<D> createPredicate(final String pattern, final boolean isRegex,
        final boolean isCaseSensitive, final Function<D, String> toStringFunction) {
        final var stringPredicate = StringPredicate.pattern(pattern, isRegex, isCaseSensitive);
        return value -> stringPredicate.test(toStringFunction.apply(value));
    }

}
