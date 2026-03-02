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
import java.util.stream.Collectors;

import org.knime.base.node.mine.bfn.BasisFunctionLearnerNodeParameters;
import org.knime.base.node.mine.bfn.fuzzy.norm.Norm;
import org.knime.base.node.mine.bfn.fuzzy.shrink.Shrink;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.migration.LoadDefaultsForAbsentFields;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.Label;

/**
 * Node parameters for Fuzzy Rule Learner.
 *
 * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
 * @author AI Migration Pipeline v1.2
 */
@LoadDefaultsForAbsentFields
class FuzzyBasisFunctionLearnerNodeParameters extends BasisFunctionLearnerNodeParameters {

    @Widget(title = "Fuzzy norm", description = """
            Select a fuzzy norm to compute the rules' activation across all dimensions and
            rules. Fuzzy norms are important, because they combine the membership values of each fuzzy
            interval for one rule and compute a final output across all rules.
            """)
    @Persistor(FuzzyNormPersistor.class)
    NormChoice m_norm = NormChoice.MIN_MAX;

    @Widget(title = "Shrink function", description = """
            Select a shrink method to reduce rules in order to avoid conflicts between rules of different classes.
            """)
    @Persistor(ShrinkFunctionPersistor.class)
    ShrinkChoice m_shrink = ShrinkChoice.VOLUME_BORDER_BASED;

    static final class FuzzyNormPersistor implements NodeParametersPersistor<NormChoice> {

        @Override
        public NormChoice load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return NormChoice.getFromIndex(settings.getInt(Norm.NORM_KEY, NormChoice.MIN_MAX.getIndex()));
        }

        @Override
        public void save(final NormChoice param, final NodeSettingsWO settings) {
            settings.addInt(Norm.NORM_KEY, param.getIndex());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{Norm.NORM_KEY}};
        }

    }

    static final class ShrinkFunctionPersistor implements NodeParametersPersistor<ShrinkChoice> {

        @Override
        public ShrinkChoice load(final NodeSettingsRO settings) throws InvalidSettingsException {
            return ShrinkChoice.getFromIndex(
                settings.getInt(Shrink.SHRINK_KEY, ShrinkChoice.VOLUME_BORDER_BASED.getIndex()));
        }

        @Override
        public void save(final ShrinkChoice param, final NodeSettingsWO settings) {
            settings.addInt(Shrink.SHRINK_KEY, param.getIndex());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{Shrink.SHRINK_KEY}};
        }

    }

    enum NormChoice {

            @Label(value = "Min/Max Norm")
            MIN_MAX(Norm.NORMS[0].toString(), 0), //
            @Label(value = "Product Norm")
            PRODUCT(Norm.NORMS[1].toString(), 1), //
            @Label(value = "Lukasiewicz Norm")
            LUKASIEWICZ(Norm.NORMS[2].toString(), 2), //
            @Label(value = "Yager[2.0] Norm")
            YAGER_2(Norm.NORMS[3].toString(), 3), //
            @Label(value = "Yager[0.5] Norm")
            YAGER_05(Norm.NORMS[4].toString(), 4);

        private final String m_value;

        private final int m_index;

        NormChoice(final String value, final int index) {
            m_value = value;
            m_index = index;
        }

        String getValue() {
            return m_value;
        }

        Integer getIndex() {
            return m_index;
        }

        static NormChoice getFromIndex(final Integer value) throws InvalidSettingsException {
            for (final NormChoice norm : values()) {
                if (norm.getIndex().equals(value)) {
                    return norm;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final Integer index) {
            var values = Arrays.asList(NormChoice.values()).stream()
                .map(missing -> "(%s := %s)".formatted(missing.getValue(), missing.getIndex()))
                .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", index, values);
        }

    }

    enum ShrinkChoice {

        @Label(value = "Volume border based", description = """
                Applies the volume loss in terms of the support or core region borders.
                """)
        VOLUME_BORDER_BASED(Shrink.SHRINKS[0].toString(), 0), //
        @Label(value = "Volume anchor based", description = "Uses the anchor value border.")
        VOLUME_ANCHOR_BASED(Shrink.SHRINKS[1].toString(), 1), //
        @Label(value = "Volume rule based", description = """
                Uses the entire rule volume interval for one rule and compute a final output across all rules.
                """)
        VOLUME_RULE_BASED(Shrink.SHRINKS[2].toString(), 2);

        private final String m_value;

        private final int m_index;

        ShrinkChoice(final String value, final int index) {
            m_value = value;
            m_index = index;
        }

        String getValue() {
            return m_value;
        }

        Integer getIndex() {
            return m_index;
        }

        static ShrinkChoice getFromIndex(final Integer value) throws InvalidSettingsException {
            for (final ShrinkChoice shrink : values()) {
                if (shrink.getIndex().equals(value)) {
                    return shrink;
                }
            }
            throw new InvalidSettingsException(createInvalidSettingsExceptionMessage(value));
        }

        private static String createInvalidSettingsExceptionMessage(final Integer index) {
            var values = Arrays.asList(ShrinkChoice.values()).stream()
                .map(missing -> "(%s := %s)".formatted(missing.getValue(), missing.getIndex()))
                .collect(Collectors.joining(", "));
            return String.format("Invalid value '%s'. Possible values: %s", index, values);
        }

    }

}
