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
 *   Jan 27, 2023 (Jonas Klotz, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.columnmerge;

import org.knime.base.node.preproc.columnmerge.ColumnMergerConfiguration.OutputPlacement;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.webui.node.dialog.impl.ChoicesProvider;
import org.knime.core.webui.node.dialog.impl.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.impl.Schema;
import org.knime.core.webui.node.dialog.persistence.NodeSettingsPersistor;
import org.knime.core.webui.node.dialog.persistence.field.Persist;

/**
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 * @since 5.0
 */
@SuppressWarnings("restriction")
public final class ColumnMergerNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = ColumnMergerConfiguration.CFG_PRIMARY, settingsModel = SettingsModelString.class)
    @Schema(title = "Primary column",
        description = " The column with the value that will be used unless it is missing.", choices = AllColumns.class)
    String m_primaryColumn = "";

    @Persist(configKey = ColumnMergerConfiguration.CFG_SECONDARY, settingsModel = SettingsModelString.class)
    @Schema(title = "Second column", description = "The column with the value that will be used otherwise.",
        choices = AllColumns.class)
    String m_secondaryColumn = "";

    @Persist(customPersistor = OutputPlacementOptionsPersistor.class)
    @Schema(title = "Output placement",
        description = "Choose where to put the result column. You can replace either of the input columns, both input columns "
            + "(the output column will replace the primary column) or append a new column with a given name." + "<ul>\n"
            + "  <li>Replace primary - replaces the first column</li>\n"
            + "  <li>Replace secondary - replaces the second column</li>\n"
            + "  <li>Replace both - replaces both columns and saves the new column with the name of the first column</li>\n"
            + "  <li>Append as new column - append a new column. You can set the name below.</li>\n" + "</ul> ")
    OutputPlacement m_outputPlacement;

    @Persist(configKey = ColumnMergerConfiguration.CFG_OUTPUT_NAME, settingsModel = SettingsModelString.class)
    @Schema(title = "New column name", description = "The name for the new column.")
    String m_outputName;

    private static final class AllColumns implements ChoicesProvider {
        @Override
        public String[] choices(final SettingsCreationContext context) {
            var spec = context.getDataTableSpecs()[0];
            return spec == null ? new String[0] : spec.getColumnNames();
        }

    }

    private static final class OutputPlacementOptionsPersistor
        implements NodeSettingsPersistor<OutputPlacement> {

        @Override
        public OutputPlacement load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String outputPlacement = settings.getString(ColumnMergerConfiguration.CFG_OUTPUT_PLACEMENT);
            try {
                return OutputPlacement.valueOf(outputPlacement);
            } catch (IllegalArgumentException ex) {
                throw new InvalidSettingsException("Unknown output placement: " + outputPlacement, ex);
            }
        }

        @Override
        public void save(final OutputPlacement obj, final NodeSettingsWO settings) {
            if (obj == null) {
                settings.addString(ColumnMergerConfiguration.CFG_OUTPUT_PLACEMENT, OutputPlacement.ReplaceBoth.name());
                return;
            }
            settings.addString(ColumnMergerConfiguration.CFG_OUTPUT_PLACEMENT, obj.name());
        }

    }
}
