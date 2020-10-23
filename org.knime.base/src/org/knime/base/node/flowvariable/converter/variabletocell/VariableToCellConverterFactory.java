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
 *   Sep 7, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable.converter.variabletocell;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.BooleanArrayVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.BooleanVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.DoubleArrayVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.DoubleVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.IntArrayVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.IntVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.LongArrayVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.LongVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.StringArrayVariableConverter;
import org.knime.base.node.flowvariable.converter.variabletocell.SimpleVarToCellConverter.StringVariableConverter;
import org.knime.core.data.DataType;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.DoubleArrayType;
import org.knime.core.node.workflow.VariableType.DoubleType;
import org.knime.core.node.workflow.VariableType.IntArrayType;
import org.knime.core.node.workflow.VariableType.IntType;
import org.knime.core.node.workflow.VariableType.LongArrayType;
import org.knime.core.node.workflow.VariableType.LongType;
import org.knime.core.node.workflow.VariableType.StringArrayType;
import org.knime.core.node.workflow.VariableType.StringType;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;

/**
 * Factory class to create the {@link VariableToCellConverter} associated with the provided {@link FlowVariable}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public final class VariableToCellConverterFactory {

    private static final Map<VariableType<?>, Supplier<VariableToCellConverter>> SUPPORTED_TYPES;

    static {
        SUPPORTED_TYPES = new HashMap<>();
        SUPPORTED_TYPES.put(StringType.INSTANCE, StringVariableConverter::new);
        SUPPORTED_TYPES.put(StringArrayType.INSTANCE, StringArrayVariableConverter::new);
        SUPPORTED_TYPES.put(DoubleType.INSTANCE, DoubleVariableConverter::new);
        SUPPORTED_TYPES.put(DoubleArrayType.INSTANCE, DoubleArrayVariableConverter::new);
        SUPPORTED_TYPES.put(IntType.INSTANCE, IntVariableConverter::new);
        SUPPORTED_TYPES.put(IntArrayType.INSTANCE, IntArrayVariableConverter::new);
        SUPPORTED_TYPES.put(LongType.INSTANCE, LongVariableConverter::new);
        SUPPORTED_TYPES.put(LongArrayType.INSTANCE, LongArrayVariableConverter::new);
        SUPPORTED_TYPES.put(BooleanType.INSTANCE, BooleanVariableConverter::new);
        SUPPORTED_TYPES.put(BooleanArrayType.INSTANCE, BooleanArrayVariableConverter::new);
        SUPPORTED_TYPES.put(FSLocationVariableType.INSTANCE, FSLocationVarToCellConverter::new);
    }

    private VariableToCellConverterFactory() {
        // factory class
    }

    /**
     *
     * Creates the {@link VariableToCellConverter} associated with the given {@link VariableType}.
     *
     * @param var the {@link FlowVariable} to be converted
     * @return the converter associated with the given {@link DataType}
     */
    public static VariableToCellConverter createConverter(final FlowVariable var) {
        final VariableType<?> varType = var.getVariableType();
        if (!isSupported(varType)) {
            throw new IllegalArgumentException(
                String.format("There is no Variable to Cell converter associated with the provided variable type '%s'",
                    varType.getIdentifier().toLowerCase()));
        }
        return SUPPORTED_TYPES.get(varType).get();
    }

    /**
     * Returns {@code true} if there exists a converter for the given {@link VariableType} and {@code false} otherwise.
     *
     * @param varType the {@link VariableType} of the {@link FlowVariable} that needs to be converted
     * @return {@code true} if such a converter exists and {@code false} otherwise
     */
    public static boolean isSupported(final VariableType<?> varType) {
        return SUPPORTED_TYPES.containsKey(varType);
    }

    /**
     * Returns the {@link VariableType}s that can be converted.
     *
     * @return an array containing all supported {@link VariableType}s
     */
    public static VariableType<?>[] getSupportedTypes() { //NOSONAR
        return SUPPORTED_TYPES.keySet().stream()//
            .toArray(VariableType<?>[]::new);
    }

}
