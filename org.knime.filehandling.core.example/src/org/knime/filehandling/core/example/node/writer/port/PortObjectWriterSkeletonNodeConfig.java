package org.knime.filehandling.core.example.node.writer.port;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.context.NodeCreationConfiguration;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.filehandling.core.node.portobject.writer.PortObjectWriterNodeConfig;

final class PortObjectWriterSkeletonNodeConfig extends PortObjectWriterNodeConfig {

    private final SettingsModelBoolean m_example;

    protected PortObjectWriterSkeletonNodeConfig(final NodeCreationConfiguration creationConfig) {
        super(builder(creationConfig).withFileSuffixes(".model"));
        m_example = new SettingsModelBoolean("example", false);
    }

    SettingsModelBoolean getExampleModel() {
        return m_example;
    }

    // save additional settings. Note that if you're not using a settings model
    // you'll also have to overwrite the inDialog versions of these methods
    @Override
    protected void saveConfigurationForModel(final NodeSettingsWO settings) {
        m_example.saveSettingsTo(settings);
        super.saveConfigurationForModel(settings);
    }

    @Override
    protected void loadConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_example.loadSettingsFrom(settings);
        super.loadConfigurationForModel(settings);
    }

    @Override
    protected void validateConfigurationForModel(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_example.validateSettings(settings);
        super.validateConfigurationForModel(settings);
    }

}
