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
 *   Dec 11, 2023 (Marc Bux, KNIME GmbH, Berlin, Germany): created
 */
package org.knime.base.node.io.filehandling.csv.reader2;

import org.knime.core.webui.node.dialog.defaultdialog.DefaultNodeSettings;
import org.knime.core.webui.node.dialog.defaultdialog.layout.WidgetGroup;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.NodeSettingsPreserverPersistor;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.PersistableSettings;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.field.Persist;
import org.knime.core.webui.node.dialog.defaultdialog.setting.filechooser.FileChooser;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Widget;
import org.knime.filehandling.core.defaultnodesettings.filechooser.reader.SettingsModelReaderFileChooser;

/**
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"restriction", "java:S3052"})
public final class CSVTableReaderNodeSettings implements DefaultNodeSettings {

    private static final String FILE_CHOOSER_DESCRIPTION = """
            Select a file system which stores the data you want to read. There are
            three default file system options to choose from:
            <br />
            <ul>
                <li><i>Local File System:</i> Allows you to select a file from your local file system.
                </li>
                <li><i>Custom/KNIME URL:</i> Allows to specify a URL (e.g. file://, http:// or knime:// protocol).
                    Browsing is disabled for this option.
                </li>
                <li><i>Relative to current Hub space:</i> Allows to select a file relative to the Hub space on which
                    the workflow is run.
                </li>
            </ul>
            """;

    @Persist(configKey = "settings")
    Settings m_settings = new Settings();

    @Persist(configKey = "advanced_settings")
    AdvancedSettings m_advancedSettings = new AdvancedSettings();

    @Persist(configKey = "limit_rows")
    LimitRows m_limitRows = new LimitRows();

    @Persist(configKey = "encoding")
    Encoding m_encoding = new Encoding();

    @Persist(configKey = "table_spec_config", hidden = true, customPersistor = NodeSettingsPreserverPersistor.class)
    Void m_tableSpecConfig;

    static class Settings implements WidgetGroup, PersistableSettings {

        @Widget(title = "Read from", description = FILE_CHOOSER_DESCRIPTION)
        @Persist(configKey = "file_selection", settingsModel = SettingsModelReaderFileChooser.class)
        FileChooser m_path = new FileChooser();

        @Persist(configKey = "file_selection", hidden = true)
        FileSelectionInternal m_fileSelectionInternal = new FileSelectionInternal();

        @Persist(configKey = "has_column_header")
        boolean m_hasColumnHeader = true;

        @Persist(configKey = "has_row_id")
        boolean m_hasRowId;

        @Persist(configKey = "support_short_data_rows")
        boolean m_supportShortDataRows;

        @Persist(configKey = "skip_empty_data_rows")
        boolean m_skipEmptyDataRows;

        @Persist(configKey = "prepend_file_idx_to_row_id")
        boolean m_prependFileIdxToRowId;

        @Persist(configKey = "comment_char")
        String m_commentChar = "#";

        @Persist(configKey = "column_delimiter")
        String m_columnDelimiter = ",";

        @Persist(configKey = "quote_char")
        String m_quoteChar = "\"";

        @Persist(configKey = "quote_escape_char")
        String m_quoteEscapeChar = "\"";

        @Persist(configKey = "use_line_break_row_delimiter")
        boolean m_useLineBreakRowDelimiter = true;

        @Persist(configKey = "row_delimiter")
        String m_rowDelimiter = "\n";

        @Persist(configKey = "autodetect_buffer_size")
        int m_autodetectBufferSize = 1048576;

        static class FileSelection implements WidgetGroup, PersistableSettings {

            @Persist(configKey = "filter_mode")
            FilterMode m_filterMode = new FilterMode();

            @Persist(configKey = "file_system_chooser__Internals")
            FileSystemChooserInternal m_fileSystemChooserInternals = new FileSystemChooserInternal();

            static class FilterMode implements WidgetGroup, PersistableSettings {

                @Persist(configKey = "filter_mode")
                String m_filterMode = "FILE";

                @Persist(configKey = "include_subfolders")
                boolean m_includeSubfolders;

                @Persist(configKey = "filter_options")
                FilterOptions m_filterOptions = new FilterOptions();

                static class FilterOptions implements WidgetGroup, PersistableSettings {

                    @Persist(configKey = "filter_files_extension")
                    boolean m_filterFilesExtension;

                    @Persist(configKey = "files_extension_expression")
                    String m_filesExtensionExpression = "";

                    @Persist(configKey = "files_extension_case_sensitive")
                    boolean m_filesExtensionCaseSensitive;

                    @Persist(configKey = "filter_files_name")
                    boolean m_filterFilesName;

                    @Persist(configKey = "files_name_expression")
                    String m_filesNameExpression = "*";

                    @Persist(configKey = "files_name_case_sensitive")
                    boolean m_filesNameCaseSensitive;

                    @Persist(configKey = "files_name_filter_type")
                    String m_filesNameFilterType = "WILDCARD";

                    @Persist(configKey = "include_hidden_files")
                    boolean m_includeHiddenFiles;

                    @Persist(configKey = "include_special_files")
                    boolean m_includeSpecialFiles = true;

                    @Persist(configKey = "filter_folders_name")
                    boolean m_filterFoldersName;

                    @Persist(configKey = "folders_name_expression")
                    String m_foldersNameExpression = "*";

                    @Persist(configKey = "folders_name_case_sensitive")
                    boolean m_foldersNameCaseSensitive;

                    @Persist(configKey = "folders_name_filter_type")
                    String m_foldersNameFilterType = "WILDCARD";

                    @Persist(configKey = "include_hidden_folders")
                    boolean m_includeHiddenFolders;

                    @Persist(configKey = "follow_links")
                    boolean m_followLinks = true;

                }
            }

            static class FileSystemChooserInternal implements WidgetGroup, PersistableSettings {

                @Persist(configKey = "has_fs_port")
                boolean m_hasFsPort;

                @Persist(configKey = "overwritten_by_variable")
                boolean m_overwrittenByVariable;

                @Persist(configKey = "convenience_fs_category")
                String m_convenienceFsCategory = "RELATIVE";

                @Persist(configKey = "relative_to")
                String m_relativeTo = "knime.workflow";

                @Persist(configKey = "mountpoint")
                String m_mountpoint = "LOCAL";

                @Persist(configKey = "spaceId")
                String m_spaceId = "";

                @Persist(configKey = "spaceName")
                String m_spaceName = "";

                @Persist(configKey = "custom_url_timeout")
                int m_customUrlTimeout = 1000;

                @Persist(configKey = "connected_fs")
                boolean m_connectedFs = true;
            }

        }

        static class FileSelectionInternal implements WidgetGroup, PersistableSettings {

            @Persist(configKey = "SettingsModelID")
            String m_settingsModelID = "SMID_ReaderFileChooser";

            @Persist(configKey = "EnabledStatus")
            boolean m_enabledStatus = true;
        }

    }

    static class AdvancedSettings implements WidgetGroup, PersistableSettings {

        @Persist(configKey = "spec_merge_mode", hidden = true)
        String m_specMergeMode = "UNION";

        @Persist(configKey = "fail_on_differing_specs")
        boolean m_failOnDifferingSpecs = true;

        @Persist(configKey = "append_path_column", hidden = true)
        boolean m_appendPathColumn;

        @Persist(configKey = "path_column_name", hidden = true)
        String m_pathColumnName = "Path";

        @Persist(configKey = "limit_data_rows_scanned")
        boolean m_limitDataRowsScanned = true;

        @Persist(configKey = "max_data_rows_scanned")
        long m_maxDataRowsScanned = 10000;

        @Persist(configKey = "save_table_spec_config", hidden = true)
        boolean m_saveTableSpecConfig = true;

        @Persist(configKey = "limit_memory_per_column")
        boolean m_limitMemoryPerColumn = true;

        @Persist(configKey = "maximum_number_of_columns")
        int m_maximumNumberOfColumns = 8192;

        @Persist(configKey = "quote_option")
        String m_quoteOption = "REMOVE_QUOTES_AND_TRIM";

        @Persist(configKey = "replace_empty_quotes_with_missing")
        boolean m_replaceEmptyQuotesWithMissing = true;

        @Persist(configKey = "no_row_delimiters_in_quotes")
        boolean m_noRowDelimitersInQuotes;

        @Persist(configKey = "min_chunk_size_in_bytes")
        long m_minChunkSizeInBytes = 67108864;

        @Persist(configKey = "max_num_chunks_per_file")
        int m_maxNumChunksPerFile = 8;

        @Persist(configKey = "thousands_separator")
        String m_thousandsSeparator = " ";

        @Persist(configKey = "decimal_separator")
        String m_decimalSeparator = ".";

    }

    static class LimitRows implements WidgetGroup, PersistableSettings {

        @Persist(configKey = "skip_lines")
        boolean m_skipLines;

        @Persist(configKey = "number_of_lines_to_skip")
        long m_numberOfLinesToSkip = 1;

        @Persist(configKey = "skip_data_rows")
        boolean m_skipDataRows;

        @Persist(configKey = "number_of_rows_to_skip")
        long m_numberOfRowsToSkip = 1;

        @Persist(configKey = "limit_data_rows")
        boolean m_limitDataRows;

        @Persist(configKey = "max_rows")
        long m_maxRows = 50;

    }

    static class Encoding implements WidgetGroup, PersistableSettings {

        @Persist(configKey = "charset")
        String m_charset = "UTF-8";

    }

}
