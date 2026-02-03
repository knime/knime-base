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
 *   14 Jan 2026 (Rupert Ettrich): created
 */
package org.knime.base.node.io.filehandling.webui;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.persistence.NodeParametersPersistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.Effect.EffectType;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.EnumChoice;
import org.knime.node.parameters.widget.choices.EnumChoicesProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.TextInputWidgetValidation.PatternValidation.IsNotBlankValidation;

/**
 * File encoding parameters that are commonly used by writer or reader nodes.
 *
 * @author Rupert Ettrich
 * @since 5.10
 */
@SuppressWarnings("restriction")
public class FileEncodingParameters implements NodeParameters {

    /**
     * Empty constructor for serialization
     */
    public FileEncodingParameters() {
    }

    /**
     * A constructor for file encoding parameters.
     *
     * @param fileEncoding the file encoding option
     * @param customEncoding the custom encoding if {@link FileEncodingOption#OTHER} is used
     */
    public FileEncodingParameters(final FileEncodingOption fileEncoding, final String customEncoding) {
        m_fileEncoding = fileEncoding;
        m_customEncoding = customEncoding;
    }

    /**
     * Options for file encoding.
     */
    @SuppressWarnings("javadoc")
    public enum FileEncodingOption {
            @Label(value = "OS default", description = "Uses the default decoding set by the operating system.")
            DEFAULT(null, "OS default (" + java.nio.charset.Charset.defaultCharset().name() + ")"),
            @Label(value = "ISO-8859-1", description = "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.")
            ISO_8859_1("ISO-8859-1"),
            @Label(value = "US-ASCII", description = "Seven-bit ASCII, also referred to as US-ASCII.")
            US_ASCII("US-ASCII"), //
            @Label(value = "UTF-8", description = "Eight-bit UCS Transformation Format.")
            UTF_8("UTF-8"),
            @Label(value = "UTF-16", description = "Sixteen-bit UCS Transformation Format, byte order identified by "
                + "an optional byte-order mark in the file.")
            UTF_16("UTF-16"),
            @Label(value = "UTF-16BE", description = "Sixteen-bit UCS Transformation Format, big-endian byte order.")
            UTF_16BE("UTF-16BE"),
            @Label(value = "UTF-16LE", description = "Sixteen-bit UCS Transformation Format, little-endian byte order.")
            UTF_16LE("UTF-16LE"),
            @Label(value = "Other", description = "Enter a valid charset name supported by the Java Virtual Machine.")
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

        /**
         * Returns the enum value for the given charset name or {@link FileEncodingOption#OTHER} if it doesn't exist.
         *
         * @param charsetName the charset name
         * @return a matching file encoding option
         */
        public static FileEncodingOption fromCharsetName(final String charsetName) {
            return Arrays.stream(FileEncodingOption.values())
                .filter(fileEncoding -> Objects.equals(fileEncoding.m_charsetName, charsetName)).findFirst()
                .orElse(OTHER);
        }

        /**
         * @return the charset name
         */
        public String getCharsetName() {
            return m_charsetName;
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

    /**
     * The reference state provider for the file encoding.
     */
    public static class FileEncodingRef extends ReferenceStateProvider<FileEncodingOption> {
    }

    interface FileEncodingOptionModRef extends ParameterReference<FileEncodingOption>, Modification.Reference {
    }

    /**
     * The selected file encoding option.
     */
    @Widget(title = "File encoding", description = """
            Defines the character set used to read or write a file that \
            contains characters in a different encoding. You can choose from \
            a list of character encodings (UTF-8, UTF-16, etc.), or specify \
            any other encoding supported by your Java Virtual Machine (VM). \
            The default value uses the default encoding of the Java VM, \
            which may depend on the locale or the Java property \
            &quot;file.encoding&quot;.
            """, advanced = true)
    @Modification.WidgetReference(FileEncodingOptionModRef.class)
    @ValueReference(FileEncodingRef.class)
    @ChoicesProvider(EncodingChoicesProvider.class)
    protected FileEncodingOption m_fileEncoding = FileEncodingOption.UTF_8;

    /**
     * Generalizes the description of the file encoding option widget to general input file types.
     *
     * @param group the widget group modifier
     * @since 5.11
     */
    public static void generalizeFileEncodingDescription(final Modification.WidgetGroupModifier group) {
        group.find(FileEncodingOptionModRef.class).modifyAnnotation(Widget.class).withProperty("description", """
                Defines the character set which is used to read files that contain characters in a different encoding.
                You can choose from a list of character encodings (UTF-8, UTF-16, etc.), or specify any other encoding
                supported by your Java Virtual Machine (VM). The default value uses the default encoding of the Java VM,
                 which may depend on the locale or the Java property &quot;file.encoding&quot;.
                """).modify();
    }

    /**
     * The effect predicate provider that checks whether the selected encoding is custom.
     *
     * @author Magnus Gohm, KNIME GmbH, Konstanz, Germany
     * @since 5.11
     */
    public static final class IsOtherEncoding implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(FileEncodingRef.class).isOneOf(FileEncodingOption.OTHER);
        }
    }

