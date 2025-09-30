package org.knime.base.node.meta.looper.recursive;


import java.io.FileInputStream;
import java.io.IOException;

import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;


@SuppressWarnings("restriction")
final class RecursiveLoopEndDynamicNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    RecursiveLoopEndDynamicNodeParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(createInputPortSpecs()) //
            .testJsonFormsForModel(RecursiveLoopEndDynamicNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static PortObjectSpec[] createInputPortSpecs() {
        // Single collector and recursion port
        return new PortObjectSpec[]{
            new DataTableSpec(new String[]{"Collector"}, new DataType[]{StringCell.TYPE}),
            new DataTableSpec(new String[]{"Recursion"}, new DataType[]{StringCell.TYPE})
        };
    }

    private static RecursiveLoopEndDynamicNodeParameters readSettings() {
        try {
            var path = getSnapshotPath(RecursiveLoopEndDynamicNodeParameters.class).getParent().resolve("node_settings")
                .resolve("RecursiveLoopEndDynamicNodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    RecursiveLoopEndDynamicNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }
}
