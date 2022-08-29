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
 * ------------------------------------------------------------------------
 *
 * History
 *   Aug 7, 2010 (wiswedel): created
 */
package org.knime.base.node.preproc.cellreplace;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.knime.base.node.preproc.cellreplace.CellReplacerNodeSettings.NoMatchPolicy;
import org.knime.base.node.preproc.cellreplace.CellReplacerNodeSettings.StringMatchBehaviour;
import org.knime.core.data.DataValue;
import org.knime.core.data.StringValue;
import org.knime.core.node.defaultnodesettings.DefaultNodeSettingsPane;
import org.knime.core.node.defaultnodesettings.DialogComponentBoolean;
import org.knime.core.node.defaultnodesettings.DialogComponentButtonGroup;
import org.knime.core.node.defaultnodesettings.DialogComponentColumnNameSelection;
import org.knime.core.node.defaultnodesettings.DialogComponentString;
import org.knime.core.node.defaultnodesettings.SettingsModel;

/**
 * <code>NodeDialog</code> for the "CellReplacer" Node. Replaces cells in a column according to dictionary table (2nd
 * input)
 *
 * @author Bernd Wiswedel
 */
public final class CellReplacerNodeDialog extends DefaultNodeSettingsPane {

    /**
     * New pane for configuring the CellReplacer node.
     */
    protected CellReplacerNodeDialog() {
        var settings = new CellReplacerNodeSettings();

        @SuppressWarnings("unchecked") // VarArgs are safe
        var targetColSelector = new DialogComponentColumnNameSelection(settings.getTargetColNameModel(),
            "Target column", 0, DataValue.class);
        var noMatchPols =
            Arrays.stream(NoMatchPolicy.values()).map(NoMatchPolicy::toString).collect(Collectors.toList());
        var noMatchButtonGroup = new DialogComponentButtonGroup(settings.getNoMatchPolicyModel(), false, null,
            noMatchPols.toArray(new String[noMatchPols.size()]));

        @SuppressWarnings("unchecked") // VarArgs are safe
        var dictInputColSelector = new DialogComponentColumnNameSelection(settings.getDictInputColModel(),
            "Input (Lookup)", 1, DataValue.class);
        enableModelOnlyIfStringColSelected(settings.getStringMatchBehaviourModel(), dictInputColSelector);
        enableModelOnlyIfStringColSelected(settings.getStringCaseSensitiveMatchingModel(), dictInputColSelector);

        @SuppressWarnings("unchecked") // VarArgs are safe
        var dictOutputColSelector = new DialogComponentColumnNameSelection(settings.getDictOutputColModel(),
            "Output (Replacement)", 1, DataValue.class);
        var stringMatchBehaviours = Arrays.stream(StringMatchBehaviour.values()).map(StringMatchBehaviour::toString)
            .collect(Collectors.toList());
        var stringMatchBehaviourSelector = new DialogComponentButtonGroup(settings.getStringMatchBehaviourModel(),
            false, "Matching behaviour", stringMatchBehaviours.toArray(new String[0]));
        var stringCaseSensitiveMatchingChecker =
            new DialogComponentBoolean(settings.getStringCaseSensitiveMatchingModel(), "Case sensitive");
        var appendColumnChecker =
            new DialogComponentBoolean(settings.getAppendColumnModel(), "Append result as new column");
        var appendColumnNameField = new DialogComponentString(settings.getAppendColumnNameModel(), "");
        var appendFoundColumnChecker = new DialogComponentBoolean(settings.getAppendFoundColumnModel(),
            "Create additional \"found\" / \"not found\" column");
        var appendFoundColumnPositiveNameField =
            new DialogComponentString(settings.getFoundColumnPositiveStringModel(), "Found");
        var appendFoundColumnNegativeNameField =
            new DialogComponentString(settings.getFoundColumnNegativeStringModel(), "Not Found");
        var retainColumnMetadataField = new DialogComponentBoolean(settings.getRetainColumnPropertiesModel(),
            "Copy metadata from replacement column");

        createNewGroup("Input table");
        addDialogComponent(targetColSelector);
        closeCurrentGroup();

        createNewGroup("Dictionary table");
        addDialogComponent(dictInputColSelector);
        addDialogComponent(dictOutputColSelector);
        closeCurrentGroup();

        createNewGroup("Dictionary matching behaviour (only applicable for Strings)");
        addDialogComponent(stringMatchBehaviourSelector);
        addDialogComponent(stringCaseSensitiveMatchingChecker);
        closeCurrentGroup();

        createNewGroup("(Additional) Result Columns");
        setHorizontalPlacement(true);
        addDialogComponent(appendColumnChecker);
        addDialogComponent(appendColumnNameField);
        setHorizontalPlacement(false);
        setHorizontalPlacement(true);
        addDialogComponent(appendFoundColumnChecker);
        addDialogComponent(appendFoundColumnPositiveNameField);
        addDialogComponent(appendFoundColumnNegativeNameField);
        closeCurrentGroup();

        setHorizontalPlacement(false);
        createNewGroup("If no element matches use");
        addDialogComponent(noMatchButtonGroup);
        closeCurrentGroup();

        setHorizontalPlacement(false);
        createNewGroup("Metadata in Output");
        addDialogComponent(retainColumnMetadataField);
    }

    private static void enableModelOnlyIfStringColSelected(final SettingsModel m,
        final DialogComponentColumnNameSelection s) {
        if (s == null) {
            return;
        }
        s.getModel().addChangeListener(e -> m.setEnabled(
            s.getSelectedAsSpec() != null && s.getSelectedAsSpec().getType().isCompatible(StringValue.class)));
        m.setEnabled(s.getSelectedAsSpec() != null && s.getSelectedAsSpec().getType().isCompatible(StringValue.class));
    }

}
