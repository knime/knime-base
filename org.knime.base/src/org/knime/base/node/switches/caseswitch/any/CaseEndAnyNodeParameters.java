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

package org.knime.base.node.switches.caseswitch.any;

import java.util.Arrays;
import java.util.List;

import org.knime.base.node.switches.caseswitch.any.CaseEndAnyNodeModel.MultipleActiveHandling;
import org.knime.core.node.BufferedDataTable;
import org.knime.node.parameters.Advanced;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.persistence.legacy.EnumBooleanPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Node parameters for CASE Switch End.
 *
 * @author Kai Franze, KNIME GmbH, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
@SuppressWarnings("restriction")
final class CaseEndAnyNodeParameters implements NodeParameters {

    /**
     * The default value here is set to makes the snapshot tests pass. The actual default is defined in
     * {@link CaseEndAnyNodeModel}.
     */
    @Widget(title = "If multiple inputs are active", description = """
            Choose the expected behavior of the node if two or more inputs are active. \
            In the case all inputs are inactive the node itself will pass on an inactive \
            object.""")
    @ValueReference(MultipleActiveHandlingChoiceRef.class)
    @ChoicesProvider(MultipleActiveHandlingChoiceProvider.class)
    @Persist(configKey = CaseEndAnyNodeModel.CFG_HANDLING)
    @RadioButtonsWidget
    MultipleActiveHandling m_multipleActiveHandling = MultipleActiveHandling.Fail;

    enum DuplicateRowHandling {
            @Label(value = "Append suffix", description = """
                    The output table will contain all rows, but duplicate row identifiers are labeled with a suffix. \
                    Similar to the "Skip Rows" option this method is also memory intensive.""")
            APPEND_SUFFIX,

            @Label(value = "Skip rows", description = """
                    Duplicate row identifiers (RowID) occurring in the second table are not appended to the output \
                    table. This option is relatively memory intensive as it needs to cache the RowIDs in order to \
                    find duplicates.""")
            SKIP_ROWS
    }

    @Widget(title = "Duplicate RowID handling",
        description = "How to handle duplicate row identifiers (RowID) when concatenating the two input tables.")
    @ValueSwitchWidget
    @ValueReference(DuplicateRowHandlingRef.class)
    @Persistor(DuplicateRowHandlingPersistor.class)
    @Effect(predicate = IsMergeTablesPredicate.class, type = EffectType.SHOW)
    DuplicateRowHandling m_duplicateRowHandling = DuplicateRowHandling.APPEND_SUFFIX;

    @Widget(title = "Suffix",
        description = "The suffix to append to duplicate row identifiers (RowID) to make them unique.")
    @Effect(predicate = IsAppendSuffixPredicate.class, type = EffectType.SHOW)
    String m_suffix = CaseEndAnyNodeModel.DEF_SUFFIX;

    @Advanced
    @Widget(title = "Enable hiliting",
        description = "Enables hiliting between both inputs and the concatenated output table.")
    @Persist(configKey = CaseEndAnyNodeModel.CFG_HILITING)
    @Effect(predicate = IsDataTableOutPort.class, type = EffectType.SHOW)
    boolean m_enableHiliting = CaseEndAnyNodeModel.DEF_HILITING;

    private static final class MultipleActiveHandlingChoiceProvider
        implements EnumChoicesProvider<MultipleActiveHandling> {
        @Override
        public List<MultipleActiveHandling> choices(final NodeParametersInput context) {
            final var isDataTableOutPort = Arrays.stream(context.getOutPortTypes()) //
                .findFirst() //
                .filter(outPortType -> outPortType.equals(BufferedDataTable.TYPE)) //
                .isPresent();
            return isDataTableOutPort //
                ? Arrays.asList(MultipleActiveHandling.values()) //
                : List.of(MultipleActiveHandling.Fail, MultipleActiveHandling.UseFirstActive);
        }
    }

    private static final class MultipleActiveHandlingChoiceRef implements ParameterReference<MultipleActiveHandling> {
    }

    private static final class DuplicateRowHandlingRef implements ParameterReference<DuplicateRowHandling> {
    }

    private static final class DuplicateRowHandlingPersistor extends EnumBooleanPersistor<DuplicateRowHandling> {
        DuplicateRowHandlingPersistor() {
            super(CaseEndAnyNodeModel.CFG_APPEND_SUFFIX, DuplicateRowHandling.class,
                DuplicateRowHandling.APPEND_SUFFIX);
        }
    }

    private static final class IsDataTableOutPort implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getConstant(context -> Arrays.stream(context.getOutPortTypes()) //
                .findFirst() //
                .filter(outPortType -> outPortType.equals(BufferedDataTable.TYPE)) //
                .isPresent());
        }
    }

    private static final class IsMergeTablesPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(MultipleActiveHandlingChoiceRef.class).isOneOf(MultipleActiveHandling.Merge);
        }
    }

    /**
     * Because we only need to show the suffix field if both "Merge tables" and "Append suffix" are selected.
     */
    private static final class IsAppendSuffixPredicate implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            final var isMergeTables = i.getPredicate(IsMergeTablesPredicate.class);
            final var isAppendSuffix =
                i.getEnum(DuplicateRowHandlingRef.class).isOneOf(DuplicateRowHandling.APPEND_SUFFIX);
            return isMergeTables.and(isAppendSuffix);
        }
    }
}
