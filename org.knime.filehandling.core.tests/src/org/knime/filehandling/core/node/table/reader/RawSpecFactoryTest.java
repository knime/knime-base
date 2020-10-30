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
 *   Oct 23, 2020 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.filehandling.core.node.table.reader;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec.createWithName;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.knime.filehandling.core.node.table.reader.selector.RawSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderColumnSpec;
import org.knime.filehandling.core.node.table.reader.spec.TypedReaderTableSpec;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy;
import org.knime.filehandling.core.node.table.reader.type.hierarchy.TypeHierarchy.TypeResolver;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Contains unit test for {@link RawSpecFactory}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("javadoc") // keeping the link is more useful
@RunWith(MockitoJUnitRunner.class)
public class RawSpecFactoryTest {

    @Mock
    private TypeHierarchy<String, String> m_typeHierarchy;

    @Mock
    private TypeResolver<String, String> m_typeResolver;

    private RawSpecFactory<String> m_testInstance;

    @Before
    public void init() {
        m_testInstance = new RawSpecFactory<>(m_typeHierarchy);
    }

    @Test
    public void testCreateAllHaveType() {
        TypedReaderColumnSpec<String> hans = createWithName("hans", "berta", true);
        TypedReaderColumnSpec<String> rudiger = createWithName("rudiger", "elsa", true);
        TypedReaderColumnSpec<String> ulf = createWithName("ulf", "frieda", true);
        TypedReaderTableSpec<String> spec1 = new TypedReaderTableSpec<>(asList(hans, rudiger));
        TypedReaderTableSpec<String> spec2 = new TypedReaderTableSpec<>(asList(rudiger, ulf));
        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.getMostSpecificType()).thenReturn("georgina", "siegfrieda", "bella");
        TypedReaderColumnSpec<String> resolvedHans = createWithName("hans", "georgina", true);
        TypedReaderColumnSpec<String> resolvedRudiger = createWithName("rudiger", "siegfrieda", true);
        TypedReaderColumnSpec<String> resolvedUlf = createWithName("ulf", "bella", true);

        RawSpec<String> expected = new RawSpec<>(new TypedReaderTableSpec<>(asList(resolvedHans, resolvedRudiger, resolvedUlf)),
            new TypedReaderTableSpec<>(asList(resolvedRudiger)));
        assertEquals(expected, m_testInstance.create(asList(spec1, spec2)));
        // Both specs have 2 columns and every column has a type
        verify(m_typeResolver, Mockito.times(4)).accept(any());
    }

    @Test
    public void testCreateSomeWithoutType() {
        TypedReaderColumnSpec<String> rudiger = createWithName("rudiger", "elsa", true);
        TypedReaderColumnSpec<String> ulf = createWithName("ulf", "frieda", true);
        TypedReaderColumnSpec<String> hansWithoutType = createWithName("hans", "franz", false);
        TypedReaderTableSpec<String> spec1 = new TypedReaderTableSpec<>(asList(hansWithoutType, rudiger));
        TypedReaderTableSpec<String> spec2 = new TypedReaderTableSpec<>(asList(rudiger, ulf));

        TypedReaderColumnSpec<String> resolvedHans = createWithName("hans", "georgina", true);
        TypedReaderColumnSpec<String> resolvedRudiger = createWithName("rudiger", "siegfrieda", true);
        TypedReaderColumnSpec<String> resolvedUlf = createWithName("ulf", "bella", true);

        RawSpec<String> expected = new RawSpec<>(new TypedReaderTableSpec<>(asList(resolvedHans, resolvedRudiger, resolvedUlf)),
            new TypedReaderTableSpec<>(asList(resolvedRudiger)));

        when(m_typeHierarchy.createResolver()).thenReturn(m_typeResolver);
        when(m_typeResolver.getMostSpecificType()).thenReturn("georgina", "siegfrieda", "bella");

        assertEquals(expected, m_testInstance.create(asList(spec1, spec2)));
        // Both specs have 2 columns and hansWithoutType has no type, so the type resolver shouldn't be invoked for it
        verify(m_typeResolver, Mockito.times(3)).accept(any());
    }

    @Test(expected = IllegalStateException.class)
    public void testFailsIfNameIsUninitialized() {
        m_testInstance.create(asList(TypedReaderTableSpec.create(asList("siegfrieda"), asList(true))));
    }


}
