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
 *   8 Jan 2024 (carlwitt): created
 */
package org.knime.base.node.preproc.regexsplit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.knime.base.node.preproc.regexsplit.OutputSettings.ColumnPrefixMode;
import org.knime.base.node.preproc.regexsplit.OutputSettings.OutputMode;
import org.knime.base.node.preproc.regexsplit.RegexSplitNodeSettings.CaseMatching;
import org.knime.core.data.DataTableSpec;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.webui.node.dialog.SettingsType;
import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.testing.node.dialog.DefaultNodeSettingsSnapshotTest;

/**
 * Test loading legacy settings.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
@SuppressWarnings("restriction") // Web UI is pending API
class RegexSplitNodeSettingsTest extends DefaultNodeSettingsSnapshotTest {

    protected RegexSplitNodeSettingsTest() {
        super(Map.of(SettingsType.MODEL, RegexSplitNodeSettings.class), new DataTableSpec());
    }

    /**
     * @param defaultValue set all boolean fields to this value
     * @return a node settings with regex split options in the legacy node settings format
     */
    private static NodeSettingsRO legacySettings(final boolean defaultValue) {
        final var settings = new NodeSettings("test");
        settings.addString("column", "col");
        settings.addString("pattern", "pattern");
        settings.addBoolean("isUnixLines", defaultValue);
        settings.addBoolean("isCaseInsensitive", defaultValue);
        settings.addBoolean("isComments", defaultValue);
        settings.addBoolean("isMultiLine", defaultValue);
        settings.addBoolean("isLiteral", defaultValue);
        settings.addBoolean("isDotAll", defaultValue);
        settings.addBoolean("isUniCodeCase", defaultValue);
        settings.addBoolean("isCanonEQ", defaultValue);
        return settings;
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void testBackwardsCompatibility(final boolean defaultValue) throws InvalidSettingsException {
        // given a legacy settings object
        final var legacy = legacySettings(defaultValue);
        // when we load it
        final var loaded = DefaultNodeSettings.loadSettings(legacy, RegexSplitNodeSettings.class);
        // then we get the expected values
        assertThat(loaded.m_column).as("read name of column to split").isEqualTo("col");
        assertThat(loaded.m_pattern).as("read pattern").isEqualTo("pattern");
        assertThat(loaded.m_isUnixLines).as("read isUnixLines").isEqualTo(defaultValue);
        assertThat(loaded.m_caseMatching).as("read isCaseInsensitive")
            .isEqualTo(defaultValue ? CaseMatching.CASEINSENSITIVE : CaseMatching.CASESENSITIVE);
        assertThat(loaded.m_isComments).as("read isComments").isEqualTo(defaultValue);
        assertThat(loaded.m_isMultiLine).as("read isMultiLine").isEqualTo(defaultValue);
        assertThat(loaded.m_isLiteral).as("read isLiteral").isEqualTo(defaultValue);
        assertThat(loaded.m_isDotAll).as("read isDotAll").isEqualTo(defaultValue);
        assertThat(loaded.m_isUnicodeCase).as("read isUniCodeCase").isEqualTo(defaultValue);
        assertThat(loaded.m_isCanonEQ).as("read isCanonEQ").isEqualTo(defaultValue);

        // legacy node output columns are named split_0, split_1, ... by default
        assertThat(loaded.m_output.m_mode).as("loading output from legacy settings").isEqualTo(OutputMode.COLUMNS);
        assertThat(loaded.m_output.m_columnPrefixMode).as("loading output column naming strategy from legacy settings")
            .isEqualTo(ColumnPrefixMode.CUSTOM);
        assertThat(loaded.m_output.m_columnPrefix).as("loading output columns prefix settings").isEqualTo("split_");
    }

}
