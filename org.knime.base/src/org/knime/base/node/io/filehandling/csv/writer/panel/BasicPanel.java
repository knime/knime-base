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
 *   Mar 9, 2007 (ohl): created
 */
package org.knime.base.node.io.filehandling.csv.writer.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.io.filehandling.csv.writer.FileOverwritePolicy;
import org.knime.base.node.io.filehandling.csv.writer.LineBreakTypes;
import org.knime.base.node.io.filehandling.csv.writer.config.CSVWriter2Config;
import org.knime.base.node.io.filehandling.table.csv.reader.EscapeUtils;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NotConfigurableException;

/**
 * A dialog panel for advanced settings of CSV writer node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
public final class BasicPanel extends JPanel {

    /** Auto generated serialVersionUID */
    private static final long serialVersionUID = -6233070493423721644L;

    private final JCheckBox m_writeColumnHeaderChecker;

    private final JCheckBox m_skipColumnHeaderOnAppendChecker;

    private final JCheckBox m_writeRowHeaderChecker;

    private final JRadioButton m_overwritePolicyAbortButton;

    private final JRadioButton m_overwritePolicyAppendButton;

    private final JRadioButton m_overwritePolicyOverwriteButton;

    private final JTextField m_colDelimiterField;

    private final JComboBox<LineBreakTypes> m_lineBreakSelection;

    private final JTextField m_quoteField;

    private final JTextField m_quoteEscapeField;

    private final JCheckBox m_createParentDirChecker;

    /**
     * Default constructor - creates a populated JPanel
     */
    public BasicPanel() {
        super(new GridBagLayout());
        m_writeColumnHeaderChecker = new JCheckBox("Write column header");
        m_writeColumnHeaderChecker.addItemListener(e -> checkCheckerState());
        m_skipColumnHeaderOnAppendChecker = new JCheckBox("Don't write column headers if file exists");
        m_writeRowHeaderChecker = new JCheckBox("Write row ID");

        ButtonGroup bg = new ButtonGroup();
        m_overwritePolicyOverwriteButton = new JRadioButton("Overwrite");
        bg.add(m_overwritePolicyOverwriteButton);
        m_overwritePolicyAppendButton = new JRadioButton("Append");
        bg.add(m_overwritePolicyAppendButton);
        m_overwritePolicyAppendButton.addChangeListener(e -> checkCheckerState());
        m_overwritePolicyAbortButton = new JRadioButton("Abort");
        bg.add(m_overwritePolicyAbortButton);
        m_overwritePolicyAbortButton.doClick();

        m_createParentDirChecker = new JCheckBox("Create parent directories if required");

        final int textWidth = 3;
        m_colDelimiterField = new JTextField(",", textWidth);

        m_quoteField = new JTextField("\"", textWidth);
        m_quoteEscapeField = new JTextField("\"", textWidth);

        m_lineBreakSelection = new JComboBox<>(LineBreakTypes.values());
        m_lineBreakSelection.setEnabled(true);

        initLayout();
        checkCheckerState();
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
        add(createFileOptionsPanel(), gbc);
        gbc.gridy++;
        add(createWriterOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        add(Box.createVerticalBox(), gbc);
    }

    private JPanel createFileOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel fileOptionsPanel = new JPanel(new GridBagLayout());
        fileOptionsPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File options:"));

        gbc.insets = new Insets(5, 0, 3, 0);
        gbc.gridwidth = 3;
        fileOptionsPanel.add(m_createParentDirChecker, gbc);

        gbc.insets = new Insets(5, 0, 3, 0);
        gbc.gridy++;
        fileOptionsPanel.add(new JLabel("If file exists"), gbc);

        gbc.insets = new Insets(0, 5, 0, 0);
        gbc.gridwidth = 1;
        gbc.gridy++;
        fileOptionsPanel.add(m_overwritePolicyOverwriteButton, gbc);
        gbc.gridx++;
        fileOptionsPanel.add(m_overwritePolicyAppendButton, gbc);
        gbc.gridx++;
        fileOptionsPanel.add(m_overwritePolicyAbortButton, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        fileOptionsPanel.add(Box.createHorizontalBox(), gbc);

        return fileOptionsPanel;
    }

    private JPanel createWriterOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel writerOptionsPanel = new JPanel(new GridBagLayout());
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        writerOptionsPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Writer options:"));
        gbc.insets = new Insets(0, 5, 0, 0);
        writerOptionsPanel.add(createFormatOptionsPanel(), gbc);
        gbc.gridy++;
        writerOptionsPanel.add(createHeaderOptionsPanel(), gbc);
        return writerOptionsPanel;
    }

    private JPanel createFormatOptionsPanel() {
        GridBagConstraints gbc = createAndInitGBC();
        final JPanel formatOptionsPanel = new JPanel(new GridBagLayout());
        formatOptionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Format:"));

        final Insets labelPad = new Insets(5, 5, 5, 5);
        final Insets columnPad = new Insets(5, 60, 5, 5);

        gbc.insets = labelPad;
        formatOptionsPanel.add(m_colDelimiterField, gbc);
        gbc.gridx++;
        formatOptionsPanel.add(new JLabel("Column Delimiter"), gbc);
        gbc.gridx++;
        gbc.insets = columnPad;
        formatOptionsPanel.add(m_lineBreakSelection, gbc);
        gbc.gridx++;
        gbc.insets = labelPad;
        formatOptionsPanel.add(new JLabel("Row Delimiter"), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = labelPad;
        formatOptionsPanel.add(m_quoteField, gbc);
        gbc.gridx++;
        formatOptionsPanel.add(new JLabel("Quote Char"), gbc);
        gbc.gridx++;
        gbc.insets = columnPad;
        gbc.anchor = GridBagConstraints.LINE_END;
        formatOptionsPanel.add(m_quoteEscapeField, gbc);
        gbc.gridx++;
        gbc.insets = labelPad;
        formatOptionsPanel.add(new JLabel("Quote Escape Char"), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        formatOptionsPanel.add(Box.createHorizontalBox(), gbc);

        return formatOptionsPanel;
    }

    private JPanel createHeaderOptionsPanel() {
        final JPanel miscOptionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        miscOptionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Header:"));

        gbc.insets = new Insets(5, 5, 5, 5);
        miscOptionsPanel.add(m_writeColumnHeaderChecker, gbc);

        gbc.gridy++;
        miscOptionsPanel.add(m_skipColumnHeaderOnAppendChecker, gbc);

        gbc.gridy++;
        miscOptionsPanel.add(m_writeRowHeaderChecker, gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        miscOptionsPanel.add(Box.createHorizontalBox(), gbc);
        return miscOptionsPanel;
    }

    private FileOverwritePolicy getSelectedFileOverwritePolicy() throws InvalidSettingsException {
        if (m_overwritePolicyOverwriteButton.isSelected()) {
            return FileOverwritePolicy.OVERWRITE;
        } else if (m_overwritePolicyAppendButton.isSelected()) {
            return FileOverwritePolicy.APPEND;
        } else if (m_overwritePolicyAbortButton.isSelected()) {
            return FileOverwritePolicy.ABORT;
        } else {
            throw new InvalidSettingsException("There is no valid FileOverwritePolicy selected!");
        }
    }

    private void selectFileOverwritePolicy(final FileOverwritePolicy overwritePolicy) throws NotConfigurableException {
        switch (overwritePolicy) {
            case APPEND:
                m_overwritePolicyAppendButton.doClick();
                break;
            case OVERWRITE:
                m_overwritePolicyOverwriteButton.doClick();
                break;
            case ABORT: // Abort
                m_overwritePolicyAbortButton.doClick();
                break;
            default:
                throw new NotConfigurableException(
                    "Unexpected FileOverwritePolicy " + overwritePolicy.name() + " is provided!");
        }
    }

    /**
     * Checks whether or not the "on file exists" check should be enabled.
     */
    private void checkCheckerState() {
        m_skipColumnHeaderOnAppendChecker
            .setEnabled(m_writeColumnHeaderChecker.isSelected() && m_overwritePolicyAppendButton.isSelected());
    }

    /**
     * Loads dialog components with values from the provided configuration
     *
     * @param config the configuration to read values from
     * @throws NotConfigurableException if unexpected FileOverwritePolicy is provided
     */
    public void loadDialogSettings(final CSVWriter2Config config) throws NotConfigurableException {
        m_writeColumnHeaderChecker.setSelected(config.writeColumnHeader());
        m_skipColumnHeaderOnAppendChecker.setSelected(config.skipColumnHeaderOnAppend());
        m_writeRowHeaderChecker.setSelected(config.writeRowHeader());

        m_colDelimiterField.setText(EscapeUtils.escape(config.getColumnDelimiter()));

        m_lineBreakSelection.setSelectedItem(config.getLineBreak());

        m_quoteField.setText(EscapeUtils.escape(String.valueOf(config.getQuoteChar())));
        m_quoteEscapeField.setText(EscapeUtils.escape(String.valueOf(config.getQuoteEscapeChar())));

        selectFileOverwritePolicy(config.getFileOverwritePolicy());
        m_createParentDirChecker.setSelected(config.createParentDirectoryIfRequired());
        checkCheckerState();
    }

    /**
     * Reads values from dialog and updates the provided configuration.
     *
     * @param config the configuration to read values from
     * @throws InvalidSettingsException if there is no valid FileOverwritePolicy selected
     */
    public void saveDialogSettings(final CSVWriter2Config config) throws InvalidSettingsException {
        config.setWriteColumnHeader(m_writeColumnHeaderChecker.isSelected());
        config.setSkipColumnHeaderOnAppend(m_skipColumnHeaderOnAppendChecker.isSelected());
        config.setWriteRowHeader(m_writeRowHeaderChecker.isSelected());

        config.setColumnDelimiter(EscapeUtils.unescape(m_colDelimiterField.getText()));
        config.setLineBreak((LineBreakTypes)m_lineBreakSelection.getSelectedItem());

        config.setQuoteChar(EscapeUtils.unescape(m_quoteField.getText()));
        config.setQuoteEscapeChar(EscapeUtils.unescape(m_quoteEscapeField.getText()));

        config.setFileOverwritePolicy(getSelectedFileOverwritePolicy());
        config.setCreateParentDirectory(m_createParentDirChecker.isSelected());
    }
}
