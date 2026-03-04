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
 */
package org.knime.base.node.preproc.manipulator;

import java.util.stream.Stream;

import org.knime.base.node.io.filehandling.webui.FileSystemPortConnectionUtil;
import org.knime.base.node.io.filehandling.webui.reader2.ColumnSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.DataTypeSerializer;
import org.knime.base.node.io.filehandling.webui.reader2.TableSpecSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationElementSettings;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersCommon;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersCommon.ColumnTransformationComparator;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.InitialTransformationElementSettingsStateProvider;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.SpecsRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TransformationElementSettingsArrayWidgetRef;
import org.knime.base.node.io.filehandling.webui.reader2.TransformationParametersStateProvidersCommon.TransformationElementsDirtyTrackerStateProvider;
import org.knime.core.data.DataType;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.Pair;
import org.knime.core.webui.node.dialog.defaultdialog.internal.dirty.DirtyTracker;
import org.knime.core.webui.node.dialog.defaultdialog.internal.widget.ArrayWidgetInternal;
import org.knime.core.webui.node.dialog.defaultdialog.widget.Modification;
import org.knime.filehandling.core.node.table.reader.DefaultTableTransformation;
import org.knime.filehandling.core.node.table.reader.config.MultiTableReadConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.ConfigID;
import org.knime.filehandling.core.node.table.reader.config.tablespec.DefaultTableSpecConfig;
import org.knime.filehandling.core.node.table.reader.config.tablespec.TableSpecConfig;
import org.knime.filehandling.core.node.table.reader.selector.ColumnFilterMode;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.Effect;
import org.knime.node.parameters.updates.ValueProvider;
import org.knime.node.parameters.updates.ValueReference;

