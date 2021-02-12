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

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JTextField;

import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.ValidationResult;

/**
 * Default panel with two fields for condition filters.
 *
 * @author Viktor Buria
 * @since 4.0
 */
public class TwoFieldsPanel extends AbstractFieldPanel {

    private static final long serialVersionUID = 5055834940172974499L;

    private static final int STRUT_WIDTH = 5;

    private final JTextField m_firstTextField;
    private final JTextField m_secondTextField;

    /**
     * Constructs a {@link TwoFieldsPanel} object.
     */
    public TwoFieldsPanel() {
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));// NOSONAR, can't fix because it's API

        m_firstTextField = createTextField();// NOSONAR, can't fix because it's API
        add(m_firstTextField);// NOSONAR, can't fix because it's API

        add(Box.createHorizontalStrut(STRUT_WIDTH));// NOSONAR, can't fix because it's API

        m_secondTextField = createTextField();// NOSONAR, can't fix because it's API
        add(m_secondTextField);// NOSONAR, can't fix because it's API
    }

    @Override
    public void init(final OperatorPanelParameters parameters) {
        final String[] values = parameters.getValues();
        m_firstTextField.setText(values == null || values.length == 0 ? "" : values[0]);
        m_secondTextField.setText(values == null || values.length < 2 ? "" : values[1]);
    }

    @Override
    public String[] getValues() {
        return new String[]{m_firstTextField.getText(), m_secondTextField.getText()};
    }

    @Override
    public void setValidationResult(final ValidationResult result) {
        setValidationResult(result, m_firstTextField, 0);
        setValidationResult(result, m_secondTextField, 1);
    }
}
