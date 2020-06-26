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
import java.io.IOException;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.UnaryOperator;

import org.knime.base.node.preproc.joiner3.Joiner3Settings.ColumnNameDisambiguation;
import org.knime.base.node.preproc.joiner3.Joiner3Settings.CompositionMode;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.RowKey;
import org.knime.core.data.join.JoinImplementation;
import org.knime.core.data.join.JoinSpecification;
import org.knime.core.data.join.JoinSpecification.InputTable;
import org.knime.core.data.join.JoinTableSettings;
import org.knime.core.data.join.JoinerFactory.JoinAlgorithm;
import org.knime.core.data.join.results.JoinResults.OutputCombined;
import org.knime.core.data.join.results.JoinResults.OutputSplit;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.port.PortObject;
import org.knime.core.node.port.PortObjectSpec;
import org.knime.core.node.port.inactive.InactiveBranchPortObject;
import org.knime.core.node.port.inactive.InactiveBranchPortObjectSpec;
import org.knime.core.node.property.hilite.HiLiteHandler;
import org.knime.core.node.property.hilite.HiLiteMapper;
import org.knime.core.node.property.hilite.HiLiteTranslator;

/**
 * This is the model of the joiner node. It delegates the dirty work to the Joiner class.
 *
 * @author Carl Witt, KNIME AG, Zurich, Switzerland
 */
class Joiner3NodeModel extends NodeModel {

    private static final int MATCHES = 0, LEFT_OUTER = 1, RIGHT_OUTER = 2;

    private static final NodeLogger LOGGER = NodeLogger.getLogger(Joiner3NodeModel.class);

    private Joiner3Settings m_settings;

    private Hiliter m_hiliter = new Hiliter();

    Joiner3NodeModel(final Joiner3Settings settings) {
        super(2, 3);
        m_settings = settings;
    }

    @Override
    protected PortObjectSpec[] configure(final PortObjectSpec[] inSpecs) throws InvalidSettingsException {

        JoinSpecification joinSpecification = joinSpecificationForSpecs(inSpecs);
        DataTableSpec matchSpec = joinSpecification.specForMatchTable();

        if (m_settings.getDuplicateHandling() == ColumnNameDisambiguation.DO_NOT_EXECUTE) {
            Optional<String> duplicateColumn = checkForDuplicateColumn(joinSpecification);
            if (duplicateColumn.isPresent()) {
                throw new InvalidSettingsException(String.format("Do not execute on ambiguous column names selected. "
                    + "Column %s appears both in left and right table.", duplicateColumn.get()));
            }
        }

        PortObjectSpec[] portObjectSpecs = new PortObjectSpec[3];
        if (m_settings.isOutputUnmatchedRowsToSeparateOutputPort()) {
            // split output
            portObjectSpecs[MATCHES] = matchSpec;
            portObjectSpecs[LEFT_OUTER] = joinSpecification.specForUnmatched(InputTable.LEFT);
            portObjectSpecs[RIGHT_OUTER] = joinSpecification.specForUnmatched(InputTable.RIGHT);
            return portObjectSpecs;
        } else {
            // single table output
            portObjectSpecs[MATCHES] = matchSpec;
            portObjectSpecs[LEFT_OUTER] = InactiveBranchPortObjectSpec.INSTANCE;
            portObjectSpecs[RIGHT_OUTER] = InactiveBranchPortObjectSpec.INSTANCE;
            return portObjectSpecs;
        }

    }

    @Override
    protected PortObject[] execute(final PortObject[] inPortObjects, final ExecutionContext exec) throws Exception {

        BufferedDataTable left = (BufferedDataTable) inPortObjects[0];
        BufferedDataTable right = (BufferedDataTable) inPortObjects[1];

        JoinSpecification joinSpecification = joinSpecificationForTables(left, right);

        JoinImplementation implementation = JoinAlgorithm.HYBRID_HASH.getFactory().create(joinSpecification, exec);
        implementation.setMaxOpenFiles(m_settings.getMaxOpenFiles());
        implementation.setEnableHiliting(m_settings.isHilitingEnabled());

        if (m_settings.isOutputUnmatchedRowsToSeparateOutputPort()) {
            // split output
            OutputSplit joinedTable = implementation.joinOutputSplit();
            PortObject[] outPortObjects = new PortObject[3];
            outPortObjects[MATCHES] = joinedTable.getMatches();
            outPortObjects[LEFT_OUTER] = joinedTable.getLeftOuter();
            outPortObjects[RIGHT_OUTER] = joinedTable.getRightOuter();
            return outPortObjects;
        } else {
            // single table output
            OutputCombined joinedTable = implementation.joinOutputCombined();
            PortObject[] outPortObjects = new PortObject[3];
            outPortObjects[MATCHES] = joinedTable.getTable();
            outPortObjects[LEFT_OUTER] = InactiveBranchPortObject.INSTANCE;
            outPortObjects[RIGHT_OUTER] = InactiveBranchPortObject.INSTANCE;

            LOGGER.info(String.format("%s rows ⨝ %s rows = %s rows", left.size(), right.size(), joinedTable.getTable().size()));
            return outPortObjects;
        }
    }