    /**
     * The reference state provider for the custom encoding.
     */
    public static class CustomEncodingRef extends ReferenceStateProvider<String> {
    }

    interface CustomEncodingModRef extends ParameterReference<String>, Modification.Reference {
    }

    /**
     * If {@link FileEncodingOption#OTHER } is selected, this field is used to contain the custom file encoding.
     */
    @Widget(title = "Custom encoding", description = "A custom character set used to read a CSV file.", advanced = true)
    @ValueReference(CustomEncodingRef.class)
    @Modification.WidgetReference(CustomEncodingModRef.class)
    @Effect(predicate = IsOtherEncoding.class, type = EffectType.SHOW)
    @TextInputWidget(patternValidation = IsNotBlankValidation.class)
    protected String m_customEncoding = "";

    /**
     * Changes the effect predicate provider of the custom encoding field.
     *
     * @param group the widget group modifier
     * @param predicateProviderClass the new predicate provider class
     * @since 5.11
     */
    public static void changeEffectPredicateProviderOfCustomEncoding(final Modification.WidgetGroupModifier group,
        final Class<? extends EffectPredicateProvider> predicateProviderClass) {
        if (predicateProviderClass != null) {
            group.find(CustomEncodingModRef.class).modifyAnnotation(Effect.class)
                .withProperty("predicate", predicateProviderClass).modify();
        }
    }

    /**
     * Returns the charset name of the selected encoding.
     *
     * @param encoding the selected encoding option
     * @param customEncoding the custom encoding if {@link FileEncodingOption#OTHER } is selected
     * @return the encoding as a string
     */
    public static String fileEncodingToCharsetName(final FileEncodingOption encoding, final String customEncoding) {
        return encoding == FileEncodingOption.OTHER ? customEncoding : encoding.m_charsetName;
    }

    /**
     * @return the fileEncoding
     */
    public FileEncodingOption getFileEncoding() {
        return m_fileEncoding;
    }

    /**
     * @param fileEncoding the fileEncoding to set
     */
    public void setFileEncoding(final FileEncodingOption fileEncoding) {
        m_fileEncoding = fileEncoding;
    }

    /**
     * @return the customEncoding
     */
    public String getCustomEncoding() {
        return m_customEncoding;
    }

    /**
     * @param customEncoding the customEncoding to set
     */
    public void setCustomEncoding(final String customEncoding) {
        m_customEncoding = customEncoding;
    }


    /**
     * Abstract persistor for file encoding parameters.
     *
     * @author Tim Crundall
     * @since 5.11
     */
    public abstract static class AbstractFileEncodingPersistor
        implements NodeParametersPersistor<FileEncodingParameters> {

        private final String m_cfgCharacterSetName;

        private static final String ASCII_CHARSET_NAME = StandardCharsets.US_ASCII.name();

        /**
         * @param cfgCharacterSetKey the configuration key to use
         */
        protected AbstractFileEncodingPersistor(final String cfgCharacterSetKey) {
            this.m_cfgCharacterSetName = cfgCharacterSetKey;
        }

        @Override
        public FileEncodingParameters load(final NodeSettingsRO settings) throws InvalidSettingsException {
            final var charsetName = settings.getString(m_cfgCharacterSetName, ASCII_CHARSET_NAME);
            final var fileEncoding = FileEncodingOption.fromCharsetName(charsetName);
            return new FileEncodingParameters(fileEncoding,
                fileEncoding == FileEncodingOption.OTHER ? charsetName : null);
        }

        @Override
        public void save(final FileEncodingParameters param, final NodeSettingsWO settings) {
            final var fileEncoding = param.getFileEncoding();
            settings.addString(m_cfgCharacterSetName,
                fileEncoding == FileEncodingOption.OTHER ? param.getCustomEncoding() : fileEncoding.getCharsetName());
        }

        @Override
        public String[][] getConfigPaths() {
            return new String[][]{{m_cfgCharacterSetName}};
        }
    }

    /**
     * Persistor for file encoding parameters.
     *
     * @since 5.11
     */
    public static final class FileEncodingPersistor extends AbstractFileEncodingPersistor {

        /**
         * Constructor
         */
        public FileEncodingPersistor() {
            super("characterSetName");
        }

    }
}
