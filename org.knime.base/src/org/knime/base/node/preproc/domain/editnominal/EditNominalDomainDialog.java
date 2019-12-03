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
 *   Dec 18, 2019 (Perla Gjoka, KNIME GmbH, Konstanz, Germany): created
 */
package org.knime.base.node.preproc.domain.editnominal;

import static org.knime.core.node.util.DataColumnSpecListCellRenderer.isInvalid;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.renderer.DefaultDataValueRenderer;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.NotConfigurableException;
import org.knime.core.node.util.ColumnFilter;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ConfigurationRequestEvent;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.ListModifier;
import org.knime.core.node.util.ColumnSelectionSearchableListPanel.SearchedItemsSelectionMode;
import org.knime.core.node.util.DataColumnSpecListCellRenderer;

/**
 * This class creates the dialog for the Edit Nominal Domain node. The created panel is also used by the Edit
 * Probability Distribution node.
 *
 * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
 */
public final class EditNominalDomainDialog {

    private static final String IGNORE_COLUMNS_NOT_PRESENT_IN_DATA = "If a configured column is not present in data";

    private static final String IGNORE_COLUMNS_NOT_MATCHING_TYPES = "If a configured column has an incompatible type";

    private static final NodeLogger LOGGER = NodeLogger.getLogger(EditNominalDomainDialog.class);

    private final JPanel m_panel;

    private final DataType m_classType;

    private final ColumnFilter m_columnFilter;

    private final Function<DataColumnSpec, Optional<Set<DataCell>>> m_specToPossibleValuesFunction;

    private final boolean m_allowRemovingExistingCells;

    /**
     * The list of all columns.
     */
    private ColumnSelectionSearchableListPanel m_searchableListPanel;

    /**
     * The list of (maybe resorted) data cells.
     */
    private JList<ListListModel<DataCell>> m_jlist;

    /**
     * The data model behind the {@link #m_jlist}.
     */
    private ListListModel<DataCell> m_currentSorting;

    /**
     * The table spec of the input table.
     */
    private DataTableSpec m_orgSpec;

    /**
     * The data column spec of the current selected column for resorting.
     */
    private DataColumnSpec m_currentColSpec;

    /**
     * The possible domain values of the current selected column. (Cached to fill them with an empty list on default)
     */
    private Set<DataCell> m_orgDomainCells;

    /**
     * True if anything changed on the sorting during the current column was selected.
     */
    private boolean m_somethingChanged = false;

    /**
     * The current configuration.
     */
    private EditNominalDomainConfiguration m_configuration;

    private JButton m_resetButton;

    private Map<String, JRadioButton> m_buttonMap = new HashMap<>();

    private ListModifier m_searchableListModifier;

    /**
     * This interface may be implemented for specifying different column filter and possible values of the corresponding
     * column.
     *
     * @author Perla Gjoka, KNIME GmbH, Konstanz, Germany
     */
    public interface TypeHandler {

        /**
         * @return the class type of the picked column.
         */
        public DataType getClassType();

        /**
         * @return a column filter of a specific data type.
         */
        public ColumnFilter getColumnFilter();

        /**
         * @param spec holds the column spec of the input table.
         * @return all the possible values of the corresponding column of that type. The returned set of the possible
         *         values shouldn't be null.
         */
        public Optional<Set<DataCell>> getPossibleValues(DataColumnSpec spec);

    }

    /**
     * @param typeHandler inteface for handling the type specific configurations
     * @param allowRemovingExistingCells holds if the current domain values can be deleted or not.
     */
    public EditNominalDomainDialog(final TypeHandler typeHandler, final boolean allowRemovingExistingCells) {
        m_classType = typeHandler.getClassType();
        m_columnFilter = typeHandler.getColumnFilter();
        m_specToPossibleValuesFunction = typeHandler::getPossibleValues;
        m_allowRemovingExistingCells = allowRemovingExistingCells;
        m_panel = createEditNominalDomainTab();
    }

