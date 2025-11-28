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
 *   Dec 1, 2025: created
 */
package org.knime.filehandling.utility.nodes.compress.filechooser;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.NodeParametersUtil;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.node.parameters.persistence.legacy.LegacyFileWriterWithOverwritePolicyOptions;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;
import org.knime.testing.node.dialog.updates.DialogUpdateSimulator;

/**
 * Snapshot test for {@link CompressFileChooserNodeParameters}.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @author AI Migration Pipeline
 */
@SuppressWarnings("restriction")
final class CompressFileChooserNodeParametersTest extends DefaultNodeSettingsSnapshotTest {

    CompressFileChooserNodeParametersTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        var portConfig = new CompressFileChooserNodeFactory().createNodeCreationConfig().getPortConfig().get();
        return SnapshotTestConfiguration.builder() //
            .withPortsConfiguration(portConfig) //
            .testJsonFormsForModel(CompressFileChooserNodeParameters.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static CompressFileChooserNodeParameters readSettings() {
        return readSettings("CompressFileChooserNodeParameters.xml");
    }

    private static CompressFileChooserNodeParameters readSettings(final String filename) {
        try {
            var path = getSnapshotPath(CompressFileChooserNodeParameters.class).getParent().resolve("node_settings")
                .resolve(filename);
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return NodeParametersUtil.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    CompressFileChooserNodeParameters.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @SuppressWarnings("static-method")
    void testExtensionUpdateOnCompressionFormatChange() {
        // given
        var settings = new CompressFileChooserNodeParameters();
        var outputFile = new FSLocation(FSCategory.LOCAL, "archive.zip");
        var fileSelection = new FileSelection(outputFile);
        settings.m_outputLocation = new LegacyFileWriterWithOverwritePolicyOptions(fileSelection, true,
            LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy.overwrite);
        var simulator = new DialogUpdateSimulator(settings, null);

        // when
        settings.m_compression = CompressFileChooserNodeParameters.CompressionFormat.TAR_GZ;
        var result = simulator.simulateValueChange("compression");

        // then
        assertThat(result.getValueUpdateAt("outputLocation", "file"))
            .isEqualTo(new FileSelection(new FSLocation(FSCategory.LOCAL, "archive.tar.gz")));
        assertThat(result.getUiStateUpdateAt(List.of("outputLocation", "file"), "fileExtension")).isEqualTo("tar.gz");
    }

    @Test
    @SuppressWarnings("static-method")
    void testUnknownExtensionIsNotUpdatedOnCompressionFormatChange() {
        // given
        var settings = new CompressFileChooserNodeParameters();
        var outputFile = new FSLocation(FSCategory.LOCAL, "archive.txt");
        var fileSelection = new FileSelection(outputFile);
        settings.m_outputLocation = new LegacyFileWriterWithOverwritePolicyOptions(fileSelection, true,
            LegacyFileWriterWithOverwritePolicyOptions.OverwritePolicy.overwrite);
        var simulator = new DialogUpdateSimulator(settings, null);

        // when
        settings.m_compression = CompressFileChooserNodeParameters.CompressionFormat.TAR_GZ;
        var result = simulator.simulateValueChange("compression");

        // then
        assertThat(result.hasNoValueUpdateAt("outputLocation", "file")).isTrue();
        assertThat(result.getUiStateUpdateAt(List.of("outputLocation", "file"), "fileExtension")).isEqualTo("tar.gz");
    }

}
