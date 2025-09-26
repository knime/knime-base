package org.knime.base.node.preproc.colconvert.categorytonumber2;

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
final class CategoryToNumberNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    CategoryToNumberNodeParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .withInputPortObjectSpecs(new PortObjectSpec[]{ //
                new DataTableSpec( //
                    new String[]{"String column"}, //
                    new DataType[]{StringCell.TYPE} //
                )}) //
            .testJsonFormsForModel(CategoryToNumberNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static CategoryToNumberNodeParameters readSettings() {
        try {
            var path = getSnapshotPath(CategoryToNumberNodeParameters.class).getParent().resolve("node_settings")
                .resolve("CategoryToNumberNodeParameters.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    CategoryToNumberNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }
}
