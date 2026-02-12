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
 *
 * History
 *   12.02.2026 (mgohm): created
 */
package org.knime.base.node.preproc.tablestructure;

import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.ColumnExistenceHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorColConfiguration.DataTypeHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.RejectBehavior;
import org.knime.base.node.preproc.datavalidator.DataValidatorConfiguration.UnknownColumnHandling;
import org.knime.base.node.preproc.datavalidator.DataValidatorSpecNodeParameters.ColumnNameMatchingEnum;
import org.knime.core.data.DataCellFactory.FromString;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.def.StringCell;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.DoNotPersistBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.array.ArrayWidget.ElementLayout;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * {@link NodeParameters} for the Table Structure Validator node.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
final class TableStructureValidatorNodeParameters implements NodeParameters {

    @Section(title = "Reference Structure")
    interface ReferenceStructureSection {
    }

    @Section(title = "Structure Validation")
    @After(ReferenceStructureSection.class)
    interface StructureValidationSection {
    }

    @Section(title = "Output")
    @After(StructureValidationSection.class)
    interface OutputSection {
    }

    @Layout(ReferenceStructureSection.class)
    @Widget(title = "Reference structure", description = """
            Define the reference structure to validate against by specifying the expected columns and their types.
            """)
    @ArrayWidget(elementLayout = ElementLayout.VERTICAL_CARD, addButtonText = "Add column")
    @ValueProvider(ReferenceStructureColumnsArrayProvider.class)
    @ValueReference(ReferenceStructureColumnsArrayRef.class)
    ReferenceStructureColumns[] m_referenceStructureColumns = new ReferenceStructureColumns[0];

    static final class ReferenceStructureColumnsArrayRef implements ParameterReference<ReferenceStructureColumns[]> {
    }

    static final class ReferenceStructureColumns implements NodeParameters {

        @HorizontalLayout
        interface ColumnNameAndTypeLayout {
        }

        @Layout(ColumnNameAndTypeLayout.class)
        @Widget(title = "Column name", description = "The name of the reference column to validate against.")
        @TextInputWidget(patternValidation = ColumnNameValidation.class)
        String m_columnName;

        @Layout(ColumnNameAndTypeLayout.class)
        @Widget(title = "Column type", description = "The type of the reference column to validate against.")
        @ChoicesProvider(SupportedDataTypeChoicesProvider.class)
        @ValueReference(ColumnTypeRef.class)
        DataType m_columnType = StringCell.TYPE;

        static final class ColumnTypeRef implements ParameterReference<DataType> {
        }

        ReferenceStructureColumns() {
        }

        ReferenceStructureColumns(final String columnName, final DataType columnType) {
            m_columnName = columnName;
            m_columnType = columnType;
        }

        static final class SupportedDataTypeChoicesProvider implements DataTypeChoicesProvider {

            @Override
            public void init(final StateProviderInitializer initializer) {
                initializer.computeBeforeOpenDialog();
            }

            @Override
            public List<DataType> choices(final NodeParametersInput context) {
                return DataTypeRegistry.getInstance().availableDataTypes().stream() //
                    .filter(d -> {
                        var factory = d.getCellFactory(null);
                        return factory.map(FromString.class::isInstance).orElse(false);
                    }).toList();
            }

        }

    }

