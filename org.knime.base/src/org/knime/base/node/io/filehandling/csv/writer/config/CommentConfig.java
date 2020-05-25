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
 *   May 4, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer.config;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.config.Config;
import org.knime.core.node.defaultnodesettings.SettingsModel;
import org.knime.core.node.port.PortObjectSpec;

/**
 * Comment related configurations for CSV writer node
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class CommentConfig extends SettingsModel {

    private static final String CFGKEY_ROOT = "comment_header_settings";

    private static final String CFGKEY_COMMENT_BEGIN = "comment_begin";

    private static final String CFGKEY_COMMENT_END = "comment_end";

    private static final String CFGKEY_COMMENT_INDENT = "comment_indentation";

    private static final String CFGKEY_COMMENT_ADD_TIME = "add_time_to_comment";

    private static final String CFGKEY_COMMENT_ADD_USER = "add_user_to_comment";

    private static final String CFGKEY_COMMENT_ADD_TABLENAME = "add_table_name_to_comment";

    private static final String CFGKEY_COMMENT_ADD_CUSTOM_TEXT = "add_custom_text_to_comment";

    private static final String CFGKEY_COMMENT_CUSTOM_TEXT = "custom_comment_text";

    private static final String DEFAULT_COMMENT_INDENT = "\t";

    private String m_commentBegin;

    private String m_commentEnd;

    private String m_commentIndent;

    private boolean m_addCreationTime;

    private boolean m_addCreationUser;

    private boolean m_addTableName;

    private boolean m_addCustomText;

    private String m_customText;

    /**
     * Default constructor
     */
    public CommentConfig() {
        m_commentBegin = "";
        m_commentEnd = "";
        m_commentIndent = DEFAULT_COMMENT_INDENT;
        m_addCreationTime = false;
        m_addCreationUser = false;
        m_addTableName = false;
        m_addCustomText = false;
        m_customText = "";
    }

    /**
     * Copy constructor
     *
     * @param source the source object to copy
     */
    public CommentConfig(final CommentConfig source) {
        m_commentBegin = source.getCommentBegin();
        m_commentEnd = source.getCommentEnd();
        m_commentIndent = DEFAULT_COMMENT_INDENT;
        m_addCreationTime = source.addCreationTime();
        m_addCreationUser = source.addCreationUser();
        m_addTableName = source.addTableName();
        m_addCustomText = source.addCustomText();
        m_customText = source.getCustomText();
    }

    @SuppressWarnings("unchecked")
    @Override
    protected CommentConfig createClone() {
        return new CommentConfig(this);
    }

    @Override
    protected String getModelTypeID() {
        return "MODEL_TYPE_ID_" + CFGKEY_ROOT;
    }

    @Override
    protected String getConfigName() {
        return CFGKEY_ROOT;
    }

    @Override
    protected void loadSettingsForDialog(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        loadSettingsForDialog(settings);
    }

    /**
     * Read the value(s) of this settings model from configuration object for the purpose of loading them into node
     * dialog. Default values are used if the key to a specific setting is not found.
     *
     * @param settings the configuration object
     * @throws NotConfigurableException if the sub-setting can not be extracted.
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings) throws NotConfigurableException {
        Config config;
        try {
            config = settings.getConfig(CFGKEY_ROOT);
        } catch (final InvalidSettingsException ex) {
            throw new NotConfigurableException(ex.getMessage());
        }
        m_commentBegin = config.getString(CFGKEY_COMMENT_BEGIN, "");
        m_commentEnd = config.getString(CFGKEY_COMMENT_END, "");
        m_commentIndent = config.getString(CFGKEY_COMMENT_INDENT, DEFAULT_COMMENT_INDENT);
        m_addCreationTime = config.getBoolean(CFGKEY_COMMENT_ADD_TIME, false);
        m_addCreationUser = config.getBoolean(CFGKEY_COMMENT_ADD_USER, false);
        m_addTableName = config.getBoolean(CFGKEY_COMMENT_ADD_TABLENAME, false);
        m_addCustomText = config.getBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT, false);
        m_customText = config.getString(CFGKEY_COMMENT_CUSTOM_TEXT, "");
    }

    @Override
    protected void saveSettingsForDialog(final NodeSettingsWO settings) throws InvalidSettingsException {
        saveSettingsForModel(settings);
    }

    @Override
    protected void validateSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(CFGKEY_ROOT);
        config.getString(CFGKEY_COMMENT_BEGIN);
        config.getString(CFGKEY_COMMENT_END);
        config.getString(CFGKEY_COMMENT_INDENT);
        config.getBoolean(CFGKEY_COMMENT_ADD_TIME);
        config.getBoolean(CFGKEY_COMMENT_ADD_USER);
        config.getBoolean(CFGKEY_COMMENT_ADD_TABLENAME);
        config.getBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT);
        config.getString(CFGKEY_COMMENT_CUSTOM_TEXT);
    }

    @Override
    protected void loadSettingsForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        final Config config = settings.getConfig(CFGKEY_ROOT);
        m_commentBegin = config.getString(CFGKEY_COMMENT_BEGIN);
        m_commentEnd = config.getString(CFGKEY_COMMENT_END);
        m_commentIndent = config.getString(CFGKEY_COMMENT_INDENT);
        m_addCreationTime = config.getBoolean(CFGKEY_COMMENT_ADD_TIME);
        m_addCreationUser = config.getBoolean(CFGKEY_COMMENT_ADD_USER);
        m_addTableName = config.getBoolean(CFGKEY_COMMENT_ADD_TABLENAME);
        m_addCustomText = config.getBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT);
        m_customText = config.getString(CFGKEY_COMMENT_CUSTOM_TEXT);

    }

    @Override
    protected void saveSettingsForModel(final NodeSettingsWO settings) {
        final Config config = settings.addConfig(CFGKEY_ROOT);
        config.addString(CFGKEY_COMMENT_BEGIN, m_commentBegin);
        config.addString(CFGKEY_COMMENT_END, m_commentEnd);
        config.addString(CFGKEY_COMMENT_INDENT, m_commentIndent);
        config.addBoolean(CFGKEY_COMMENT_ADD_TIME, m_addCreationTime);
        config.addBoolean(CFGKEY_COMMENT_ADD_USER, m_addCreationUser);
        config.addBoolean(CFGKEY_COMMENT_ADD_TABLENAME, m_addTableName);
        config.addBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT, m_addCustomText);
        config.addString(CFGKEY_COMMENT_CUSTOM_TEXT, m_customText);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " ('" + CFGKEY_ROOT + "')";
    }

    /**
     *
     * @return <code>true</code> if there is something to write as a comment.
     */
    private boolean commentWritingOn() {
        return addCreationTime() || addCreationUser() || addTableName() || !StringUtils.isEmpty(getCustomText())
            || StringUtils.isEmpty(getCommentBegin());
    }

    /**
     * Writes a comment header to the file, if specified so in the settings.
     *
     * @param file the writer to write the header out to.
     * @param tableName the name of the input table
     * @param append If the output will be appended to an existing file
     * @throws IOException if something went wrong during writing.
     */
    public void writeCommentHeader(final BufferedWriter file, final String tableName, final boolean append)
        throws IOException {
        boolean commentOn = commentWritingOn();

        if (file == null || !commentOn) { // No comment marker provided
            return;
        }
        // if we have block comment patterns we write them only once. Otherwise
        // we add the commentBegin to every line.
        final boolean blockComment = !StringUtils.isEmpty(m_commentEnd);
        if (blockComment) {
            file.write(m_commentBegin);
            file.newLine();
        }
        if (m_addCreationTime || m_addCreationUser) {
            file.write(getCreationText(blockComment, append));
            file.newLine();
        }
        // add the table name
        if (m_addTableName) {
            if (!blockComment) {
                file.write(m_commentBegin);
            }
            file.write(m_commentIndent + "The data was read from the \"" + tableName + "\" data table.");
            file.newLine();
        }
        // at last: add the user comment line
        if (!StringUtils.isEmpty(m_customText)) {
            file.write(getUserCommentText(blockComment));
            file.newLine();
        }
        // close the block comment
        if (blockComment) {
            file.write(m_commentEnd);
            file.newLine();
        }
    }

    private String getCreationText(final boolean blockComment, final boolean append) {
        StringBuilder commentBuilder = new StringBuilder();

        if (!blockComment) {
            commentBuilder.append(m_commentBegin);
        }
        commentBuilder.append(m_commentIndent);
        if (append) {
            commentBuilder.append("The following data was added ");
        } else {
            commentBuilder.append("This file was created ");
        }

        if (m_addCreationTime) {
            commentBuilder.append("on " + new Date() + " ");
        }
        if (m_addCreationUser) {
            commentBuilder.append("by user '" + System.getProperty("user.name") + "'");
        }
        return commentBuilder.toString();
    }

    private String getUserCommentText(final boolean blockComment) {
        StringBuilder commentBuilder = new StringBuilder();
        String[] lines = m_customText.split("\n");
        for (String line : lines) {
            if (!blockComment) {
                commentBuilder.append(m_commentBegin);
            }
            commentBuilder.append(m_commentIndent + line);
        }
        return commentBuilder.toString();
    }

    /**
     * @return the commentBegin
     */
    public String getCommentBegin() {
        return m_commentBegin;
    }

    /**
     * @param commentBegin the commentBegin to set
     */
    public void setCommentBegin(final String commentBegin) {
        m_commentBegin = commentBegin;
    }

    /**
     * @return the commentEnd
     */
    public String getCommentEnd() {
        return m_commentEnd;
    }

    /**
     * @param commentEnd the commentEnd to set
     */
    public void setCommentEnd(final String commentEnd) {
        m_commentEnd = commentEnd;
    }

    /**
     * @return the commentIndent
     */
    public String getCommentIndent() {
        return m_commentIndent;
    }

    /**
     * @param commentIndent the commentIndent to set
     */
    public void setCommentIndent(final String commentIndent) {
        m_commentIndent = commentIndent;
    }

    /**
     * @return the addCreationTime
     */
    public boolean addCreationTime() {
        return m_addCreationTime;
    }

    /**
     * @param addCreationTime the addCreationTime to set
     */
    public void setAddCreationTime(final boolean addCreationTime) {
        m_addCreationTime = addCreationTime;
    }

    /**
     * @return the addCreationUser
     */
    public boolean addCreationUser() {
        return m_addCreationUser;
    }

    /**
     * @param addCreationUser the addCreationUser to set
     */
    public void setAddCreationUser(final boolean addCreationUser) {
        m_addCreationUser = addCreationUser;
    }

    /**
     * @return the addTableName
     */
    public boolean addTableName() {
        return m_addTableName;
    }

    /**
     * @param addTableName the addTableName to set
     */
    public void setAddTableName(final boolean addTableName) {
        m_addTableName = addTableName;
    }

    /**
     * @return the addCustomText
     */
    public boolean addCustomText() {
        return m_addCustomText;
    }

    /**
     * @param addCustomText the addCustomText to set
     */
    public void setAddCustomText(final boolean addCustomText) {
        m_addCustomText = addCustomText;
    }

    /**
     * @return the custom comment text
     */
    public String getCustomText() {
        return m_customText;
    }

    /**
     * @param customText the custom comment text to set
     */
    public void setCustomtext(final String customText) {
        m_customText = customText;
    }
}
