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

import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.persistence.field.Persist;

/**
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
public final class LagColumnNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = LagColumnConfiguration.CFG_COLUMN, settingsModel = SettingsModelString.class)
    @Schema(title = "Column lag", description = "The column to be lagged.", choices = AllColumns.class)
    String m_column;

    @Persist(configKey = LagColumnConfiguration.CFG_LAG_INTERVAL, settingsModel = SettingsModelInteger.class)
    @Schema(title = "Lag interval",
        description = "<i>I</i> = lag interval (sometimes also called periodicity or seasonality) \n"
            + "            defines how many column copies and how many row shifts to apply.")
    int m_lagInterval;

    @Persist(configKey = LagColumnConfiguration.CFG_LAG, settingsModel = SettingsModelInteger.class)
    @Schema(title = "Lag",
        description = " <i>L</i> = lag defines how many column copies and how many row shifts to apply.")
    int m_lag;

    @Persist(configKey = LagColumnConfiguration.CFG_SKIP_INITIAL_COMPLETE_ROWS,
        settingsModel = SettingsModelBoolean.class)
    @Schema(title = "Skip initial incomplete rows",
        description = "If selected the first rows from the input table are omitted in the output so that the lag output column(s)\n"
            + "            is not missing (unless the reference data is missing).")
    boolean m_skipInitialIncompleteRows = false;

    @Persist(configKey = LagColumnConfiguration.CFG_SKIP_LAST_COMPLETE_ROWS, settingsModel = SettingsModelBoolean.class)
    @Schema(title = "Skip last incomplete rows",
        description = "If selected the rows containing the lagged values of the last real data row are \n"
            + "            omitted (no artificial new rows). Otherwise new rows are added, which contain missing values in all columns\n"
            + "            but the new lag output.")
    boolean m_skipLastIncompleteRows = true;

    private static final class AllColumns implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            var spec = context.getDataTableSpecs()[0];
            return spec == null ? new String[0] : spec.getColumnNames();
        }

    }
}
