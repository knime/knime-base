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
 *   Feb 26, 2026 (Thomas Reifenberger): extracted from TransformationParametersStateProviders
 */
package org.knime.base.node.io.filehandling.webui.reader2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.knime.core.data.DataType;
import org.knime.core.data.convert.map.ProductionPath;
import org.knime.core.webui.node.dialog.defaultdialog.setting.datatype.convert.ProductionPathUtils;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.internal.StateProviderInitializerInternal;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * This utility class holds all common logic & classes which is used by both the table readers and the table
 * manipulator, primarily in the state providers for the transformation parameters.
 *
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH
 */
public final class TransformationParametersStateProvidersCommon {

    static final String DEFAULT_COLUMNTYPE_ID = "<default-columntype>";

    static final String DEFAULT_COLUMNTYPE_TEXT = "Default columntype";

    public interface HasTableSpec<T> {
        TypedReaderTableSpec<T> getTableSpec();
    }

    // --- common refs ---
    public static final class SpecsRef implements Modification.Reference {
    }

    public static final class ColumnNameRef implements ParameterReference<String> {
    }

    static final class TypeChoicesWidgetRef implements Modification.Reference {
    }

    public static final class TransformationElementSettingsArrayWidgetRef implements Modification.Reference {
    }

    public static final class TableSpecSettingsRef implements ParameterReference<TableSpecSettings[]> {
    }

    public static final class TransformationElementSettingsRef
        implements ParameterReference<TransformationElementSettings[]> {
    }

    public static final class InitialTransformationElementSettingsRef
        implements ParameterReference<TransformationElementSettings.Data[]> {
    }
    // -- end common refs ---

    /**
     * Common mapping logic used by table readers and the table manipulator.
     * 
     * @param <T> the type used to represent external data [T]ypes
     */
    public interface TransformationElementsMapper<T> extends ReaderSpecific.ProductionPathProviderAndTypeHierarchy<T> {

        default TransformationElementSettings[] toTransformationElements(
            final Collection<? extends HasTableSpec<T>> specs,
            final MultiFileReaderParameters.HowToCombineColumnsOption howToCombineColumnsOption,
            final TransformationElementSettings[] existingSettings, final TypedReaderTableSpec<T> existingSpecsUnion) {

            // List of existing elements before the unknown element
            final List<TransformationElementSettings> elementsBeforeUnknown = new ArrayList<>();

            final var existingColumnTypesByName = existingSpecsUnion.stream().filter(s -> s.getName().isPresent())
                .collect(Collectors.toMap(s -> s.getName().get(), TypedReaderColumnSpec::getType));
            final Collection<String> existingElementNamesWithChangedType = new HashSet<>();
            // The existing unknown element.
            TransformationElementSettings foundUnknownElement = null;
            // List of existing elements after the unknown element
            final List<TransformationElementSettings> elementsAfterUnknown = new ArrayList<>();

            final var rawSpec = toRawSpec(specs.stream().map(HasTableSpec::getTableSpec).toList());
            final var newSpecs = howToCombineColumnsOption.toColumnFilterMode().getRelevantSpec(rawSpec);
            final var newSpecsByName = newSpecs.stream().filter(column -> column.getName().isPresent())
                .collect(Collectors.toMap(column -> column.getName().get(), Function.identity()));

            for (int i = 0; i < existingSettings.length; i++) { // NOSONAR
                final var existingElement = existingSettings[i];
                if (existingElement.m_columnName == null) {
                    foundUnknownElement = existingSettings[i];
                    continue;
                }
                final var newSpec = newSpecsByName.get(existingElement.m_columnName);
                if (newSpec == null) {
                    // element does not exist anymore -> it is removed
                    continue;
                }
                final var existingType = existingColumnTypesByName.get(newSpec.getName().orElse(null));
                if (existingType != null && !existingType.equals(newSpec.getType())) {
                    // element has different type than before
                    existingElementNamesWithChangedType.add(existingElement.m_columnName);
                    continue;
                }
                final var targetList = foundUnknownElement == null ? elementsBeforeUnknown : elementsAfterUnknown;
                targetList.add(mergeExistingWithNew(existingElement, newSpec));
            }

            // foundUnknownElement can only be null when the dialog is opened for the first time
            final var unknownElement = foundUnknownElement == null
                ? TransformationElementSettings.createUnknownElement() : foundUnknownElement;

            final var existingColumnNames = Stream.concat(elementsBeforeUnknown.stream(), elementsAfterUnknown.stream())
                .map(element -> element.m_columnName).collect(Collectors.toSet());

            final var unknownElementsType = getUnknownElementsType(unknownElement);
            final Predicate<String> isNewColumn = colName -> existingElementNamesWithChangedType.contains(colName)
                || !existingColumnNames.contains(colName);

            final var newElements =
                newSpecs.stream().filter(colSpec -> isNewColumn.test(colSpec.getName().orElse(null)))
                    .map(colSpec -> createNewElement(colSpec, unknownElementsType.orElse(null),
                        unknownElement.m_includeInOutput))
                    .toList();

            return Stream.concat( //
                Stream.concat(elementsBeforeUnknown.stream(), newElements.stream()),
                Stream.concat(Stream.of(unknownElement), elementsAfterUnknown.stream()))
                .toArray(TransformationElementSettings[]::new);

        }

