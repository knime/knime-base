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
 *   Mar 2, 2026 (Thomas Reifenberger): extracted from TransformationParameters
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import static org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.DEFAULT_COLUMNTYPE_ID;
import static org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.fromDataTypeId;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert.ProductionPathUtils;
import org.knime.filehandling.core.node.table.reader.ImmutableColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.ImmutableUnknownColumnsTransformation;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.selector.UnknownColumnsTransformation;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 * Common code for the transformation parameters used by the table readers and the table manipulator.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 * @noreference Non-public API
 * @since 5.12
 */
public final class TransformationParametersCommon {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(TransformationParametersCommon.class);

    /**
     * Common mapping and validation logic used by table readers and the table manipulator.
     *
     * @param <T> the type used to represent external data [T]ypes
     */
    public interface TransformationsMapper<T>
        extends ReaderSpecific.ProductionPathProviderAndTypeHierarchy<T>, ReaderSpecific.ExternalDataTypeSerializer<T> {

        /**
         * @return the current column transformation settings including the unknown columns placeholder element
         */
        TransformationElementSettings[] getColumnTransformation();

        /**
         * @return the current table spec settings for all input tables
         */
        TableSpecSettings[] getSpecs();

        /**
         * Determines the list of concrete column transformations and the unknown columns transformation from the
         * current settings.
         *
         * @param rawSpec the raw spec containing both union and intersection of all input table specs
         * @param columnFilterMode the column filter mode (union or intersection)
         * @return a pair of the list of column transformations and the unknown columns transformation
         */
        default Pair<ArrayList<ColumnTransformation<T>>, UnknownColumnsTransformation>
            determineTransformations(final RawSpec<T> rawSpec, final ColumnFilterMode columnFilterMode) {
            final Map<String, Integer> transformationIndexByColumnName = new HashMap<>();
            int unknownColumnsTransformationPosition = -1;
            for (int i = 0; i < getColumnTransformation().length; i++) {
                final var columnName = getColumnTransformation()[i].m_columnName;
                if (columnName == null) {
                    unknownColumnsTransformationPosition = i;
                } else {
                    transformationIndexByColumnName.put(columnName, i);
                }
            }
            final var transformations = new ArrayList<ColumnTransformation<T>>();
            for (var column : columnFilterMode.getRelevantSpec(rawSpec)) {

                // in the TypedReaderTableSpecsProvider we make sure that names are always present
                final var columnName = column.getName().orElseThrow(IllegalStateException::new);
                final var position = transformationIndexByColumnName.get(columnName);
                final var transformation = getColumnTransformation()[position];
                ProductionPath productionPath;
                try {
                    productionPath = ProductionPathUtils.fromPathIdentifier(transformation.m_productionPath,
                        getProductionPathSerializer()); // validate path id
                } catch (InvalidSettingsException e) {
                    LOGGER.error(String.format(
                        "The column '%s' can't be converted to the configured data type. Unknown production path: %s",
                        column, transformation.m_productionPath), e);
                    productionPath = getProductionPathProvider().getDefaultProductionPath(column.getType());
                }
                transformations.add(new ImmutableColumnTransformation<>(column, productionPath,
                    transformation.m_includeInOutput, position, transformation.m_columnRename));
            }
            final var unknownColumnsTransformation = determineUnknownTransformation(
                getColumnTransformation()[unknownColumnsTransformationPosition], unknownColumnsTransformationPosition);
            return new Pair<>(transformations, unknownColumnsTransformation);
        }

        @SuppressWarnings("java:S1176")
        private static ImmutableUnknownColumnsTransformation determineUnknownTransformation(
            final TransformationElementSettings unknownTransformation, final int unknownColumnsTransformationPosition) {
            DataType forcedUnknownType = null;
            if (!unknownTransformation.m_productionPath.equals(DEFAULT_COLUMNTYPE_ID)) {
                forcedUnknownType = fromDataTypeId(unknownTransformation.m_productionPath);
            }
            return new ImmutableUnknownColumnsTransformation(unknownColumnsTransformationPosition,
                unknownTransformation.m_includeInOutput, forcedUnknownType != null, forcedUnknownType);
        }

        /**
         * Converts a known {@link ColumnTransformation} to a position-indexed {@link TransformationElementSettings}
         * pair.
         *
         * @param knownTransformation the known column transformation to convert
         * @return a pair of position and corresponding {@link TransformationElementSettings}
         */
        default Pair<Integer, TransformationElementSettings>
            getTransformationElement(final ColumnTransformation<T> knownTransformation) {
            return new Pair<>(knownTransformation.getPosition(), toTransformationElementSettings(knownTransformation));
        }

        /**
         * Converts an {@link UnknownColumnsTransformation} to a position-indexed {@link TransformationElementSettings}
         * pair.
         *
         * @param unknownColumnsTransformation the unknown columns transformation to convert
         * @return a pair of position and corresponding {@link TransformationElementSettings}
         */
        default Pair<Integer, TransformationElementSettings>
            getTransformationElement(final UnknownColumnsTransformation unknownColumnsTransformation) {
            return new Pair<>(unknownColumnsTransformation.getPosition(),
                toTransformationElementSettings(unknownColumnsTransformation));
        }

        @SuppressWarnings("java:S1176")
        private TransformationElementSettings toTransformationElementSettings(final ColumnTransformation<T> t) {
            final var defaultProductionPath =
                getProductionPathProvider().getDefaultProductionPath(t.getExternalSpec().getType());
            return new TransformationElementSettings( //
                t.getOriginalName(), //
                t.keep(), //
                t.getName(), //
                t.getProductionPath(), //
                defaultProductionPath, //
                getProductionPathSerializer());
        }

        @SuppressWarnings("java:S1176")
        private static TransformationElementSettings
            toTransformationElementSettings(final UnknownColumnsTransformation t) {
            return new TransformationElementSettings(t.keep(), getForcedType(t));
        }

        @SuppressWarnings("java:S1176")
        private static DataType getForcedType(final UnknownColumnsTransformation t) {
            if (t.forceType()) {
                return t.getForcedType();
            }
            return null;
        }

        /**
         * Validates the stored table spec settings by checking that all column types can be parsed as external types.
         *
         * @throws InvalidSettingsException if a column type in the specs cannot be parsed
         */
        default void validateSpecs() throws InvalidSettingsException {
            for (TableSpecSettings tSpec : getSpecs()) {
                for (ColumnSpecSettings cSpec : tSpec.m_spec) {
                    try {
                        toExternalType(cSpec.m_type);
                    } catch (ReaderSpecific.ExternalDataTypeParseException e) {
                        throw new InvalidSettingsException(
                            String.format("The type '%s' for column '%s' is invalid.", cSpec.m_type, cSpec.m_name), e);
                    }
                }
            }
        }

        /**
         * Validates the column transformation settings by checking that all non-trivial column renames are valid
         * column names.
         *
         * @throws InvalidSettingsException if a column rename is empty, blank, or not trimmed
         */
        default void validateColumnTransformations() throws InvalidSettingsException {
            for (TransformationElementSettings elem : getColumnTransformation()) {
                if (elem.m_columnRename != null && !elem.m_columnRename.equals(elem.m_columnName)) {
                    ColumnNameValidationUtils.validateColumnName(elem.m_columnRename, state -> { // NOSONAR complexity is OK
                        switch (state) {
                            case EMPTY:
                                return String.format("The new name for column '%s' is empty.", elem.m_columnName);
                            case BLANK:
                                return String.format("The new name for column '%s' is blank.", elem.m_columnName);
                            case NOT_TRIMMED:
                                return String.format(
                                    "The new name for column '%s' starts or ends with whitespace characters.",
                                    elem.m_columnName);
                            default:
                                throw new IllegalStateException("Unknown InvalidColumnNameState: " + state);
                        }
                    });
                }
            }
        }
    }

    /**
     * Comparator for ordering {@link TransformationElementSettings} pairs by position, placing the unknown columns
     * element before any known column element at the same position.
     *
     * @param unknownElement the pair representing the &lt;any unknown column&gt; placeholder element
     */
    @SuppressWarnings("java:S1176")
    public record ColumnTransformationComparator(Pair<Integer, TransformationElementSettings> unknownElement)
        implements Comparator<Pair<Integer, TransformationElementSettings>> {

        @Override
        public int compare(final Pair<Integer, TransformationElementSettings> a,
            final Pair<Integer, TransformationElementSettings> b) {
            var aPos = a.getFirst();
            var bPos = b.getFirst();
            if (!Objects.equals(aPos, bPos)) {
                return Integer.compare(aPos, bPos);
            }
            /*
             * Workaround for legacy behaviour: The "unknown columns" placeholder might have the same index as a known
             * column. It should be inserted at its position, shifting all subsequent columns (including the one with
             * the same index) to the right. This comparator implicitly assumes that there is only one
             * "unknown columns" element.
             */
            if (a == unknownElement) {
                return -1;
            }
            if (b == unknownElement) {
                return 1;
            }
            return 0;
        }
    }

    private TransformationParametersCommon() {
        // prevent instantiation
    }
}
