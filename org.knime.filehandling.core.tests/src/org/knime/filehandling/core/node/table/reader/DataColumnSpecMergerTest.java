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
 *   Jun 14, 2023 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.stream.Collectors.toCollection;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataColumnSpecCreator.MergeOptions;
import org.knime.core.data.container.DataContainerSettings;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.StringCell;

/**
 * Unit tests for DataColumnSpecMerger.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class DataColumnSpecMergerTest {

    private static final EnumSet<MergeOptions> MERGE_OPTIONS = EnumSet.noneOf(MergeOptions.class);

    @Test
    void testMergingWithNullBounds() throws Exception {
        IntCell upperBound = new IntCell(42);
        IntCell lowerBound = new IntCell(3);
        var noLower = createWithBounds(null, upperBound);
        var noUpper = createWithBounds(lowerBound, null);
        var expected = createWithBounds(lowerBound, upperBound);
        assertEquals(expected, createMerger().merge(noLower).merge(noUpper).createMergedSpec(),
            "The present bounds should have been used.");
        assertEquals(expected, createMerger().merge(noUpper).merge(noLower).createMergedSpec(),
            "The present bounds should have been used.");
    }

    @Test
    void testMergingWithPresentBounds() throws Exception {
        var first = createWithBounds(new IntCell(23), new IntCell(666));
        var second = createWithBounds(new IntCell(Integer.MIN_VALUE), new IntCell(0));
        var expected = createWithBounds(new IntCell(Integer.MIN_VALUE), new IntCell(666));
        assertEquals(expected, createMerger().merge(first).merge(second).createMergedSpec(), "Unexpected bounds.");
        assertEquals(expected, createMerger().merge(second).merge(first).createMergedSpec(), "Unexpected bounds.");
    }

    private static DataColumnSpecMerger createMerger() {
        return new DataColumnSpecMerger(MERGE_OPTIONS);
    }

    private static DataColumnSpec createWithBounds(final IntCell lowerBound, final IntCell upperBound) {
        var col = new DataColumnSpecCreator("foo", IntCell.TYPE);
        col.setDomain(new DataColumnDomainCreator(lowerBound, upperBound).createDomain());
        return col.createSpec();
    }

    @Test
    void testDropPossibleValuesIfTooMany() throws Exception {
        var maxPossibleValues = DataContainerSettings.getDefault().getMaxDomainValues();
        var firstPossibleValues = IntStream.range(0, maxPossibleValues)//
            .mapToObj(Integer::toString)//
            .map(StringCell::new)//
            .collect(toCollection(LinkedHashSet::new));
        var secondPossibleValues = IntStream.range(0, maxPossibleValues)//
            .map(i -> i + maxPossibleValues)//
            .mapToObj(Integer::toString)//
            .map(StringCell::new)//
            .collect(toCollection(LinkedHashSet::new));

        var first = createWithPossibleValues(firstPossibleValues);
        var second = createWithPossibleValues(secondPossibleValues);
        assertFalse(createMerger().merge(first).merge(second).createMergedSpec().getDomain().hasValues(),
            "Possible values should have been dropped because there are too many.");
    }

    @Test
    void testDropPossibleValuesOnNull() throws Exception {
        var hasValues = createWithPossibleValues(Set.of(new StringCell("foo")));
        var hasNoValues = createWithPossibleValues(null);
        assertFalse(createMerger().merge(hasValues).merge(hasNoValues).createMergedSpec().getDomain().hasValues(),
            "Should have no possible values because one of the columns has no possible values.");
        assertFalse(createMerger().merge(hasNoValues).merge(hasValues).createMergedSpec().getDomain().hasValues(),
                "Should have no possible values because one of the columns has no possible values.");
    }

    @Test
    void testComplainsIfMergeIsNotCalled() throws Exception {
        assertThrows(IllegalStateException.class, () -> createMerger().createMergedSpec());
    }

    private static DataColumnSpec createWithPossibleValues(final Set<StringCell> possibleValues) {
        var col = new DataColumnSpecCreator("foo", StringCell.TYPE);
        col.setDomain(new DataColumnDomainCreator(possibleValues).createDomain());
        return col.createSpec();
    }

}
