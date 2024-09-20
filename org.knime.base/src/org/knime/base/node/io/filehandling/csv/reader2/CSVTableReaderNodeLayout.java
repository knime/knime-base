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
 *   Mar 6, 2024 (marcbux): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.After;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Before;
import org.knime.core.webui.node.dialog.defaultdialog.layout.HorizontalLayout;
import org.knime.core.webui.node.dialog.defaultdialog.layout.Section;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"restriction", "java:S1214", "java:S103"})
public class CSVTableReaderNodeLayout {

    interface File {
        @After(CommonReaderLayout.File.Source.class)
        interface FileEncoding {
            String DESCRIPTION =
                """
                        Defines the character set used to read a CSV file that contains characters in a different encoding. You
                        can choose from a list of character encodings (UTF-8, UTF-16, etc.), or specify any other encoding
                        supported by your Java Virtual Machine (VM). The default value uses the default encoding of the Java VM,
                        which may depend on the locale or the Java property &quot;file.encoding&quot;.
                        """;

            String DESCRIPTION_DEFAULT = "Uses the default decoding set by the operating system.";

            String DESCRIPTION_ISO_8859_1 = "ISO Latin Alphabet No. 1, a.k.a. ISO-LATIN-1.";

            String DESCRIPTION_US_ASCII = "Seven-bit ASCII, also referred to as US-ASCII.";

            String DESCRIPTION_UTF_8 = "Eight-bit UCS Transformation Format.";

            String DESCRIPTION_UTF_16 =
                """
                        Sixteen-bit UCS Transformation Format, byte order identified by an optional byte-order mark in the file.
                        """;

            String DESCRIPTION_UTF_16BE = "Sixteen-bit UCS Transformation Format, big-endian byte order.";

            String DESCRIPTION_UTF_16LE = "Sixteen-bit UCS Transformation Format, little-endian byte order.";

            String DESCRIPTION_OTHER = "Enter a valid charset name supported by the Java Virtual Machine.";
        }

        @After(FileEncoding.class)
        interface CustomEncoding {
            String DESCRIPTION = "A custom character set used to read a CSV file.";
        }
    }

    @Section(title = "File Format")
    @After(CommonReaderLayout.File.class)
    interface FileFormat {
        interface SkipFirstLines {
            String DESCRIPTION =
                """
                        Use this option to skip lines that do not fit in the table structure (e.g. multi-line comments).
                        <br/>
                        The specified number of lines are skipped in the input file before the parsing starts. Skipping lines
                        prevents parallel reading of individual files.
                        """;
        }

        @After(SkipFirstLines.class)
        interface CommentLineCharacter {
            String DESCRIPTION = "Defines the character indicating line comments.";
        }

        @After(CommentLineCharacter.class)
        interface RowDelimiter {
            String DESCRIPTION = "Defines the character string delimiting rows. Can get detected automatically.";

            String DESCRIPTION_LINE_BREAK =
                "Uses the line break character as row delimiter. This option is platform-agnostic.";

            String DESCRIPTION_CUSTOM = "Uses the provided string as row delimiter.";
        }

        @After(RowDelimiter.class)
        interface CustomRowDelimiter {
            String DESCRIPTION = "Defines the character to be used as custom row delimiter.";
        }

        @After(CustomRowDelimiter.class)
        interface ColumnDelimiter {
            String DESCRIPTION = """
                    Defines the character string delimiting columns. Use '\\t' for tab characters. Can get detected
                    automatically.
                    """;
        }

        @After(ColumnDelimiter.class)
        interface QuotedStringsContainNoRowDelimiters {
            String DESCRIPTION = """
                    Check this box if there are no quotes that contain row delimiters inside the files. Row delimiters
                    should not be inside of quotes for parallel reading of individual files.
                    """;
        }

        @After(QuotedStringsContainNoRowDelimiters.class)
        @HorizontalLayout
        interface QuoteCharacters {
            interface QuoteCharacter {
                String DESCRIPTION = "The character indicating quotes. Can get detected automatically.";
            }

            @After(QuoteCharacter.class)
            interface QuoteEscapeCharacter {
                String DESCRIPTION = """
                        The character used for escaping quotes inside an already quoted value. Can get detected
                        automatically.
                        """;
            }
        }

