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
 *   Nov 24, 2025 (Paul Bärnreuther): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.knime.base.node.io.filehandling.csv.reader.api.CSVTableReaderConfig;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * Parameters for file encoding settings.
 *
 * @author Paul Bärnreuther
 */
public final class FileEncodingParameters implements NodeParameters {

    /**
     * Options for file encoding.
     */
    public enum FileEncodingOption {
            @Label(value = "OS default", description = "Uses the default decoding set by the operating system.") //
            DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"), //
            @Label(value = "ISO-8859-1", description = "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.") //
            ISO_8859_1("ISO-8859-1"), //
            @Label(value = "US-ASCII", description = "Seven-bit ASCII, also referred to as US-ASCII.") //
            US_ASCII("US-ASCII"), //
            @Label(value = "UTF-8", description = "Eight-bit UCS Transformation Format.") //
            UTF_8("UTF-8"), //
            @Label(value = "UTF-16",
                description = "Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark in the file.") //
            UTF_16("UTF-16"), //
            @Label(value = "UTF-16BE",
                description = "Sixteen-bit UCS Transformation Format, big-endian byte order.") //
            UTF_16BE("UTF-16BE"), //
            @Label(value = "UTF-16LE",
                description = "Sixteen-bit UCS Transformation Format, little-endian byte order.") //
            UTF_16LE("UTF-16LE"), //
            @Label(value = "Other",
                description = "Enter a valid charset name supported by the Java Virtual Machine.") //
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

    @Widget(title = "File encoding", description = """
            Defines the character set used to read a CSV file that contains characters in a different encoding. You \
            can choose from a list of character encodings (UTF-8, UTF-16, etc.), or specify any other encoding \
            supported by your Java Virtual Machine (VM). The default value uses the default encoding of the Java VM, \
            which may depend on the locale or the Java property &quot;file.encoding&quot;.
            """, advanced = true)
    @ValueReference(FileEncodingRef.class)
    @ChoicesProvider(EncodingChoicesProvider.class)
    @Layout(CSVTableReaderLayoutAdditions.File.FileEncoding.class)
    FileEncodingOption m_fileEncoding = FileEncodingOption.UTF_8;

    static final class IsOtherEncoding implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileEncodingRef.class).isOneOf(FileEncodingOption.OTHER);
        }
    }

    static class CustomEncodingRef extends ReferenceStateProvider<String> {
    }

    @Widget(title = "Custom encoding", description = "A custom character set used to read a CSV file.", advanced = true)
    @ValueReference(CustomEncodingRef.class)
    @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
    @Layout(CSVTableReaderLayoutAdditions.File.FileEncoding.class)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    String m_customEncoding = "";

    static String fileEncodingToCharsetName(final FileEncodingOption encoding, final String customEncoding) {
        return encoding == FileEncodingOption.OTHER ? customEncoding : encoding.m_charsetName;
    }

    /**
     * Save the settings to the given config.
     *
     * @param csvConfig the config to save to
     */
    public void saveToConfig(final CSVTableReaderConfig csvConfig) {
        csvConfig.setCharSetName(fileEncodingToCharsetName(m_fileEncoding, m_customEncoding));
    }

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_fileEncoding == FileEncodingOption.OTHER && m_customEncoding.isBlank()) {
            throw new InvalidSettingsException("The specified custom encoding must not be empty.");
        }
    }
}
