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
package org.knime.base.node.io.filehandling.csv.reader2.common;

import java.util.Optional;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.ReferenceStateProvider;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.LimitNumberOfRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.SkipFirstDataRows;
import org.knime.base.node.io.filehandling.webui.reader.CommonReaderLayout.DataArea.UseExistingRowId;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileReaderWidget;
import org.knime.core.webui.node.dialog.defaultdialog.internal.file.FileSelection;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.SpecMergeMode;
import org.knime.filehandling.core.node.table.reader.config.AbstractMultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.DefaultTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.updates.EffectPredicate;
import org.knime.node.parameters.updates.EffectPredicateProvider;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.OptionalWidget;
import org.knime.node.parameters.widget.OptionalWidget.DefaultValueProvider;
import org.knime.node.parameters.widget.choices.Label;
import org.knime.node.parameters.widget.choices.RadioButtonsWidget;
import org.knime.node.parameters.widget.choices.ValueSwitchWidget;
import org.knime.node.parameters.widget.message.TextMessage;
import org.knime.node.parameters.widget.message.TextMessage.MessageType;
import org.knime.node.parameters.widget.message.TextMessage.SimpleTextMessageProvider;
import org.knime.node.parameters.widget.number.NumberInputWidget;
import org.knime.node.parameters.widget.number.NumberInputWidgetValidation.MinValidation.IsNonNegativeValidation;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;

/**
 *
 * @author Marc Bux, KNIME GmbH, Berlin, Germany
 */
@SuppressWarnings({"javadoc", "restriction"})
public final class CommonTableReaderNodeParameters implements NodeParameters {

    public static final class FileSelectionRef extends ReferenceStateProvider<FileSelection>
        implements Modification.Reference {
    }

    /**
     * Set the file extensions for the file reader widget using {@link Modification} on the implementation of this class
     * or the field where it is used.
     */
    public abstract static class SetFileReaderWidgetExtensions implements Modification.Modifier {
        @Override
        public void modify(final Modification.WidgetGroupModifier group) {
            group.find(FileSelectionRef.class).modifyAnnotation(FileReaderWidget.class)
                .withProperty("fileExtensions", getExtensions()).modify();
        }

        /**
         * @return the the valid extensions by which the browsable files should be filtered
         */
        protected abstract String[] getExtensions();
    }

    static final class FileSystemManagedByPortMessage implements SimpleTextMessageProvider {

        @Override
        public boolean showMessage(final NodeParametersInput context) {
            return FileSystemPortConnectionUtil.hasEmptyFileSystemPort(context);
        }

        @Override
        public String title() {
            return "File system managed by File System Input Port";
        }

        @Override
        public String description() {
            return "No file system is currently connected. To proceed, either connect a file system to the input"
                + " port or remove the port.";
        }

        @Override
        public MessageType type() {
            return MessageType.INFO;
        }
    }

    @TextMessage(value = FileSystemManagedByPortMessage.class)
    @Layout(CommonReaderLayout.File.Source.class)
    Void m_authenticationManagedByPortText;

    @Widget(title = "Source", description = CommonReaderLayout.File.Source.DESCRIPTION)
    @ValueReference(FileSelectionRef.class)
    @Layout(CommonReaderLayout.File.Source.class)
    @Modification.WidgetReference(FileSelectionRef.class)
    @FileReaderWidget()
    FileSelection m_source = new FileSelection();

    //  static class SkipFirstDataRowsRef extends ReferenceStateProvider<Long> {
    //    }

    @Widget(title = "Skip first data rows", description = SkipFirstDataRows.DESCRIPTION)
    //    @ValueReference(SkipFirstDataRowsRef.class)
    @NumberInputWidget(minValidation = IsNonNegativeValidation.class)
    @Layout(SkipFirstDataRows.class)
    long m_skipFirstDataRows;

    static final class MaximumNumberOfRowsDefaultProvider implements DefaultValueProvider<Long> {
        @Override
        public Long computeState(final NodeParametersInput context) {
            return 50L;
        }
    }

    //    interface LimitNumberOfRowsRef extends ParameterReference<Boolean> {
    //    }

    @Widget(title = "Limit number of rows", description = LimitNumberOfRows.DESCRIPTION, advanced = true)
    @Layout(LimitNumberOfRows.class)
    //    @ValueReference(LimitNumberOfRowsRef.class)
    @OptionalWidget(defaultProvider = MaximumNumberOfRowsDefaultProvider.class)
    Optional<Long> m_maximumNumberOfRows = Optional.empty();

    /**
     * Access {@link #m_firstColumnContainsRowIds} by this reference.
     */
    public static class FirstColumnContainsRowIdsRef extends ReferenceStateProvider<Boolean> {
    }

    /**
     * This reference is meant to be used to possibly modify title and description of
     * {@link #m_firstColumnContainsRowIds}.
     */
    public static class UseExistingRowIdWidgetRef implements Modification.Reference {
    }

    @Widget(title = "Use existing RowID", description = UseExistingRowId.DESCRIPTION)
    @ValueReference(FirstColumnContainsRowIdsRef.class)
    @Layout(UseExistingRowId.class)
    @Modification.WidgetReference(UseExistingRowIdWidgetRef.class)
    boolean m_firstColumnContainsRowIds;

    public enum IfSchemaChangesOption {
            @Label(value = "Fail",
                description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_FAIL) //
            FAIL, //
            @Label(value = "Use new schema",
                description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION_USE_NEW_SCHEMA) //
            USE_NEW_SCHEMA, //
    }