        private TransformationElementSettings mergeExistingWithNew(final TransformationElementSettings existingElement,
            final TypedReaderColumnSpec<T> newSpec) {
            final var newElement = createNewElement(newSpec);
            if (!newElement.m_originalProductionPath.equals(existingElement.m_originalProductionPath)) {
                return newElement;
            }
            newElement.m_productionPath = existingElement.m_productionPath;
            newElement.m_columnRename = existingElement.m_columnRename;
            newElement.m_includeInOutput = existingElement.m_includeInOutput;
            return newElement;

        }

        private static Optional<DataType> getUnknownElementsType(final TransformationElementSettings unknownElement) {
            if (DEFAULT_COLUMNTYPE_ID.equals(unknownElement.m_productionPath)) {
                return Optional.empty();
            }
            return Optional.of(fromDataTypeId(unknownElement.m_productionPath));
        }

        /**
         * @return a new element as if it would be constructed as unknown new when the <any unknown column> element is
         *         configured like the default.
         */
        private TransformationElementSettings createNewElement(final TypedReaderColumnSpec<T> colSpec) {
            return createNewElement(colSpec, null, true);
        }

        private TransformationElementSettings createNewElement(final TypedReaderColumnSpec<T> colSpec,
            final DataType unknownElementsType, final boolean includeInOutput) {
            final var name = colSpec.getName().get(); // NOSONAR in the TypedReaderTableSpecProvider we make sure that names are always present
            final var defPath = getProductionPathProvider().getDefaultProductionPath(colSpec.getType());

            final var path = Optional.ofNullable(unknownElementsType)
                .flatMap(type -> findProductionPath(colSpec.getType(), type)).orElse(defPath);
            return new TransformationElementSettings(name, includeInOutput, name, path, defPath,
                getProductionPathSerializer());

        }

        private Optional<ProductionPath> findProductionPath(final T from, final DataType to) {
            return getProductionPathProvider().getAvailableProductionPaths(from).stream()
                .filter(path -> to.equals(path.getDestinationType())).findFirst();
        }

        default TypedReaderTableSpec<T> getExistingSpecsUnion(
            final ReaderSpecific.ExternalDataTypeSerializer<T> serializer, final TableSpecSettings[] specs) {
            final var existingSpecs = toSpecMap(serializer, specs);
            final var existingRawSpecs = toRawSpec(existingSpecs.values());
            return existingRawSpecs.getUnion();
        }
    }

    /**
     * Common mapping logic used by table readers and the table manipulator.
     * 
     * @param <T> the type used to represent external data [T]ypes
     */
    public interface TypeChoicesMapper<T> extends ReaderSpecific.ProductionPathProviderAndTypeHierarchy<T> {

        default List<StringChoice> computeTypeChoices(final Collection<TypedReaderTableSpec<T>> tableSpecs,
            final String columnName) {

            if (columnName == null) { // i.e., any unknown column
                final var defaultChoice = new StringChoice(DEFAULT_COLUMNTYPE_ID, DEFAULT_COLUMNTYPE_TEXT);
                final var dataTypeChoices = getProductionPathProvider().getAvailableDataTypes().stream()
                    .sorted((t1, t2) -> t1.toPrettyString().compareTo(t2.toPrettyString()))
                    .map(type -> new StringChoice(getDataTypeId(type), type.toPrettyString())).toList();
                return Stream.concat(Stream.of(defaultChoice), dataTypeChoices.stream()).toList();
            }

            final var union = toRawSpec(tableSpecs).getUnion();
            final var columnSpecOpt =
                union.stream().filter(colSpec -> colSpec.getName().get().equals(columnName)).findAny();
            if (columnSpecOpt.isEmpty()) {
                return List.of();
            }
            final var columnSpec = columnSpecOpt.get();
            final var productionPaths = getProductionPathProvider().getAvailableProductionPaths(columnSpec.getType());
            return productionPaths.stream()
                .map(p -> new StringChoice(ProductionPathUtils.getPathIdentifier(p, getProductionPathSerializer()),
                    p.getDestinationType().toPrettyString()))
                .toList();
        }
    }

