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
 *   20 Jul 2021 (Moditha Hewasinghage, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.filehandling.core.node.table.writer;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Optional;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FolderStatusMessageReporter;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * An abstract implementation of a node dialoge for table writer nodes.
 *
 * @author Moditha Hewasinghage, KNIME GmbH, Berlin, Germany
 * @author Lars Schweikardt, KNIME GmbH, Konstanz, Germany
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 *
 * @param <C> the type of {@link AbstractMultiTableWriterNodeConfig}
 *
 * @noreference non-public API
 * @noextend non-public API
 */
public abstract class AbstractMultiTableWriterNodeDialog<C extends AbstractMultiTableWriterNodeConfig<? extends DataValue>>
    extends NodeDialogPane {

    private final String m_fileHistoryId;

    private final C m_nodeConfig;

    private final DialogComponentWriterFileChooser m_folderChooser;

    private final DialogComponentColumnNameSelection m_sourceColumnSelection;

    private final DialogComponentBoolean m_removeSourceColumn;

    private final DialogComponentString m_filenamePattern;

    private final DialogComponentColumnNameSelection m_filenameColumnSelection;

    private final JRadioButton m_generateFilenameRadio;

    private final JRadioButton m_columnFilenameRadio;

    private final String m_writerTypeName;

    /**
     * Constructor.
     *
     * @param nodeConfig storing the user settings (concrete subclass instantiation of
     *            {@link AbstractMultiTableWriterNodeConfig})
     * @param inputTableIdx index of data-table-input-port-group-name
     */
    @SuppressWarnings("unchecked")
    protected AbstractMultiTableWriterNodeDialog(final C nodeConfig, final int inputTableIdx) {
        m_nodeConfig = nodeConfig;

        m_writerTypeName = m_nodeConfig.getWriterTypeName();

        m_fileHistoryId = String.format("%s_table_reader_writer", m_writerTypeName);

        final FlowVariableModel fvm = createFlowVariableModel( // NOSONAR standard API usage
            m_nodeConfig.getOutputLocation().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_folderChooser = new DialogComponentWriterFileChooser(m_nodeConfig.getOutputLocation(),
            m_fileHistoryId, fvm, FolderStatusMessageReporter::new);

        m_sourceColumnSelection = new DialogComponentColumnNameSelection(m_nodeConfig.getSourceColumn(),
            "Column", inputTableIdx, m_nodeConfig.getValueClass());

        m_removeSourceColumn = new DialogComponentBoolean(m_nodeConfig.getRemoveSourceColumn(),
            String.format("Remove %s column", m_writerTypeName));

        final ButtonGroup buttonGrp = new ButtonGroup();
        m_generateFilenameRadio = new JRadioButton("Generate");
        m_columnFilenameRadio = new JRadioButton("From column");
        buttonGrp.add(m_generateFilenameRadio);
        buttonGrp.add(m_columnFilenameRadio);

        m_filenamePattern = new DialogComponentString(m_nodeConfig.getFilenamePattern(), "", true, 25);

        m_filenameColumnSelection = new DialogComponentColumnNameSelection(m_nodeConfig.getFilenameColumn(),
            "", inputTableIdx, false, StringValue.class);
    }

    /**
     * Creates the settings tab including the writer-specific settings. Additional writer-specific tabs should go on the
     * concrete implementation
     */
    protected final void createSettingsTab() {
        final JPanel settingsPanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder(new Insets(5, 0, 5, 0));
        settingsPanel.add(createOutputLocationPanel(),
            gbcBuilder.setX(0).setY(0).setWeightX(1.0).fillHorizontal().build());
        settingsPanel.add(createSourceColumnPanel(), gbcBuilder.incY().build());
        settingsPanel.add(createFilenamePanel(), gbcBuilder.incY().build());
        final Optional<Component> additonalWriterSpecificPanels = addWriterSpecificSettingsDialogs();
        if (additonalWriterSpecificPanels.isPresent()) {
            settingsPanel.add(additonalWriterSpecificPanels.get(), gbcBuilder.incY().build());
        }
        settingsPanel.add(new JPanel(), gbcBuilder.incY().setWeightY(1.0).setWeightX(1.0).fillBoth().build());
        addTab("Settings", settingsPanel);
    }

    private Component createSourceColumnPanel() {
        final JPanel columnSelectionPanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder();

        columnSelectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
            StringUtils.capitalize(m_writerTypeName)));

        columnSelectionPanel.add(m_sourceColumnSelection.getComponentPanel(),
            gbcBuilder.anchorFirstLineStart().setX(0).setY(0).build());

        columnSelectionPanel.add(m_removeSourceColumn.getComponentPanel(), gbcBuilder.incY().insetLeft(-3).build());

        columnSelectionPanel.add(new JPanel(), gbcBuilder.incX().setWeightX(1.0).fillHorizontal().build());

        return columnSelectionPanel;
    }

    private Component createFilenamePanel() {
        final JPanel fileNamePanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder();
        fileNamePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "File names"));

        fileNamePanel.add(m_generateFilenameRadio, gbcBuilder.anchorLineStart().setX(0).setY(0).build());
        fileNamePanel.add(m_filenamePattern.getComponentPanel(), gbcBuilder.incX().build());
        fileNamePanel.add(new JPanel(), gbcBuilder.incX().setWeightX(1.0).fillHorizontal().build());

        fileNamePanel.add(m_columnFilenameRadio,
            gbcBuilder.anchorLineStart().setX(0).setWeightX(0).incY().fillNone().build());
        fileNamePanel.add(m_filenameColumnSelection.getComponentPanel(), gbcBuilder.incX().build());
        fileNamePanel.add(new JPanel(), gbcBuilder.incX().setWeightX(1.0).fillHorizontal().build());

        m_generateFilenameRadio.addActionListener(l -> toggleGenerateMode());
        m_columnFilenameRadio.addActionListener(l -> toggleGenerateMode());

        return fileNamePanel;
    }

    private Component createOutputLocationPanel() {
        final JPanel outputPanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder();

        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output location"));

        outputPanel.add(m_folderChooser.getComponentPanel(),
            gbcBuilder.anchorFirstLineStart().insetLeft(5).insetRight(5).setWeightX(1.0).fillHorizontal().build());

        return outputPanel;
    }

    private void toggleGenerateMode() {
        m_filenameColumnSelection.getModel().setEnabled(m_columnFilenameRadio.isSelected());
        m_filenamePattern.getModel().setEnabled(m_generateFilenameRadio.isSelected());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_folderChooser.saveSettingsTo(settings);
        m_sourceColumnSelection.saveSettingsTo(settings);
        m_removeSourceColumn.saveSettingsTo(settings);
        m_nodeConfig.saveFileNameRadioSelectionForDialog(settings, m_generateFilenameRadio.isSelected());
        m_filenamePattern.saveSettingsTo(settings);
        m_filenameColumnSelection.saveSettingsTo(settings);
        saveWriterSpecificSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_sourceColumnSelection.loadSettingsFrom(settings, specs);
        m_removeSourceColumn.loadSettingsFrom(settings, specs);
        m_nodeConfig.loadFileNameRadioSelectionForDialog(settings);
        m_filenamePattern.loadSettingsFrom(settings, specs);
        m_filenameColumnSelection.loadSettingsFrom(settings, specs);
        m_generateFilenameRadio.setSelected(m_nodeConfig.shouldGenerateFilename());
        m_columnFilenameRadio.setSelected(!m_nodeConfig.shouldGenerateFilename());
        m_folderChooser.loadSettingsFrom(settings, specs);
        loadWriterSpecificSettingsFrom(settings, specs);
    }

    @Override
    public void onClose() {
        m_folderChooser.onClose();
    }

    /**
     * Add writer specific settings component after default setting components (outputLocationPanel, srcColPanel,
     * FileNamePanel) in the settings tab.
     *
     * @return Optional of additional settings component
     */
    protected abstract Optional<Component> addWriterSpecificSettingsDialogs();

    /**
     * Save writer specific settings.
     *
     * @param settings node settings to save
     * @throws InvalidSettingsException
     */
    protected abstract void saveWriterSpecificSettingsTo(NodeSettingsWO settings) throws InvalidSettingsException;

    /**
     * Load writer specific settings.
     *
     * @param settings node settings to load
     * @param specs {@link PortObjectSpec}
     * @throws NotConfigurableException
     */
    protected abstract void loadWriterSpecificSettingsFrom(NodeSettingsRO settings, PortObjectSpec[] specs)
        throws NotConfigurableException;

    /**
     * @return the nodeConfig
     */
    public C getNodeConfig() {
        return m_nodeConfig;
    }

}