    /**
     * @return the edit nominal panel.
     */
    public JPanel getPanel() {
        return m_panel;
    }

    /**
     * @param settings holds the settings to be loaded
     * @param spec holds the {@link DataTableSpec} of the input table
     */
    public void load(final NodeSettingsRO settings, final DataTableSpec spec) {
        try {
            if (spec.getNumColumns() == 0) {
                throw new NotConfigurableException("No data at input.");
            }
            m_orgSpec = spec;

            m_currentSorting.clear();

            m_configuration = new EditNominalDomainConfiguration();
            m_configuration.loadConfigurationInDialog(settings);

            if (m_configuration.isIgnoreNotExistingColumns()) {
                m_buttonMap.get(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA).doClick();
            }
            if (m_configuration.isIgnoreWrongTypes()) {
                m_buttonMap.get(IGNORE_COLUMNS_NOT_MATCHING_TYPES).doClick();
            }

            m_searchableListModifier = m_searchableListPanel.update(m_orgSpec, m_columnFilter);
            addColumnSpecsForNotExistingColumns();
        } catch (Exception e) {
            LOGGER.error("Unable to load the settings into the dialog.", e);
        }
    }

    /**
     * @param settings holds the settings to be saved
     */
    public void save(final NodeSettingsWO settings) {
        storeCurrentList();
        m_configuration.setIgnoreNotExistingColumns(m_buttonMap.get(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA).isSelected());
        m_configuration.setIgnoreWrongTypes(m_buttonMap.get(IGNORE_COLUMNS_NOT_MATCHING_TYPES).isSelected());
        m_configuration.saveSettings(settings);
    }

    /**
     * Sets the current {@link DataColumnSpec} to null everytime the dialog is closed.
     */
    public void close() {
        // AP-11443: Clear state to trigger update in ConfigurationRequestListener of
        // m_searchableListPanel on re-open of dialog.
        m_currentColSpec = null;
    }

    private JPanel createEditNominalDomainTab() {
        //attach corresponding listeners to sortingList and searchable panel
        attachListenersToComponents();

        JPanel tabpanel = new JPanel(new BorderLayout());
        tabpanel.add(m_searchableListPanel, BorderLayout.WEST);

        JPanel jPanel = new JPanel(new BorderLayout());
        jPanel.add(createDomainValuesPane(), BorderLayout.CENTER);
        jPanel.add(createButtonBox(), BorderLayout.EAST);

        tabpanel.add(createRadioGroups(), BorderLayout.SOUTH);
        tabpanel.add(jPanel);

        return tabpanel;
    }

    private void attachListenersToComponents() {
        m_currentSorting = new ListListModel<>();
        m_searchableListPanel =
            new ColumnSelectionSearchableListPanel(SearchedItemsSelectionMode.SELECT_FIRST, this::checkConfigured);

        m_searchableListPanel.addConfigurationRequestListener(this::configurationListenerRequested);

        // if something is changed in the currentSorting we set the save flag, which leads wo a storage
        m_currentSorting.addListDataListener(configurationCurrentSortingListener());
    }

    @SuppressWarnings("unchecked")
    // The m_currentSorting is of type ListListModel, which is an implementation of ListModel.
    // Since JList expects a ListModel, the surprise warning is safe to be added.
    private JScrollPane createDomainValuesPane() {
        JScrollPane scrollPane = new JScrollPane();
        m_jlist = new JList<>(m_currentSorting);
        scrollPane.setViewportView(m_jlist);
        m_jlist.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
        m_jlist.setCellRenderer(new HighlightSpecialCellsRenderer());
        m_jlist.setBorder(BorderFactory.createTitledBorder("Domain values"));
        return scrollPane;
    }

