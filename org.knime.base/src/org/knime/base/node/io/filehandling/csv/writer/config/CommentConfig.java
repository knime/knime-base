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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;

/**
 * Comment related configurations for CSV writer node
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class CommentConfig implements SimpleConfig {

    private static final String CFGKEY_COMMENT_BEGIN = "comment_line_marker";

    private static final String CFGKEY_COMMENT_INDENT = "comment_indentation";

    private static final String CFGKEY_COMMENT_ADD_TIME = "add_time_to_comment";

    private static final String CFGKEY_COMMENT_ADD_USER = "add_user_to_comment";

    private static final String CFGKEY_COMMENT_ADD_TABLENAME = "add_table_name_to_comment";

    private static final String CFGKEY_COMMENT_ADD_CUSTOM_TEXT = "add_custom_text_to_comment";

    private static final String CFGKEY_COMMENT_CUSTOM_TEXT = "custom_comment_text";

    private String m_commentLineMarker;

    private String m_commentIndent;

    private boolean m_addExecutionTime;

    private boolean m_addUsername;

    private boolean m_addTableName;

    private boolean m_addCustomComment;

    private String m_customComment;

    /**
     * Default constructor
     */
    public CommentConfig() {
        m_commentLineMarker = "#";
        m_commentIndent = "\t";
        m_addExecutionTime = false;
        m_addUsername = false;
        m_addTableName = false;
        m_addCustomComment = false;
        m_customComment = "";
    }

    @Override
    public void loadInDialog(final NodeSettingsRO settings) {
        m_commentLineMarker = settings.getString(CFGKEY_COMMENT_BEGIN, "#");
        m_commentIndent = settings.getString(CFGKEY_COMMENT_INDENT, "\t");
        m_addExecutionTime = settings.getBoolean(CFGKEY_COMMENT_ADD_TIME, false);
        m_addUsername = settings.getBoolean(CFGKEY_COMMENT_ADD_USER, false);
        m_addTableName = settings.getBoolean(CFGKEY_COMMENT_ADD_TABLENAME, false);
        m_addCustomComment = settings.getBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT, false);
        m_customComment = settings.getString(CFGKEY_COMMENT_CUSTOM_TEXT, "");
    }

    @Override
    public void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_commentLineMarker = settings.getString(CFGKEY_COMMENT_BEGIN);
        m_commentIndent = settings.getString(CFGKEY_COMMENT_INDENT);
        m_addExecutionTime = settings.getBoolean(CFGKEY_COMMENT_ADD_TIME);
        m_addUsername = settings.getBoolean(CFGKEY_COMMENT_ADD_USER);
        m_addTableName = settings.getBoolean(CFGKEY_COMMENT_ADD_TABLENAME);
        m_addCustomComment = settings.getBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT);
        m_customComment = settings.getString(CFGKEY_COMMENT_CUSTOM_TEXT);
    }

    @Override
    public void validate(final NodeSettingsRO settings) throws InvalidSettingsException {
        settings.getString(CFGKEY_COMMENT_BEGIN);
        settings.getString(CFGKEY_COMMENT_INDENT);
        settings.getBoolean(CFGKEY_COMMENT_ADD_TIME);
        settings.getBoolean(CFGKEY_COMMENT_ADD_USER);
        settings.getBoolean(CFGKEY_COMMENT_ADD_TABLENAME);
        settings.getBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT);
        settings.getString(CFGKEY_COMMENT_CUSTOM_TEXT);
    }

    @Override
    public void save(final NodeSettingsWO settings) {
        settings.addString(CFGKEY_COMMENT_BEGIN, m_commentLineMarker);
        settings.addString(CFGKEY_COMMENT_INDENT, m_commentIndent);
        settings.addBoolean(CFGKEY_COMMENT_ADD_TIME, m_addExecutionTime);
        settings.addBoolean(CFGKEY_COMMENT_ADD_USER, m_addUsername);
        settings.addBoolean(CFGKEY_COMMENT_ADD_TABLENAME, m_addTableName);
        settings.addBoolean(CFGKEY_COMMENT_ADD_CUSTOM_TEXT, m_addCustomComment);
        settings.addString(CFGKEY_COMMENT_CUSTOM_TEXT, m_customComment);
    }

    /**
     *
     * @return <code>true</code> if there is something to write as a comment.
     */
    private boolean writingComment() {
        return addExecutionTime() || //
            addUsername() || //
            addTableName() || //
            (addCustomComment() && !StringUtils.isEmpty(getCustomComment()));
    }

    /**
     * Creates a list of comment lines based on the settings. Returns an empty list if no comments are available.
     *
     * @param tableName the name of the input table
     * @param isAppending If the output will be appended to an existing file
     * @return a list of String where elements represent a single line of comment
     */
    public List<String> getCommentHeader(final String tableName, final boolean isAppending) {
        List<String> commentLines = new ArrayList<>();
        if (writingComment()) {
            if (m_addExecutionTime || m_addUsername) {
                String creationLine = isAppending ? "The following data was added" : "This file was created";
                if (m_addExecutionTime) {
                    creationLine += " on " + new Date();
                }
                if (m_addUsername) {
                    creationLine += " by user '" + System.getProperty("user.name") + "'";
                }
                commentLines.add(creationLine);
            }
            // add the table name
            if (m_addTableName) {
                commentLines.add("The data was read from the \"" + tableName + "\" data table.");
            }
            // at last: add the user comment line
            if (!StringUtils.isEmpty(m_customComment)) {
                String[] lines = normalizeLines(m_customComment).split("\n");
                commentLines.addAll(Arrays.asList(lines));
            }
            // Modify each entry to start with m_commentLineMarker + m_commentIndent
            commentLines = commentLines.stream()//
                .map(l -> m_commentLineMarker + m_commentIndent + l)//
                .collect(Collectors.toList());
        }
        return commentLines;
    }

    private static final String normalizeLines(final String lines) {
        return lines.replaceAll(LineBreakTypes.WINDOWS.getLineBreak(), "\n") //
            .replaceAll(LineBreakTypes.MAC_OS9.getLineBreak(), "\n");
    }

    /**
     * @return the value marking the beginning of a comment line
     */
    public String getCommentLineMarker() {
        return m_commentLineMarker;
    }

    /**
     * @param commentLineMarker a value marking the beginning of a comment line
     */
    public void setCommentLineMarker(final String commentLineMarker) {
        m_commentLineMarker = commentLineMarker;
    }

    /**
     * @return the value used as indentation after the comment marker
     */
    public String getCommentIndent() {
        return m_commentIndent;
    }

    /**
     * @param commentIndent the value to be used as indentation after the comment marker
     */
    public void setCommentIndent(final String commentIndent) {
        m_commentIndent = commentIndent;
    }

    /**
     * @return {@code true} if the current execution time should be added in the comment header
     */
    public boolean addExecutionTime() {
        return m_addExecutionTime;
    }

    /**
     * @param addExecutionTime a flag indicating whether or not to add the current execution time in the comment header
     */
    public void setAddExecutionTime(final boolean addExecutionTime) {
        m_addExecutionTime = addExecutionTime;
    }

    /**
     * @return {@code true} if the user/login name should be added in the comment header
     */
    public boolean addUsername() {
        return m_addUsername;
    }

    /**
     * @param addUsername a flag indicating whether or not to add the user/login name in the comment header
     */
    public void setAddUsername(final boolean addUsername) {
        m_addUsername = addUsername;
    }

    /**
     * @return {@code true} if the input DataTable name should be added in the comment header
     */
    public boolean addTableName() {
        return m_addTableName;
    }

    /**
     * @param addTableName a flag indicating whether or not to add the input DataTable name in the comment header
     */
    public void setAddTableName(final boolean addTableName) {
        m_addTableName = addTableName;
    }

    /**
     * @return {@code true} if a custom text should be added in the comment header
     */
    public boolean addCustomComment() {
        return m_addCustomComment;
    }

    /**
     * @param addCustomComment a flag indicating whether or not to add a custom text in the comment header
     */
    public void setAddCustomComment(final boolean addCustomComment) {
        m_addCustomComment = addCustomComment;
    }

    /**
     * @return the custom comment text that should be added as a comment header
     */
    public String getCustomComment() {
        return m_customComment;
    }

    /**
     * @param customComment the custom comment text that should be added as a comment header
     */
    public void setCustomComment(final String customComment) {
        m_customComment = customComment;
    }

}
