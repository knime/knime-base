package org.knime.filehandling.core.nodes.localfsconnection;

import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModelString;

/**
 * Node dialog for the local file system connection node
 *
 * @author Tobias Urhaug, KNIME GmbH, Berlin, Germany
 */
class LocalFSConnectionNodeDialog extends DefaultNodeSettingsPane {

    protected LocalFSConnectionNodeDialog() {
        super();
        addDialogComponent(new DialogComponentString(createNumberFormatSettingsModel(), "Connection name"));
    }

    static SettingsModelString createNumberFormatSettingsModel() {
        return new SettingsModelString("Connection name", "");
    }

}