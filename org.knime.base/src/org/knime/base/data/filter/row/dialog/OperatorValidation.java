/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog;

import static org.knime.base.data.filter.row.dialog.OperandValidations.canParseBoolean;
import static org.knime.base.data.filter.row.dialog.OperandValidations.canParseDouble;
import static org.knime.base.data.filter.row.dialog.OperandValidations.canParseInteger;
import static org.knime.base.data.filter.row.dialog.OperandValidations.canParseLong;
import static org.knime.base.data.filter.row.dialog.OperandValidations.notBlankValue;

import java.util.function.Consumer;
import java.util.function.Function;

import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;

/**
 * Operation validation functional interface.
 *
 * @author Viktor Buria
 * @since 4.0
 */
@FunctionalInterface
public interface OperatorValidation extends Function<OperatorParameters, ValidationResult> {

    /**
     * Validates the given value and reports error if any.
     *
     * @param value a value for the validation
     * @param type the {@link DataType} of the given value
     * @param errorConsumer the {@link Consumer} for error messages
     */
    default void checkValue(final String value, final DataType type, final Consumer<String> errorConsumer) {

        notBlankValue(value).ifPresent(errorConsumer);

        if (BooleanCell.TYPE.equals(type)) {
            canParseBoolean(value).ifPresent(errorConsumer);
        }

        if (IntCell.TYPE.equals(type)) {
            canParseInteger(value).ifPresent(errorConsumer);
        }

        if (LongCell.TYPE.equals(type)) {
            canParseLong(value).ifPresent(errorConsumer);
        }

        if (DoubleCell.TYPE.equals(type)) {
            canParseDouble(value).ifPresent(errorConsumer);
        }
    }

}
