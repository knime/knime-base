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
 *   Mar 24, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.spec;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.ArrayUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.core.node.ExecutionMonitor;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.randomaccess.RandomAccessible;
import org.knime.filehandling.core.node.table.reader.read.Read;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link TableSpecGuesser}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("resource") // no ressources are actually allocated anywhere in this class
public class TableSpecGuesserTest {

    @Mock
    private TypeHierarchy<String, String> m_typeHierarchy;

    @Mock
    private TypeResolver<String, String> m_typeResolver;

    @Mock
    private RandomAccessible<String> m_randomAccessible;

    @Mock
    private ExecutionMonitor m_monitor = null;

    private TableSpecGuesser<Object, String, String> m_testInstance;

    @SuppressWarnings("unchecked")
    private static Read<Object, String> mockRead(final String[]... table) throws IOException {
        List<RandomAccessible<String>> rows =
            Stream.concat(Arrays.stream(table).map(TableSpecGuesserTest::mockRandomAccessible),
                Stream.of((RandomAccessible<String>)null)).collect(toList());
        Read<Object, String> read = Mockito.mock(Read.class);
        when(read.next()).thenReturn(rows.get(0), rows.stream().skip(1).toArray(RandomAccessible[]::new));
        return read;
    }

    private static String[][] to2D(final int nCols, final String... elements) {
        final int nRows = (int)Math.ceil(elements.length / ((double)nCols));
        final String[][] table = new String[nRows][nCols];
        for (int i = 0; i < elements.length; i++) {
            final int r = i / nCols;
            final int c = i % nCols;
            table[r][c] = elements[i];
        }
        return table;
    }

    private static String[] a(final String... elements) {
        return elements;
    }

    private static String[][] m(final String[]... rows) {
        return rows;
    }

    private static Collection<Boolean> c(final int n) {
        return Collections.nCopies(n, Boolean.TRUE);
    }

    private static RandomAccessible<String> mockRandomAccessible(final String[] values) {
        @SuppressWarnings("unchecked")
        RandomAccessible<String> mock = mock(RandomAccessible.class);
        when(mock.size()).thenReturn(values.length);
        for (int i = 0; i < values.length; i++) {
            when(mock.get(i)).thenReturn(values[i]);
        }
        when(mock.copy()).thenReturn(mock);
        when(mock.stream()).thenReturn(Arrays.stream(values));
        return mock;
    }

