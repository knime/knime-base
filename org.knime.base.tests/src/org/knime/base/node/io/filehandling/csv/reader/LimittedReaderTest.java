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
 *   May 15, 2023 (adrian.nembach): created
 */
package org.knime.base.node.io.filehandling.csv.reader;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;

import org.junit.jupiter.api.Test;

/**
 * Tests the {@link LimittedReader}.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("static-method")
final class LimittedReaderTest {

    private static final Charset CHARSET = Charset.forName("utf-8");

    @Test
    void testReadDistantFromBoundary() throws Exception {
        var textUntilSeparator = "This is a text where exclamation mark is the end sequence!";
        var text = textUntilSeparator +
            " The reader should stop after if it occurs after the boundary.";

        var textDistantFromBoundary = "This is a text";

        var lenUntilEndSequence = CHARSET.encode(textUntilSeparator).limit();

        var encoded = CHARSET.encode(text);

        try (var reader =
            new LimittedReader(new ByteArrayInputStream(encoded.array()), CHARSET, "!", lenUntilEndSequence - 8)) {
            char[] expectedCharArray = textDistantFromBoundary.toCharArray();
            var cbuf = new char[expectedCharArray.length];
            reader.read(cbuf, 0, cbuf.length);
            assertArrayEquals(expectedCharArray, cbuf, "The read char array has unexpected content.");
        }
    }

    @Test
    void testCloseToBoundary() throws Exception {
        var textUntilSeparator = "This is a text where exclamation mark is the end sequence!";
        var text = textUntilSeparator +
            " The reader should stop after if it occurs after the boundary.";

        var lenUntilEndSequence = CHARSET.encode(textUntilSeparator).limit();

        var encoded = CHARSET.encode(text);

        try (var reader =
            new LimittedReader(new ByteArrayInputStream(encoded.array()), CHARSET, "!", lenUntilEndSequence - 8)) {
            char[] expectedCharArray = textUntilSeparator.toCharArray();
            var cbuf = new char[expectedCharArray.length];
            reader.read(cbuf, 0, cbuf.length);
            assertArrayEquals(expectedCharArray, cbuf, "The read char array has unexpected content.");
        }
    }

}
