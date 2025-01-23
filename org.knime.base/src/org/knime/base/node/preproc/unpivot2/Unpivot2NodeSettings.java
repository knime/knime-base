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
 *   Feb 1, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.unpivot2;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Layout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.ColumnFilter;
import org.knime.core.webui.node.dialog.defaultdialog.setting.columnfilter.LegacyColumnFilterPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings of the Unpivoting node.
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class Unpivot2NodeSettings implements DefaultNodeSettings {

    @Persistor(ValueColumnsPersistor.class)
    @Widget(title = "Value columns",
        description = "This list contains the columns that are rotated into one single column.")
    @ChoicesWidget(choices = AllColumns.class)
    ColumnFilter m_valueColumns = new ColumnFilter();

    @Persistor(MissingValuesPersistor.class)
    @Widget(title = "Skip rows containing missing cells",
        description = "Skip all rows containing missing cells in the selected value column(s).")
    boolean m_missingValues;

    @Persistor(RetainedColumnsPersistor.class)
    @Widget(title = "Retained columns",
        description = "This list contains the columns "
            + "which are duplicated by the number of selected value columns.")
    @ChoicesWidget(choices = AllColumns.class)
    ColumnFilter m_retainedColumns = new ColumnFilter();

    @Section(title = "Performance", advanced = true)
    interface PerformanceSection {
    }

    @Persistor(EnableHilitePersistor.class)
    @Widget(title = "Enable hiliting", description = "Select if hiliting is enabled between input and output data.")
    @Layout(PerformanceSection.class)
    boolean m_enableHilite;

    private static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);

        }

    }

    static final class ValueColumnsPersistor extends LegacyColumnFilterPersistor {
        ValueColumnsPersistor() {
            super(Unpivot2NodeModel.CFG_VALUE_COLUMNS);
        }
    }

    static final class MissingValuesPersistor extends SettingsModelBooleanPersistor {
        MissingValuesPersistor() {
            super(Unpivot2NodeModel.CFG_MISSING_VALUES);
        }
    }

    static final class RetainedColumnsPersistor extends LegacyColumnFilterPersistor {
        RetainedColumnsPersistor() {
            super(Unpivot2NodeModel.CFG_RETAINED_COLUMNS);
        }
    }

    static final class EnableHilitePersistor extends SettingsModelBooleanPersistor {
        EnableHilitePersistor() {
            super(Unpivot2NodeModel.CFG_HILITING);
        }
    }
}
