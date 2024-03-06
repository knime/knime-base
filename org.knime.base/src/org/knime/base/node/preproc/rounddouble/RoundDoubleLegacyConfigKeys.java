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
 *   Feb 8, 2024 (kai): created
 */
package org.knime.base.node.preproc.rounddouble;

import org.knime.core.node.InvalidSettingsException;

/**
 * Legacy configuration keys for backwards compatibility
 *
 * @author Kilian Thiel, KNIME.com, Berlin, Germany
 * @author Kai Franze, KNIME GmbH, Germany
 */
final class RoundDoubleLegacyConfigKeys {

    private RoundDoubleLegacyConfigKeys() {
        // Utility class
    }

    enum RoundOutputType {

            Double("Double"), // NOSONAR: Backwards compatibility

            StringStandard("Standard String"), // NOSONAR: Backwards compatibility

            StringPlain("Plain String (no exponent)"), // NOSONAR: Backwards compatibility

            StringEngineering("Engineering String"); // NOSONAR: Backwards compatibility

        private final String m_label;

        RoundOutputType(final String label) {
            m_label = label;
        }

        public String getLabel() {
            return m_label;
        }

        /**
         * Get object for label.
         *
         * @param label ...
         * @return ...
         * @throws InvalidSettingsException If argument is invalid
         */
        static RoundOutputType valueByTextLabel(final String label) throws InvalidSettingsException {
            for (RoundOutputType t : values()) {
                if (t.getLabel().equals(label)) {
                    return t;
                }
            }
            throw new InvalidSettingsException("Round output type \"" + label + "\" is unknown.");
        }

    }

    enum NumberMode {

            /**
             * rounding to decimal places.
             */
            DECIMAL_PLACES("Decimal places"),

            /**
             * rounding to significant figures.
             */
            SIGNIFICANT_FIGURES("Significant figures");

        private String m_description;

        /**
         * Constructor.
         *
         * @param description The modes description.
         */
        NumberMode(final String description) {
            m_description = description;
        }

        /**
         * Returns the number mode value for the given description. If there exists no mode for description an
         * <code>IllegalArgumentException</code> is thrown.
         *
         * @param description The description to get the number mode value for.
         * @return Number mode value for given description
         * @throws InvalidSettingsException If argument is invalid
         */
        public static NumberMode valueByDescription(final String description) throws InvalidSettingsException {
            for (NumberMode mode : values()) {
                if (mode.description().equals(description)) {
                    return mode;
                }
            }
            throw new InvalidSettingsException("Number mode \"" + description + "\" is not supported.");
        }

        /**
         * @return The description of the mode.
         */
        public String description() {
            return m_description;
        }
    }

    /**
     * The configuration key for the selected column names.
     */
    static final String COLUMN_NAMES = "StringColNames";

    /**
     * The configuration key for the specified precision number.
     */
    static final String PRECISION_NUMBER = "PrecisionNumer";

    /**
     * The configuration key for the specified column appending.
     */
    static final String APPEND_COLUMNS = "AppendColumns";

    /**
     * The configuration key for the specified column suffix.
     */
    static final String COLUMN_SUFFIX = "ColumnSuffix";

    /**
     * The configuration key for the specified rounding mode.
     */
    static final String ROUNDING_MODE = "RoundingMode";

    /**
     * The configuration key for the output as string settings (a bool).
     */
    static final String OUTPUT_AS_STRING_DEPRECATED = "OutputAsString";

    /**
     * The configuration key for the output as string settings (plain, engineering, standard).
     */
    static final String OUTPUT_TYPE = "OutputType";

    /**
     * The configuration key for the specified number mode.
     */
    static final String NUMBER_MODE = "NumberMode";

}
