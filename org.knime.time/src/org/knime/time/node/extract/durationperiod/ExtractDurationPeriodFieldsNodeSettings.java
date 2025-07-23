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
 *   Nov 20, 2024 (david): created
 */
package org.knime.time.node.extract.durationperiod;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.time.duration.DurationValue;
import org.knime.core.data.time.period.PeriodValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;

/**
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
@SuppressWarnings("restriction")
class ExtractDurationPeriodFieldsNodeSettings implements NodeParameters {

    @Widget(title = "Duration column", description = """
            A duration column from which to extract the fields.
            """)
    @ChoicesProvider(SelectedInputColumnHelpers.ColumnChoicesProvider.class)
    @ValueReference(SelectedInputColumnHelpers.ValueRef.class)
    @ValueProvider(SelectedInputColumnHelpers.ValueProvider.class)
    public String m_selectedColumn;

    @Widget(title = "Extracted fields", description = "Define the fields to extract and their column names.")
    @ArrayWidget( //
        elementLayout = ElementLayout.HORIZONTAL_SINGLE_LINE, //
        addButtonText = "Add field", //
        showSortButtons = true, //
        elementDefaultValueProvider = ExtractFieldSettings.DefaultExtractFieldWidgetProvider.class //
    )
    ExtractFieldSettings[] m_extractFields = new ExtractFieldSettings[0];

    /**
     * Helper classes for the selected input column widget. Grouped here into an otherwise empty inner class because we
     * have so many of them.
     */
    static final class SelectedInputColumnHelpers {

        private SelectedInputColumnHelpers() {
            // prevent instantiation
        }

        static final class ColumnChoicesProvider extends CompatibleColumnsProvider {

            static final List<Class<? extends DataValue>> COMPATIBLE_TYPES =
                List.of(DurationValue.class, PeriodValue.class);

            ColumnChoicesProvider() {
                super(COMPATIBLE_TYPES);
            }

            static boolean isCompatible(final DataColumnSpec colSpec) {
                return COMPATIBLE_TYPES.stream().anyMatch(colSpec.getType()::isCompatible);
            }
        }

        static final class ValueRef implements ParameterReference<String> {
        }

        static final class ValueProvider implements StateProvider<String> {

            private Supplier<String> m_valueSupplier;

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeAfterOpenDialog();
                m_valueSupplier = initializer.getValueSupplier(ValueRef.class);
            }

            @Override
            public String computeState(final NodeParametersInput context) {
                if (m_valueSupplier.get() == null || m_valueSupplier.get().isEmpty()) {
                    return context.getInTableSpec(0) //
                        .map(ValueProvider::getFirstCompatibleColumnNameInInputTable) //
                        .orElse(null);
                } else {
                    return m_valueSupplier.get();
                }
            }

            private static String getFirstCompatibleColumnNameInInputTable(final DataTableSpec spec) {
                Optional<DataColumnSpec> bestGuessColumnSpec = spec.stream() //
                    .filter(ColumnChoicesProvider::isCompatible) //
                    .findFirst(); //

                return bestGuessColumnSpec.map(DataColumnSpec::getName).orElse(null);
            }
        }
    }
}
