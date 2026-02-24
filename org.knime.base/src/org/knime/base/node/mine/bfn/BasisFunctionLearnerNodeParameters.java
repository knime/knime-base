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
 *   Feb 24, 2026 (magnus): created
 */
package org.knime.base.node.mine.bfn;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerTable.MissingValueReplacementFunction;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.ConfigMigration;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.migration.Migration;
import org.knime.node.parameters.migration.NodeParametersMigration;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;
import org.knime.node.parameters.widget.choices.filter.TwinlistWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * {@link NodeParameters} for the basis function learner nodes.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @since 5.11
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
public abstract class BasisFunctionLearnerNodeParameters implements NodeParameters {

    @Widget(title = "Missing values", description = """
            Select one method to handle missing values <br/>
            <ul>
                <li>
                    <b>Incorp</b>
                    May generate fuzzy rules with missing values, if no replacement value has been found during the
                    learning process.
                </li>
                <li>
                    <b>Best Guess</b>
                    Computes the optimal replacement value by projecting the fuzzy rule (with missing value(s)) onto the
                    missing dimension(s) of all other rules.
                </li>
                <li>
                    <b>Mean</b>
                    Replaces the missing value(s) with the column mean.
                </li>
                <li>
                    <b>Min</b>
                    Replaces the missing value(s) with the column min.
                </li>
                <li>
                    <b>Max</b>
                    Replaces the missing value(s) with the column max.
                </li>
                <li>
                    <b>Zero</b>
                    Replaces the missing value(s) with zero.
                </li>
                <li>
                    <b>One</b>
                    Replaces the missing value(s) with one.
                </li>
            </ul>
            """)
    @ChoicesProvider(MissingValueChoicesProvider.class)
    @Persistor(MissingValuePersistor.class)
    @ValueProvider(MissingValueProvider.class)
    @ValueReference(MissingValueRef.class)
    String m_missing;

    static final class MissingValueRef implements ParameterReference<String> {
    }

    @Widget(title = "Shrink after commit", description = """
            If selected, a shrink to reduce conflicting rules is executed
            immediately after a new rule is committed, i.e. the new rule is reduced so that conflicts with
            all other rules of different classes are avoided.
            """)
    @Persist(configKey = BasisFunctionLearnerNodeModel.SHRINK_AFTER_COMMIT)
    boolean m_shrinkAfterCommit;

    @Widget(title = "Use class with max coverage", description = """
            If selected, only the class with maximum coverage degree of the target columns is used during
            training, otherwise all class columns are considered for coverage.
            """)
    @Persist(configKey = BasisFunctionLearnerNodeModel.MAX_CLASS_COVERAGE)
    boolean m_maxClassCoverage = true;

    @Persistor(MaxEpochsParametersPersistor.class)
    MaxEpochsParameters m_maxEpochsParameters = new MaxEpochsParameters();

    static class MaxEpochsParameters implements NodeParameters {

        @Widget(title = "Use maximum number of epochs", description = """
                If selected, the option defines the maximum number of epochs the algorithm has to process the
                entire data set, otherwise it repeats this process until this rule model is stable, i.e. no new
                rule has been committed and/or no shrink is executed.
                """)
        @ValueReference(UseMaxEpochs.class)
        boolean m_useMaxEpochs;

        static final class UseMaxEpochs implements BooleanReference {
        }

        @Widget(title = "Maximum number of epochs", description = """
                The maximum number of epochs the algorithm has to process the entire data set.
                """)
        @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
        @Effect(predicate = UseMaxEpochs.class, type = EffectType.SHOW)
        int m_maxEpochs = 42;

    }

    @Widget(title = "Target columns", description = """
            Select the target(s) to be used for classification. If more than one column (only numeric) is
            selected, the columns must contain class membership values between 0 and 1 for each class given
            by the column name.
            """)
    @TwinlistWidget
    @ChoicesProvider(AllColumnsProvider.class)
    @Persist(configKey = BasisFunctionLearnerNodeModel.TARGET_COLUMNS)
    @ValueProvider(TargetColumnsProvider.class)
    @ValueReference(TargetColumnsRef.class)
    @Migration(SingleTargetColumnNameMigration.class)
    String[] m_targetColumns = new String[0];

