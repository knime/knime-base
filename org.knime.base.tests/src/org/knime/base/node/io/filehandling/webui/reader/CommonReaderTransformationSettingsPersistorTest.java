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
 *   May 28, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.webui.reader;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderNodeSettings.AdvancedSettingsWithMultipleFileHandling.HowToCombineColumnsOption;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettings.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderTransformationSettingsStateProviders.TransformationElementSettingsProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.api.NodeSettingsPersistor;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;

/**
 * Test utils for implementations of {@link CommonReaderTransformationSettingsPersistor}.
 *
 * @author Paul Bärnreuther
 * @param <R> the reader specific transformation settings type
 */
@SuppressWarnings({"unchecked", "rawtypes", "restriction"})
public abstract class CommonReaderTransformationSettingsPersistorTest<R extends CommonReaderTransformationSettings> {

    private final CommonReaderTransformationSettingsPersistor m_persistor;

    private final Class<? extends NodeSettingsPersistor<R>> m_persistorClass;

    private final Class<R> m_settingsClass;

    /**
     * @param persistor
     * @param settingsClass
     */
    protected CommonReaderTransformationSettingsPersistorTest(
        final CommonReaderTransformationSettingsPersistor persistor, final Class<R> settingsClass) {
        m_persistor = persistor;
        m_persistorClass = (Class<? extends NodeSettingsPersistor<R>>)persistor.getClass();
        m_settingsClass = settingsClass;
    }

    /**
     * @param settings
     * @throws InvalidSettingsException
     */
    protected void testSaveLoad(final R settings) throws InvalidSettingsException {
        final R copy = (R)CommonReaderNodeSettingsTest.saveLoad(m_persistor, settings);
        assertEquals(settings.m_persistorSettings.m_takeColumnsFrom, copy.m_persistorSettings.m_takeColumnsFrom);
        assertEquals(settings.m_columnTransformation.length, copy.m_columnTransformation.length);
        for (int i = 0; i < settings.m_columnTransformation.length; i++) {
            assertEquals(settings.m_columnTransformation[i].m_columnName, copy.m_columnTransformation[i].m_columnName);
            assertEquals(settings.m_columnTransformation[i].m_includeInOutput,
                copy.m_columnTransformation[i].m_includeInOutput);
            assertEquals(settings.m_columnTransformation[i].m_columnRename,
                copy.m_columnTransformation[i].m_columnRename);
            assertEquals(settings.m_columnTransformation[i].m_originalType,
                copy.m_columnTransformation[i].m_originalType);
            assertEquals(settings.m_columnTransformation[i].m_type, copy.m_columnTransformation[i].m_type);
        }
    }

    /**
     * @return the default settings to use for testing (e.g. new CSVTransformationSettings())
     */
    protected abstract R constructSettings();

    /**
     * @param columnFilterMode
     * @return transformation settings with the specified columnFilterMode
     */
    protected R createTransformationSettings(final ColumnFilterMode columnFilterMode) {
        final R transformationSettings = constructSettings();
        transformationSettings.m_persistorSettings.m_takeColumnsFrom = columnFilterMode;
        return transformationSettings;
    }

    /**
     * @param <S> the type external data types are made serializable with
     * @param type of the column spec
     * @return transformation settings with a single column spec with the specified type
     */
    protected <S> R createTransformationSettingsWithSpecs(final S type) {
        final R transformationSettings = constructSettings();

        final var specs = List.of(new TableSpecSettings<>("foo", List.of(new ColumnSpecSettings<S>("bar", type))));
        transformationSettings.m_persistorSettings.m_specs = specs;
        transformationSettings.m_persistorSettings.m_appendPathColumn = true;
        transformationSettings.m_persistorSettings.m_fsLocations =
            new FSLocation[]{new FSLocation(FSCategory.LOCAL, "foo"), new FSLocation(FSCategory.LOCAL, "bar")};
        final var specMap = ReaderSpecific.toSpecMap(m_persistor, specs);
        transformationSettings.m_columnTransformation = new TransformationElementSettingsProvider() {

            @Override
            public ProductionPathProvider getProductionPathProvider() {
                return m_persistor.getProductionPathProvider();
            }

            @Override
            public TypeHierarchy getTypeHierarchy() {
                return m_persistor.getTypeHierarchy();
            }

            @Override
            public Class getTypedReaderTableSpecsProvider() {
                return null; // not needed
            }

            @Override
            public Object toSerializableType(final Object externalType) {
                return m_persistor.toSerializableType(externalType);
            }

            @Override
            public Object toExternalType(final Object serializedType) {
                return m_persistor.toExternalType(serializedType);
            }

            @Override
            protected TypeReference getTableSpecSettingsTypeReference() {
                return null;
            }

            @Override
            protected boolean hasMultipleFileHandling() {
                return true;
            }

        }.toTransformationElements(specMap, HowToCombineColumnsOption.UNION, new TransformationElementSettings[0],
            new TypedReaderTableSpec<>());
        return transformationSettings;
    }
}
