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
 *   Jan 26, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.columnlag;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.Persistor;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class LagColumnNodeSettings implements DefaultNodeSettings {

    /**
     * Constructor called by the framework to get default settings.
     *
     * @param context of the settings creation
     */
    LagColumnNodeSettings(final DefaultNodeSettingsContext context) {
        m_column = context.getDataTableSpec(0)//
            .map(LagColumnConfiguration::findDefaultColumn)//
            .orElse(ROW_KEYS);
    }

    /**
     * Constructor called by the framework for persistence and JSON conversion.
     */
    LagColumnNodeSettings() {

    }

    private static final String ROW_KEYS = "<row-keys>";


    @Persistor(ColumnNodeSettingsPersistor.class)
    @Widget(title = "Column to lag", description = "The column to be lagged.")
    @ChoicesWidget(choices = AllColumns.class, showRowKeysColumn = true)
    String m_column = ROW_KEYS;

    @Persist(configKey = LagColumnConfiguration.CFG_LAG)
    @Widget(title = "Number of copies", description = " <i>L</i> = defines how many lagged column copies to create.")
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    int m_lag = 1;

    @Persist(configKey = LagColumnConfiguration.CFG_LAG_INTERVAL)
    @Widget(title = "Lag per copy",
        description = "<i>I</i> = lag interval (sometimes also called periodicity or seasonality), defines "
            + "how many rows to shift per column copy.")
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    int m_lagInterval = 1;

    @Persist(configKey = LagColumnConfiguration.CFG_SKIP_INITIAL_COMPLETE_ROWS)
    @Widget(title = "Drop incomplete rows at the top of the table",
        description = "If selected, the first rows from the input table are omitted in the output so that the lag "
            + "output column(s) is not missing (unless the reference data is missing).")
    boolean m_skipInitialIncompleteRows;

    @Persist(configKey = LagColumnConfiguration.CFG_SKIP_LAST_COMPLETE_ROWS)
    @Widget(title = "Drop incomplete rows at the bottom of the table",
        description = "If selected the rows containing the lagged values of the last real data row are "
            + "omitted (no artificial new rows). Otherwise new rows are added, "
            + "which contain missing values in all columns but the new lag output.")
    boolean m_skipLastIncompleteRows = true;

    private static final class ColumnNodeSettingsPersistor implements NodeSettingsPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var column = settings.getString(LagColumnConfiguration.CFG_COLUMN);
            return column == null ? ROW_KEYS : column;
        }

        @Override
        public void save(final String obj, final NodeSettingsWO settings) {
            // LagColumnConfiguration uses null to indicate that the RowID is used
            settings.addString(LagColumnConfiguration.CFG_COLUMN, ROW_KEYS.equals(obj) ? null : obj);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{LagColumnConfiguration.CFG_COLUMN}};
        }
    }

    private static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final DefaultNodeSettingsContext context) {
            return context.getDataTableSpec(0)//
                .stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);
        }

    }
}
