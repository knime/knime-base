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
 *   Nov 14, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.groupby.common;

import org.knime.base.node.preproc.groupby.Sections;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.internal.persistence.PersistArray;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;

/**
 * Parameters for data type based aggregations in GroupBy node.
 */
@SuppressWarnings("restriction")
@LoadDefaultsForAbsentFields
public final class DataTypeAggregationParameters implements NodeParameters {

    @Layout(Sections.TypeAggregation.class)
    @Widget(title = "Type Based Aggregation", description = """
            Aggregations are applied to all columns of the selected type.
            """)
    @ArrayWidget(addButtonText = "Add type")
    @PersistArray(LegacyDataTypeAggregatorsArrayPersistor.class)
    @ValueReference(DataTypeAggregationParameters.TypeAggregationsRef.class)
    DataTypeAggregatorElement[] m_dataTypeAggregators = new DataTypeAggregatorElement[0];

    static final class TypeAggregationsRef implements ParameterReference<DataTypeAggregatorElement[]> {
    }

    @Layout(Sections.TypeAggregation.class)
    @Effect(predicate = DataTypeAggregationParameters.HasTypeAggregations.class, type = EffectType.SHOW)
    @Widget(title = "Type matching", description = "Define how strict the type matching for the aggregations is.")
    @ValueSwitchWidget
    @Persistor(LegacyTypeMatchPersistor.class)
    TypeMatch m_typeMatch = TypeMatch.STRICT;

    static final class HasTypeAggregations implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            // neither TrueCondition, nor ConstantPredicate is exported
            return i.getArray(DataTypeAggregationParameters.TypeAggregationsRef.class)
                .containsElementSatisfying(el -> el.always());
        }
    }

    /**
     * Legacy persistor for TypeMatch in GroupBy node.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    static final class LegacyTypeMatchPersistor implements NodeParametersPersistor<TypeMatch> {
        private static final String CFG_TYPE_MATCH = "typeMatch";

        @Override
        public TypeMatch load(final NodeSettingsRO settings) throws InvalidSettingsException {
            if (settings.containsKey(CFG_TYPE_MATCH)) {
                final byte persistByte = settings.getByte(CFG_TYPE_MATCH);
                for (final TypeMatch strategy : TypeMatch.values()) {
                    if (persistByte == strategy.m_persistByte) {
                        return strategy;
                    }
                }
                throw new InvalidSettingsException("The selection type matching strategy could not be loaded.");

            } else {
                return TypeMatch.SUB_TYPE;
            }
        }

        @Override
        public void save(final TypeMatch obj, final NodeSettingsWO settings) {
            settings.addByte(CFG_TYPE_MATCH, obj.m_persistByte);
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{new String[]{CFG_TYPE_MATCH}};
        }
    }

    enum TypeMatch {

            @Label(value = "Strict",
                description = "The type based aggregation method is only applied to columns of the selected type.")
            STRICT((byte)0),

            @Label(value = "Include sub-types", description = """
                    The type based aggregation method is also applied to columns containing
                    sub-types of the selected type.
                    For example <i>Boolean</i> is a sub-type of
                    <i>Number (Integer)</i>, <i>Number (Integer)</i> of <i>Number (Long)</i>,
                    and <i>Number (Long)</i> of <i>Number (Float)</i>.
                        """)
            SUB_TYPE((byte)1);

        private final byte m_persistByte;

        TypeMatch(final byte persistByte) {
            m_persistByte = persistByte;
        }

    }

}
