/*
 * ------------------------------------------------------------------------
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
 * ------------------------------------------------------------------------
 */

package org.knime.base.data.filter.row.dialog.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.knime.base.data.filter.row.dialog.OperatorFunction;
import org.knime.base.data.filter.row.dialog.OperatorPanel;
import org.knime.base.data.filter.row.dialog.OperatorValidation;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.core.data.DataType;

/**
 * Abstract implementation of the {@link OperatorFunction} interface.
 *
 * @param <F> the {@link OperatorFunction} subclass to apply on the operator
 * @author Viktor Buria
 * @since 3.8
 */
public class AbstractOperatorRegistry<F extends OperatorFunction<?, ?>> implements OperatorRegistry<F> {

    private final Map<OperatorKey, OperatorValue<F>> m_operators = Collections.synchronizedMap(new LinkedHashMap<>());

    @Override
    public void addOperator(final OperatorKey key, final OperatorValue<F> value) {
        m_operators.put(key, value);
    }

    @Override
    public Optional<F> findFunction(final OperatorKey key) {
        final OperatorValue<F> value = findOperator(key);
        return Optional.ofNullable(value == null ? null : value.getFunction());
    }

    @Override
    public Optional<OperatorValidation> findValidation(final OperatorKey key) {
        final OperatorValue<F> value = findOperator(key);
        return Optional.ofNullable(value == null ? null : value.getValidation());
    }

    @Override
    public Optional<OperatorPanel> findPanel(final OperatorKey key) {
        final OperatorValue<F> value = findOperator(key);
        return Optional.ofNullable(value == null ? null : value.getPanel());
    }

    @Override
    public List<Operator> findRegisteredOperators(final DataType dataType) {
        return m_operators.keySet().stream().filter(k -> k.getDataType() == null || k.getDataType().equals(dataType))
            .map(OperatorKey::getOperator).distinct().collect(Collectors.toList());
    }

    /**
     * Gets an operator by the given {@link OperatorKey}. That function is for internal use only.
     *
     * @param key the {@link OperatorKey}.
     * @return the {@link OperatorValue}
     */
    protected OperatorValue<F> findOperator(final OperatorKey key) {
        return m_operators.getOrDefault(Objects.requireNonNull(key, "key"),
            m_operators.get(OperatorKey.defaultKey(key.getOperator())));
    }

}
