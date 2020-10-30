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
 *   Oct 15, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog.transformer;

import java.awt.GridBagLayout;
import java.util.EnumMap;
import java.util.Map.Entry;

import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * Panel for selecting the {@link ColumnFilterMode}.</br>
 * It consists of three radio buttons aligned horizontally.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class ColumnFilterModePanel extends JPanel {

    private static final long serialVersionUID = 1L;

    private final EnumMap<ColumnFilterMode, JRadioButton> m_btns = new EnumMap<>(ColumnFilterMode.class);

    private final transient ColumnFilterModeModel m_model;

    ColumnFilterModePanel(final ColumnFilterModeModel model) {
        super(new GridBagLayout());

        m_btns.put(ColumnFilterMode.UNION, new JRadioButton("Union", true));
        m_btns.put(ColumnFilterMode.INTERSECTION, new JRadioButton("Intersection"));

        m_model = model;
        final ButtonGroup btnGrp = new ButtonGroup();
        final GBCBuilder gbc = new GBCBuilder().resetY();
        for (JRadioButton btn : m_btns.values()) {
            add(btn, gbc.incX().build());
            btn.addActionListener(e -> handleButtonChange());
            btnGrp.add(btn);
        }
        add(new JPanel(), gbc.fillHorizontal().setWeightX(1).incX().build());
        m_model.addChangeListener(e -> handleModelChange());
    }

    private void handleButtonChange() {
        m_model.setColumnFilterModel(getColumnFilterMode());
    }

    private void handleModelChange() {
        m_btns.get(m_model.getColumnFilterMode()).setSelected(true);
    }

    private ColumnFilterMode getColumnFilterMode() {
        return m_btns.entrySet().stream()//
                .filter(e -> e.getValue().isSelected())//
                .map(Entry::getKey)//
                .findFirst()//
                .orElseThrow(() -> new IllegalStateException("No column filter mode selected."));
    }

}
