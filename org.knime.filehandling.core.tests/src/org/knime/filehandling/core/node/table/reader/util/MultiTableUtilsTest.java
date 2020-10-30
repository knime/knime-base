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
 *   Mar 31, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader.util;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.config.TableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnTransformation;
import org.knime.filehandling.core.node.table.reader.selector.TableTransformation;
import org.knime.filehandling.core.node.table.reader.spec.ReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit tests for {@link MultiTableUtils}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiTableUtilsTest {

    @Mock
    private TableReadConfig<?> m_tableReadConfig;

    @Mock
    private TableTransformation<String> m_transformationModel;

    @Mock
    private ColumnTransformation<String> m_transformation;

    /**
     * Tests the {@link MultiTableUtils#getNameAfterInit(ReaderColumnSpec)} method.
     */
    @Test
    public void testGetNameAfterInit() {
        assertEquals("test",
            MultiTableUtils.getNameAfterInit(TypedReaderColumnSpec.createWithName("test", "foo", true)));
    }

    /**
     * Tests if {@link MultiTableUtils#getNameAfterInit(ReaderColumnSpec)} fails if the provided
     * {@link TypedReaderColumnSpec} has no name.
     */
    @Test(expected = IllegalStateException.class)
    public void testGetNameAfterInitFailsIfSpecHasNoName() {
        MultiTableUtils.getNameAfterInit(TypedReaderColumnSpec.create("foo", true));
    }

    /**
     * Tests the {@code assingNamesIfMissing} method.
     */
    @Test
    public void testAssignNamesIfMissing() {
        TypedReaderTableSpec<String> namesMissing = TypedReaderTableSpec.create(asList("hubert", null),
            asList("berta", "frieda"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> expected = TypedReaderTableSpec.create(asList("hubert", "Column1"),
            asList("berta", "frieda"), asList(Boolean.TRUE, Boolean.TRUE));
        TypedReaderTableSpec<String> actual = MultiTableUtils.assignNamesIfMissing(namesMissing);
        assertEquals(expected, actual);
    }

}
