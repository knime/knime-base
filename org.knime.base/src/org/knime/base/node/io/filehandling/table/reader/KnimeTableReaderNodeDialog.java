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
 *   27 May 2021 (moditha.hewasinghge): created
 */
package org.knime.base.node.io.filehandling.table.reader;

import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Stream;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeListener;

import org.knime.base.node.preproc.manipulator.TableManipulatorConfig;
import org.knime.core.data.DataType;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.connections.FSPath;
import org.knime.filehandling.core.data.location.variable.FSLocationSpecVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.dialog.SourceIdentifierColumnPanel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AbstractPathTableReaderNodeDialog;
import org.knime.filehandling.core.util.GBCBuilder;
import org.knime.filehandling.core.util.SettingsUtils;

/**
 * Node dialog of the table reader node.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 */
final class KnimeTableReaderNodeDialog extends AbstractPathTableReaderNodeDialog<TableManipulatorConfig, DataType> {

    private static final String TRANSFORMATION_TAB = "Transformation";

    private final DialogComponentReaderFileChooser m_sourceFilePanel;

    private final JCheckBox m_limitRowsChecker;

    private final JSpinner m_limitRowsSpinner;

    private final JCheckBox m_skipFirstRowsChecker;

    private final JSpinner m_skipFirstRowsSpinner;

    private final JCheckBox m_supportChangingFileSchemas = new JCheckBox("Support changing file schemas");

    private final KnimeTableMultiTableReadConfig m_config;

    private final SettingsModelReaderFileChooser m_settingsModelReaderFileChooser;

    private final JCheckBox m_failOnDifferingSpecs;

    private final JCheckBox m_useRowID;

    private final JCheckBox m_prependTableIdxToRowID = new JCheckBox("Prepend table index to row ID");

    private final SourceIdentifierColumnPanel m_pathColumnPanel = new SourceIdentifierColumnPanel("Path");

    /**
     * Constructor.
     *
     * @param settingsModelFileChooser the {@link SettingsModelReaderFileChooser}
     * @param config the {@link DefaultMultiTableReadConfig}
     * @param multiReader the {@link MultiTableReadFactory}
     * @param productionPathProvider the {@link ProductionPathProvider}
     */
    KnimeTableReaderNodeDialog(final SettingsModelReaderFileChooser settingsModelFileChooser,
        final KnimeTableMultiTableReadConfig config,
        final MultiTableReadFactory<FSPath, TableManipulatorConfig, DataType> multiReader,
        final ProductionPathProvider<DataType> productionPathProvider) {
        super(multiReader, productionPathProvider, true);



        Long stepSize = Long.valueOf(1);
        Long rowStart = Long.valueOf(0);
        Long rowEnd = Long.valueOf(Long.MAX_VALUE);
        Long skipOne = Long.valueOf(1);
        Long initLimit = Long.valueOf(50);

        m_settingsModelReaderFileChooser = settingsModelFileChooser;

        final FlowVariableModel sourceFvm = createFlowVariableModel(
            Stream.concat(Stream.of(SettingsUtils.CFG_SETTINGS_TAB),
                Arrays.stream(m_settingsModelReaderFileChooser.getKeysForFSLocation())).toArray(String[]::new),
            FSLocationSpecVariableType.INSTANCE);

        m_config = config;

        m_sourceFilePanel =
            new DialogComponentReaderFileChooser(m_settingsModelReaderFileChooser, "source_chooser", sourceFvm);

        m_skipFirstRowsChecker = new JCheckBox("Skip first data rows ");
        m_skipFirstRowsSpinner = new JSpinner(new SpinnerNumberModel(skipOne, rowStart, rowEnd, stepSize));
        m_skipFirstRowsChecker.addActionListener(e -> controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner));
        m_skipFirstRowsChecker.doClick();

        m_limitRowsChecker = new JCheckBox("Limit data rows ");
        m_limitRowsSpinner = new JSpinner(new SpinnerNumberModel(initLimit, rowStart, rowEnd, initLimit));
        m_limitRowsChecker.addActionListener(e -> controlSpinner(m_limitRowsChecker, m_limitRowsSpinner));
        m_limitRowsChecker.doClick();

        m_supportChangingFileSchemas.addActionListener(e -> updateTransformationTabEnabledStatus());
        m_failOnDifferingSpecs = new JCheckBox("Fail if specs differ");
        m_sourceFilePanel.getSettingsModel().getFilterModeModel()
            .addChangeListener(l -> toggleFailOnDifferingCheckBox());

