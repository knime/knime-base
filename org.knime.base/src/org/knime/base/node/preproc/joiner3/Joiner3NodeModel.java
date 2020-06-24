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
 *   27.07.2007 (thor): created
 */
package org.knime.base.node.preproc.joiner3;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiFunction;

import org.knime.base.node.preproc.joiner3.Joiner3Settings.CompositionMode;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinImplementation;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinerFactory.JoinAlgorithm;
import org.knime.core.data.join.results.JoinResults;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettings;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.DefaultHiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteTranslator;
import org.knime.core.node.streamable.InputPortRole;
import org.knime.core.node.streamable.OutputPortRole;

/**
 * This is the model of the joiner node. It delegates the dirty work to the Joiner class.
 *
 * @author Heiko Hofer
 */
class Joiner3NodeModel extends NodeModel {

    private static final int MATCHES = 0, LEFT_OUTER = 1, RIGHT_OUTER = 2;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Joiner3NodeModel.class);

    private Joiner3Settings m_settings;

    private Hiliter m_hiliter = new Hiliter();

    Joiner3NodeModel() {
        super(2, 3);
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {
//        assureSettingsAreInitialized(inSpecs);

        JoinSpecification joinSpecification = joinSpecification(inSpecs);
        DataTableSpec matchSpec = joinSpecification.specForMatchTable();
        LOGGER.info(matchSpec);

        PortObjectSpec[] portObjectSpecs = new PortObjectSpec[3];
        if (m_settings.isOutputUnmatchedRowsToSeparateOutputPort()) {
            portObjectSpecs[MATCHES] = matchSpec;
            portObjectSpecs[LEFT_OUTER] = joinSpecification.specForUnmatched(InputTable.LEFT);
            portObjectSpecs[RIGHT_OUTER] = joinSpecification.specForUnmatched(InputTable.RIGHT);
            return portObjectSpecs;
        } else {
            portObjectSpecs[MATCHES] = matchSpec;
            portObjectSpecs[LEFT_OUTER] = InactiveBranchPortObjectSpec.INSTANCE;
            portObjectSpecs[RIGHT_OUTER] = InactiveBranchPortObjectSpec.INSTANCE;
            return portObjectSpecs;
        }

    }

    @Override
    protected PortObject[] execute(final PortObject[] inPortObjects, final ExecutionContext exec) throws Exception {
//        assureSettingsAreInitialized(new DataTableSpec[]{inData[0].getDataTableSpec(), inData[1].getDataTableSpec()});

        BufferedDataTable inData[] = new BufferedDataTable[] {
          (BufferedDataTable) inPortObjects[0],
          (BufferedDataTable) inPortObjects[1]
        };
        long before = System.currentTimeMillis();

        // if present, provides an upper bound on the number of distinct values
        // if not present, there could be too many values (more than 60 by default) or the column is continuous (e.g., double cell)
        //        inData[0].getSpec().getColumnSpec(0).getDomain().getValues().size();

        // warning and progress messages are directly fed to the execution context
        JoinSpecification joinSpecification = joinSpecification(inData);

        JoinImplementation implementation = JoinAlgorithm.HYBRID_HASH.getFactory().create(joinSpecification, exec);
        implementation.setMaxOpenFiles(m_settings.getMaxOpenFiles());
        implementation.setEnableHiliting(m_settings.isHilitingEnabled());

        JoinResults joinedTable = implementation.join();

        long after = System.currentTimeMillis();

        // FIXME route to output ports
        if (m_settings.isOutputUnmatchedRowsToSeparateOutputPort()) {
            BufferedDataTable[] bufferedDataTables = new BufferedDataTable[3];
            bufferedDataTables[MATCHES] = joinedTable.getMatches();
            bufferedDataTables[LEFT_OUTER] = joinedTable.getLeftOuter();
            bufferedDataTables[RIGHT_OUTER] = joinedTable.getRightOuter();
            LOGGER.info(String.format("%s rows ⨝ %s rows = %s rows (%sms)", inData[0].size(), inData[1].size(),
                bufferedDataTables[MATCHES].size(), after - before));
            LOGGER.info(String.format("%10s left unmatched\n%10s right unmatched",
                bufferedDataTables[LEFT_OUTER].size(), bufferedDataTables[RIGHT_OUTER].size()));
            return bufferedDataTables;
        } else {
            // report
            BufferedDataTable singleTable = joinedTable.getSingleTable();
            LOGGER.info(String.format("%s rows ⨝ %s rows = %s rows (%sms)", inData[0].size(), inData[1].size(),
                singleTable.size(), after - before));
            PortObject[] outPortObjects = new PortObject[3];
            outPortObjects[MATCHES] = singleTable;
            outPortObjects[LEFT_OUTER] = InactiveBranchPortObject.INSTANCE;
            outPortObjects[RIGHT_OUTER] = InactiveBranchPortObject.INSTANCE;

            return outPortObjects;
        }
    }

    /**
     * Create the join specification from the settings for the given table specs.
     */
    private JoinSpecification joinSpecification(final PortObjectSpec[] portSpecs) throws InvalidSettingsException {

        DataTableSpec[] inSpecs = new DataTableSpec[2];
        inSpecs[0] = (DataTableSpec) portSpecs[0];
        inSpecs[1] = (DataTableSpec) portSpecs[1];

        // left (top port) input table
        JoinTableSettings leftSettings = JoinTableSettings.left(
            m_settings.getJoinMode().isIncludeLeftUnmatchedRows(),
            m_settings.getLeftJoinColumns(),
            m_settings.getLeftColumnSelectionConfig().applyTo(inSpecs[0]).getIncludes(),
            inSpecs[0]);

        // right (bottom port) input table
        JoinTableSettings rightSettings = JoinTableSettings.right(
            m_settings.getJoinMode().isIncludeRightUnmatchedRows(),
            m_settings.getRightJoinColumns(),
            m_settings.getRightColumnSelectionConfig().applyTo(inSpecs[1]).getIncludes(),
            inSpecs[1]);

        BiFunction<DataRow, DataRow, RowKey> rowKeysFactory;
        if (m_settings.isAssignNewRowKeys()) {
            rowKeysFactory = JoinSpecification.createSequenceRowKeysFactory();
            LOGGER.info("Create new row keys");
        } else {
            rowKeysFactory = JoinSpecification.createConcatRowKeysFactory(m_settings.getRowKeySeparator());
            LOGGER.info("Concat original row keys");
        }

        LOGGER.info(String.format("m_settings.isMergeJoinColumns()=%s", m_settings.isMergeJoinColumns()));
        return new JoinSpecification.Builder(leftSettings, rightSettings)
            .conjunctive(m_settings.getCompositionMode() == CompositionMode.MatchAll)
            .outputRowOrder(m_settings.getOutputRowOrder())
            .retainMatched(m_settings.getJoinMode().isIncludeMatchingRows())
            .mergeJoinColumns(m_settings.isMergeJoinColumns())
            .columnNameDisambiguator(s -> s.concat(m_settings.getDuplicateColumnSuffix()))
            .rowKeyFactory(rowKeysFactory)
            .build();
    }

    /**
     * Create join specification for tables by passing their specs to {@link #joinSpecification(PortObjectSpec[])} and
     * setting the data afterwards.
     * @param portObjects {@link BufferedDataTable}s in disguise
     */
    private JoinSpecification joinSpecification(final PortObject[] portObjects) throws InvalidSettingsException {
        DataTableSpec[] inSpecs = new DataTableSpec[2];
        inSpecs[0] = (DataTableSpec) portObjects[0].getSpec();
        inSpecs[1] = (DataTableSpec) portObjects[1].getSpec();

        JoinSpecification joinSpecification = joinSpecification(inSpecs);
        joinSpecification.getSettings(InputTable.LEFT).setTable((BufferedDataTable)portObjects[0]);
        joinSpecification.getSettings(InputTable.RIGHT).setTable((BufferedDataTable)portObjects[1]);
        return joinSpecification;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Streaming
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public InputPortRole[] getInputPortRoles() {
        // mark first input port as streamable, all others nonstreamable
        // disable distribution for now (however, the joiner is generally amenable to distribution)
        final InputPortRole[] roles = new InputPortRole[getNrInPorts()];
        Arrays.fill(roles, InputPortRole.NONDISTRIBUTED_NONSTREAMABLE);
        roles[0] = InputPortRole.NONDISTRIBUTED_STREAMABLE;
        return roles;
    }

    @Override
    public OutputPortRole[] getOutputPortRoles() {
        // no distribution, no merging
        final OutputPortRole[] roles = new OutputPortRole[getNrOutPorts()];
        Arrays.fill(roles, OutputPortRole.NONDISTRIBUTED);
        return roles;
    }

    //    @Override
    //    public StreamableOperator createStreamableOperator(final PartitionInfo partitionInfo, final PortObjectSpec[] inSpecs)
    //        throws InvalidSettingsException {
    //        return new StreamableOperator() {
    //
    //            private SimpleStreamableOperatorInternals m_internals;
    //
    //            @Override
    //            public void loadInternals(final StreamableOperatorInternals internals) {
    //                m_internals = (SimpleStreamableOperatorInternals) internals;
    //            }
    //
    //            /**
    //             * The joiner  delegates the streaming to a join implementation.
    //             * <br/>
    //             * {@inheritDoc}
    //             */
    //            @Override
    //            public void runFinal(final PortInput[] inputs, final PortOutput[] outputs, final ExecutionContext exec) throws Exception {
    //                DataTableSpec[] inputSpecs = Arrays.stream(inputs).map(i -> ((RowInput)i).getDataTableSpec()).toArray(DataTableSpec[]::new);
    //                StreamableFunction func = m_joiner.getStreamableFunction(getJoinSpecification(), inputSpecs);
    //                func.runFinal(inputs, outputs, exec);
    //            }
    //
    //            @Override
    //            public StreamableOperatorInternals saveInternals() {
    //                return m_internals;
    //            }
    //        };
    //    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // HiLiting
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
        return m_hiliter.m_outHandler;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setInHiLiteHandler(final int inIndex, final HiLiteHandler hiLiteHdl) {
        if (0 == inIndex) {
            m_hiliter.m_leftTranslator.removeAllToHiliteHandlers();
            m_hiliter.m_leftTranslator = new HiLiteTranslator(hiLiteHdl, m_hiliter.m_leftMapper);
            m_hiliter.m_leftTranslator.addToHiLiteHandler(m_hiliter.m_outHandler);
        } else {
            m_hiliter.m_rightTranslator.removeAllToHiliteHandlers();
            m_hiliter.m_rightTranslator = new HiLiteTranslator(hiLiteHdl, m_hiliter.m_rightMapper);
            m_hiliter.m_rightTranslator.addToHiLiteHandler(m_hiliter.m_outHandler);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        if (m_settings == null) {
            m_settings = new Joiner3Settings();
        }
        m_settings.loadSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_hiliter.m_leftTranslator.setMapper(null);
        m_hiliter.m_rightTranslator.setMapper(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        File settingsFile = new File(nodeInternDir, "joinerInternalSettings");

        try (FileInputStream in = new FileInputStream(settingsFile)) {
            NodeSettingsRO settings = NodeSettings.loadFromXML(in);
            NodeSettingsRO leftMapSet = settings.getNodeSettings("leftHiliteMapping");
            m_hiliter.m_leftTranslator.setMapper(DefaultHiLiteMapper.load(leftMapSet));
            m_hiliter.m_leftMapper = m_hiliter.m_leftTranslator.getMapper();

            NodeSettingsRO rightMapSet = settings.getNodeSettings("rightHiliteMapping");
            m_hiliter.m_rightTranslator.setMapper(DefaultHiLiteMapper.load(rightMapSet));
            m_hiliter.m_rightMapper = m_hiliter.m_rightTranslator.getMapper();
        } catch (InvalidSettingsException e) {
            throw new IOException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
        // TODO restore
        //        NodeSettings internalSettings = new NodeSettings("joiner");
        //        NodeSettingsWO leftMapSet =
        //            internalSettings.addNodeSettings("leftHiliteMapping");
        //        ((DefaultHiLiteMapper) m_leftTranslator.getMapper()).save(leftMapSet);
        //        NodeSettingsWO rightMapSet =
        //            internalSettings.addNodeSettings("rightHiliteMapping");
        //        ((DefaultHiLiteMapper) m_rightTranslator.getMapper()).save(rightMapSet);
        //        File f = new File(nodeInternDir, "joinerInternalSettings");
        //        try(FileOutputStream out = new FileOutputStream(f)){
        //            internalSettings.saveToXML(out);
        //        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_settings.saveSettings(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        Joiner3Settings s = new Joiner3Settings();
        s.loadSettings(settings);
        // TODO validation without specs
        //        JoinImplementation.validateSettings(s.getJoinSpecification());
    }

    private class Hiliter {
        /**
         *
         */
        public HiLiteHandler m_outHandler;

        /**
         *
         */
        public HiLiteTranslator m_rightTranslator;

        /**
         *
         */
        public HiLiteTranslator m_leftTranslator;

        /**
         *
         */
        public HiLiteMapper m_leftMapper;

        /**
         *
         */
        public HiLiteMapper m_rightMapper;

        /**
         *
         */
        public Hiliter() {
            m_outHandler = new HiLiteHandler();
            m_leftTranslator = new HiLiteTranslator();
            m_rightTranslator = new HiLiteTranslator();
        }
    }
}
