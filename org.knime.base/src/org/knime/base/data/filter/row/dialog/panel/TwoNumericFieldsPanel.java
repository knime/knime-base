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
 *   Sep 3, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.filter.row.dialog.panel;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JSpinner;

import org.knime.base.data.filter.row.dialog.OperatorPanelParameters;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;

/**
 * A panel with two spinner fields for numerical values.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class TwoNumericFieldsPanel extends AbstractFieldPanel {

    private static final long serialVersionUID = -2484880904993526490L;

    private final JSpinner m_firstSpinner;

    private final JSpinner m_secondSpinner;

    /**
     * Constructs a {@link TwoNumericFieldsPanel} object.
     */
    public TwoNumericFieldsPanel() {
        m_firstSpinner = createSpinner();
        m_secondSpinner = createSpinner();

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        add(m_firstSpinner, gbc);

        gbc.ipadx = 10;
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 20, 0, 0);
        add(m_secondSpinner, gbc);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(final OperatorPanelParameters parameters) {
        final String[] values = parameters.getValues();
        final ColumnSpec columnSpec = parameters.getColumnSpec();
        NumericFieldUtil.initSpinner(m_firstSpinner, columnSpec,
            values == null || values.length < 1 ? null : values[0]);
        NumericFieldUtil.initSpinner(m_secondSpinner, columnSpec,
            values == null || values.length < 2 ? null : values[1]);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getValues() {
        return new String[]{m_firstSpinner.getValue().toString(), m_secondSpinner.getValue().toString()};
    }

    @Override
    public void setValidationResult(final ValidationResult result) {
        setValidationResult(result, m_firstSpinner, 0);
        setValidationResult(result, m_secondSpinner, 1);

    }

}
