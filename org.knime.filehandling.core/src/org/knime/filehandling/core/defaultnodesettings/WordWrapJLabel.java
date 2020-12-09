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
 *   25.03.2020 (Mareike Hoeger, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JViewport;

/**
 * A <code>JLabel</code> that wraps the text in a HTML paragraph to ensure word wrapping.
 *
 * @author Mareike Hoeger, KNIME GmbH, Konstanz, Germany
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class WordWrapJLabel extends JLabel {
    private static final long serialVersionUID = 1L;

    private static final String HTML_FORMAT = "<html><p>%s</p></html>";

    private JViewport m_viewport;

    private static final ComponentAdapter LISTENER = new ComponentAdapter() {
        @Override
        public void componentResized(final ComponentEvent ce) {
            if (ce.getSource() instanceof JViewport) {
                final JViewport viewport = (JViewport)ce.getSource();
                final Container view = (Container)viewport.getView();
                //set width to min size or the visible size of the viewport
                final int width = Math.max(view.getMinimumSize().width, viewport.getExtentSize().width);
                view.setPreferredSize(new Dimension(width, Integer.MAX_VALUE));
                final int minWidth = Math.min(view.getLayout().preferredLayoutSize(view).width, view.getPreferredSize().width);
                view.setPreferredSize(new Dimension(minWidth, view.getLayout().preferredLayoutSize(view).height));
                view.revalidate();
            }
        }
    };

    /**
     * Creates a <code>JLabel</code> that wraps the text in a fixed size HTML paragraph to ensure word wrapping.
     *
     * @param text the text to set
     */
    public WordWrapJLabel(final String text) {
        super(text);
        addHierarchyListener(new HierarchyListener() {
            private boolean m_notSet = true;

            @Override
            public void hierarchyChanged(final HierarchyEvent e) {
                if (m_notSet && e.getChangeFlags() == HierarchyEvent.PARENT_CHANGED
                    && e.getChangedParent().getParent() instanceof JScrollPane) {
                    m_viewport = (JViewport)e.getChangedParent();
                    m_viewport.removeComponentListener(LISTENER);
                    m_viewport.addComponentListener(LISTENER);
                    m_notSet = false;
                }
            }
        });
    }

    @Override
    public void setText(final String text) {
        if (text == null || text.trim().isEmpty()) {
            super.setText(" ");
        } else {
            super.setText(String.format(HTML_FORMAT, text));
        }
        /*
         * we hold the viewport to update the extent size
         * whenever we changing the text of the label
         */
        if (m_viewport != null) {
            m_viewport.setExtentSize(m_viewport.getPreferredSize());
        }
    }
}
