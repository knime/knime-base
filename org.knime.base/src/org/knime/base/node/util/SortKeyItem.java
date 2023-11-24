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
 *   23 Nov 2022 (Manuel Hotz, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.OptionalInt;
import java.util.function.Predicate;

import org.apache.commons.lang3.BooleanUtils;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.sort.RowComparator;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.base.ConfigBaseRO;
import org.knime.core.node.config.base.ConfigBaseWO;
import org.knime.core.node.util.CheckUtils;

/**
 * Item in a sort key, which is used to specify how a row comparator should be built. The "sort key" is just a sequence
 * of columns or the row key. Since handling of missing cells is (currently) set on the sort key level and not for each
 * item individually, this setting is not handled in here.
 *
 * @author Manuel Hotz, KNIME GmbH, Konstanz, Germany
 */
public final class SortKeyItem {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(SortKeyItem.class);

    /**
     * Identifier.
     */
    private final String m_identifier;

    /**
     * Sort item ascendingly if {@code true}, else sort descendingly.
     */
    private final boolean m_ascendingOrder;

    /**
     * Use alphanumeric comparison instead of lexicographic for string-compatible values.
     * @since 4.7.0
     */
    private final boolean m_alphaNumComp;

    /**
     * Creates a new sort key item.
     * @param identifier identifier to use for the item
     * @param sortAscending {@code true} if it should be sorted ascendingly, {@code false} for descending sorting
     * @param compareAlphanum {@code true} to use alphanumeric string comparison, {@code false} otherwise
     */
    public SortKeyItem(final String identifier, final boolean sortAscending, final boolean compareAlphanum) {
        m_identifier = identifier;
        m_ascendingOrder = sortAscending;
        m_alphaNumComp = compareAlphanum;
    }

    /**
     * @return the identifier of sort key item
     */
    public String getIdentifier() {
        return m_identifier;
    }

    /**
     * @return {@code true} if column should be sorted in ascending order, {@code false} otherwise
     */
    public boolean isAscendingOrder() {
        return m_ascendingOrder;
    }

    /**
     * @return {@code true} if values should be compared alphanumerically, {@code false} otherwise
     */
    public boolean isAlphaNumComp() {
        return m_alphaNumComp;
    }


    /**
     * Save sort key to settings using the given settings keys.
     * @param sortKey the sort key to save
     * @param includeIdentifiersKey the settings key to use for the included identifiers (column names or row key)
     * @param sortOrderKey the settings key to use for the sort order for each column
     * @param alphaNumCompKey the settings key for alphanumeric comparison of column values
     * @param settings the settings to save to
     *
     */
    public static void saveTo(final List<SortKeyItem> sortKey,  final String includeIdentifiersKey,
        final String sortOrderKey, final String alphaNumCompKey, final ConfigBaseWO settings) {
        final int numCols = sortKey.size();
        final var include = new String[numCols];
        final var ascending = new boolean[numCols];
        final var alphaNum = new boolean[numCols];
        for (var i = 0; i < numCols; i++) {
            final var col = sortKey.get(i);
            include[i] = col.m_identifier;
            ascending[i] = col.m_ascendingOrder;
            alphaNum[i] = col.m_alphaNumComp;
        }
        settings.addStringArray(includeIdentifiersKey, include);
        settings.addBooleanArray(sortOrderKey, ascending);
        // added in 4.7
        settings.addBooleanArray(alphaNumCompKey, alphaNum);
    }

    /**
     * @param includeIdentifiersKey the settings key for the included identifiers (column names or row key)
     * @param sortOrderKey the settings key for the sort order of each column
     * @param alphaNumCompKey the settings key for alphanumeric comparison of column values
     * @param settings the settings to load from
     * @return the sort key or an empty list if no sort key was stored in settings under given identifiers key
     * @throws InvalidSettingsException if any of the given keys is not available or has no default set
     */
    public static List<SortKeyItem> loadFrom(final String includeIdentifiersKey, final String sortOrderKey,
        final String alphaNumCompKey, final ConfigBaseRO settings) throws InvalidSettingsException {
        if (!settings.containsKey(includeIdentifiersKey)) {
            return Collections.emptyList();
        }
        final List<SortKeyItem> items = new ArrayList<>();
        final var identifiers = settings.getStringArray(includeIdentifiersKey);
        final var ascending = settings.getBooleanArray(sortOrderKey);

        // added in 4.7, catch missing setting
        // alphanum comparisons are default behavior only for new instances of sort keys
        // we use an empty array to indicate missing setting to properly handle it
        final var marker = new boolean[0];
        boolean[] alphaNum = settings.getBooleanArray(alphaNumCompKey, marker);

        // fill up with "false" as default if it is missing or mis-configured (old workflow)
        if (alphaNum.length == 0 || identifiers.length != alphaNum.length) {
            // We may be currently loading after flow variables were applied, which might not have set the "new"
            // alphanum flags. This means the array lengths might differ: inclList/sortOrder from flow variables,
            // alphaNum from model settings (so each sort key is only partially overwritten by flow variables)
            LOGGER.debug(String.format("Setting for \"%s\" missing or mis-matched, using default values.%n" +
                    "If you are setting flow variables, check that all relevant settings are overwritten by flow " +
                    "variables.", alphaNumCompKey));
            alphaNum = new boolean[identifiers.length];
        }

        for (var i = 0; i < identifiers.length; i++) {
            items.add(new SortKeyItem(identifiers[i], ascending[i], alphaNum[i]));
        }
        return Collections.unmodifiableList(items);
    }

    /**
     * Validate settings values for the given keys.
     * @param includeIdentifiersKey key for included identifiers
     * @param sortOrderKey key for sort orders
     * @param alphaNumCompKey key for alphanum comparison
     * @param settings node settings
     * @throws InvalidSettingsException if a required settings key is missing or invalid or there are duplicate
     *      identifiers
     */
    public static void validate(final String includeIdentifiersKey, final String sortOrderKey,
        final String alphaNumCompKey, final ConfigBaseRO settings) throws InvalidSettingsException {
        final var noColSelected = "No column selected.";
        CheckUtils.checkSetting(settings.containsKey(includeIdentifiersKey), noColSelected);
        final var inclList = CheckUtils.checkSettingNotNull(settings.getStringArray(includeIdentifiersKey),
            noColSelected);
        // scan for duplicate entries in include list
        for (var i = 0; i < inclList.length; i++) {
            final var entry = inclList[i];
            for (int j = i + 1; j < inclList.length; j++) {
                if (entry.equals(inclList[j])) {
                    throw new InvalidSettingsException(
                        "The column \"" + entry + "\" appears multiple times at positions " + i + " and " + j
                            + ". The entries must be unique.");
                }
            }
        }
        CheckUtils.checkSetting(settings.containsKey(sortOrderKey),
            "No sorting order was specified. Set it in the node configuration.");
        final var sortOrders = CheckUtils.checkSettingNotNull(settings.getBooleanArray(sortOrderKey),
            "Invalid sort orders.");
        CheckUtils.checkSetting(inclList.length == sortOrders.length, "Mismatch in number of columns and sort orders.");
        /* Don't validate alphaNum setting for backwards compatibility (added in 4.7):
         * If you overwrite `inclList` and `sortOrders` by flow variable, but not `alphaNumComp` (e.g. when loading
         * a workflow created in 4.6), you get the dialog/model settings overwritten by flow variables passed here.
         * However, the `alphaNumComp` missing from the flow variables is not overwritten and stays like before.
         * E.g. ([-RowID-], [true], [false]) would come from the dialog by default for a 4.6 workflow, but overwriting
         * it with this flow variable assignment (inclList=[col1, col2], sortOrders=[true, false]) would result
         * in settings as ([col1, col2], [true, false], [false]), which would fail to validate since the lengths don't
         * match.
         */
        if (LOGGER.isDebugEnabled() && settings.containsKey(alphaNumCompKey)) {
            final var alpha = settings.getBooleanArray(alphaNumCompKey);
            // check if the array present differs from a default array with all set to false
            if (alpha.length != inclList.length && BooleanUtils.or(alpha)) {
                LOGGER.debug(String.format("Inconsistent SortKey setting for \"%s\": %s=\"%s\" but %s=\"%s\"",
                    alphaNumCompKey, includeIdentifiersKey, Arrays.toString(inclList), alphaNumCompKey,
                        Arrays.toString(alpha)));
            }
        }
    }

    /**
     * Obtain column identifiers specified in sort key but missing in data table spec.
     * @param sk sort key
     * @param dts data table spec
     * @param isRowKey the check if an identifier represents the row key
     * @return missing column identifiers
     */
    public static List<String> getMissing(final Iterable<SortKeyItem> sk, final DataTableSpec dts,
        final Predicate<String> isRowKey) {
        final List<String> missing = new ArrayList<>();
        for (final var i : sk) {
            final var idx = dts.findColumnIndex(i.m_identifier);
            if (idx == -1 && !isRowKey.test(i.m_identifier)) {
                missing.add(i.m_identifier);
            }
        }
        return missing;
    }

    /**
     * Converts the given sort key into a row comparator for the given data table.
     * @param dts data table to compare rows of
     * @param sortKey sort key to convert
     * @param missingsToEnd whether to always put missing cells at the end of the table, regardless of sort order
     * @param isRowKey for the check if an identifier represents the row key
     * @return a row comparator to compare rows of given data table with
     */
    public static RowComparator toRowComparator(final DataTableSpec dts, final Iterable<SortKeyItem> sortKey,
        final boolean missingsToEnd, final Predicate<String> isRowKey) {
        final var rc = RowComparator.on(dts);
        sortKey.forEach(pos -> {
            final var ascending = pos.isAscendingOrder();
            final var alphaNum = pos.isAlphaNumComp();
            resolveColumnName(dts, pos.getIdentifier(), isRowKey).ifPresentOrElse(
                col -> rc.thenComparingColumn(col, c -> c.withDescendingSortOrder(!ascending)
                    .withAlphanumericComparison(alphaNum).withMissingsLast(missingsToEnd)),
                () -> rc.thenComparingRowKey(k -> k.withDescendingSortOrder(!ascending)
                    .withAlphanumericComparison(alphaNum))
            );
        });
        return rc.build();
    }

    private static OptionalInt resolveColumnName(final DataTableSpec dts, final String colName,
        final Predicate<String> isRowKey) {
        final var idx = dts.findColumnIndex(colName);
        if (idx == -1) {
            if (!isRowKey.test(colName)) {
                throw new IllegalArgumentException(
                    "The column identifier \"" + colName + "\" does not refer to a known column.");
            }
            return OptionalInt.empty();
        }
        return OptionalInt.of(idx);
    }

}
