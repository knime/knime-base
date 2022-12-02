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
 *   Nov 4, 2022 (Alexander Bondaletov): created
 */
package org.knime.filehandling.core.connections.base.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;

import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;

/**
 * The editor component for the browser relativization behavior settings.
 *
 * @author Alexander Bondaletov
 */
public final class WorkingDirectoryRelativizationPanel extends JPanel {

    private static final long serialVersionUID = 1L;

    /**
     * @param relativizePaths The relativize paths settings model.
     */
    public WorkingDirectoryRelativizationPanel(final SettingsModelBoolean relativizePaths) {
        this(relativizePaths, new Insets(0, 0, 5, 0));
    }

    /**
     * @param relativizePaths The relativize paths settings model.
     * @param insets The insets for the checkbox component.
     */
    public WorkingDirectoryRelativizationPanel(final SettingsModelBoolean relativizePaths, final Insets insets) {
        var input = new DialogComponentBoolean(relativizePaths, "Relativize selected path after browsing");

        setLayout(new GridBagLayout());
        var c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.NONE;
        c.gridx = 0;
        c.gridy = 0;
        c.insets = insets;
        add(input.getComponentPanel().getComponent(0), c);

        c.weightx = 1;
        c.gridx += 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        add(Box.createHorizontalGlue(), c);
        setBorder(BorderFactory.createTitledBorder("Working directory"));
    }
}
