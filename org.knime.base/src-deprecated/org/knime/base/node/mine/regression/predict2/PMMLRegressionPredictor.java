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
 *   6 Nov 2019 (Alexander): created
 */
package org.knime.base.node.mine.regression.predict2;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.knime.base.node.mine.regression.PMMLRegressionTranslator;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator.NumericPredictor;
import org.knime.base.node.mine.regression.PMMLRegressionTranslator.RegressionTable;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.FunctionName;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionContent.ModelType;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLGeneralRegressionTranslator;
import org.knime.base.node.mine.regression.pmmlgreg.PMMLPredictor;
import org.knime.base.predict.PMMLClassificationPredictorOptions;
import org.knime.base.predict.PMMLTablePredictor;
import org.knime.base.predict.PredictorContext;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DoubleValue;
import org.knime.core.data.NominalValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.vector.bitvector.BitVectorValue;
import org.knime.core.data.vector.bytevector.ByteVectorValue;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.port.pmml.PMMLPortObject;
import org.knime.core.node.port.pmml.PMMLPortObjectSpec;
import org.knime.core.node.util.CheckUtils;
import org.knime.core.pmml.PMMLModelType;
import org.w3c.dom.Node;

/**
 * Class for predicting PMML regression models.
 *
 * @author Alexander Fillbrunn, KNIME GmbH, Konstanz, Germany
 * @since 4.1
 */
public final class PMMLRegressionPredictor implements PMMLTablePredictor {

    /**
     * The pattern covariates have to satisfy.
     */
    private static final Pattern COVARIATE_PATTERN = Pattern.compile("(.*)\\[\\d+\\]");

    private static final String NO_REG_MODEL_FOUND_MSG = "No Regression Model found.";

    /** The node logger for this class. */
    private static final NodeLogger LOGGER = NodeLogger.getLogger(PMMLRegressionPredictor.class);

    private PMMLClassificationPredictorOptions m_options;

    /**
     * Creates a new instance of {@code RegressionPredictor}.
     *
     * @param options the options determining the predictor output. These are {@link PMMLClassificationPredictorOptions}
     *            because it might be a logistic regression. Probabilities are not output for other types of regression.
     */
    public PMMLRegressionPredictor(final PMMLClassificationPredictorOptions options) {
        m_options = options;
    }

    @Override
    public BufferedDataTable predict(final BufferedDataTable input, final PMMLPortObject model,
        final PredictorContext ctx) throws Exception {
        ExecutionContext exec = ctx.getExecutionContext();
        RegressionPredictorSettings settings = createSettings(m_options);
        List<Node> models = model.getPMMLValue().getModels(PMMLModelType.GeneralRegressionModel);
        if (models.isEmpty()) {
            LOGGER.warn("No regression models in the input PMML.");
            List<Node> regmodels = model.getPMMLValue().getModels(PMMLModelType.RegressionModel);
            if (regmodels.isEmpty()) {
                LOGGER.error(NO_REG_MODEL_FOUND_MSG);
                throw new RuntimeException(NO_REG_MODEL_FOUND_MSG);//NOSONAR callers my catch for this exception type
            }
            PMMLRegressionTranslator trans = new PMMLRegressionTranslator();
            model.initializeModelTranslator(trans);

            DataTableSpec spec = input.getDataTableSpec();
            ColumnRearranger c = createRearranger1(spec, model.getSpec(), trans);
            BufferedDataTable out = exec.createColumnRearrangeTable(input, c, exec);
            return adjustSpecOfRegressionPredictorTable(out, input, model, ctx, settings);
        }
        PMMLGeneralRegressionTranslator trans = new PMMLGeneralRegressionTranslator();
        model.initializeModelTranslator(trans);

        DataTableSpec spec = input.getDataTableSpec();
        ColumnRearranger c = createRearranger(trans.getContent(), model.getSpec(), spec, settings);
        return exec.createColumnRearrangeTable(input, c, exec);
    }

