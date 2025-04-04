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
 *   Jan 20, 2023 (jonasklotz): created
 */
package org.knime.base.node.preproc.double2int2;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.IntValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.util.column.ColumnSelectionUtil;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.FilteredInputTableColumnsProvider;

/**
 * Settings for the Web UI dialog of the Double to Int node. Double check backwards compatible loading if this class is
 * ever used in the NodeModel.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin
 */
@SuppressWarnings("restriction")
public final class DoubleToIntNodeSettings implements DefaultNodeSettings {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    DoubleToIntNodeSettings() {

    }

    /**
     * Constructor for settings initialization.
     *
     * @param context of settings creation
     */
    DoubleToIntNodeSettings(final DefaultNodeSettingsContext context) {
        this();
        final var numericColumns =
            ColumnSelectionUtil.getFilteredColumns(context, 0, NumericalColumns::isNumericColumn);
        m_inclCols = new ColumnFilter(numericColumns).withIncludeUnknownColumns();
    }

    @Section(title = "Column Selection")
    @Before(RoundingOptionsSection.class)
    interface ColumnSelectionSection {
    }

    @Persistor(InclColsPersistor.class)
    @Widget(title = "Column Selection", description = "Move the columns of interest into the &quot;Includes&quot; list")
    @ChoicesProvider(NumericalColumns.class)
    @Layout(ColumnSelectionSection.class)
    ColumnFilter m_inclCols = new ColumnFilter();

    static final class InclColsPersistor extends LegacyColumnFilterPersistor {

        InclColsPersistor() {
            super(DoubleToIntNodeModel.CFG_INCLUDED_COLUMNS);
        }

    }

    @Section(title = "Rounding Options")
    @After(ColumnSelectionSection.class)
    interface RoundingOptionsSection {
    }

    @Persistor(RoundingOptionsPersistor.class)
    @Widget(title = "Rounding type",
        description = "The type of rounding applied to the selected double cells. "
            + "(Round: standard rounding, Floor: next smaller integer, Ceil: next bigger integer")
    @Layout(RoundingOptionsSection.class)
    @ValueSwitchWidget
    RoundingOptions m_calctype = RoundingOptions.ROUND;

    enum RoundingOptions {

            @Label("Round")
            ROUND,

            @Label("Floor")
            FLOOR,

            @Label("Ceil")
            CEIL;

    }

    @Persistor(ProdLongPersistor.class)
    @Widget(title = "Create long values",
        description = "Use this option to generate 64bit long values instead of 32bit integer values. "
            + "This is useful if double values in the input are too big to fit into an integer.")
    @Layout(RoundingOptionsSection.class)
    boolean m_prodLong = false; //NOSONAR being explicit is desired here

    static final class ProdLongPersistor extends SettingsModelBooleanPersistor {

        ProdLongPersistor() {
            super(DoubleToIntNodeModel.CFG_LONG);
        }
    }

    static final class NumericalColumns implements FilteredInputTableColumnsProvider {

        @Override
        public boolean isIncluded(final DataColumnSpec colSpec) {
            return isNumericColumn(colSpec);
        }

        static boolean isNumericColumn(final DataColumnSpec colSpec) {
            final var type = colSpec.getType();
            return type.isCompatible(DoubleValue.class) && !type.isCompatible(IntValue.class);
        }

    }

    private static final class RoundingOptionsPersistor implements NodeSettingsPersistor<RoundingOptions> {

        @Override
        public RoundingOptions load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var roundingOption = settings.getString(DoubleToIntNodeModel.CFG_TYPE_OF_ROUND);
            return RoundingOptions.valueOf(roundingOption.toUpperCase());
        }

        @Override
        public void save(final RoundingOptions obj, final NodeSettingsWO settings) {
            settings.addString(DoubleToIntNodeModel.CFG_TYPE_OF_ROUND, obj.name().toLowerCase());

        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{DoubleToIntNodeModel.CFG_TYPE_OF_ROUND}};
        }
    }

}
