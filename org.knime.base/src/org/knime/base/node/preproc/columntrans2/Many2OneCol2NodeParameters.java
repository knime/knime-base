/*
 * ------------------------------------------------------------------------
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
 *   20 Aug 2025 (Robin Gerling, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.columntrans2;

import org.knime.base.node.preproc.pmml.columntrans2.Many2OneCol2PMMLNodeModel;
import org.knime.base.node.preproc.pmml.columntrans2.Many2OneCol2PMMLNodeModel.IncludeMethod;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.EnumSettingsModelStringPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelBooleanPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.persistors.settingsmodel.SettingsModelStringPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

/**
 * Settings for the Web UI dialog of the Many to One node.
 *
 * @author Robin Gerling, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@SuppressWarnings("restriction")
final class Many2OneCol2NodeParameters implements NodeParameters {

    /**
     * Constructor for persistence and conversion from JSON.
     */
    Many2OneCol2NodeParameters() {
    }

    Many2OneCol2NodeParameters(final NodeParametersInput context) {
        m_includedColumns =
            new ColumnFilter(ColumnSelectionUtil.getAllColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    @Persistor(IncludedColumnsPersistor.class)
    @Widget(title = "Columns to aggregate",
        description = "Select those columns which should be condensed into the aggregated result column.")
    @ChoicesProvider(AllColumnsProvider.class)
    ColumnFilter m_includedColumns = new ColumnFilter();

    @Persistor(AppendedColumnNamePersistor.class)
    @Widget(title = "Appended column name", description = "Name of the aggregate column that will be created.")
    String m_appendedColumnName = "Condensed Column";

    static class IncludeMethodRef implements ParameterReference<IncludeMethod> {

    }

    @Persistor(IncludeMethodPersistor.class)
    @Widget(title = "Include method", //
        description = "Choose the method to determine the matching column:")
    @ValueReference(IncludeMethodRef.class)
    IncludeMethod m_includeMethod = IncludeMethod.Binary;

    static class IsRegExpPattern implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(IncludeMethodRef.class).isOneOf(IncludeMethod.RegExpPattern);
        }
    }

    @Persistor(PatternPersistor.class)
    @Widget(title = "Include Pattern",
        description = "Enter the regular expression pattern if RegExpPattern was chosen as include method.")
    @Effect(predicate = IsRegExpPattern.class, type = EffectType.SHOW)
    String m_pattern = "[^0]*";

    @Persistor(KeepColumnsPersistor.class)
    @Widget(title = "Keep original columns",
        description = "If checked, the selected columns are kept in the output table, otherwise they are deleted.")
    boolean m_keepColumns = true;

    static final class IncludedColumnsPersistor extends LegacyColumnFilterPersistor {
        IncludedColumnsPersistor() {
            super(Many2OneCol2PMMLNodeModel.SELECTED_COLS);
        }
    }

    static final class AppendedColumnNamePersistor extends SettingsModelStringPersistor {
        AppendedColumnNamePersistor() {
            super(Many2OneCol2PMMLNodeModel.CONDENSED_COL_NAME);
        }
    }

    static final class IncludeMethodPersistor extends EnumSettingsModelStringPersistor<IncludeMethod> {
        IncludeMethodPersistor() {
            super(Many2OneCol2PMMLNodeModel.INCLUDE_METHOD, IncludeMethod.class);
        }
    }

    static final class PatternPersistor extends SettingsModelStringPersistor {
        PatternPersistor() {
            super(Many2OneCol2PMMLNodeModel.RECOGNICTION_REGEX);
        }
    }

    static final class KeepColumnsPersistor extends SettingsModelBooleanPersistor {
        KeepColumnsPersistor() {
            super(Many2OneCol2PMMLNodeModel.KEEP_COLS);
        }
    }
}