    private BufferedDataTable adjustSpecOfRegressionPredictorTable(final BufferedDataTable table,
        final BufferedDataTable input, final PMMLPortObject model, final PredictorContext ctx,
        final RegressionPredictorSettings settings) throws InvalidSettingsException {
        String predColumn = determinePredictedColumName(input, model, settings, ctx);
        if (predColumn != null) {
            DataColumnSpec[] colSpecs = getColumnSpecs(table.getSpec());
            colSpecs[colSpecs.length - 1] = replaceNameOf(colSpecs[colSpecs.length - 1], predColumn);
            return ctx.getExecutionContext().createSpecReplacerTable(table, new DataTableSpec(colSpecs));
        } else {
            return table;
        }
    }

    private static DataColumnSpec replaceNameOf(final DataColumnSpec colSpec, final String name) {
        DataColumnSpecCreator creator = new DataColumnSpecCreator(colSpec);
        creator.setName(name);
        return creator.createSpec();
    }

    private static DataColumnSpec[] getColumnSpecs(final DataTableSpec spec) {
        DataColumnSpec[] colSpecs = new DataColumnSpec[spec.getNumColumns()];
        for (int i = 0; i < spec.getNumColumns(); i++) {
            colSpecs[i] = spec.getColumnSpec(i);
        }
        return colSpecs;
    }

    private String determinePredictedColumName(final BufferedDataTable input, final PMMLPortObject model,
        final RegressionPredictorSettings settings, final PredictorContext ctx) throws InvalidSettingsException {
        DataTableSpec outSpec = getOutputSpec(input.getDataTableSpec(), model.getSpec(), ctx);
        if (outSpec != null) {
            return outSpec.getColumnSpec(outSpec.getNumColumns() - 1).getName();
        } else {
            return null;
        }
    }

    @Override
    public DataTableSpec getOutputSpec(final DataTableSpec inputSpec, final PMMLPortObjectSpec modelSpec,
        final PredictorContext ctx) throws InvalidSettingsException {
        RegressionPredictorSettings settings = createSettings(m_options);
        if (modelSpec.getTargetCols().get(0).getType().isCompatible(DoubleValue.class)
            && settings.getIncludeProbabilities()) {
            ctx.setWarningMessage("The option \"Append columns with predicted probabilities\""
                + " has only an effect for nominal targets");
        }
        if (null != RegressionPredictorCellFactory.createColumnSpec(modelSpec, inputSpec, settings)) {
            ColumnRearranger c = new ColumnRearranger(inputSpec);
            c.append(new RegressionPredictorCellFactory(modelSpec, inputSpec, settings) {
                @Override
                public DataCell[] getCells(final DataRow row) {
                    // not called during configure
                    return null;
                }
            });
            return c.createSpec();
        } else {
            return null;
        }
    }

    private static ColumnRearranger createRearranger(final PMMLGeneralRegressionContent content,
        final PMMLPortObjectSpec pmmlSpec, final DataTableSpec inDataSpec, final RegressionPredictorSettings settings)
        throws InvalidSettingsException {
        validateContent(content, inDataSpec);

        ColumnRearranger c = new ColumnRearranger(inDataSpec);
        if (content.getModelType() == ModelType.generalLinear) {
            c.append(new LinReg2Predictor(content, inDataSpec, pmmlSpec, pmmlSpec.getTargetFields().get(0), settings));
        } else {
            c.append(new LogRegPredictor(content, inDataSpec, pmmlSpec, pmmlSpec.getTargetFields().get(0), settings));
        }
        return c;
    }

    private static void validateContent(final PMMLGeneralRegressionContent content, final DataTableSpec inDataSpec)
        throws InvalidSettingsException {
        if (content == null) {
            throw new InvalidSettingsException("No input");
        }
        validateModelType(content);
        validateFactors(content, inDataSpec);
        validateCovariates(content, inDataSpec);
    }

