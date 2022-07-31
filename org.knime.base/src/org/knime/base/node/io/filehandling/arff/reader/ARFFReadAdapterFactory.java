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
 *   02.07.2022. (Dragan Keselj): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
import org.knime.core.data.convert.datacell.JavaToDataCellConverterRegistry;
import org.knime.core.data.convert.map.CellValueProducer;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.convert.map.Source;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.HierarchyAwareProductionPathProvider;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.spec.TableSpecGuesser;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;

/**
 * This class defines most of the type-related tasks the framework needs:
 * <ul>
 * <li>It defines the {@link TypeHierarchy} both for value based type-guessing and type resolution.
 * <li>It represents the link to the Type Mapping Framework
 * </ul>
 *
 * The Type Mapping Framework uses a two-step approach to convert values from a {@link Source} into {@link String}
 * objects.<br>
 * <ol>
 * <li>A {@link CellValueProducer} reads a plain Java value from a {@link Source} class (in our case a ReadAdapter).
 * <li>A {@link JavaToDataCellConverter} converts the plain Java value into a DataCell.
 * </ol>
 * In this class we only define the {@link CellValueProducer CellValueProducers} that perform the first step, while the
 * {@link JavaToDataCellConverter} are provided by the Type Mapping Framework.
 *
 * @author Dragan Keselj, Redfield SE
 *
 */
public enum ARFFReadAdapterFactory implements ReadAdapterFactory<DataType, String> {
        /**
         * The singleton instance.
         */
        INSTANCE;

    private static final Map<Class<?>, DataType> EXTERNAL_DATA_TYPES = new HashMap<>();

    private static final ProducerRegistry<DataType, ARFFReadAdapter> PRODUCER_REGISTRY = initializeProducerRegistry();

    /**
     * Used by a {@link TableSpecGuesser} to guess the type of a column based on the values it contains.
     */
    static final TreeTypeHierarchy<DataType, String> VALUE_TYPE_HIERARCHY = TreeTypeHierarchy//
        .builder(TypeTester.createTypeTester(StringCell.TYPE, createTypeTester(StringCell.TYPE, s -> true)))

        .addType(StringCell.TYPE, createTypeTester(DoubleCell.TYPE, ARFFReadAdapterFactory::canBeParsedAsDouble))
        .addType(DoubleCell.TYPE, createTypeTester(LongCell.TYPE, ARFFReadAdapterFactory::canBeParsedAsLong))
        .addType(LongCell.TYPE, createTypeTester(IntCell.TYPE, ARFFReadAdapterFactory::canBeParsedAsInt))

        .addType(StringCell.TYPE, createTypeTester(getExternalDataType(ZonedDateTime.class), //
            ARFFReadAdapterFactory::canBeParsedAsZonedDateTime))
        .addType(getExternalDataType(ZonedDateTime.class), createTypeTester(getExternalDataType(LocalDateTime.class), //
            ARFFReadAdapterFactory::canBeParsedAsLocalDateTime))
        .addType(getExternalDataType(LocalDateTime.class),
            createTypeTester(getExternalDataType(LocalDate.class), ARFFReadAdapterFactory::canBeParsedAsLocalDate))
        .build();

    private static DataType getExternalDataType(final Class<?> sourceType) {
        if (EXTERNAL_DATA_TYPES.containsKey(sourceType)) {
            return EXTERNAL_DATA_TYPES.get(sourceType);
        } else {
            final var dataType = JavaToDataCellConverterRegistry.getInstance() //
                .getFactoriesForSourceType(sourceType) //
                .stream() //
                .findFirst() //
                .orElseThrow(() -> new IllegalStateException("There is no DataType for source type: " + sourceType))
                .getDestinationType();
            EXTERNAL_DATA_TYPES.put(sourceType, dataType);
            return dataType;
        }
    }

    /**
     * Used by the Table Reader Framework to resolve type conflicts if there exists a column in multiple files that has
     * the same name but different types.<br>
     * The hierarchy currently looks as follows:
     *
     * <pre>
     * String
     *   |----------
     *   |         |
     *  Double    ZonedDateTime
     *   |         |
     *  Long      LocalDateTime
     *   |         |
     *  Integer   LocalDate
     *
     * </pre>
     */
    static final TreeTypeHierarchy<DataType, DataType> TYPE_HIERARCHY =
        VALUE_TYPE_HIERARCHY.createTypeFocusedHierarchy();

