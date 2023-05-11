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
 *   Jan 26, 2023 (jonasklotz): created
 */
package org.knime.base.node.preproc.colconvert.stringtonumber2;

import java.util.stream.Stream;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.StringValue;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 *
 * Settings for the Web UI dialog of the String to Number node. Double check backwards compatible loading if this class
 * is ever used in the NodeModel.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin
 */
@SuppressWarnings("restriction")
final class StringToNumber2NodeSettings implements DefaultNodeSettings {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public StringToNumber2NodeSettings() {
    }

    StringToNumber2NodeSettings(final SettingsCreationContext context) {
        this();
        m_inclCols = ColumnFilter.createDefault(StringColumns.class, context);
    }

    /** The decimal separator. */
    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_DECIMALSEP, settingsModel = SettingsModelString.class)
    @Widget(title = "Decimal separator",
        description = "Choose a decimal separator, which is used to mark the boundary between the integral and the "
            + " fractional parts of the decimal string.")
    String m_decimalSep = AbstractStringToNumberNodeModel.DEFAULT_DECIMAL_SEPARATOR;

    /** The thousands separator. */
    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_THOUSANDSSEP, settingsModel = SettingsModelString.class)
    @Widget(title = "Thousands separator",
        description = "Choose a thousands separator used in the decimal string to group together three digits.")
    String m_thousandsSep = AbstractStringToNumberNodeModel.DEFAULT_THOUSANDS_SEPARATOR;

    @Persist(customPersistor = DataTypeOptionssPersistor.class)
    @Widget(title = "Type", description = "Choose the DataType that your string should be converted to.")
    DataTypeOptions m_parseType = DataTypeOptions.DOUBLE;

    enum DataTypeOptions {
            @Label("Number (Integer)")
            INT,

            @Label("Number (Double)")
            DOUBLE,

            @Label("Number (Long)")
            LONG;
    }

    private static final class DataTypeOptionssPersistor implements FieldNodeSettingsPersistor<DataTypeOptions> {

        @Override
        public DataTypeOptions load(final NodeSettingsRO settings) throws InvalidSettingsException {

            DataType dtype = settings.getDataType(AbstractStringToNumberNodeModel.CFG_PARSETYPE,
                AbstractStringToNumberNodeModel.POSSIBLETYPES[0]);

            if (dtype.equals(IntCell.TYPE)) {
                return DataTypeOptions.INT;
            } else if (dtype.equals(DoubleCell.TYPE)) {
                return DataTypeOptions.DOUBLE;
            } else {
                return DataTypeOptions.LONG;
            }
        }

        @Override
        public void save(final DataTypeOptions obj, final NodeSettingsWO settings) {
            DataType dtype;
            if (obj == DataTypeOptions.INT) {
                dtype = IntCell.TYPE;

            } else if (obj == DataTypeOptions.DOUBLE) {
                dtype = DoubleCell.TYPE;

            } else {
                dtype = LongCell.TYPE;

            }

            settings.addDataType(AbstractStringToNumberNodeModel.CFG_PARSETYPE, dtype);
        }

        @Override
        public String[] getConfigKeys() {
            return new String[]{AbstractStringToNumberNodeModel.CFG_PARSETYPE};
        }

    }

    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_GENERIC_PARSE, settingsModel = SettingsModelBoolean.class)
    @Widget(title = "Accept type suffix, e.g. 'd', 'D', 'f', 'F'",
        description = "When checked, the type suffix will be accepted, "
            + "otherwise it fails to parse input like <tt>1d</tt>. " + "These suffixes are typically used "
            + "in java-style programs to represent floating point numbers ('f' for float and 'd' for double). "
            + "Default is not checked.")
    boolean m_genericParse = AbstractStringToNumberNodeModel.DEFAULT_GENERIC_PARSE;

    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_FAIL_ON_ERROR, settingsModel = SettingsModelBoolean.class)
    @Widget(title = "Fail on error", description = "When checked, the node will fail if an error occurs.")
    boolean m_failOnError = AbstractStringToNumberNodeModel.DEFAULT_FAIL_ON_ERROR;

    @Persist(configKey = AbstractStringToNumberNodeModel.CFG_INCLUDED_COLUMNS,
        settingsModel = SettingsModelColumnFilter2.class)
    @Widget(title = "Column selection", description = "Move the columns of interest into the &quot;Includes&quot; list")
    @ChoicesWidget(choices = StringColumns.class)
    ColumnFilter m_inclCols = new ColumnFilter();

    static final class StringColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            return context.getDataTableSpec(0)//
                .map(DataTableSpec::stream)//
                .orElseGet(Stream::empty)//
                .filter(c -> c.getType().isCompatible(StringValue.class))//
                .map(DataColumnSpec::getName)//
                .toArray(String[]::new);
        }

    }
}
