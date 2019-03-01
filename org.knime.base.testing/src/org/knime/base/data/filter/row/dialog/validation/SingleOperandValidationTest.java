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

package org.knime.base.data.filter.row.dialog.validation;

import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SuppressWarnings("javadoc")
public class SingleOperandValidationTest {

    private SingleOperandValidation m_validation;

    @Mock
    private Operator m_operator;

    @Rule
    public MockitoRule m_mockitoRule = MockitoJUnit.rule();

    @Before
    public void setUp() {
        m_validation = new SingleOperandValidation();
    }

    @Test
    public void shouldAllowOneValueOnly() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("string", StringCell.TYPE);

        // when values array has one value only
        ValidationResult result =
            m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"abc"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when values array is NULL
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, null));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("one value"));

        // when values array is empty
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[0]));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("one value"));

        // when values array has two values
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"abc", "def"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("one value"));
    }

    @Test
    public void canCheckStringValue() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("string", StringCell.TYPE);

        // when values array has a value
        ValidationResult result =
            m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"abc"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when string value is NULL
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{null}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("empty"));

        // when string value is blank
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{" \t \n "}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("empty"));
    }

    @Test
    public void canCheckIntegerValue() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("integer", IntCell.TYPE);

        // when integer value is OK
        ValidationResult result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"1"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when integer value is OK bat has spaces
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{" 1 "}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when couldn't parse integer value
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"1.5"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("convert to integer"));
    }

    @Test
    public void canCheckLongValue() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("long", LongCell.TYPE);

        // when long value is OK
        ValidationResult result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"1"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when long value is OK but has spaces
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{" 1 "}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when couldn't parse long value
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"1.5"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("convert to long"));
    }

    @Test
    public void canCheckDoubleValue() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("double", DoubleCell.TYPE);

        // when double value is OK
        ValidationResult result =
            m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"1.876"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when double value is OK but has spaces
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{" 10 "}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when couldn't parse double value
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"abc"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        Assert.assertThat(result.getErrors(), Matchers.hasSize(1));
        Assert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("convert to double"));
    }
}