        @After(QuoteCharacters.class)
        interface AutodetectFormat {
            String DESCRIPTION =
                """
                        By pressing this button, the format of the file will be guessed automatically. It is not guaranteed that
                        the correct values are being detected.
                        """;
        }

        @After(AutodetectFormat.class)
        interface NumberOfCharactersForAutodetection {
            String DESCRIPTION = """
                    Specifies on how many characters of the selected file should be used for autodetection. The
                    autodetection by default is based on the first 1024 * 1024 characters.
                    """;
        }
    }

    interface DataArea {

        @Before(CommonReaderLayout.DataArea.SkipFirstDataRows.class)
        interface FirstRowContainsColumnNames {
            String DESCRIPTION = "Select this box if the first row contains column name headers.";
        }

        @After(CommonReaderLayout.DataArea.UseExistingRowId.class)
        interface IfRowHasLessColumns {
            String DESCRIPTION = "Specifies the behavior in case some rows are shorter than others. ";

            String DESCRIPTION_INSERT_MISSING = "the shorter rows are completed with missing values.";

            String DESCRIPTION_FAIL = "if there are shorter rows in the input file the node execution fails.";
        }
    }

    @Section(title = "Values")
    @After(CommonReaderLayout.DataArea.class)
    @Before(CommonReaderLayout.ColumnAndDataTypeDetection.class)
    interface Values {
        interface DecimalSeparator {
            String DESCRIPTION =
                """
                        Specifies the decimal separator character for parsing numbers. The decimal separator is only used for
                        the parsing of double values. Note that the decimal separator must differ from the thousands separator.
                        You must always provide a decimal separator.
                        """;
        }

        @After(DecimalSeparator.class)
        interface ThousandsSeparator {
            String DESCRIPTION = """
                    Specifies the thousands separator character for parsing numbers. The thousands separator is used for
                    integer, long and double parsing. Note that the thousands separator must differ from the decimal
                    separator. It is possible to leave the thousands separator unspecified.
                    """;
        }

        @After(ThousandsSeparator.class)
        interface ReplaceEmptyQuotedStringsByMissingValues {
            String DESCRIPTION =
                "Select this box if you want <b>quoted</b> empty strings to be replaced by missing value cells.";
        }

        @After(ReplaceEmptyQuotedStringsByMissingValues.class)
        interface QuotedStrings {
            String DESCRIPTION = "Specifies the behavior in case there are quoted strings in the input table.";

            String DESCRIPTION_REMOVE_QUOTES_AND_TRIM =
                "Quotes will be removed from the value followed by trimming any leading/trailing whitespaces.";

            String DESCRIPTION_KEEP_QUOTES =
                "Quotes of a value will be kept. Note: No trimming will be done inside the quotes.";
        }
    }

    interface ColumnAndDataTypeDetection {

        @Before(CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.class)
        interface LimitScannedRows {
            String DESCRIPTION =
                """
                        If enabled, only the specified number of input <i>rows</i> are used to analyze the file (i.e to
                        determine the column types). This option is recommended for long files where the first <i>n</i> rows are
                        representative for the whole file. The "Skip first data rows" option has no effect on the scanning. Note
                        also, that this option and the "Limit number of rows" option are independent from each other, i.e., if
                        the value in "Limit number of rows" is smaller than the value specified here, we will still read as many
                        rows as specified here.
                        """;
        }

        @After(CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.class)
        interface MaximumNumberOfColumns {
            String DESCRIPTION =
                """
                        Sets the number of allowed columns (default 8192 columns) to prevent memory exhaustion. The node will
                        fail if the number of columns exceeds the set limit.
                        """;
        }

        @After(MaximumNumberOfColumns.class)
        interface LimitMemoryPerColumn {
            String DESCRIPTION =
                """
                        If selected the memory per column is restricted to 1MB in order to prevent memory exhaustion. Uncheck
                        this option to disable these memory restrictions.
                        """;
        }
    }

    interface MulitpleFileHandling {
        @After(CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.class)
        @Before(CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.class)
        interface PrependFileIndexToRowId {
            String DESCRIPTION =
                """
                        Select this box if you want to prepend a prefix that depends on the index of the source file to the
                        RowIDs. The prefix for the first file is "File_0_", for the second "File_1_" and so on. This option is
                        useful if the RowIDs within a single file are unique but the same RowIDs appear in multiple files.
                        Prepending the file index prevents parallel reading of individual files.
                        """;
        }

    }
}
