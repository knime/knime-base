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

package org.knime.base.node.mine.subgroupminer;

import java.util.List;
import java.util.Optional;

import org.knime.base.node.mine.subgroupminer.apriori.AprioriAlgorithmFactory.AlgorithmDataStructure;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet;
import org.knime.base.node.mine.subgroupminer.freqitemset.FrequentItemSet.Type;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.collection.CollectionDataValue;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.updates.legacy.ColumnNameAutoGuessValueProvider;
import org.knime.node.parameters.updates.util.BooleanReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.choices.util.CompatibleColumnsProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MaxValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsPositiveIntegerValidation;

/**
 * Node parameters for Association Rule Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
final class SubgroupMinerFactory2Parameters implements NodeParameters {

    @Section(title = "Output")
    interface OutputSection {
    }

    @Widget(title = "Column containing transactions", description = """
            Select the column containing the transactions (BitVector or Collection) to mine for frequent itemsets or
            association rules. There must be at least one, since this is the only valid input for the subgroup miner.
            """)
    @Persist(configKey = SubgroupMinerModel2.CFG_TRANSACTION_COL)
    @ChoicesProvider(TransactionColumnChoicesProvider.class)
    @ValueProvider(TransactionColumnProvider.class)
    @ValueReference(TransactionColumnRef.class)
    String m_transactionColumn = "";

    static final class TransactionColumnRef implements ParameterReference<String> {
    }

    @Widget(title = "Minimum support", description = """
            An itemset is considered to be frequent if there are at least "minimum support" transactions, where the
            itemset occurs. Make sure to have here a meaningful number in proportion of the number of rows of the input.
            """)
    @Persist(configKey = SubgroupMinerModel2.CFG_MIN_SUPPORT)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = MaxOneValidation.class)
    double m_minSupport = SubgroupMinerModel2.DEFAULT_MIN_SUPPORT;

    @Persist(configKey = SubgroupMinerModel2.CFG_UNDERLYING_STRUCT)
    @Widget(title = "Underlying data structure", description = """
            Choose the underlying data structure
            """)
    @ValueSwitchWidget
    AlgorithmDataStructure m_underlyingStruct = AlgorithmDataStructure.ARRAY;

    @Layout(OutputSection.class)
    @Persist(configKey = SubgroupMinerModel2.CFG_ITEMSET_TYPE)
    @Widget(title = "Itemset type", description = "Choose the type of frequent itemsets to output.")
    @ValueSwitchWidget
    FrequentItemSet.Type m_itemSetType = Type.CLOSED;

    @Layout(OutputSection.class)
    @Widget(title = "Maximal itemset length", description = """
            The maximal length of the resulting itemsets. A lower value may reduce the runtime if there are very long
            frequent itemsets.
            """)
    @Persist(configKey = SubgroupMinerModel2.CFG_MAX_ITEMSET_LENGTH)
    @NumberInputWidget(minValidation = IsPositiveIntegerValidation.class)
    int m_maxItemsetLength = SubgroupMinerModel2.DEFAULT_MAX_ITEMSET_LENGTH;

    @Layout(OutputSection.class)
    @Widget(title = "Output association rules", description = """
            Check if association rules should be generated out of the frequent itemsets. Note: association rules are
            always generated from free frequent itemsets and are constrained to have only one item in the consequence.
            """)
    @Persist(configKey = SubgroupMinerModel2.CFG_ASSOCIATION_RULES)
    @ValueReference(AssociationRulesFlagRef.class)
    boolean m_associationRules;

    static final class AssociationRulesFlagRef implements BooleanReference {
    }

    @Layout(OutputSection.class)
    @Widget(title = "Minimum confidence", description = """
            The confidence is a measure for "how often the rule is right". Thus, how often, if the items in the
            antecedence appeared also the consequence occurred in the transactions.
            """)
    @Persist(configKey = SubgroupMinerModel2.CFG_CONFIDENCE)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class, maxValidation = MaxOneValidation.class)
    @Effect(predicate = AssociationRulesFlagRef.class, type = EffectType.SHOW)
    double m_confidence = SubgroupMinerModel2.DEFAULT_CONFIDENCE;

    static final class TransactionColumnProvider extends ColumnNameAutoGuessValueProvider {

        protected TransactionColumnProvider() {
            super(TransactionColumnRef.class);
        }

        @Override
        protected Optional<DataColumnSpec> autoGuessColumn(final NodeParametersInput parametersInput) {
            return ColumnSelectionUtil.getFirstCompatibleColumnOfFirstPort(
                parametersInput, BitVectorValue.class, CollectionDataValue.class);
        }

    }

    static final class TransactionColumnChoicesProvider extends CompatibleColumnsProvider {

        protected TransactionColumnChoicesProvider() {
            super(List.of(BitVectorValue.class, CollectionDataValue.class));
        }

    }

    static final class MaxOneValidation extends MaxValidation {

        @Override
        protected double getMax() {
            return 1;
        }

    }

}
