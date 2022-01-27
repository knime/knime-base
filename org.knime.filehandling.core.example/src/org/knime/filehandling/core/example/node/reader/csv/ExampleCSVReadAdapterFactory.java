package org.knime.filehandling.core.example.node.reader.csv;

import java.util.function.Predicate;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TreeTypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeTester;

/**
 * Factory for ExampleCSVReadAdapter objects.
 *
 * @author modithahewasinghage
 *
 */
public enum ExampleCSVReadAdapterFactory implements ReadAdapterFactory<DataType, String> {
        /**
         * The singleton instance.
         */
        INSTANCE;

    private static final ProducerRegistry<DataType, ExampleCSVReadAdapter> PRODUCER_REGISTRY =
        initializeProducerRegistry();

    static final TreeTypeHierarchy<DataType, String> VALUE_TYPE_HIERARCHY = TreeTypeHierarchy//
        .builder(TypeTester.createTypeTester(StringCell.TYPE, createTypeTester(StringCell.TYPE, s -> true)))//
        .addType(DoubleCell.TYPE, createTypeTester(DoubleCell.TYPE, ExampleCSVReadAdapterFactory::canBeParsedAsDouble))
        .build();

    static final TypeHierarchy<DataType, DataType> TYPE_HIERARCHY = VALUE_TYPE_HIERARCHY.createTypeFocusedHierarchy();

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

    private static ProducerRegistry<DataType, ExampleCSVReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<DataType, ExampleCSVReadAdapter> registry =
            MappingFramework.forSourceType(ExampleCSVReadAdapter.class);
        registry.register(new SimpleCellValueProducerFactory<>(StringCell.TYPE, String.class,
            ExampleCSVReadAdapterFactory::readStringFromSource));
        return registry;
    }

    private static String readStringFromSource(final ExampleCSVReadAdapter source,
        final ReadAdapterParams<ExampleCSVReadAdapter, ExampleCSVReaderConfig> params) {
        return source.get(params);
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
        return StringCell.TYPE;
    }

}
