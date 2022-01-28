package org.knime.filehandling.core.example.node.reader.csv;

import java.util.function.Function;
import java.util.function.Predicate;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.convert.datacell.JavaToDataCellConverter;
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
 * The Type Mapping Framework uses a two-step approach to convert values from a {@link Source} into KNIME
 * {@link DataCell} objects.<br>
 * <ol>
 * <li>A {@link CellValueProducer} reads a plain Java value from a {@link Source} class (in our case a ReadAdapter).
 * <li>A {@link JavaToDataCellConverter} converts the plain Java value into a DataCell.
 * </ol>
 * In this class we only define the {@link CellValueProducer CellValueProducers} that perform the first step, while the
 * {@link JavaToDataCellConverter} are provided by the Type Mapping Framework.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 *
 */
public enum ExampleCSVReadAdapterFactory implements ReadAdapterFactory<DataType, String> {
        /**
         * The singleton instance.
         */
        INSTANCE;

    private static final ProducerRegistry<DataType, ExampleCSVReadAdapter> PRODUCER_REGISTRY =
        initializeProducerRegistry();

    /**
     * Used by a {@link TableSpecGuesser} to guess the type of a column based on the values it contains.
     */
    static final TreeTypeHierarchy<DataType, String> VALUE_TYPE_HIERARCHY = TreeTypeHierarchy//
        .builder(TypeTester.createTypeTester(StringCell.TYPE, createTypeTester(StringCell.TYPE, s -> true)))//
        .addType(StringCell.TYPE, createTypeTester(DoubleCell.TYPE, ExampleCSVReadAdapterFactory::canBeParsedAsDouble))
        .build();

    /**
     * Used by the Table Reader Framework to resolve type conflicts if there exists a column in multiple files that has
     * the same name but different types.<br>
     * The hierarchy currently looks as follows:
     *
     * <pre>
     * String | Double
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

    private static boolean canBeParsedAsDouble(final String value) {
        try {
            Double.parseDouble(value);
            return true;
        } catch (NumberFormatException ex) {
            // not a double
            return false;
        }
    }

    static HierarchyAwareProductionPathProvider<DataType> createProductionPathProvider() {
        return new HierarchyAwareProductionPathProvider<>(PRODUCER_REGISTRY, TYPE_HIERARCHY, Function.identity(),
            ExampleCSVReadAdapterFactory::isValidPath);
    }

    /**
     * Checks if the proposed path is valid for the guessed type of a column.
     */
    private static boolean isValidPath(final DataType guessedType, final ProductionPath proposedPath) {
        var proposedOutputType = proposedPath.getDestinationType();
        if (guessedType == StringCell.TYPE) {
            // if the column was numeric, then it's guessed type would be double and not String
            return !(proposedOutputType == DoubleCell.TYPE || proposedOutputType == LongCell.TYPE
                || proposedOutputType == IntCell.TYPE);
        } else {
            return true;
        }
    }

    /**
     * Registers the ExampleCSVReadAdapter as Source in the MappingFramework and adds value producers for String and
     * double values to the corresponding ProducerRegistry.
     *
     *
     * @return the producer registry containing producers for String and double values
     */
    private static ProducerRegistry<DataType, ExampleCSVReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<DataType, ExampleCSVReadAdapter> registry =
            MappingFramework.forSourceType(ExampleCSVReadAdapter.class);
        registry.register(new SimpleCellValueProducerFactory<>(StringCell.TYPE, String.class,
            ExampleCSVReadAdapterFactory::readStringFromSource));
        registry.register(new SimpleCellValueProducerFactory<>(DoubleCell.TYPE, Double.class,
            ExampleCSVReadAdapterFactory::readDoubleFromSource));
        return registry;
    }

    private static String readStringFromSource(final ExampleCSVReadAdapter source,
        final ReadAdapterParams<ExampleCSVReadAdapter, ExampleCSVReaderConfig> params) {
        return source.get(params);
    }

    private static double readDoubleFromSource(final ExampleCSVReadAdapter source,
        final ReadAdapterParams<ExampleCSVReadAdapter, ExampleCSVReaderConfig> params) {
        return Double.parseDouble(readStringFromSource(source, params));
    }

    @Override
    public ReadAdapter<DataType, String> createReadAdapter() {
        return new ExampleCSVReadAdapter();
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
