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
 *   14.08.2019 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.FSConnectionFlowVariableProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * Configuration for the {@link LineReaderNodeModel}
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
public class LineReaderConfig {

    /** Config key for file chooser. */
    static final String CFG_FILE_CHOOSER = "filechooser";

    /** Config key for row prefix. */
    static final String CFG_ROW_PREFIX = "rowPrefix";

    /** Config key for column header. */
    static final String CFG_COLUMN_HEADER = "columnHeader";

    /** Config key for skip empty lines. */
    static final String CFG_SKIP_EMPTY = "skipEmpty";

    /** Config key for limit lines decision */
    static final String CFG_LIMIT_LINES = "limitLines";

    /** Config key for limit row count */
    static final String CFG_LIMIT_ROW_COUNT = "limitRowCount";

    /** Config key for regex. */
    static final String CFG_REGEX = "regex";

    private final SettingsModelFileChooser2 m_fileChooserModel = new SettingsModelFileChooser2(CFG_FILE_CHOOSER);

    private final SettingsModelString m_rowPrefixModel = new SettingsModelString(CFG_ROW_PREFIX, "Row");

    private final SettingsModelString m_chooserModel =
        new SettingsModelString("ColumnHeaderChooser", CustomColumnHeader.Custom.getActionCommand());

    private final SettingsModelString m_columnHeaderModel = new SettingsModelString(CFG_COLUMN_HEADER, "Column");

    private final SettingsModelBoolean m_skipEmptyLinesModel = new SettingsModelBoolean(CFG_SKIP_EMPTY, false);

    private final SettingsModelBoolean m_limitLinesModel = new SettingsModelBoolean(CFG_LIMIT_LINES, false);

    private final SettingsModelInteger m_limitRowCountModel = new SettingsModelInteger(CFG_LIMIT_ROW_COUNT, 1000);

    private final SettingsModelString m_regexModel = new SettingsModelString(CFG_REGEX, "");

    private final SettingsModelBoolean m_useRegexModel = new SettingsModelBoolean("useRegex", false);

    /**
     *
     */
    public LineReaderConfig() {
        m_limitRowCountModel.setEnabled(getLimitLines());
        m_regexModel.setEnabled(getUseRegex());
    }

    /**
     * @return the file chooser model
     */
    public SettingsModelFileChooser2 getFileChooserModel() {
        return m_fileChooserModel;
    }

    /**
     * Returns a list of Paths retrieved from the file chooser dialog choices
     *
     * @param provider the {@link FSConnectionFlowVariableProvider}
     * @return a list of paths
     * @throws IOException
     */
    public List<Path> getPaths(final FSConnectionFlowVariableProvider provider) throws IOException {

        FileChooserHelper helper = new FileChooserHelper(provider, getFileChooserModel());
        return helper.getPaths(getFileChooserModel().getPathOrURL());
    }

    /**
     * @return the rowPrefixModel
     */
    public SettingsModelString getRowPrefixModel() {
        return m_rowPrefixModel;
    }

    /**
     * @return the rowPrefix
     */
    public String getRowPrefix() {
        return m_rowPrefixModel.getStringValue();
    }

    /**
     * @return the columnHeaderModel
     */
    public SettingsModelString getColumnHeaderModel() {
        return m_columnHeaderModel;
    }

    /**
     * @return the columnHeader
     */
    public String getColumnHeader() {
        return m_columnHeaderModel.getStringValue();
    }

    /**
     * @return the chooserModel
     */
    public SettingsModelString getChooserModel() {
        return m_chooserModel;
    }

    /**
     * @return the readColHeader
     */
    public Boolean getReadColHeader() {
        return m_chooserModel.getStringValue().equals(CustomColumnHeader.FirstLine.getActionCommand());
    }

    /**
     * @return the skipEmptyLinesModel
     */
    public SettingsModelBoolean getSkipEmptyLinesModel() {
        return m_skipEmptyLinesModel;
    }

    /**
     * @return is skipEmptyLines
     */
    public boolean getSkipEmptyLines() {
        return m_skipEmptyLinesModel.getBooleanValue();
    }

