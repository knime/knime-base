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
 *   15 Mar 2021 (Laurin Siefermann, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.imagewriter.table;

import java.awt.Component;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.image.ImageValue;
import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FolderStatusMessageReporter;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Node dialog of the image writer table node.
 *
 * @author Laurin Siefermann, KNIME GmbH, Konstanz, Germany
 */
final class ImageWriterTableNodeDialog extends NodeDialogPane {

    private static final String FILE_HISTORY_ID = "image_table_reader_writer";

    private final ImageWriterTableNodeConfig m_nodeConfig;

    private final DialogComponentWriterFileChooser m_folderChooser;

    private final DialogComponentColumnNameSelection m_colSelection;

    private final DialogComponentBoolean m_removeImgCol;

    /**
     * Constructor.
     *
     * @param nodeConfig
     * @param inputTableIdx index of data table input port group name
     */
    @SuppressWarnings("unchecked")
    ImageWriterTableNodeDialog(final ImageWriterTableNodeConfig nodeConfig, final int inputTableIdx) {
        m_nodeConfig = nodeConfig;

        final FlowVariableModel fvm = createFlowVariableModel(
            m_nodeConfig.getFolderChooserModel().getKeysForFSLocation(), FSLocationVariableType.INSTANCE);
        m_folderChooser = new DialogComponentWriterFileChooser(m_nodeConfig.getFolderChooserModel(), FILE_HISTORY_ID,
            fvm, FolderStatusMessageReporter::new);

        m_colSelection = new DialogComponentColumnNameSelection(m_nodeConfig.getColSelectModel(), "Column",
            inputTableIdx, ImageValue.class);

        m_removeImgCol = new DialogComponentBoolean(m_nodeConfig.getRemoveImgColModel(), "Remove image column");

        addTab("Settings", createSettingsPanel());
    }

    private Component createSettingsPanel() {
        final JPanel settingsPanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder(new Insets(5, 0, 5, 0));

        settingsPanel.add(createColSelectionPanel(),
            gbcBuilder.setX(0).setY(0).setWeightX(1.0).fillHorizontal().build());

        settingsPanel.add(createOutputPanel(), gbcBuilder.incY().build());

        settingsPanel.add(new JPanel(), gbcBuilder.incY().setWeightY(1.0).setWeightX(1.0).fillBoth().build());

        return settingsPanel;
    }

    private Component createColSelectionPanel() {
        final JPanel colSelectionPanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder();

        colSelectionPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Image"));

        colSelectionPanel.add(m_colSelection.getComponentPanel(),
            gbcBuilder.anchorFirstLineStart().setX(0).setY(0).build());

        colSelectionPanel.add(m_removeImgCol.getComponentPanel(), gbcBuilder.incY().insetLeft(-3).build());

        colSelectionPanel.add(new JPanel(), gbcBuilder.incX().setWeightX(1.0).fillHorizontal().build());

        return colSelectionPanel;
    }

    private Component createOutputPanel() {
        final JPanel outputPanel = new JPanel(new GridBagLayout());
        final GBCBuilder gbcBuilder = new GBCBuilder();

        outputPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Output Location"));

        outputPanel.add(m_folderChooser.getComponentPanel(),
            gbcBuilder.anchorFirstLineStart().insetLeft(5).insetRight(5).setWeightX(1.0).fillHorizontal().build());

        return outputPanel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_colSelection.saveSettingsTo(settings);
        m_removeImgCol.saveSettingsTo(settings);
        m_folderChooser.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_colSelection.loadSettingsFrom(settings, specs);
        m_removeImgCol.loadSettingsFrom(settings, specs);
        m_folderChooser.loadSettingsFrom(settings, specs);
    }

    @Override
    public void onClose() {
        m_folderChooser.onClose();
    }

}
