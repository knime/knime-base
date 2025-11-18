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
package org.knime.base.node.preproc.filter.row3.operators.defaults;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.filter.row3.operators.defaults.TypeMappingUtils.ConverterException;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterValueParameters;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.valuefilter.ValueFilterValidationUtil;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Before;
import org.knime.node.parameters.layout.Layout;

/**
 * Parameters for default filter operators that work with any data type by using string representation.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class SingleStringParameters implements FilterValueParameters.SingleCellValueParameters<StringCell> {

    @Before(ValuePart.class)
    interface BeforeValuePart {
    }

    interface ValuePart {

    }

    @Layout(ValuePart.class)
    @Widget(title = "Value", description = "The value to compare with.")
    String m_value = "";

    SingleStringParameters() {
        // default constructor called from the framework
    }

    SingleStringParameters(final String value) {
        m_value = value;
    }

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

    DataCell createCellAs(final DataType dataType) throws InvalidSettingsException { //
        try {
            return TypeMappingUtils.readDataCellFromString(dataType, m_value);
        } catch (final ConverterException e) {
            final var isEmpty = StringUtils.isEmpty(m_value);
            final var content = isEmpty ? "An empty string" : "The string \"%s\"".formatted(m_value);
            throw ValueFilterValidationUtil.createInvalidSettingsException(builder -> { // NOSONAR
                builder.withSummary(
                    String.format("%s does not represent a valid \"%s\"", content, dataType.toPrettyString()));
                if (e.getMessage() != null && !e.getMessage().isBlank()) {
                    builder.addTextIssue(e.getMessage());
                }
                final var cause = e.getCause();
                if (cause != null && StringUtils.isNotBlank(cause.getMessage())) {
                    builder.addTextIssue(String.format("Caused by: %s", cause.getMessage()));
                }
                return builder;
            }, e);
        }
    }

    @Override
    public DataValue[] stash() {
        return new DataValue[]{new StringCell(m_value)};
    }

    @Override
    public void applyStash(final DataValue[] stashedValues) {
        if (stashedValues.length == 0) {
            return;
        }
        final var first = stashedValues[0];
        if (first == null) {
            return;
        }
        if (first instanceof StringValue str) {
            m_value = str.getStringValue();
        }
        try {
            m_value = TypeMappingUtils.getStringFromDataCell(first.materializeDataCell());
        } catch (TypeMappingUtils.ConverterException e) { // NOSONAR
            // ignore, keep original string
        }

    }
}
