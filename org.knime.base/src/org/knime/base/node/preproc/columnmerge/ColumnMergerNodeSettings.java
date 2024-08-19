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
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.RadioButtonsWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;

/**
 *
 * @author Jonas Klotz, KNIME GmbH, Berlin, Germany
 * @since 5.0
 */
@SuppressWarnings("restriction")
public final class ColumnMergerNodeSettings implements DefaultNodeSettings {

    @Persist(configKey = ColumnMergerConfiguration.CFG_PRIMARY, settingsModel = SettingsModelString.class)
    @Widget(title = "Primary column",
        description = "The column with the value that will be used, unless it is missing.")
    @ChoicesWidget(choices = AllColumns.class)
    String m_primaryColumn = "";

    @Persist(configKey = ColumnMergerConfiguration.CFG_SECONDARY, settingsModel = SettingsModelString.class)
    @Widget(title = "Secondary column", description = "The column with the value that will be used if it is missing "//
        + "in the primary column.")
    @ChoicesWidget(choices = AllColumns.class)
    String m_secondaryColumn = "";

    @Persist(customPersistor = OutputPlacementOptionsPersistor.class)
    @Widget(title = "Replace/append columns", description = "Choose where to put the result column:"//
        + "<ul>"//
        + "<li><b>Replace primary and delete secondary</b>: Replace the primary column with the merge "
        + "result and remove the secondary column.</li>"//
        + "<li><b>Replace primary</b>: Replace the primary column with the merge "
        + "result and keep the secondary column.</li>"//
        + "<li><b>Replace secondary</b>: Keep the primary column and replace the "
        + "secondary column with the merge result.</li>"//
        + "<li><b>Append as new column</b>: Append a new column with the name provided below.</li>"//
        + "</ul>")
    @RadioButtonsWidget
    @ValueReference(OutputPlacement.Ref.class)
    OutputPlacement m_outputPlacement;

    @Persist(configKey = ColumnMergerConfiguration.CFG_OUTPUT_NAME, settingsModel = SettingsModelString.class)
    @Widget(title = "New column name", description = "The name for the new column.")
    @Effect(predicate = OutputPlacement.IsAppendAsNewColumn.class, type = EffectType.SHOW)
    String m_outputName;

    private static final class AllColumns implements ChoicesProvider {
        @Override
        public String[] choices(final DefaultNodeSettingsContext context) {
            var spec = context.getDataTableSpecs()[0];
            return spec == null ? new String[0] : spec.getColumnNames();
        }

    }

    private static final class OutputPlacementOptionsPersistor implements FieldNodeSettingsPersistor<OutputPlacement> {

        @Override
        public OutputPlacement load(final NodeSettingsRO settings) throws InvalidSettingsException {
            String outputPlacement = settings.getString(ColumnMergerConfiguration.CFG_OUTPUT_PLACEMENT);
            try {
                return OutputPlacement.valueOf(outputPlacement);
            } catch (IllegalArgumentException ex) {
                throw new InvalidSettingsException(
                    "Unrecognized option \"" + outputPlacement + "\" for output placement selection.", ex);
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

        @Override
        public String[] getConfigKeys() {
            return new String[]{ColumnMergerConfiguration.CFG_OUTPUT_PLACEMENT};
        }
    }
}
