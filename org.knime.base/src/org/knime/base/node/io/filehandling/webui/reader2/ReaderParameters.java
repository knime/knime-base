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
package org.knime.base.node.io.filehandling.webui.reader2;

import java.net.URL;
import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.updates.util.BooleanReference;

/**
 * Common parameters for reader nodes. This class composes the extracted parameter classes.
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"javadoc", "restriction"})
public final class ReaderParameters implements NodeParameters {

    public ReaderParameters(final URL url) {
        m_singleFileReader = new SingleFileReaderParameters(url);
    }

    public ReaderParameters() {
        // default constructor
    }

    // Re-export for backwards compatibility - note: this is now reader-specific (CSV vs KnimeTable)
    // Keeping for backwards compatibility but should be migrated to reader-specific references
    @Deprecated
    public static class FirstColumnContainsRowIdsRef extends ReferenceStateProvider<Boolean>
        implements BooleanReference {
    }

    // Re-export for backwards compatibility
    @Deprecated
    public static class UseExistingRowIdWidgetRef implements Modification.Reference {
    }

    // Re-export for backwards compatibility
    public enum IfSchemaChangesOption {
            FAIL, USE_NEW_SCHEMA;

        public static IfSchemaChangesOption from(final MultiFileReaderParameters.IfSchemaChangesOption option) {
            return option == MultiFileReaderParameters.IfSchemaChangesOption.FAIL ? FAIL : USE_NEW_SCHEMA;
        }

        public MultiFileReaderParameters.IfSchemaChangesOption toNew() {
            return this == FAIL ? MultiFileReaderParameters.IfSchemaChangesOption.FAIL
                : MultiFileReaderParameters.IfSchemaChangesOption.USE_NEW_SCHEMA;
        }
    }

    // Re-export for backwards compatibility
    public enum HowToCombineColumnsOption {
            FAIL, UNION, INTERSECTION;

        public static HowToCombineColumnsOption from(final MultiFileReaderParameters.HowToCombineColumnsOption option) {
            switch (option) {
                case FAIL:
                    return FAIL;
                case UNION:
                    return UNION;
                case INTERSECTION:
                    return INTERSECTION;
                default:
                    throw new IllegalArgumentException("Unknown option: " + option);
            }
        }

        public MultiFileReaderParameters.HowToCombineColumnsOption toNew() {
            switch (this) {
                case FAIL:
                    return MultiFileReaderParameters.HowToCombineColumnsOption.FAIL;
                case UNION:
                    return MultiFileReaderParameters.HowToCombineColumnsOption.UNION;
                case INTERSECTION:
                    return MultiFileReaderParameters.HowToCombineColumnsOption.INTERSECTION;
                default:
                    throw new IllegalArgumentException("Unknown option: " + this);
            }
        }

        public ColumnFilterMode toColumnFilterMode() {
            return toNew().toColumnFilterMode();
        }
    }

    public SingleFileReaderParameters m_singleFileReader = new SingleFileReaderParameters();

    public SkipFirstDataRowsParameters m_skipFirstDataRowsParams = new SkipFirstDataRowsParameters();

    public MaxNumberOfRowsParameters m_maxNumberOfRowsParams = new MaxNumberOfRowsParameters();

    public MultiFileReaderParameters m_multiFileReaderParams = new MultiFileReaderParameters();

    // Backwards compatibility accessors
    public FileSelection m_source() {
        return m_singleFileReader.m_source;
    }

    public long m_skipFirstDataRows() {
        return m_skipFirstDataRowsParams.m_skipFirstDataRows;
    }

    public Optional<Long> m_maximumNumberOfRows() {
        return m_maxNumberOfRowsParams.m_maximumNumberOfRows;
    }

    public HowToCombineColumnsOption m_howToCombineColumns() {
        return HowToCombineColumnsOption.from(m_multiFileReaderParams.m_howToCombineColumns);
    }

    public Optional<String> m_appendPathColumn() {
        return m_multiFileReaderParams.m_appendPathColumn;
    }

    /**
     * @param config the config to save to
     */
    public void saveToConfig(final AbstractMultiTableReadConfig<?, ? extends DefaultTableReadConfig<?>, ?, ?> config) {
        final var tableReadConfig = config.getTableReadConfig();

        m_skipFirstDataRowsParams.saveToConfig(tableReadConfig);
        m_maxNumberOfRowsParams.saveToConfig(tableReadConfig);
        m_multiFileReaderParams.saveToConfig(config);
    }

    public void saveToSource(final FileSelectionPath sourceSettings) {
        m_singleFileReader.saveToSource(sourceSettings);
    }

    @Override
    public void validate() throws InvalidSettingsException {
        m_singleFileReader.validate();
        m_skipFirstDataRowsParams.validate();
        m_maxNumberOfRowsParams.validate();
        m_multiFileReaderParams.validate();
    }

}
