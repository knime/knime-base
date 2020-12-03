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
 *   Aug 14, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.manipulator;

import java.awt.GridBagLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;

import org.knime.base.node.preproc.manipulator.table.DataTableBackedBoundedTable;
import org.knime.base.node.preproc.manipulator.table.EmptyTable;
import org.knime.base.node.preproc.manipulator.table.Table;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.DataAwareNodeDialogPane;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.node.table.reader.DefaultMultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.StorableMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AnalysisComponentModel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.GenericItemAccessor;
import org.knime.filehandling.core.node.table.reader.preview.dialog.TableReaderPreviewModel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.TableReaderPreviewTransformationCoordinator;
import org.knime.filehandling.core.node.table.reader.preview.dialog.TableReaderPreviewView;
import org.knime.filehandling.core.node.table.reader.preview.dialog.transformer.TableTransformationPanel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.transformer.TableTransformationTableModel;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.util.CheckNodeContextUtil;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Table manipulator implementation of a {@link NodeDialogPane}.</br>
 * It takes care of creating and managing the table preview.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @author Tobias Koetter, KNIME GmbH, Konstanz, Germany
 */
public class TableManipulatorNodeDialog extends DataAwareNodeDialogPane {
    /**A dummy table that has no columns and no rows.*/
    private static final Table DUMMY_TABLE = new EmptyTable(new DataTableSpec("DUMMY"));

    private static final class TableAccessor implements GenericItemAccessor<Table> {

        private List<Table> m_tables;

        private TableAccessor(final List<Table> tables) {
            m_tables = tables;
        }

        private void setTables(final List<Table> tables) {
            m_tables = tables;
        }

        @Override
        public void close() throws IOException {
            //nothing to close
        }

        @Override
        public List<Table> getItems(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return m_tables;
        }

        @Override
        public Table getRootItem(final Consumer<StatusMessage> statusMessageConsumer)
            throws IOException, InvalidSettingsException {
            return DUMMY_TABLE;
        }
    }

    private final TableReaderPreviewTransformationCoordinator<Table, TableManipulatorConfig, DataType> m_coordinator;

    private final List<TableReaderPreviewView> m_previews = new ArrayList<>();

    private final TableReaderPreviewModel m_previewModel;

    private final TableTransformationPanel m_specTransformer;

    private final boolean m_disableIOComponents;

    private boolean m_ignoreEvents = false;

    private final StorableMultiTableReadConfig<TableManipulatorConfig> m_config;

    private final TableAccessor m_itemAccessor = new TableAccessor(Collections.<Table>emptyList());

    private final JCheckBox m_useRowID;

    private final JCheckBox m_prependTableIdxToRowID = new JCheckBox("Prepend table index to row ID");

    TableManipulatorNodeDialog() {
        m_config = TableManipulatorNodeModel.createConfig();
        final DefaultMultiTableReadFactory<Table, TableManipulatorConfig, DataType, DataValue> readFactory =
                TableManipulatorNodeModel.createReadFactory();
        final ProductionPathProvider<DataType> productionPathProvider =
                TableManipulatorNodeModel.createProductionPathProvider();
        final AnalysisComponentModel analysisComponentModel = new AnalysisComponentModel();
        final TableReaderPreviewModel previewModel = new TableReaderPreviewModel(analysisComponentModel);
        m_previewModel = previewModel;
        final TableTransformationTableModel<DataType> transformationModel =
            new TableTransformationTableModel<>(productionPathProvider::getDefaultProductionPath);
        m_coordinator = new TableReaderPreviewTransformationCoordinator<>(readFactory, transformationModel,
            analysisComponentModel, previewModel, this::getConfig, this::getReadPathAccessor);
        m_specTransformer = new TableTransformationPanel(transformationModel,
            t -> productionPathProvider.getAvailableProductionPaths((DataType)t), true, true);
        m_disableIOComponents = CheckNodeContextUtil.isRemoteWorkflowContext();
        m_useRowID = new JCheckBox("Use existing row ID");
        m_useRowID.addActionListener(l -> configChanged());
        m_useRowID.addActionListener(l -> m_prependTableIdxToRowID.setEnabled(m_useRowID.isSelected()));
        m_prependTableIdxToRowID.addActionListener(l -> configChanged());
        addTab("Settings", createSettingsPanel());
    }

    GenericItemAccessor<Table> getReadPathAccessor() {
        return m_itemAccessor;
    }

    private JPanel createSettingsPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().resetPos().anchorFirstLineStart().fillHorizontal().setWeightX(1.0);
        panel.add(createRowIDPanel(), gbc.incY().build());
        panel.add(createTransformationTab(), gbc.fillBoth().setWeightY(1.0).incY().build());
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

