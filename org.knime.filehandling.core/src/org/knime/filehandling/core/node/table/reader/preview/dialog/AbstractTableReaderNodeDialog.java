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
import java.util.function.Function;

import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.ReadPathAccessor;
import org.knime.filehandling.core.node.table.reader.MultiTableReadFactory;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.ReaderSpecificConfig;
import org.knime.filehandling.core.node.table.reader.config.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.TransformationModel;

/**
 * Abstract implementation of a {@link NodeDialogPane} for table reader nodes.</br>
 * It takes care of creating and managing the table preview.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @param <C> the type of {@link ReaderSpecificConfig} used by the reader node
 * @param <T> the type used to identify external types
 */
public abstract class AbstractTableReaderNodeDialog<C extends ReaderSpecificConfig<C>, T> extends NodeDialogPane {

    private final TableReaderPreviewTransformationController<C, T> m_coordinator;

    private final TableReaderPreviewView m_preview;

    private boolean m_ignoreEvents = false;

    /**
     * Constructor.
     *
     * @param readFactory the {@link MultiTableReadFactory} to use for reading
     * @param defaultProductionPathProvider provides the default production paths for every external type
     */
    public AbstractTableReaderNodeDialog(final MultiTableReadFactory<C, T> readFactory,
        final Function<T, ProductionPath> defaultProductionPathProvider) {
        final AnalysisComponentModel analysisComponentModel = new AnalysisComponentModel();
        final TableReaderPreviewModel previewModel = new TableReaderPreviewModel(analysisComponentModel);
        m_preview = new TableReaderPreviewView(previewModel);
        final TransformationTableModel<T> transformationModel =
            new TransformationTableModel<>(defaultProductionPathProvider);
        m_coordinator = new TableReaderPreviewTransformationController<>(readFactory, transformationModel,
            analysisComponentModel, previewModel, this::getConfig, this::createReadPathAccessor);
    }

    /**
     * Returns the {@link TableReaderPreviewView}.
     *
     * @return the {@link TableReaderPreviewView}
     */
    protected final TableReaderPreviewView getPreview() {
        return m_preview;
    }

    /**
     * Should be called by inheriting classes whenever the config changed i.e. if the user interacts with the dialog.
     */
    protected final void configChanged() {
        if (!areEventsIgnored()) {
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
     * Method to load the preview from the stored {@link DefaultTableSpecConfig}.
     *
     * @param tableSpecConfig to load from
     */
    protected final void loadFromTableSpecConfig(final TableSpecConfig tableSpecConfig) {
        final TransformationModel<T> transformationModel = tableSpecConfig.getTransformationModel();
        m_coordinator.load(transformationModel);
    }

    /**
     * Retrieves the currently configured {@link DefaultTableSpecConfig} or {@code null} if none is available e.g. if the
     * current settings are invalid and thus no preview could be loaded.
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
    protected abstract MultiTableReadConfig<C> getConfig() throws InvalidSettingsException;

    /**
     * Creates a <b>new</b> {@link ReadPathAccessor} that corresponds to the current file selection.</br>
     * It is important to create a new {@link ReadPathAccessor} for every call, otherwise {@link IOException} can occur
     * in the preview.
     *
     * @return the {@link ReadPathAccessor} corresponding to the current file selection
     */
    protected abstract ReadPathAccessor createReadPathAccessor();

    @Override
    public void onClose() {
        m_coordinator.onClose();
        super.onClose();
    }
}
