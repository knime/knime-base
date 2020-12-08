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
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import javax.swing.event.ChangeEvent;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.preview.dialog.transformer.TableTransformationPanel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.transformer.TableTransformationTableModel;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.util.CheckNodeContextUtil;

/**
 * Abstract implementation of a {@link NodeDialogPane} for table reader nodes.</br>
 * It takes care of creating and managing the table preview.
 *
 * NOTE: Extending classes should call {@link #loadFromTableSpecConfig(TableSpecConfig)} when loading there settings
 * once the TableSpecConfig has been loaded.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <I> the type of source items the reader reads from
 * @param <C> the type of {@link ReaderSpecificConfig} used by the reader node
 * @param <T> the type used to identify external types
 */
public abstract class AbstractTableReaderNodeDialog<I, C extends ReaderSpecificConfig<C>, T> extends NodeDialogPane {

    private final TableReaderPreviewTransformationCoordinator<I, C, T> m_coordinator;

    private final List<TableReaderPreviewView> m_previews = new ArrayList<>();

    private final TableReaderPreviewModel m_previewModel;

    private final TableTransformationPanel m_specTransformer;

    private final boolean m_disableIOComponents;

    private boolean m_ignoreEvents = false;

    /**
     * Constructor.
     *
     * @param readFactory the {@link MultiTableReadFactory} to use for reading
     * @param productionPathProvider provides the default production paths for every external type
     * @param allowsMultipleFiles whether the reader supports reading tables from multiple files at once
     */
    @SuppressWarnings("unchecked")
    public AbstractTableReaderNodeDialog(final MultiTableReadFactory<I, C, T> readFactory,
        final ProductionPathProvider<T> productionPathProvider, final boolean allowsMultipleFiles) {
        final AnalysisComponentModel analysisComponentModel = new AnalysisComponentModel();
        final TableReaderPreviewModel previewModel = new TableReaderPreviewModel(analysisComponentModel);
        m_previewModel = previewModel;
        final TableTransformationTableModel<T> transformationModel =
            new TableTransformationTableModel<>(productionPathProvider::getDefaultProductionPath);
        m_coordinator = new TableReaderPreviewTransformationCoordinator<>(readFactory, transformationModel,
            analysisComponentModel, previewModel, this::getConfig, this::createItemAccessor);
        m_specTransformer = new TableTransformationPanel(transformationModel,
            t -> productionPathProvider.getAvailableProductionPaths((T)t), allowsMultipleFiles);
        m_disableIOComponents = CheckNodeContextUtil.isRemoteWorkflowContext();
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
        splitPane.setRightComponent(createPreviewComponent());
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerSize(15);
        return splitPane;
    }

    /**
     * Returns the the component containing the preview.
     *
     * @return the component containing the preview
     */
    protected JComponent createPreviewComponent() {
        return createPreview();
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
     * @param tableSpecConfig to load from
     */
    protected final void loadFromTableSpecConfig(final TableSpecConfig<T> tableSpecConfig) {
        final TableTransformation<T> transformationModel = tableSpecConfig.getTransformationModel();
        m_coordinator.load(transformationModel);
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_specTransformer.commitChanges();
    }

    @Override
    protected final void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        ignoreEvents(true);
        setPreviewEnabled(false);
        loadSettings(settings, specs);
        ignoreEvents(false);
        refreshPreview(true);
    }

    /**
     * See {@link #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])}.<br>
     * NOTE: Extending classes should call {@link #loadFromTableSpecConfig(TableSpecConfig)} once the TableSpecConfig
     * has been loaded.
     *
     * @param settings the settings
     * @param specs the specs
     * @throws NotConfigurableException if a component cannot be configured
     * @see #loadSettingsFrom(NodeSettingsRO, PortObjectSpec[])
     */
    protected abstract void loadSettings(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException;

    /**
     * Retrieves the currently configured {@link DefaultTableSpecConfig} or {@code null} if none is available e.g. if
     * the current settings are invalid and thus no preview could be loaded.
     *
     * @return the currently configured {@link DefaultTableSpecConfig} or {@code null} if none is available
     */
    protected final TableSpecConfig<T> getTableSpecConfig() {
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
    protected abstract MultiTableReadConfig<C, T> getConfig() throws InvalidSettingsException;

    /**
     * Creates a <b>new</b> {@link GenericItemAccessor} that corresponds to the current file selection.</br>
     * It is important to create a new {@link GenericItemAccessor} for every call, otherwise {@link IOException} can
     * occur in the preview.
     *
     * @return the {@link GenericItemAccessor} corresponding to the current file selection
     */
    protected abstract GenericItemAccessor<I> createItemAccessor();

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