/**
 * @author Thomas Reifenberger, TNG Technology Consulting GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
@Modification({TableManipulatorTransformationParametersStateProviders.TransformationSettingsWidgetModification.class})
final class TableManipulatorTransformationParameters
    implements NodeParameters, TableManipulatorSpecific.ProductionPathProviderAndTypeHierarchy, DataTypeSerializer,
    TransformationParametersCommon.TransformationsMapper<DataType> {

    private static final String TRANSFORMATION_DESCRIPTION = """
            This option allows modifying the structure of the output table. It supports reordering, filtering and
            renaming columns. It is also possible to change the type of the columns.
            Whether and where to add unknown columns during execution is specified via the special entry
            <i>Any unknown column</i>.
            <br />
            <b>Note:</b><br />
            The transformation matching during execution is based on name. That means if there was a column
            [A, Integer] during configuration in the dialog and this column becomes [A, String] during execution, then
            the stored transformation is applied to it. For filtering, ordering and renaming, this is straight forward.
            For type mapping the following is done: If there is an alternative converter to the specified KNIME type,
            then this converter is used, otherwise we default to the default KNIME type for the new type. In our example
            we might have specified that [A, Integer] should be mapped to Long. For the changed column [A, String] there
            is no converter to Long, so we default back to String and A becomes a String column in the output table.
            """;

    @ValueReference(TransformationParametersStateProvidersCommon.TableSpecSettingsRef.class)
    // for adding dynamic and provider
    @Modification.WidgetReference(SpecsRef.class)

    /*
     * Note that this field remains `null` until it is updated by the state provider when specs could be computed.
     */
    TableSpecSettings[] m_specs;

    @Widget(title = "Enforce types", description = """
            Controls how columns whose type changes are dealt with.
            If selected, the mapping to the KNIME type you configured is attempted.
            The node will fail if that is not possible.
            If unselected, the KNIME type corresponding to the new type is used.
            """)
    boolean m_enforceTypes = true;

    @DirtyTracker(TransformationElementsDirtyTrackerStateProvider.class)
    Void m_dirtyTracker;

    @Widget(title = "Transformations", description = TRANSFORMATION_DESCRIPTION)
    @ArrayWidget(elementTitle = "Column", showSortButtons = true, hasFixedSize = true)
    @ArrayWidgetInternal(withEditAndReset = true, withElementCheckboxes = true,
        titleProvider = TransformationElementSettings.TitleProvider.class,
        subTitleProvider = TransformationElementSettings.SubTitleProvider.class)
    @ValueReference(TransformationParametersStateProvidersCommon.TransformationElementSettingsRef.class)
    // for adding dynamic choices
    @Modification.WidgetReference(TransformationElementSettingsArrayWidgetRef.class)
    @Effect(predicate = FileSystemPortConnectionUtil.ConnectedWithoutFileSystemSpec.class,
        type = Effect.EffectType.HIDE)
    TransformationElementSettings[] m_columnTransformation =
        new TransformationElementSettings[]{TransformationElementSettings.createUnknownElement()};

    @ValueReference(TransformationParametersStateProvidersCommon.InitialTransformationElementSettingsRef.class)
    @ValueProvider(InitialTransformationElementSettingsStateProvider.class)
    @Persistor(TransformationElementSettings.Data.DoNotPersist.class)
    TransformationElementSettings.Data[] m_initialColumnTransformation = new TransformationElementSettings.Data[0];

    @Override
    public void validate() throws InvalidSettingsException {
        if (m_specs != null) {
            validateSpecs();
        }
        validateColumnTransformations();
    }

    /**
     * Use this method in a {@link org.knime.node.parameters.migration.NodeParametersMigration} to load transformation
     * settings from a legacy {@link TableSpecConfig}.
     *
     * @param tableSpecConfig the table spec config to load from
     */
    void loadFromTableSpecConfig(final TableSpecConfig<DataType> tableSpecConfig) {
        if (tableSpecConfig == null) {
            return;
        }
        final var items = tableSpecConfig.getItems();
        m_specs = items.stream().map(key -> {
            final var spec = tableSpecConfig.getSpec(key);
            final var colSpecs = spec.stream().map(colSpec -> new ColumnSpecSettings(colSpec.getName().get(),
                toSerializableType(colSpec.getType()), colSpec.hasType())).toArray(ColumnSpecSettings[]::new);
            return new TableSpecSettings(key, colSpecs);
        }).toArray(TableSpecSettings[]::new);

        m_enforceTypes = tableSpecConfig.getTableTransformation().enforceTypes();

        final var transformationElements =
            tableSpecConfig.getTableTransformation().stream().map(this::getTransformationElement).toList();
        final var unknownTransformationElement =
            getTransformationElement(tableSpecConfig.getTableTransformation().getTransformationForUnknownColumns());
        m_columnTransformation = Stream.concat(transformationElements.stream(), Stream.of(unknownTransformationElement))
            .sorted(new ColumnTransformationComparator(unknownTransformationElement))//
            .map(Pair::getSecond) //
            .toArray(TransformationElementSettings[]::new);
    }

    void saveToConfig(final MultiTableReadConfig<TableManipulatorConfig, DataType> config, final ConfigID configID,
        final ColumnFilterMode columnFilterMode) {
        if (m_specs == null) {
            return;
        }

        final var individualSpecs = toSpecMap(this, m_specs);
        final var rawSpec = toRawSpec(individualSpecs.values());
        final var transformations = determineTransformations(rawSpec, columnFilterMode);
        final var tableTransformation = new DefaultTableTransformation<>(rawSpec, transformations.getFirst(),
            columnFilterMode, transformations.getSecond(), m_enforceTypes, false);

        final var tableSpecConfig = DefaultTableSpecConfig.createFromTransformationModel(TableSourceGroup.ROOTPATH,
            configID, individualSpecs, tableTransformation, null);

        config.setTableSpecConfig(tableSpecConfig);
    }

    @Override
    public TransformationElementSettings[] getColumnTransformation() {
        return m_columnTransformation;
    }

    @Override
    public TableSpecSettings[] getSpecs() {
        return m_specs;
    }
}