    private void setupTypeHierarchy(final TypedReaderTableSpec<String> spec, final boolean stopEarly) {
        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.reachedTop()).thenReturn(stopEarly);
        if (spec.size() == 1) {
            when(m_typeResolver.getMostSpecificType()).thenReturn(spec.getColumnSpec(0).getType());
        } else {
            when(m_typeResolver.getMostSpecificType()).thenReturn(spec.getColumnSpec(0).getType(),
                spec.stream().skip(1).map(TypedReaderColumnSpec::getType).toArray(String[]::new));
        }
    }

    private static TableReadConfig<?> setupConfig(final long columnHeaderIdx, final int rowIdIdx,
        final boolean allowShortRows) {
        final TableReadConfig<?> config = mock(TableReadConfig.class);
        when(config.useRowIDIdx()).thenReturn(rowIdIdx >= 0);
        when(config.useColumnHeaderIdx()).thenReturn(columnHeaderIdx >= 0);
        when(config.allowShortRows()).thenReturn(allowShortRows);
        if (rowIdIdx >= 0) {
            when(config.getRowIDIdx()).thenReturn(rowIdIdx);
        }
        if (columnHeaderIdx >= 0) {
            when(config.getColumnHeaderIdx()).thenReturn(columnHeaderIdx);
        }
        return config;
    }

    /**
     * Initializes the test instance before each test.
     */
    @Before
    public void init() {
        m_testInstance = new TableSpecGuesser<>(m_typeHierarchy, Object::toString);
    }

    /**
     * Tests if the constructor fails if the typeHierarchy argument is {@code null}.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFailsOnNullHierarchy() {
        new TableSpecGuesser<Object, String, String>(null, Object::toString);
    }

    /**
     * Tests if the constructor fails if the columnNameExtractor argument is {@code null}.
     */
    @SuppressWarnings("unused")
    @Test(expected = IllegalArgumentException.class)
    public void testConstructorFailsOnNullExtractor() {
        new TableSpecGuesser<Object, String, String>(m_typeHierarchy, null);
    }

    /**
     * Tests the {@link TableSpecGuesser#guessSpec(Read, TableReadConfig, ExecutionMonitor)} method if no headers are
     * retrieved and all rows have the same size.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testAllRowsSameSizeWithoutAnyHeaders() throws IOException {
        TableReadConfig<?> config = setupConfig(-1, -1, false);
        String[][] table = to2D(3, "a", "b", "c", "d", "e", "f", "g", "h", "i");
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList("foo", "bar", "bla"), c(3));
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests guessing if there are no column or row headers and some rows are shorter.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testSomeShortRowsWithoutAnyHeaders() throws IOException {
        TableReadConfig<?> config = setupConfig(-1, -1, true);
        String[][] table = m(a("a", "b", "c"), a("d", "e"), a("f", "g", "h"));
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList("foo", "bar", "bla"), c(3));
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests guessing if all rows are of the same size and column headers are read.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testAllRowsSameSizeWithColumnHeader() throws IOException {
        String[][] table = to2D(3, "a", "b", "c", "d", "e", "f", "g", "h", "i");
        for (int i = 0; i < table.length; i++) {
            TableReadConfig<?> config = setupConfig(i, -1, false);
            TypedReaderTableSpec<String> expected =
                TypedReaderTableSpec.create(asList(table[i]), asList("foo", "bar", "bla"), c(3));
            testGuessSpec(table, config, expected);
        }
    }

    /**
     * Tests guessing if some rows are shorter and column headers are present.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testSomeShortRowsWithColumnHeader() throws IOException {
        String[][] table = m(a("a", "b", "c"), a("d", "e"), a("f", "g", "h"));
        for (int i = 0; i < table.length; i++) {
            TableReadConfig<?> config = setupConfig(i, -1, true);
            TypedReaderTableSpec<String> expected =
                TypedReaderTableSpec.create(padToSize(table[i], 3), asList("foo", "bar", "bla"), c(3));
            testGuessSpec(table, config, expected);
        }
    }

    /**
     * Tests guessing if all rows are of the same size and there is a rowid column.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testAllRowsSameSizeWithRowID() throws IOException {
        String[][] table = to2D(3, "a", "b", "c", "d", "e", "f", "g", "h", "i");
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length - 1);
        for (int i = 0; i < 3; i++) {
            TableReadConfig<?> config = setupConfig(-1, i, false);
            TypedReaderTableSpec<String> expected =
                TypedReaderTableSpec.create(asList(ArrayUtils.remove(expectedTypes, i)), hasTypes);
            testGuessSpec(table, config, expected);
        }
    }

    /**
     * Tests guessing if some rows are shorter and there is a rowid column.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testSomeShortRowsWithRowID() throws IOException {
        String[][] table = m(a("a", "b", "c"), a("d", "e"), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length - 1);
        for (int i = 0; i < 3; i++) {
            TableReadConfig<?> config = setupConfig(-1, i, true);
            TypedReaderTableSpec<String> expected =
                TypedReaderTableSpec.create(asList(ArrayUtils.remove(expectedTypes, i)), hasTypes);
            testGuessSpec(table, config, expected);
        }
    }

    /**
     * Tests guessing if the rows are all the same size and there are both column headers and a rowid column.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testAllRowsSameSizeWithColumnHeaderAndRowID() throws IOException {
        String[][] table = to2D(3, "a", "b", "c", "d", "e", "f", "g", "h", "i");
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length - 1);
        for (int i = 0; i < 3; i++) {
            TableReadConfig<?> config = setupConfig(i, i, false);
            TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList(ArrayUtils.remove(table[i], i)),
                asList(ArrayUtils.remove(expectedTypes, i)), hasTypes);
            testGuessSpec(table, config, expected);
        }
    }

    /**
     * Tests guessing if some rows are shorter and there are both column headers and a rowid column.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testSomeShortRowsWithColumnHeaderAndRowID() throws IOException {
        String[][] table = m(a("a", "b", "c"), a("d", "e"), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length - 1);
        for (int i = 0; i < 3; i++) {
            TableReadConfig<?> config = setupConfig(i, i, true);
            TypedReaderTableSpec<String> expected =
                TypedReaderTableSpec.create(asList(ArrayUtils.remove(padToSize(table[i], 3).toArray(new String[0]), i)),
                    asList(ArrayUtils.remove(expectedTypes, i)), hasTypes);
            testGuessSpec(table, config, expected);
        }
    }

    /**
     * Tests if the guessing fails if the column header row is not part of the table.
     *
     * @throws IOException never thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void testColumnHeaderRowNotInTable() throws IOException {
        TableReadConfig<?> config = setupConfig(10, -1, false);
        String[][] table = to2D(3, "a", "b", "c", "d", "e", "f", "g", "h", "i");
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList("foo", "bar", "bla"), c(3));
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests if skipping empty rows works with column headers and rowids.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testSkipEmptyRowsWithColumnHeaderAndRowID() throws IOException {
        String[][] table = m(a("a", "b", "c"), a(), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length - 1);
        TableReadConfig<?> config = setupConfig(1, 1, true);
        when(config.skipEmptyRows()).thenReturn(true);
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList(ArrayUtils.remove(table[2], 1)),
            asList(ArrayUtils.remove(expectedTypes, 1)), hasTypes);
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests the behavior if the column header row is empty.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testColumnHeaderRowIsEmpty() throws IOException {
        String[][] table = m(a("a", "b", "c"), a(), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length);
        TableReadConfig<?> config = setupConfig(1, -1, true);
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList(expectedTypes), hasTypes);
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests the behavior if the column header contains a null value.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testColumnHeaderContainsNull() throws IOException {
        String[][] table = m(a("a", null, "c"), a(), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length);
        TableReadConfig<?> config = setupConfig(0, -1, true);
        TypedReaderTableSpec<String> expected =
            TypedReaderTableSpec.create(padToSize(table[0], 3), asList(expectedTypes), hasTypes);
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests the behavior if the column header row is not contained in the table to read.
     *
     * @throws IOException never thrown
     */
    @Test(expected = IllegalArgumentException.class)
    public void testColumnHeaderRowIndexGreatThanSizeOfTheTableToRead() throws IOException {
        String[][] table = m(a("a", "b", "c"), a(), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length);
        TableReadConfig<?> config = setupConfig(4, -1, true);
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList(expectedTypes), hasTypes);
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests the behavior if the row header row is not existent.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testRowIdxIndexGreaterThanNumberColumnsOfTheTableToRead() throws IOException {
        String[][] table = m(a("a", "b", "c"), a(), a("f", "g", "h"));
        String[] expectedTypes = a("foo", "bar", "bla");
        Collection<Boolean> hasTypes = c(expectedTypes.length);
        TableReadConfig<?> config = setupConfig(-1, 6, true);
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList(expectedTypes), hasTypes);
        testGuessSpec(table, config, expected);
    }

    /**
     * Tests stopping the type search early on if all rows are of the same size.
     *
     * @throws IOException never thrown
     */
    @Test
    public void testStopTypeSearchEarlyAllRowsSameSize() throws IOException {
        String[][] table = to2D(3, "a", "b", "c", "d", "e", "f", "g", "h", "i");
        for (int i = 0; i < table.length; i++) {
            TableReadConfig<?> config = setupConfig(i, -1, false);
            TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList(table[i]),
                asList("foo", "bar", "bla"), asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));
            testGuessSpec(table, config, expected, true);
        }
    }

    /**
     * Tests stopping the type search early on if some rows are shorter (in which case it won't be stopped since later
     * rows might introduce more columns).
     *
     * @throws IOException never thrown
     */
    @Test
    public void testStopTypeSearchEarlySomeShortRows() throws IOException {
        String[][] table = m(a("a", "b", "c"), a("d", "e"), a("f", "g", "h"));
        for (int i = 0; i < table.length; i++) {
            TableReadConfig<?> config = setupConfig(i, -1, true);
            TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(padToSize(table[i], 3),
                asList("foo", "bar", "bla"), asList(Boolean.TRUE, Boolean.TRUE, Boolean.TRUE));
            testGuessSpec(table, config, expected, true);
        }
    }

    private static List<String> padToSize(final String[] array, final int size) {
        return Stream.concat(Stream.of(array), Stream.generate(() -> null))//
            .limit(size)//
            .collect(toList());
    }

    private void testGuessSpec(final String[][] table, final TableReadConfig<?> config,
        final TypedReaderTableSpec<String> expected, final boolean earlyStopping) throws IOException {
        setupTypeHierarchy(expected, earlyStopping);
        Read<Object, String> read = mockRead(table);
        TypedReaderTableSpec<String> actual = m_testInstance.guessSpec(read, config, m_monitor);
        int colHeaderIdx = (int)config.getColumnHeaderIdx();
        assertEquals("Specs differed when column header was in row " + colHeaderIdx, expected, actual);
    }

    private void testGuessSpec(final String[][] table, final TableReadConfig<?> config,
        final TypedReaderTableSpec<String> expected) throws IOException {
        testGuessSpec(table, config, expected, false);
    }

}