    /**
     * Common base class used by both the table readers and the table manipulator.
     * 
     * Put a reader or table manipulator specific implementation of this class in a {@link Modification} annotation on
     * the reader / table manipulator specific implementation of {@link TransformationParameters}. This ensures, that
     * the common fields within these settings are updated as expected.
     *
     * For filling the abstract methods within this class, some abstract state providers need to be implemented. But
     * none of the to be overwritten methods of these state providers should be implemented directly, since they stem
     * from interfaces which are used for multiple state providers. Instead, extend these interfaces (in
     * {@link ReaderSpecific}) by ones that overwrite all methods with default implementations and use those in the
     * respective places.
     *
     * @param <S> the type used to represent the internal table spec settings
     */
    public abstract static class AbstractTransformationSettingsWidgetModification<S>
        implements Modification.Modifier {

        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(SpecsRef.class).addAnnotation(ValueProvider.class).withValue(getSpecsValueProvider()).modify();
            group.find(TypeChoicesWidgetRef.class).addAnnotation(ChoicesProvider.class)
                .withValue(getTypeChoicesProvider()).modify();
            group.find(TransformationElementSettingsArrayWidgetRef.class).addAnnotation(ValueProvider.class)
                .withValue(getTransformationSettingsValueProvider()).modify();
        }

        /**
         * @return the value provider for the specs.
         */
        protected abstract Class<? extends StateProvider<S[]>> getSpecsValueProvider();

        /**
         * @return the value provider for the transformation settings array layout.
         */
        protected abstract Class<? extends StateProvider<TransformationElementSettings[]>>
            getTransformationSettingsValueProvider();

        /**
         * @return the choices provider for the types of array layout elements.
         */
        protected abstract Class<? extends StringChoicesProvider> getTypeChoicesProvider();
    }

    /**
     * State provider for the initial transformation element settings. Needed for the dirty tracking.
     *
     * @noreference non-public API
     * @noimplement non-public API
     */
    public static class InitialTransformationElementSettingsStateProvider
        implements StateProvider<TransformationElementSettings.Data[]> {

        Supplier<TransformationElementSettings[]> m_initialTransformationElementSettingsSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeBeforeOpenDialog();
            ((StateProviderInitializerInternal)initializer).computeAfterApplyDialog();
            m_initialTransformationElementSettingsSupplier =
                initializer.getValueSupplier(TransformationElementSettingsRef.class);
        }

        @Override
        public TransformationElementSettings.Data[] computeState(final NodeParametersInput parametersInput)
            throws StateComputationFailureException {
            return Arrays.stream(m_initialTransformationElementSettingsSupplier.get())
                .map(TransformationElementSettings.Data::new) //
                .toList() //
                .toArray(TransformationElementSettings.Data[]::new);
        }
    }

    /**
     * Dirty tracker to make the dialog dirty in cases not covered by the framework, e.g. file schema has changed or
     * column reordering.
     *
     * @noreference non-public API
     * @noimplement non-public API
     */
    public static class TransformationElementsDirtyTrackerStateProvider implements StateProvider<Boolean> {

        Supplier<TransformationElementSettings.Data[]> m_initialSpecSupplier;

        Supplier<TransformationElementSettings[]> m_currentSpecSupplier;

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_initialSpecSupplier = initializer.getValueSupplier(InitialTransformationElementSettingsRef.class);
            m_currentSpecSupplier = initializer.computeFromValueSupplier(TransformationElementSettingsRef.class);
        }

        @Override
        public Boolean computeState(final NodeParametersInput parametersInput) throws StateComputationFailureException {
            var initialSpecs = m_initialSpecSupplier.get();
            var currentSpecs = m_currentSpecSupplier.get();

            if (initialSpecs == null || currentSpecs == null) {
                return false;
            }

            return !TransformationElementSettings.Data.areSettingsMatching(initialSpecs, currentSpecs);
        }
    }

    static DataType fromDataTypeId(final String id) {
        return DataTypeSerializer.stringToType(id);
    }

    static String getDataTypeId(final DataType type) {
        return DataTypeSerializer.typeToString(type);
    }

    private TransformationParametersStateProvidersCommon() {
        // utility class
    }
}
