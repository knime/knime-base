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
package org.knime.base.node.flowvariable.converter.celltovariable;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.knime.core.data.collection.ListCell.getCollectionType;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.junit.Test;
import org.knime.core.data.DataType;
import org.knime.core.data.def.BooleanCell;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
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
import org.knime.filehandling.core.data.location.cell.SimpleFSLocationCellFactory;
import org.knime.filehandling.core.data.location.variable.FSLocationVariableType;

/**
 * Tests the {@link CellToVariableConverterFactory}.
 *
 * @author Mark Ortmann, KNIME GmbH, Berlin, Germany
 */
public class CellToVariableConverterFactoryTest {

    private static final DataType[] SUPPORTED_DATA_TYPES = new DataType[]{//
        BooleanCell.TYPE, //
        IntCell.TYPE, //
        LongCell.TYPE, //
        DoubleCell.TYPE, //
        SimpleFSLocationCellFactory.TYPE, //
        StringCell.TYPE, //
        getCollectionType(BooleanCell.TYPE), //
        getCollectionType(IntCell.TYPE), //
        getCollectionType(LongCell.TYPE), //
        getCollectionType(DoubleCell.TYPE), //
        getCollectionType(StringCell.TYPE)//
    };

    private static final VariableType<?>[] SUPPORTED_VARIABLE_TYPES =
        new VariableType[]{BooleanType.INSTANCE, BooleanArrayType.INSTANCE, IntType.INSTANCE, IntArrayType.INSTANCE,
            LongType.INSTANCE, LongArrayType.INSTANCE, DoubleType.INSTANCE, DoubleArrayType.INSTANCE,
            StringType.INSTANCE, StringArrayType.INSTANCE, FSLocationVariableType.INSTANCE};

    /**
     * Tests the correctness of the {@link CellToVariableConverterFactory#isSupported(DataType)} method.
     */
    @Test
    public void testIsSupported() {
        Arrays.stream(SUPPORTED_DATA_TYPES)//
            .forEach(t -> Assert.isTrue(CellToVariableConverterFactory.isSupported(t)));
    }

    /**
     * Tests the correctness of the {@link CellToVariableConverterFactory#createConverter(DataType)} method.
     */
    @Test
    public void testCreateConverters() {
        Arrays.stream(SUPPORTED_DATA_TYPES)//
            .forEach(t -> assertNotNull(CellToVariableConverterFactory.createConverter(t)));
    }

    /**
     * Tests the correctness of the {@link CellToVariableConverterFactory#getSupportedTargetVariableTypes()} method.
     */
    @Test
    public void testGetSupportedTargetVariableTypes() {
        final Set<VariableType<?>> expectedSupportedVariableTypes =
            new HashSet<>(Arrays.asList(SUPPORTED_VARIABLE_TYPES));
        final Set<VariableType<?>> supportedTargetVariableTypes =
            CellToVariableConverterFactory.getSupportedTargetVariableTypes();
        assertTrue(expectedSupportedVariableTypes.containsAll(supportedTargetVariableTypes));
        assertTrue(supportedTargetVariableTypes.containsAll(expectedSupportedVariableTypes));
    }

}
