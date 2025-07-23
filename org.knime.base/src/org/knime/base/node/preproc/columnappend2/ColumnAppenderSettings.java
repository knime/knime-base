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
 *   Dec 23, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.columnappend2;

import static org.knime.base.node.preproc.columnappend2.ColumnAppender2NodeModel.NOT_SET;

import org.knime.base.node.preproc.columnappend2.ColumnAppender2NodeModel.RowKeyMode;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.EnumSettingsModelStringPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Settings for the Column Appender node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class ColumnAppenderSettings implements NodeParameters {

    static final class RowKeyModeSettingsModelStringPersistor extends EnumSettingsModelStringPersistor<RowKeyMode> {

        RowKeyModeSettingsModelStringPersistor() {
            super(ColumnAppender2NodeModel.KEY_SELECTED_ROWID_MODE, RowKeyMode.class);
        }

    }

    @Persistor(RowKeyModeSettingsModelStringPersistor.class)
    @Widget(title = "RowID mode", description = "Determines the RowIDs of the output table:" + "<ul>"//
        + "<li><b>Identical RowIDs and table lengths</b>: If the RowIDs in both input tables exactly match "
        + "(i.e. the RowID names, their order, and their number have to match) this option can "//
        + "be checked in order to allow a faster execution with less memory consumption. "//
        + "If the RowIDs (names, order, number) don't match exactly the node execution will fail. "//
        + "<br/><br/>" //
        + "If this option <i>is NOT checked</i> the result table is newly created. "//
        + "This might result in a longer processing time. "//
        + "However, in this case the number of rows in the input tables can differ and missing "//
        + "values are added accordingly. The RowIDs are either generated new or taken from "//
        + "one of the input tables (see options below).</li>"//
        + "<li><b>Generate new RowIDs</b>: RowIDs are newly generated. "//
        + "If one of the input tables is longer than the other, missing values are inserted accordingly.</li> "//
        + "<li><b>Use RowIDs from the selected input table</b>: "//
        + "The RowIDs of the table at the selected input port number are used. "//
        + "Tables with fewer rows will be filled with missing values accordingly. "//
        + "And tables with more rows will be truncated.</li>"//
        + "</ul>")
    @ValueReference(RowKeyModeRef.class)
    @RadioButtonsWidget
    RowKeyMode m_rowIdMode = RowKeyMode.IDENTICAL;

    static final class NumTablesMaxValidation implements StateProvider<MaxValidation> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public MaxValidation computeState(final NodeParametersInput context) {
            final var max = context.getInTableSpecs().length;
            return new MaxValidation() {
                @Override
                protected double getMax() {
                    return max;
                }

                @Override
                public String getErrorMessage() {
                    return String.format("Only %d table input ports available.", max);
                }
            };
        }

    }

    @Persistor(RowIdTableSelectPersistor.class)
    @Widget(title = "RowID table number",
        description = "Select the table whose RowIDs should be used for the output table.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class,
        maxValidationProvider = NumTablesMaxValidation.class)
    @Effect(type = EffectType.SHOW, predicate = IsKeyTable.class)
    int m_rowIdTableSelect = 1;

    interface RowKeyModeRef extends ParameterReference<RowKeyMode> {
    }

    static class IsKeyTable implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(RowKeyModeRef.class).isOneOf(RowKeyMode.KEY_TABLE);
        }
    }

    private static final class RowIdTableSelectPersistor implements NodeParametersPersistor<Integer> {

        @Override
        public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var zeroBased = settings.getInt(ColumnAppender2NodeModel.KEY_SELECTED_ROWID_TABLE);
            var oneBased = settings.getInt(ColumnAppender2NodeModel.KEY_SELECTED_ROWID_TABLE_NUMBER);

            if (oneBased == NOT_SET) {
                return zeroBased + 1;
            } else {
                return oneBased;
            }
        }

        @Override
        public void save(final Integer value, final NodeSettingsWO settings) {
            // we always set the old zero based index to -1 so that the model can detect
            // if it is overwritten by a flow variable
            settings.addInt(ColumnAppender2NodeModel.KEY_SELECTED_ROWID_TABLE, NOT_SET);
            settings.addInt(ColumnAppender2NodeModel.KEY_SELECTED_ROWID_TABLE_NUMBER, value);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{ //
                {ColumnAppender2NodeModel.KEY_SELECTED_ROWID_TABLE}, //
                {ColumnAppender2NodeModel.KEY_SELECTED_ROWID_TABLE_NUMBER} //
            };
        }
    }

}
