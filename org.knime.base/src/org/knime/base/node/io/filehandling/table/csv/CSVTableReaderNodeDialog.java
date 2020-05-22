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
 *   Feb 10, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.table.csv;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.node.io.filehandling.table.csv.reader.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.table.csv.reader.EscapeUtils;
import org.knime.base.node.io.filehandling.table.csv.reader.QuoteOption;
import org.knime.base.node.io.filereader.CharsetNamePanel;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.SharedIcons;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.connections.FSConnection;
import org.knime.filehandling.core.defaultnodesettings.DialogComponentFileChooser2;
import org.knime.filehandling.core.defaultnodesettings.FileChooserHelper;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.node.table.reader.MultiTableReader;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.preview.dialog.TableReaderPreview;
import org.knime.filehandling.core.port.FileSystemPortObjectSpec;
import org.knime.filehandling.core.util.CheckNodeContextUtil;

import com.univocity.parsers.csv.CsvFormat;

/**
 * Node dialog of the CSV reader prototype node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CSVTableReaderNodeDialog extends NodeDialogPane {

    private static final String START_AUTODETECT_LABEL = "Autodetect format";

    private static final String AUTODETECT_CANCEL_LABEL = "Cancel";

    private static final String PROGRESS_BAR_CARD = "progress";

    private static final String STATUS_CARD = "status";

    private static final String EMPTY_CARD = "empty";

    private static final int FS_INPUT_PORT = 0;

    private final DialogComponentFileChooser2 m_filePanel;

    private final JRadioButton m_failOnDifferingSpecs = new JRadioButton("Fail if specs differ");

    private final JRadioButton m_union = new JRadioButton("Union");

    private final JRadioButton m_intersection = new JRadioButton("Intersection");

    private final JTextField m_colDelimiterField;

    private final JTextField m_rowDelimiterField;

    private final JTextField m_quoteField;

    private final JTextField m_quoteEscapeField;

    private final JTextField m_commentStartField;

    private final JCheckBox m_hasRowIDChecker;

    private final JCheckBox m_hasColHeaderChecker;

    private final JCheckBox m_allowShortDataRowsChecker;

    private final JCheckBox m_skipEmptyDataRowsChecker;

    private final JCheckBox m_replaceQuotedEmptyStringChecker;

    private final JCheckBox m_limitRowsChecker;

    private final JSpinner m_limitRowsSpinner;

    private final JCheckBox m_skipFirstRowsChecker;

    private final JSpinner m_skipFirstRowsSpinner;

    private final JCheckBox m_skipFirstLinesChecker;

    private final JSpinner m_skipFirstLinesSpinner;

    private final JCheckBox m_limitAnalysisChecker;

    private final JSpinner m_limitAnalysisSpinner;

    private final JSpinner m_maxColsSpinner;

    private final JCheckBox m_maxCharsColumnChecker;

    private final CharsetNamePanel m_encodingPanel;

    private final MultiTableReadConfig<CSVTableReaderConfig> m_config;

    private final ButtonGroup m_quoteOptionsButtonGroup;

    private final JButton m_startAutodetection;

    private final JButton m_autoDetectionSettings;

    private CSVFormatAutoDetectionSwingWorker m_formatAutoDetectionSwingWorker;

    private JProgressBar m_analyzeProgressBar;

    private final JLabel m_autoDetectionStatusLabel;

    private final JLabel m_autoDetectionStatusIcon;

    private final JPanel m_statusProgressCardLayout;

    private Optional<FSConnection> m_fsConnection = null;

    private int m_autoDetectionBufferSize;

    private final ProducerRegistry<?, ?> m_producerRegistry;

    private PortObjectSpec[] m_specs;

    private final TableReaderPreview<CSVTableReaderConfig, String> m_tableReaderPreview;

    private final boolean m_isRemoteWorkflowContext;

    /** Create new CsvTableReader dialog. */
    CSVTableReaderNodeDialog(final SettingsModelFileChooser2 fileChooserModel,
        final MultiTableReadConfig<CSVTableReaderConfig> config,
        final MultiTableReader<CSVTableReaderConfig, Class<?>, String> multiReader,
        final ProducerRegistry<?, ?> producerRegistry) {
        m_producerRegistry = producerRegistry;
        m_tableReaderPreview =
            new TableReaderPreview<>(multiReader, fileChooserModel::getPathOrURL, this::getPaths, this::getConfig);
        final FlowVariableModel fvm = createFlowVariableModel(
            new String[]{fileChooserModel.getConfigName(), SettingsModelFileChooser2.PATH_OR_URL_KEY},
            VariableType.StringType.INSTANCE);
        m_isRemoteWorkflowContext = CheckNodeContextUtil.isRemoteWorkflowContext();

        m_filePanel = new DialogComponentFileChooser2(0, fileChooserModel, "csv_reader_prototype",
            JFileChooser.OPEN_DIALOG, JFileChooser.FILES_AND_DIRECTORIES, fvm);

        ButtonGroup specMergeGroup = new ButtonGroup();
        specMergeGroup.add(m_failOnDifferingSpecs);
        specMergeGroup.add(m_intersection);
        specMergeGroup.add(m_union);

        int textWidth = 3;
        Long stepSize = Long.valueOf(1);
        Long rowStart = Long.valueOf(0);
        Long rowEnd = Long.valueOf(Long.MAX_VALUE);
        Long skipOne = Long.valueOf(1);
        Long initLimit = Long.valueOf(50);

        m_colDelimiterField = new JTextField("###", textWidth);
        m_rowDelimiterField = new JTextField("###", textWidth);
        m_quoteField = new JTextField("###", textWidth);
        m_quoteEscapeField = new JTextField("###", textWidth);
        m_commentStartField = new JTextField("###", textWidth);
        m_hasRowIDChecker = new JCheckBox("Has row ID");
        m_hasColHeaderChecker = new JCheckBox("Has column header");
        m_allowShortDataRowsChecker = new JCheckBox("Support short data rows");
        m_skipEmptyDataRowsChecker = new JCheckBox("Skip empty data rows");
        m_replaceQuotedEmptyStringChecker = new JCheckBox("Replace empty quoted strings with missing values", true);
        m_startAutodetection = new JButton(START_AUTODETECT_LABEL);
        m_autoDetectionSettings = new JButton(SharedIcons.SETTINGS.get());
        m_autoDetectionStatusLabel = new JLabel();
        m_autoDetectionStatusIcon = new JLabel();
        m_statusProgressCardLayout = new JPanel(new CardLayout());

        m_skipFirstLinesChecker = new JCheckBox("Skip first lines ");
        m_skipFirstLinesSpinner = new JSpinner(new SpinnerNumberModel(skipOne, rowStart, rowEnd, stepSize));
        m_skipFirstLinesChecker
            .addActionListener(e -> controlSpinner(m_skipFirstLinesChecker, m_skipFirstLinesSpinner));
        m_skipFirstLinesChecker.doClick();

        m_skipFirstRowsChecker = new JCheckBox("Skip first data rows ");
        m_skipFirstRowsSpinner = new JSpinner(new SpinnerNumberModel(skipOne, rowStart, rowEnd, stepSize));
        m_skipFirstRowsChecker.addActionListener(e -> controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner));
        m_skipFirstRowsChecker.doClick();

        m_limitRowsChecker = new JCheckBox("Limit data rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitRowsChecker.addActionListener(e -> controlSpinner(m_limitRowsChecker, m_limitRowsSpinner));
        m_limitRowsChecker.doClick();

        m_limitAnalysisChecker = new JCheckBox("Limit data rows scanned for spec ");
        m_limitAnalysisSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitAnalysisChecker.addActionListener(e -> controlSpinner(m_limitAnalysisChecker, m_limitAnalysisSpinner));
        m_limitAnalysisChecker.doClick();

        m_maxColsSpinner = new JSpinner(new SpinnerNumberModel(1024, 1, Integer.MAX_VALUE, 1024));
        m_maxCharsColumnChecker = new JCheckBox();

        m_quoteOptionsButtonGroup = new ButtonGroup();

        m_analyzeProgressBar = new JProgressBar();
        m_analyzeProgressBar.setIndeterminate(true);

        m_startAutodetection.addActionListener(e -> {
            if (e.getActionCommand().equals(START_AUTODETECT_LABEL)) {
                startFormatAutoDetection();
            } else {
                m_startAutodetection.setText(START_AUTODETECT_LABEL);

                if (m_formatAutoDetectionSwingWorker != null) {
                    m_formatAutoDetectionSwingWorker.cancel(true);
                    m_formatAutoDetectionSwingWorker = null;

                    resetUIafterAutodetection();
                }
            }
        });

        m_autoDetectionSettings.addActionListener(e -> openSettingsDialog());

        addTab("Options", initLayout());
        addTab("Advanced Options", createAdvancedOptionsPanel());
        addTab("Limit Rows", getLimitRowsPanel());

        m_encodingPanel = new CharsetNamePanel(new FileReaderSettings());
        addTab("Encoding", m_encodingPanel);

        m_config = config;
        registerPreviewChangeListeners();
    }

    private void registerPreviewChangeListeners() {
        final DocumentListener documentListener = new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                m_tableReaderPreview.configChanged();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                m_tableReaderPreview.configChanged();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                m_tableReaderPreview.configChanged();
            }
        };
        final ActionListener actionListener = l -> m_tableReaderPreview.configChanged();
        final ChangeListener changeListener = l -> m_tableReaderPreview.configChanged();

        m_colDelimiterField.getDocument().addDocumentListener(documentListener);
        m_rowDelimiterField.getDocument().addDocumentListener(documentListener);
        m_quoteField.getDocument().addDocumentListener(documentListener);
        m_quoteEscapeField.getDocument().addDocumentListener(documentListener);
        m_commentStartField.getDocument().addDocumentListener(documentListener);

        m_failOnDifferingSpecs.addActionListener(actionListener);
        m_intersection.addActionListener(actionListener);
        m_union.addActionListener(actionListener);
        m_hasRowIDChecker.addActionListener(actionListener);
        m_hasColHeaderChecker.addActionListener(actionListener);
        m_allowShortDataRowsChecker.addActionListener(actionListener);
        m_skipEmptyDataRowsChecker.addActionListener(actionListener);
        m_replaceQuotedEmptyStringChecker.addActionListener(actionListener);
        m_limitRowsChecker.addActionListener(actionListener);
        m_skipFirstRowsChecker.addActionListener(actionListener);
        m_skipFirstLinesChecker.addActionListener(actionListener);
        m_limitAnalysisChecker.addActionListener(actionListener);
        m_maxCharsColumnChecker.addActionListener(actionListener);
        for (final AbstractButton b : Collections.list(m_quoteOptionsButtonGroup.getElements())) {
            b.addActionListener(actionListener);
        }

        m_skipFirstLinesSpinner.getModel().addChangeListener(changeListener);
        m_skipFirstRowsSpinner.getModel().addChangeListener(changeListener);
        m_limitRowsSpinner.getModel().addChangeListener(changeListener);
        m_limitAnalysisSpinner.getModel().addChangeListener(changeListener);
        m_maxColsSpinner.getModel().addChangeListener(changeListener);

        m_encodingPanel.addChangeListener(changeListener);
        m_filePanel.getModel().addChangeListener(changeListener);
    }

    private List<Path> getPaths() throws IOException {
        if (m_specs == null) {
            throw new IllegalStateException("The spec is not available.");
        }
        final FileChooserHelper fch =
            new FileChooserHelper(FileSystemPortObjectSpec.getFileSystemConnection(m_specs, FS_INPUT_PORT),
                (SettingsModelFileChooser2)m_filePanel.getModel());
        try {
            return fch.getPaths();
        } catch (InvalidSettingsException e) {
            throw new IOException(e);
        }
    }

    private MultiTableReadConfig<CSVTableReaderConfig> getConfig() {
        try {
            saveConfig();
        } catch (InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
        return m_config;
    }

    /**
     * Enables a {@link JSpinner} based on a corresponding {@link JCheckBox}.
     *
     * @param checker the {@link JCheckBox} which controls if a {@link JSpinner} should be enabled
     * @param spinner a {@link JSpinner} controlled by the {@link JCheckBox}
     */
    private static void controlSpinner(final JCheckBox checker, final JSpinner spinner) {
        spinner.setEnabled(checker.isSelected());
    }

    /**
     * Creates a {@link JPanel} filled with dialog components, including file chooser dialog, Spec merge options in case
     * of multiple files and options for reading CSV files.
     *
     * @return a {@link JPanel} filled with dialog components.
     */
    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;

        panel.add(createFilePanel(), gbc);
        gbc.gridy++;
        panel.add(createSpecMergePanel(), gbc);
        gbc.gridy++;
        panel.add(createOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(m_tableReaderPreview, gbc);
        return panel;
    }

    private JPanel createFilePanel() {
        final JPanel filePanel = new JPanel();
        filePanel.setLayout(new BoxLayout(filePanel, BoxLayout.X_AXIS));
        filePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        filePanel.setMaximumSize(
            new Dimension(Integer.MAX_VALUE, m_filePanel.getComponentPanel().getPreferredSize().height));
        filePanel.add(m_filePanel.getComponentPanel());
        filePanel.add(Box.createHorizontalGlue());
        return filePanel;
    }

    private JPanel createSpecMergePanel() {
        final JPanel specMergePanel = new JPanel(new GridBagLayout());
        specMergePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            "Spec merge options (multiple files)"));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 5, 0, 5);
        specMergePanel.add(m_failOnDifferingSpecs, gbc);
        gbc.gridx = 1;
        specMergePanel.add(m_intersection, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        specMergePanel.add(m_union, gbc);
        return specMergePanel;
    }

    /** Creates the {@link JPanel} for the Advanced Settings Tab. */
    private JPanel createAdvancedOptionsPanel() {
        final JPanel outerPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        outerPanel.add(createMemoryLimitsPanel(), gbc);
        gbc.gridy++;
        outerPanel.add(createQuoteOptionsPanel(), gbc);
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.gridy++;
        gbc.weightx = 0;
        gbc.weighty = 1;
        outerPanel.add(Box.createVerticalBox(), gbc);
        return outerPanel;
    }

    /** Creates the panel allowing to adjust the memory limits of the reader. */
    private JPanel createMemoryLimitsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 5, 5, 5);

        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader memory limits"));
        panel.add(new JLabel("Limit memory per column"), gbc);
        gbc.gridx += 1;
        panel.add(m_maxCharsColumnChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        panel.add(new JLabel("Maximum number of columns"), gbc);
        gbc.gridx += 1;
        panel.add(m_maxColsSpinner, gbc);
        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalBox(), gbc);
        return panel;
    }

    /** Creates the panel allowing to set the trimming mode for quoted values. */
    private JPanel createQuoteOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 5, 0, 0);

        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Quote options"));

        for (final QuoteOption mode : QuoteOption.values()) {
            final JRadioButton b = new JRadioButton(mode.toString());
            b.setActionCommand(mode.name());
            m_quoteOptionsButtonGroup.add(b);
            panel.add(b, gbc);
            ++gbc.gridy;
            gbc.insets = new Insets(0, 5, 0, 0);
        }
        gbc.insets = new Insets(10, 0, 5, 0);
        gbc.gridx = 0;
        gbc.gridwidth = QuoteOption.values().length;
        ++gbc.gridy;
        panel.add(m_replaceQuotedEmptyStringChecker, gbc);
        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalBox(), gbc);
        return panel;
    }

    private JPanel createOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader options:"));
        gbc.weightx = 1;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        optionsPanel.add(createAutoDetectOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        gbc.insets = new Insets(0, 5, 0, 0);
        optionsPanel.add(createOptionsSubPanel(), gbc);
        return optionsPanel;
    }

    private JPanel createAutoDetectOptionsPanel() {
        final JPanel autoDetectPanel = new JPanel(new GridBagLayout());
        autoDetectPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Format"));
        final GridBagConstraints gbc = createAndInitGBC();

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints buttonConstraints = createAndInitGBC();
        buttonConstraints.weightx = 0;
        buttonPanel.add(m_startAutodetection, buttonConstraints);
        buttonConstraints.gridx += 1;
        buttonPanel.add(m_autoDetectionSettings, buttonConstraints);

        gbc.weightx = 0;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 9, 5, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        autoDetectPanel.add(buttonPanel, gbc);

        createCardLayout();
        gbc.gridx += 1;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        autoDetectPanel.add(m_statusProgressCardLayout, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        autoDetectPanel.add(getInFlowLayout(m_colDelimiterField, new JLabel("Column delimiter ")), gbc);

        gbc.gridx += 1;
        autoDetectPanel.add(getInFlowLayout(m_rowDelimiterField, new JLabel("Row delimiter ")), gbc);

        gbc.gridx += 1;
        gbc.weightx = 1;
        autoDetectPanel.add(new JPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0;
        autoDetectPanel.add(getInFlowLayout(m_quoteField, new JLabel("Quote char ")), gbc);

        gbc.gridx += 1;
        gbc.weightx = 1;
        autoDetectPanel.add(getInFlowLayout(m_quoteEscapeField, new JLabel("Quote escape char ")), gbc);

        return autoDetectPanel;
    }

    private void createCardLayout() {
        JPanel tempPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints tempConstraints = createAndInitGBC();
        m_statusProgressCardLayout.add(tempPanel, EMPTY_CARD);

        tempPanel = new JPanel(new GridBagLayout());
        tempConstraints.gridx = 0;
        tempConstraints.weightx = 1;
        tempConstraints.fill = GridBagConstraints.HORIZONTAL;
        tempPanel.add(m_analyzeProgressBar, tempConstraints);
        m_statusProgressCardLayout.add(tempPanel, PROGRESS_BAR_CARD);

        tempPanel = new JPanel(new GridBagLayout());
        tempConstraints.gridx = 0;
        tempConstraints.weightx = 0;
        tempPanel.add(m_autoDetectionStatusIcon, tempConstraints);
        tempConstraints.gridx += 1;
        tempConstraints.insets = new Insets(0, 5, 0, 0);
        tempPanel.add(m_autoDetectionStatusLabel, tempConstraints);
        // empty panel to keep icon and label in the same position, even if label text is shor
        tempConstraints.gridx += 1;
        tempConstraints.weightx = 1;
        tempPanel.add(new JPanel(), tempConstraints);
        m_statusProgressCardLayout.add(tempPanel, STATUS_CARD);
    }

    private JPanel createOptionsSubPanel() {
        final JPanel optionSubPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 6, 5, 5);
        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.NONE;
        optionSubPanel.add(getInFlowLayout(m_commentStartField, new JLabel("Comment char ")), gbc);

        JPanel tempPanel = createReaderOptions();
        gbc.gridy += 1;
        gbc.insets = new Insets(0, 7, 0, 0);
        optionSubPanel.add(tempPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        optionSubPanel.add(m_replaceQuotedEmptyStringChecker, gbc);

        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        optionSubPanel.add(Box.createHorizontalBox(), gbc);
        return optionSubPanel;
    }

    private JPanel createReaderOptions() {
        JPanel readerOptions = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        readerOptions.add(m_hasColHeaderChecker, gbc);
        gbc.gridx += 1;
        gbc.insets = new Insets(0, 26, 0, 0);
        readerOptions.add(m_hasRowIDChecker, gbc);
        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.insets = new Insets(5, 0, 5, 5);
        readerOptions.add(m_allowShortDataRowsChecker, gbc);
        gbc.gridx += 1;
        gbc.insets = new Insets(5, 26, 0, 0);
        readerOptions.add(m_skipEmptyDataRowsChecker, gbc);

        return readerOptions;
    }

    /**
     * Creates a {@link JPanel} filled with dialog components specific to limiting the number of rows that are read.
     *
     * @return a {@link JPanel} filled with dialog components.
     */
    private JPanel getLimitRowsPanel() {
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 5, 5, 5);
        optionsPanel.add(m_skipFirstLinesChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        optionsPanel.add(m_skipFirstLinesSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        optionsPanel.add(m_skipFirstRowsChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        optionsPanel.add(m_skipFirstRowsSpinner, gbc);

        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        optionsPanel.add(m_limitRowsChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        optionsPanel.add(m_limitRowsSpinner, gbc);

        gbc.gridy += 1;
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.weightx = 0;
        optionsPanel.add(m_limitAnalysisChecker, gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        optionsPanel.add(m_limitAnalysisSpinner, gbc);

        return optionsPanel;
    }

    private static JPanel getInFlowLayout(final JComponent... comps) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        for (JComponent c : comps) {
            p.add(c);
        }
        return p;
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
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        return gbc;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filePanel.saveSettingsTo(settings);
        saveConfig();
        m_config.save(settings);
    }

    /**
     * Fill in the setting values in {@link TableReadConfig} using values from dialog.
     */
    private void saveTableReadSettings() {
        final TableReadConfig<CSVTableReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        tableReadConfig.setUseRowIDIdx(m_hasRowIDChecker.isSelected());
        tableReadConfig.setRowIDIdx(0);

        tableReadConfig.setUseColumnHeaderIdx(m_hasColHeaderChecker.isSelected());
        tableReadConfig.setColumnHeaderIdx(0);

        tableReadConfig.setSkipRows(m_skipFirstRowsChecker.isSelected());
        tableReadConfig.setNumRowsToSkip((Long)m_skipFirstRowsSpinner.getValue());

        tableReadConfig.setLimitRows(m_limitRowsChecker.isSelected());
        tableReadConfig.setMaxRows((Long)m_limitRowsSpinner.getValue());

        tableReadConfig.setLimitRowsForSpec(m_limitAnalysisChecker.isSelected());
        tableReadConfig.setMaxRowsForSpec((Long)m_limitAnalysisSpinner.getValue());

        tableReadConfig.setAllowShortRows(m_allowShortDataRowsChecker.isSelected());
        tableReadConfig.setSkipEmptyRows(m_skipEmptyDataRowsChecker.isSelected());
    }

    /**
     * Fill in the setting values in {@link CSVTableReaderConfig} using values from dialog.
     */
    private void saveCsvSettings() {
        CSVTableReaderConfig csvReaderConfig = m_config.getTableReadConfig().getReaderSpecificConfig();

        csvReaderConfig.setDelimiter(EscapeUtils.unescape(m_colDelimiterField.getText()));
        csvReaderConfig.setLineSeparator(EscapeUtils.unescape(m_rowDelimiterField.getText()));

        csvReaderConfig.setQuote(m_quoteField.getText());
        csvReaderConfig.setQuoteEscape(m_quoteEscapeField.getText());
        csvReaderConfig.setComment(m_commentStartField.getText());

        csvReaderConfig.setSkipLines(m_skipFirstLinesChecker.isSelected());
        csvReaderConfig.setNumLinesToSkip((Long)m_skipFirstLinesSpinner.getValue());

        csvReaderConfig.setMaxColumns((Integer)m_maxColsSpinner.getValue());
        csvReaderConfig.limitCharsPerColumn(m_maxCharsColumnChecker.isSelected());

        csvReaderConfig.setReplaceEmptyWithMissing(m_replaceQuotedEmptyStringChecker.isSelected());

        csvReaderConfig
            .setQuoteOption(QuoteOption.valueOf(m_quoteOptionsButtonGroup.getSelection().getActionCommand()));

        FileReaderNodeSettings s = new FileReaderNodeSettings();
        m_encodingPanel.overrideSettings(s);
        csvReaderConfig.setCharSetName(s.getCharsetName());

        csvReaderConfig.setAutoDetectionBufferSize(m_autoDetectionBufferSize);
    }

    private void saveConfig() throws InvalidSettingsException {
        saveTableReadSettings();
        saveCsvSettings();
        m_config.setSpecMergeMode(getSpecMergeMode());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_tableReaderPreview.setEnabled(false);
        m_specs = specs;
        m_config.loadInDialog(settings, m_producerRegistry);
        m_filePanel.loadSettingsFrom(settings, specs);
        loadTableReadSettings();
        loadCSVSettings();
        setSpecMergeMode();
        m_fsConnection = FileSystemPortObjectSpec.getFileSystemConnection(specs, FS_INPUT_PORT);
        showCardInCardLayout(EMPTY_CARD);

        // enable/disable spinners
        controlSpinner(m_skipFirstLinesChecker, m_skipFirstLinesSpinner);
        controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner);
        controlSpinner(m_limitRowsChecker, m_limitRowsSpinner);
        controlSpinner(m_limitAnalysisChecker, m_limitAnalysisSpinner);

        refreshPreview(true);
    }

    /**
     * Fill in dialog components with TableReadConfig values
     */
    private void loadTableReadSettings() {
        // row limit options
        final TableReadConfig<CSVTableReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        m_hasColHeaderChecker.setSelected(tableReadConfig.useColumnHeaderIdx());
        m_hasRowIDChecker.setSelected(tableReadConfig.useRowIDIdx());

        m_allowShortDataRowsChecker.setSelected(tableReadConfig.allowShortRows());

        m_skipEmptyDataRowsChecker.setSelected(tableReadConfig.skipEmptyRows());

        m_skipFirstRowsChecker.setSelected(tableReadConfig.skipRows());
        m_skipFirstRowsSpinner.setValue(tableReadConfig.getNumRowsToSkip());

        m_limitRowsChecker.setSelected(tableReadConfig.limitRows());
        m_limitRowsSpinner.setValue(tableReadConfig.getMaxRows());

        m_limitAnalysisChecker.setSelected(tableReadConfig.limitRowsForSpec());
        m_limitAnalysisSpinner.setValue(tableReadConfig.getMaxRowsForSpec());
    }

    /**
     * Fill in dialog components with CSVTableReaderConfig values
     */
    private void loadCSVSettings() {
        // CSV specific options
        CSVTableReaderConfig csvReaderConfig = m_config.getTableReadConfig().getReaderSpecificConfig();
        m_colDelimiterField.setText(EscapeUtils.escape(csvReaderConfig.getDelimiter()));
        m_rowDelimiterField.setText(EscapeUtils.escape(csvReaderConfig.getLineSeparator()));

        m_quoteField.setText(EscapeUtils.escape(csvReaderConfig.getQuote()));
        m_quoteEscapeField.setText(EscapeUtils.escape(csvReaderConfig.getQuoteEscape()));
        m_commentStartField.setText(EscapeUtils.escape(csvReaderConfig.getComment()));

        m_maxColsSpinner.setValue(csvReaderConfig.getMaxColumns());
        m_maxCharsColumnChecker.setSelected(csvReaderConfig.isCharsPerColumnLimited());

        m_skipFirstLinesChecker.setSelected(csvReaderConfig.skipLines());
        m_skipFirstLinesSpinner.setValue(csvReaderConfig.getNumLinesToSkip());

        m_replaceQuotedEmptyStringChecker.setSelected(csvReaderConfig.replaceEmptyWithMissing());

        setQuoteOption(csvReaderConfig.getQuoteOption());

        FileReaderSettings fReadSettings = new FileReaderSettings();
        fReadSettings.setCharsetName(csvReaderConfig.getCharSetName());
        m_encodingPanel.loadSettings(fReadSettings);
        m_autoDetectionBufferSize = csvReaderConfig.getAutoDetectionBufferSize();
    }

    /**
     * Selects the corresponding radio button for the passed mode in the {@link ButtonGroup}.
     *
     * @param mode the mode returned by the {@link CSVTableReaderConfig}
     */
    private void setQuoteOption(final QuoteOption quoteOption) {
        final Enumeration<AbstractButton> quoteOptions = m_quoteOptionsButtonGroup.getElements();
        while (quoteOptions.hasMoreElements()) {
            final AbstractButton button = quoteOptions.nextElement();
            if (QuoteOption.valueOf(button.getActionCommand()) == quoteOption) {
                button.setSelected(true);
                return;
            }
        }
    }

    /**
     * get the selected {@link SpecMergeMode} from the dialog.
     *
     * @return the selected {@link SpecMergeMode}
     * @throws InvalidSettingsException
     */
    private SpecMergeMode getSpecMergeMode() throws InvalidSettingsException {
        if (m_failOnDifferingSpecs.isSelected()) {
            return SpecMergeMode.FAIL_ON_DIFFERING_SPECS;
        } else if (m_intersection.isSelected()) {
            return SpecMergeMode.INTERSECTION;
        } else if (m_union.isSelected()) {
            return SpecMergeMode.UNION;
        } else {
            throw new InvalidSettingsException("No spec merge mode selected!");
        }
    }

    /**
     * sets the Spec merge options in the dialog.
     *
     * @throws NotConfigurableException
     */
    private void setSpecMergeMode() throws NotConfigurableException {
        switch (m_config.getSpecMergeMode()) {
            case FAIL_ON_DIFFERING_SPECS:
                m_failOnDifferingSpecs.setSelected(true);
                break;
            case INTERSECTION:
                m_intersection.setSelected(true);
                break;
            case UNION:
                m_union.setSelected(true);
                break;
            default:
                throw new NotConfigurableException("Unknown spec merge mode " + m_config.getSpecMergeMode());

        }
    }

    private void startFormatAutoDetection() {
        m_formatAutoDetectionSwingWorker = new CSVFormatAutoDetectionSwingWorker(m_fsConnection, this);

        m_tableReaderPreview.setEnabled(false);

        setAutodetectComponentsEnabled(false);
        showCardInCardLayout(PROGRESS_BAR_CARD);
        m_startAutodetection.setText(AUTODETECT_CANCEL_LABEL);

        m_formatAutoDetectionSwingWorker.execute();
    }

    void setAutodetectComponentsEnabled(final boolean enabled) {
        m_commentStartField.setEnabled(enabled);
        m_skipFirstLinesChecker.setEnabled(enabled);
        m_skipFirstLinesSpinner.setEnabled(enabled);
        m_colDelimiterField.setEnabled(enabled);
        m_rowDelimiterField.setEnabled(enabled);
        m_quoteField.setEnabled(enabled);
        m_quoteEscapeField.setEnabled(enabled);
        m_filePanel.getModel().setEnabled(enabled);
    }

    void resetUIafterAutodetection() {
        setAutodetectComponentsEnabled(true);
        showCardInCardLayout(EMPTY_CARD);
        m_startAutodetection.setText(CSVTableReaderNodeDialog.START_AUTODETECT_LABEL);
    }

    void updateAutodetectionFields(final CsvFormat format) {
        m_colDelimiterField.setText(EscapeUtils.escape(format.getDelimiterString()));

        m_rowDelimiterField.setText(EscapeUtils.escape(format.getLineSeparatorString()));
        m_quoteField.setText(Character.toString(format.getQuote()));
        m_quoteEscapeField.setText(Character.toString(format.getQuoteEscape()));
    }

    String getCommentStart() {
        return m_commentStartField.getText();
    }

    boolean getSkipLines() {
        return m_skipFirstLinesChecker.isSelected();
    }

    long getNumLinesToSkip() {
        return (long)m_skipFirstLinesSpinner.getValue();
    }

    Charset getSelectedCharset() {
        final Optional<String> charsetName = m_encodingPanel.getSelectedCharsetName();
        return charsetName.isPresent() ? Charset.forName(charsetName.get()) : Charset.defaultCharset();
    }

    SettingsModelFileChooser2 getFileChooserSettingsModel() {
        return ((SettingsModelFileChooser2)m_filePanel.getModel()).clone();
    }

    int getBufferSize() {
        return m_autoDetectionBufferSize;
    }

    void setStatus(final String text, final String toolTipText, final Icon icon) {
        m_autoDetectionStatusIcon.setIcon(icon);
        m_autoDetectionStatusLabel.setText(text);
        m_autoDetectionStatusLabel.setToolTipText(toolTipText);

        showCardInCardLayout(STATUS_CARD);
    }

    private void showCardInCardLayout(final String cardName) {
        final CardLayout cardLayout = (CardLayout)m_statusProgressCardLayout.getLayout();
        cardLayout.show(m_statusProgressCardLayout, cardName);
    }

    private void openSettingsDialog() {
        // figure out the parent to be able to make the dialog modal
        Frame f = null;
        Container c = getPanel().getParent();
        while (c != null) {
            if (c instanceof Frame) {
                f = (Frame)c;
                break;
            }
            c = c.getParent();
        }

        CSVAutodetectionSettingsDialog settingsDialog =
            new CSVAutodetectionSettingsDialog(f, m_autoDetectionBufferSize);

        settingsDialog.setVisible(true);

        m_autoDetectionBufferSize = settingsDialog.getBufferSize();
    }

    void refreshPreview(final boolean refreshPreview) {
        m_tableReaderPreview.setEnabled(!m_isRemoteWorkflowContext);
        if (refreshPreview) {
            m_tableReaderPreview.configChanged();
        }
    }

    @Override
    public void onClose() {
        m_tableReaderPreview.onClose();
        super.onClose();
    }
}