    /**
     * @return the limitLinesModel
     */
    public SettingsModelBoolean getLimitLinesModel() {
        return m_limitLinesModel;
    }

    /**
     * @return the limitLines
     */
    public boolean getLimitLines() {
        return m_limitLinesModel.getBooleanValue();
    }

    /**
     * @return the limitRowCountModel
     */
    public SettingsModelInteger getLimitRowCountModel() {
        return m_limitRowCountModel;
    }

    /**
     * @return the limitRowCount
     */
    public int getLimitRowCount() {
        return m_limitRowCountModel.getIntValue();
    }

    /**
     * @return the regexModel
     */
    public SettingsModelString getRegexModel() {
        return m_regexModel;
    }

    /**
     * @return the regex
     */
    public String getRegex() {
        return m_regexModel.getStringValue();
    }

    /**
     * @return the useRegexModel
     */
    public SettingsModelBoolean getUseRegexModel() {
        return m_useRegexModel;
    }

    /**
     * @return the useRegex
     */
    public boolean getUseRegex() {
        return m_useRegexModel.getBooleanValue();
    }

    /**
     * Validates the given settings
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if settings are invalid
     */
    public void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

        m_fileChooserModel.validateSettings(settings);
        m_rowPrefixModel.validateSettings(settings);
        if (m_rowPrefixModel.getJavaUnescapedStringValue() == null) {
            throw new InvalidSettingsException("Invalid (null) row prefix");
        }
        m_columnHeaderModel.validateSettings(settings);
        m_chooserModel.validateSettings(settings);
        if (!getReadColHeader()
            && (m_columnHeaderModel.getStringValue() == null || m_columnHeaderModel.getStringValue().length() == 0)) {
            throw new InvalidSettingsException("Invalid (empty) column header");
        }
        m_skipEmptyLinesModel.validateSettings(settings);
        m_limitLinesModel.validateSettings(settings);
        m_limitRowCountModel.validateSettings(settings);
        m_regexModel.validateSettings(settings);
        if (!m_regexModel.getStringValue().isEmpty()) {
            try {
                Pattern.compile(m_regexModel.getStringValue());
            } catch (final PatternSyntaxException e) {
                throw new InvalidSettingsException("Invalid Regex: " + m_regexModel, e);
            }
        }
    }

    /**
     * Save current configuration.
     *
     * @param settings to save to.
     */
    final void saveConfiguration(final NodeSettingsWO settings) {
        m_fileChooserModel.saveSettingsTo(settings);
        m_rowPrefixModel.saveSettingsTo(settings);
        m_columnHeaderModel.saveSettingsTo(settings);
        m_skipEmptyLinesModel.saveSettingsTo(settings);
        m_limitRowCountModel.saveSettingsTo(settings);
        m_regexModel.saveSettingsTo(settings);
        m_useRegexModel.saveSettingsTo(settings);
        m_limitLinesModel.saveSettingsTo(settings);
        m_chooserModel.saveSettingsTo(settings);
    }

    /**
     * Load configuration in NodeModel.
     *
     * @param settings To load from.
     * @throws InvalidSettingsException If invalid.
     */
    final void loadConfiguration(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_fileChooserModel.loadSettingsFrom(settings);
        m_rowPrefixModel.loadSettingsFrom(settings);
        m_chooserModel.loadSettingsFrom(settings);
        m_columnHeaderModel.loadSettingsFrom(settings);

        if (!getReadColHeader()
            && (m_columnHeaderModel.getStringValue() == null || m_columnHeaderModel.getStringValue().length() == 0)) {
            throw new InvalidSettingsException("Invalid (empty) column header");
        }
        m_skipEmptyLinesModel.loadSettingsFrom(settings);
        m_limitLinesModel.loadSettingsFrom(settings);
        m_limitRowCountModel.loadSettingsFrom(settings);
        m_useRegexModel.loadSettingsFrom(settings);
        m_regexModel.loadSettingsFrom(settings);
        if (!m_regexModel.getStringValue().isEmpty()) {
            try {
                Pattern.compile(m_regexModel.getStringValue());
            } catch (final PatternSyntaxException e) {
                throw new InvalidSettingsException("Invalid Regex: " + m_regexModel, e);
            }
        }
    }



}
