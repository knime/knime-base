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
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
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
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for k-Means.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.0
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
class ClusterNodeSettings implements NodeParameters {

    /**
     * Constructor for the k-Means node parameters.
     */
    ClusterNodeSettings() {
    }

    /**
     * Constructor with context for initializing default values based on input table.
     *
     * @param context the node parameters input context
     */
    ClusterNodeSettings(final NodeParametersInput context) {
        this();
        m_columnFilter = new ColumnFilter(ColumnSelectionUtil.getDoubleColumnsOfFirstPort(context))
                .withIncludeUnknownColumns();
    }

    @Section(title = "Clusters")
    interface ClustersSection {
    }

    @Section(title = "Number of Iterations")
    @After(ClustersSection.class)
    interface NumberOfIterationsSection {
    }

    @Section(title = "Column Selection")
    @After(NumberOfIterationsSection.class)
    interface ColumnSelectionSection {
    }

    @Section(title = "Hilite Mapping")
    @After(ColumnSelectionSection.class)
    interface HiliteMappingSection {
    }

    @Widget(title = "Number of clusters",
            description = "The number of clusters (cluster centers) to be created.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(ClustersSection.class)
    int m_nrOfClusters = ClusterNodeModel2.INITIAL_NR_CLUSTERS;

    @Widget(title = "Centroid initialization",
            description = "First k rows: Initializes the centroids using the first rows of the input table. "
                       + "Random initialization: Initializes the centroids with random rows of the input table. "
                       + "Checking the Use static random seed it is possible to get reproducible results.")
    @ValueSwitchWidget
    @ValueReference(CentroidInitializationModeRef.class)
    @Layout(ClustersSection.class)
    CentroidInitialization m_centroidInitialization = CentroidInitialization.RANDOM_INITIALIZATION;

    @Widget(title = "Use static random seed",
            description = "When checked, uses the provided static seed for "
                + "reproducible random centroid initialization.")
    @Layout(ClustersSection.class)
    @Effect(predicate = IsRandomInitialization.class, type = EffectType.SHOW)
    @ValueReference(IsUseStaticRandomSeedRef.class)
    boolean m_useStaticRandomSeed = true;

    @Widget(title = "Seed value",
            description = "The seed value for random centroid initialization.")
    @NumberInputWidget(minValidation = MinLongValueValidation.class, maxValidation = MaxLongValueValidation.class)
    @Layout(ClustersSection.class)
    @Effect(predicate = IsRandomInitializationAndUseStaticRandomSeed.class, type = EffectType.SHOW)
    @ValueProvider(NewRandomSeedProvider.class)
    long m_seedValue = 0L;

    @Widget(title = "New", description = "Generate a new random seed value.")
    @SimpleButtonWidget(ref = NewRandomSeedButtonRef.class)
    @Layout(ClustersSection.class)
    @Effect(predicate = IsRandomInitializationAndUseStaticRandomSeed.class, type = EffectType.SHOW)
    Void m_newRandomSeedValue;

    static final class NewRandomSeedButtonRef implements ButtonReference {
    }

    static final class NewRandomSeedProvider implements StateProvider<Long> {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeOnButtonClick(NewRandomSeedButtonRef.class);
        }

        @Override
        public Long computeState(final NodeParametersInput context) {
            final long l1 = Double.doubleToLongBits(Math.random());
            final long l2 = Double.doubleToLongBits(Math.random());
            return ((0xFFFFFFFFL & l1) << 32)
                + (0xFFFFFFFFL & l2);
        }

    }

    static final class MinLongValueValidation extends MinValidation {

        @Override
        public double getMin() {
            return Long.MIN_VALUE;
        }

    }

    static final class MaxLongValueValidation extends MaxValidation {

        @Override
        public double getMax() {
            return Long.MAX_VALUE;
        }

    }

    @Widget(title = "Max number of iterations",
            description = "The maximum number of iterations after which the algorithm "
                + "terminates if it hasn't found a stable solution before.")
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    @Layout(NumberOfIterationsSection.class)
    int m_maxIterations = ClusterNodeModel2.INITIAL_MAX_ITERATIONS;

    @Widget(title = "Column selection",
            description = "Move the numeric columns of interest to the \"Include\" list. "
                + "Always include all columns option moves all numeric columns to the \"Include\" list by default.")
    @ChoicesProvider(DoubleColumnsProvider.class)
    @Layout(ColumnSelectionSection.class)
    ColumnFilter m_columnFilter = new ColumnFilter();

    @Widget(title = "Enable Hilite Mapping",
            description = "If enabled, the hiliting of a cluster row (2nd output) will "
                + "hilite all rows of this cluster in the input table and the 1st output table. "
                + "Depending on the number of rows, enabling this feature might consume a lot of memory.")
    @Layout(HiliteMappingSection.class)
    boolean m_enableHilite = false;

    interface CentroidInitializationModeRef extends ParameterReference<CentroidInitialization> {
    }

    static class IsFirstKRowsInitialization implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CentroidInitializationModeRef.class).isOneOf(CentroidInitialization.FIRST_ROWS);
        }
    }

    static class IsRandomInitialization implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CentroidInitializationModeRef.class)
                    .isOneOf(CentroidInitialization.RANDOM_INITIALIZATION);
        }
    }

    interface IsUseStaticRandomSeedRef extends ParameterReference<Boolean> {
    }

    static class IsUseStaticRandomSeed implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getBoolean(IsUseStaticRandomSeedRef.class).isTrue();
        }
    }

    static class IsRandomInitializationAndUseStaticRandomSeed implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(CentroidInitializationModeRef.class)
                .isOneOf(CentroidInitialization.RANDOM_INITIALIZATION)
                .and(i.getBoolean(IsUseStaticRandomSeedRef.class).isTrue());
        }
    }

    enum CentroidInitialization {

        /**
         * No recursion policy.
         */
        @Label(value = "First k rows")
        FIRST_ROWS,

        /**
         * No recursion policy.
         */
        @Label(value = "Random initialization")
        RANDOM_INITIALIZATION;

    }

}
