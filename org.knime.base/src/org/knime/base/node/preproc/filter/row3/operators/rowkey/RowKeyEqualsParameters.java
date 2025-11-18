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
 *  propagation of KNIME.
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
 *   Sep 25, 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.rowkey;

import java.util.function.Predicate;

import org.knime.base.node.preproc.filter.row3.operators.defaults.CaseSensitivity;
import org.knime.base.node.preproc.filter.row3.operators.legacy.StringPredicate;
import org.knime.core.data.DataType;
import org.knime.core.data.RowKeyValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters.SingleCellValueParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Parameters for row key equality comparisons.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction") // webui
public final class RowKeyEqualsParameters implements SingleCellValueParameters<StringCell> {

    @ValueSwitchWidget
    @Widget(title = "Case matching", description = "Whether the comparison should be case sensitive or not.")
    CaseSensitivity m_caseSensitivity = CaseSensitivity.CASE_SENSITIVE;

    @Widget(title = "RowID", description = "The RowID to compare with.")
    String m_value = "";

    RowKeyEqualsParameters() {
        super();
        // for instantiation by framework
    }

    /**
     * Constructs the parameters with the given string value.
     *
     * @param value the string value
     */
    public RowKeyEqualsParameters(final String value) {
        m_value = value;
    }

    /**
     * Gets the parameters as a predicate on row keys.
     * @return the predicate to test row keys for equality
     */
    Predicate<RowKeyValue> toPredicate() {
        final var pred = StringPredicate.equality(m_value, m_caseSensitivity == CaseSensitivity.CASE_SENSITIVE);
        return key -> pred.test(key.getString());
    }

    /**
     * Creates a new cell with the current value.
     *
     * @return the new cell
     */
    @Override
    public StringCell createCell() {
        return new StringCell(m_value);
    }

    @Override
    public void loadFrom(final StringCell valueFromStash) {
        m_value = valueFromStash.getStringValue();
    }

    @Override
    public DataType getSpecificType() {
        return StringCell.TYPE;
    }
}
