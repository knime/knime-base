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

package org.knime.base.node.mine.bfn.fuzzy;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeParameters;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * Node parameters for Fuzzy Rule Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
class FuzzyBasisFunctionLearnerNodeParameters extends BasisFunctionLearnerNodeParameters {

    @Widget(title = "Fuzzy norm", description = """
            Select a fuzzy norm to compute the rules' activation across all dimensions and
            rules. Fuzzy norms are important, because they combine the membership values of each fuzzy
            interval for one rule and compute a final output across all rules. <br/>
            <ul>
                <li>
                    <b>Min/Max Norm</b>
                </li>
                <li>
                    <b>Product Norm</b>
                </li>
                <li>
                    <b>Lukasiewicz Norm</b>
                </li>
                <li>
                    <b>Yager[2.0] Norm</b>
                </li>
            </ul>
            """)
    @ChoicesProvider(FuzzyNormChoicesProvider.class)
    @Persistor(FuzzyNormPersistor.class)
    @ValueProvider(FuzzyNormProvider.class)
    @ValueReference(FuzzyNormRef.class)
    String m_norm;

    static final class FuzzyNormRef implements ParameterReference<String> {
    }

    @Widget(title = "Shrink function", description = """
            Select a shrink method to reduce rules in order to avoid conflicts between rules of different classes.<br/>
            <ul>
                <li>
                    <b>VolumnBorderBased</b>
                    Applies the volume loss in terms of the support or core region borders.
                </li>
                <li>
                    <b>VolumnAnchorBased</b>
                    Uses the anchor value border.
                </li>
                <li>
                    <b>VolumnRuleBased</b>
                    Uses the entire rule volume interval for one rule and compute a final output across all rules.
                </li>
            </ul>
            """)
    @ChoicesProvider(ShrinkFunctionChoicesProvider.class)
    @Persistor(ShrinkFunctionPersistor.class)
    @ValueProvider(ShrinkFunctionProvider.class)
    @ValueReference(ShrinkFunctionRef.class)
    String m_shrink;

    static final class ShrinkFunctionRef implements ParameterReference<String> {
    }

    static final class FuzzyNormProvider implements StateProvider<String> {

        Supplier<String> m_normSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_normSupplier = initializer.getValueSupplier(FuzzyNormRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (m_normSupplier.get() == null || m_normSupplier.get().isEmpty()) {
                return Norm.NORMS[0].toString();
            }
            throw new StateComputationFailureException();
        }

    }

    static final class ShrinkFunctionProvider implements StateProvider<String> {

        Supplier<String> m_shrinkSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_shrinkSupplier = initializer.getValueSupplier(ShrinkFunctionRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            if (m_shrinkSupplier.get() == null || m_shrinkSupplier.get().isEmpty()) {
                return Shrink.SHRINKS[0].toString();
            }
            throw new StateComputationFailureException();
        }

    }

    static final class FuzzyNormChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return Arrays.stream(Norm.NORMS).map(Norm::toString).toList();
        }

    }

    static final class ShrinkFunctionChoicesProvider implements StringChoicesProvider {

        @Override
        public List<String> choices(final NodeParametersInput context) {
            return Arrays.stream(Shrink.SHRINKS).map(Shrink::toString).toList();
        }

    }

    static final class FuzzyNormPersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var normIndex = settings.getInt(Norm.NORM_KEY, 0);
            if (normIndex < 0 || normIndex >= Norm.NORMS.length) {
                throw new InvalidSettingsException("Invalid norm index: " + normIndex +
                    ". Valid range is [0, " + (Norm.NORMS.length - 1) + "].");
            }
            return Norm.NORMS[normIndex].toString();
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            final var normChoices = Arrays.stream(Norm.NORMS).map(Norm::toString).toList();
            settings.addInt(Norm.NORM_KEY, normChoices.indexOf(param));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{Norm.NORM_KEY}};
        }

    }

    static final class ShrinkFunctionPersistor implements NodeParametersPersistor<String> {

        @Override
        public String load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var shrinkIndex = settings.getInt(Shrink.SHRINK_KEY, 0);
            if (shrinkIndex < 0 || shrinkIndex >= Shrink.SHRINKS.length) {
                throw new InvalidSettingsException("Invalid shrink index: " + shrinkIndex +
                    ". Valid range is [0, " + (Shrink.SHRINKS.length - 1) + "].");
            }
            return Shrink.SHRINKS[shrinkIndex].toString();
        }

        @Override
        public void save(final String param, final NodeSettingsWO settings) {
            final var shrinkChoices = Arrays.stream(Shrink.SHRINKS).map(Shrink::toString).toList();
            settings.addInt(Shrink.SHRINK_KEY, shrinkChoices.indexOf(param));
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{Shrink.SHRINK_KEY}};
        }

    }

}