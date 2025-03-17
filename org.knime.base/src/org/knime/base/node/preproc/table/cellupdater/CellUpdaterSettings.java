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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.knime.base.node.flowvariable.converter.variabletocell.VariableToCellConverterFactory;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.dialog.defaultdialog.widget.NumberInputWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.ValueSwitchWidget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.ChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.StringChoicesProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.column.AllColumnsProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Effect.EffectType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Predicate;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.PredicateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.Reference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.updates.ValueReference;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

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
    CellUpdaterSettings(final DefaultNodeSettingsContext context) {
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

    interface ColumnModeRef extends Reference<ColumnMode> {
    }

    static final class ColumnModeIsByName implements PredicateProvider {
        @Override
        public Predicate init(final PredicateInitializer i) {
            return i.getEnum(ColumnModeRef.class).isOneOf(ColumnMode.BY_NAME);
        }
    }

    @Widget(title = "Column specification", description = "Select whether to specify the column by name or by number.")
    @ValueSwitchWidget
    @ValueReference(ColumnModeRef.class)
    ColumnMode m_columnMode = ColumnMode.BY_NAME;

    @Widget(title = "Column name", description = "Select the column that contains the target cell.")
    @ChoicesProvider(AllColumns.class)
    @Effect(predicate = ColumnModeIsByName.class, type = EffectType.SHOW)
    String m_columnName;

    @Widget(title = "Column number", description = "Provide the number of the column that contains the target cell.")
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    @Effect(predicate =  ColumnModeIsByName.class, type = EffectType.HIDE)
    int m_columnNumber = 1;

    @Widget(title = "Row number", description = "Provide the number of the row that contains the target cell.")
    @NumberInputWidget(validation = IsPositiveIntegerValidation.class)
    int m_rowNumber = 1;

    @Widget(title = "Count rows from the end of the table",
        description = "If selected, the rows will be counted from the end of the table.")
    boolean m_countFromEnd = false;

    @Widget(title = "New cell value", description = "Select the flow variable containing the new cell value.")
    @ChoicesProvider(AllVariables.class)
    String m_flowVariableName;

    private static final class AllColumns extends AllColumnsProvider {

        @Override
        public int getInputTableIndex() {
            return 1;
        }

    }

    private static final class AllVariables implements StringChoicesProvider {
        @Override
        public List<String> choices(final DefaultNodeSettingsContext context) {
            return Arrays.asList(context.getAvailableFlowVariableNames());
        }
    }

    enum ColumnMode {
            @Label("Name")
            BY_NAME,

            @Label("Number")
            BY_NUMBER;
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
