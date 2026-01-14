package org.knime.filehandling.core.example.node.writer.port;

import java.awt.GridBagLayout;

import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.node.portobject.writer.PortObjectWriterNodeDialog;
import org.knime.filehandling.core.util.GBCBuilder;

final class PortObjectWriterSkeletonNodeDialog extends PortObjectWriterNodeDialog<PortObjectWriterSkeletonNodeConfig> {

    private final DialogComponentBoolean m_exampleCheckbox;

    public PortObjectWriterSkeletonNodeDialog(final PortObjectWriterSkeletonNodeConfig config, final String historyID) {
        super(config, historyID);
        m_exampleCheckbox = new DialogComponentBoolean(config.getExampleModel(), "Example checkbox");
        addAdditionalPanel(createValidationSettingsPanel());
    }

    private JPanel createValidationSettingsPanel() {
        final var panel = new JPanel(new GridBagLayout());
        panel.setBorder(
            BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "An additional example panel"));
        final GBCBuilder gbc = new GBCBuilder().setWeightX(1).resetPos().anchorLineStart();
        panel.add(m_exampleCheckbox.getComponentPanel(), gbc.build());
        return panel;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_exampleCheckbox.saveSettingsTo(settings);
        super.saveSettingsTo(settings);
    }

    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        super.loadSettingsFrom(settings, specs);
        m_exampleCheckbox.loadSettingsFrom(settings, specs);
    }
}
