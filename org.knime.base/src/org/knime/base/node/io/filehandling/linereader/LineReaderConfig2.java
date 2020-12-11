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
 *   04.11.2020 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;

/**
 * {@link ReaderSpecificConfig} for the line reader node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class LineReaderConfig2 implements ReaderSpecificConfig<LineReaderConfig2> {

    /** Setting used to store the character set name (encoding) */
    private String m_charSet = null;

    /** Setting to store the name of the column header */
    private String m_columnHeaderName = "Column";

    /** Setting to store the regular expression */
    private String m_regex = ".*";

    /** Setting to store the flag whether to use the regular expression or not */
    private boolean m_useRegex = false;

    /** Setting to store the the replacement of an empty line expression */
    private String m_emptyLineReplacement = "";

    /** Setting to store the flag whether to replace empty rows */
    private EmptyLineMode m_emptyLineMode = EmptyLineMode.REPLACE_BY_MISSING;

    /**
     * Constructor.
     */
    LineReaderConfig2() {
    }

    private LineReaderConfig2(final LineReaderConfig2 toCopy) {
        setCharSetName(toCopy.getCharSetName());
        setColumnHeaderName(toCopy.getColumnHeaderName());
        setRegex(toCopy.getRegex());
        setUseRegex(toCopy.useRegex());
        setEmptyLineMode(toCopy.getReplaceEmptyMode());
        setEmptyLineReplacement(toCopy.getEmptyLineReplacement());
    }

    @Override
    public LineReaderConfig2 copy() {
        return new LineReaderConfig2(this);
    }

    public String getCharSetName() {
        return m_charSet;
    }

    public void setCharSetName(final String charSet) {
        m_charSet = charSet;
    }

    public String getColumnHeaderName() {
        return m_columnHeaderName;
    }

    public void setColumnHeaderName(final String columnHeaderName) {
        m_columnHeaderName = columnHeaderName;
    }

    public String getRegex() {
        return m_regex;
    }

    public void setRegex(final String regex) {
        m_regex = regex;
    }

    public boolean useRegex() {
        return m_useRegex;
    }

    public void setUseRegex(final boolean useRegex) {
        m_useRegex = useRegex;
    }

    public String getEmptyLineReplacement() {
        return m_emptyLineReplacement;
    }

    public void setEmptyLineReplacement(final String replacement) {
        m_emptyLineReplacement = replacement;
    }

    public EmptyLineMode getReplaceEmptyMode() {
        return m_emptyLineMode;
    }

    public void setEmptyLineMode(final EmptyLineMode emptyLineMode) {
        m_emptyLineMode = emptyLineMode;
    }

}