    private Box createButtonBox() {
        Box buttonBox = Box.createVerticalBox();
        buttonBox.setBorder(new TitledBorder("Actions"));

        Component moveFirstButton = createButton("Move First", e -> moveFirstConfiguration());

        Dimension size = moveFirstButton.getPreferredSize();
        //  all buttons should have the same size
        buttonBox.add(createButton("Add", e -> addStringCellConfiguration(), size));
        buttonBox.add(createButton("Remove", e -> removeStringCellsConfiguration(), size));
        buttonBox.add(createButton("A-Z", e -> sortNamesConfiguration(SortOption.ASCENDING), size));
        buttonBox.add(createButton("Z-A", e -> sortNamesConfiguration(SortOption.DESCENDING), size));
        buttonBox.add(moveFirstButton);
        buttonBox.add(createButton("Move Last", e -> moveLastConfiguration(), size));
        buttonBox.add(createButton("Up", e -> moveUpConfiguration(), size));
        buttonBox.add(createButton("Down", e -> moveDownConfiguration(), size));
        m_resetButton = createButton("Reset", e -> removeConfiguration(), size);
        buttonBox.add(m_resetButton);
        buttonBox.add(createLegend());
        return buttonBox;
    }

    private JPanel createRadioGroups() {
        JPanel radioGroups = new JPanel();
        radioGroups.setLayout(new BorderLayout());
        radioGroups.add(createRadioButtonGroup(IGNORE_COLUMNS_NOT_PRESENT_IN_DATA), BorderLayout.CENTER);
        radioGroups.add(createRadioButtonGroup(IGNORE_COLUMNS_NOT_MATCHING_TYPES), BorderLayout.SOUTH);
        return radioGroups;
    }

    private ListDataListener configurationCurrentSortingListener() {
        return new ListDataListener() {

            @Override
            public void intervalRemoved(final ListDataEvent e) {
                m_somethingChanged = true;
            }

            @Override
            public void intervalAdded(final ListDataEvent e) {
                m_somethingChanged = true;
            }

            @Override
            public void contentsChanged(final ListDataEvent e) {
                m_somethingChanged = true;
            }
        };

    }

    private void configurationListenerRequested(final ConfigurationRequestEvent searchEvent) {
        switch (searchEvent.getType()) {
            case DELETION:
                DataColumnSpec selectedColumn = m_searchableListPanel.getSelectedColumn();
                if (checkConfigured(selectedColumn)) {
                    removeConfiguration();
                }
                break;
            case SELECTION:
                DataColumnSpec newSpec = m_searchableListPanel.getSelectedColumn();

                if (newSpec != null && newSpec != m_currentColSpec) {

                    storeCurrentList();

                    m_currentSorting.clear();

                    m_currentColSpec = newSpec;

                    addDataCellElements(m_currentColSpec);

                    if (isInvalid(m_currentColSpec)) {
                        m_resetButton.setText("Delete");
                    } else {
                        m_resetButton.setText("Reset");
                    }

                    m_somethingChanged = false;
                }
                break;
            default:
                break;
        }
    }

    private void removeConfiguration() {
        if (m_currentColSpec != null) {
            m_currentSorting.clear();
            m_configuration.removeSorting(m_currentColSpec.getName());

            if (m_searchableListPanel.isAdditionalColumn(m_currentColSpec)) {
                m_searchableListModifier.removeAdditionalColumn(m_currentColSpec.getName());
                m_currentColSpec = null;
            } else {
                Optional<Set<DataCell>> values = m_specToPossibleValuesFunction.apply(m_currentColSpec);
                m_currentSorting.addAll(values.isPresent() ? values.get() : Collections.<DataCell> emptySet());
                m_currentSorting.add(EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL);
            }
            m_searchableListPanel.repaint();

            // Reset also removed incoming chages
            m_somethingChanged = false;
        }
    }

    private void moveDownConfiguration() {
        int[] selectedIndices = m_jlist.getSelectedIndices();
        if (selectedIndices.length > 0) {
            boolean overflows = (selectedIndices[selectedIndices.length - 1] == m_currentSorting.size() - 1);
            moveItems(selectedIndices, 1, overflows, 0, true);
        }
    }

