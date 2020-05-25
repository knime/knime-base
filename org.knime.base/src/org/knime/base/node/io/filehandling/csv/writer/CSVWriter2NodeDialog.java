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
import javax.swing.JFileChooser;
import javax.swing.JPanel;

import org.knime.base.node.io.filehandling.csv.writer.config.CSVWriter2Config;
import org.knime.base.node.io.filehandling.csv.writer.panel.AdvancedPanel;
import org.knime.base.node.io.filehandling.csv.writer.panel.BasicPanel;
import org.knime.base.node.io.filehandling.csv.writer.panel.CommentPanel;
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

    private final BasicPanel m_basicPanel;

    private final AdvancedPanel m_advancedPanel;

    private final CharsetNamePanel m_encodingPanel;

    private final CommentPanel m_commentPanel;

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

        m_basicPanel = new BasicPanel();
        m_advancedPanel = new AdvancedPanel();
        m_commentPanel = new CommentPanel();

        addTab("Options", initLayout());
        addTab("Advanced Options", m_advancedPanel);
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
        panel.add(m_basicPanel, gbc);
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

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_writerConfig.loadInDialog(settings);

        m_filePanel.loadSettingsFrom(settings, specs);
        m_basicPanel.loadDialogSettings(m_writerConfig);

        m_advancedPanel.loadDialogSettings(m_writerConfig.getAdvancedConfig());
        m_commentPanel.loadDialogSettings(m_writerConfig.getCommentConfig());
        m_encodingPanel.setCharsetName(m_writerConfig.getCharsetName());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filePanel.saveSettingsTo(settings);
        m_basicPanel.saveDialogSettings(m_writerConfig);

        m_advancedPanel.saveDialogSettings(m_writerConfig.getAdvancedConfig());
        m_commentPanel.saveDialogSettings(m_writerConfig.getCommentConfig());

        final FileReaderNodeSettings s = new FileReaderNodeSettings();
        m_encodingPanel.overrideSettings(s);
        m_writerConfig.setCharSetName(s.getCharsetName());
        m_writerConfig.saveSettingsTo(settings);
    }
}
