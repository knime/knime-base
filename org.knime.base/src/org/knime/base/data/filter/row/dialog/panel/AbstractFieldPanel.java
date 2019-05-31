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

import java.awt.Color;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.Border;

import org.knime.base.data.filter.row.dialog.OperatorPanel;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.ValidationResult.OperandError;

/**
 * Abstract panel for n fields for condition filters.
 *
 * @author Sascha Wolke, KNIME GmbH
 * @since 4.0
 */
abstract class AbstractFieldPanel extends JPanel implements OperatorPanel {
    private static final long serialVersionUID = -178672669602975655L;

    private static final int DEFAULT_TEXT_FIELD_SIZE = 15;

    private Color m_originalTextColor;

    private Border m_originalBorder;

    private Border m_redBorder = BorderFactory.createLineBorder(Color.RED, 2);

    private Consumer<String[]> m_onChangeConsumer;

    /**
     * Creates a text field with predefined {@link KeyAdapter}.
     *
     * @return new {@link JTextField} object
     */
    protected JTextField createTextField() {
        final JTextField textField = new JTextField(DEFAULT_TEXT_FIELD_SIZE);
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(final KeyEvent event) {
                if (m_onChangeConsumer != null) {
                    m_onChangeConsumer.accept(getValues());
                }
            }
        });
        return textField;
    }

    @Override
    public JPanel getPanel() {
        return this;
    }

    @Override
    public void setOnValuesChanged(final Consumer<String[]> consumer) {
        m_onChangeConsumer = consumer;
    }

    /**
     * Changes the color of a text field based on validation results.
     *
     * @param result validation result
     * @param textField text field to change
     * @param param parameter index in validation result (starting at 0)
     */
    protected void setValidationResult(final ValidationResult result, final JTextField textField, final int param) {
        if (m_originalTextColor == null) {
            m_originalTextColor = textField.getForeground();
            m_originalBorder = textField.getBorder();
        }

        if (hasErrors(result, param)) {
            textField.setForeground(Color.RED);
            textField.setBorder(m_redBorder);
        } else {
            textField.setForeground(m_originalTextColor);
            textField.setBorder(m_originalBorder);
        }
    }

    /**
     * Checks if validation result has errors.
     *
     * @return <code>true</code> if result contains errors for given parameter
     */
    private static boolean hasErrors(final ValidationResult result, final int parameter) {
        for (OperandError error : result.getErrors()) {
            if (error.getIdx() == parameter) {
                return true;
            }
        }

        return false;
    }
}
