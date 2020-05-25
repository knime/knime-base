/*
 * ------------------------------------------------------------------------
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
 *   May 3, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.apache.commons.lang.StringUtils;
import org.knime.base.node.io.filehandling.csv.writer.config.CommentConfig;
import org.knime.core.node.InvalidSettingsException;

/**
 * A dialog panel for comment header related settings of CSV writer node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany (Modified)
 */
public final class CommentPanel extends JPanel {

    /** Auto generated serialVersionUID */
    private static final long serialVersionUID = 4531820284015324141L;

    private static final int TEXT_FIELD_WIDTH = 7;

    private final JTextField m_commentBeginField;

    private final JTextField m_commentIndentField;

    private final JCheckBox m_addCreationTimeChecker;

    private final JCheckBox m_addCreationUserChecker;

    private final JCheckBox m_addTableNameChecker;

    private final JCheckBox m_addCustomCommentChecker;

    private final JTextArea m_customCommentTextArea;

    /**
     * Default constructor
     */
    public CommentPanel() {
        super(new GridBagLayout());

        m_commentBeginField = new JTextField("#", TEXT_FIELD_WIDTH);
        m_commentIndentField = new JTextField("\\t", TEXT_FIELD_WIDTH);
        m_commentIndentField.setToolTipText("Use \\n or \\t for a new line or the " + "tab character");

        m_addCreationTimeChecker = new JCheckBox("the current creation time");
        m_addCreationUserChecker = new JCheckBox("the user account name");
        m_addTableNameChecker = new JCheckBox("the input table name");
        m_addCustomCommentChecker = new JCheckBox("the following text:");
        m_customCommentTextArea = new JTextArea("", 4, 45);
        // make the JTextArea border look similar to that of a JTextField
        (m_customCommentTextArea).setBorder(m_commentBeginField.getBorder());

        m_addCreationTimeChecker.addChangeListener(e -> commentSelectionChanged());
        m_addCreationUserChecker.addChangeListener(e -> commentSelectionChanged());
        m_addTableNameChecker.addChangeListener(e -> commentSelectionChanged());
        m_addCustomCommentChecker.addChangeListener(e -> commentSelectionChanged());
        m_addCustomCommentChecker.addChangeListener(e -> customCommentSelectionChanged());

        initLayout();

        commentSelectionChanged();
        customCommentSelectionChanged();
    }

