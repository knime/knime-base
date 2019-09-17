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
 *   18 Sep 2019 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.preproc.filter.rowref;

import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings("javadoc")
public class DiskBackedBitArrayTest {

    private static final int LENGTH = DiskBackedBitArray.BUFFER_SIZE * 2 + 1;

    private static final boolean[] TEST_ARRAY = new boolean[LENGTH];

    private static final Random RANDOM = new Random(42);

    static {
        for (int i = 0; i < LENGTH; i++) {
            TEST_ARRAY[i] = RANDOM.nextBoolean();
        }
    }

    @Test
    public void testSetAndGet() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            for (boolean b : TEST_ARRAY) {
                if (b) {
                    bitArray.setBit();
                } else {
                    bitArray.skipBit();
                }
            }
            bitArray.setPosition(0);
            for (boolean b : TEST_ARRAY) {
                Assert.assertEquals(b, bitArray.getBit());
            }
        }
    }

    @Test
    public void testSetAndGetSamePass() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            for (boolean b : TEST_ARRAY) {
                if (b) {
                    bitArray.setBit();
                } else {
                    Assert.assertEquals(false, bitArray.getBit());
                }
            }
            bitArray.setPosition(0);
            for (boolean b : TEST_ARRAY) {
                Assert.assertEquals(b, bitArray.getBit());
            }
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayIndexOutOfBoundsExceptionOnSet() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            for (int i = 0; i < LENGTH + 1; i++) {
                bitArray.setBit();
            }
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayIndexOutOfBoundsExceptionOnGet() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            for (int i = 0; i < LENGTH + 1; i++) {
                bitArray.getBit();
            }
        }
    }

    @Test(expected = ArrayIndexOutOfBoundsException.class)
    public void testArrayIndexOutOfBoundsExceptionOnSkip() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            for (int i = 0; i < LENGTH + 1; i++) {
                bitArray.skipBit();
            }
        }
    }

    @Test
    public void testSetPosition() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            final List<Integer> positions =
                IntStream.range(0, 50).map(i -> RANDOM.nextInt(LENGTH)).boxed().collect(Collectors.toList());
            for (int pos : positions) {
                bitArray.setPosition(pos);
                bitArray.setBit();
            }
            for (int pos : positions) {
                bitArray.setPosition(pos);
                Assert.assertEquals(true, bitArray.getBit());
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetOutOfBoundsPositionLow() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.setPosition(-1);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSetOutOfBoundsPositionHigh() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.setPosition(LENGTH);
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalStateExceptionOnSet() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.close();
            bitArray.setBit();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalStateExceptionOnGet() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.close();
            bitArray.getBit();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalStateExceptionOnSkip() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.close();
            bitArray.skipBit();
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalStateExceptionOnSetPosition() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.close();
            bitArray.setPosition(0);
        }
    }

    @Test
    public void testEmptyArray() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(0)) {
        }
    }

    @Test
    public void testSetAndGetArrayOfBufferSize() throws IOException {
        final int length = DiskBackedBitArray.BUFFER_SIZE;
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(length)) {
            for (int i = 0; i < length; i++) {
                bitArray.setBit();
            }
            bitArray.setPosition(0);
            for (int i = 0; i < length; i++) {
                Assert.assertEquals(true, bitArray.getBit());
            }
        }
    }

    @Test
    public void testInitializedWithZeros() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            for (int i = 0; i < LENGTH; i++) {
                Assert.assertEquals(false, bitArray.getBit());
            }
        }
    }

    @Test
    public void testToStringRestorePosition() throws IOException {
        try (final DiskBackedBitArray bitArray = new DiskBackedBitArray(LENGTH)) {
            bitArray.setPosition(LENGTH / 2);
            bitArray.setBit();
            bitArray.setPosition(LENGTH / 2);
            bitArray.toString();
            Assert.assertEquals(true, bitArray.getBit());
        }
    }

}