    static final class IfSchemaChangesOptionRef implements ParameterReference<IfSchemaChangesOption> {
    }

    static final class UseNewSchema implements EffectPredicateProvider {
        @Override
        public EffectPredicate init(final PredicateInitializer i) {
            return i.getEnum(IfSchemaChangesOptionRef.class).isOneOf(IfSchemaChangesOption.USE_NEW_SCHEMA);
        }
    }

    @Widget(title = "If schema changes",
        description = CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.DESCRIPTION)
    @RadioButtonsWidget
    @Layout(CommonReaderLayout.ColumnAndDataTypeDetection.IfSchemaChanges.class)
    @ValueReference(IfSchemaChangesOptionRef.class)
    public IfSchemaChangesOption m_ifSchemaChangesOption = IfSchemaChangesOption.FAIL;

    enum HowToCombineColumnsOption {
            @Label(value = "Fail if different",
                description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_FAIL)
            FAIL(ColumnFilterMode.UNION),

            @Label(value = "Union",
                description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_UNION)
            UNION(ColumnFilterMode.UNION),

            @Label(value = "Intersection",
                description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION_INTERSECTION)
            INTERSECTION(ColumnFilterMode.INTERSECTION);

        private final ColumnFilterMode m_columnFilterMode;

        HowToCombineColumnsOption(final ColumnFilterMode columnFilterMode) {
            m_columnFilterMode = columnFilterMode;
        }

        ColumnFilterMode toColumnFilterMode() {
            return m_columnFilterMode;
        }
    }

    //static class HowToCombineColumnsOptionRef implements ParameterReference<HowToCombineColumnsOption> {
    //}

    @Widget(title = "How to combine columns",
        description = CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.DESCRIPTION)
    @ValueSwitchWidget
    //@ValueReference(HowToCombineColumnsOptionRef.class)
    @Layout(CommonReaderLayout.MultipleFileHandling.HowToCombineColumns.class)
    public HowToCombineColumnsOption m_howToCombineColumns = HowToCombineColumnsOption.FAIL;
    // TODO NOSONAR this setting should be shown when reading multiple files; currently blocked by UIEXT-1805

    static final class AppendPathColumnDefaultProvider implements DefaultValueProvider<String> {
        @Override
        public String computeState(final NodeParametersInput parametersInput) {
            return "File Path";
        }
    }

    //static class AppendPathColumnRef extends ReferenceStateProvider<Boolean> {
    //}

    @Widget(title = "Append file path column",
        description = CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.DESCRIPTION)
    //@ValueReference(AppendPathColumnRef.class)
    @Layout(CommonReaderLayout.MultipleFileHandling.AppendFilePathColumn.class)
    @OptionalWidget(defaultProvider = AppendPathColumnDefaultProvider.class)
    @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
    Optional<String> m_appendPathColumn = Optional.empty();

    public void loadFromTableReaderPathSettings(final CommonTableReaderPath path) {
        m_source = path.location;
    }

    public void saveToTableReaderPathSettings(final CommonTableReaderPath path) {
        path.location = m_source;
    }

    public void loadFromConfig(final MultiTableReadConfig<?, ?> config) {

        final var tableReadConfig = config.getTableReadConfig();

        m_skipFirstDataRows = tableReadConfig.skipRows() ? tableReadConfig.getNumRowsToSkip() : 0L;

        m_maximumNumberOfRows =
            tableReadConfig.getMaxRows() <= 0 ? Optional.empty() : Optional.of(tableReadConfig.getMaxRows());

        m_firstColumnContainsRowIds = tableReadConfig.useRowIDIdx();

        m_ifSchemaChangesOption =
            config.saveTableSpecConfig() ? IfSchemaChangesOption.FAIL : IfSchemaChangesOption.USE_NEW_SCHEMA;

        m_howToCombineColumns = config.failOnDifferingSpecs() ? HowToCombineColumnsOption.FAIL
            : (config.getSpecMergeMode() == SpecMergeMode.UNION ? HowToCombineColumnsOption.UNION
                : HowToCombineColumnsOption.INTERSECTION);

        m_appendPathColumn =
            config.appendItemIdentifierColumn() ? Optional.of(config.getItemIdentifierColumnName()) : Optional.empty();
    }

    public void saveToConfig(final AbstractMultiTableReadConfig<?, ? extends DefaultTableReadConfig<?>, ?, ?> config) {

        final var tableReadConfig = config.getTableReadConfig();

        tableReadConfig.setSkipRows(m_skipFirstDataRows > 0);
        tableReadConfig.setNumRowsToSkip(m_skipFirstDataRows);

        tableReadConfig.setLimitRows(m_maximumNumberOfRows.isPresent());
        tableReadConfig.setMaxRows(m_maximumNumberOfRows.orElse(0L));

        tableReadConfig.setRowIDIdx(0);
        tableReadConfig.setUseRowIDIdx(m_firstColumnContainsRowIds);

        config.setSaveTableSpecConfig(m_ifSchemaChangesOption == IfSchemaChangesOption.FAIL);
        config.setCheckSavedTableSpec(true); // the option to ignore saved table spec is deprecated

        config.setFailOnDifferingSpecs(m_howToCombineColumns == HowToCombineColumnsOption.FAIL);
        config.setSpecMergeMode(m_howToCombineColumns == HowToCombineColumnsOption.INTERSECTION
            ? SpecMergeMode.INTERSECTION : SpecMergeMode.UNION);

        config.setAppendItemIdentifierColumn(m_appendPathColumn.isPresent());
        config.setItemIdentifierColumnName(m_appendPathColumn.orElse(""));
    }

}
