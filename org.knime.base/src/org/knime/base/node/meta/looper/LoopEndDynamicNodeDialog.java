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
 * ---------------------------------------------------------------------
 *
 * Created on Nov 28, 2013 by Patrick Winter, KNIME AG, Zurich, Switzerland
 */
package org.knime.base.node.meta.looper;

import java.util.function.IntFunction;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.border.Border;

/**
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author based on {@link LoopEndNodeDialog} by Patrick Winter, KNIME AG, Zurich, Switzerland
 * @since 4.5
 */
final class LoopEndDynamicNodeDialog extends AbstractLoopEndNodeDialog<LoopEndDynamicNodeSettings> {

    private final JCheckBox[] m_ignoreEmptyTables;

    private final JCheckBox[] m_tolerateColumnTypes;

    private final JCheckBox[] m_tolerateChangingSpecs;

    /** Create a new dialog. */
    LoopEndDynamicNodeDialog(final int numberOfTables) {
        super(new LoopEndDynamicNodeSettings(numberOfTables));
        m_ignoreEmptyTables = new JCheckBox[numberOfTables];
        m_tolerateColumnTypes = new JCheckBox[numberOfTables];
        m_tolerateChangingSpecs = new JCheckBox[numberOfTables];
        final IntFunction<Border> createBorder = numberOfTables > 1
            ? (i -> BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Port " + i))
            : (i -> BorderFactory.createEmptyBorder());

        for (var i = 0; i < numberOfTables; i++) {
            m_ignoreEmptyTables[i] = new JCheckBox("Ignore empty input tables");
            m_tolerateColumnTypes[i] = new JCheckBox("Allow variable column types");
            m_tolerateChangingSpecs[i] = new JCheckBox("Allow changing table specifications");
            final var panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(m_ignoreEmptyTables[i]);
            panel.add(m_tolerateColumnTypes[i]);
            panel.add(m_tolerateChangingSpecs[i]);
            panel.setBorder(createBorder.apply(i));
            addComponent(panel);
        }
    }

    @Override
    protected void addToSettings(final LoopEndDynamicNodeSettings settings) {
        for (var i = 0; i < m_ignoreEmptyTables.length; i++) {
            settings.ignoreEmptyTables(i, m_ignoreEmptyTables[i].isSelected());
            settings.tolerateColumnTypes(i, m_tolerateColumnTypes[i].isSelected());
            settings.tolerateChangingTableSpecs(i, m_tolerateChangingSpecs[i].isSelected());
        }
    }

    @Override
    protected void loadFromSettings(final LoopEndDynamicNodeSettings settings) {
        for (var i = 0; i < m_ignoreEmptyTables.length; i++) {
            m_ignoreEmptyTables[i].setSelected(settings.ignoreEmptyTables(i));
            m_tolerateColumnTypes[i].setSelected(settings.tolerateColumnTypes(i));
            m_tolerateChangingSpecs[i].setSelected(settings.tolerateChangingTableSpecs(i));
        }
    }

}
