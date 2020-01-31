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
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelInteger;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * Configuration for the {@link LineReaderNodeModel}.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 */
final class LineReaderConfig {

    static final String DEFAULT_ENCODING = "default";

    /** Config key for file chooser. */
    private static final String CFG_FILE_CHOOSER = "filechooser";

    /** Config key for row prefix. */
    private static final String CFG_ROW_PREFIX = "rowPrefix";

    /** Config key for column header. */
    private static final String CFG_COLUMN_HEADER = "columnHeader";

    /** Config key for skip empty lines. */
    private static final String CFG_SKIP_EMPTY = "skipEmpty";

    /** Config key for limit lines decision */
    private static final String CFG_LIMIT_LINES = "limitLines";

    /** Config key for limit row count */
    private static final String CFG_LIMIT_ROW_COUNT = "limitRowCount";

    /** Config key for regex. */
    private static final String CFG_REGEX = "regex";

    private static final String CFG_ENCODING = "encoding";

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

    private final SettingsModelString m_encodingModel = new SettingsModelString(CFG_ENCODING, DEFAULT_ENCODING);

    LineReaderConfig() {
        m_limitRowCountModel.setEnabled(getLimitLines());
        m_regexModel.setEnabled(getUseRegex());
    }

    final SettingsModelFileChooser2 getFileChooserModel() {
        return m_fileChooserModel;
    }

    /**
     * Returns a list of Paths retrieved from the file chooser dialog choices
     *
     * @param fs the {@link FSConnection}
     * @return a list of paths
     * @throws IOException
     * @throws InvalidSettingsException
     */
    final List<Path> getPaths(final Optional<FSConnection> fs) throws IOException, InvalidSettingsException {
        final FileChooserHelper helper = new FileChooserHelper(fs, getFileChooserModel());
        return helper.getPaths();
    }

    final SettingsModelString getRowPrefixModel() {
        return m_rowPrefixModel;
    }

    final String getRowPrefix() {
        return m_rowPrefixModel.getStringValue();
    }

    final SettingsModelString getColumnHeaderModel() {
        return m_columnHeaderModel;
    }

    final String getColumnHeader() {
        return m_columnHeaderModel.getStringValue();
    }

    final SettingsModelString getChooserModel() {
        return m_chooserModel;
    }

    final Boolean getReadColHeader() {
        return m_chooserModel.getStringValue().equals(CustomColumnHeader.FirstLine.getActionCommand());
    }

    final SettingsModelBoolean getSkipEmptyLinesModel() {
        return m_skipEmptyLinesModel;
    }

    final boolean getSkipEmptyLines() {
        return m_skipEmptyLinesModel.getBooleanValue();
    }

    final SettingsModelBoolean getLimitLinesModel() {
        return m_limitLinesModel;
    }

    final boolean getLimitLines() {
        return m_limitLinesModel.getBooleanValue();
    }

    final SettingsModelInteger getLimitRowCountModel() {
        return m_limitRowCountModel;
    }

    final int getLimitRowCount() {
        return m_limitRowCountModel.getIntValue();
    }

    final SettingsModelString getRegexModel() {
        return m_regexModel;
    }

    final String getRegex() {
        return m_regexModel.getStringValue();
    }

    final SettingsModelBoolean getUseRegexModel() {
        return m_useRegexModel;
    }

    final boolean getUseRegex() {
        return m_useRegexModel.getBooleanValue();
    }

    final SettingsModelString getEncodingModel() {
        return m_encodingModel;
    }

    final String getEncoding() {
        return m_encodingModel.getStringValue();
    }

    /**
     * Validates the given settings
     *
     * @param settings the node settings
     * @throws InvalidSettingsException if settings are invalid
     */
    final void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {

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

        if (settings.containsKey(LineReaderConfig.CFG_ENCODING)) {
            m_encodingModel.validateSettings(settings);
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
        m_encodingModel.saveSettingsTo(settings);
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

        if (settings.containsKey(LineReaderConfig.CFG_ENCODING)) {
            m_encodingModel.loadSettingsFrom(settings);
        } else {
            m_encodingModel.setStringValue(LineReaderConfig.DEFAULT_ENCODING);
        }
    }
}
