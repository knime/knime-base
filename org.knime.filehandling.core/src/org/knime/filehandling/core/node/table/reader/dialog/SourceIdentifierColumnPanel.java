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
 *   Jun 2, 2021 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.dialog;

import java.awt.GridBagLayout;
import java.util.concurrent.CopyOnWriteArraySet;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.filehandling.core.util.GBCBuilder;

/**
 * A panel for the options relating to the source identifier column.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
public final class SourceIdentifierColumnPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JCheckBox m_prependSourceIdentifierColumn;

    private final JTextField m_sourceIdentifierColumnName = new JTextField(20);

    private final transient CopyOnWriteArraySet<ChangeListener> m_listeners = new CopyOnWriteArraySet<>();

    private final ChangeEvent m_event = new ChangeEvent(this);

    /**
     * Constructor.
     *
     * @param sourceType a descriptive name of the kind of source used (e.g. Path)
     */
    public SourceIdentifierColumnPanel(final String sourceType) {
        super(new GridBagLayout());
        setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), sourceType + " column"));
        m_prependSourceIdentifierColumn = new JCheckBox(String.format("Prepend %s column", sourceType.toLowerCase()));
        hookupListeners();
        layoutPanel();
    }

    private void hookupListeners() {
        m_prependSourceIdentifierColumn.addActionListener(e -> togglePathColumnName());
        m_prependSourceIdentifierColumn.addActionListener(e -> notifyListeners());
        m_sourceIdentifierColumnName.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(final DocumentEvent e) {
                notifyListeners();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                notifyListeners();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                notifyListeners();
            }

        });
    }

    private void layoutPanel() {
        final GBCBuilder gbc = new GBCBuilder().resetPos().anchorFirstLineStart();
        add(m_prependSourceIdentifierColumn, gbc.build());
        add(m_sourceIdentifierColumnName, gbc.incX().insetTop(2).build());
        add(new JPanel(), gbc.incX().fillHorizontal().setWeightX(1.0).build());
    }

    private void togglePathColumnName() {
        m_sourceIdentifierColumnName.setEnabled(m_prependSourceIdentifierColumn.isSelected());
    }

    /**
     * Loads the provided values into the panel.
     *
     * @param prependSourceIdentifierColumn whether the column should be prepended
     * @param sourceIdentifierColumnName the name of the column to append
     */
    public void load(final boolean prependSourceIdentifierColumn, final String sourceIdentifierColumnName) {
        m_prependSourceIdentifierColumn.setSelected(prependSourceIdentifierColumn);
        m_sourceIdentifierColumnName.setText(sourceIdentifierColumnName);
        togglePathColumnName();
    }

    /**
     * @return {@code true} if the append checkbox is checked
     */
    public boolean isPrependSourceIdentifierColumn() {
        return m_prependSourceIdentifierColumn.isSelected();
    }

    /**
     * @return the name in the text field
     */
    public String getSourceIdentifierColumnName() {
        return m_sourceIdentifierColumnName.getText();
    }

    /**
     * Allows to register a listener on this panel.
     *
     * @param listener to notify about changes
     */
    public void addChangeListener(final ChangeListener listener) {
        m_listeners.add(listener);//NOSONAR
    }

    private void notifyListeners() {
        m_listeners.forEach(l -> l.stateChanged(m_event));
    }

}
