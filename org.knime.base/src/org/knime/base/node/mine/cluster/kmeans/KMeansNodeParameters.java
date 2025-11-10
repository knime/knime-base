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

package org.knime.base.node.mine.cluster.kmeans;

import org.knime.core.webui.node.dialog.defaultdialog.internal.button.SimpleButtonWidget;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
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
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider.DoubleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;

/**
 * Node parameters for k-Means.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 */
@SuppressWarnings("restriction")
final class KMeansNodeParameters implements NodeParameters {

    /**
     * Constructor for the k-Means node parameters.
     */
    KMeansNodeParameters() {
    }

    /**
     * Constructor with context for initializing default values based on input table.
     *
     * @param context the node parameters input context
     */
    KMeansNodeParameters(final NodeParametersInput context) {
        this();
        m_columnFilter =
            new ColumnFilter(ColumnSelectionUtil.getDoubleColumnsOfFirstPort(context)).withIncludeUnknownColumns();
    }

    @Widget(title = "Column selection", description = "The numerical columns to be used for clustering.")
    @ChoicesProvider(DoubleColumnsProvider.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Number of clusters", description = "The number of clusters (cluster centers) to be created.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_nrOfClusters = 3;

    @Widget(title = "Centroid initialization",
        description = "Determine how the initial centroids (cluster centers) are chosen.")
    @ValueSwitchWidget
    @ValueReference(CentroidInitializationModeRef.class)
    CentroidInitialization m_centroidInitialization = CentroidInitialization.RANDOM_INITIALIZATION;

    @Widget(title = "Use static random seed",
        description = "When checked, uses the provided static seed for "
            + "reproducible random centroid initialization.")
    @Effect(predicate = IsRandomInitialization.class, type = EffectType.SHOW)
    @ValueReference(IsUseStaticRandomSeedRef.class)
    boolean m_useStaticRandomSeed = true;

    @Widget(title = "Seed value", description = "The seed value for random centroid initialization.")
    @TextInputWidget(patternValidation = LongAsStringPersistor.IsLongInteger.class)
    @Effect(predicate = IsRandomInitializationAndUseStaticRandomSeed.class, type = EffectType.SHOW)
    @ValueProvider(NewRandomSeedProvider.class)
    String m_seedValue = "0";

    @Widget(title = "New", description = "Generate a new random seed value.")
    @SimpleButtonWidget(ref = NewRandomSeedButtonRef.class)
    @Effect(predicate = IsRandomInitializationAndUseStaticRandomSeed.class, type = EffectType.SHOW)
    Void m_newRandomSeedValue;

    @Widget(title = "Max number of iterations",
        description = "The maximum number of iterations after which the algorithm "
            + "terminates if it hasn't found a stable solution before.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_maxIterations = 99;

    @Advanced
    @Widget(title = "Enable Hilite Mapping",
        description = "If enabled, the hiliting of a cluster row (2nd output) will "
            + "hilite all rows of this cluster in the input table and the 1st output table. "
            + "Depending on the number of rows, enabling this feature might consume a lot of memory.")
    boolean m_enableHilite;

    static final class CentroidInitializationModeRef implements ParameterReference<CentroidInitialization> {
    }

    static final class IsUseStaticRandomSeedRef implements ParameterReference<Boolean> {
    }

    static final class NewRandomSeedButtonRef implements ButtonReference {
    }

    static final class IsFirstKRowsInitialization implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CentroidInitializationModeRef.class).isOneOf(CentroidInitialization.FIRST_ROWS);
        }

    }

    static final class IsRandomInitialization implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CentroidInitializationModeRef.class).isOneOf(CentroidInitialization.RANDOM_INITIALIZATION);
        }

    }

    static final class IsUseStaticRandomSeed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(IsUseStaticRandomSeedRef.class).isTrue();
        }

    }

    static final class IsRandomInitializationAndUseStaticRandomSeed implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CentroidInitializationModeRef.class).isOneOf(CentroidInitialization.RANDOM_INITIALIZATION)
                .and(i.getBoolean(IsUseStaticRandomSeedRef.class).isTrue());
        }

    }

    static final class NewRandomSeedProvider implements StateProvider<String> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(NewRandomSeedButtonRef.class);
        }

        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            final long l1 = Double.doubleToLongBits(Math.random());
            final long l2 = Double.doubleToLongBits(Math.random());
            final var seed = ((0xFFFFFFFFL & l1) << 32) + (0xFFFFFFFFL & l2);
            return Long.toString(seed);
        }

    }

    enum CentroidInitialization {

            /**
             * No recursion policy.
             */
            @Label(value = "Random initialization",
                description = "Initializes the centroids with random rows of the input table. Checking the Use static random seed it is possible to get reproducible results.")
            RANDOM_INITIALIZATION,
            /**
             * No recursion policy.
             */
            @Label(value = "First k rows",
                description = "Initializes the centroids using the first rows of the input table.")
            FIRST_ROWS;

    }

}