    /**
     * Create the join specification from the {@link #m_settings} for the given table specs.
     * @throws InvalidSettingsException
     */
    private JoinSpecification joinSpecificationForSpecs(final PortObjectSpec... portSpecs) throws InvalidSettingsException {

        DataTableSpec left = (DataTableSpec) portSpecs[0];
        DataTableSpec right = (DataTableSpec) portSpecs[1];

        // left (top port) input table
        JoinTableSettings leftSettings = new JoinTableSettings(
            m_settings.getJoinMode().isIncludeLeftUnmatchedRows(),
            m_settings.getLeftJoinColumns(),
            m_settings.getLeftColumnSelectionConfig().applyTo(left).getIncludes(),
            InputTable.LEFT,
            left);

        // right (bottom port) input table
        JoinTableSettings rightSettings = new JoinTableSettings(
            m_settings.getJoinMode().isIncludeRightUnmatchedRows(),
            m_settings.getRightJoinColumns(),
            m_settings.getRightColumnSelectionConfig().applyTo(right).getIncludes(),
            InputTable.RIGHT,
            right);

        BiFunction<DataRow, DataRow, RowKey> rowKeysFactory;
        if (m_settings.isAssignNewRowKeys()) {
            rowKeysFactory = JoinSpecification.createSequenceRowKeysFactory();
        } else {
            rowKeysFactory = JoinSpecification.createConcatRowKeysFactory(m_settings.getRowKeySeparator());
        }

        UnaryOperator<String> columnNameDisambiguator;
        // replace with custom
        if (m_settings.getDuplicateHandling() == ColumnNameDisambiguation.APPEND_SUFFIX) {
            columnNameDisambiguator = s -> s.concat(m_settings.getDuplicateColumnSuffix());
        } else {
            columnNameDisambiguator = s -> s.concat(" (#1)");
        }

        return new JoinSpecification.Builder(leftSettings, rightSettings)
            .conjunctive(m_settings.getCompositionMode() == CompositionMode.MatchAll)
            .outputRowOrder(m_settings.getOutputRowOrder())
            .retainMatched(m_settings.getJoinMode().isIncludeMatchingRows())
            .mergeJoinColumns(m_settings.isMergeJoinColumns())
            .columnNameDisambiguator(columnNameDisambiguator)
            .rowKeyFactory(rowKeysFactory)
            .build();
    }

    /**
     * Throw an {@link InvalidSettingsException} if column names are ambiguous. Used for the
     * @param joinSpecification
     */
    private static Optional<String> checkForDuplicateColumn(final JoinSpecification joinSpecification) {
        DataTableSpec matchTableSpec = joinSpecification.specForMatchTable();
        final String suffix = joinSpecification.getColumnNameDisambiguator().apply("");
        return matchTableSpec.stream()
                // find columns that end with the disambiguator suffix
                .map(DataColumnSpec::getName).filter(s -> s.endsWith(suffix))
                .findAny()
                .map(s -> s.substring(0, s.length() - suffix.length()));
    }

    /**
     * Create join specification for tables by passing their specs to {@link #joinSpecification(PortObjectSpec[])} and
     * setting the data afterwards.
     * @param portObjects {@link BufferedDataTable}s in disguise
     */
    private JoinSpecification joinSpecificationForTables(final BufferedDataTable left, final BufferedDataTable right) throws InvalidSettingsException {

        JoinSpecification joinSpecification = joinSpecificationForSpecs(left.getSpec(), right.getSpec());
        joinSpecification.getSettings(InputTable.LEFT).setTable(left);
        joinSpecification.getSettings(InputTable.RIGHT).setTable(right);

        return joinSpecification;
    }

    @Override
    protected void reset() {
        // called for instance after changing something and confirming in the node dialog
        // or if something goes wrong in the node execution
//        m_hiliter.m_leftTranslator.setMapper(null);
//        m_hiliter.m_rightTranslator.setMapper(null);
    }

    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
//        File settingsFile = new File(nodeInternDir, "joinerInternalSettings");
//
//        try (FileInputStream in = new FileInputStream(settingsFile)) {
//            NodeSettingsRO settings = NodeSettings.loadFromXML(in);
//            NodeSettingsRO leftMapSet = settings.getNodeSettings("leftHiliteMapping");
//            m_hiliter.m_leftTranslator.setMapper(DefaultHiLiteMapper.load(leftMapSet));
//            m_hiliter.m_leftMapper = m_hiliter.m_leftTranslator.getMapper();
//
//            NodeSettingsRO rightMapSet = settings.getNodeSettings("rightHiliteMapping");
//            m_hiliter.m_rightTranslator.setMapper(DefaultHiLiteMapper.load(rightMapSet));
//            m_hiliter.m_rightMapper = m_hiliter.m_rightTranslator.getMapper();
//        } catch (InvalidSettingsException e) {
//            throw new IOException(e);
//        }
    }

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
        Joiner3Settings validationSettings = new Joiner3Settings();
        validationSettings.loadSettings(settings);
        // TODO validation without specs
        //        JoinImplementation.validateSettings(s.getJoinSpecification());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_settings.loadSettingsInModel(settings);
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

        /**
         * {@inheritDoc}
         */
//        @Override
        protected HiLiteHandler getOutHiLiteHandler(final int outIndex) {
            return m_hiliter.m_outHandler;
        }

        /**
         * {@inheritDoc}
         */
//        @Override
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

    }
}
