/*
 * ------------------------------------------------------------------------
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
 * ---------------------------------------------------------------------
 */
package org.knime.base.collection.list.create2;

import org.knime.core.webui.node.dialog.defaultdialog.setting.filter.column.LegacyColumnFilterPersistor;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.layout.After;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.layout.Section;
import org.knime.node.parameters.migration.Migrate;
import org.knime.node.parameters.persistence.Persist;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.widget.choices.filter.ColumnFilter;
import org.knime.node.parameters.widget.choices.filter.ColumnFilterWidget;
import org.knime.node.parameters.widget.choices.util.AllColumnsProvider;
import org.knime.node.parameters.widget.choices.util.ColumnSelectionUtil;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Settings for the "Create Collection Column" node using the DefaultNode dialog.
 *
 * Keeps persistence keys compatible with the legacy dialog so the existing
 * {@link CollectionCreate2NodeModel} can be kept untouched.
 */
@SuppressWarnings("restriction")
public final class CollectionCreate2NodeSettings implements NodeParameters {

    /** Marker for the Column selection section. */
    @Section(title = "Column selection",
        description = "Select input columns to aggregate into the collection. Use include/exclude lists,"
            + " wildcard/regex filters, and type filters to refine the selection.")
    interface ColumnSelectionSection {}

    /** Marker for the Collection type section. */
    @Section(title = "Collection type",
        description = "Choose the kind of collection to create and how to handle missing values.")
    @After(ColumnSelectionSection.class)
    interface CollectionTypeSection {}

    /** Marker for the Output structure section. */
    @Section(title = "Output table structure",
        description = "Configure the output: remove the aggregated input columns and set the name of"
            + " the newly appended collection column.")
    @After(CollectionTypeSection.class)
    interface OutputStructureSection {}

    /** Default constructor. */
    public CollectionCreate2NodeSettings() {}

    /** Context constructor (optional). */
    public CollectionCreate2NodeSettings(final NodeParametersInput context) {
        this();
        final var columns = ColumnSelectionUtil.getAllColumns(context, 0);
        m_includes = new ColumnFilter(columns).withIncludeUnknownColumns();
    }

    // Column selection -------------------------------------------------------

    /**
     * Column filter that mirrors the legacy DialogComponentColumnFilter2 and saves under key "includes" so that the
     * legacy SettingsModelColumnFilter2 in the node model can read it unchanged.
     */
    @Persistor(InclColsPersistor.class)
    @Widget(title = "Input columns",
        description = "Select the columns to aggregate into the collection. Move columns between the Include and"
            + " Exclude lists, filter by name using wildcards or regular expressions, and optionally filter by"
            + " column type. The selection is stored compatibly under the legacy key 'includes'.")
    @ColumnFilterWidget(choicesProvider = AllColumnsProvider.class)
    @Layout(ColumnSelectionSection.class)
    @Migrate(loadDefaultIfAbsent = true)
    ColumnFilter m_includes = new ColumnFilter();

    // Collection type --------------------------------------------------------

    /** If true a SetCell is created, otherwise a ListCell. */
    @Persist(configKey = "createSet")
    @Migrate(loadDefaultIfAbsent = true)
    @Widget(title = "Create a collection of type 'set' (doesn't store duplicate values)",
        description = "If enabled, the new collection column will be of type 'set' and store only distinct values."
            + " If disabled, a 'list' will be created preserving duplicates and order.")
    @Layout(CollectionTypeSection.class)
    boolean m_createSet = false;

    /** If true missing values are ignored when creating the collection. */
    @Persist(configKey = "ignoreMissingValue")
    @Migrate(loadDefaultIfAbsent = true)
    @Widget(title = "Ignore missing values",
        description = "If enabled, missing cells in the selected input columns are ignored and not added to the"
            + " resulting collection. If disabled, missing values are included as missing cells in the collection.")
    @Layout(CollectionTypeSection.class)
    boolean m_ignoreMissing = false;

    // Output structure -------------------------------------------------------

    /** Remove aggregated input columns from the output. */
    @Persist(configKey = "removeCols")
    @Migrate(loadDefaultIfAbsent = true)
    @Widget(title = "Remove aggregated columns from table",
        description = "If enabled, the original input columns that were aggregated into the collection are removed"
            + " from the output table. If disabled, they are kept alongside the new collection column.")
    @Layout(OutputStructureSection.class)
    boolean m_removeCols = false;

    /** Name of the new collection column. */
    @Persist(configKey = "newColName")
    @Migrate(loadDefaultIfAbsent = true)
    @Widget(title = "Enter the name of the new column",
        description = "Provide the name for the new collection column to append to the output table. The name must"
            + " be a valid column name and must not conflict with existing columns.")
    @TextInputWidget(patternValidation = ColumnNameValidation.class)
    @Layout(OutputStructureSection.class)
    String m_newColName = "AggregatedValues";

    /** Persistor that stores/loads the column filter under the legacy key "includes". */
    static final class InclColsPersistor extends LegacyColumnFilterPersistor {
        InclColsPersistor() {
            super("includes");
        }
    }

}
