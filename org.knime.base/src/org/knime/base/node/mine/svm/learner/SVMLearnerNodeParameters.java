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

package org.knime.base.node.mine.svm.learner;

import java.util.Optional;

import org.knime.base.node.mine.svm.kernel.KernelFactory.KernelType;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.StringValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.StringColumnsProvider;

/**
 * Node parameters for SVM Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class SVMLearnerNodeParameters implements NodeParameters {

    @Persist(configKey = SVMLearnerNodeModel.CFG_CLASSCOL)
    @Widget(title = "Class column", description = """
            Choose the column that contains the nominal target variable.
            """)
    @ChoicesProvider(StringColumnsProvider.class)
    @ValueProvider(ClossColumnProvider.class)
    @ValueReference(ClassColumnRef.class)
    String m_classColumn;

    static final class ClassColumnRef implements ParameterReference<String> {
    }

    @Persist(configKey = SVMLearnerNodeModel.CFG_PARAMC)
    @Widget(title = "Overlapping penalty", description = """
            The overlapping penalty is useful in the case that the input data is not separable.
            It determines how much penalty is assigned to each point that is misclassified.
            A good value for it is 1.
            """)
    double m_paramC = SVMLearnerNodeModel.DEFAULT_PARAMC;

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELTYPE)
    @Widget(title = "Kernel type", description = """
            Choose the kernel function for the support vector machine. Each kernel has its own
            parameters which can be configured below.
            """)
    @ValueSwitchWidget
    @ValueReference(KernelTypeRef.class)
    KernelType m_kernelType = KernelType.HyperTangent;

    static final class KernelTypeRef implements ParameterReference<KernelType> {
    }

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELPARAM + "_Bias")
    @Widget(title = "Bias", description = """
            The bias parameter for the polynomial kernel.
            """)
    @Effect(predicate = IsPolynomialKernel.class, type = EffectType.SHOW)
    double m_bias = 1.0;

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELPARAM + "_Power")
    @Widget(title = "Power", description = """
            The power parameter for the polynomial kernel.
            """)
    @Effect(predicate = IsPolynomialKernel.class, type = EffectType.SHOW)
    double m_power = 1.0;

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELPARAM + "_Gamma")
    @Widget(title = "Gamma", description = """
            The gamma parameter for the polynomial kernel.
            """)
    @Effect(predicate = IsPolynomialKernel.class, type = EffectType.SHOW)
    double m_gamma = 1.0;

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELPARAM + "_kappa")
    @Widget(title = "Kappa", description = """
            The kappa parameter for the hyperbolic tangent kernel.
            """)
    @Effect(predicate = IsHyperTangentKernel.class, type = EffectType.SHOW)
    double m_kappa = 0.1;

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELPARAM + "_delta")
    @Widget(title = "Delta", description = """
            The delta parameter for the hyperbolic tangent kernel
            """)
    @Effect(predicate = IsHyperTangentKernel.class, type = EffectType.SHOW)
    double m_delta = 0.5;

    @Persist(configKey = SVMLearnerNodeModel.CFG_KERNELPARAM + "_sigma")
    @Widget(title = "Sigma", description = """
            The sigma parameter for the Radial Basis Function (RBF) kernel.
            """)
    @Effect(predicate = IsRBFKernel.class, type = EffectType.SHOW)
    double m_sigma = 0.1;

    static final class IsPolynomialKernel implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(KernelTypeRef.class).isOneOf(KernelType.Polynomial);
        }

    }

    static final class IsHyperTangentKernel implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(KernelTypeRef.class).isOneOf(KernelType.HyperTangent);
        }

    }

    static final class IsRBFKernel implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(KernelTypeRef.class).isOneOf(KernelType.RBF);
        }

    }

    static final class ClossColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected ClossColumnProvider() {
            super(ClassColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            final var compatibleColumns =
                    ColumnSelectionUtil.getCompatibleColumnsOfFirstPort(parametersInput, StringValue.class);
            return compatibleColumns.isEmpty() ? Optional.empty() :
                Optional.of(compatibleColumns.get(compatibleColumns.size() - 1));
        }

    }

}
