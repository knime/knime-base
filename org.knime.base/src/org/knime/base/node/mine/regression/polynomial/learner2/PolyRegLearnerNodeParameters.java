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

package org.knime.base.node.mine.regression.polynomial.learner2;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.mine.regression.MissingValueHandling;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataValue;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LegacyColumnFilterPersistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Polynomial Regression Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class PolyRegLearnerNodeParameters implements NodeParameters {

    @Section(title = "View Settings")
    interface ViewSettingsSection {
    }

    @Persist(configKey = PolyRegLearnerSettings.CFG_TARGET_COLUMN)
    @Widget(title = "Target column (dependent variable)", description = """
            The column that contains the dependent "target" variable.
            """)
    @ChoicesProvider(DoubleColumnsProvider.class)
    @ValueReference(TargetColumnRef.class)
    String m_targetColumn;

    @Persist(configKey = PolyRegLearnerSettings.CFG_DEGREE)
    @Widget(title = "Maximum polynomial degree", description = """
            The maximum degree the polynomial regression function should have.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_degree = 2;

    @Widget(title = "Independent variables", description = """
            Select the columns containing the independent variables and move them to the "include" list.
            """)
    @ChoicesProvider(IncludedColumnsProvider.class)
    @Persistor(IncludedColumnsPersistor.class)
    ColumnFilter m_includedColumns = new ColumnFilter().withIncludeUnknownColumns();

    @Persistor(MissingValueHandlingPersistor.class)
    @Widget(title = "Missing values in input data", description = """
            Define how to handle rows with missing values in the input data.
            """)
    @ValueSwitchWidget
    MissingValueMode m_missingValueHandling = MissingValueMode.FAIL;

    @Layout(ViewSettingsSection.class)
    @Persist(configKey = PolyRegLearnerSettings.CFG_MAX_VIEW_ROWS)
    @Widget(title = "Number of data points to show in view", description = """
            This option can be used to change the number of data points in the node view if e.g. there are too
            many points. The default value is 10,000 points.
            """)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_maxRowsForView = 10000;

    static final class TargetColumnRef implements ParameterReference<String> {
    }

    static final class IncludedColumnsPersistor extends LegacyColumnFilterPersistor {

        protected IncludedColumnsPersistor() {
            super(PolyRegLearnerSettings.CFG_COLUMN_FILTER);
        }

    }

    static final class MissingValueHandlingPersistor implements NodeParametersPersistor<MissingValueMode> {
        @Override
        public MissingValueMode load(final NodeSettingsRO settings) throws InvalidSettingsException {
            var legacyValue = settings.getString(
                PolyRegLearnerSettings.CFG_MISSING_VALUE_HANDLING, MissingValueHandling.ignore.name());
            return MissingValueMode.fromLegacy(MissingValueHandling.valueOf(legacyValue));
        }

        @Override
        public void save(final MissingValueMode value, final NodeSettingsWO settings) {
            settings.addString(PolyRegLearnerSettings.CFG_MISSING_VALUE_HANDLING, value.toLegacy().name());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{PolyRegLearnerSettings.CFG_MISSING_VALUE_HANDLING}};
        }
    }

    enum MissingValueMode {
        @Label(value = "Fail", description = """
                Stops execution with an error if missing values occur in the input data.
                """)
        FAIL(MissingValueHandling.fail), //

        @Label(value = "Ignore", description = """
                Skips rows containing missing values so the regression model is built only on complete rows.
                """)
        IGNORE(MissingValueHandling.ignore);


        MissingValueMode(final MissingValueHandling delegate) {
            m_delegate = delegate;
        }

        MissingValueHandling toLegacy() {
            return m_delegate;
        }

        static MissingValueMode fromLegacy(final MissingValueHandling handling) {
            return handling == MissingValueHandling.ignore ? IGNORE : FAIL;
        }

        private final MissingValueHandling m_delegate;
    }

    static final class IncludedColumnsProvider implements ColumnChoicesProvider {

        private Supplier<String> m_targetColumnSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            initializer.computeOnValueChange(TargetColumnRef.class);
            m_targetColumnSupplier = initializer.getValueSupplier(TargetColumnRef.class);
        }

        @Override
        public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
            String classCol = m_targetColumnSupplier.get();
            if (classCol == null || classCol.isEmpty()) {
                return List.of();
            }

            final var specOpt = context.getInTableSpec(0);
            if (specOpt.isEmpty()) {
                return List.of();
            }

            return specOpt.get().stream().filter(col -> !col.getName().equals(classCol))
                    .filter(IncludedColumnsProvider::isIncluded).toList();
        }

        private static boolean isIncluded(final DataColumnSpec col) {
            return hasCompatibleType(col, List.of(DoubleValue.class));
        }

        private static boolean hasCompatibleType(final DataColumnSpec col,
            final Collection<Class<? extends DataValue>> valueClasses) {
            return valueClasses.stream().anyMatch(valueClass -> col.getType().isCompatible(valueClass));
        }

    }

}