    private void moveUpConfiguration() {
        int[] selectedIndices = m_jlist.getSelectedIndices();
        if (selectedIndices.length > 0) {
            boolean overflows = (selectedIndices[0] == 0);
            int overflowOffset = m_currentSorting.size() - selectedIndices.length;
            moveItems(selectedIndices, -1, overflows, overflowOffset, false);
        }
    }

    /**
     * @param selectedIndices holds the indices of the selected elements
     * @param step defines if the element is being moved up or down
     * @param overflows defines if there are added elements
     * @param boundOffset set to {@code true} while moving down and set to {@code false} while moving up
     */
    private void moveItems(final int[] selectedIndices, final int step, final boolean overflows, final int boundOffset,
        final boolean addToVisibleIndices) {

        TreeMap<Integer, DataCell> toAdd = new TreeMap<>();

        int offset = 0;
        for (int i : selectedIndices) {
            toAdd.put(i, m_currentSorting.remove(i - offset++));
        }

        if (!overflows) {
            for (Map.Entry<Integer, DataCell> entry : toAdd.entrySet()) {
                m_currentSorting.add(entry.getKey() + step, entry.getValue());
            }
            m_jlist.setSelectedIndices(addOffset(selectedIndices, step));
            m_jlist.ensureIndexIsVisible(selectedIndices[addToVisibleIndices ? selectedIndices.length - 1 : 0]);
        } else {
            m_currentSorting.addAll(boundOffset, toAdd.values());
            m_jlist.setSelectedIndices(createAscendingIntArray(boundOffset, selectedIndices.length));
            m_jlist.ensureIndexIsVisible(addToVisibleIndices ? boundOffset : boundOffset + selectedIndices.length - 1);
        }

    }

    private void moveLastConfiguration() {
        int[] selectedIndices = m_jlist.getSelectedIndices();
        insertAtPosition(selectedIndices, m_currentSorting.size() - selectedIndices.length, true);
    }

    private void moveFirstConfiguration() {
        insertAtPosition(m_jlist.getSelectedIndices(), 0, false);
    }

    private void addStringCellConfiguration() {
        String s = (String)JOptionPane.showInputDialog(EditNominalDomainDialog.this.getPanel(), "Value: ",
            "Add Data Cell", JOptionPane.PLAIN_MESSAGE, null, null, "");

        if (s != null && !s.isEmpty() && m_currentColSpec != null) {
            int index = m_jlist.getSelectedIndex() == -1 ? 0 : m_jlist.getSelectedIndex();
            StringCell stringCell = new StringCell(s);

            int lastIndexOf = m_currentSorting.lastIndexOf(stringCell);
            if (lastIndexOf != -1) {
                JOptionPane.showMessageDialog(EditNominalDomainDialog.this.getPanel(),
                    String.format("Value: '%s' does already exist at index: %d", s, lastIndexOf));
            } else {
                m_currentSorting.add(index, stringCell);
                m_configuration.addCreatedValue(m_currentColSpec.getName(), stringCell);
                m_jlist.setSelectedIndices(new int[]{index});
                LOGGER.info("created new value: " + s);
            }
        }
    }

    private void removeStringCellsConfiguration() {
        int[] selectedIndices = m_jlist.getSelectedIndices();

        if (selectedIndices.length > 0) {
            int offset = 0;
            for (int i : selectedIndices) {
                DataCell dataCell = m_currentSorting.get(i - offset);
                if (!EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(dataCell)) {
                    if (m_allowRemovingExistingCells && m_currentSorting.size() > 2) {
                        m_currentSorting.remove(i - offset++);
                        m_configuration.addRemovedValue(m_currentColSpec.getName(), dataCell);
                    } else {
                        // we can only remove cells which are not part of the input table domain.
                        if (!m_orgDomainCells.contains(dataCell)) {
                            m_currentSorting.remove(i - offset++);
                        }
                    }
                }
            }
        }
    }

