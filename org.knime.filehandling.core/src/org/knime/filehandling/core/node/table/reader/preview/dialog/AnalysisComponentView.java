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
 *   Aug 5, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.preview.dialog;

import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import org.knime.core.node.util.SharedIcons;
import org.knime.filehandling.core.defaultnodesettings.WordWrapJLabel;
import org.knime.filehandling.core.node.table.reader.preview.dialog.AnalysisComponentModel.MessageType;
import org.knime.filehandling.core.util.GBCBuilder;

/**
 * View of the analysis components of a table reader preview.</br>
 * Also contains an error label.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
final class AnalysisComponentView extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JButton m_quickScanButton = new JButton("Stop file scanning");

    private final JLabel m_analysisProgressLabel = new JLabel();

    private final JLabel m_analysisProgressPathLabel = new JLabel();

    private final WordWrapJLabel m_errorLabel = new WordWrapJLabel(" ", 500);

    private final JProgressBar m_analysisProgressBar;

    private final transient AnalysisComponentModel m_model;

    AnalysisComponentView(final AnalysisComponentModel model) {
        m_model = model;
        m_quickScanButton.setModel(m_model.getQuickScanModel());
        m_analysisProgressBar = new JProgressBar(m_model.getProgress());
        update();
        m_model.addChangeListener(e -> update());
        layoutPanel();
    }

    private void update() {
        m_analysisProgressBar.setIndeterminate(m_model.isProgressIndeterminate());
        m_analysisProgressLabel.setIcon(getIcon(m_model.getAnalysisProgressMessageType()));
        m_analysisProgressLabel.setText(m_model.getAnalysisProgressText());
        m_analysisProgressPathLabel.setText(m_model.getCurrentPath());
        m_errorLabel.setIcon(getIcon(m_model.getErrorMessageType()));
        m_errorLabel.setText(m_model.getErrorText());
        m_analysisProgressBar.setVisible(m_model.showProgressBar());
        m_quickScanButton.setVisible(m_model.showQuickScanButton());
    }

    private static Icon getIcon(final MessageType messageType) {
        switch (messageType) {
            case ERROR:
                return SharedIcons.ERROR.get();
            case INFO:
                return SharedIcons.INFO.get();
            case SUCCESS:
                return SharedIcons.SUCCESS.get();
            case WARNING:
                return SharedIcons.WARNING_YELLOW.get();
            case NONE:
            default:
                return null;
        }
    }

    private void layoutPanel() {
        setLayout(new GridBagLayout());
        final GBCBuilder gbc =
            new GBCBuilder(new Insets(5, 5, 1, 1)).anchorFirstLineStart().ipadX(50).resetX().resetY();
        add(m_analysisProgressBar, gbc.build());
        add(m_quickScanButton, gbc.incX().ipadX(0).insets(0, 5, 1, 5).build());
        add(m_analysisProgressLabel, gbc.incX().setWeightX(1).insets(5, 5, 1, 1).build());
        add(m_errorLabel,
            gbc.resetX().incY().widthRemainder().setWeightX(1).fillHorizontal().insets(5, 5, 5, 5).build());
        add(m_analysisProgressPathLabel, gbc.build());
    }

}
