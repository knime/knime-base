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
 *   Jan 8, 2025 (david): created
 */
package org.knime.time.node.format;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.base.node.viz.format.AlignmentSuggestionOption;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;

/**
 * A utility class for transforming strings to HTML with alignment. This is used to create html formatters from string
 * formats.
 *
 * @author David Hickey, TNG Technology Consulting GmbH
 */
public final class StringToAlignedHTMLUtil {

    private StringToAlignedHTMLUtil() {
        // Utility class
    }

    private static final String CFG_KEY_ALIGNMENT = "alignment";

    /**
     * Transform a string to a span element with text-align
     *
     * @param plainText
     * @param alignment
     * @return a span with a set text-align style
     */
    public static String getHTML(final String plainText, final AlignmentSuggestionOption alignment) {

        return "<span style=\""//
            + "display:inline-block;width:100%;"//
            + "overflow:hidden;text-overflow:ellipsis;"//
            + String.format("%s\">%s</span>", //
                alignment.getCSSAttribute(), //
                plainText //
            );
    }

    /**
     * Save the alignment option to the config. This is to be used in the save method of formatters
     *
     * @param config
     * @param alignment
     */
    public static void saveAlignment(final ConfigBaseWO config, final AlignmentSuggestionOption alignment) {
        config.addString(CFG_KEY_ALIGNMENT, alignment.name());
    }

    /**
     * Load the alignment option from the config. This is to be used in formatter factories.
     *
     * @param config
     * @return the alignment
     * @throws InvalidSettingsException if the alignment is not a valid option
     */
    public static AlignmentSuggestionOption loadAlignment(final ConfigBaseRO config) throws InvalidSettingsException {
        final var alignmentAsString = config.getString(CFG_KEY_ALIGNMENT);
        try {
            return AlignmentSuggestionOption.valueOf(alignmentAsString);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException("Invalid alignment: '%s'. Valid alignments are: %s".formatted( //
                alignmentAsString, //
                Arrays.stream(AlignmentSuggestionOption.values()) //
                    .map(AlignmentSuggestionOption::name) //
                    .collect(Collectors.joining(", ")) //
            ), e);
        }

    }

}
