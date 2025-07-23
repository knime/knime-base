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
 *   Dec 15, 2022 (Adrian Nembach, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.column.renamer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.webui.node.dialog.defaultdialog.persistence.booleanhelpers.AlwaysSaveTrueBoolean;
import org.knime.core.webui.node.dialog.defaultdialog.util.updates.StateComputationFailureException;
import org.knime.node.parameters.NodeParameters;
import org.knime.node.parameters.NodeParametersInput;
import org.knime.node.parameters.Widget;
import org.knime.node.parameters.array.ArrayWidget;
import org.knime.node.parameters.layout.HorizontalLayout;
import org.knime.node.parameters.layout.Layout;
import org.knime.node.parameters.persistence.Persistor;
import org.knime.node.parameters.updates.ParameterReference;
import org.knime.node.parameters.updates.StateProvider;
import org.knime.node.parameters.updates.ValueReference;
import org.knime.node.parameters.widget.choices.ChoicesProvider;
import org.knime.node.parameters.widget.choices.ColumnChoicesProvider;
import org.knime.node.parameters.widget.text.TextInputWidget;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils;
import org.knime.node.parameters.widget.text.util.ColumnNameValidationUtils.ColumnNameValidation;

/**
 * Settings of the Column Renamer node.
 *
 * @author Adrian Nembach, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public final class ColumnRenamerSettings implements NodeParameters {

    ColumnRenamerSettings(final NodeParametersInput context) {
        // pick the last column because a typical scenario is to rename columns appended by the previous node
        var initialColumn = context.getInTableSpec(0)//
            .filter(s -> s.getNumColumns() > 0)//
            .map(s -> s.getColumnSpec(s.getNumColumns() - 1).getName());
        if (initialColumn.isPresent()) {
            // initialize as identity and let the NodeModel warn the user
            var renaming = new Renaming();
            renaming.m_oldName = initialColumn.get();
            renaming.m_newName = renaming.m_oldName;
            m_renamings = new Renaming[]{renaming};
        }
    }

    ColumnRenamerSettings() {
        // persistence and JSON conversion constructor
    }

    @Widget(title = "Renamings", description = "Allows to define new names for columns.")
    @ArrayWidget(elementLayout = ArrayWidget.ElementLayout.HORIZONTAL_SINGLE_LINE, addButtonText = "Add column",
        elementDefaultValueProvider = RenamingDefaultValueProvider.class)
    @ValueReference(RenamingsRef.class)
    public Renaming[] m_renamings = new Renaming[0];

    interface RenamingsRef extends ParameterReference<Renaming[]> {
    }

    static final class RenamingDefaultValueProvider implements StateProvider<Renaming> {

        private Supplier<Renaming[]> m_renamings;

        @Override
        public void init(final StateProviderInitializer initializer) {
            this.m_renamings = initializer.computeFromValueSupplier(RenamingsRef.class);
            initializer.computeBeforeOpenDialog();
        }

        @Override
        public Renaming computeState(final NodeParametersInput context) throws StateComputationFailureException {
            final var spec = context.getInTableSpec(0);
            if (spec.isEmpty()) {
                return new Renaming();
            }
            final var alreadySelectedColumns = Arrays.stream(m_renamings.get()).map(r -> r.m_oldName)
                .filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            final var firstAvailableCol = spec.get().stream()
                .filter(colSpec -> !alreadySelectedColumns.contains(colSpec.getName())).reduce((a, b) -> b);
            if (firstAvailableCol.isEmpty()) {
                return new Renaming();
            }
            final var renaming = new Renaming();
            renaming.m_oldName = firstAvailableCol.get().getName();
            renaming.m_newName = renaming.m_oldName;
            return renaming;
        }

    }

    static final class Renaming implements NodeParameters {

        @HorizontalLayout
        interface RenamingLayout {
        }

        @Widget(title = "Column", description = "The column to rename.")
        @ChoicesProvider(DynamicAllColumnsProvider.class)
        @Layout(RenamingLayout.class)
        @ValueReference(OldNameRef.class)
        public String m_oldName;

        @Widget(title = "New name",
            description = "The new column name. Must not be empty or consist only of whitespaces.")
        @TextInputWidget(patternValidation = ColumnNameValidationUtils.ColumnNameValidation.class)
        @Layout(RenamingLayout.class)
        public String m_newName;

        interface OldNameRef extends ParameterReference<String> {
        }

        static final class DynamicAllColumnsProvider implements ColumnChoicesProvider {

            private Supplier<Renaming[]> m_renamings;

            private Supplier<String> m_currentSelection;

            @Override
            public void init(final StateProviderInitializer initializer) {
                this.m_renamings = initializer.computeFromValueSupplier(RenamingsRef.class);
                this.m_currentSelection = initializer.getValueSupplier(OldNameRef.class);
                ColumnChoicesProvider.super.init(initializer);
            }

            @Override
            public List<DataColumnSpec> columnChoices(final NodeParametersInput context) {
                final var spec = context.getInTableSpec(0);
                if (spec.isEmpty()) {
                    return Collections.emptyList();
                }
                final var columns = Arrays.stream(m_renamings.get()).map(r -> r.m_oldName)
                    .filter(oldName -> oldName != null && !oldName.equals(this.m_currentSelection.get()))
                    .collect(Collectors.toSet());
                return spec.get().stream().filter(colSpec -> !columns.contains(colSpec.getName())).toList();
            }
        }
    }

    static final class DoNotAllowPaddedColumnNamePersistor extends AlwaysSaveTrueBoolean {
        protected DoNotAllowPaddedColumnNamePersistor() {
            super("doNotAllowPaddedColumnName");
        }
    }

    @Persistor(DoNotAllowPaddedColumnNamePersistor.class)
    boolean m_doNotAllowPaddedColumnName = true;

}