    static final class TargetColumnsRef implements ParameterReference<String[]> {
    }

    static final class SingleTargetColumnNameMigration implements NodeParametersMigration<String[]> {

        static final String LEGACY_TARGET_COLUMN = UUID.randomUUID().toString();

        static String[] loadTargetColumn(final NodeSettingsRO settings) throws InvalidSettingsException {
            return new String[]{settings.getString(BasisFunctionLearnerNodeModel.TARGET_COLUMNS)};
        }

        @Override
        public List<ConfigMigration<String[]>> getConfigMigrations() {
            return List.of(ConfigMigration.builder(SingleTargetColumnNameMigration::loadTargetColumn)
                .withMatcher(settings-> settings.containsKey(BasisFunctionLearnerNodeModel.TARGET_COLUMNS)
                    && !settings.getString(BasisFunctionLearnerNodeModel.TARGET_COLUMNS, LEGACY_TARGET_COLUMN)
                        .equals(LEGACY_TARGET_COLUMN))
                .build());
        }

    }

    // Invisible parameter still persisted for backward compatibility
    @Persist(configKey = BasisFunctionLearnerNodeModel.DISTANCE)
    int m_distance;

    static final class MissingValueProvider implements StateProvider<String> {

        Supplier<String> m_missingValueSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_missingValueSupplier = initializer.getValueSupplier(MissingValueRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (m_missingValueSupplier.get() == null || m_missingValueSupplier.get().isEmpty()) {
                return BasisFunctionLearnerTable.MISSINGS[0].toString();
            }
            throw new StateComputationFailureException();
        }

    }

    static final class MissingValueChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return Arrays.stream(BasisFunctionLearnerTable.MISSINGS)
                    .map(MissingValueReplacementFunction::toString).toList();
        }

    }

    static final class TargetColumnsProvider implements StateProvider<String[]> {

        Supplier<String[]> m_targetColumnsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_targetColumnsSupplier = initializer.getValueSupplier(TargetColumnsRef.class);
        }

        @Override
        public String[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            final var compatibleColumns = ColumnSelectionUtil.getAllColumnsOfFirstPort(parametersInput);
            final var targetColumns = m_targetColumnsSupplier.get();
            if (targetColumns == null || targetColumns.length == 0 && !compatibleColumns.isEmpty()) {
                return new String[]{compatibleColumns.get(compatibleColumns.size() - 1).getName()};
            }
            throw new StateComputationFailureException();
        }

    }

    static final class MaxEpochsParametersPersistor implements NodeParametersPersistor<MaxEpochsParameters> {

        @Override
        public MaxEpochsParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            MaxEpochsParameters params = new MaxEpochsParameters();
            int maxEpochs = settings.getInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, -1);
            if (maxEpochs > 0) {
                params.m_useMaxEpochs = true;
                params.m_maxEpochs = maxEpochs;
            } else {
                params.m_useMaxEpochs = false;
                params.m_maxEpochs = 42;
            }
            return params;
        }

        @Override
        public void save(final MaxEpochsParameters param, final NodeSettingsWO settings) {
            if (param.m_useMaxEpochs) {
                settings.addInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, param.m_maxEpochs);
            } else {
                settings.addInt(BasisFunctionLearnerNodeModel.MAX_EPOCHS, -1);
            }
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BasisFunctionLearnerNodeModel.MAX_EPOCHS}};
        }
    }

    static final class MissingValuePersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var missingIndex = settings.getInt(BasisFunctionLearnerTable.MISSING, 0);
            if (missingIndex < 0 || missingIndex >= BasisFunctionLearnerTable.MISSINGS.length) {
                throw new InvalidSettingsException("Invalid missing index: " + missingIndex +
                    ". Valid range is [0, " + (BasisFunctionLearnerTable.MISSINGS.length - 1) + "].");
            }
            return BasisFunctionLearnerTable.MISSINGS[missingIndex].toString();
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            final var missingChoices = Arrays.stream(BasisFunctionLearnerTable.MISSINGS)
                    .map(MissingValueReplacementFunction::toString).toList();
            settings.addInt(BasisFunctionLearnerTable.MISSING, missingChoices.indexOf(param));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{BasisFunctionLearnerTable.MISSING}};
        }

    }

}