    private static void validateModelType(final PMMLGeneralRegressionContent content) throws InvalidSettingsException {
        // the predictor can only predict linear regression models
        final ModelType modelType = content.getModelType();
        if (!(modelType == ModelType.multinomialLogistic
            || modelType == ModelType.generalLinear)) {
            throw new InvalidSettingsException("Model Type: " + modelType + " is not supported.");
        }
        if (modelType == ModelType.generalLinear
            && content.getFunctionName() != FunctionName.regression) {
            throw new InvalidSettingsException(
                "Function Name: " + content.getFunctionName() + " is not supported for linear regression.");
        }
        if (modelType == ModelType.multinomialLogistic
            && content.getFunctionName() != FunctionName.classification) {
            throw new InvalidSettingsException(
                "Function Name: " + content.getFunctionName() + " is not supported for logistic regression.");
        }
    }

    private static void validateFactors(final PMMLGeneralRegressionContent content, final DataTableSpec inDataSpec)
        throws InvalidSettingsException {
        // check if all factors are in the given data table and that they
        // are nominal values
        for (PMMLPredictor factor : content.getFactorList()) {
            DataColumnSpec columnSpec = inDataSpec.getColumnSpec(factor.getName());
            if (null == columnSpec) {
                throw new InvalidSettingsException(
                    "The column \"" + factor.getName() + "\" is in the model but not in given table.");
            }
            if (!columnSpec.getType().isCompatible(NominalValue.class)) {
                throw new InvalidSettingsException(
                    "The column \"" + factor.getName() + "\" is supposed to be nominal.");
            }
        }
    }

    private static void validateCovariates(final PMMLGeneralRegressionContent content, final DataTableSpec inDataSpec)
            throws InvalidSettingsException {
            // check if all covariates are in the given data table and that they
            // are numeric values
            for (PMMLPredictor covariate : content.getCovariateList()) {
                final String covariateName = covariate.getName();
                DataColumnSpec columnSpec = inDataSpec.getColumnSpec(covariateName);
                if (null == columnSpec) {
                    // in case the learning column was a vector the vector position is appended as [0], [1] and so on
                    columnSpec = inDataSpec.getColumnSpec(extractVectorName(covariateName));
                    CheckUtils.checkSetting(columnSpec != null, "The column \"%s\" is in the model but not in given table.",
                        covariateName);
                }
                CheckUtils.checkSetting(!isColumnNumeric(content, columnSpec),
                    "The column \"%s\" is supposed to be numeric.", covariateName);
            }
        }

