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
 *   Oct 9, 2020 (ayazqureshi): created
 */
package org.knime.filehandling.utility.nodes.binaryobjects.writer;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.core.data.StringValue;
import org.knime.core.data.blob.BinaryObjectDataValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * The {@link NodeModel} for converting Binary Objects to files.
 *
 * @author Ayaz Ali Qureshi, KNIME GmbH, Berlin, Germany
 */
final class BinaryObjectsToFilesNodeDialog extends NodeDialogPane {

    private static final String BINARY_OBJ_BORDER_LABEL = "Binary object";

    private static final String BINARY_OBJ_COLUMN_LABEL = "Column";

    private static final String REMOVE_BINARY_OBJ_COLUMN_LABEL = "Remove column";

    private static final String OUTPUT_LOC_PANEL_LABEL = "Output location";

    private static final String FILE_NAMES_LABEL = "File names";

    private static final String FILE_HISTORY_ID = "binary_objects_to_files_reader_writer";

    private final BinaryObjectsToFilesNodeConfig m_binaryObjectsToFilesNodeSettings;

    private final DialogComponentColumnNameSelection m_binaryColSelection;

    private final DialogComponentWriterFileChooser m_destinationFolderSelection;

    private final DialogComponentBoolean m_removeBinaryObjectColumn;

    private final DialogComponentColumnNameSelection m_outputFilenameColSelection;

    private final DialogComponentString m_userDefinedOutputFilename;

    private final JRadioButton m_generateRadio;

    private final JRadioButton m_columnRadio;

    @SuppressWarnings("unchecked")
    BinaryObjectsToFilesNodeDialog(final PortsConfiguration portsConfiguration,
        final BinaryObjectsToFilesNodeConfig incomingSettings) {

        m_binaryObjectsToFilesNodeSettings = incomingSettings;

        m_binaryColSelection =
            new DialogComponentColumnNameSelection(
                m_binaryObjectsToFilesNodeSettings.getBinaryObjectsSelectionColumnModel(), BINARY_OBJ_COLUMN_LABEL,
                portsConfiguration.getInputPortLocation()
                    .get(BinaryObjectsToFilesNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0],
                BinaryObjectDataValue.class);

        m_outputFilenameColSelection = new DialogComponentColumnNameSelection(
            m_binaryObjectsToFilesNodeSettings.getOutputFilenameColumnModel(), "", portsConfiguration
                .getInputPortLocation().get(BinaryObjectsToFilesNodeFactory.DATA_TABLE_INPUT_PORT_GRP_NAME)[0],
            false, StringValue.class);

        SettingsModelWriterFileChooser destionationFolderChooser =
            m_binaryObjectsToFilesNodeSettings.getFileSettingsModelWriterFileChooser();

        final FlowVariableModel writeFvm =
            createFlowVariableModel(destionationFolderChooser.getKeysForFSLocation(), FSLocationVariableType.INSTANCE);

        m_destinationFolderSelection =
            new DialogComponentWriterFileChooser(destionationFolderChooser, FILE_HISTORY_ID, writeFvm);

        m_removeBinaryObjectColumn = new DialogComponentBoolean(
            m_binaryObjectsToFilesNodeSettings.getRemoveBinaryObjColumnModel(), REMOVE_BINARY_OBJ_COLUMN_LABEL);

        m_userDefinedOutputFilename =
            new DialogComponentString(m_binaryObjectsToFilesNodeSettings.getUserDefinedOutputFilename(), "", true, 25);

        m_generateRadio = new JRadioButton("Generate");
        m_columnRadio = new JRadioButton("From column");

        final ButtonGroup buttonGrp = new ButtonGroup();
        buttonGrp.add(m_generateRadio);
        buttonGrp.add(m_columnRadio);

        m_generateRadio.addActionListener(l -> toggleGenerateMode());
        m_columnRadio.addActionListener(l -> toggleGenerateMode());

        addTab("Settings", createSettingsDialog());
    }

    private void toggleGenerateMode() {
        m_outputFilenameColSelection.getModel().setEnabled(m_columnRadio.isSelected());
        m_userDefinedOutputFilename.getModel().setEnabled(m_generateRadio.isSelected());
    }

