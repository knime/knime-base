package org.knime.filehandling.core.example.node.reader.csv;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;

/**
 * Factory for ExampleCSVReadAdapter objects.
 * 
 * @author modithahewasinghage
 *
 */
public enum ExampleCSVReadAdapterFactory implements ReadAdapterFactory<Class<?>, String> {
    /**
     * The singleton instance.
     */
    INSTANCE;

    private static final ProducerRegistry<Class<?>, ExampleCSVReadAdapter> PRODUCER_REGISTRY = initializeProducerRegistry();

    private static ProducerRegistry<Class<?>, ExampleCSVReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<Class<?>, ExampleCSVReadAdapter> registry = MappingFramework
                .forSourceType(ExampleCSVReadAdapter.class);
        registry.register(new SimpleCellValueProducerFactory<>(String.class, String.class,
                ExampleCSVReadAdapterFactory::readStringFromSource));
        return registry;
    }

    private static String readStringFromSource(final ExampleCSVReadAdapter source,
            final ReadAdapterParams<ExampleCSVReadAdapter, ExampleCSVReaderConfig> params) {
        return source.get(params);
    }

    @Override
    public ReadAdapter<Class<?>, String> createReadAdapter() {
        return new ExampleCSVReadAdapter();
    }

    @Override
    public ProducerRegistry<Class<?>, ? extends ReadAdapter<Class<?>, String>> getProducerRegistry() {
        return PRODUCER_REGISTRY;
    }

    @Override
    public DataType getDefaultType(Class<?> type) {
        return StringCell.TYPE;
    }

}
