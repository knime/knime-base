package org.knime.filehandling.core.example.node.reader.csv;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.MappingFramework;
import org.knime.core.data.convert.map.ProducerRegistry;
import org.knime.core.data.convert.map.SimpleCellValueProducerFactory;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ReadAdapter;
import org.knime.filehandling.core.node.table.reader.ReadAdapter.ReadAdapterParams;
import org.knime.filehandling.core.node.table.reader.ReadAdapterFactory;

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

    private static final ProducerRegistry<DataType, ExampleCSVReadAdapter> PRODUCER_REGISTRY = initializeProducerRegistry();

    private static ProducerRegistry<DataType, ExampleCSVReadAdapter> initializeProducerRegistry() {
        final ProducerRegistry<DataType, ExampleCSVReadAdapter> registry = MappingFramework
                .forSourceType(ExampleCSVReadAdapter.class);
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
