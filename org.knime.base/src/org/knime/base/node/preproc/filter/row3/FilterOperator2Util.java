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
 *   Sep 8, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.preproc.filter.row3;


/**
 * This utility is used both during configuration of the dialog and execution of the node.
 *
 * Within the dialog, we need to determine which operators are applicable for a selected filter column and which filter
 * parameters are to be shown depending on the selected operator.
 *
 * During execution, we need to create the actual predicate from the selected operator and its parameters. There we have
 * to deal with situations where the data type of the used column has been changed in the meantime or with combinations
 * of operator id and parameters that is not creatable in the dialog anymore but has been at some point.
 *
 * Operators can be provided via extension point (see {@link FilterOperatorsExtensionsUtil}). The extension point allows
 * defining completely custom operators, operators that replace existing ones (see, e.g. {@link EqualsOperator}) or to
 * use standard operators without custom implementation that are to be handled by the framework (see e.g.
 * {@link SingleCellOperatorFamily}).
 *
 *
 * @author Paul Bärnreuther
 */
public final class FilterOperator2Util {
//
//    private FilterOperator2Util() {
//        // utility class
//    }
//
//    //
//    // builtin -> EQ/NEQ, LT, GT
//    // default: our impls for builtins
//    // derived (from builtin) -> NEQ_NORMISS, LTE, GTE
//    //
//
//    record AvailableOperators(//
//        List<BuiltinOperator<?>> builtinOperatorsFromExtension, //
//        List<Pair<FilterOperator, FilterOperator.InternalFilterOperator<?>>> internalBuiltinOperators, //
//        List<ValueFilterOperator<?>> customOperatorsFromExtension //
//    ) {
//
//    }
//
//    static final Map<DataType, List<ValueFilterOperator<FilterValueParameters>>> EXTENSION_FILTER_OPERATORS =
//        FilterOperatorsExtensionsUtil.getFilterOperatorExtensions();
//
//    static AvailableOperators getOperatorsForColumn(final DataType dataType) {
//        final List<ValueFilterOperator<? extends FilterValueParameters>> operatorsFromExtension =
//            EXTENSION_FILTER_OPERATORS.getOrDefault(dataType, List.of());
//
//        final List<BuiltinOperator<?>> builtinOperators = new ArrayList<>();
//        final List<ValueFilterOperator<?>> customOperators = new ArrayList<>();
//        for (final var op : operatorsFromExtension) {
//            if (op instanceof BuiltinOperator<?> builtinOp) {
//                builtinOperators.add(builtinOp);
//            } else {
//                customOperators.add(op);
//            }
//        }
//        final List<Pair<FilterOperator, FilterOperator.InternalFilterOperator<?>>> internalBuiltinOperators =
//            new ArrayList<>();
//        for (var fo : FilterOperator.values()) {
//            fo.getColumnFilterOperator(dataType).ifPresent(ifo ->
//                internalBuiltinOperators.add(new Pair<>(fo, ifo))
//            );
//        }
//
//        return new AvailableOperators(//
//            builtinOperators, //
//            internalBuiltinOperators, //
//            customOperators//
//        );
//
//    }
//
//    static final List<Pair<FilterOperator, StringChoice>> BUILTIN_CHOICES_ORDERED = Arrays
//        .stream(FilterOperator.values()).map(c -> new Pair<>(c, new StringChoice(c.name(), getLabelTitle(c)))).toList();
//
//    public static String getLabelTitle(final Enum<?> constant) {
//        var enumClass = constant.getDeclaringClass();
//        var name = constant.name();
//        try {
//            final var field = enumClass.getField(name);
//            if (field.isAnnotationPresent(Label.class)) {
//                final var label = field.getAnnotation(Label.class);
//                return label.value();
//            }
//        } catch (NoSuchFieldException | SecurityException e) {
//            throw new IllegalStateException(String.format("Exception when accessing field %s.", name), e);
//        }
//        return StringUtils.capitalize(name.toLowerCase(Locale.getDefault()).replace("_", " "));
//    }
//
//    static final Map<String, Class<? extends BuiltinOperator>> BUILTIN_OPERATOR_ID_MAP = Map.ofEntries(//
//        Map.entry("EQ", EqualsOperator.class), //
//        Map.entry("NEQ", EqualsOperator.class), //
//        Map.entry("NEQ_MISS", EqualsOperator.class), //
//        Map.entry("LT", LessThanOperator.class), //
//        Map.entry("GT", GreaterThanOperator.class), //
//        Map.entry("LTE", GreaterThanOperator.class), //
//        Map.entry("GTE", LessThanOperator.class)//
//    );
//
//    static Stream<String> idsForBuiltinOperator(final BuiltinOperator builtinOp) {
//        return BUILTIN_OPERATOR_ID_MAP.entrySet().stream().filter(e -> e.getValue().isInstance(builtinOp))
//            .map(Map.Entry::getKey);
//    }
//
//    /**
//     * DIALOG USE CASE:
//     *
//     * <ul>
//     * <li>hidden operators are not shown</li>
//     * <li>if an operator is provided via extension point, the builtin version is not shown</li>
//     * </ul>
//     */
//
//    static List<StringChoice> getSortedChoices(final AvailableOperators opsAvailable) {
//        final List<StringChoice> sortedChoices = new ArrayList<>();
//        for (var builtinStringChoice : BUILTIN_CHOICES_ORDERED) {
//            final var filterOp = builtinStringChoice.getFirst();
//            final var stringChoice = builtinStringChoice.getSecond();
//            final var builtinOpInterface = BUILTIN_OPERATOR_ID_MAP.get(filterOp.name());
//            if (builtinOpInterface != null) {
//                for (var builtinOp : opsAvailable.builtinOperatorsFromExtension()) {
//                    if (builtinOpInterface.isInstance(builtinOp)) {
//                        sortedChoices.add(stringChoice);
//                        continue; // no internal builtin operator needed
//                    }
//                }
//            }
//            // Add internal operators
//            if (!sortedChoices.contains(stringChoice) && opsAvailable.internalBuiltinOperators().stream()
//                .filter(pair -> !pair.getSecond().isHidden()).anyMatch(pair -> pair.getFirst().equals(filterOp))) {
//                sortedChoices.add(stringChoice);
//            }
//
//        }
//        for (var customOp : opsAvailable.customOperatorsFromExtension()) {
//            sortedChoices.add(new StringChoice(customOp.getId(), customOp.getDisplayName()));
//        }
//        return sortedChoices;
//
//    }
//
//    static Class<? extends FilterValueParameters> getTargetClass(final AvailableOperators opsAvailable,
//        final String chosenId) throws InvalidSettingsException {
//        final var builtinOpInterface = BUILTIN_OPERATOR_ID_MAP.get(chosenId);
//        if (builtinOpInterface != null) {
//            for (var builtinOp : opsAvailable.builtinOperatorsFromExtension()) {
//                if (builtinOpInterface.isInstance(builtinOp)) {
//                    return builtinOp.getNodeParametersClass();
//                }
//            }
//        }
//        for (var customOp : opsAvailable.customOperatorsFromExtension()) {
//            if (customOp.getId().equals(chosenId)) {
//                return customOp.getNodeParametersClass();
//            }
//        }
//        for (var ifo : opsAvailable.internalBuiltinOperators()) {
//            final var id = ifo.getFirst().name();
//            if (id.equals(chosenId)) {
//                return ifo.getSecond().getParametersClass();
//            }
//        }
//        throw new InvalidSettingsException(
//            "Could not find operator with id " + chosenId + " for the selected column. ");
//
//    }

    /**
     * EXECUTION USE CASE:
     *
     * We need to support also hidden and also internal operators when extension-provided operators are present.
     *
     * TODO: currently not implemented
     */

}
