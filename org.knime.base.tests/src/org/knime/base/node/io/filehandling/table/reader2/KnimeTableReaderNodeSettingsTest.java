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
 *   Jan 29, 2024 (hornm): created
 */
package org.knime.base.node.io.filehandling.table.reader2;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeSettings.AdvancedSettings.HowToCombineColumnsOptionPersistor;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeSettings.AdvancedSettings.IfSchemaChangesOption;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeSettings.AdvancedSettings.IfSchemaChangesPersistor;
import org.knime.base.node.io.filehandling.table.reader2.KnimeTableReaderNodeSettings.AdvancedSettings.SkipFirstDataRowsPersistor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.FieldNodeSettingsPersistor;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;
import org.knime.testing.node.dialog.SnapshotTestConfiguration;

/**
 * {@link DefaultNodeSettingsSnapshotTest} for the {@link KnimeTableReaderNodeSettings}.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("restriction")
class KnimeTableReaderNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {
    protected KnimeTableReaderNodeSettingsTest() {
        super(getConfig());
    }

    private static SnapshotTestConfiguration getConfig() {
        return SnapshotTestConfiguration.builder() //
            .testJsonFormsForModel(KnimeTableReaderNodeSettings.class) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> createSettings()) //
            .testJsonFormsWithInstance(SettingsType.MODEL, () -> readSettings()) //
            .testNodeSettingsStructure(() -> createSettings()) //
            .testNodeSettingsStructure(() -> readSettings()) //
            .build();
    }

    private static KnimeTableReaderNodeSettings createSettings() {
        var res = new KnimeTableReaderNodeSettings();
        res.m_settings.m_source.m_path = new FSLocation(FSCategory.RELATIVE, "foo");
        return res;
    }

    private static KnimeTableReaderNodeSettings readSettings() {
        try {
            var path = getSnapshotPath(KnimeTableReaderNodeSettingsTest.class).getParent().resolve("node_settings")
                .resolve("KnimeTableReaderNodeSettings.xml");
            try (var fis = new FileInputStream(path.toFile())) {
                var nodeSettings = NodeSettings.loadFromXML(fis);
                return DefaultNodeSettings.loadSettings(nodeSettings.getNodeSettings(SettingsType.MODEL.getConfigKey()),
                    KnimeTableReaderNodeSettings.class);
            }
        } catch (IOException | InvalidSettingsException e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest
    @MethodSource
    void testHowToCombineColumnsOptionPersistor(final HowToCombineColumnsOption howToCombineColumnsOption)
        throws InvalidSettingsException {
        final var copy = saveLoad(HowToCombineColumnsOptionPersistor.class, HowToCombineColumnsOption.class,
            howToCombineColumnsOption);
        assertEquals(howToCombineColumnsOption, copy);
    }

    private static Stream<HowToCombineColumnsOption> testHowToCombineColumnsOptionPersistor() {
        return Stream.of(HowToCombineColumnsOption.FAIL, HowToCombineColumnsOption.UNION,
            HowToCombineColumnsOption.INTERSECTION);
    }

    @ParameterizedTest
    @MethodSource
    void testIfSchemaChangesPersistor(final IfSchemaChangesOption ifSchemaChangesOption)
        throws InvalidSettingsException {
        final var copy = saveLoad(IfSchemaChangesPersistor.class, IfSchemaChangesOption.class, ifSchemaChangesOption);
        assertEquals(ifSchemaChangesOption, copy);
    }

    private static Stream<IfSchemaChangesOption> testIfSchemaChangesPersistor() {
        return Stream.of(IfSchemaChangesOption.FAIL, IfSchemaChangesOption.USE_NEW_SCHEMA,
            IfSchemaChangesOption.IGNORE);
    }

    @ParameterizedTest
    @MethodSource
    void testSkipFirstDataRowsPersistor(final Long l) throws InvalidSettingsException {
        final var copy = saveLoad(SkipFirstDataRowsPersistor.class, Long.class, l);
        assertEquals(l, copy);
    }

    private static Stream<Long> testSkipFirstDataRowsPersistor() {
        return Stream.of(0l, 1l);
    }

    static <S, P extends FieldNodeSettingsPersistor<S>> S saveLoad(final Class<P> persistorType,
        final Class<S> settingsType, final S value) throws InvalidSettingsException {
        var persistor = FieldNodeSettingsPersistor.createInstance(persistorType, settingsType, "key");
        var nodeSettings = new NodeSettings("settings");
        persistor.save(value, nodeSettings);
        return persistor.load(nodeSettings);
    }
}
