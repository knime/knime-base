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
 *   04.11.2020 (Lars Schweikardt, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.linereader;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.node.io.filereader.CharsetNamePanel;
import org.knime.base.node.io.filereader.FileReaderNodeSettings;
import org.knime.base.node.io.filereader.FileReaderSettings;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationSpecVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.paths.PathSettings;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Node dialog of the line reader node.
 *
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 */
final class LineReaderNodeDialog2 extends AbstractPathTableReaderNodeDialog<LineReaderConfig2, Class<?>> {

    private final DialogComponentReaderFileChooser m_sourceFilePanel;

    private final JRadioButton m_useFixColHeader;

    private final JRadioButton m_useFirstLineColHeader;

    private final JRadioButton m_skipEmptyLines;

    private final JRadioButton m_replaceEmpty;

    private final JRadioButton m_replaceEmptyByMissing;

    private final JCheckBox m_limitRowsChecker;

    private final JCheckBox m_failOnDiffSpecs;

    private final JSpinner m_limitRowsSpinner;

    private final JTextField m_columnHeaderField = new JTextField("######", 6);

    private final JTextField m_regexField = new JTextField("##########", 10);

    private final JTextField m_replaceEmptyField = new JTextField("##########", 10);

    private final JTextField m_rowHeaderPrefix = new JTextField("######", 6);

    private final JCheckBox m_regexChecker;

    private final CharsetNamePanel m_encodingPanel;

    private final DefaultMultiTableReadConfig<LineReaderConfig2, DefaultTableReadConfig<LineReaderConfig2>> m_config;

    private final PathSettings m_pathSettings;

    /**
     * Constructor.
     *
     * @param pathSettings the {@link PathSettings}
     * @param config the {@link DefaultMultiTableReadConfig}
     * @param multiReader the {@link MultiTableReadFactory}
     * @param productionPathProvider the {@link ProductionPathProvider}
     */
    LineReaderNodeDialog2(final PathSettings pathSettings,
        final DefaultMultiTableReadConfig<LineReaderConfig2, DefaultTableReadConfig<LineReaderConfig2>> config,
        final MultiTableReadFactory<Path, LineReaderConfig2, Class<?>> multiReader,
        final ProductionPathProvider<Class<?>> productionPathProvider) {
        super(multiReader, productionPathProvider, true);

        final SettingsModelReaderFileChooser fileChooserModel = (SettingsModelReaderFileChooser)pathSettings;
        final FlowVariableModel sourceFvm = createFlowVariableModel(
            Stream.concat(Stream.of(SettingsUtils.CFG_SETTINGS_TAB),
                Arrays.stream(fileChooserModel.getKeysForFSLocation()))
                .toArray(String[]::new),
            FSLocationSpecVariableType.INSTANCE);

        m_config = config;
        m_pathSettings = pathSettings;

        m_sourceFilePanel = new DialogComponentReaderFileChooser(fileChooserModel, "source_chooser", sourceFvm,
            FilterMode.FILE, FilterMode.FILES_IN_FOLDERS);
        m_sourceFilePanel.getSettingsModel().getFilterModeModel().addChangeListener(l -> failOnDiffSpecsListener());

        m_encodingPanel = new CharsetNamePanel(new FileReaderSettings());

        m_regexChecker = new JCheckBox("Match input against regex");
        m_regexChecker.addChangeListener(l -> m_regexField.setEnabled(m_regexChecker.isSelected()));

        final ButtonGroup colHeaderBtnGrp = new ButtonGroup();
        m_useFixColHeader = new JRadioButton("Use fix column header");
        colHeaderBtnGrp.add(m_useFixColHeader);
        m_useFirstLineColHeader = new JRadioButton("Use first line as column header");
        colHeaderBtnGrp.add(m_useFirstLineColHeader);
        m_useFixColHeader.addChangeListener(l -> m_columnHeaderField.setEnabled(m_useFixColHeader.isSelected()));

        m_failOnDiffSpecs = new JCheckBox("Fail on differing specs");

        final ButtonGroup emptyLineBtnGrp = new ButtonGroup();
        m_skipEmptyLines = new JRadioButton(EmptyLineMode.SKIP_EMPTY.getText());
        emptyLineBtnGrp.add(m_skipEmptyLines);
        m_replaceEmpty = new JRadioButton(EmptyLineMode.REPLACE_EMPTY.getText());
        emptyLineBtnGrp.add(m_replaceEmpty);
        m_replaceEmpty.addChangeListener(l -> m_replaceEmptyField.setEnabled(m_replaceEmpty.isSelected()));
        m_replaceEmptyByMissing = new JRadioButton(EmptyLineMode.REPLACE_BY_MISSING.getText());
        emptyLineBtnGrp.add(m_replaceEmptyByMissing);

        m_limitRowsChecker = new JCheckBox("Limit data rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(Long.valueOf(1000), Long.valueOf(0),
            Long.valueOf(Long.MAX_VALUE), Long.valueOf(1000)));
        m_limitRowsChecker.addActionListener(e -> controlSpinner(m_limitRowsChecker, m_limitRowsSpinner));

        m_regexChecker.doClick();
        m_useFixColHeader.doClick();
        m_replaceEmpty.doClick();

        registerPreviewChangeListeners();

        createDialogPanels();
    }

    /**
     * The {@link ChangeListener} for the fail on differing specs option
     */
    private void failOnDiffSpecsListener() {
        final boolean enable = m_sourceFilePanel.getSettingsModel().getFilterMode() != FilterMode.FILE;
        m_failOnDiffSpecs.setEnabled(enable);
    }

    /**
     * Register the listeners for the preview.
     */
    private void registerPreviewChangeListeners() {
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

        m_sourceFilePanel.getModel().addChangeListener(changeListener);

        m_useFixColHeader.addActionListener(actionListener);
        m_useFirstLineColHeader.addActionListener(actionListener);
        m_columnHeaderField.getDocument().addDocumentListener(documentListener);

        m_rowHeaderPrefix.getDocument().addDocumentListener(documentListener);

        m_replaceEmpty.addActionListener(actionListener);
        m_replaceEmptyField.getDocument().addDocumentListener(documentListener);

        m_skipEmptyLines.addActionListener(actionListener);

        m_replaceEmptyByMissing.addActionListener(actionListener);

        m_failOnDiffSpecs.addActionListener(actionListener);

        m_limitRowsChecker.addActionListener(actionListener);
        m_limitRowsSpinner.getModel().addChangeListener(changeListener);

        m_regexField.getDocument().addDocumentListener(documentListener);
        m_regexChecker.addActionListener(actionListener);

        m_encodingPanel.addChangeListener(changeListener);
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

    private void createDialogPanels() {
        addTab("Settings", createSettingsPanel());
        addTab("Advanced Settings", createAdvancedSettingsPanel());
        addTab("Encoding", createEncodingPanel());
    }

    /**
     * Creates a standard setup {@link GBCBuilder}.
     *
     * @return returns a {@link GBCBuilder}
     */
    private static final GBCBuilder createGBCBuilder() {
        return new GBCBuilder().resetPos().fillHorizontal().anchorFirstLineStart();
    }

    /**
     * Creates the {@link JPanel} for the advanced settings tabs.
     *
     * @return the advanced settings {@link JPanel}
     */
    private JPanel createAdvancedSettingsPanel() {
        final JPanel advPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1).anchorPageStart();
        advPanel.add(createEmptyLinePanel(), gbc.build());
        gbc.incY();
        advPanel.add(createRegexPanel(), gbc.build());
        gbc.incY();
        advPanel.add(createLimitRowsPanel(), gbc.build());
        gbc.setWeightY(1).resetX().widthRemainder().incY().insetBottom(0).fillBoth();
        advPanel.add(createPreview(), gbc.build());
        return advPanel;
    }

    private JPanel createEmptyLinePanel() {
        final JPanel emptyLinePanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal();
        emptyLinePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Empty lines"));
        emptyLinePanel.add(m_skipEmptyLines, gbc.build());
        gbc.incY();
        emptyLinePanel.add(m_replaceEmptyByMissing, gbc.build());
        gbc.incY();
        emptyLinePanel.add(m_replaceEmpty, gbc.build());
        gbc.incX().setWeightX(1);
        emptyLinePanel.add(m_replaceEmptyField, gbc.build());

        return emptyLinePanel;
    }

    /**
     * Creates the regular expression {@link JPanel}.
     *
     * @return the regular expression{@link JPanel}
     */
    private JPanel createRegexPanel() {
        final JPanel regexPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal();
        regexPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Regular expression"));
        regexPanel.add(m_regexChecker, gbc.build());
        gbc.incX().setWeightX(1);
        regexPanel.add(m_regexField, gbc.build());

        return regexPanel;
    }

    /**
     * Creates the {@link JPanel} for the limit rows tab.
     *
     * @return the limit rows panel {@link JPanel}
     */
    private JPanel createLimitRowsPanel() {
        final JPanel limitPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillNone();
        limitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Limit rows"));
        limitPanel.add(m_limitRowsChecker, gbc.build());
        gbc.incX().setWeightX(1);
        limitPanel.add(m_limitRowsSpinner, gbc.build());
        return limitPanel;
    }

    /**
     * Creates the column header {@link JPanel}.
     *
     * @return the column header {@link JPanel}
     */
    private JPanel createColHeaderPanel() {
        final JPanel colHeaderPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal();
        colHeaderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Column Header"));
        colHeaderPanel.add(m_useFixColHeader, gbc.build());
        gbc.incX();
        colHeaderPanel.add(m_columnHeaderField, gbc.build());
        gbc.incX().setWeightX(1);
        colHeaderPanel.add(m_useFirstLineColHeader, gbc.build());

        return colHeaderPanel;
    }

    private JPanel createRowHeaderPrefixPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().insetBottom(5);
        panel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Row Header"));
        panel.add(new JLabel("Row header prefix"), gbc.insetTop(3).build());
        panel.add(m_rowHeaderPrefix, gbc.incX().insetTop(0).insetLeft(10).build());
        panel.add(new JPanel(), gbc.incX().setWeightX(1.0).build());
        return panel;
    }

    /**
     * Creates the {@link JPanel} for the Settings tab.
     *
     * @return the settings panel {@link JPanel}
     */
    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1).anchorPageStart();
        panel.add(createSourcePanel(), gbc.build());
        panel.add(createFailOnDifferingPanel(), gbc.incY().build());
        panel.add(createRowHeaderPrefixPanel(), gbc.incY().build());
        panel.add(createColHeaderPanel(), gbc.incY().build());
        gbc.setWeightY(1).resetX().widthRemainder().incY().fillBoth();
        panel.add(createPreview(), gbc.build());

        return panel;
    }

    /**
     * Creates the source file {@link JPanel}.
     *
     * @return the source file {@link JPanel}
     */
    private JPanel createSourcePanel() {
        final JPanel sourcePanel = new JPanel(new GridBagLayout());
        sourcePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        GBCBuilder gbc = createGBCBuilder().setWeightX(1);
        sourcePanel.add(m_sourceFilePanel.getComponentPanel(), gbc.build());
        return sourcePanel;
    }

    /**
     * Creates the {@link JPanel} for the fail on differing specs option.
     *
     * @return the {@link JPanel} for the fail on differing specs option
     */
    private JPanel createFailOnDifferingPanel() {
        final JPanel failOnDiffPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1);
        failOnDiffPanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options for multiple files"));
        failOnDiffPanel.add(m_failOnDiffSpecs, gbc.build());

        return failOnDiffPanel;
    }

    /**
     * Creates the encoding {@link JPanel}.
     *
     * @return the enconding {@link JPanel}
     */
    private JPanel createEncodingPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = createGBCBuilder();
        panel.add(m_encodingPanel, gbc.build());
        gbc.incY().fillBoth().weight(1, 1);
        panel.add(createPreview(), gbc.build());

        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_sourceFilePanel.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        m_config.setFailOnDifferingSpecs(m_failOnDiffSpecs.isSelected());
        saveTableReadSettings(m_config.getTableReadConfig());
        saveLineReaderSettings(m_config.getTableReadConfig().getReaderSpecificConfig());

        m_config.saveInDialog(settings);
    }

    @Override
    protected MultiTableReadConfig<LineReaderConfig2> getConfig() throws InvalidSettingsException {
        m_config.setFailOnDifferingSpecs(m_failOnDiffSpecs.isSelected());
        saveTableReadSettings(m_config.getTableReadConfig());
        saveLineReaderSettings(m_config.getTableReadConfig().getReaderSpecificConfig());

        return m_config;
    }

    @Override
    protected ReadPathAccessor createReadPathAccessor() {
        return m_pathSettings.createReadPathAccessor();
    }

    @Override
    protected void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sourceFilePanel.loadSettingsFrom(SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB), specs);

        final DefaultTableReadConfig<LineReaderConfig2> tableReadConfig = m_config.getTableReadConfig();
        final LineReaderConfig2 lineReaderConfig = m_config.getReaderSpecificConfig();

        m_config.loadInDialog(settings, specs);

        m_failOnDiffSpecs.setSelected(m_config.failOnDifferingSpecs());

        m_rowHeaderPrefix.setText(tableReadConfig.getPrefixForGeneratedRowIDs());

        m_useFirstLineColHeader.setSelected(tableReadConfig.useColumnHeaderIdx());
        m_columnHeaderField.setText(lineReaderConfig.getColumnHeaderName());
        m_useFixColHeader.setSelected(!tableReadConfig.useColumnHeaderIdx());

        m_skipEmptyLines.setSelected(lineReaderConfig.getReplaceEmptyMode() == EmptyLineMode.SKIP_EMPTY);
        m_replaceEmptyByMissing.setSelected(lineReaderConfig.getReplaceEmptyMode() == EmptyLineMode.REPLACE_BY_MISSING);
        m_replaceEmpty.setSelected(lineReaderConfig.getReplaceEmptyMode() == EmptyLineMode.REPLACE_EMPTY);
        m_replaceEmptyField.setText(lineReaderConfig.getEmptyLineReplacement());

        m_regexField.setText(lineReaderConfig.getRegex());
        m_regexChecker.setSelected(lineReaderConfig.useRegex());

        m_limitRowsChecker.setSelected(tableReadConfig.limitRows());
        m_limitRowsSpinner.setValue(tableReadConfig.getMaxRows());
        controlSpinner(m_limitRowsChecker, m_limitRowsSpinner);

        final FileReaderSettings fReadSettings = new FileReaderSettings();
        fReadSettings.setCharsetName(lineReaderConfig.getCharSetName());
        m_encodingPanel.loadSettings(fReadSettings);

    }

    /**
     * Saves the {@link LineReaderConfig2}.
     *
     * @param config the {@link LineReaderConfig2}
     */
    private void saveLineReaderSettings(final LineReaderConfig2 config) {
        final FileReaderNodeSettings s = new FileReaderNodeSettings();
        m_encodingPanel.overrideSettings(s);
        config.setCharSetName(s.getCharsetName());
        config.setColumnHeaderName(m_columnHeaderField.getText());
        config.setRegex(m_regexField.getText());
        config.setUseRegex(m_regexChecker.isSelected());
        config.setEmptyLineReplacement(m_replaceEmptyField.getText());
        config.setEmptyLineMode(getSelectedEmptyLineMode());
    }

    /**
     * Method which returns the {@link EmptyLineMode} which is currently selected.
     */
    private EmptyLineMode getSelectedEmptyLineMode() {
        if (m_skipEmptyLines.isSelected()) {
            return EmptyLineMode.SKIP_EMPTY;
        } else if (m_replaceEmpty.isSelected()) {
            return EmptyLineMode.REPLACE_EMPTY;
        } else {
            return EmptyLineMode.REPLACE_BY_MISSING;
        }
    }

    /**
     * Saves the {@link DefaultTableReadConfig}.
     *
     * @param config the {@link DefaultTableReadConfig}
     */
    private void saveTableReadSettings(final DefaultTableReadConfig<LineReaderConfig2> config) {
        config.setUseRowIDIdx(false);
        config.setRowIDIdx(-1);
        config.setPrefixForGeneratedRowIds(m_rowHeaderPrefix.getText());
        config.setUseColumnHeaderIdx(m_useFirstLineColHeader.isSelected());
        config.setColumnHeaderIdx(0);
        config.setSkipEmptyRows(m_skipEmptyLines.isSelected());
        config.setLimitRows(m_limitRowsChecker.isSelected());
        config.setMaxRows((Long)m_limitRowsSpinner.getValue());
    }

    @Override
    public void onClose() {
        m_sourceFilePanel.onClose();
    }
}