    static final class ReferenceStructureColumnsArrayProvider implements StateProvider<ReferenceStructureColumns[]> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(SetInputTableAsTemplateRef.class);
        }

        @Override
        public ReferenceStructureColumns[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var inTableSpecOpt = parametersInput.getInTableSpec(0);
            if (inTableSpecOpt.isEmpty()) {
                return new ReferenceStructureColumns[0];
            }
            return inTableSpecOpt.get().stream()
                    .map(cSpec -> new ReferenceStructureColumns(cSpec.getName(), cSpec.getType()))
                    .toArray(ReferenceStructureColumns[]::new);
        }

    }

    @Layout(ReferenceStructureSection.class)
    @Widget(title = "Set input table as template", description = """
            Sets the input table specification as reference structure template.
            """)
    @SimpleButtonWidget(ref = SetInputTableAsTemplateRef.class)
    @Effect(predicate = InputTableIsTemplate.class, type = EffectType.DISABLE)
    Void m_setInputTableAsTemplate;

    static final class SetInputTableAsTemplateRef implements ButtonReference {
    }

    @Persistor(DoNotPersistBoolean.class)
    @ValueReference(InputTableIsTemplate.class)
    @ValueProvider(InputTableIsTemplateProvider.class)
    boolean m_inputTableIsTemplate;

    static final class InputTableIsTemplate implements BooleanReference {
    }

    static final class InputTableIsTemplateProvider implements StateProvider<Boolean> {

        Supplier<ReferenceStructureColumns[]> m_referenceStructureColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_referenceStructureColumnsSupplier =
                    initializer.computeFromValueSupplier(ReferenceStructureColumnsArrayRef.class);
        }

        @SuppressWarnings("java:S3655") // optional for in table spec is present
        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) {
            final var inTableSpecOpt = parametersInput.getInTableSpec(0);
            final var referenceStructureColumns = m_referenceStructureColumnsSupplier.get();
            if ((referenceStructureColumns == null || referenceStructureColumns.length == 0)
                    != (inTableSpecOpt.isEmpty() || inTableSpecOpt.get().getNumColumns() == 0)) {
                return false; // “exactly one missing”
            }
            if (referenceStructureColumns == null || referenceStructureColumns.length == 0) {
                return true;  // both missing
            }

            final var inTableSpec = inTableSpecOpt.get();
            if (referenceStructureColumns.length != inTableSpec.getNumColumns()) {
                return false;
            }

            for (int i = 0; i < referenceStructureColumns.length; i++) {
                final var colSpec = inTableSpec.getColumnSpec(i);
                if (!colSpec.getName().equals(referenceStructureColumns[i].m_columnName)
                        || !colSpec.getType().equals(referenceStructureColumns[i].m_columnType)) {
                    return false;
                }
            }

            return true;
        }

    }

    @Layout(StructureValidationSection.class)
    @Widget(title = "Column name matching", description = """
            Controls what counts as a column name match between the input table and the reference table. If
            'case insensitive' is chosen, it still tries to find an exactly (case sensitively) matching column name
            first, and then falls back to case insensitive matching.
            """)
    @ValueSwitchWidget
    ColumnNameMatchingEnum m_columnNameMatchingBehavior = ColumnNameMatchingEnum.CASE_SENSITIVE;

    @Layout(StructureValidationSection.class)
    @Widget(title = "If a column is not in the table", description = """
            Ensures that the configured columns exist in the input table. If case insensitive name matching
            is selected, the first matching column will satisfy this condition.
            """)
    @RadioButtonsWidget
    ColumnExistenceHandling m_missingColumnHandling = ColumnExistenceHandling.NONE;

    @Layout(StructureValidationSection.class)
    @Widget(title = "If there is an additional column in the table", description = """
            Specifies how to handle columns which are not included in the reference table but present in the table
            to validate.
            """)
    @ValueSwitchWidget
    UnknownColumnHandling m_additionalColumnsHandling = UnknownColumnHandling.REJECT;

    @Layout(StructureValidationSection.class)
    @Widget(title = "If data type does not match", description = """
            Ensures correct data type for the columns.
            """)
    @ValueSwitchWidget
    DataTypeHandling m_dataTypeHandling = DataTypeHandling.FAIL;

    @Layout(OutputSection.class)
    @Widget(title = "If validation fails", description = """
            Controls how validation faults should influence the workflow execution.
            """)
    @ValueSwitchWidget
    RejectBehavior m_validationFailureBehavior = RejectBehavior.FAIL_NODE;

}
