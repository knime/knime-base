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
 * -------------------------------------------------------------------
 *
 * History
 *   Apr 26, 2020 (Temesgen H. Dadi, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.writer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.knime.base.node.io.filehandling.csv.writer.config.CSVWriter2Config;
import org.knime.base.node.io.filehandling.csv.writer.panel.AdvancedPanel;
import org.knime.base.node.io.filehandling.csv.writer.panel.CommentPanel;
import org.knime.base.node.io.filehandling.table.csv.reader.EscapeUtils;
import org.knime.base.node.io.filereader.CharsetNamePanel;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialog;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.defaultnodesettings.DialogComponentFileChooser2;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;

/**
 * {@link NodeDialog} for the "CSVWriter" Node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CSVWriter2NodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "csv_file_writer_history";

    /** textfield to enter file name. */
    private final DialogComponentFileChooser2 m_filePanel;

    /** Checkbox for writing column header. */
    private final JCheckBox m_writeColumnHeaderChecker;

    /** Checkbox for writing column header even if file exits. */
    private final JCheckBox m_skipColumnHeaderOnAppendChecker;

    /** Checkbox for writing column header. */
    private final JCheckBox m_writeRowHeaderChecker;

    private final JRadioButton m_overwritePolicyAbortButton;

    private final JRadioButton m_overwritePolicyAppendButton;

    private final JRadioButton m_overwritePolicyOverwriteButton;

    private final JTextField m_colDelimiterField;

    private final JComboBox<String> m_lineBreakSelection;

    private final JTextField m_quoteField;

    private final JTextField m_quoteEscapeField;

    private final AdvancedPanel m_advancedPanel;

    private final CharsetNamePanel m_encodingPanel;

    private final CommentPanel m_commentPanel;

    private final JCheckBox m_createParentDirChecker;

    private final CSVWriter2Config m_writerConfig;

    /**
     * Creates a new CSV writer dialog.
     *
     * @param writerConfig a {@code CSVWriter2Config}
     *
     */
    public CSVWriter2NodeDialog(final CSVWriter2Config writerConfig) {
        final FlowVariableModel fvm = createFlowVariableModel(
            new String[]{CSVWriter2Config.CFG_FILE_CHOOSER, SettingsModelFileChooser2.PATH_OR_URL_KEY},
            VariableType.StringType.INSTANCE);

        m_writerConfig = writerConfig;

        m_filePanel = new DialogComponentFileChooser2(0, writerConfig.getFileChooserModel(), FILE_HISTORY_ID,
            JFileChooser.SAVE_DIALOG, JFileChooser.FILES_ONLY, fvm);

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

        m_lineBreakSelection = new JComboBox<>(LineBreakTypes.displayNames());
        m_lineBreakSelection.setEnabled(true);

        addTab("Options", initLayout());

        m_advancedPanel = new AdvancedPanel();

        addTab("Advanced Options", m_advancedPanel);

        m_commentPanel = new CommentPanel();
        addTab("Comment Header", m_commentPanel);

        m_encodingPanel = new CharsetNamePanel(new FileReaderSettings());
        addTab("Encoding", m_encodingPanel);

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

    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 0, 5, 0);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        panel.add(createFilePanel(), gbc);
        gbc.gridy++;
        panel.add(createFileOptionsPanel(), gbc);
        gbc.gridy++;
        panel.add(createWriterOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(Box.createVerticalBox(), gbc);
        return panel;
    }

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output location:"));
        filePanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, m_filePanel.getComponentPanel().getPreferredSize().height));
        filePanel.add(m_filePanel.getComponentPanel());
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
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
        fileOptionsPanel.add(new JLabel("If file exists "), gbc);

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
        formatOptionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Format: "));

        final Insets labelPad = new Insets(5, 5, 5, 5);
        final Insets columnPad = new Insets(5, 60, 5, 5);

        gbc.insets = labelPad;
        formatOptionsPanel.add(m_colDelimiterField, gbc);
        gbc.gridx++;
        formatOptionsPanel.add(new JLabel("Column Delimiter "), gbc);
        gbc.gridx++;
        gbc.insets = columnPad;
        formatOptionsPanel.add(m_lineBreakSelection, gbc);
        gbc.gridx++;
        gbc.insets = labelPad;
        formatOptionsPanel.add(new JLabel("Line Ending Mode "), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.insets = labelPad;
        formatOptionsPanel.add(m_quoteField, gbc);
        gbc.gridx++;
        formatOptionsPanel.add(new JLabel("Quote Char "), gbc);
        gbc.gridx++;
        gbc.insets = columnPad;
        gbc.anchor = GridBagConstraints.LINE_END;
        formatOptionsPanel.add(m_quoteEscapeField, gbc);
        gbc.gridx++;
        gbc.insets = labelPad;
        formatOptionsPanel.add(new JLabel("Quote Escape Char "), gbc);

        gbc.gridx++;
        gbc.weightx = 1;
        formatOptionsPanel.add(Box.createHorizontalBox(), gbc);

        return formatOptionsPanel;
    }

    private JPanel createHeaderOptionsPanel() {
        final JPanel miscOptionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        miscOptionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Header: "));

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

    /**
     * Checks whether or not the "on file exists" check should be enabled.
     */
    private void checkCheckerState() {
        m_skipColumnHeaderOnAppendChecker
            .setEnabled(m_writeColumnHeaderChecker.isSelected() && m_overwritePolicyAppendButton.isSelected());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_filePanel.loadSettingsFrom(settings, specs);
        m_writerConfig.loadInDialog(settings);

        m_writeColumnHeaderChecker.setSelected(m_writerConfig.writeColumnHeader());
        m_skipColumnHeaderOnAppendChecker.setSelected(m_writerConfig.skipColumnHeaderOnAppend());
        m_writeRowHeaderChecker.setSelected(m_writerConfig.writeRowHeader());

        m_colDelimiterField.setText(EscapeUtils.escape(m_writerConfig.getColumnDelimeter()));
        m_lineBreakSelection.setSelectedIndex(m_writerConfig.getLineBreakIndex());

        m_quoteField.setText(EscapeUtils.escape(String.valueOf(m_writerConfig.getQuoteChar())));
        m_quoteEscapeField.setText(EscapeUtils.escape(String.valueOf(m_writerConfig.getQuoteEscapeChar())));

        selectFileOverwritePolicy();
        m_createParentDirChecker.setSelected(m_writerConfig.createParentDirectoryIfRequired());

        m_advancedPanel.loadDialogSettings(m_writerConfig.getAdvancedConfig());
        m_commentPanel.loadDialogSettings(m_writerConfig.getCommentConfig());

        m_encodingPanel.setCharsetName(m_writerConfig.getCharsetName());

        checkCheckerState();
    }

    private void selectFileOverwritePolicy() {
        switch (m_writerConfig.getFileOverwritePolicy()) {
            case APPEND:
                m_overwritePolicyAppendButton.doClick();
                break;
            case OVERWRITE:
                m_overwritePolicyOverwriteButton.doClick();
                break;
            default: // Abort
                m_overwritePolicyAbortButton.doClick();
        }
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filePanel.saveSettingsTo(settings);

        m_writerConfig.setWriteColumnHeader(m_writeColumnHeaderChecker.isSelected());
        m_writerConfig.setSkipColumnHeaderOnAppend(m_skipColumnHeaderOnAppendChecker.isSelected());
        m_writerConfig.setWriteRowHeader(m_writeRowHeaderChecker.isSelected());

        m_writerConfig.setColumnDelimeter(EscapeUtils.unescape(m_colDelimiterField.getText()));
        m_writerConfig.setLineBreak(m_lineBreakSelection.getSelectedIndex());

        m_writerConfig.setQuoteChar(EscapeUtils.unescape(m_quoteField.getText()));
        m_writerConfig.setQuoteEscapeChar(EscapeUtils.unescape(m_quoteEscapeField.getText()));

        m_writerConfig.setFileOverwritePolicy(getSelectedFileOverwritePolicy());
        m_writerConfig.setCreateParentDirectory(m_createParentDirChecker.isSelected());

        m_advancedPanel.saveDialogSettings(m_writerConfig.getAdvancedConfig());
        m_commentPanel.saveDialogSettings(m_writerConfig.getCommentConfig());

        FileReaderNodeSettings s = new FileReaderNodeSettings();
        m_encodingPanel.overrideSettings(s);
        m_writerConfig.setCharSetName(s.getCharsetName());
        m_writerConfig.saveSettingsTo(settings);
    }

    private FileOverwritePolicy getSelectedFileOverwritePolicy() {
        if (m_overwritePolicyOverwriteButton.isSelected()) {
            return FileOverwritePolicy.OVERWRITE;
        } else if (m_overwritePolicyAppendButton.isSelected()) {
            return FileOverwritePolicy.APPEND;
        } else {
            return FileOverwritePolicy.ABORT;
        }
    }
}
