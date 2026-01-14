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

package org.knime.base.node.preproc.domain.editnominal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

import org.apache.commons.lang3.StringUtils;
import org.knime.base.node.preproc.domain.editnominal.EditNominalDomainNodeParameters.ColumnDomainParameters.InsertNewAddedDomainValues.NewValuesAndDomainValuesChoicesProvider;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.SortListWidget;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.choices.DomainChoicesUtil;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.FilteredInputTableColumnsProvider;

/**
 * Node parameters for Edit Nominal Domain.
 *
 * @author Paul Baernreuther, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
public class EditNominalDomainNodeParameters implements NodeParameters {

    @Widget(title = "Configured columns",
        description = "The list of columns for which the domain values should be edited.")
    @ArrayWidget(addButtonText = "Add column to edit", elementTitle = "Column")
    @Persistor(ColumnDomainParametersPersistor.class)
    @Migration(LoadDefaultIfColumnsKeyAbsent.class)
    List<ColumnDomainParameters> configuredColumns = List.of(new ColumnDomainParameters());

    enum MissingColumnPolicy {
            @Label(value = "Fail", description = "The node execution fails.")
            FAIL, //
            @Label(value = "Ignore column", description = "The invalid columns are kept unchanged.")
            IGNORE;
    }

    @Widget(title = "If a modified column is not present in data",
        description = "Determine the behavior if a modified column "
            + "is invalid since it no longer exists in the input table.")
    @ValueSwitchWidget
    @Persistor(IgnoreMissingColumnsPersistor.class)
    MissingColumnPolicy ignoreMissingColumns = MissingColumnPolicy.FAIL;

    @Widget(title = "If a modified column has an incompatible type",
        description = "Determine the behavior if a modified column "
            + "is invalid since it is no longer of type <i>\"String\"</i>.")
    @ValueSwitchWidget
    @Persistor(IgnoreIncompatibleTypesPersistor.class)
    MissingColumnPolicy ignoreIncompatibleTypes = MissingColumnPolicy.FAIL;

    static final class LoadDefaultIfColumnsKeyAbsent implements NodeParametersMigration<List<ColumnDomainParameters>> {

        @Override
        public List<ConfigMigration<List<ColumnDomainParameters>>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(settings -> List.of(new ColumnDomainParameters()))
                .withMatcher(s -> !s.containsKey(ColumnDomainParametersPersistor.COLUMNS_KEY))
                // we still list it as deprecated to show old flow variables
                .withDeprecatedConfigPath(ColumnDomainParametersPersistor.COLUMNS_KEY).build());
        }

    }

    static class ColumnDomainParameters implements NodeParameters {

        ColumnDomainParameters() {
            // default constructor
        }

        ColumnDomainParameters(final NodeParametersInput input) {
            input.getInTableSpec(0).stream().flatMap(DataTableSpec::stream)
                .filter(col -> StringCell.TYPE.equals(col.getType())).findFirst()
                .ifPresent(col -> m_column = col.getName());
        }

        @Widget(title = "Column",
            description = "Selects the column for which the domain values should be edited. Non-string typed columns are filtered out.")
        @ChoicesProvider(StringCellColumnsProvider.class)
        @ValueReference(ColumnRef.class)
        @ValueProvider(AutoGuessColumnProvider.class)
        String m_column;

        static final class AutoGuessColumnProvider extends ColumnNameAutoGuessValueProvider {

            protected AutoGuessColumnProvider() {
                super(ColumnRef.class);
            }

            @Override
            protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
                return parametersInput.getInTableSpec(0).stream().flatMap(DataTableSpec::stream)
                    .filter(col -> StringCell.TYPE.equals(col.getType())).findFirst();
            }

        }

        interface ColumnRef extends ParameterReference<String> {
        }

        static final class StringCellColumnsProvider implements FilteredInputTableColumnsProvider {

            @Override
            public boolean isIncluded(final DataColumnSpec col) {
                return StringCell.TYPE.equals(col.getType());
            }

        }

        @ValueReference(DomainValuesRef.class)
        @ValueProvider(DomainValuesProvider.class)
        String[] m_domainValues = new String[0];

        interface DomainValuesRef extends ParameterReference<String[]> {
        }

        static final class DomainValuesProvider implements StateProvider<String[]> {

            private Supplier<String> m_columnNameSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_columnNameSupplier = initializer.computeFromValueSupplier(ColumnRef.class);
            }

            @Override
            public String[] computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                return DomainChoicesUtil.getChoicesByContextAndColumn(parametersInput, m_columnNameSupplier.get())
                    .toArray(String[]::new);
            }
        }

        @Widget(title = "Added domain values",
            description = "Specify values to be added to the domain."
                + " The values appear as part of the sorted domain values and are marked as new (e.g. "
                + "<i>'value (new)'</i>). Values that are already part of the domain are ignored.")
        @ValueReference(NewDomainValuesRef.class)
        String[] m_newDomainValues = new String[0];

        interface NewDomainValuesRef extends ParameterReference<String[]> {
        }

        @Widget(title = "Sorted domain values",
            description = "Specify the order of the domain values (including "
                + "newly added values). The position of values that are not yet part of the domain (unknown values) is "
                + "determined by the special entry <i>Any unknown value</i>.")
        @SortListWidget(unknownElementId = EditNominalDomainConfiguration.UNKNOWN_VALUES_ID,
            unknownElementLabel = "Any unknown value", resetSortButtonLabel = "Reset sorting")
        @ValueProvider(InsertNewAddedDomainValues.class)
        @ValueReference(SortedDomainValuesRef.class)
        @ChoicesProvider(NewValuesAndDomainValuesChoicesProvider.class)
        String[] m_sortedDomainValues = new String[0];

        interface SortedDomainValuesRef extends ParameterReference<String[]> {
        }

        static final class InsertNewAddedDomainValues implements StateProvider<String[]> {

            private Supplier<String[]> m_newValues;

            private Supplier<String[]> m_domainValues;

            private Supplier<String[]> m_sortedValues;

            @Override
            public void init(final StateProviderInitializer initializer) {
                m_newValues = initializer.computeFromValueSupplier(NewDomainValuesRef.class);
                m_domainValues = initializer.computeFromValueSupplier(DomainValuesRef.class);
                m_sortedValues = initializer.getValueSupplier(SortedDomainValuesRef.class);
            }

            @Override
            public String[] computeState(final NodeParametersInput parametersInput)
                throws StateComputationFailureException {
                final var newValues = m_newValues.get();
                final var domainValues = m_domainValues.get();
                final var sortedValues = m_sortedValues.get();

                Set<String> domainValuesSet = new HashSet<>(Arrays.asList(domainValues));
                Set<String> sortedValuesSet = new HashSet<>(Arrays.asList(sortedValues));
                Set<String> newValuesSet = new HashSet<>(Arrays.asList(newValues));
                List<String> resultList = new ArrayList<>();
                // First, add new values that are not already in sorted values
                for (String v : newValues) {
                    if (!sortedValuesSet.contains(v)) {
                        resultList.add(v);
                    }
                }
                if (sortedValuesSet.contains(EditNominalDomainConfiguration.UNKNOWN_VALUES_ID)
                    && sortedValuesSet.size() == 1 && newValuesSet.isEmpty()) {
                    /**
                     * To prevent that the "Any unknown value" option is at the top initially, we return the domain
                     * values here since the frontend control would otherwise include them below the "Any unknown value"
                     * option.
                     */
                    for (var dv : domainValues) {
                        resultList.add(dv);
                    }
                    resultList.add(EditNominalDomainConfiguration.UNKNOWN_VALUES_ID);
                } else {
                    // Then, add sorted values that are in domain values or new values
                    for (String v : sortedValues) {
                        if (domainValuesSet.contains(v) || newValuesSet.contains(v)
                            || EditNominalDomainConfiguration.UNKNOWN_VALUES_ID.equals(v)) {
                            resultList.add(v);
                        }
                    }
                }
                return resultList.toArray(new String[0]);

            }

            static final class NewValuesAndDomainValuesChoicesProvider implements StringChoicesProvider {

                private Supplier<String[]> m_newValues;

                private Supplier<String[]> m_domainValues;

                @Override
                public void init(final StateProviderInitializer initializer) {
                    initializer.computeAfterOpenDialog();
                    m_newValues = initializer.computeFromValueSupplier(NewDomainValuesRef.class);
                    m_domainValues = initializer.computeFromValueSupplier(DomainValuesRef.class);
                }

                @Override
                public List<StringChoice> computeState(final NodeParametersInput context) {
                    return mergeWithNewValues(m_domainValues.get(), m_newValues.get());
                }

                private static List<StringChoice> mergeWithNewValues(final String[] domainChoices,
                    final String[] newValues) {
                    List<StringChoice> mergedChoices = new ArrayList<>();
                    for (String v : domainChoices) {
                        mergedChoices.add(new StringChoice(v, v));
                    }
                    Set<String> existingValuesSet = new HashSet<>(Arrays.asList(domainChoices));
                    if (newValues != null) {
                        for (String v : newValues) {
                            if (!existingValuesSet.contains(v)) {
                                mergedChoices.add(0, new StringChoice(v, v + " (new)"));
                            }
                        }
                    }
                    return mergedChoices;
                }
            }

        }

    }

    static final class ColumnDomainParametersPersistor
        implements NodeParametersPersistor<List<ColumnDomainParameters>> {

        private static final String COLUMNS_KEY = "columns";

        private static final String COLUMN_DOMAIN_VALUE_ORDERING_KEY = "column-domain-value-ordering";

        private static final String CREATED_DOMAIN_VALUES_KEY = "created-domain-values";

        @Override
        public List<ColumnDomainParameters> load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var columns = settings.getNodeSettings(COLUMNS_KEY);
            final List<ColumnDomainParameters> columnDomains = new ArrayList<>();
            for (var columnSettingIter = columns.iterator(); columnSettingIter.hasNext();) {
                final var columnDomain = new ColumnDomainParameters();
                final var columnName = columnSettingIter.next();
                columnDomain.m_column = columnName;
                final var columnSettings = columns.getNodeSettings(columnName);
                final var createdDomainValues =
                    columnSettings.getDataCellArray(CREATED_DOMAIN_VALUES_KEY, new DataCell[0]);
                columnDomain.m_newDomainValues = toStringArray(createdDomainValues);
                final var columnDomainValueOrdering =
                    columnSettings.getDataCellArray(COLUMN_DOMAIN_VALUE_ORDERING_KEY, new DataCell[0]);
                columnDomain.m_sortedDomainValues = toStringArray(columnDomainValueOrdering);
                columnDomains.add(columnDomain);
            }
            return columnDomains;
        }

        static String[] toStringArray(final DataCell[] cells) {
            String[] values = new String[cells.length];
            for (int i = 0; i < cells.length; i++) {
                values[i] = ((StringCell)cells[i]).getStringValue();
            }
            return values;
        }

        @Override
        public void save(final List<ColumnDomainParameters> param, final NodeSettingsWO settings) {
            final var columnsSettings = settings.addNodeSettings(COLUMNS_KEY);
            for (final var columnDomain : param) {
                if (StringUtils.isEmpty(columnDomain.m_column)) {
                    continue;
                }
                final var columnSettings = columnsSettings.addNodeSettings(columnDomain.m_column);
                addDataCellArraySetting(CREATED_DOMAIN_VALUES_KEY, //
                    columnDomain.m_newDomainValues, columnSettings);
                addDataCellArraySetting(COLUMN_DOMAIN_VALUE_ORDERING_KEY, //
                    columnDomain.m_sortedDomainValues, columnSettings);
            }
        }

        static final void addDataCellArraySetting(final String key, final String[] values,
            final NodeSettingsWO settings) {
            final var cells = new DataCell[values.length];
            for (int i = 0; i < values.length; i++) {
                cells[i] = new StringCell(values[i]);
            }
            settings.addDataCellArray(key, cells);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[0][];
        }

    }

    static final class IgnoreMissingColumnsPersistor extends EnumBooleanPersistor<MissingColumnPolicy> {
        IgnoreMissingColumnsPersistor() {
            super("ignore-not-present-col", MissingColumnPolicy.class, MissingColumnPolicy.IGNORE);
        }
    }

    static final class IgnoreIncompatibleTypesPersistor extends EnumBooleanPersistor<MissingColumnPolicy> {
        IgnoreIncompatibleTypesPersistor() {
            super("ignore-not-matching-types", MissingColumnPolicy.class, MissingColumnPolicy.IGNORE);
        }
    }
}
