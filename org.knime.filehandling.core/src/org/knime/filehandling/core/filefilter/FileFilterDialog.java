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
 *   Aug 21, 2019 (bjoern): created
 */
package org.knime.filehandling.core.filefilter;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 *
 * @author bjoern
 */
public class FileFilterDialog extends JDialog {


    private final FileFilterPanel m_fileFilterPanel;

    public FileFilterDialog(final Frame owner, final String[] defaultSuffixes) {
        super(owner, "File filter configuration", true);

        m_fileFilterPanel = new FileFilterPanel(defaultSuffixes);


        final JPanel rootPanel = new JPanel();
        rootPanel.setLayout(new GridBagLayout());

        final GridBagConstraints gc = new GridBagConstraints();
        gc.anchor = GridBagConstraints.CENTER;
        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.insets = new Insets(10, 10, 10, 10);
        gc.fill = GridBagConstraints.BOTH;
        gc.weightx = 1;
        gc.weighty = 1;

        rootPanel.add(m_fileFilterPanel, gc);

        //buttons
        gc.anchor = GridBagConstraints.LINE_END;
        gc.fill = GridBagConstraints.NONE;
        gc.weightx = 1;
        gc.weighty = 0;
        gc.ipadx = 20;
        gc.gridwidth = 1;
        gc.gridx = 0;
        gc.gridy = 1;
        gc.insets = new Insets(0, 10, 10, 0);
        final JButton okButton = new JButton("OK");
        okButton.addActionListener((e) -> onOk());
        rootPanel.add(okButton, gc);

        gc.anchor = GridBagConstraints.LINE_START;
        gc.weightx = 0;
        gc.ipadx = 10;
        gc.gridx = 1;
        gc.insets = new Insets(0, 5, 10, 10);
        final JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener((e) -> onCancel());
        rootPanel.add(cancelButton, gc);

        setContentPane(rootPanel);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        setAlwaysOnTop(true);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent we) {
                //handle all window closing events triggered by none of
                //the given buttons
                onCancel();
            }
        });
//        setMinimumSize(new Dimension(DIALOG_WIDTH, DIALOG_HEIGHT));
        pack();
    }

    /**
     * @param dialog the dialog to close
     */
    private void closeDialog() {
        setVisible(false);
        dispose();
    }

    private void onOk() {
        closeDialog();
    }

    private void onCancel() {
        closeDialog();
    }

    /**
     * Sets the filter type to the filter type combo box.
     *
     * @param filterType the filter type to set
     */
    public void setFilterType(final String filterType) {
        // TODO Check if type exists
        m_fileFilterPanel.setFilterType(filterType);
    }

    /**
     * Returns the selected filter type.
     *
     * @return the filter type
     */
    public String getSelectedFilterType() {
        return m_fileFilterPanel.getSelectedFilterType();
    }

    /**
     * Sets the filter expression to the filter type combo box.
     *
     * @param filterExpression the filter expression to set
     */
    public void setFilterExpression(final String filterExpression) {
        m_fileFilterPanel.setFilterExpression(filterExpression);
    }

    /**
     * Returns the selected filter expression.
     *
     * @return the filter expression
     */
    public String getSelectedFilterExpression() {
        return m_fileFilterPanel.getSelectedFilterExpression();
    }

    /**
     * Sets case sensitivity of file filter.
     *
     * @param caseSensitive case sensitivity
     */
    public void setCaseSensitive(final boolean caseSensitive) {
        m_fileFilterPanel.setCaseSensitive(caseSensitive);
    }

    /**
     * Returns if file filter is case sensitive or not.
     *
     * @return true, if file filter is case sensitive or not
     */
    public boolean getCaseSensitive() {
        return m_fileFilterPanel.getCaseSensitive();
    }

    /**
     * Method to enable/disable the components.
     *
     * @param enabled true, if components should be disabled
     */
    public void enableComponents(final boolean enabled) {
        m_fileFilterPanel.enableComponents(enabled);
    }
}
