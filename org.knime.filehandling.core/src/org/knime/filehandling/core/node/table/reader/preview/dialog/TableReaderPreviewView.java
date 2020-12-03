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

import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.Rectangle;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.knime.core.node.tableview.TableView;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * View of the table reader preview.</br>
 * Displays a {@link TableView} with additional components for progress and error reporting.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference not part of public API
 * @noinstantiate not part of public API
 */
public final class TableReaderPreviewView extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int PREVIEW_WIDTH = 750;

    private static final int PREVIEW_HEIGHT = 250;

    private final AnalysisComponentView m_analysisComponentView;

    private final TableView m_tableView;

    private final transient CopyOnWriteArraySet<ChangeListener> m_scrollListeners = new CopyOnWriteArraySet<>();

    private final ChangeEvent m_changeEvent = new ChangeEvent(this);

    /**
     * Constructor.
     *
     * @param model the model this view displays
     */
    public TableReaderPreviewView(final TableReaderPreviewModel model) {
        m_analysisComponentView = new AnalysisComponentView(model.getAnalysisComponent());
        m_tableView = new TableView(model.getPreviewTableModel());
        // reordering the columns might give the impression that the order in the output changes too
        m_tableView.getContentTable().getTableHeader().setReorderingAllowed(false);
        // tell listeners that the scrolling changed
        m_tableView.getViewport().addChangeListener(e -> notifyListeners());
        createPanel();
    }

    private void notifyListeners() {
        for (ChangeListener listener : m_scrollListeners) {
            listener.stateChanged(m_changeEvent);
        }
    }

    private void createPanel() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Preview"));
        final GBCBuilder gbc = new GBCBuilder().resetX().resetY().anchorFirstLineStart();
        add(m_analysisComponentView, gbc.build());
        m_tableView.setPreferredSize(new Dimension(PREVIEW_WIDTH, PREVIEW_HEIGHT));
        add(m_tableView, gbc.fillBoth().incY().setWeightX(1).setWeightY(1).build());
    }

    /**
     * Updates the viewport of this preview so that the first displayed row is the same as in
     * {@link TableReaderPreviewView other}.
     *
     * @param other {@link TableReaderPreviewView} whose viewport to match
     */
    public void updateViewport(final TableReaderPreviewView other) {
        final Rectangle visibleRect = other.m_tableView.getViewport().getViewRect();
        m_tableView.getViewport().setViewPosition(visibleRect.getLocation());
    }

    /**
     * Registers the provided {@link ChangeListener}, which will be notified when this preview is scrolled.
     *
     * @param scrollListener to add
     */
    public void addScrollListener(final ChangeListener scrollListener) {
        m_scrollListeners.add(scrollListener); //NOSONAR
    }

    /**
     * Returns the table view.
     *
     * @return the table view
     */
    public TableView getTableView() {
        return m_tableView;
    }

}