    /**
     * Use this method to notify the dialog that the reading switched from single file to multiple files or vice versa.
     *
     * @param readingMultipleFiles {@code true} if potentially multiple files are read
     */
    protected final void setReadingMultipleFiles(final boolean readingMultipleFiles) {
        m_specTransformer.setColumnFilterModeEnabled(readingMultipleFiles);
    }

    /**
     * Enables/disables all previews created with {@link #createPreview()}.
     *
     * @param enabled {@code true} if enabled, {@code false} otherwise
     */
    protected final void setPreviewEnabled(final boolean enabled) {
        for (TableReaderPreviewView preview : m_previews) {
            preview.setEnabled(enabled);
        }
    }

    /**
     * Creates a {@link TableReaderPreviewView} that is synchronized with all other previews created by this method.
     * This means that scrolling in one preview will scroll to the same position in all other previews.
     *
     * @return a {@link TableReaderPreviewView}
     */
    protected final TableReaderPreviewView createPreview() {
        final TableReaderPreviewView preview = new TableReaderPreviewView(m_previewModel);
        m_previews.add(preview);
        preview.addScrollListener(this::updateScrolling);
        return preview;
    }

    /**
     * Convenience method that creates a {@link JSplitPane} containing the {@link TableTransformationPanel} and a
     * {@link TableReaderPreviewView}. NOTE: If this method is called multiple times, then the
     * {@link TableTransformationPanel} will only be shown in the {@link JSplitPane} created by the latest call.
     *
     * @return a {@link JSplitPane} containing the {@link TableTransformationPanel} and a {@link TableReaderPreviewView}
     */
    protected final JSplitPane createTransformationTab() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
        splitPane.setLeftComponent(getTransformationPanel());
        splitPane.setRightComponent(createPreview());
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(15);
        return splitPane;
    }

    private void updateScrolling(final ChangeEvent changeEvent) {
        final TableReaderPreviewView updatedView = (TableReaderPreviewView)changeEvent.getSource();
        for (TableReaderPreviewView preview : m_previews) {
            if (preview != updatedView) {
                preview.updateViewport(updatedView);
            }
        }
    }

    /**
     * Returns the {@link TableTransformationPanel} that allows to alter the table structure.
     *
     * @return the {@link TableTransformationPanel}
     */
    protected final TableTransformationPanel getTransformationPanel() {
        return m_specTransformer;
    }

    /**
     * Should be called by inheriting classes whenever the config changed i.e. if the user interacts with the dialog.
     */
    protected final void configChanged() {
        if (areIOComponentsDisabled()) {
            m_coordinator.setDisabledInRemoteJobViewInfo();
        } else if (!areEventsIgnored()) {
            m_coordinator.configChanged();
        }
    }

    /**
     * Sets whether this dialog should react to calls of {@link #configChanged()}. Call when loading the settings to
     * avoid unnecessary I/O due to many calls to {@link #configChanged()}.
     *
     * @param ignoreEvents whether events should be ignored or not
     */
    protected final void ignoreEvents(final boolean ignoreEvents) {
        m_ignoreEvents = ignoreEvents;
    }

    /**
     * Indicates whether events should be ignored. If this returns {@code true}, calls to {@link #configChanged()} won't
     * have any effect.
     *
     * @return {@code true} if {@link #configChanged()} doesn't react to calls
     */
    protected final boolean areEventsIgnored() {
        return m_ignoreEvents;
    }

    /**
     * Indicates whether the components doing IO, such as the preview, are disabled (by default when opened in remote
     * job view).
     *
     * @return if IO components are disabled
     */
    protected boolean areIOComponentsDisabled() {
        return m_disableIOComponents;
    }

    /**
     * Method to load the preview from the stored {@link DefaultTableSpecConfig}.
     *
     * @param genericTableSpecConfig to load from
     */
    private void loadFromTableSpecConfig(final TableSpecConfig genericTableSpecConfig) {
        final TableTransformation<DataType> transformationModel = genericTableSpecConfig.getTransformationModel();
        m_coordinator.load(transformationModel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_specTransformer.commitChanges();
        saveConfig();
        m_config.saveInModel(settings);
    }

    /**
     * Save the current config.
     *
     * @throws InvalidSettingsException if a setting could not be saved
     */
    protected void saveConfig() throws InvalidSettingsException {
        saveTableReadSettings();
        m_config.setTableSpecConfig(getTableSpecConfig());
    }

    /**
     * Fill in the setting values in {@link TableReadConfig} using values from dialog.
     */
    private void saveTableReadSettings() {
        final DefaultTableReadConfig<TableManipulatorConfig> tableReadConfig =
                (DefaultTableReadConfig<TableManipulatorConfig>)m_config.getTableReadConfig();



        //TODO: true throws mapping exception because of strange index mapper
        tableReadConfig.setUseRowIDIdx(m_useRowID.isSelected());
        tableReadConfig.setRowIDIdx(0);

        tableReadConfig.setPrependSourceIdxToRowId(m_prependTableIdxToRowID.isSelected());

        tableReadConfig.setUseColumnHeaderIdx(false);
        tableReadConfig.setColumnHeaderIdx(0);

        tableReadConfig.setSkipRows(false);
        tableReadConfig.setNumRowsToSkip(0);

        tableReadConfig.setLimitRows(false);
        tableReadConfig.setMaxRows(0);

        tableReadConfig.setLimitRowsForSpec(false);
        tableReadConfig.setMaxRowsForSpec(0);

        tableReadConfig.setAllowShortRows(false);
    }

    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        loadSettings(settings, specs, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObject[] input)
            throws NotConfigurableException {
        final DataTableSpec[] specs = new DataTableSpec[input.length];
        final BufferedDataTable[] tables = new BufferedDataTable[input.length];
        for (int i = 0, length = specs.length; i < length; i++) {
            final BufferedDataTable table = (BufferedDataTable)input[i];
            specs[i] = table.getDataTableSpec();
            tables[i] = table;
        }
        loadSettings(settings, specs, tables);
    }

    private void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs,
        final BufferedDataTable[] input)
            throws NotConfigurableException {
        ignoreEvents(true);
        setPreviewEnabled(false);

        m_config.loadInDialog(settings, specs);
        if (m_config.hasTableSpecConfig()) {
            loadFromTableSpecConfig(m_config.getTableSpecConfig());
        }
        final boolean useRowIDIdx = m_config.getTableReadConfig().useRowIDIdx();
        m_useRowID.setSelected(useRowIDIdx);
        m_prependTableIdxToRowID.setSelected(m_config.getTableReadConfig().prependSourceIdxToRowID());
        m_prependTableIdxToRowID.setEnabled(useRowIDIdx);
        final List<Table> rowInputs = new ArrayList<>(specs.length);
        if (input != null ) {
            for (BufferedDataTable table : input) {
                rowInputs.add(new DataTableBackedBoundedTable(table));
            }
        } else {
            for (PortObjectSpec spec : specs) {
                rowInputs.add(new EmptyTable((DataTableSpec)spec));
            }
        }
        m_itemAccessor.setTables(rowInputs);

        ignoreEvents(false);
        refreshPreview(true);
    }

    /**
     * Retrieves the currently configured {@link DefaultTableSpecConfig} or {@code null} if none is available e.g. if
     * the current settings are invalid and thus no preview could be loaded.
     *
     * @return the currently configured {@link DefaultTableSpecConfig} or {@code null} if none is available
     */
    protected final TableSpecConfig getTableSpecConfig() {
        return m_coordinator.getTableSpecConfig();
    }

    /**
     * This method must return the current {@link MultiTableReadConfig}. It is used to load the preview, so please make
     * sure that all settings are stored in the config, otherwise the preview will be incorrect.</br>
     * {@link RuntimeException} should be wrapped into {@link InvalidSettingsException} if they indicate an invalid
     * configuration.
     *
     * @return the current configuration
     * @throws InvalidSettingsException if the settings are invalid
     */
    protected MultiTableReadConfig<TableManipulatorConfig> getConfig()
            throws InvalidSettingsException{
        return saveAndGetConfig();
    }

    private MultiTableReadConfig<TableManipulatorConfig> saveAndGetConfig() throws InvalidSettingsException {
        try {
            saveConfig();
        } catch (RuntimeException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        return m_config;
    }

    @Override
    public void onClose() {
        m_coordinator.onClose();
        m_specTransformer.onClose();
        super.onClose();
    }

    /**
     * Enables/disables the preview depending on what {@link #areIOComponentsDisabled()} returns. Refreshes the preview
     * if the passed parameter is {@code true} and the preview enabled.
     *
     * @param refreshPreview whether the preview should be refreshed
     */
    public void refreshPreview(final boolean refreshPreview) {
        final boolean enabled = !areIOComponentsDisabled();
        setPreviewEnabled(enabled);
        if (enabled && refreshPreview) {
            configChanged();
        }
    }
}
