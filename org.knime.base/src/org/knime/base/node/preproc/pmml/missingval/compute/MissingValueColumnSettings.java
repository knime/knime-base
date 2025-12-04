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
 *   Dec 3, 2025 (paulbaernreuther): created
 */
package org.knime.base.node.preproc.pmml.missingval.compute;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Supplier;

import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactoryChoicesProvider;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactoryDynamicSettingsProvider;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactoryIDRef;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.FactorySettingsRef;
import org.knime.base.node.preproc.pmml.missingval.compute.MissingValueTreatment.MissingValueTreatmentModifier;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;

class MissingValueColumnTreatment implements NodeParameters {

    MissingValueColumnTreatment() {
        // default constructor
    }

    MissingValueColumnTreatment(final NodeParametersInput input) {
        ColumnSelectionUtil.getFirstColumnOfFirstPort(input).ifPresent(col -> m_colNames = new String[]{col.getName()});
    }

    @ChoicesProvider(AllColumnsProvider.class)
    @Widget(title = "Columns",
        description = "Choose one or multiple columns for which a missing values treatment should be defined."
            + "If multiple columns are selected, only treatment options that are applicable to all of their "
            + "data types become available.")
    @ValueReference(ColNamesReference.class)
    String[] m_colNames = new String[0];

    interface ColNamesReference extends ParameterReference<String[]> {
    }

    private static final String TREATMENT_SETTINGS_CFG_KEY = "settings";

    @SuppressWarnings("restriction")
    @Modification(ColumnTreatmentModifier.class)
    @Persist(configKey = TREATMENT_SETTINGS_CFG_KEY)
    MissingValueTreatment m_treatment = new MissingValueTreatment();

    static final class ColumnTreatmentModifier extends MissingValueTreatmentModifier {

        @Override
        Class<? extends FactoryChoicesProvider> getChoicesProviderClass() {
            return ColumnFactoryChoicesProvider.class;
        }

        @Override
        Class<? extends FactoryDynamicSettingsProvider> getDynamicSettingsProviderClass() {
            return ColumnFactoryDynamicSettingsProvider.class;
        }

        @Override
        Class<? extends FactoryIDRef> getFactoryIDParameterRefClass() {
            return ColumnFactoryIDReference.class;
        }

        @Override
        Class<? extends FactorySettingsRef> getParameterRefClass() {
            return ColumnTreatmentParametersReference.class;
        }

    }

    static final class ColumnFactoryChoicesProvider extends FactoryChoicesProvider {

        private Supplier<String[]> m_columnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            super.init(initializer);
            m_columnsSupplier = initializer.computeFromValueSupplier(ColNamesReference.class);
        }

        @Override
        DataType[] getDataTypes(final NodeParametersInput context) {
            final var spec = context.getInTableSpec(0);
            if (spec.isEmpty()) {
                return new DataType[0];
            }
            final var tableSpec = spec.get();
            return Arrays.stream(m_columnsSupplier.get())
                .flatMap(col -> Optional.ofNullable(tableSpec.getColumnSpec(col)).stream().map(DataColumnSpec::getType))
                .distinct().toArray(DataType[]::new);
        }

    }

    static final class ColumnFactoryDynamicSettingsProvider extends FactoryDynamicSettingsProvider {

        @Override
        protected Class<? extends FactorySettingsRef> getParameterRefClass() {
            return ColumnTreatmentParametersReference.class;
        }

        @Override
        protected Class<? extends FactoryIDRef> getFactoryIDParameterRefClass() {
            return ColumnFactoryIDReference.class;
        }

        @Override
        Class<? extends FactoryChoicesProvider> getChoicesProviderClass() {
            return ColumnFactoryChoicesProvider.class;
        }

    }

    interface ColumnTreatmentParametersReference extends FactorySettingsRef {
    }

    interface ColumnFactoryIDReference extends FactoryIDRef {
    }

}
