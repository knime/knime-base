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

import org.knime.base.node.preproc.colconvert.stringtonumber2.StringToNumber2NodeSettings.ParsingOptionsSection;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.TextInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.CompatibleColumnsProvider.StringColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.TextInputWidgetValidation.MaxLengthValidation.HasAtMaxOneCharValidation;

/**
 *
 * Settings for the Web UI dialog of the String to Number node. Double check backwards compatible loading if this class
 * is ever used in the NodeModel.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin
 * @since 5.1
 */
@SuppressWarnings("restriction")
@Layout(ParsingOptionsSection.class)
public final class StringToNumber2NodeSettings implements DefaultNodeSettings {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    public StringToNumber2NodeSettings() {
    }

    StringToNumber2NodeSettings(final DefaultNodeSettingsContext context) {
        this();
        m_inclCols =
            new ColumnFilter(ColumnSelectionUtil.getStringColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    @Section(title = "Column Selection")
    interface ColumnSelectionSection {
    }

    @Section(title = "Parsing options")
    @After(ColumnSelectionSection.class)
    interface ParsingOptionsSection {
    }

    /** The decimal separator. */
    @Persistor(DecSepPersistor.class)
    @Widget(title = "Decimal separator",
        description = "Choose a decimal separator, which is used to mark the boundary between the integral and the "
            + " fractional parts of the decimal string.")
    @TextInputWidget(validation = HasAtMaxOneCharValidation.class)
    String m_decimalSep = AbstractStringToNumberNodeModel.DEFAULT_DECIMAL_SEPARATOR;

    /** The thousands separator. */
    @Persistor(ThousSepPersistor.class)
    @Widget(title = "Thousands separator",
        description = "Choose a thousands separator used in the decimal string to group together three digits.")
    @TextInputWidget(validation = HasAtMaxOneCharValidation.class)
    String m_thousandsSep = AbstractStringToNumberNodeModel.DEFAULT_THOUSANDS_SEPARATOR;

    @Persistor(DataTypeOptionssPersistor.class)
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

    private static final class DataTypeOptionssPersistor implements NodeSettingsPersistor<DataTypeOptions> {

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
        public String[][] getConfigPaths() {
            return new String[][]{{AbstractStringToNumberNodeModel.CFG_PARSETYPE}};
        }

    }

    @Persistor(GenericParsePersistor.class)
    @Widget(title = "Accept type suffix, e.g. 'd', 'D', 'f', 'F'",
        description = "When checked, the type suffix will be accepted, "
            + "otherwise it fails to parse input like <tt>1d</tt>. " + "These suffixes are typically used "
            + "in java-style programs to represent floating point numbers ('f' for float and 'd' for double). "
            + "Default is not checked.")
    boolean m_genericParse = AbstractStringToNumberNodeModel.DEFAULT_GENERIC_PARSE;

    @Persistor(FailOnErrorPersistor.class)
    @Widget(title = "Fail on error", description = "When checked, the node will fail if an error occurs.")
    boolean m_failOnError = AbstractStringToNumberNodeModel.DEFAULT_FAIL_ON_ERROR;

    @Persistor(InclColsPersistor.class)
    @Widget(title = "Column selection", description = "Move the columns of interest into the &quot;Includes&quot; list")
    @ChoicesProvider(StringColumnsProvider.class)
    @Layout(ColumnSelectionSection.class)
    ColumnFilter m_inclCols = new ColumnFilter();

    static final class DecSepPersistor extends SettingsModelStringPersistor {
        DecSepPersistor() {
            super(AbstractStringToNumberNodeModel.CFG_DECIMALSEP);
        }
    }

    static final class ThousSepPersistor extends SettingsModelStringPersistor {
        ThousSepPersistor() {
            super(AbstractStringToNumberNodeModel.CFG_THOUSANDSSEP);
        }
    }

    static final class GenericParsePersistor extends SettingsModelBooleanPersistor {
        GenericParsePersistor() {
            super(AbstractStringToNumberNodeModel.CFG_GENERIC_PARSE);
        }
    }

    static final class FailOnErrorPersistor extends SettingsModelBooleanPersistor {
        FailOnErrorPersistor() {
            super(AbstractStringToNumberNodeModel.CFG_FAIL_ON_ERROR);
        }
    }

    static final class InclColsPersistor extends LegacyColumnFilterPersistor {
        InclColsPersistor() {
            super(AbstractStringToNumberNodeModel.CFG_INCLUDED_COLUMNS);
        }

    }
}