        m_useRowID = new JCheckBox("Use existing row ID");
        m_useRowID.addActionListener(l -> m_prependTableIdxToRowID.setEnabled(m_useRowID.isSelected()));

        registerPreviewChangeListeners();

        createDialogPanels();
    }

    private void updateTransformationTabEnabledStatus() {
        setEnabled(!m_supportChangingFileSchemas.isSelected(), TRANSFORMATION_TAB);
    }

    private void toggleFailOnDifferingCheckBox() {
        final boolean enable = m_sourceFilePanel.getSettingsModel().getFilterMode() != FilterMode.FILE;
        m_failOnDifferingSpecs.setEnabled(enable);
    }

    private void registerPreviewChangeListeners() {
        final ActionListener actionListener = l -> configChanged();
        final ChangeListener changeListener = l -> configChanged();

        m_sourceFilePanel.getModel().addChangeListener(changeListener);

        m_limitRowsChecker.addActionListener(actionListener);
        m_skipFirstRowsChecker.addActionListener(actionListener);

        m_skipFirstRowsSpinner.getModel().addChangeListener(changeListener);
        m_limitRowsSpinner.getModel().addChangeListener(changeListener);

        m_supportChangingFileSchemas.addActionListener(actionListener);
        m_failOnDifferingSpecs.addActionListener(actionListener);

        m_useRowID.addActionListener(actionListener);
        m_prependTableIdxToRowID.addActionListener(actionListener);

        m_pathColumnPanel.addChangeListener(changeListener);

    }

    private static void controlSpinner(final JCheckBox checker, final JSpinner spinner) {
        spinner.setEnabled(checker.isSelected());
    }

    private void createDialogPanels() {
        addTab("Settings", createSettingsPanel());
        addTab(TRANSFORMATION_TAB, createTransformationTab());
        addTab("Advanced Settings", createAdvancedSettingsPanel());
    }

    private static final GBCBuilder createGBCBuilder() {
        return new GBCBuilder().resetPos().fillHorizontal().anchorFirstLineStart();
    }

    private JPanel createAdvancedSettingsPanel() {
        final JPanel advPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1).anchorPageStart();
        advPanel.add(createLimitRowsPanel(), gbc.build());
        advPanel.add(createDataRowsSpecLimitPanel(), gbc.incY().build());
        advPanel.add(createSpecMergePanel(), gbc.incY().build());
        advPanel.add(m_pathColumnPanel, gbc.incY().build());
        gbc.setWeightY(1).resetX().widthRemainder().incY().insetBottom(0).fillBoth();
        advPanel.add(createPreview(), gbc.build());
        return advPanel;
    }

    private JPanel createLimitRowsPanel() {
        final JPanel limitPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillNone();
        limitPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Limit rows"));
        limitPanel.add(m_skipFirstRowsChecker, gbc.build());
        gbc.incX().setWeightX(1);
        limitPanel.add(m_skipFirstRowsSpinner, gbc.build());
        gbc.incY();
        gbc.setX(0).setWeightX(0);
        limitPanel.add(m_limitRowsChecker, gbc.build());
        gbc.incX().setWeightX(1);
        limitPanel.add(m_limitRowsSpinner, gbc.build());
        return limitPanel;
    }

    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillHorizontal().setWeightX(1).anchorPageStart();
        panel.add(createSourcePanel(), gbc.build());
        panel.add(createRowIDPanel(), gbc.incY().build());
        gbc.setWeightY(1).resetX().widthRemainder().incY().fillBoth();
        panel.add(createPreview(), gbc.build());
        return panel;
    }

    private JComponent createRowIDPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Row ID handling"));
        final GBCBuilder gbc = new GBCBuilder().resetPos().anchorFirstLineStart().fillHorizontal();
        panel.add(m_useRowID, gbc.incY().build());
        panel.add(m_prependTableIdxToRowID, gbc.incX().insetLeft(10).build());
        panel.add(new JPanel(), gbc.setWeightX(1.0).incX().build());
        return panel;
    }

    private JPanel createDataRowsSpecLimitPanel() {
        final JPanel specLimitPanel = new JPanel(new GridBagLayout());
        GBCBuilder gbc = createGBCBuilder().fillNone();
        specLimitPanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Table specification"));
        specLimitPanel.add(m_supportChangingFileSchemas, gbc.build());
        verticalFillPanel(specLimitPanel, gbc);
        return specLimitPanel;
    }

    private JPanel createSpecMergePanel() {
        final JPanel specMergePanel = new JPanel(new GridBagLayout());
        specMergePanel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Options for multiple files"));
        GBCBuilder gbc = createGBCBuilder().fillNone();
        specMergePanel.add(m_failOnDifferingSpecs, gbc.build());
        verticalFillPanel(specMergePanel, gbc);
        return specMergePanel;
    }

    private static void verticalFillPanel(final JPanel panel, final GBCBuilder gbc) {
        gbc.incX().setWeightX(1);
        gbc.fillHorizontal();
        panel.add(Box.createVerticalBox(), gbc.build());
    }

    private JPanel createSourcePanel() {
        final JPanel sourcePanel = new JPanel(new GridBagLayout());
        sourcePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Input location"));
        GBCBuilder gbc = createGBCBuilder().setWeightX(1);
        sourcePanel.add(m_sourceFilePanel.getComponentPanel(), gbc.build());
        return sourcePanel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        super.saveSettingsTo(settings);
        m_sourceFilePanel.saveSettingsTo(SettingsUtils.getOrAdd(settings, SettingsUtils.CFG_SETTINGS_TAB));
        saveTableReadSettings(m_config.getTableReadConfig());
        m_config.saveInDialog(settings);
    }

    @Override
    protected KnimeTableMultiTableReadConfig getConfig() throws InvalidSettingsException {
        saveTableReadSettings(m_config.getTableReadConfig());
        return m_config;
    }

    @Override
    protected ReadPathAccessor createReadPathAccessor() {
        return m_settingsModelReaderFileChooser.createReadPathAccessor();
    }

    @Override
    protected KnimeTableMultiTableReadConfig loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sourceFilePanel.loadSettingsFrom(SettingsUtils.getOrEmpty(settings, SettingsUtils.CFG_SETTINGS_TAB), specs);

        final DefaultTableReadConfig<TableManipulatorConfig> tableReadConfig = m_config.getTableReadConfig();

        m_config.loadInDialog(settings, specs);

        m_limitRowsChecker.setSelected(tableReadConfig.limitRows());
        m_limitRowsSpinner.setValue(tableReadConfig.getMaxRows());
        m_skipFirstRowsChecker.setSelected(tableReadConfig.skipRows());
        m_skipFirstRowsSpinner.setValue(tableReadConfig.getNumRowsToSkip());

        controlSpinner(m_limitRowsChecker, m_limitRowsSpinner);
        controlSpinner(m_skipFirstRowsChecker, m_skipFirstRowsSpinner);

        m_supportChangingFileSchemas.setSelected(!m_config.saveTableSpecConfig());
        updateTransformationTabEnabledStatus();

        m_failOnDifferingSpecs.setSelected(m_config.failOnDifferingSpecs());
        toggleFailOnDifferingCheckBox();
        m_pathColumnPanel.load(m_config.prependItemIdentifierColumn(), m_config.getItemIdentifierColumnName());

        final boolean useRowIDIdx = m_config.getTableReadConfig().useRowIDIdx();
        m_useRowID.setSelected(useRowIDIdx);
        m_prependTableIdxToRowID.setSelected(m_config.getTableReadConfig().prependSourceIdxToRowID());
        m_prependTableIdxToRowID.setEnabled(useRowIDIdx);

        return m_config;

    }

    private void saveTableReadSettings(final DefaultTableReadConfig<TableManipulatorConfig> config) {
        config.setColumnHeaderIdx(0);

        config.setLimitRows(m_limitRowsChecker.isSelected());
        config.setMaxRows((Long)m_limitRowsSpinner.getValue());
        config.setSkipRows(m_skipFirstRowsChecker.isSelected());
        config.setNumRowsToSkip((Long)m_skipFirstRowsSpinner.getValue());
        config.setUseRowIDIdx(m_useRowID.isSelected());
        config.setPrependSourceIdxToRowId(m_prependTableIdxToRowID.isSelected());

        final boolean saveTableSpecConfig = !m_supportChangingFileSchemas.isSelected();
        m_config.setSaveTableSpecConfig(saveTableSpecConfig);
        m_config.setTableSpecConfig(saveTableSpecConfig ? getTableSpecConfig() : null);
        m_config.setFailOnDifferingSpecs(m_failOnDifferingSpecs.isSelected());
        m_config.setPrependItemIdentifierColumn(m_pathColumnPanel.isPrependSourceIdentifierColumn());
        m_config.setItemIdentifierColumnName(m_pathColumnPanel.getSourceIdentifierColumnName());
    }

    @Override
    public void onClose() {
        m_sourceFilePanel.onClose();
        super.onClose();
    }
}
