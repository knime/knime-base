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
 *   May 6, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.util.valueaccess;

import org.knime.base.node.meta.explain.util.Caster;
import org.knime.core.data.DataCell;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;

/**
 * Static factory class for {@link ValueAccessor ValueAccessors}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class ValueAccessors {

    private static final Caster<DoubleValue> DOUBLE_CASTER = new Caster<>(DoubleValue.class, false);

    private static final Caster<NominalValue> NOMINAL_CASTER = new Caster<>(NominalValue.class, false);

    private ValueAccessors() {
        // static utility class
    }

    /**
     * @return {@link NumericValueAccessor} for {@link DataCell cells} implementing {@link DoubleValue}
     */
    public static NumericValueAccessor getDoubleValueAccessor() {
        return ValueAccessors::getDoubleValue;
    }

    /**
     * @return {@link NominalValueAccessor} for {@link DataCell cells} implementing {@link NominalValue}
     */
    public static NominalValueAccessor getNominalValueAccessor() {
        return ValueAccessors::getNominalValue;
    }

    /**
     * @param idx the index at which the accessor should extract the value from a {@link BitVectorValue}
     * @return {@link NumericValueAccessor} that extracts the value at position <b>idx</b> from a {@link BitVectorValue}
     */
    public static NumericValueAccessor getBitVectorValueAccessor(final int idx) {
        return new BitVectorValueAccessor(idx);
    }

    /**
     * @param idx the index at which the accessor should extract the value from a {@link ByteVectorValue}
     * @return {@link NumericValueAccessor} that extracts the value at position <b>idx</b> from a {@link ByteVectorValue}
     */
    public static NumericValueAccessor getByteVectorValueAccessor(final int idx) {
        return new ByteVectorValueAccessor(idx);
    }

    private static double getDoubleValue(final DataCell cell) {
        return DOUBLE_CASTER.getAsT(cell).getDoubleValue();
    }

    private static DataCell getNominalValue(final DataCell cell) {
        // only check if cell is actually a nominal value
        NOMINAL_CASTER.getAsT(cell);
        return cell;
    }
}
