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
 *   May 12, 2020 (lars.schweikardt): created
 */
package org.knime.base.node.io.filehandling.table.csv.reader;

import org.knime.core.node.util.CheckUtils;

/**
 * Trimming options within quotes for the CSV reader.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @since 4.2
 */
public enum QuoteOption {
        /**
         * This mode is the standard mode and trims trailing and leading whitespaces and removes the quotes.
         */
        REMOVE_QUOTES_AND_TRIM("Remove quotes and trim whitespaces", false, true),

        /**
         * This mode keeps the quotes and does no trimming.
         */
        KEEP_QUOTES("Keep quotes", true, false);

    /** The {@link #toString()} representation of the {@link QuoteOption}. */
    private final String m_text;

    /** Flag indicating whether to keep quotes, or not. */
    private final boolean m_keepQuotes;

    /** Flag indicating whether quoted values have to be trimmed, or not. */
    private final boolean m_trimQuotedValues;

    /**
     * Constructor.
     *
     * @param text the {@code String} returned by {@link #toString()}
     * @param keepQuotes flag whether to keep quotes, or not
     * @param trimInsideQuotes flag whether to trim quoted values, or not
     */
    private QuoteOption(final String text, final boolean keepQuotes, final boolean trimQuotedValues) {
        CheckUtils.checkArgument(!(keepQuotes && trimQuotedValues),
            "Univocity does not support trimming inside quotes when the quotes have to be kept");
        m_text = text;
        m_keepQuotes = keepQuotes;
        m_trimQuotedValues = trimQuotedValues;
    }

    /**
     * Returns the keep quotes flag.
     *
     * @return the keep quotes flag
     */
    public boolean keepQuotes() {
        return m_keepQuotes;
    }

    /**
     * Returns the trim quoted values flag.
     *
     * @return the trim quoted values flag
     */
    public boolean trimQuotedValues() {
        return m_trimQuotedValues;
    }

    @Override
    public String toString() {
        return m_text;
    }

}
