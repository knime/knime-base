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
 *   Jun 10, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.defaultnodesettings.filechooser.reader;

import java.util.function.Function;

import org.knime.core.node.FlowVariableModel;
import org.knime.core.node.util.FileSystemBrowser.DialogType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractDialogComponentFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.AbstractSettingsModelFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.StatusMessageReporter;
import org.knime.filehandling.core.defaultnodesettings.fileselection.FileSelectionDialog;

/**
 * File chooser dialog component for reader nodes.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 * @noreference non-public API
 * @noinstantiate non-public API
 */
public final class DialogComponentReaderFileChooser
    extends AbstractDialogComponentFileChooser<SettingsModelReaderFileChooser> {

    /**
     * Constructor using a default status message calculator implementation.</br>
     * </br>
     * In order to create the {@link FlowVariableModel}, prefix the key chain returned by
     * {@link AbstractSettingsModelFileChooser#getKeysForFSLocation()} with the path to the settings model within your
     * settings structure.</br>
     * Suppose your settings have the structure foo/bar/model, then you can create the FlowVariableModel as follows:
     * <pre>
     * String[] keyChain = Stream.concat(Stream.of("foo", "bar"), Arrays.stream(model.getKeysForFSLocation())).toArray(String[]::new);
     * FlowVariableModel fvm = createFlowVariableModel(keyChain, FSLocationVariableType.INSTANCE);
     * </pre>
     *
     * @param model the {@link AbstractSettingsModelFileChooser} the dialog component interacts with
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param locationFvm the {@link FlowVariableModel} for the location
     */
    public DialogComponentReaderFileChooser(final SettingsModelReaderFileChooser model, final String historyID,
        final FlowVariableModel locationFvm) {
        this(model//
            , historyID//
            , locationFvm//
            , DefaultReaderStatusMessageReporter::new);
    }

    /**
     * Constructor.</br>
     * </br>
     * In order to create the {@link FlowVariableModel}, prefix the key chain returned by
     * {@link AbstractSettingsModelFileChooser#getKeysForFSLocation()} with the path to the settings model within your
     * settings structure.</br>
     * Suppose your settings have the structure foo/bar/model, then you can create the FlowVariableModel as follows:
     * <pre>
     * String[] keyChain = Stream.concat(Stream.of("foo", "bar"), Arrays.stream(model.getKeysForFSLocation())).toArray(String[]::new);
     * FlowVariableModel fvm = createFlowVariableModel(keyChain, FSLocationVariableType.INSTANCE);
     * </pre>
     *
     * @param model the {@link AbstractSettingsModelFileChooser} the dialog component interacts with
     * @param historyID id used to store file history used by {@link FileSelectionDialog}
     * @param locationFvm the {@link FlowVariableModel} for the location
     * @param statusMessageReporter function to create a {@link StatusMessageReporter} used to update the status of this
     *            component
     */
    public DialogComponentReaderFileChooser(final SettingsModelReaderFileChooser model, final String historyID,
        final FlowVariableModel locationFvm,
        final Function<SettingsModelReaderFileChooser, StatusMessageReporter> statusMessageReporter) {
        super(model//
            , historyID//
            , DialogType.OPEN_DIALOG//
            , "Read from"//
            , locationFvm//
            , statusMessageReporter);
    }

}
