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

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.knime.base.node.io.filereader.CharsetNamePanel;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.workflow.VariableType;
import org.knime.filehandling.core.defaultnodesettings.DialogComponentFileChooser2;
import org.knime.filehandling.core.defaultnodesettings.SettingsModelFileChooser2;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;

/**
 * Node dialog of the CSV reader prototype node.
 *
 * @author Temesgen H. Dadi, KNIME GmbH, Berlin, Germany
 */
final class CSVTableReaderNodeDialog extends NodeDialogPane {

    private final DialogComponentFileChooser2 m_filePanel;

    private final JRadioButton m_failOnDifferingSpecs = new JRadioButton("Fail if specs differ");

    private final JRadioButton m_union = new JRadioButton("Union");

    private final JRadioButton m_intersection = new JRadioButton("Intersection");

    private final JTextField m_colDelimiterField;

    private final JTextField m_rowDelimiterField;

    private final JTextField m_quoteField;

    private final JTextField m_quoteEscapeField;

    private final JTextField m_commentStartField;

    private final JCheckBox m_hasRowHeaderChecker;

    private final JCheckBox m_hasColHeaderChecker;

    private final JCheckBox m_allowShortLinesChecker;

    private final JCheckBox m_skipEmptyLinesChecker;

    private final JCheckBox m_replaceQuotedEmptyStringChecker;

    private final JCheckBox m_limitRowsChecker;

    private final JSpinner m_limitRowsSpinner;

    private final JCheckBox m_skipFirstRowsChecker;

    private final JSpinner m_skipFirstRowsSpinner;

    private final JCheckBox m_skipFirstLinesChecker;

    private final JSpinner m_skipFirstLinesSpinner;

    private final JCheckBox m_limitAnalysisChecker;

    private final JSpinner m_limitAnalysisSpinner;

    private final CharsetNamePanel m_encodingPanel;

    private final MultiTableReadConfig<CSVTableReaderConfig> m_config;

    /** Create new CsvTableReader dialog. */
    CSVTableReaderNodeDialog(final SettingsModelFileChooser2 fileChooserModel,
        final MultiTableReadConfig<CSVTableReaderConfig> config) {

        final FlowVariableModel fvm = createFlowVariableModel(
            new String[]{fileChooserModel.getConfigName(), SettingsModelFileChooser2.PATH_OR_URL_KEY},
            VariableType.StringType.INSTANCE);

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
        m_hasRowHeaderChecker = new JCheckBox("Has row header");
        m_hasColHeaderChecker = new JCheckBox("Has column header");
        m_allowShortLinesChecker = new JCheckBox("Support short lines");
        m_skipEmptyLinesChecker = new JCheckBox("Skip empty lines");
        m_replaceQuotedEmptyStringChecker = new JCheckBox("Replace empty quoted strings with missing values");

        m_skipFirstLinesChecker = new JCheckBox("Skip first lines ");
        m_skipFirstLinesSpinner = new JSpinner(new SpinnerNumberModel(skipOne, rowStart, rowEnd, stepSize));
        m_skipFirstLinesChecker
            .addChangeListener(e -> controlSpinner(m_skipFirstLinesChecker, m_skipFirstLinesSpinner));
        m_skipFirstLinesChecker.doClick();

        m_skipFirstRowsChecker = new JCheckBox("Skip first data rows ");
        m_skipFirstRowsSpinner = new JSpinner(new SpinnerNumberModel(skipOne, rowStart, rowEnd, stepSize));
        m_skipFirstRowsChecker.addChangeListener(e -> controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner));
        m_skipFirstRowsChecker.doClick();

        m_limitRowsChecker = new JCheckBox("Limit data rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitRowsChecker.addChangeListener(e -> controlSpinner(m_limitRowsChecker, m_limitRowsSpinner));
        m_limitRowsChecker.doClick();

        m_limitAnalysisChecker = new JCheckBox("Limit data rows scanned for spec. ");
        m_limitAnalysisSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitAnalysisChecker.addChangeListener(e -> controlSpinner(m_limitAnalysisChecker, m_limitAnalysisSpinner));
        m_limitAnalysisChecker.doClick();

        addTab("Options", initLayout());
        addTab("Limit Rows", getLimitRowsPanel());

        m_encodingPanel = new CharsetNamePanel(new FileReaderSettings());
        addTab("Encoding", m_encodingPanel);

        m_config = config;

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
     * Creates a {@link JPanel} filed with dialog components, including file chooser dialog, Spec merge options in case
     * of multiple files and options for reading CSV files.
     *
     * @return a {@link JPanel} filed with dialog components.
     */
    private JPanel initLayout() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = createAndInitGBC();
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(createFilePanel(), gbc);
        gbc.gridy++;
        panel.add(createSpecMergePanel(), gbc);
        gbc.gridy++;
        gbc.weighty = 1;
        panel.add(createOptionsPanel(), gbc);

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
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.insets = new Insets(0, 5, 0, 5);
        gbc.weightx = 0;
        specMergePanel.add(m_failOnDifferingSpecs, gbc);
        gbc.gridx = 1;
        specMergePanel.add(m_intersection, gbc);
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        specMergePanel.add(m_union, gbc);
        return specMergePanel;
    }

