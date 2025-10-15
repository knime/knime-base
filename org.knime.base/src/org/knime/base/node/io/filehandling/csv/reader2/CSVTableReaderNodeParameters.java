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
 *   Oct 16, 2025 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.knime.base.node.io.filehandling.csv.reader.CSVMultiTableReadConfig;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.CustomEncoding;
import org.knime.base.node.io.filehandling.csv.reader2.CSVTableReaderNodeLayout.File.FileEncoding;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;

/**
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@Modification({CSVTableReaderNodeParameters.SetCSVExtensions.class})
public class CSVTableReaderNodeParameters extends CommonTableReaderNodeParameters {

    static final class SetCSVExtensions extends CommonTableReaderNodeParameters.SetFileReaderWidgetExtensions {

        @Override
        protected String[] getExtensions() {
            return new String[]{"csv", "tsv", "txt", "gz"};
        }

    }

    enum FileEncodingOption {
            @Label(value = "OS default", description = FileEncoding.DESCRIPTION_DEFAULT) //
            DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
            @Label(value = "ISO-8859-1", description = FileEncoding.DESCRIPTION_ISO_8859_1) //
            ISO_8859_1("ISO-8859-1"), //
            @Label(value = "US-ASCII", description = FileEncoding.DESCRIPTION_US_ASCII) //
            US_ASCII("US-ASCII"), //
            @Label(value = "UTF-8", description = FileEncoding.DESCRIPTION_UTF_8) //
            UTF_8("UTF-8"), //
            @Label(value = "UTF-16", description = FileEncoding.DESCRIPTION_UTF_16) //
            UTF_16("UTF-16"), //
            @Label(value = "UTF-16BE", description = FileEncoding.DESCRIPTION_UTF_16BE) //
            UTF_16BE("UTF-16BE"), //
            @Label(value = "UTF-16LE", description = FileEncoding.DESCRIPTION_UTF_16LE) //
            UTF_16LE("UTF-16LE"), //
            @Label(value = "Other", description = FileEncoding.DESCRIPTION_OTHER) //
            OTHER("");

        final String m_charsetName;

        final String m_nonConstantDisplayText;

        FileEncodingOption(final String persistId) {
            this(persistId, null);
        }

        FileEncodingOption(final String charsetName, final String nonConstantDisplayText) {
            m_charsetName = charsetName;
            m_nonConstantDisplayText = nonConstantDisplayText;
        }

        static FileEncodingOption fromCharsetName(final String charsetName) {
            return Arrays.stream(FileEncodingOption.values())
                .filter(fileEncoding -> Objects.equals(fileEncoding.m_charsetName, charsetName)).findFirst()
                .orElse(OTHER);
        }

        EnumChoice<FileEncodingOption> toEnumChoice() {
            if (m_nonConstantDisplayText == null) {
                return EnumChoice.fromEnumConst(this);
            }
            return new EnumChoice<>(this, m_nonConstantDisplayText);
        }
    }

    /**
     * This provider is needed to display the non-constant display text of the default option.
     */
    static final class EncodingChoicesProvider implements EnumChoicesProvider<FileEncodingOption> {

        @Override
        public List<EnumChoice<FileEncodingOption>> computeState(final NodeParametersInput context) {
            return Arrays.stream(FileEncodingOption.values()).map(FileEncodingOption::toEnumChoice).toList();
        }
    }

    static class FileEncodingRef extends ReferenceStateProvider<FileEncodingOption> {
    }

    static final class IsOtherEncoding implements EffectPredicateProvider {

        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileEncodingRef.class).isOneOf(FileEncodingOption.OTHER);
        }

    }

    @Widget(title = "File encoding", description = FileEncoding.DESCRIPTION, advanced = true)
    @ValueReference(FileEncodingRef.class)
    @ChoicesProvider(EncodingChoicesProvider.class)
    FileEncodingOption m_fileEncoding = FileEncodingOption.OTHER;

    @Widget(title = "Custom encoding", description = CustomEncoding.DESCRIPTION, advanced = true)
    @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
    String m_customEncoding = "foobar";

    void loadFromConfig(final CSVMultiTableReadConfig config) {
        final var tableReadConfig = config.getTableReadConfig();
        final var csvConfig = tableReadConfig.getReaderSpecificConfig();

        final var charset = csvConfig.getCharSetName();
        m_fileEncoding = FileEncodingOption.fromCharsetName(charset);
        m_customEncoding = m_fileEncoding == FileEncodingOption.OTHER ? charset : "";
    }

    void saveToConfig(final CSVMultiTableReadConfig config) {
        final var tableReadConfig = config.getTableReadConfig();
        final var csvConfig = tableReadConfig.getReaderSpecificConfig();

        csvConfig.setCharSetName(
            m_fileEncoding == FileEncodingOption.OTHER ? m_customEncoding : m_fileEncoding.m_charsetName);
    }

}
