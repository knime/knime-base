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
 * -------------------------------------------------------------------
 *
 */
package org.knime.filehandling.core.node.portobject.reader;

import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.DialogComponentReaderFileChooser;
import org.knime.filehandling.core.defaultnodesettings.fileselection.FileSelectionDialog;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;
import org.knime.filehandling.core.node.portobject.PortObjectIONodeDialog;
import org.knime.filehandling.core.node.portobject.SelectionMode;

/**
 * Node dialog for port object reader nodes that can be extended with additional settings components.
 *
 * @author Simon Schmid, KNIME GmbH, Konstanz, Germany
 * @param <C> the config of the node
 * @noreference non-public API
 * @noextend non-public API
 * @noinstantiate non-public API
 */
public class PortObjectReaderNodeDialog<C extends PortObjectReaderNodeConfig> extends PortObjectIONodeDialog<C> {

    /**
     * Constructor.
     *
     * @param config the config
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param filterModes the available {@link FilterMode FilterModes} (if a none are provided, the default filter mode
     *            from <b>model</b> is used)
     */
    private PortObjectReaderNodeDialog(final C config, final String historyID, final FilterMode[] filterModes) {
        super(config,
            fvm -> new DialogComponentReaderFileChooser(config.getFileChooserModel(), historyID, fvm, filterModes));
    }

    /**
     * Constructor.
     *
     * @param config the config
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param selectionMode the available {@link SelectionMode}s
     */
    public PortObjectReaderNodeDialog(final C config, final String historyID, final SelectionMode selectionMode) {
        this(config, historyID, selectionMode.getFilters());
    }

    /**
     * Constructor.
     *
     * @param config the config
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     */
    public PortObjectReaderNodeDialog(final C config, final String historyID) {
        this(config, historyID, new FilterMode[0]);
    }

}
