package org.knime.filehandling.core.example.node.writer.table;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.ports.PortsConfiguration;
import org.knime.filehandling.core.defaultnodesettings.EnumConfig;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.FileOverwritePolicy;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filtermode.SettingsModelFilterMode.FilterMode;

final class TableWriterSkeletonSettings {

    /**
     * The extensions that are selectable when browsing the file system and will be automatically appended when entering
     * the file name via the browser.
     */
    private final String[] FILE_EXTENSIONS = new String[]{".dat", ".tmp", "txt"};

    private final SettingsModelWriterFileChooser m_writerFileChooser;

    TableWriterSkeletonSettings(final PortsConfiguration portsCfg, final String fsPortId) {
        // the supported filtermode is set to file, i.e., only files can be written and
        // selected via the browser
        final EnumConfig<FilterMode> filterMode = EnumConfig.create(FilterMode.FILE);
        // the supported file overwrite policies if required you can also add
        // FileOverwritePolicy.APPEND or .IGNORE
        final EnumConfig<FileOverwritePolicy> overwritePolicies =
            EnumConfig.create(FileOverwritePolicy.FAIL, FileOverwritePolicy.OVERWRITE);
        m_writerFileChooser = new SettingsModelWriterFileChooser("output_path", portsCfg, fsPortId, filterMode,
            overwritePolicies, FILE_EXTENSIONS);
    }

    SettingsModelWriterFileChooser getWriterFileChooser() {
        return m_writerFileChooser;
    }

    void saveInModel(final NodeSettingsWO settings) {
        m_writerFileChooser.saveSettingsTo(settings);
    }

    void validateInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_writerFileChooser.validateSettings(settings);
    }

    void loadInModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_writerFileChooser.loadSettingsFrom(settings);
    }

}