    /**
     * Helper method to create and initialize {@link GridBagConstraints}.
     *
     * @return initialized {@link GridBagConstraints}
     */
    private static final GridBagConstraints createAndInitGBC() {
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    private void initLayout() {
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        add(createCommentSettingsPanel(), gbc);
        gbc.gridy++;
        add(createCommentContentPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        add(Box.createVerticalBox(), gbc);
    }

    private JPanel createCommentSettingsPanel() {
        final JPanel commentSettingsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        commentSettingsPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Comment pattern: "));

        final Insets labelPad = new Insets(5, 5, 5, 5);

        gbc.insets = labelPad;
        commentSettingsPanel.add(m_commentBeginField, gbc);
        gbc.gridx++;
        commentSettingsPanel.add(new JLabel("Comment Begin "), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        m_commentIndentField.setToolTipText("\"Use \\t for a tab character");
        commentSettingsPanel.add(m_commentIndentField, gbc);
        gbc.gridx++;
        commentSettingsPanel.add(new JLabel("Comment Lines Indentation "), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        commentSettingsPanel.add(Box.createHorizontalBox(), gbc);

        return commentSettingsPanel;
    }

    private JPanel createCommentContentPanel() {

        final JPanel commentContentPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        commentContentPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Included comment content: "));

        gbc.insets = new Insets(5, 5, 5, 5);
        commentContentPanel.add(m_addCreationUserChecker, gbc);
        gbc.gridy++;
        commentContentPanel.add(m_addCreationTimeChecker, gbc);
        gbc.gridy++;
        commentContentPanel.add(m_addTableNameChecker, gbc);
        gbc.gridy++;
        commentContentPanel.add(m_addCustomCommentChecker, gbc);
        gbc.gridy++;
        commentContentPanel.add(m_customCommentTextArea, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        commentContentPanel.add(Box.createHorizontalBox(), gbc);
        return commentContentPanel;
    }

    /**
     * @return the commentBegin
     */
    public String getCommentBegin() {
        return m_commentBeginField.getText();
    }

    /**
     * @return the custom text from the TextArea
     */
    public String getCustomText() {
        return m_customCommentTextArea.getText();
    }

    /**
     * @return the commentIndent
     */
    public String getCommentIndent() {
        return m_commentIndentField.getText().replaceAll("\\\\t", "\t");
    }

    /**
     * @return {@code true} if the user name should be included in the comment
     */
    public boolean addCreationTime() {
        return m_addCreationTimeChecker.isSelected();
    }

    /**
     * @return {@code true} if the user name should be included in the comment
     */
    public boolean addCreationUser() {
        return m_addCreationUserChecker.isSelected();
    }

    /**
     * @return the addTableName
     */
    public boolean addTableName() {
        return m_addTableNameChecker.isSelected();
    }

    /**
     * @return the addCustom
     */
    public boolean getAddCustom() {
        return m_addCustomCommentChecker.isSelected();
    }

    /**
     * @param commentBegin the commentBegin to set
     */
    public void setCommentBegin(final String commentBegin) {
        m_commentBeginField.setText(commentBegin);
    }

    /**
     * @param commentLine the commentLine to set
     */
    public void setCommentLine(final String commentLine) {
        m_customCommentTextArea.setText(commentLine);
    }

    /**
     * @param commentIndent the commentIndent to set
     */
    public void setCommentIndent(final String commentIndent) {
        m_commentIndentField.setText(commentIndent.replaceAll("\t", "\\\\t"));
    }

    /**
     * @param addCreationTime the addDate to set
     */
    public void setAddCreationTime(final boolean addCreationTime) {
        m_addCreationTimeChecker.setSelected(addCreationTime);
    }

    /**
     * @param addCreationUser the addUser to set
     */
    public void setAddCreationUser(final boolean addCreationUser) {
        m_addCreationUserChecker.setSelected(addCreationUser);
    }

    /**
     * @param addTableName the addTableName to set
     */
    public void setAddTableName(final boolean addTableName) {
        m_addTableNameChecker.setSelected(addTableName);
    }

    /**
     * @param addCustom the addCustom to set
     */
    public void setAddCustom(final boolean addCustom) {
        m_addCustomCommentChecker.setSelected(addCustom);
    }

    /**
     * Enables or disables the comment pattern text boxes depending on whether there is a comments to add or not.
     */
    private void commentSelectionChanged() {
        final boolean notEmpty = notEmptyComment();
        m_commentBeginField.setEnabled(notEmpty);
        m_commentIndentField.setEnabled(notEmpty);
    }

    /**
     * Enables or disables the custom comment text area depending on the state of m_addCustomCommentChecker.
     */
    private void customCommentSelectionChanged() {
        m_customCommentTextArea.setEnabled(m_addCustomCommentChecker.isSelected());
    }

    /**
     * @return {@code true} if there is actually a comment to write.
     */
    private boolean notEmptyComment() {
        return m_addCreationTimeChecker.isSelected() //
            || m_addCreationUserChecker.isSelected() //
            || m_addTableNameChecker.isSelected() //
            || (m_addCustomCommentChecker.isSelected());
    }

    private void checkCommentSettings() throws InvalidSettingsException {
        // if we are supposed to add some creation data, we need to know the comment pattern
        if ((addCreationTime() || addCreationUser() || addTableName()
            || StringUtils.isNotEmpty(getCustomText())) && StringUtils.isEmpty(getCommentBegin())) {
            throw new InvalidSettingsException(
                "The comment begin can not be empty if there is comment content to write!");
        }
    }

    /**
     * Loads dialog components with values from the provided configuration
     *
     * @param config the configuration to read values from
     */
    public void loadDialogSettings(final CommentConfig config) {
        setCommentBegin(config.getCommentBegin());
        setCommentIndent(config.getCommentIndent());
        setAddCreationTime(config.addCreationTime());
        setAddCreationUser(config.addCreationUser());
        setAddTableName(config.addTableName());
        setAddCustom(config.addCustomText());
        setCommentLine(config.getCustomText());
    }

    /**
     * Reads values from dialog and updates the provided configuration.
     *
     * @param config the configuration to read values from
     * @throws InvalidSettingsException if configured settings are not valid
     */
    public void saveDialogSettings(final CommentConfig config) throws InvalidSettingsException {
        checkCommentSettings();
        config.setCommentBegin(getCommentBegin());
        config.setCommentIndent(getCommentIndent());
        config.setAddCreationTime(addCreationTime());
        config.setAddCreationUser(addCreationUser());
        config.setAddTableName(addTableName());
        config.setAddCustomText(getAddCustom());
        config.setCustomtext(getCustomText());
    }
}
