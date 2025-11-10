/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */
package org.knime.base.node.mine.cluster.kmeans;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnFilter;
import org.knime.core.node.defaultnodesettings.DialogComponentLabel;
import org.knime.core.node.defaultnodesettings.DialogComponentNumber;
import org.knime.core.node.defaultnodesettings.DialogComponentSeed;
import org.knime.core.node.defaultnodesettings.SettingsModelIntegerBounded;

/**
 * Dialog for {@link KMeansNodeModel} - allows to adjust number of clusters and other properties.
 *
 * @author Michael Berthold, University of Konstanz
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @deprecated Replaced by {@link KMeansNodeParameters}
 */
@Deprecated
public class ClusterNodeDialog extends NodeDialogPane {

    final DialogComponentNumber m_nrOfClusters;

    private final DialogComponentLabel m_centroidInitializationLabel =
        new DialogComponentLabel("Centroid initialization: ");

    private final DialogComponentSeed m_centroidSeeds =
        new DialogComponentSeed(ClusterNodeModel.createCentroidSeedsModel(), "Use static random seed");

    private final DialogComponentButtonGroup m_centroidInitialization = new DialogComponentButtonGroup(
        ClusterNodeModel.createCentroidInitializationModel(), null, true, CentroidInitialization.values());

    private final DialogComponentNumber m_maxNrOfIterations =
        new DialogComponentNumber(ClusterNodeModel.createNrMaxIterationsModel(), "Max. number of iterations: ", 10);

    @SuppressWarnings("unchecked")
    private final DialogComponentColumnFilter m_columnFilter =
        new DialogComponentColumnFilter(ClusterNodeModel.createUsedColumnsModel(), 0, true, DoubleValue.class);

    private final DialogComponentBoolean m_enableHilite =
        new DialogComponentBoolean(ClusterNodeModel.createEnableHiliteModel(), "Enable Hilite Mapping");

    /**
     * Constructor to create the dialog panel of the k-means node.
     */
       ClusterNodeDialog() {
        //initialize the number of clusters model
        final SettingsModelIntegerBounded nrOfClustersModel = ClusterNodeModel.createNrOfClustersModel();

        m_nrOfClusters = new DialogComponentNumber(nrOfClustersModel, "Number of clusters: ", 1,
            createFlowVariableModel(nrOfClustersModel));

        //creating the panel
        JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        panel.add(createClustersPanel(), c);
        c.gridy++;
        panel.add(leftAlignmentPanel(m_maxNrOfIterations.getComponentPanel(), "Number of Iterations"), c);
        c.gridy++;
        panel.add(leftAlignmentPanel(m_columnFilter.getComponentPanel(), "Column Selection"), c);
        c.gridy++;
        c.weighty = 1;
        panel.add(leftAlignmentPanel(m_enableHilite.getComponentPanel(), "Hilite Mapping"), c);
        addTab("K-Means Properties", panel);
    }

    private JPanel createClustersPanel() {
        m_centroidInitialization.getModel().addChangeListener(e -> updateCentroidInitialization());
        JPanel clusters = new JPanel(new GridBagLayout());
        clusters.setBorder(BorderFactory.createTitledBorder("Clusters"));
        final GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.FIRST_LINE_START;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        clusters.add(m_nrOfClusters.getComponentPanel(), c);
        c.gridy++;
        c.gridwidth = 1;
        clusters.add(m_centroidInitializationLabel.getComponentPanel(), c);
        c.gridy++;
        c.insets = new Insets(0, 30, 0, 0);
        clusters.add(m_centroidInitialization.getButton(CentroidInitialization.FIRST_ROWS.getActionCommand()), c);
        c.gridy++;
        c.anchor = GridBagConstraints.LINE_START;
        clusters.add(
            m_centroidInitialization.getButton(CentroidInitialization.RANDOM_INITIALIZATION.getActionCommand()), c);
        c.gridx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        c.weightx = 1;
        clusters.add(m_centroidSeeds.getComponentPanel(), c);
        return clusters;
    }

    private static JPanel leftAlignmentPanel(final JPanel innerPanel, final String borderTitle) {
        JPanel outerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        outerPanel.setBorder(BorderFactory.createTitledBorder(borderTitle));
        outerPanel.add(innerPanel);
        return outerPanel;
    }

    private boolean firstRowsIsSelected() {
        return m_centroidInitialization.getButton(CentroidInitialization.FIRST_ROWS.getActionCommand()).isSelected();
    }

    private void updateCentroidInitialization() {
        m_centroidSeeds.getModel().setEnabled(!firstRowsIsSelected());

    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final DataTableSpec[] specs)
        throws NotConfigurableException {
        m_nrOfClusters.loadSettingsFrom(settings, specs);
        m_centroidInitialization.loadSettingsFrom(settings, specs);
        m_centroidSeeds.loadSettingsFrom(settings, specs);
        m_maxNrOfIterations.loadSettingsFrom(settings, specs);
        m_columnFilter.loadSettingsFrom(settings, specs);
        m_enableHilite.loadSettingsFrom(settings, specs);
        updateCentroidInitialization();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_nrOfClusters.saveSettingsTo(settings);
        m_centroidInitialization.saveSettingsTo(settings);
        m_centroidSeeds.saveSettingsTo(settings);
        m_maxNrOfIterations.saveSettingsTo(settings);
        m_columnFilter.saveSettingsTo(settings);
        m_enableHilite.saveSettingsTo(settings);

    }
}
