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
 *   22 May 2020 (Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.text.DefaultFormatter;

/**
 * Dialog for setting the number of characters used for the format autodetection.
 *
 * @author Timmo Waller-Ehrat, KNIME GmbH, Konstanz, Germany
 */
public class CSVAutodetectionSettingsDialog extends JDialog {

    private static final long serialVersionUID = 1L;

    private int m_autoDetectionBufferSize;

    private JSpinner m_bufferSizeSelection;

    private static final int MINIMUM_BUFFER_SIZE = 1;

    private static final int MAXIMUM_BUFFER_SIZE = Integer.MAX_VALUE;

    /**
     * Initializes a new dialog window with the last saved buffer size.
     *
     * @param parent the parent frame needed to make the dialog modal
     * @param bufferSize the last saved buffer size
     */
    public CSVAutodetectionSettingsDialog(final Frame parent, final int bufferSize) {
        super(parent, "Autodetection settings", true);
        m_autoDetectionBufferSize = bufferSize;
        initialize();
    }

    private void initialize() {
        this.setContentPane(getJContentPane());
        this.pack();
        this.setLocationRelativeTo(getParent());
        this.setResizable(false);
    }

    private JPanel getJContentPane() {
        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.insets = new Insets(10, 10, 10, 10);
        contentPane.add(getMainPanel(), gbc);

        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 10, 0);
        contentPane.add(getControlPanel(), gbc);

        return contentPane;
    }

    private JPanel getControlPanel() {
        final JPanel controlPanel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;

        controlPanel.add(getOkButton(), gbc);

        gbc.gridx++;
        gbc.insets = new Insets(0, 5, 0, 0);
        controlPanel.add(getCancelButton(), gbc);

        return controlPanel;
    }

    private JPanel getMainPanel() {
        final JPanel mainPanel = new JPanel(new GridBagLayout());
        final JLabel header = new JLabel("Number of characters for autodetection:");

        final SpinnerNumberModel spinnerModel =
            new SpinnerNumberModel(m_autoDetectionBufferSize, MINIMUM_BUFFER_SIZE, MAXIMUM_BUFFER_SIZE, 1024);

        m_bufferSizeSelection = new JSpinner(spinnerModel);

        ((DefaultFormatter)((JSpinner.DefaultEditor)m_bufferSizeSelection.getEditor()).getTextField().getFormatter())
            .setAllowsInvalid(false);

        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.gridx = 0;
        gbc.gridy = 0;
        mainPanel.add(header, gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 0, 0, 0);
        mainPanel.add(m_bufferSizeSelection, gbc);

        return mainPanel;
    }

    private JButton getOkButton() {
        final JButton okButton = new JButton();
        okButton.setText("Ok");
        okButton.addActionListener(e -> {
            m_autoDetectionBufferSize = (int)m_bufferSizeSelection.getValue();
            setVisible(false);
        });

        return okButton;
    }

    private JButton getCancelButton() {
        final JButton cancelButton = new JButton();
        cancelButton.setText("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        return cancelButton;
    }

    int getBufferSize() {
        return m_autoDetectionBufferSize;
    }
}
