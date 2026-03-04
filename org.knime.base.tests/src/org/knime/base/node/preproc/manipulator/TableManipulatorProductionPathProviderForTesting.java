package org.knime.base.node.preproc.manipulator;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;
import org.knime.filehandling.core.node.table.reader.ProductionPathProvider;

class TableManipulatorProductionPathProviderForTesting implements ProductionPathProvider<DataType> {

    private final ProductionPathProvider<DataType> ORIGINAL_PRODUCTION_PATH_PROVIDER =
        TableManipulatorNodeModel.createProductionPathProvider();

    private final Set<DataType> allowedDestinationDataTypes =
        Set.of(BooleanCell.TYPE, DoubleCell.TYPE, IntCell.TYPE, StringCell.TYPE);

    @Override
    public ProductionPath getDefaultProductionPath(final DataType externalType) {
        return ORIGINAL_PRODUCTION_PATH_PROVIDER.getDefaultProductionPath(externalType);
    }

    @Override
    public List<ProductionPath> getAvailableProductionPaths(final DataType externalType) {
        return ORIGINAL_PRODUCTION_PATH_PROVIDER.getAvailableProductionPaths(externalType)
            .stream() //
            .filter(p -> allowedDestinationDataTypes.contains(p.getDestinationType())) //
            .toList();
    }

    @Override
    public Set<DataType> getAvailableDataTypes() {
        return ORIGINAL_PRODUCTION_PATH_PROVIDER.getAvailableDataTypes().stream() //
            .filter(allowedDestinationDataTypes::contains) //
            .collect(Collectors.toSet());
    }
}
