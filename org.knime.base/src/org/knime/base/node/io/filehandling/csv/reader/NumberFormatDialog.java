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
 *   Jul 2, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Objects;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.filehandling.core.defaultnodesettings.status.DefaultStatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage;
import org.knime.filehandling.core.defaultnodesettings.status.StatusMessage.MessageType;
import org.knime.filehandling.core.defaultnodesettings.status.StatusView;

/**
 * Dialog for the number format. It consists of two textfields, one for the thousands and one for the decimal separator,
 * as well as an error label that is displayed if the user input is invalid. The input is invalid if the two separators
 * are the same or no decimal separator is provided.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class NumberFormatDialog {

    private static final StatusMessage SAME_SEPARATORS_ERROR =
        new DefaultStatusMessage(MessageType.ERROR, "The separators must differ.");

    private static final StatusMessage EMPTY_DECIMAL_SEPARATOR_ERROR =
        new DefaultStatusMessage(MessageType.ERROR, "Please specify a decimal separator.");

    private final JTextField m_thousandsSeparatorField = CSVReaderDialogUtils.mkTextField();

    private final JTextField m_decimalSeparatorField = CSVReaderDialogUtils.mkTextField();

    private String m_columnDelimiter = ",";

    private final StatusView m_status = new StatusView();

    NumberFormatDialog() {
        final DocumentListener listener = new DocumentListener() {

            @Override
            public void insertUpdate(final DocumentEvent e) {
                validateSeparators();
            }

            @Override
            public void removeUpdate(final DocumentEvent e) {
                validateSeparators();
            }

            @Override
            public void changedUpdate(final DocumentEvent e) {
                validateSeparators();
            }

        };

        m_decimalSeparatorField.getDocument().addDocumentListener(listener);
        m_thousandsSeparatorField.getDocument().addDocumentListener(listener);
    }

    private void validateSeparators() {
        final String decimalSeparator = getDecimalSeparator();
        if (decimalSeparator.isEmpty()) {
            m_status.setStatus(EMPTY_DECIMAL_SEPARATOR_ERROR);
        } else if (separatorsAreEqual()) {
            m_status.setStatus(SAME_SEPARATORS_ERROR);
        } else if (Objects.equals(m_columnDelimiter, decimalSeparator)) {
            setColumnDelimiterError("decimal");
        } else if (Objects.equals(m_columnDelimiter, getThousandsSeparator())) {
            setColumnDelimiterError("thousands");
        } else {
            m_status.clearStatus();
        }
    }

    private void setColumnDelimiterError(final String separatorLabel) {
        m_status.setStatus(new DefaultStatusMessage(MessageType.WARNING,
            "The %s separator can't be the same as the column delimiter ('%s').", separatorLabel, m_columnDelimiter));
    }

    private boolean separatorsAreEqual() {
        return Objects.equals(getDecimalSeparator(), getThousandsSeparator());
    }

    private String getThousandsSeparator() {
        final String thousandsSeparator = m_thousandsSeparatorField.getText();
        return thousandsSeparator.replace("\0", "");
    }

    private String getDecimalSeparator() {
        return m_decimalSeparatorField.getText();
    }

    void setColumnDelimiter(final String columnDelimiter) {
        m_columnDelimiter = columnDelimiter;
        validateSeparators();
    }

    void registerDocumentListener(final DocumentListener listener) {
        m_thousandsSeparatorField.getDocument().addDocumentListener(listener);
        m_decimalSeparatorField.getDocument().addDocumentListener(listener);
    }

    void load(final CSVTableReaderConfig config) {
        m_thousandsSeparatorField.setText(config.getThousandsSeparator());
        m_decimalSeparatorField.setText(config.getDecimalSeparator());
    }

    void save(final CSVTableReaderConfig config) {
        config.setThousandsSeparator(m_thousandsSeparatorField.getText());
        config.setDecimalSeparator(m_decimalSeparatorField.getText());
    }

    JPanel getPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(CSVReaderDialogUtils.createBorder("Number format"));
        final GridBagConstraints gbc = CSVReaderDialogUtils.createAndInitGBC();
        gbc.insets = new Insets(5, 5, 0, 0);
        panel.add(new JLabel("Thousands separator"), gbc);
        gbc.gridx++;
        panel.add(m_thousandsSeparatorField, gbc);
        gbc.gridy++;
        gbc.gridx = 0;
        panel.add(new JLabel("Decimal separator"), gbc);
        gbc.gridx++;
        panel.add(m_decimalSeparatorField, gbc);
        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        panel.add(m_status.getLabel(), gbc);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(new JPanel(), gbc);
        return panel;
    }
}
