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
 *   1 Dec 2025 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.filter.row3.operators.pattern;


import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperator;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dynamic.extensions.filtervalue.FilterOperatorFamily;

/**
 * Operator family for pattern-based operators (regex and wildcard).
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 * @param <D> type of data value filtered by predicates created by this family
 * @param <P> type of pattern filter parameters
 *
 * @noreference This class is not intended to be referenced by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
@SuppressWarnings("restriction") // pending API
public abstract class PatternOperatorFamily<D extends DataValue, P extends PatternFilterParameters>
    implements FilterOperatorFamily<P> {

    private final Class<P> m_paramClass;

    private final Class<D> m_dataValueClass;

    /**
     * Constructor.
     *
     * @param dataValueClass class of the data values filtered by predicates created by this family
     * @param paramClass class of the filter parameters used by the operators of this family
     */
    protected PatternOperatorFamily(final Class<D> dataValueClass, final Class<P> paramClass) {
        m_dataValueClass = dataValueClass;
        m_paramClass = paramClass;
    }

    @Override
    public List<FilterOperator<P>> getOperators() {
        return List.of(new RegexOp(), new WildcardOp());
    }

    private void throwIfRuntimeColumnTypeNotSupported(final DataColumnSpec runtimeColumnSpec,
        final DataType configureColumnType) throws InvalidSettingsException {
        PatternFilterUtils.throwIfNotSupported(runtimeColumnSpec);
        final var singleSupportedDataType = PatternOperatorFamily.this.m_dataValueClass;
        PatternFilterUtils.throwIfNotCompatible(singleSupportedDataType, configureColumnType, runtimeColumnSpec);

        final var subtypes = supportedTypes();
        // no subtypes means no further restriction
        if (!subtypes.isEmpty()) {
            final var incompatible = subtypes.stream().noneMatch(cd -> runtimeColumnSpec.getType().isCompatible(cd));
            if (incompatible) {
                // throwIfNotSupported(..) already checked that a pattern filter operator can be applied, but
                // _this_ operator is not compatible. Hence, a different pattern operator would be
                // through reconfiguring the node. E.g. when going from Int to String, the parameters need a decision
                // for the case-sensitivity that we don't want to do without the user noticing.
                PatternFilterUtils.throwNotCompatible(configureColumnType, runtimeColumnSpec);
            }
        }
    }

    /**
     * Creates the actual filter predicate.
     *
     * @param runtimeColumnSpec column spec at run time
     * @param configureColumnType column data type when the dialog was configured
     * @param isRegex {@code true} for regex, {@code false} for wildcard
     * @param params operator specific parameters
     * @return predicate to filder data values of type {@code D}
     * @throws InvalidSettingsException if the parameters are invalid
     */
    protected abstract Predicate<D> createPredicate(DataColumnSpec runtimeColumnSpec, DataType configureColumnType,
        boolean isRegex, P params) throws InvalidSettingsException;

    /**
     * Override this method to further restrict the supported data value types for this operator family. Can be used if
     * there is no common DataValue superclass for all supported types. Each of the given types must be compatible
     * with {@code D}, otherwise an exception is thrown.
     *
     * @return set of supported data value types, or empty if all types compatible with {@code D} are supported
     */
    protected Set<Class<? extends DataValue>> supportedTypes() {
        return Set.of();
    }

    private Predicate<D> createPredicateCompatible(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final boolean isRegex, final P params) throws InvalidSettingsException {
        throwIfRuntimeColumnTypeNotSupported(runtimeColumnSpec, configureColumnType);
        return createPredicate(runtimeColumnSpec, configureColumnType, isRegex, params);
    }

    // default implementation for regex operator
    private final class RegexOp implements FilterOperator<P>, RegexOperator {

        @Override
        public Class<P> getNodeParametersClass() {
            return m_paramClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P params) throws InvalidSettingsException {
            return (Predicate<DataValue>)PatternOperatorFamily.this.createPredicateCompatible(runtimeColumnSpec,
                configureColumnType, true, params);
        }

    }

    // default implementation for wildcard operator
    private final class WildcardOp implements FilterOperator<P>, WildcardOperator {

        @Override
        public Class<P> getNodeParametersClass() {
            return m_paramClass;
        }

        @SuppressWarnings("unchecked")
        @Override
        public Predicate<DataValue> createPredicate(final DataColumnSpec runtimeColumnSpec,
            final DataType configureColumnType, final P params) throws InvalidSettingsException {
            return (Predicate<DataValue>)PatternOperatorFamily.this.createPredicateCompatible(runtimeColumnSpec,
                configureColumnType, false, params);
        }

    }

}
