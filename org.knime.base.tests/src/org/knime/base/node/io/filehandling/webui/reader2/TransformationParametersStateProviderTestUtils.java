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
package org.knime.base.node.io.filehandling.webui.reader2;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.IntStream;

import org.knime.base.node.io.filehandling.webui.reader2.ReaderSpecific.ExternalDataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParameters.TransformationElementSettings;
import org.knime.core.data.DataType;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.connections.FSCategory;
import org.knime.filehandling.core.connections.FSLocation;
import org.knime.node.parameters.WidgetGroup;
import org.knime.testing.node.dialog.updates.UpdateSimulator;
import org.knime.testing.node.dialog.updates.UpdateSimulator.UpdateSimulatorResult;

/**
 * Utility class for reader tests to access package-scoped fields relevant to testing updates within the transformation
 * table spec settings.
 *
 * @author Paul Bärnreuther
 */
public class TransformationParametersStateProviderTestUtils {

    private TransformationParametersStateProviderTestUtils() {
        // Utility class
    }

    /**
     * Use this method to assert the result of a simulated value update of specs.
     *
     * @param <S> the serialize type for external data types
     * @param specs to assert
     * @param file the associated file
     * @param columNames the names of the columns
     * @param types the types of the columns
     */
    public static <S> void assertTableSpec(final TableSpecSettings[] specs, final String file,
        final String[] columNames, final List<S> types) {
        assertThat(specs).hasSize(1);
        final var spec = specs[0];
        if (file != null) {
            assertThat(spec.m_fsLocation.getPath()).isEqualTo(file);
        }
        assertThat(spec.m_spec).hasSize(columNames.length);
        for (int i = 0; i < columNames.length; i++) {
            assertThat(spec.m_spec[i].m_name).isEqualTo(columNames[i]);
            assertThat(spec.m_spec[i].m_type).isEqualTo(types.get(i));
        }
    }

    /**
     * Use this method to assert the result of a simulated value update of specs.
     *
     * @param <S> the serialize type for external data types
     * @param specsObj to assert
     * @param columNames the names of the columns
     * @param types the types of the columns
     */
    public static <S> void assertTableSpec(final TableSpecSettings[] specsObj, final String[] columNames,
        final List<S> types) {
        assertTableSpec(specsObj, null, columNames, types);
    }

    /**
     * Use this method to prepare a test that accesses the existing specs field.
     *
     * @param transformationSettings to modify
     * @param columnNames the existing column names
     * @param columnTypes and their types
     */
    public static void setExistingTableSpecs(final TransformationParameters<?> transformationSettings,
        final List<String> columnNames, final List<String> columnTypes) {
        final var columnSpecSettings = IntStream.range(0, columnNames.size())
            .mapToObj(i -> new ColumnSpecSettings(columnNames.get(i), columnTypes.get(i)))
            .toArray(ColumnSpecSettings[]::new);
        transformationSettings.m_specs = new TableSpecSettings[]{
            new TableSpecSettings(new FSLocation(FSCategory.LOCAL, "existingSource"), columnSpecSettings)};
    }

    /**
     * @param transformationElementSettingsObj the transformation element settings to assert
     * @param columnNames the names of the columns
     * @param includeInOutputs whether the columns are to be included in the output
     * @param columnRenames the new names which columns are to be renamed to
     * @param types the types of the columns
     */
    public static void assertTransformationElementSettings(final Object transformationElementSettingsObj,
        final String[] columnNames, final boolean[] includeInOutputs, final String[] columnRenames,
        final String[] types) {
        assertTransformationElementSettings(transformationElementSettingsObj, columnNames, includeInOutputs,
            columnRenames, types, types);
    }

    /**
     * @param transformationElementSettingsObj the transformation element settings to assert
     * @param columnNames the names of the columns
     * @param includeInOutputs whether the columns are to be included in the output
     * @param columnRenames the new names which columns are to be renamed to
     * @param types the types of the columns
     * @param originalTypes the original types of the columns
     */
    public static void assertTransformationElementSettings(final Object transformationElementSettingsObj,
        final String[] columnNames, final boolean[] includeInOutputs, final String[] columnRenames,
        final String[] types, final String[] originalTypes) {
        final var transformationElements = (TransformationElementSettings[])transformationElementSettingsObj;
        assertThat(transformationElements).hasSize(columnNames.length);

        for (int i = 0; i < columnNames.length; i++) {
            assertThat(transformationElements[i].m_columnName).isEqualTo(columnNames[i]);
            assertThat(transformationElements[i].m_includeInOutput).isEqualTo(includeInOutputs[i]);
            assertThat(transformationElements[i].m_columnRename).isEqualTo(columnRenames[i]);
            assertThat(transformationElements[i].m_type).isEqualTo(types[i]);
            assertThat(transformationElements[i].m_originalType).isEqualTo(originalTypes[i]);
        }
    }