    private JPanel createOptionsPanel() {
        final GridBagConstraints gbc = createAndInitGBC();
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        optionsPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Reader options:"));

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        optionsPanel.add(getInFlowLayout(m_colDelimiterField, new JLabel("Column Delimiter ")), gbc);
        gbc.gridx += 1;
        optionsPanel.add(getInFlowLayout(m_rowDelimiterField, new JLabel("Row Delimiter ")), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        optionsPanel.add(new JPanel(), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0;
        optionsPanel.add(getInFlowLayout(m_quoteField, new JLabel("Quote Char ")), gbc);
        gbc.gridx += 1;
        gbc.weightx = 1;
        optionsPanel.add(getInFlowLayout(m_quoteEscapeField, new JLabel("Quote Escape Char ")), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        gbc.weightx = 0;
        optionsPanel.add(getInFlowLayout(m_commentStartField, new JLabel("Comment Char ")), gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        optionsPanel.add(m_hasColHeaderChecker, gbc);
        gbc.gridx += 1;
        optionsPanel.add(m_hasRowHeaderChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        optionsPanel.add(m_allowShortLinesChecker, gbc);

        gbc.gridx += 1;
        optionsPanel.add(m_skipEmptyLinesChecker, gbc);

        gbc.gridx = 0;
        gbc.gridy += 1;
        optionsPanel.add(m_replaceQuotedEmptyStringChecker, gbc);

        //empty panel to eat up extra space
        gbc.gridy += 1;
        gbc.gridx = 0;
        gbc.weighty = 1;
        optionsPanel.add(new JPanel(), gbc);
        return optionsPanel;
    }

    /**
     * Creates a {@link JPanel} filed with dialog components specific to limiting the number of rows that are read.
     *
     * @return a {@link JPanel} filed with dialog components.
     */
    private JPanel getLimitRowsPanel() {
        JPanel optionsPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.gridx = 0;
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.FIRST_LINE_START;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
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
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 1;
        return gbc;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_filePanel.saveSettingsTo(settings);
        saveTableReadSettings();
        saveCsvSettings();
        m_config.setSpecMergeMode(getSpecMergeMode());
        m_config.save(settings);
    }

    /**
     * Fill in the setting values in {@link TableReadConfig} using values from dialog.
     */
    private void saveTableReadSettings() {
        final TableReadConfig<CSVTableReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        tableReadConfig.setUseRowIDIdx(m_hasRowHeaderChecker.isSelected());
        tableReadConfig.setRowIDIdx(0);

        tableReadConfig.setUseColumnHeaderIdx(m_hasColHeaderChecker.isSelected());
        tableReadConfig.setColumnHeaderIdx(0);

        tableReadConfig.setSkipRows(m_skipFirstRowsChecker.isSelected());
        tableReadConfig.setNumRowsToSkip((Long)m_skipFirstRowsSpinner.getValue());

        tableReadConfig.setLimitRows(m_limitRowsChecker.isSelected());
        tableReadConfig.setMaxRows((Long)m_limitRowsSpinner.getValue());

        tableReadConfig.setLimitRowsForSpec(m_limitAnalysisChecker.isSelected());
        tableReadConfig.setMaxRowsForSpec((Long)m_limitAnalysisSpinner.getValue());

        tableReadConfig.setAllowShortRows(m_allowShortLinesChecker.isSelected());
        tableReadConfig.setSkipEmptyRows(m_skipEmptyLinesChecker.isSelected());

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

        csvReaderConfig.setSkipEmptyLines(m_skipEmptyLinesChecker.isSelected());
        csvReaderConfig.setReplaceEmptyWithMissing(m_replaceQuotedEmptyStringChecker.isSelected());

        FileReaderNodeSettings s = new FileReaderNodeSettings();
        m_encodingPanel.overrideSettings(s);
        csvReaderConfig.setCharSetName(s.getCharsetName());
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_config.loadInDialog(settings);

        m_filePanel.loadSettingsFrom(settings, specs);
        loadTableReadSettings();
        loadCSVSettings();
        setSpecMergeMode();
    }

    /**
     * Fill in dialog components with TableReadConfig values
     */
    private void loadTableReadSettings() {
        // row limit options
        final TableReadConfig<CSVTableReaderConfig> tableReadConfig = m_config.getTableReadConfig();

        m_hasColHeaderChecker.setSelected(tableReadConfig.useColumnHeaderIdx());
        m_hasRowHeaderChecker.setSelected(tableReadConfig.useRowIDIdx());

        m_allowShortLinesChecker.setSelected(tableReadConfig.allowShortRows());
        // TODO decide where to save setting. Univocity vs TableReadConfig
        m_skipEmptyLinesChecker.setSelected(tableReadConfig.skipEmptyRows());

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

        m_skipFirstLinesChecker.setSelected(csvReaderConfig.skipLines());
        m_skipFirstLinesChecker.setSelected(csvReaderConfig.skipLines());
        m_skipFirstLinesSpinner.setValue(csvReaderConfig.getNumLinesToSkip());

        m_skipEmptyLinesChecker.setSelected(csvReaderConfig.skipEmptyLines());

        m_replaceQuotedEmptyStringChecker.setSelected(csvReaderConfig.replaceEmptyWithMissing());

        FileReaderSettings fReadSettings = new FileReaderSettings();
        fReadSettings.setCharsetName(csvReaderConfig.getCharSetName());
        m_encodingPanel.loadSettings(fReadSettings);
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

}
