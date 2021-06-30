package org.knime.filehandling.core.example.node.writer.table;

import java.awt.Component;
import java.awt.GridBagLayout;

import javax.swing.JPanel;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeDialogPane;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.DialogComponentWriterFileChooser;
import org.knime.filehandling.core.defaultnodesettings.filechooser.writer.SettingsModelWriterFileChooser;
import org.knime.filehandling.core.util.GBCBuilder;

final class TableWriterSkeletonNodeDialog extends NodeDialogPane {

    private final DialogComponentWriterFileChooser m_writerFileChooser;

    TableWriterSkeletonNodeDialog(final TableWriterSkeletonSettings settings) {
        final SettingsModelWriterFileChooser writeFileChooser = settings.getWriterFileChooser();
        m_writerFileChooser = new DialogComponentWriterFileChooser(writeFileChooser, "dummy_history_id",
            createFlowVariableModel(writeFileChooser.getKeysForFSLocation(), FSLocationVariableType.INSTANCE));
        addTab("Settings", createWriterPanel());
    }

    private Component createWriterPanel() {
        final JPanel p = new JPanel(new GridBagLayout());
        final GBCBuilder gbc = new GBCBuilder().setWeightX(1).resetPos().anchorLineStart().fillHorizontal();
        p.add(m_writerFileChooser.getComponentPanel(), gbc.build());
        p.add(new JPanel(), gbc.setWeightY(1).fillBoth().incY().build());
        return p;
    }

    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) throws InvalidSettingsException {
        m_writerFileChooser.saveSettingsTo(settings);
    }

    // Make sure to overwrite the PortObjectSpec[] version!
    @Override
    protected void loadSettingsFrom(final NodeSettingsRO settings, final PortObjectSpec[] specs)
        throws NotConfigurableException {
        m_writerFileChooser.loadSettingsFrom(settings, specs);
    }

    @Override
    public void onClose() {
        // this will cancel all threads created by the writer file chooser
        m_writerFileChooser.onClose();
        super.onClose();
    }
}