    private static String extractVectorName(final String covariateName) {
        final Matcher matcher = COVARIATE_PATTERN.matcher(covariateName);
        if (matcher.matches()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }

        private static boolean isColumnNumeric(final PMMLGeneralRegressionContent content,
            final DataColumnSpec columnSpec) {
            final DataType type = columnSpec.getType();
            return type.isCompatible(DoubleValue.class)//
                || (isColumnVector(content, columnSpec)//
                    && (isDoubleList(type)//
                        || type.isCompatible(BitVectorValue.class)//
                        || type.isCompatible(ByteVectorValue.class)));
        }

        private static boolean isColumnVector(final PMMLGeneralRegressionContent content, final DataColumnSpec columnSpec) {
            return content.getVectorLengths().containsKey(columnSpec.getName());
        }

        private static boolean isDoubleList(final DataType type) {
            return type.isCollectionType()//
                    && type.getCollectionElementType().isCompatible(DoubleValue.class);
        }

    private static ColumnRearranger createRearranger1(final DataTableSpec inSpec, final PMMLPortObjectSpec regModelSpec,
        final PMMLRegressionTranslator regModel) throws InvalidSettingsException {
        if (regModelSpec == null) {
            throw new InvalidSettingsException("No input");
        }

        String targetCol = getTargetCol(regModelSpec);

        final List<String> learnFields = getLearnFields(regModelSpec, regModel);

        final int[] colIndices = findColIndices(inSpec, learnFields);
        DataColumnSpec newCol = createPredictionColSpec(inSpec, targetCol);

        // during regModel may be null during configuration but in that case the getCell method isn't called
        // therefore it is save to pass null for the regression table.
        SingleCellFactory fac =
            new RegPredictorCellFactory(newCol, regModel != null ? regModel.getRegressionTable() : null, colIndices);
        ColumnRearranger c = new ColumnRearranger(inSpec);
        c.append(fac);
        return c;
    }

    private static String getTargetCol(final PMMLPortObjectSpec regModelSpec) {
        // exclude last (response column)
        final List<String> targetFields = regModelSpec.getTargetFields();
        if (targetFields != null && !targetFields.isEmpty()) {
            return targetFields.get(0);
        } else {
            return "Response";
        }
    }

    private static List<String> getLearnFields(final PMMLPortObjectSpec regModelSpec,
        final PMMLRegressionTranslator regModel) {
        final List<String> learnFields;
        if (regModel != null) {
            RegressionTable regTable = regModel.getRegressionTable();
            learnFields = new ArrayList<>();
            for (NumericPredictor p : regTable.getVariables()) {
                learnFields.add(p.getName());
            }
        } else {
            learnFields = new ArrayList<>(regModelSpec.getLearningFields());
        }
        return learnFields;
    }

    private static int[] findColIndices(final DataTableSpec inSpec, final List<String> learnFields)
            throws InvalidSettingsException {
            final int[] colIndices = new int[learnFields.size()];
            int k = 0;
            for (String learnCol : learnFields) {
                int index = inSpec.findColumnIndex(learnCol);
                if (index < 0) {
                    throw new InvalidSettingsException("Missing column for regressor variable : \"" + learnCol + "\"");
                }
                DataColumnSpec regressor = inSpec.getColumnSpec(index);
                String name = regressor.getName();
                DataColumnSpec col = inSpec.getColumnSpec(index);
                if (!col.getType().isCompatible(DoubleValue.class)) {
                    throw new InvalidSettingsException("Incompatible type of column \"" + name + "\": " + col.getType());
                }

                colIndices[k] = index;
                k++;
            }
            return colIndices;
        }

    private static DataColumnSpec createPredictionColSpec(final DataTableSpec inSpec, final String targetCol) {
        // try to use some smart naming scheme for the append column
        String oldName = targetCol;
        if (inSpec.containsName(oldName) && !oldName.toLowerCase().endsWith("(prediction)")) {
            oldName = oldName + " (prediction)";
        }
        String newColName = DataTableSpec.getUniqueColumnName(inSpec, oldName);
        return new DataColumnSpecCreator(newColName, DoubleCell.TYPE).createSpec();
    }

    private static class RegPredictorCellFactory extends SingleCellFactory {

        private final RegressionTable m_regressionTable;
        private final int[] m_colIndices;

        RegPredictorCellFactory(final DataColumnSpec colSpec, final RegressionTable regressionTable,
            final int[] colIndices) {
            super(colSpec);
            m_regressionTable = regressionTable;
            m_colIndices = colIndices;
        }

        @Override
        public DataCell getCell(final DataRow row) {
            int j = 0;
            double result = m_regressionTable.getIntercept();
            for (NumericPredictor p : m_regressionTable.getVariables()) {
                DataCell c = row.getCell(m_colIndices[j]);
                j++;
                if (c.isMissing()) {
                    return DataType.getMissingCell();
                }
                double v = ((DoubleValue)c).getDoubleValue();
                if (p.getExponent() != 1) {
                    v = Math.pow(v, p.getExponent());
                }
                result += p.getCoefficient() * v;
            }
            return new DoubleCell(result);
        }

    }

    private static RegressionPredictorSettings createSettings(final PMMLClassificationPredictorOptions options) {
        RegressionPredictorSettings s = new RegressionPredictorSettings();
        s.setCustomPredictionName(options.getPredictionColumnName());
        s.setHasCustomPredictionName(options.hasCustomPredictionColumnName());
        s.setPropColumnSuffix(options.getPropColumnSuffix());
        s.setIncludeProbabilities(options.includeClassProbabilities());
        return s;
    }
}