    /**
     * Used to pin the generics of {@link TypeTester#createTypeTester(Object, Predicate)}.
     */
    private static TypeTester<DataType, String> createTypeTester(final DataType type,
        final Predicate<String> predicate) {
        return TypeTester.createTypeTester(type, predicate);
    }

    private static boolean canBeParsedAsLocalDate(final String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean canBeParsedAsLocalDateTime(final String value) {
        try {
            LocalDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean canBeParsedAsZonedDateTime(final String value) {
        try {
            ZonedDateTime.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    private static boolean canBeParsedAsDouble(final String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean canBeParsedAsLong(final String value) {
        try {
            Long.parseLong(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean canBeParsedAsInt(final String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    static HierarchyAwareProductionPathProvider<DataType> createProductionPathProvider() {
        return new HierarchyAwareProductionPathProvider<>(PRODUCER_REGISTRY, TYPE_HIERARCHY, Function.identity(),
            ARFFReadAdapterFactory::isValidPath);
    }

    /**
     * Checks if the proposed path is valid for the guessed type of a column.
     */
    private static boolean isValidPath(final DataType guessedType, final ProductionPath proposedPath) {
        var proposedOutputType = proposedPath.getDestinationType();
        if (guessedType == StringCell.TYPE) {
            // if the column was numeric, then it's guessed type would be double and not String
            return !(proposedOutputType == DoubleCell.TYPE || proposedOutputType == LongCell.TYPE //NOSONAR
                || proposedOutputType == IntCell.TYPE || proposedOutputType == getExternalDataType(LocalDate.class) //NOSONAR
                || proposedOutputType == getExternalDataType(LocalDateTime.class) //NOSONAR
                || proposedOutputType == getExternalDataType(ZonedDateTime.class)); //NOSONAR
        } else {
            return true;
        }
    }

    /**
     * Registers the ARFFReadAdapter as Source in the MappingFramework and adds value producers for double, long and
     * integer values to the corresponding ProducerRegistry.
     *
     *
     * @return the producer registry containing producers for double, long and integer values
     */
    private static ProducerRegistry<DataType, ARFFReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<DataType, ARFFReadAdapter> registry =
            MappingFramework.forSourceType(ARFFReadAdapter.class);
        registry.register(new SimpleCellValueProducerFactory<>(StringCell.TYPE, String.class,
            ARFFReadAdapterFactory::readStringFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(DoubleCell.TYPE, Double.class,
            ARFFReadAdapterFactory::readDoubleFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(LongCell.TYPE, Long.class,
            ARFFReadAdapterFactory::readLongFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(IntCell.TYPE, Integer.class,
            ARFFReadAdapterFactory::readIntFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(getExternalDataType(ZonedDateTime.class),
                ZonedDateTime.class, ARFFReadAdapterFactory::readZonedDateTimeFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(getExternalDataType(LocalDateTime.class),
            LocalDateTime.class, ARFFReadAdapterFactory::readLocalDateTimeFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(getExternalDataType(LocalDate.class), LocalDate.class,
                ARFFReadAdapterFactory::readLocalDateFromSource));
        return registry;
    }

    private static String readStringFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        return source.get(params);
    }

    private static Double readDoubleFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        final String d = source.get(params);
        return d == null ? null : Double.parseDouble(d);
    }

    private static Long readLongFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        final String l = source.get(params);
        return l == null ? null : Long.parseLong(l);
    }

    private static Integer readIntFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        final String i = source.get(params);
        return i == null ? null : Integer.parseInt(i);
    }

    private static ZonedDateTime readZonedDateTimeFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        final String zonedDateTime = source.get(params);
        return zonedDateTime == null ? null : ZonedDateTime.parse(zonedDateTime);
    }

    private static LocalDateTime readLocalDateTimeFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        final String localDateTime = source.get(params);
        return localDateTime == null ? null : LocalDateTime.parse(localDateTime);
    }

    private static LocalDate readLocalDateFromSource(final ARFFReadAdapter source,
        final ReadAdapterParams<ARFFReadAdapter, ARFFReaderConfig> params) {
        final String localDate = source.get(params);
        return localDate == null ? null : LocalDate.parse(localDate);
    }

    @Override
    public ReadAdapter<DataType, String> createReadAdapter() {
        return new ARFFReadAdapter();
    }

    @Override
    public ProducerRegistry<DataType, ? extends ReadAdapter<DataType, String>> getProducerRegistry() {
        return PRODUCER_REGISTRY;
    }

    @Override
    public DataType getDefaultType(final DataType type) {
        return type;
    }

}
