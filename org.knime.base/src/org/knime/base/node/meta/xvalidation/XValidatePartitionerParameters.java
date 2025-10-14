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

package org.knime.base.node.meta.xvalidation;

import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.LongAsStringPersistor;
import org.knime.node.parameters.updates.ButtonReference;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for X-Partitioner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.1
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class XValidatePartitionerParameters implements NodeParameters {

    /**
     * Default constructor.
     */
    XValidatePartitionerParameters() {
    }

    /**
     * Determines a class column from the context if possible.
     *
     * @param context to retrieve the data table spec from
     */
    XValidatePartitionerParameters(final NodeParametersInput context) {
        final var firstCol = context.getInTableSpec(0).stream().flatMap(DataTableSpec::stream).findFirst();
        if (firstCol.isPresent()) {
            m_classColumn = firstCol.get().getName();
        }
    }

    @Persistor(value = ShortPersistor.class)
    @Widget(title = "Number of validations", description = """
            The number of cross validation iterations that should be performed.
            """)
    @NumberInputWidget(minValidation = MinNumberOfValidationIterations.class,
        maxValidation = MaxNumberOfValidationIterations.class)
    @Effect(predicate = IsLeaveOneOutCrossValidation.class, type = EffectType.DISABLE)
    int m_validations = 10;

    @Persistor(SamplingMethodPersistor.class)
    @Widget(title = "Sampling method", description = "Choose the sampling method for partitioning the data:")
    @RadioButtonsWidget
    @ValueReference(SamplingMethodRef.class)
    SamplingMethod m_samplingMethod = SamplingMethod.RANDOM;

    @Persist(configKey = XValidateSettings.CFG_CLASS_COLUMN)
    @Widget(title = "Class column", description = """
            The name of the column with the class labels for stratified sampling.
            """)
    @Effect(predicate = IsStratifiedSampling.class, type = EffectType.SHOW)
    @ChoicesProvider(value = AllColumnsProvider.class)
    String m_classColumn;

    @Widget(title = "Use random seed", description = """
            For random and stratified sampling you can choose a seed for the random number generator in order
            to get reproducible results. Otherwise you get different partitions every time.
            """)
    @ValueReference(UseRandomSeedRef.class)
    @Effect(predicate = IsNotLinearSampling.class, type = EffectType.SHOW)
    @Persist(configKey = XValidateSettings.CFG_USE_RANDOM_SEED)
    boolean m_useRandomSeed;

    @Persistor(value = SeedPersistor.class)
    @Widget(title = "Random seed", description = """
            The seed value for the random number generator.
            """)
    @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
    @Effect(predicate = IsNotLinearSamplingAndUseRandomSeed.class, type = EffectType.SHOW)
    @ValueProvider(SeedValueProvider.class)
    String m_randomSeed = "0";

    @Widget(title = "Draw seed",
        description = "Generate a random seed and set it in the Random seed input above for reproducible runs.")
    @SimpleButtonWidget(ref = DrawSeedButtonRef.class)
    @Effect(predicate = IsNotLinearSamplingAndUseRandomSeed.class, type = EffectType.SHOW)
    Void m_drawSeed;

    // ====== Value References ======

    static final class SamplingMethodRef implements ParameterReference<SamplingMethod> {
    }

    static final class UseRandomSeedRef implements ParameterReference<Boolean> {
    }

    static final class DrawSeedButtonRef implements ButtonReference {
    }

    // ====== Effect Predicates ======

    static final class IsLinearSampling implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingMethodRef.class).isOneOf(SamplingMethod.LINEAR);
        }
    }

    static final class IsNotLinearSampling implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return not(i.getPredicate(IsLinearSampling.class));
        }
    }

    static final class IsNotLinearSamplingAndUseRandomSeed implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(UseRandomSeedRef.class).isTrue().and(i.getPredicate(IsNotLinearSampling.class));
        }
    }

    static final class IsStratifiedSampling implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingMethodRef.class).isOneOf(SamplingMethod.STRATIFIED);
        }
    }

    static final class IsLeaveOneOutCrossValidation implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(SamplingMethodRef.class).isOneOf(SamplingMethod.LEAVE_ONE_OUT);
        }
    }

    // ====== Validation Classes ======

    static final class MinNumberOfValidationIterations extends MinValidation {
        @Override
        public double getMin() {
            return 2;
        }
    }

    static final class MaxNumberOfValidationIterations extends MaxValidation {
        @Override
        public double getMax() {
            return 100;
        }
    }

    // ====== Custom Persistors ======

    static final class SamplingMethodPersistor implements NodeParametersPersistor<SamplingMethod> {

        @Override
        public SamplingMethod load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final boolean randomSampling = settings.getBoolean(XValidateSettings.CFG_RANDOM_SAMPLING, true);
            final boolean stratifiedSampling = settings.getBoolean(XValidateSettings.CFG_STRATIFIED_SAMPLING, false);
            final boolean leaveOneOutSampling = settings.getBoolean(XValidateSettings.CFG_LEAVE_ONE_OUT, false);

            if (randomSampling) {
                return SamplingMethod.RANDOM;
            } else if (stratifiedSampling) {
                return SamplingMethod.STRATIFIED;
            } else if (leaveOneOutSampling) {
                return SamplingMethod.LEAVE_ONE_OUT;
            } else {
                return SamplingMethod.LINEAR;
            }
        }

        @Override
        public void save(final SamplingMethod obj, final NodeSettingsWO settings) {
            final boolean randomSampling = (obj == SamplingMethod.RANDOM);
            final boolean stratifiedSampling = (obj == SamplingMethod.STRATIFIED);
            final boolean leaveOneOutSampling = (obj == SamplingMethod.LEAVE_ONE_OUT);

            settings.addBoolean(XValidateSettings.CFG_RANDOM_SAMPLING, randomSampling);
            settings.addBoolean(XValidateSettings.CFG_STRATIFIED_SAMPLING, stratifiedSampling);
            settings.addBoolean(XValidateSettings.CFG_LEAVE_ONE_OUT, leaveOneOutSampling);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{
                {XValidateSettings.CFG_RANDOM_SAMPLING},
                {XValidateSettings.CFG_STRATIFIED_SAMPLING},
                {XValidateSettings.CFG_LEAVE_ONE_OUT}
            };
        }
    }

    static final class ShortPersistor implements NodeParametersPersistor<Integer> {

        @Override
        public Integer load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return (int)settings.getShort(XValidateSettings.CFG_VALIDATIONS);
        }

        @Override
        public void save(final Integer param, final NodeSettingsWO settings) {
            settings.addShort(XValidateSettings.CFG_VALIDATIONS, param.shortValue());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{XValidateSettings.CFG_VALIDATIONS}};
        }

    }

    static final class SeedPersistor extends LongAsStringPersistor {

        SeedPersistor() {
            super(XValidateSettings.CFG_RANDOM_SEED);
        }

    }

    static final class SeedValueProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(DrawSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            final long l1 = Double.doubleToLongBits(Math.random());
            final long l2 = Double.doubleToLongBits(Math.random());
            long l = ((0xFFFFFFFFL & l1) << 32) + (0xFFFFFFFFL & l2);
            return Long.toString(l);
        }
    }

    enum SamplingMethod {
        @Label(value = "Linear sampling", description = "The input table is cut into consecutive pieces.")
        LINEAR,
        @Label(value = "Random sampling", description = "The partitions are sampled randomly from the input table.")
        RANDOM,
        @Label(value = "Stratified sampling", description = """
                The partitions are sampled randomly but the class distribution
                from the column selected below is maintained.
                """)
        STRATIFIED,
        @Label(value = "Leave-one-out", description = """
                Performs a leave-one-out cross validation, i.e. there are as many iterations as data points and
                in each iteration another point's target value is predicted by using all remaining points as
                training set.
                """)
        LEAVE_ONE_OUT
    }

}