    /**
     * Method to create a JPanel component for Settings Tab
     *
     * @return Component which can be added as Settings Tab in DialogPane
     */
    private Component createSettingsDialog() {
        final JPanel p = new JPanel(new GridBagLayout());

        final GBCBuilder gbc = new GBCBuilder().resetPos().setWeightX(1).setWeightY(0).fillHorizontal();
        p.add(createBinaryObjectPanel(), gbc.build());

        gbc.incY();
        p.add(createFileNameSettingsDialog(), gbc.build());

        gbc.incY();
        p.add(createFileWriterDialog(), gbc.build());

        gbc.incY().setWeightY(1).fillBoth();
        p.add(new JPanel(), gbc.build());

        return p;
    }

    private Component createBinaryObjectPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), BINARY_OBJ_BORDER_LABEL));

        final GBCBuilder gbc =
            new GBCBuilder().resetPos().setWeightX(0).setWeightY(0).anchorLineStart().fillNone().insetLeft(3);

        p.add(m_binaryColSelection.getComponentPanel(), gbc.build());

        gbc.incY().insetLeft(0);
        p.add(m_removeBinaryObjectColumn.getComponentPanel(), gbc.build());

        gbc.incY().setWeightX(1).fillHorizontal().insetTop(-10);
        p.add(new JPanel(), gbc.build());

        return p;
    }

    /**
     * Creates the file names panel.
     *
     * @return JPanel A new JPanel with the enclosed components
     */
    private JPanel createFileNameSettingsDialog() {
        final JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), FILE_NAMES_LABEL));

        final GBCBuilder gbc =
            new GBCBuilder().anchorLineStart().resetX().resetY().setWeightX(0).setWeightY(0).fillNone();

        p.add(m_generateRadio, gbc.build());

        gbc.incX();
        p.add(m_userDefinedOutputFilename.getComponentPanel(), gbc.build());

        gbc.incY().resetX();
        p.add(m_columnRadio, gbc.build());

        gbc.incX();
        p.add(m_outputFilenameColSelection.getComponentPanel(), gbc.build());

        gbc.resetX().incY().setWeightX(1).setWidth(2).fillHorizontal().insetTop(-10);
        p.add(new JPanel(), gbc.build());

        return p;
    }

    /**
     * Wrap the DialogComponentWriterFileChooser in another panel which has an outline
     *
     * @return JPanel which encloses DialogComponentWriterFileChooser
     */
    private JPanel createFileWriterDialog() {
        final JPanel filePanel = new JPanel(new GridBagLayout());
        filePanel
            .setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), OUTPUT_LOC_PANEL_LABEL));

        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorLineStart().fillHorizontal().setWeightX(1)
            .insetRight(7).insetLeft(4);
        filePanel.add(m_destinationFolderSelection.getComponentPanel(), gbc.build());

        return filePanel;
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_binaryColSelection.loadSettingsFrom(settings, specs);
        m_removeBinaryObjectColumn.loadSettingsFrom(settings, specs);
        m_binaryObjectsToFilesNodeSettings.loadGenerateFileNamesForDialog(settings);
        m_userDefinedOutputFilename.loadSettingsFrom(settings, specs);
        m_outputFilenameColSelection.loadSettingsFrom(settings, specs);
        m_destinationFolderSelection.loadSettingsFrom(settings, specs);
        m_generateRadio.setSelected(m_binaryObjectsToFilesNodeSettings.generateFileNames());
        m_columnRadio.setSelected(!m_binaryObjectsToFilesNodeSettings.generateFileNames());
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_binaryColSelection.saveSettingsTo(settings);
        m_removeBinaryObjectColumn.saveSettingsTo(settings);
        m_binaryObjectsToFilesNodeSettings.saveGenerateFileNamesForDialog(settings, m_generateRadio.isSelected());
        m_userDefinedOutputFilename.saveSettingsTo(settings);
        m_outputFilenameColSelection.saveSettingsTo(settings);
        m_destinationFolderSelection.saveSettingsTo(settings);
    }

}
