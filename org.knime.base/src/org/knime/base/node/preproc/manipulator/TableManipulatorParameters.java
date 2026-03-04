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
 * ------------------------------------------------------------------------
 */

package org.knime.base.node.preproc.manipulator;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

import static org.knime.base.node.preproc.manipulator.TableManipulatorConfigSerializer.CFG_SETTINGS_TAB;

/**
 * Node parameters for Table Manipulator.
 * 
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class TableManipulatorParameters implements NodeParameters {

    static final String CFG_COLUMN_FILTER_MODE = "column_filter_mode";

    @Persist(configKey = CFG_SETTINGS_TAB)
    Settings m_settings = new Settings();

    static class ColumnFilterModeOptionRef implements ParameterReference<ColumnFilterModeOption> {
    }

    @Widget(title = "Take columns from",
        description = "Only relevant when several input tables are available. Specifies which set of columns "
            + "is considered for the output table. <br />"
            + "<b>Note:</b><br/>This setting has special implications if you are changing the input table without "
            + "reconfiguring the node. If <i>Intersection</i> is selected, any column that moves into the intersection "
            + "during execution will be considered to be new, even if it was previously part of the union of columns.")
    @ValueSwitchWidget
    @ValueReference(ColumnFilterModeOptionRef.class)
    @Persist(configKey = CFG_COLUMN_FILTER_MODE)
    @Effect(predicate = HasMultipleInputTables.class, type = EffectType.SHOW)
    @Layout(TableManipulatorNodeParameters.TableTransformationSection.class)
    @Migration(TableManipulatorMigration.ColumnFilterModeMigration.class)
    ColumnFilterModeOption m_columnFilterMode = ColumnFilterModeOption.UNION;

    private static final class Settings implements NodeParameters {

        private interface UseExistingRowIDRef extends ParameterReference<Boolean> {
        }

        @Widget(title = "Use existing RowID",
            description = "Check this box if the RowIDs from the input tables should be used for the output tables."
                + "If unchecked, a new RowID is generated. The generated RowID follows the schema \"Row0\", \"Row1\" "
                + "and so on.")
        @Persist(configKey = "has_row_id")
        @ValueReference(UseExistingRowIDRef.class)
        @Layout(TableManipulatorNodeParameters.RowIdHandlingSection.class)
        boolean m_useExistingRowID;

        private interface PrependTableIndexRef extends ParameterReference<Boolean> {
        }

        @Widget(title = "Prepend table index to RowID",
            description = "Only enabled if the existing RowIDs are used. If checked, a prefix is prepended "
                + "to the RowIDs that indicates which table the row came from. "
                + "The format of the prefix is \"Table_0_\", \"Table_1_\", and so on.")
        @Persist(configKey = "prepend_table_index_to_row_id")
        @ValueReference(PrependTableIndexRef.class)
        @Effect(predicate = ShowPrependTableIndex.class, type = EffectType.SHOW)
        @Layout(TableManipulatorNodeParameters.RowIdHandlingSection.class)
        boolean m_prependTableIndexToRowID;

        private interface InitialPrependTableIndexRef extends ParameterReference<Boolean> {
        }

        @Persistor(DoNotPersistBoolean.class)
        @ValueProvider(InitialPrependTableIndexProvider.class)
        @ValueReference(InitialPrependTableIndexRef.class)
        boolean m_initialPrependTableIndexToRowID;
    }

    enum ColumnFilterModeOption {
            @Label(value = "Union", description = "Any column that is part of any input table is considered. "
                + "If an input table is missing a column, it is filled up with missing values.")
            UNION,

            @Label(value = "Intersection",
                description = "Only columns that appear in all input tables are considered for the output table.")
            INTERSECTION;

        ColumnFilterMode toColumnFilterMode() {
            return switch (this) {
                case UNION -> ColumnFilterMode.UNION;
                case INTERSECTION -> ColumnFilterMode.INTERSECTION;
            };
        }

        static ColumnFilterModeOption fromColumnFilterMode(final ColumnFilterMode columnFilterMode) {
            return switch (columnFilterMode) {
                case UNION -> ColumnFilterModeOption.UNION;
                case INTERSECTION -> ColumnFilterModeOption.INTERSECTION;
            };
        }
    }

    private static final class InitialPrependTableIndexProvider implements StateProvider<Boolean> {
        private java.util.function.Supplier<Boolean> m_prependTableIndexSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            m_prependTableIndexSupplier = initializer.getValueSupplier(Settings.PrependTableIndexRef.class);
        }

        @Override
        public Boolean computeState(final org.knime.node.parameters.NodeParametersInput context) {
            return m_prependTableIndexSupplier.get();
        }
    }

    private static final class ShowPrependTableIndex implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            final var useExistingRowID = i.getBoolean(Settings.UseExistingRowIDRef.class).isTrue();
            final var hasMultipleInputTables = i.getConstant(context -> context.getInPortTypes().length > 1);
            final var initiallySet = i.getBoolean(Settings.InitialPrependTableIndexRef.class).isTrue();
            return useExistingRowID.and(hasMultipleInputTables.or(initiallySet));
        }
    }

    private static final class HasMultipleInputTables implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(context -> context.getInPortTypes().length > 1);
        }
    }

    ColumnFilterModeOption getColumnFilterMode() {
        return m_columnFilterMode;
    }

    /**
     * Currently, the configId is static for the table manipulator, see {@link
     * TableManipulatorConfigSerializer#createFromConfig(TableManipulatorMultiTableReadConfig)
     */
    ConfigID saveToConfig(final TableManipulatorMultiTableReadConfig config) {
        var tableReadConfig = config.getTableReadConfig();
        tableReadConfig.setUseRowIDIdx(m_settings.m_useExistingRowID);
        tableReadConfig.setPrependSourceIdxToRowId(m_settings.m_prependTableIndexToRowID);
        return config.getConfigID();
    }

    @Override
    public void validate() throws InvalidSettingsException {
        // nothing to validate
    }
}