    private void sortNamesConfiguration(final SortOption b) {
        Collections.sort(m_currentSorting, b);
        m_jlist.ensureIndexIsVisible(0);
    }

    private void insertAtPosition(final int[] selectedIndices, final int offsets, //
        final boolean addLegthToVisibleIndex) {
        if (selectedIndices.length > 0) {
            List<DataCell> toAdd = new ArrayList<>(selectedIndices.length);

            int offset = 0;
            for (int i : selectedIndices) {
                toAdd.add(m_currentSorting.remove(i - offset++));
            }

            m_currentSorting.addAll(offsets, toAdd);

            m_jlist.setSelectedIndices(createAscendingIntArray(offsets, selectedIndices.length));
            m_jlist.ensureIndexIsVisible(addLegthToVisibleIndex ? offsets + selectedIndices.length - 1 : offsets);
        }
    }

    private static int[] addOffset(final int[] selectedIndices, final int offset) {
        for (int i = 0; i < selectedIndices.length; i++) {
            selectedIndices[i] = selectedIndices[i] + offset;
        }
        return selectedIndices;
    }

    private static int[] createAscendingIntArray(final int offset, final int length) {
        int[] toReturn = new int[length];
        for (int i = 0; i < length; i++) {
            toReturn[i] = offset + i;
        }
        return toReturn;
    }

    private boolean checkConfigured(final DataColumnSpec value) {
        return m_configuration.isConfiguredColumn(value.getName());
    }

    private void storeCurrentList() {
        if (m_currentColSpec != null && m_currentSorting != null && !m_currentSorting.isEmpty() && m_somethingChanged) {
            m_configuration.setSorting(m_currentColSpec.getName(), m_currentSorting);
        }
    }

