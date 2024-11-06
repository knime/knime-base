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
 * ---------------------------------------------------------------------
 *
 * History
 *   Sept 17 2008 (mb): created (from wiswedel's TableToVariableNode)
 */
package org.knime.base.node.switches.caseswitch.any;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.knime.base.node.switches.endcase.EndcaseNodeModel;
import org.knime.core.data.DataTable;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.append.AppendedRowsIterator;
import org.knime.core.data.append.AppendedRowsIterator.RuntimeCanceledExecutionException;
import org.knime.core.data.append.AppendedRowsTable;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.PortType;
import org.knime.core.node.port.inactive.InactiveBranchConsumer;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteManager;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.util.ButtonGroupEnumInterface;
import org.knime.core.node.util.CheckUtils;

/**
 * End of an CASE Statement. Takes the data from the first available input port
 *
 * @author Jannik Löscher, KNIME GmbH, Konstanz, Germany
 * @author M. Berthold, University of Konstanz (original {@link EndcaseNodeModel} as a base)
 * @author Tim-Oliver Buchholz, KNIME AG, Zurich, Switzerland (original {@link CaseEndNodeModel} as a base)
 */
final class CaseEndAnyNodeModel extends NodeModel implements InactiveBranchConsumer {

    //// BufferedDataTable ////

    /** NodeSettings key if to append suffix. If false, skip the rows. */
    static final String CFG_APPEND_SUFFIX = "append_suffix";

    /** NodeSettings key: suffix to append. */
    static final String CFG_SUFFIX = "suffix";

    /** NodeSettings key: enable hiliting. */
    static final String CFG_HILITING = "enable_hiliting";

    /** NodeSettings default if to append suffix. If false, skip the rows. */
    static final boolean DEF_APPEND_SUFFIX = true;

    /** NodeSettings default: suffix to append. */
    static final String DEF_SUFFIX = "_dup";

    /** NodeSettings default: enable hiliting. */
    static final boolean DEF_HILITING = false;

    private static final String HILITE_MAPPING_KEY = "hilite_mapping";

    private boolean m_appendSuffix = DEF_APPEND_SUFFIX;

    private String m_suffix = DEF_SUFFIX;

    private boolean m_enableHiliting = DEF_HILITING;

    /** Hilite manager that summarizes both input handlers into one. */
    private final HiLiteManager m_hiliteManager = new HiLiteManager();

    /** Hilite translator for duplicate row keys. */
    private final HiLiteTranslator m_hiliteTranslator = new HiLiteTranslator();

    /** Default hilite handler used if hilite translation is disabled. */
    private final HiLiteHandler m_dftHiliteHandler = new HiLiteHandler();

    //// Any other types ////
    /** NodeSettings key: suffix to append. */
    static final String CFG_HANDLING = "multipleActiveHandling";

    // How to handle multiple active ports
    private MultipleActiveHandling m_handling;

    /**
     * @param inPorts the input ports
     * @param outPorts the output ports
     */
    protected CaseEndAnyNodeModel(final PortType[] inPorts, final PortType[] outPorts) {
        super(inPorts, outPorts);
        m_handling = isInBufferedDataTableMode() ? MultipleActiveHandling.Merge : MultipleActiveHandling.Fail;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
        if (getNrOutPorts() == 0) {
            throw new InvalidSettingsException("Please set an output type!");
        } else if (isInBufferedDataTableMode() && m_handling == MultipleActiveHandling.Merge) {
            return configureBufferedDataTable(inSpecs);
        } else {
            return new PortObjectSpec[]{processDefault(inSpecs, InactiveBranchPortObjectSpec.INSTANCE)};
        }
    }

    private static PortObjectSpec[] configureBufferedDataTable(final PortObjectSpec[] inSpecs)
        throws InvalidSettingsException {
        final var specs = Arrays.stream(inSpecs)//
            .filter(Objects::nonNull)// if connected
            .filter(v -> !(v instanceof InactiveBranchPortObjectSpec))// and active
            .map(v -> (DataTableSpec)v)// cast
            .collect(Collectors.toList()); // and collect

        if (specs.isEmpty()) {
            // all inactive or not connected, return first spec (which must
            // be connected!)
            assert inSpecs[0] instanceof InactiveBranchPortObjectSpec;
            return new PortObjectSpec[]{inSpecs[0]};
        }

        // check compatibility of specs against first spec in list
        if (!specs.stream().allMatch(specs.get(0)::equalStructure)) {
            // incompatible - refuse to configure
            throw new InvalidSettingsException("The table structures of active ports are not compatible.");
        }
        // all ok, return first spec:
        return new PortObjectSpec[]{specs.get(0)};
    }

    private <T> T processDefault(final T[] in, final T inactive) throws InvalidSettingsException {
        final var activePorts = Arrays.stream(in)//
            .filter(Objects::nonNull)// if connected
            .filter(Predicate.not(inactive::equals))// and active (this assumes that T is a singleton)
            .limit(2)// we only need the first to get and the second to check
            .collect(Collectors.toList());
        if (activePorts.size() > 1 && m_handling == MultipleActiveHandling.Fail) {
            throw new InvalidSettingsException("Multiple inputs are active - causing node to fail. "
                + "You can change this behavior in the node configuration dialog.");
        }
        return activePorts.stream().findFirst().orElse(inactive);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected PortObject[] execute(final PortObject[] inObjects, final ExecutionContext exec) throws Exception {
        if (isInBufferedDataTableMode()) {
            return executeBufferedDataTable(inObjects, exec);
        } else {
            return new PortObject[]{processDefault(inObjects, InactiveBranchPortObject.INSTANCE)};
        }
    }

    private PortObject[] executeBufferedDataTable(final PortObject[] inData, final ExecutionContext exec)
        throws InvalidSettingsException, CanceledExecutionException {
        final var tables = Arrays.stream(m_handling == MultipleActiveHandling.Merge// should we merge?
            ? inData// look at complete data
            : new PortObject[]{processDefault(inData, InactiveBranchPortObject.INSTANCE)})// only look at first table (if at all)
            .filter(Objects::nonNull)// if connected
            .filter(v -> !(v instanceof InactiveBranchPortObject))// and active
            .map(v -> (BufferedDataTable)v)// cast
            .collect(Collectors.toList()); // and collect

        if (tables.isEmpty()) {
            // all inactive or not connected, return first PO (which must
            // be connected!)
            if (m_enableHiliting) {
                // create empty hilite translation map (so we correctly
                // handle the internals).
                final var map = new HashMap<RowKey, Set<RowKey>>();
                m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
            }
            return new PortObject[]{inData[0]};
        } else if (tables.size() == 1) {
            return new BufferedDataTable[]{tables.get(0)};
        }

        // check compatibility of specs against first spec in list
        if (!tables.stream().map(BufferedDataTable::getSpec).allMatch(tables.get(0).getSpec()::equalStructure)) {
            // incompatible - refuse to execute
            throw new InvalidSettingsException("The data table structures of the active ports are not compatible.");
        }

        final var totalRowCount = tables.stream().mapToLong(BufferedDataTable::size).sum();
        final var dtables = tables.toArray(new DataTable[0]);

        final var out = new AppendedRowsTable((m_appendSuffix ? m_suffix : null), dtables);
        final var c = exec.createDataContainer(out.getDataTableSpec());

        // note, this iterator throws runtime exceptions when canceled.
        try (final var it = out.iterator(exec, Math.toIntExact(totalRowCount))) {
            appendRows(it, c);

            if (m_enableHiliting) {
                updateHilite(it);
            }
        }

        return new BufferedDataTable[]{c.getTable()};
    }

    private void appendRows(final AppendedRowsIterator it, final BufferedDataContainer c)
        throws CanceledExecutionException {
        try {
            while (it.hasNext()) {
                // may throw exception, also sets progress
                c.addRowToTable(it.next());
            }
        } catch (RuntimeCanceledExecutionException rcee) { // NOSONAR: We are only interested in the actual CancelledException
            throw rcee.getCause();
        } finally {
            c.close();
        }
        if (it.getNrRowsSkipped() > 0) {
            setWarningMessage("Filtered out " + it.getNrRowsSkipped() + " duplicate RowID(s).");
        }
    }

    private void updateHilite(final AppendedRowsIterator it) {
        // create hilite translation map
        final var map = new HashMap<RowKey, Set<RowKey>>();
        // map of all RowKeys and duplicate RowKeys in the resulting table
        final var dupMap = it.getDuplicateNameMap();
        for (final var e : dupMap.entrySet()) {
            // if a duplicate key
            if (!e.getKey().equals(e.getValue())) {
                Set<RowKey> set = Collections.singleton(e.getValue());
                // put duplicate key and original key into map
                map.put(e.getKey(), set);
            } else {
                // skip duplicate keys
                if (!dupMap.containsKey(new RowKey(e.getKey().getString() + m_suffix))) {
                    map.put(e.getKey(), Collections.singleton(e.getValue()));
                }
            }
        }
        m_hiliteTranslator.setMapper(new DefaultHiLiteMapper(map));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        settings.addBoolean(CFG_APPEND_SUFFIX, m_appendSuffix);
        if (m_suffix != null) {
            settings.addString(CFG_SUFFIX, m_suffix);
        }
        settings.addBoolean(CFG_HILITING, m_enableHiliting);
        settings.addString(CFG_HANDLING, m_handling.getActionCommand());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        boolean appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (appendSuffix) {
            String suffix = settings.getString(CFG_SUFFIX);
            if (suffix == null || suffix.equals("")) {
                throw new InvalidSettingsException("Invalid suffix: " + suffix);
            }
        }
        try {
            final var value = MultipleActiveHandling.valueOf(settings.getString(CFG_HANDLING));
            CheckUtils.checkSetting(isInBufferedDataTableMode() || value != MultipleActiveHandling.Merge,
                "Can only merge tables! Please change the input type.");
        } catch (IllegalArgumentException | NullPointerException e) { // NOSONAR: This may be user input and makes the code simpler
            throw new InvalidSettingsException(
                "Invalid constant for multiple-active handling policy: " + settings.getShort(CFG_HANDLING));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_appendSuffix = settings.getBoolean(CFG_APPEND_SUFFIX);
        if (m_appendSuffix) {
            m_suffix = settings.getString(CFG_SUFFIX);
        } else {
            // may be in there, but must not necessarily
            m_suffix = settings.getString(CFG_SUFFIX, m_suffix);
        }
        m_enableHiliting = settings.getBoolean(CFG_HILITING, false);
        m_handling = MultipleActiveHandling.valueOf(settings.getString(CFG_HANDLING));
    }

    private boolean isInBufferedDataTableMode() {
        return getNrOutPorts() > 0 && getOutPortType(0).equals(BufferedDataTable.TYPE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        if (isInBufferedDataTableMode()) {
            m_hiliteManager.removeAllToHiliteHandlers();
            m_hiliteManager.addToHiLiteHandler(m_hiliteTranslator.getFromHiLiteHandler());
            m_hiliteManager.addToHiLiteHandler(getInHiLiteHandler(0));
            m_hiliteTranslator.removeAllToHiliteHandlers();
            m_hiliteTranslator.addToHiLiteHandler(getInHiLiteHandler(1));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (m_enableHiliting && isInBufferedDataTableMode()) {
            try (final var stream =
                new GZIPInputStream(new FileInputStream(new File(nodeInternDir, "hilite_mapping.xml.gz")))) {
                final var config = NodeSettings.loadFromXML(stream);
                if (HILITE_MAPPING_KEY.equals(config.getKey())) {
                    m_hiliteTranslator.setMapper(DefaultHiLiteMapper.load(config));
                }
            } catch (final InvalidSettingsException ex) {
                throw new IOException(ex.getMessage(), ex);
            }
        }
    }

    /**
     * @return the multipleActiveHandlingSettingsModel
     */
    static SettingsModelString createMultipleActiveHandlingSettingsModel() {
        return new SettingsModelString(CFG_HANDLING, MultipleActiveHandling.Fail.getActionCommand());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        if (m_enableHiliting && isInBufferedDataTableMode()) {
            try (final var stream =
                new GZIPOutputStream(new FileOutputStream(new File(nodeInternDir, "hilite_mapping.xml.gz")))) {
                final var mapper = (DefaultHiLiteMapper)m_hiliteTranslator.getMapper();
                final var config = new NodeSettings(mapper == null ? ("no_" + HILITE_MAPPING_KEY) : HILITE_MAPPING_KEY);
                if (mapper != null) {
                    mapper.save(config);
                }
                config.saveToXML(stream);
            }
        }
    }

    /**
     * This does nothing if the node is not in {@link BufferedDataTable} mode
     *
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        if (!isInBufferedDataTableMode()) {
            return;
        }
        super.setInHiLiteHandler(inIndex, hiLiteHdl);
        if (inIndex == 0) {
            m_hiliteManager.removeAllToHiliteHandlers();
            m_hiliteManager.addToHiLiteHandler(m_hiliteTranslator.getFromHiLiteHandler());
            m_hiliteManager.addToHiLiteHandler(hiLiteHdl);
        } else if (inIndex == 1) {
            m_hiliteTranslator.removeAllToHiliteHandlers();
            m_hiliteTranslator.addToHiLiteHandler(hiLiteHdl);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        if (m_enableHiliting && isInBufferedDataTableMode()) {
            return m_hiliteManager.getFromHiLiteHandler();
        } else {
            return m_dftHiliteHandler;
        }
    }

    enum MultipleActiveHandling implements ButtonGroupEnumInterface {

            /** Merge tables (only possible if in BufferedDataTable mode). */
            Merge("Merge tables", "Tries to merge multiple tables", false), //NOSONAR: Keep case for settings backwards compatibility
            /** Fail the execution. */
            Fail("Fail", "Fails during node execution if multiple inputs are active", true), //NOSONAR: Keep case for settings backwards compatibility
            /** Pass on the first active input. */
            UseFirstActive("Use first non-inactive input", "Chooses the top-most active input object", false); //NOSONAR: Keep case for settings backwards compatibility

        /** The options if the node is processing non-BufferedDataTable data. */
        static final MultipleActiveHandling[] OPTIONS_OTHER = {Fail, UseFirstActive};

        private final String m_text;

        private final String m_tooltip;

        private final boolean m_isDefault;

        MultipleActiveHandling(final String text, final String tooltip, final boolean isDefault) {
            m_text = text;
            m_tooltip = tooltip;
            m_isDefault = isDefault;
        }

        /** {@inheritDoc} */
        @Override
        public String getText() {
            return m_text;
        }

        /** {@inheritDoc} */
        @Override
        public String getActionCommand() {
            return name();
        }

        /** {@inheritDoc} */
        @Override
        public String getToolTip() {
            return m_tooltip;
        }

        /** {@inheritDoc} */
        @Override
        public boolean isDefault() {
            return m_isDefault;
        }
    }
}
