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

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.knime.base.data.filter.row.dialog.OperatorParameters;
import org.knime.base.data.filter.row.dialog.OperatorValidation;
import org.knime.base.data.filter.row.dialog.ValidationResult;
import org.knime.base.data.filter.row.dialog.model.ColumnSpec;
import org.knime.base.data.filter.row.dialog.model.Operator;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@SuppressWarnings("javadoc")
public abstract class AbstractMultipleDateTimeOperandsValidationTest {

    private OperatorValidation m_validation;

    @Mock
    private Operator m_operator;

    @Rule
    public MockitoRule m_mockitoRule = MockitoJUnit.rule();

    protected AbstractMultipleDateTimeOperandsValidationTest(final OperatorValidation validation) {
        m_validation = validation;
    }

    @Test
    public void canCheckDateValues() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("date", LocalDateCellFactory.TYPE);

        // when date values are OK
        ValidationResult result = m_validation
            .apply(new OperatorParameters(columnSpec, m_operator, new String[]{"2017-05-23", "2019-01-15"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when date value are OK but have spaces
        result = m_validation.apply(
            new OperatorParameters(columnSpec, m_operator, new String[]{" 2017-05-23 ", " \t \n 2019-01-15  \t \n "}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when couldn't parse date values
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"2017-99-99", "abc"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        MatcherAssert.assertThat(result.getErrors(), Matchers.hasSize(2));
        MatcherAssert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("convert to local date"));
        MatcherAssert.assertThat(result.getErrors().get(1).getError(), Matchers.containsString("convert to local date"));
    }

    @Test
    public void canCheckDateTimeValues() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("date_time", LocalDateTimeCellFactory.TYPE);

        // when date & time values are OK
        ValidationResult result = m_validation.apply(
            new OperatorParameters(columnSpec, m_operator, new String[]{"2017-05-23T06:25", "2019-01-15T12:15:23"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when date&time values are OK but have spaces
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator,
            new String[]{" 2017-05-23T06:25 ", " \t \n 2019-01-15T12:15:23 \t \n "}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when couldn't parse date&time values
        result =
            m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"06:25:13", "11:45:53"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        MatcherAssert.assertThat(result.getErrors(), Matchers.hasSize(2));
        MatcherAssert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("convert to local date & time"));
        MatcherAssert.assertThat(result.getErrors().get(1).getError(), Matchers.containsString("convert to local date & time"));
    }

    @Test
    public void canCheckTimeValue() {
        // given
        final ColumnSpec columnSpec = new ColumnSpec("time", LocalTimeCellFactory.TYPE);

        // when time values are OK
        ValidationResult result =
            m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"06:25", "09:15:43"}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when time values are OK but have spaces
        result = m_validation
            .apply(new OperatorParameters(columnSpec, m_operator, new String[]{" 06:25 ", " \t \n 09:15:43 \t \n "}));

        // then
        Assert.assertFalse("has errors", result.hasErrors());

        // when couldn't parse time values
        result = m_validation.apply(new OperatorParameters(columnSpec, m_operator, new String[]{"abc", "06-25-13"}));

        // then
        Assert.assertTrue("no errors", result.hasErrors());
        MatcherAssert.assertThat(result.getErrors(), Matchers.hasSize(2));
        MatcherAssert.assertThat(result.getErrors().get(0).getError(), Matchers.containsString("convert to local time"));
        MatcherAssert.assertThat(result.getErrors().get(1).getError(), Matchers.containsString("convert to local time"));
    }
}
