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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.preproc.pmml.stringtonumber3;

import java.util.List;
import java.util.stream.Collectors;

import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.TypedStringFilterWidgetInternal;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.MaxLengthValidation.HasAtMaxOneCharValidation;

/**
 * Node parameters for String to Number (PMML).
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class StringToNumber3NodeParameters implements NodeParameters {

    @Widget(title = "Column selection", description = "Move the columns of interest into the \"Include\" list.")
    @ChoicesProvider(StringColumnsProvider.class)
    @TypedStringFilterWidgetInternal(hideTypeFilter = true)
    @Persistor(ColumnFilterPersistor.class)
    ColumnFilter m_inclCols = new ColumnFilter();

    @Widget(title = "Type", description = "Choose the DataType that your string should be converted to.")
    @Persistor(DataTypeOptionsPersistor.class)
    DataTypeOptions m_parseType = DataTypeOptions.DOUBLE;

    @Widget(title = "Decimal separator",
        description = "Choose a decimal separator, which is used to mark the boundary between"
            + " the integral and the fractional parts of the decimal string.")
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_DECIMALSEP)
    String m_decimalSep = AbstractStringToNumberNodeModel.DEFAULT_DECIMAL_SEPARATOR;

    @Widget(title = "Thousands separator",
        description = "Choose a thousands separator used in the decimal string to group together three digits.")
    @TextInputWidget(maxLengthValidation = HasAtMaxOneCharValidation.class)
    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_THOUSANDSSEP)
    String m_thousandsSep = AbstractStringToNumberNodeModel.DEFAULT_THOUSANDS_SEPARATOR;

    enum DataTypeOptions {

            @Label("Number (Float)")
            DOUBLE(DoubleCell.TYPE.getName()),

            @Label("Number (Integer)")
            INT(IntCell.TYPE.getName());

        private final String m_value;

        DataTypeOptions(final String value) {
            m_value = value;
        }

        String getValue() {
            return m_value;
        }

        static DataTypeOptions getFromValue(final String value) throws InvalidSettingsException {
            for (final DataTypeOptions condition : values()) {
                if (condition.getValue().equals(value)) {
                    return condition;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final String name) {
            var values =
                List.of(IntCell.TYPE.getName(), DoubleCell.TYPE.getName()).stream().collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", name, values);
        }

    }

    private static final class ColumnFilterPersistor extends LegacyColumnFilterPersistor {
        ColumnFilterPersistor() {
            super(AbstractStringToNumberNodeModel.CFG_INCLUDED_COLUMNS);
        }
    }

    private static final class DataTypeOptionsPersistor implements NodeParametersPersistor<DataTypeOptions> {

        @Override
        public DataTypeOptions load(final NodeSettingsRO settings) throws InvalidSettingsException {
            DataType dtype = settings.getDataType(AbstractStringToNumberNodeModel.CFG_PARSETYPE,
                AbstractStringToNumberNodeModel.POSSIBLETYPES[0]);
            return DataTypeOptions.getFromValue(dtype.getName());
        }

        @Override
        public void save(final DataTypeOptions obj, final NodeSettingsWO settings) {
            DataType dataType = switch (obj) {
                case DOUBLE -> AbstractStringToNumberNodeModel.POSSIBLETYPES[0];
                case INT -> AbstractStringToNumberNodeModel.POSSIBLETYPES[1];
            };
            settings.addDataType(AbstractStringToNumberNodeModel.CFG_PARSETYPE, dataType);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{AbstractStringToNumberNodeModel.CFG_PARSETYPE}};
        }
    }
}
