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
 *   Nov 8, 2022 (ivan.prigarin): created
 */
package org.knime.base.node.preproc.table.cellupdater;

import java.util.Map;
import java.util.Optional;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.rule.OneOfEnumCondition;
import org.knime.core.webui.node.dialog.defaultdialog.rule.Signal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ChoicesWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ColumnChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;

/**
 * Settings of the Cell Updater node.
 *
 * @author Ivan Prigarin, KNIME GmbH, Konstany, Germany
 */
@SuppressWarnings("restriction")
public final class CellUpdaterSettings implements DefaultNodeSettings {

    /**
     * Constructor for auto-configure.
     *
     * @param context the creation context
     */
    CellUpdaterSettings(final SettingsCreationContext context) {
        var portObjects = context.getPortObjectSpecs();

        // only perform autoconfigure when both ports are connected
        if ((portObjects[0] != null) && (portObjects[1] != null)) {
            var spec = (DataTableSpec)portObjects[1];
            var vars = context.getAvailableInputFlowVariables(VariableToCellConverterFactory.getSupportedTypes());
            autoconfigureSettings(vars, spec);
        }
    }

    /**
     * Constructor for deserialization.
     */
    CellUpdaterSettings() {

    }

    @Widget(title = "Column specification", description = "Select whether to specify the column by name or by number.")
    @ValueSwitchWidget
    @Signal(condition = ColumnMode.IsByName.class)
    ColumnMode m_columnMode = ColumnMode.BY_NAME;

    // TODO: UIEXT-1007 migrate String to ColumnSelection

    @Widget(title = "Column name", description = "Select the column that contains the target cell.")
    @ChoicesWidget(choices = AllColumns.class)
    @Effect(signals = ColumnMode.IsByName.class, type = EffectType.SHOW)
    String m_columnName;

    @Widget(title = "Column number", description = "Provide the number of the column that contains the target cell.")
    @NumberInputWidget(min = 1)
    @Effect(signals = ColumnMode.IsByName.class, type = EffectType.HIDE)
    int m_columnNumber = 1;

    @Widget(title = "Row number", description = "Provide the number of the row that contains the target cell.")
    @NumberInputWidget(min = 1)
    int m_rowNumber = 1;

    @Widget(title = "Count rows from the end of the table",
        description = "If selected, the rows will be counted from the end of the table.")
    boolean m_countFromEnd = false;

    @Widget(title = "New cell value", description = "Select the flow variable containing the new cell value.")
    @ChoicesWidget(choices = AllVariables.class)
    String m_flowVariableName;

    private static final class AllColumns implements ColumnChoicesProvider {

        @Override
        public DataColumnSpec[] columnChoices(final SettingsCreationContext context) {
            return context.getDataTableSpec(1).stream()//
                .flatMap(DataTableSpec::stream)//
                .toArray(DataColumnSpec[]::new);
        }

    }

    private static final class AllVariables implements ChoicesProvider {

        @Override
        public String[] choices(final SettingsCreationContext context) {
            return context.getAvailableFlowVariableNames();
        }

    }

    enum ColumnMode {
            @Label("Name")
            BY_NAME,

            @Label("Number")
            BY_NUMBER;

        static class IsByName extends OneOfEnumCondition<ColumnMode> {

            @Override
            public ColumnMode[] oneOf() {
                return new ColumnMode[]{BY_NAME};
            }

        }
    }

    /**
     * When the node gets connected to the input table, this autoconfiguration logic attempts to find the first pair of
     * column/flow variable that have a matching type.
     *
     * Otherwise they are initialised to the first column and flow variable name respectively.
     */
    private void autoconfigureSettings(final Map<String, FlowVariable> availableVars, final DataTableSpec spec) {
        if (!isAutoconfigurable(availableVars, spec)) {
            return;
        }

        final CellUpdater.Match match = Optional.ofNullable(CellUpdater.matchColumnsAndVariables(spec, availableVars))
            .orElse(new CellUpdater.Match(0, CellUpdater.getFirstFlowVariableName(availableVars)));

        m_columnName = spec.getColumnSpec(match.getMatchedColIdx()).getName();
        m_columnNumber = match.getMatchedColIdx() + 1;
        m_rowNumber = 1;
        m_countFromEnd = false;
        m_flowVariableName = match.getMatchedVarName();
        m_columnMode = ColumnMode.BY_NAME;
    }

    /*
     * Check if the current state of the node makes it autoconfigurable.
     *
     * Cases:
     * - availableVars is empty: there are no flow variables available (should never happen).
     * - spec doesn't have any columns: the input table exists but is empty.
     * - columnName is not null: the settings have already been configured.
     */
    private boolean isAutoconfigurable(final Map<String, FlowVariable> availableVars, final DataTableSpec spec) {
        return !(availableVars.isEmpty() || spec.getNumColumns() == 0 || m_columnName != null);
    }

}
