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
package org.knime.base.node.io.filehandling.csv.reader;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Optional;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
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

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.csv.reader.api.EscapeUtils;
import org.knime.base.node.io.filehandling.csv.reader.api.QuoteOption;
import org.knime.base.node.io.filereader.CharsetNamePanel;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.util.GBCBuilder;

import com.univocity.parsers.csv.CsvFormat;

/**
 * Abstract node dialog for the CSV readers based on the new table reader framework.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 */
public abstract class AbstractCSVTableReaderNodeDialog
    extends AbstractPathTableReaderNodeDialog<CSVTableReaderConfig, Class<?>> {

    private static final String START_AUTODETECT_LABEL = "Autodetect format";

    private static final String AUTODETECT_CANCEL_LABEL = "Cancel";

    private static final String PROGRESS_BAR_CARD = "progress";

    private static final String STATUS_CARD = "status";

    private static final String EMPTY_CARD = "empty";

    private final JTextField m_colDelimiterField = CSVReaderDialogUtils.mkTextField();

    private final JTextField m_rowDelimiterField = CSVReaderDialogUtils.mkTextField();

    private final JTextField m_quoteField = CSVReaderDialogUtils.mkTextField();

    private final JTextField m_quoteEscapeField = CSVReaderDialogUtils.mkTextField();

    private final JTextField m_commentStartField = CSVReaderDialogUtils.mkTextField();

    private final JCheckBox m_hasRowIDChecker;

    private final JCheckBox m_prependSourceIdxToRowId;

    private final JCheckBox m_hasColHeaderChecker;

    private final JCheckBox m_allowShortDataRowsChecker;

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

    private final ButtonGroup m_quoteOptionsButtonGroup;

    private final JButton m_startAutodetection;

    private final JButton m_autoDetectionSettings;

    private CSVFormatAutoDetectionSwingWorker m_formatAutoDetectionSwingWorker;

    private final JProgressBar m_analyzeProgressBar;

    private final JLabel m_autoDetectionStatusLabel;

    private final JLabel m_autoDetectionStatusIcon;

    private final JPanel m_statusProgressCardLayout;

    private final NumberFormatDialog m_numberFormatDialog = new NumberFormatDialog();

    private int m_autoDetectionBufferSize;

    private final PathSettings m_pathSettings;

    /** The multi table reader config. */
    protected final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> m_config;

    /**
     * Create new CsvTableReader dialog.
     *
     * @param pathSettings the path settings
     * @param config the config
     * @param multiReader the multi reader
     * @param productionPathProvider provides the default {@link ProductionPath} for a specific column type
     * @param allowsReadingMultipleFiles {@code true} if the reader allows reading multiple files at once
     */
    protected AbstractCSVTableReaderNodeDialog(final PathSettings pathSettings,
        final DefaultMultiTableReadConfig<CSVTableReaderConfig, DefaultTableReadConfig<CSVTableReaderConfig>> config,
        final MultiTableReadFactory<Path, CSVTableReaderConfig, Class<?>> multiReader,
        final ProductionPathProvider<Class<?>> productionPathProvider, final boolean allowsReadingMultipleFiles) {
        super(multiReader, productionPathProvider, allowsReadingMultipleFiles);
        init(pathSettings);
        m_pathSettings = pathSettings;

        Long stepSize = Long.valueOf(1);
        Long rowStart = Long.valueOf(0);
        Long rowEnd = Long.valueOf(Long.MAX_VALUE);
        Long skipOne = Long.valueOf(1);
        Long initLimit = Long.valueOf(50);

        m_hasRowIDChecker = new JCheckBox("Has row ID");
        m_hasColHeaderChecker = new JCheckBox("Has column header");
        m_allowShortDataRowsChecker = new JCheckBox("Support short data rows");

        if (allowsReadingMultipleFiles) {
            m_prependSourceIdxToRowId = new JCheckBox("Prepend file index to row ID");
            m_hasRowIDChecker
                .addActionListener(e -> m_prependSourceIdxToRowId.setEnabled(m_hasRowIDChecker.isSelected()));
        } else {
            m_prependSourceIdxToRowId = null;
        }

        m_replaceQuotedEmptyStringChecker = new JCheckBox("Replace empty quoted strings with missing values", true);
        m_startAutodetection = new JButton(START_AUTODETECT_LABEL);
        m_autoDetectionSettings = new JButton(SharedIcons.SETTINGS.get());

        // disable auto-detection for remote view
        if (areIOComponentsDisabled()) {
            m_startAutodetection.setEnabled(false);
            m_autoDetectionSettings.setEnabled(false);
        }

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

        m_limitAnalysisChecker = new JCheckBox("Limit data rows scanned");
        m_limitAnalysisSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitAnalysisChecker.addActionListener(e -> controlSpinner(m_limitAnalysisChecker, m_limitAnalysisSpinner));
        m_limitAnalysisChecker.doClick();

        m_maxColsSpinner = new JSpinner(new SpinnerNumberModel(1024, 1, Integer.MAX_VALUE, 1024));
        m_maxCharsColumnChecker = new JCheckBox("Limit memory per column");

        m_quoteOptionsButtonGroup = new ButtonGroup();

        m_analyzeProgressBar = new JProgressBar();
        m_analyzeProgressBar.setIndeterminate(true);

        m_startAutodetection.addActionListener(e -> {
            if (e.getActionCommand().equals(START_AUTODETECT_LABEL)) {
                startFormatAutoDetection();
            } else {
                cancelFormatAutoDetection();
            }
        });

        m_autoDetectionSettings.addActionListener(e -> openSettingsDialog());

        addTab("Settings", createSettingsTab());
        addTab("Transformation", createTransformationTab());
        addTab("Advanced Settings", createAdvancedOptionsPanel());
        addTab("Limit Rows", getLimitRowsPanel());

        m_encodingPanel = new CharsetNamePanel(new FileReaderSettings());
        addTab("Encoding", createEncodingPanel());

        m_config = config;
        registerPreviewChangeListeners();
        hookUpNumberFormatWithColumnDelimiter();
    }

    private JPanel createEncodingPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetPos().anchorFirstLineStart().fillHorizontal();
        panel.add(m_encodingPanel, gbc.build());
        panel.add(createPreview(), gbc.incY().fillBoth().weight(1, 1).build());
        return panel;
    }

    @Override
    protected final MultiTableReadConfig<CSVTableReaderConfig> getConfig() throws InvalidSettingsException {
        return saveAndGetConfig();
    }

    @Override
    protected final ReadPathAccessor createReadPathAccessor() {
        return m_pathSettings.createReadPathAccessor();
    }

    /**
     * Initializes additional components and members of the sub class.
     *
     * @param pathSettings the path settings
     */
    protected abstract void init(final PathSettings pathSettings);

    /**
     * Returns {@code true} if the selected file appears to be valid, {@code false} otherwise.
     *
     * @return {@code true} if the selected file appears to be valid, {@code false} otherwise
     */
    protected abstract boolean validateFileSelection();

    /**
     * @return the panels to be add to the dialog at the top
     */
    protected abstract JPanel[] getPanels();

    /**
     * Returns the {@link JPanel JPanels} to be added at the end of the advanced settings tab.
     *
     * @return the panels to be added to the advanced settings tabs
     */
    protected JPanel[] getAdvancedPanels() {
        return new JPanel[]{};
    }

    /**
     * Register listeners that trigger a preview refresh.
     */
    protected void registerPreviewChangeListeners() {
        final DocumentListener documentListener = new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                configChanged();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                configChanged();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                configChanged();
            }
        };
        final ActionListener actionListener = l -> configChanged();
        final ChangeListener changeListener = l -> configChanged();

        m_colDelimiterField.getDocument().addDocumentListener(documentListener);
        m_rowDelimiterField.getDocument().addDocumentListener(documentListener);
        m_quoteField.getDocument().addDocumentListener(documentListener);
        m_quoteEscapeField.getDocument().addDocumentListener(documentListener);
        m_commentStartField.getDocument().addDocumentListener(documentListener);

        m_hasRowIDChecker.addActionListener(actionListener);
        if (m_prependSourceIdxToRowId != null) {
            m_prependSourceIdxToRowId.addActionListener(actionListener);
        }
        m_hasColHeaderChecker.addActionListener(actionListener);
        m_allowShortDataRowsChecker.addActionListener(actionListener);
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

        m_numberFormatDialog.registerDocumentListener(documentListener);
    }

    private void hookUpNumberFormatWithColumnDelimiter() {
        m_colDelimiterField.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void removeUpdate(final DocumentEvent e) {
                notifyNumberFormat();
            }

            @Override
            public void insertUpdate(final DocumentEvent e) {
                notifyNumberFormat();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                notifyNumberFormat();
            }

            private void notifyNumberFormat() {
                m_numberFormatDialog.setColumnDelimiter(m_colDelimiterField.getText());
            }
        });
    }

    private MultiTableReadConfig<CSVTableReaderConfig> saveAndGetConfig() throws InvalidSettingsException {
        try {
            saveConfig();
        } catch (RuntimeException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
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
    private JPanel createSettingsTab() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        for (final JPanel p : getPanels()) {
            panel.add(p, gbc);
            gbc.gridy++;
        }
        panel.add(createOptionsPanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(createPreview(), gbc);
        return panel;
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
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        gbc.gridy++;
        outerPanel.add(createDataRowsSpecLimitPanel(), gbc);
        gbc.gridy++;
        outerPanel.add(m_numberFormatDialog.getPanel(), gbc);
        gbc.gridy++;

        for (final JPanel p : getAdvancedPanels()) {
            outerPanel.add(p, gbc);
            ++gbc.gridy;
        }
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weightx = 1;
        gbc.weighty = 1;
        outerPanel.add(createPreview(), gbc);
        return outerPanel;
    }

    private JPanel createDataRowsSpecLimitPanel() {
        final JPanel specLimitPanel = new JPanel(new GridBagLayout());
        specLimitPanel.setBorder(CSVReaderDialogUtils.createBorder("Table specification"));
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.NONE;
        gbc.gridx = 0;
        gbc.weightx = 0;
        gbc.weighty = 0;
        gbc.insets = new Insets(5, 0, 5, 5);
        specLimitPanel.add(m_limitAnalysisChecker, gbc);
        gbc.gridx += 1;
        specLimitPanel.add(m_limitAnalysisSpinner, gbc);
        gbc.gridx += 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        specLimitPanel.add(Box.createVerticalBox(), gbc);
        return specLimitPanel;
    }

    /** Creates the panel allowing to adjust the memory limits of the reader. */
    private JPanel createMemoryLimitsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 0, 5, 5);

        panel.setBorder(CSVReaderDialogUtils.createBorder("Reader memory limits"));
        panel.add(m_maxCharsColumnChecker, gbc);

        gbc.gridy += 1;
        gbc.insets = new Insets(5, 3, 5, 5);
        panel.add(new JLabel("Maximum number of columns"), gbc);
        gbc.gridx += 1;
        panel.add(m_maxColsSpinner, gbc);
        ++gbc.gridy;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 0);
        panel.add(Box.createHorizontalBox(), gbc);
        return panel;
    }

    /** Creates the panel allowing to set the trimming mode for quoted values. */
    private JPanel createQuoteOptionsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.insets = new Insets(5, 5, 0, 0);

        panel.setBorder(CSVReaderDialogUtils.createBorder("Quote options"));

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
        gbc.insets = new Insets(5, 0, 0, 0);
        ++gbc.gridy;
        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalBox(), gbc);
        return panel;
    }

    private JPanel createOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        final JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(CSVReaderDialogUtils.createBorder("Reader options"));
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
        autoDetectPanel.setBorder(CSVReaderDialogUtils.createBorder("Format"));
        final GridBagConstraints gbc = createAndInitGBC();

        JPanel buttonPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints buttonConstraints = createAndInitGBC();
        buttonConstraints.weightx = 0;
        buttonConstraints.fill = GridBagConstraints.VERTICAL;
        buttonConstraints.weighty = 1;
        buttonConstraints.insets = new Insets(0, 0, 0, 2);
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
        if (m_prependSourceIdxToRowId != null) {
            gbc.gridx++;
            gbc.insets = new Insets(5, 26, 5, 5);
            readerOptions.add(m_prependSourceIdxToRowId, gbc);
        }
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
        gbc.weighty = 1;
        gbc.gridx = 0;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.gridy += 1;
        gbc.fill = GridBagConstraints.BOTH;
        optionsPanel.add(createPreview(), gbc);

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
    protected static final GridBagConstraints createAndInitGBC() {
        return CSVReaderDialogUtils.createAndInitGBC();
    }

    @Override
    protected final void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        saveAdditionalSettingsTo(settings);
        saveConfig();
        m_config.saveInModel(settings);
    }

    /**
     * Allows to save additional settings in subclasses.
     *
     * @param settings to save to
     * @throws InvalidSettingsException if the additional settings can't be saved
     */
    protected abstract void saveAdditionalSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * Fill in the setting values in {@link TableReadConfig} using values from dialog.
     */
    private void saveTableReadSettings() {
        final DefaultTableReadConfig<CSVTableReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        tableReadConfig.setUseRowIDIdx(m_hasRowIDChecker.isSelected());
        tableReadConfig.setRowIDIdx(0);
        if (m_prependSourceIdxToRowId != null) {
            tableReadConfig.setPrependSourceIdxToRowId(m_prependSourceIdxToRowId.isSelected());
        }

        tableReadConfig.setUseColumnHeaderIdx(m_hasColHeaderChecker.isSelected());
        tableReadConfig.setColumnHeaderIdx(0);

        tableReadConfig.setSkipRows(m_skipFirstRowsChecker.isSelected());
        tableReadConfig.setNumRowsToSkip((Long)m_skipFirstRowsSpinner.getValue());

        tableReadConfig.setLimitRows(m_limitRowsChecker.isSelected());
        tableReadConfig.setMaxRows((Long)m_limitRowsSpinner.getValue());

        tableReadConfig.setLimitRowsForSpec(m_limitAnalysisChecker.isSelected());
        tableReadConfig.setMaxRowsForSpec((Long)m_limitAnalysisSpinner.getValue());

        tableReadConfig.setAllowShortRows(m_allowShortDataRowsChecker.isSelected());
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

        m_numberFormatDialog.save(csvReaderConfig);
    }

    /**
     * Save the current config.
     *
     * @throws InvalidSettingsException if a setting could not be saved
     */
    protected void saveConfig() throws InvalidSettingsException {
        saveTableReadSettings();
        saveCsvSettings();
        m_config.setTableSpecConfig(getTableSpecConfig());
    }

    /**
     * Use {@link #loadAdditionalSettings(NodeSettingsRO, PortObjectSpec[])} instead of overriding this function.</br>
     * </br>
     * {@inheritDoc}
     */
    @Override
    protected final void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_config.loadInDialog(settings, specs);
        loadTableReadSettings();
        loadCSVSettings();
        showCardInCardLayout(EMPTY_CARD);
        loadAdditionalSettings(settings, specs);

        // enable/disable spinners
        controlSpinner(m_skipFirstLinesChecker, m_skipFirstLinesSpinner);
        controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner);
        controlSpinner(m_limitRowsChecker, m_limitRowsSpinner);
        controlSpinner(m_limitAnalysisChecker, m_limitAnalysisSpinner);

        if (m_config.hasTableSpecConfig()) {
            loadFromTableSpecConfig(m_config.getTableSpecConfig());
        }
    }

    /**
     * Loads additional settings.
     *
     * @see #loadAdditionalSettings(NodeSettingsRO, PortObjectSpec[])
     *
     * @param settings the settings to load
     * @param specs the input specs
     * @throws NotConfigurableException if a component cannot be configured
     */
    protected abstract void loadAdditionalSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException;

    /**
     * Fill in dialog components with TableReadConfig values
     */
    private void loadTableReadSettings() {
        // row limit options
        final TableReadConfig<CSVTableReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        m_hasColHeaderChecker.setSelected(tableReadConfig.useColumnHeaderIdx());
        m_hasRowIDChecker.setSelected(tableReadConfig.useRowIDIdx());

        if (m_prependSourceIdxToRowId != null) {
            m_prependSourceIdxToRowId.setSelected(tableReadConfig.prependSourceIdxToRowID());
            m_prependSourceIdxToRowId.setEnabled(tableReadConfig.useRowIDIdx());
        }

        m_allowShortDataRowsChecker.setSelected(tableReadConfig.allowShortRows());

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

        m_numberFormatDialog.load(csvReaderConfig);
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

    private void startFormatAutoDetection() {
        m_formatAutoDetectionSwingWorker = new CSVFormatAutoDetectionSwingWorker(this, m_pathSettings);

        setPreviewEnabled(false);

        setAutodetectComponentsEnabled(false);
        showCardInCardLayout(PROGRESS_BAR_CARD);
        m_startAutodetection.setText(AUTODETECT_CANCEL_LABEL);

        m_formatAutoDetectionSwingWorker.execute();
    }

    private void cancelFormatAutoDetection() {
        if (m_formatAutoDetectionSwingWorker != null) {
            m_formatAutoDetectionSwingWorker.cancel(true);
            m_formatAutoDetectionSwingWorker = null;
        }
        m_startAutodetection.setText(START_AUTODETECT_LABEL);
        resetUIafterAutodetection();
    }

    /**
     * Set the autodetection components enabled or disabled.
     *
     * @param enabled if the components should be enabled or disabled
     */
    protected void setAutodetectComponentsEnabled(final boolean enabled) {
        m_colDelimiterField.setEnabled(enabled);
        m_rowDelimiterField.setEnabled(enabled);
        m_quoteField.setEnabled(enabled);
        m_quoteEscapeField.setEnabled(enabled);
    }

    void resetUIafterAutodetection() {
        setAutodetectComponentsEnabled(true);
        showCardInCardLayout(EMPTY_CARD);
        m_startAutodetection.setText(AbstractCSVTableReaderNodeDialog.START_AUTODETECT_LABEL);
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

    boolean isSkipLines() {
        return m_skipFirstLinesChecker.isSelected();
    }

    long getNumLinesToSkip() {
        return (long)m_skipFirstLinesSpinner.getValue();
    }

    Charset getSelectedCharset() {
        final Optional<String> charsetName = m_encodingPanel.getSelectedCharsetName();
        return charsetName.isPresent() ? Charset.forName(charsetName.get()) : Charset.defaultCharset();
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

    @Override
    public void onClose() {
        cancelFormatAutoDetection();
        super.onClose();
    }
}
