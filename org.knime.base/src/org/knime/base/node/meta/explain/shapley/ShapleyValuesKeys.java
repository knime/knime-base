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
 *   Apr 9, 2019 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.meta.explain.shapley;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.base.node.meta.explain.shapley.ShapleyValues.ShapleyValuesKeyGen;
import org.knime.core.node.util.CheckUtils;

/**
 * This class binds together the generator and parser for
 * row keys used in the context of Shapley Values.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ShapleyValuesKeys {

    private static final String DELIMITER = "_";

    private static final String FOI_INTACT = "i";

    private static final String FOI_REPLACED = "r";

    private ShapleyValuesKeys() {
        // Static factory class for shapley values keys generator and parsers
    }

    static SVKeyGen createGenerator() {
        return new SVKeyGen();
    }

    static SVKeyParser createParser() {
        return new SVKeyParser();
    }

    static class SVKeyGen implements ShapleyValuesKeyGen {

        private int m_foi;

        private int m_iteration;

        private boolean m_foiIntact;

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFoi(final int foi) {
            m_foi = foi;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setIteration(final int iteration) {
            m_iteration = iteration;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void setFoiIntact(final boolean foiIntact) {
            m_foiIntact = foiIntact;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String createKey() {
            return DELIMITER + m_foi + DELIMITER + m_iteration + DELIMITER + getFoiSuffix();
        }

        private String getFoiSuffix() {
            return m_foiIntact ? FOI_INTACT : FOI_REPLACED;
        }
    }

    static class SVKeyParser {

        private static final int POS_ITERATION = 2;

        private static final int POS_FEATURE_IDX = 3;

        private static final int POS_FOI_INTACT = 1;

        private static final int MIN_COMPONENTS_PER_KEY = 4;

        private static final int NUM_ADDITIONAL_COMPONENTS = 3;

        private String m_originalKey;

        private int m_foi;

        private int m_iteration;

        private boolean m_foiIntact;

        /**
         *
         * @param key to parse
         * @return true if the key was created by the corresponding generator
         */
        public boolean accept(final String key) {
            try {
                final String[] split = split(key);
                m_originalKey = recreateOriginalKey(split);
                m_foi = parseFeatureIdx(split);
                m_foiIntact = parseFoiIntact(split);
                m_iteration = parseIteration(split);
                return true;
            } catch (Exception e) {
                // we could not parse the key because it has the wrong structure
                return false;
            }
        }

        private static String createErrorString(final String generatedKey) {
            return "The row key '" + generatedKey + "' was not created by an instance of this RowKeyGenerator.";
        }

        private static int parseIteration(final String[] split) {
            return Integer.parseInt(split[split.length - POS_ITERATION]);
        }

        private static int parseFeatureIdx(final String[] split) {
            return Integer.parseInt(split[split.length - POS_FEATURE_IDX]);
        }

        private static String[] split(final String key) {
            final String[] split = key.split(DELIMITER);
            CheckUtils.checkArgument(split.length >= MIN_COMPONENTS_PER_KEY, createErrorString(key));
            return split;
        }

        private static String recreateOriginalKey(final String[] split) {
            // TODO figure out if there is a more efficient way to do this
            return Arrays.stream(split, 0, split.length - NUM_ADDITIONAL_COMPONENTS)
                .collect(Collectors.joining(DELIMITER));
        }

        private static boolean parseFoiIntact(final String[] split) {
            return split[split.length - POS_FOI_INTACT].equals(FOI_INTACT);
        }

        public String getOriginalKey() {
            return m_originalKey;
        }

        public boolean isFoiIntact() {
            return m_foiIntact;
        }

        public int getFoi() {
            return m_foi;
        }

        public int getIteration() {
            return m_iteration;
        }

    }

}
