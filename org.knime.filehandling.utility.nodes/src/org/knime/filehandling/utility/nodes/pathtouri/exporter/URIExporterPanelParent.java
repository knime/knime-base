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
 *   Mar 30, 2021 (Bjoern Lohrmann, KNIME GmbH): created
 */
package org.knime.filehandling.utility.nodes.pathtouri.exporter;

import java.awt.CardLayout;
import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.filehandling.core.connections.uriexport.URIExporterID;
import org.knime.filehandling.core.connections.uriexport.URIExporterPanel;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * {@link JPanel} that holds all {@link URIExporterPanel}s and allows to switch between them.
 *
 * @author Bjoern Lohrmann, KNIME GmbH
 */
public final class URIExporterPanelParent extends JPanel {

    private static final long serialVersionUID = 1L;

    private final Map<URIExporterID, URIExporterPanel> m_exporterPanels = new HashMap<>(); // NOSONAR not using serialization

    private final JPanel m_cards;

    private URIExporterID m_currentExporterID; // NOSONAR not using serialization

    /**
     * Constructor.
     */
    public URIExporterPanelParent() {
        super(new GridBagLayout());
        m_currentExporterID = null;
        m_cards = new JPanel(new CardLayout());
        initLayout();
    }

    private void initLayout() {
        final var gbc = new GBCBuilder();

        gbc.resetPos().anchorFirstLineStart().fillNone().weight(0, 0);
        add(m_cards, gbc.build());

        gbc.incX().setWeightX(1).setWeightY(1).fillBoth();
        add(Box.createGlue(), gbc.build());
    }

    /**
     * Updates UI available exporter panels.
     *
     * @param exporterPanels map of exporter id to panel
     * @param invalidExporter error message for invalid exporter
     */
    public void updateAvailableExporterPanels(final Map<URIExporterID, URIExporterPanel> exporterPanels,
        final String invalidExporter) {
        m_exporterPanels.clear();
        m_exporterPanels.putAll(exporterPanels);

        if (m_currentExporterID != null && m_exporterPanels.containsKey(m_currentExporterID)) {
            m_exporterPanels.get(m_currentExporterID).onUnshown();
        }
        m_currentExporterID = null;
        m_cards.removeAll();

        for (Entry<URIExporterID, URIExporterPanel> entry : m_exporterPanels.entrySet()) {
            m_cards.add(entry.getValue(), toCardName(entry.getKey()));
        }

        m_cards.add(new InvalidURIExporterPanel(invalidExporter), "invalid");
    }

    /**
     * Show exporter panel.
     *
     * @param currExporter {@link URIExporterID}
     */
    public void showExporterPanel(final URIExporterID currExporter) {
        if (m_currentExporterID != null && m_exporterPanels.containsKey(m_currentExporterID)) {
            m_exporterPanels.get(m_currentExporterID).onUnshown();
        }
        m_currentExporterID = null;
        final var cardLayout = (CardLayout)m_cards.getLayout();

        if (m_exporterPanels.containsKey(currExporter)) {
            m_currentExporterID = currExporter;
            m_exporterPanels.get(m_currentExporterID).onShown();
            cardLayout.show(m_cards, toCardName(currExporter));
        } else {
            cardLayout.show(m_cards, "invalid");
        }
    }

    private static String toCardName(final URIExporterID currExporter) {
        return "exporter-" + currExporter.toString();
    }
}