    /**
     * Adds the cells of the columnSpec and potential sorted but not more existing DataCells from the stored
     * configuration.
     *
     * @param columnSpec of the selected column
     */
    private void addDataCellElements(final DataColumnSpec columnSpec) {

        final String columnName = columnSpec.getName();

        List<DataCell> sorting = m_configuration.getSorting(columnName);
        Optional<Set<DataCell>> values = m_specToPossibleValuesFunction.apply(columnSpec);
        m_orgDomainCells =
            values.isPresent() ? Collections.unmodifiableSet(values.get()) : Collections.<DataCell> emptySet();
        if (sorting == null) {
            // there does not exist any sorting, so we just add the original cells and return.
            if (m_orgDomainCells != null) {
                for (DataCell cell : m_orgDomainCells) {
                    m_currentSorting.add(cell);
                }
            }
            m_currentSorting.add(EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL);
        } else {
            // determine the difference set between the original cells and the stored configuration.
            // Add them at the UNKOWN_VALUE_CELL position.
            for (DataCell cell : sorting) {
                if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(cell)) {
                    m_currentSorting.add(cell);
                } else if (m_configuration.isCreatedValue(columnName, cell)) {
                    m_currentSorting.add(cell);
                } else {
                    if (!m_configuration.isRemovedValue(columnName, cell)) {
                        m_currentSorting.add(cell);
                    }
                }
            }
        }
    }

    private void addColumnSpecsForNotExistingColumns() {
        Set<String> configuredColumns = m_configuration.getConfiguredColumns();

        for (String s : configuredColumns) {
            // include the given cell name or if the table has a wrong type.
            final DataColumnSpec columnSpec = m_orgSpec.getColumnSpec(s);
            if (columnSpec == null || !m_classType.isASuperTypeOf(columnSpec.getType())) {
                DataColumnSpec invalidSpec = DataColumnSpecListCellRenderer.createInvalidSpec(s, m_classType);
                if (!m_orgSpec.containsName(invalidSpec.getName())) {
                    m_searchableListModifier.addAdditionalColumn(invalidSpec);
                }
            }
        }
    }

    /**
     * Internal sorting options.
     *
     * @author Marcel Hanser
     */
    private enum SortOption implements Comparator<DataCell> {
            ASCENDING {

                @Override
                public int compare(final DataCell o1, final DataCell o2) {
                    if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o1)) {
                        return 1;
                    } else if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o2)) {
                        return -1;
                    }
                    return StringCell.TYPE.getComparator().compare(o1, o2);
                }

            },
            DESCENDING {

                @Override
                public int compare(final DataCell o1, final DataCell o2) {
                    if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o1)) {
                        return 1;
                    } else if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(o2)) {
                        return -1;
                    }
                    return StringCell.TYPE.getComparator().compare(o2, o1);
                }

            }
    }

    /**
     * Highlights the {@link EditNominalDomainConfiguration#UNKNOWN_VALUES_CELL} cell, self created cells and cells
     * which do not exist in the input table.
     *
     * @author Marcel Hanser
     */
    @SuppressWarnings("serial")
    private class HighlightSpecialCellsRenderer extends DefaultDataValueRenderer {

        private static final String UNKNOWN_CELL_TEXT = "<any unknown new value>";

        /**
         * {@inheritDoc}
         */
        @Override
        public Component getListCellRendererComponent(@SuppressWarnings("rawtypes") final JList list,
            final Object value, final int index, final boolean isSelected, final boolean cellHasFocus) {
            Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);

            assert (c == this);

            if (EditNominalDomainConfiguration.UNKNOWN_VALUES_CELL.equals(value)) {
                setForeground(Color.GRAY);
                setText(UNKNOWN_CELL_TEXT);
            } else if (m_currentColSpec != null && !m_orgDomainCells.contains(value)) {
                // either a created one or an unknown one.
                if (m_configuration.isCreatedValue(m_currentColSpec.getName(), (DataCell)value)) {
                    setForeground(Color.GREEN);
                } else {
                    setForeground(Color.RED);
                }
            }

            return this;
        }
    }

    /**
     * UI creation methods.
     *
     **/
    private static Component createLegend() {
        Box buttonBox = Box.createVerticalBox();
        buttonBox.add(createLabel("Created value", Color.GREEN));
        buttonBox.add(createLabel("Sorted value <br/>which does not exist", Color.RED));
        buttonBox.setBorder(BorderFactory.createTitledBorder("Legend"));
        return buttonBox;
    }

    private static Component createLabel(final String text, final Color color) {
        return new JLabel(String.format(
            "<html><font border='1' bgcolor='rgb(%d,%d,%d)'>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</font>: %s</html>",
            color.getRed(), color.getGreen(), color.getBlue(), text));
    }

    private static JButton createButton(final String string, final ActionListener sortNames,
        final Dimension preferredSize) {
        JButton toReturn = new JButton(string);
        toReturn.addActionListener(sortNames);
        if (preferredSize != null) {
            toReturn.setMinimumSize(preferredSize);
            toReturn.setMaximumSize(preferredSize);
        }
        return toReturn;
    }

    private static JButton createButton(final String string, final ActionListener actionListener) {
        return createButton(string, actionListener, null);
    }

    private JPanel createRadioButtonGroup(final String string) {
        return createRadioButtonGroup(string, "Fail",
            "Ignore column                   " + "                              ");
    }

    private JPanel createRadioButtonGroup(final String string, final String first, final String second) {
        JPanel toReturn = new JPanel(new GridLayout(2, 1));
        toReturn.setBorder(BorderFactory.createTitledBorder(string));
        JRadioButton firstBut = new JRadioButton(first);
        JRadioButton secondBut = new JRadioButton(second);

        m_buttonMap.put(string, secondBut);

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(firstBut);
        buttonGroup.add(secondBut);

        firstBut.setSelected(true);

        toReturn.add(firstBut);
        toReturn.add(secondBut);

        return toReturn;
    }

}
