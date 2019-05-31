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

package org.knime.base.data.filter.row.dialog.panel;

import javax.swing.BoxLayout;
import javax.swing.JPanel;

import org.knime.base.data.filter.row.dialog.OperatorPanel;
import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.core.data.DataType;
import org.knime.core.data.time.localdate.LocalDateCellFactory;

/**
 * Default panel with two DateTime fields for condition filters.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public class TwoDateTimeFieldsPanel extends JPanel implements OperatorPanel {

    private static final long serialVersionUID = -3921896691680007464L;

    private final DateTimeField m_firstField;

    private final DateTimeField m_secondField;

    /**
     * Constructs a {@link TwoDateTimeFieldsPanel} object.
     */
    public TwoDateTimeFieldsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        m_firstField = new DateTimeField(this, LocalDateCellFactory.TYPE);

        m_secondField = new DateTimeField(this, LocalDateCellFactory.TYPE);
    }

    @Override
    public void init(final OperatorPanelParameters parameters) {
        final DataType type = parameters.getColumnSpec().getType();
        m_firstField.setType(type);
        m_secondField.setType(type);
        final String[] values = parameters.getValues();
        m_firstField.setValue(values == null || values.length == 0 ? null : values[0]);
        m_secondField.setValue(values == null || values.length < 2 ? null : values[1]);
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public String[] getValues() {
        return new String[]{m_firstField.getValue(), m_secondField.getValue()};
    }

}
