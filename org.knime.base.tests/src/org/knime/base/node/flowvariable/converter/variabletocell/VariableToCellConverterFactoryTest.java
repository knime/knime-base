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
 *   Sep 16, 2020 (Mark Ortmann, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.flowvariable.converter.variabletocell;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.knime.core.node.workflow.FlowVariable;
import org.knime.core.node.workflow.VariableType;
import org.knime.core.node.workflow.VariableType.BooleanArrayType;
import org.knime.core.node.workflow.VariableType.BooleanType;
import org.knime.core.node.workflow.VariableType.CredentialsType;
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
 * Tests for the {@link VariableToCellConverterFactory}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class VariableToCellConverterFactoryTest {

    private static final VariableType<?>[] SUPPORTED_VARIABLE_TYPES =
        new VariableType[]{StringType.INSTANCE, StringArrayType.INSTANCE, DoubleType.INSTANCE, DoubleArrayType.INSTANCE,
            IntType.INSTANCE, IntArrayType.INSTANCE, LongType.INSTANCE, LongArrayType.INSTANCE, BooleanType.INSTANCE,
            BooleanArrayType.INSTANCE, FSLocationVariableType.INSTANCE};

    /**
     * Tests the correctness of the {@link VariableToCellConverterFactory#isSupported(VariableType)} method.
     */
    @Test
    public void testIsSupported() {
        for (final VariableType<?> t : SUPPORTED_VARIABLE_TYPES) {
            assertTrue(VariableToCellConverterFactory.isSupported(t));
        }
        assertFalse(VariableToCellConverterFactory.isSupported(CredentialsType.INSTANCE));
    }

    /**
     * Tests the correctness of the {@link VariableToCellConverterFactory#getSupportedTypes()} method.
     */
    @Test
    public void testSupportedTypes() {
        final Set<VariableType<?>> expectedSupported =
            Arrays.stream(SUPPORTED_VARIABLE_TYPES).collect(Collectors.toSet());
        final Set<VariableType<?>> factorySupported =
            Arrays.stream(VariableToCellConverterFactory.getSupportedTypes()).collect(Collectors.toSet());
        assertTrue(expectedSupported.containsAll(factorySupported));
        assertTrue(factorySupported.containsAll(expectedSupported));
    }

    /**
     * Tests the correctness of the {@link VariableToCellConverterFactory#createConverter(FlowVariable)} method.
     */
    @Test
    public void testCreateConverters() {
        Arrays.stream(SUPPORTED_VARIABLE_TYPES)//
            .forEach(t -> assertNotNull(VariableToCellConverterFactory.createConverter(new FlowVariable("dummy", t))));
    }

    /**
     * Tests that the {@link VariableToCellConverterFactory#createConverter(FlowVariable)} method fails if a non
     * supported type is provided.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testCreateConvertersFailsForUnsupportedVariableTypes() {
        VariableToCellConverterFactory.createConverter(new FlowVariable("dummy", CredentialsType.INSTANCE, null));
    }
}