    /**
     * Adds a 2-element transformation element settings array to the transformation settings. One previous column and
     * one unknown column of type long which is excluded.
     *
     * @param transformationSettings to modify
     * @param unknownColumnsDataType the type of the unknown column
     */
    public static void setTransformationElementSettingsWithUnknown(
        final TransformationParameters<?> transformationSettings, final DataType unknownColumnsDataType) {

        final var unknownElement = TransformationElementSettings.createUnknownElement();
        unknownElement.m_includeInOutput = false;
        unknownElement.m_type = DataTypeSerializer.typeToString(unknownColumnsDataType);

        transformationSettings.m_columnTransformation =
            new TransformationElementSettings[]{createDummyElement("previousColumn"), unknownElement};
    }

    /**
     * @param transformationSettings to modify
     * @param columnNames the names of the columns
     * @param includeInOutputs whether the columns are to be included in the output
     * @param columnRenames the new names which columns are to be renamed to
     * @param types the types of the columns
     * @param originalTypes the original types of the columns
     */
    public static void setTransformationElementSettingsWithExisting(
        final TransformationParameters<?> transformationSettings, final String[] columnNames,
        final boolean[] includeInOutputs, final String[] columnRenames, final String[] types,
        final String[] originalTypes) {
        transformationSettings.m_columnTransformation = IntStream
            .range(0, columnNames.length).mapToObj(i -> new TransformationElementSettings(columnNames[i],
                includeInOutputs[i], columnRenames[i], types[i], originalTypes[i], null))
            .toArray(TransformationElementSettings[]::new);

    }

    /**
     * This method enables tests outside of this package to apply transformation element settings value updates received
     * from simulation.
     *
     * @param transformationSettings to modify
     * @param transformationElementSettings received from the simulation
     * @param combineWithPathToTableSpecSettings to combine the path to the table spec settings
     * @return the further simulation of transitive updates triggered by the change
     */
    public static Function<UpdateSimulator, UpdateSimulatorResult> setTransformationElementSettings(
        final TransformationParameters<?> transformationSettings, final Object transformationElementSettings,
        final UnaryOperator<String[]> combineWithPathToTableSpecSettings) {
        transformationSettings.m_columnTransformation = (TransformationElementSettings[])transformationElementSettings;
        return simulator -> simulator
            .simulateValueChange(combineWithPathToTableSpecSettings.apply(new String[]{"specs"}));
    }

    private static TransformationElementSettings createDummyElement(final String name) {
        return new TransformationElementSettings(name, true, null, null, null, null);
    }

    /**
     * Extend this test in order to test the reader-dependent state providers in the transformation settings. For this,
     * the settings need to identify extenal data types via Class<?>
     *
     * @author Paul Bärnreuther
     * @param <S> the settings type
     */
    public static abstract class TransformationParametersUpdatesTestClassBased<S extends WidgetGroup>
        extends TransformationParametersUpdatesTest<S, Class<?>> {

        @Override
        Class<?> getIntType() {
            return Integer.class;
        }

        @Override
        Class<?> getStringType() {
            return String.class;
        }

        @Override
        Class<?> getDoubleType() {
            return Double.class;
        }

        @Override
        ExternalDataTypeSerializer<Class<?>> getExternalDataTypeSerializer() {
            return new ClassSerializer() {
            };
        }

    }

    /**
     * Extend this test in order to test the reader-dependent state providers in the transformation settings. For this,
     * the settings need to identify extenal data types via DataType
     *
     * @author Paul Bärnreuther
     * @param <S> the settings type
     */
    public static abstract class TransformationParametersUpdatesTestDataTypeBased<S extends WidgetGroup>
        extends TransformationParametersUpdatesTest<S, DataType> {

        @Override
        DataType getIntType() {
            return IntCell.TYPE;
        }

        @Override
        DataType getStringType() {
            return StringCell.TYPE;
        }

        @Override
        DataType getDoubleType() {
            return DoubleCell.TYPE;
        }

        @Override
        ExternalDataTypeSerializer<DataType> getExternalDataTypeSerializer() {
            return new DataTypeSerializer() {
            };
        }
    }

}
