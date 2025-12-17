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
 *   16 Dec 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.data.aggregation.parameters;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Stream;

import org.knime.base.data.aggregation.dialogutil.type.DataTypeNameSorter;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.collection.ListCell;
import org.knime.core.data.collection.SetCell;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.database.aggregation.AggregationFunction;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.CustomValidationProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.custom.ValidationCallback;
import org.knime.filehandling.core.util.WildcardToRegexUtil;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.widget.choices.DataTypeChoicesProvider;
import org.knime.node.parameters.widget.choices.StringChoice;
import org.knime.node.parameters.widget.choices.StringChoicesProvider;

/**
 * Commonly used state and choices providers.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 *
 * @since 5.11
 */
public final class StateAndChoicesProviders {

    private StateAndChoicesProviders() {
        // utility
    }

    /**
     * Lists all registered data types, with the types present in the input table spec first.
     *
     * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
     */
    public abstract static class RegisteredTypesChoicesProvider implements DataTypeChoicesProvider {

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
        }

        /**
         * Retrieves the data types considered as "input" types from the node parameters input.
         *
         * @param context the node parameters input
         * @return input data types
         */
        protected abstract Stream<DataType> getInputTypes(NodeParametersInput context);

        @Override
        public List<DataType> choices(final NodeParametersInput context) {
            final var registered = new HashSet<>(DataTypeRegistry.getInstance().availableDataTypes());
            final var generalType = DataType.getType(DataCell.class);
            registered.add(generalType); // plain DataCell for backwards-compatibility

            // see DBDataTypeAggregationFunctionPanel#getTypeList
            registered.add(ListCell.getCollectionType(generalType)); // for backwards-compatibility
            registered.add(SetCell.getCollectionType(generalType)); // for backwards-compatibility

            final var fromInput = getInputTypes(context) //
                .distinct() //
                .map(t -> {
                    registered.remove(t);
                    return t;
                }).toList();
            return Stream.concat(
                // first distinct types from input table (this also includes Collection types of present columns)
                fromInput.stream(),
                // fill up with all remaining registered types
                registered.stream().sorted(DataTypeNameSorter.getInstance())) //
                .toList();
        }
    }

    /**
     * Choices provider for aggregation functions compatible with a given data type, that is based on a datatype
     * reference.
     *
     * @param <F> the aggregation function type
     * @param <P> the aggregation function utility type
     */
    public abstract static class AggregationChoicesByTypeRef<F extends AggregationFunction,
            P extends AggregationFunctionsUtility<F>> implements StringChoicesProvider {

        private Supplier<DataType> m_type;

        /**
         * Gets the aggregation function utility (to obtain compatible functions from)
         * from the given port object spec. In case the port object spec is required to determine the utility,
         * e.g. from an active DB session, but the passed spec is {@code null}, an empty optional may be returned.
         *
         * @param spec the {@code null}able input port spec
         * @return the aggregation function utility, if available
         */
        protected abstract Optional<P> getUtility(PortObjectSpec spec);

        /**
         * Gets the type provider reference class on which the compatible functions are based.
         *
         * @return datatype reference to base choices on
         */
        protected abstract Class<? extends ParameterReference<DataType>> getTypeProvider();

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_type = initializer.computeFromValueSupplier(getTypeProvider());
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput parametersInput) {
            final var inSpec = parametersInput.getInPortSpec(0).orElse(null);
            // can always get the utility, even if the input is not attached
            return getUtility(inSpec) //
                    .map(util -> util.getCompatibleAggregationFunctions(m_type.get(), /*sorted*/ true)) //
                    .orElseGet(Stream::empty) //
                    .map(agg -> new StringChoice(agg.getId(), agg.getLabel())).toList(); //
        }

    }

    /**
     * Obtains the list of aggregation functions compatible with the selected column's data type, that is provided by a
     * state provider.
     */
    public abstract static class AggregationChoicesByTypeProvider implements StringChoicesProvider {

        private Supplier<Optional<DataType>> m_type;

        /**
         * Get all aggregation functions compatible with the given data type.
         *
         * @param type the data type
         * @return stream of compatible aggregation functions
         */
        protected abstract Stream<AggregationSpec> getCompatibleFunctions(DataType type);

        /**
         * Get the provider class that provides the data type of the selected column on which the compatible functions
         * are based.
         *
         * @return datatype provider class
         */
        protected abstract Class<? extends StateProvider<Optional<DataType>>> getTypeProvider();

        @Override
        public void init(final StateProviderInitializer initializer) {
            initializer.computeAfterOpenDialog();
            m_type = initializer.computeFromProvidedState(getTypeProvider());
        }

        @Override
        public List<StringChoice> computeState(final NodeParametersInput parametersInput) {
            final var type = m_type.get();
            if (type.isEmpty()) {
                return List.of();
            }
            return getCompatibleFunctions(type.get()) //
                .map(agg -> new StringChoice(agg.id(), agg.label())) //
                .toList();
        }

    }

    /**
     * Validation for wildcard or regex patterns based on a pattern type reference (e.g. configured through a value
     * switch).
     */
    public abstract static class WildcardOrRegexPatternValidation implements CustomValidationProvider<String> {

        private Supplier<PatternType> m_patternType;

        /**
         * Gets the class used for the reference to the selected pattern type.
         * @return reference class
         */
        protected abstract Class<? extends ParameterReference<PatternType>> getPatternTypeRefClass();

        @Override
        public void init(final StateProviderInitializer initializer) {
            m_patternType = initializer.getValueSupplier(getPatternTypeRefClass());
        }

        @Override
        public ValidationCallback<String> computeValidationCallback(final NodeParametersInput parametersInput) {
            final var patternType = m_patternType.get();
            return switch (patternType) {
                case REGEX -> WildcardOrRegexPatternValidation::validateRegexPattern;
                case WILDCARD -> WildcardOrRegexPatternValidation::validateWildcardPattern;
            };
        }

        private static void validateWildcardPattern(final String wildcard) throws InvalidSettingsException {
            try {
                Pattern.compile(WildcardToRegexUtil.wildcardToRegex(wildcard));
            } catch (final PatternSyntaxException e) {
                throw new InvalidSettingsException("The wildcard pattern is invalid: " + e.getMessage(), e);
            }
        }

        private static void validateRegexPattern(final String regex) throws InvalidSettingsException {
            try {
                Pattern.compile(regex);
            } catch (final PatternSyntaxException e) {
                throw new InvalidSettingsException("The regular expression pattern is invalid: " + e.getMessage(), e);
            }
        }
    }

}
