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
 *   09.07.2010 (hofer): created
 */
package org.knime.base.node.util.binning;

import java.math.RoundingMode;
import java.util.Optional;

import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DoubleValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;
import org.knime.core.util.binning.auto.BinNaming;
import org.knime.core.util.binning.auto.BinningMethod;
import org.knime.core.util.binning.auto.EqualityMethod;
import org.knime.core.util.binning.auto.OutputFormat;
import org.knime.core.util.binning.auto.PrecisionMode;

/**
 * This class hold the settings required to use {@link AutoBinningUtils}.
 *
 * @since 5.5
 */
public final class AutoBinningSettings {

    private static final String CFG_METHOD = "method";

    private static final String CFG_BIN_COUNT = "binCount";

    private static final String CFG_EQUALITY_METHOD = "equalityMethod";

    private static final String CFG_INTEGER_BOUNDS = "integerBounds";

    private static final String CFG_SAMPLE_QUANTILES = "sampleQuantiles";

    private static final String CFG_BIN_NAMING = "binNaming";

    private static final String CFG_REPLACE_COLUMN = "replaceColumn";

    private static final String CFG_ADVANCED_FORMATTING = "advancedFormatting";

    private static final String CFG_OUTPUT_FORMAT = "outputFormat";

    private static final String CFG_PRECISION = "precision";

    private static final String CFG_PRECISION_MODE = "precisionMode";

    private static final String CFG_ROUNDING_MODE = "roundingMode";

    private BinningMethod m_method = BinningMethod.FIXED_NUMBER;

    private int m_binCount = 5;

    private EqualityMethod m_equalityMethod = EqualityMethod.WIDTH;

    private boolean m_integerBounds = false;

    private double[] m_sampleQuantiles = new double[]{0, 0.25, 0.5, 0.75, 1};

    private BinNaming m_binNaming = BinNaming.NUMBERED;

    private boolean m_replaceColumn = false;

    private boolean m_advancedFormatting = false;

    private OutputFormat m_outputFormat = OutputFormat.STANDARD;

    private int m_precision = 3;

    private PrecisionMode m_precisionMode = PrecisionMode.DECIMAL;

    private RoundingMode m_roundingMode = RoundingMode.HALF_UP;

    private DataColumnSpecFilterConfiguration m_filterConfiguration = createDCSFilterConfiguration();

    private Double m_fixedLowerBound;

    private Double m_fixedUpperBound;

    /**
     * Suffix to be appended to the bin names. If it is null, we use the default value "[Binned]".
     */
    private String m_NameSuffix = null;

    public static final String DEFAULT_NAME_SUFFIX = " [Binned]";

    /**
     * @return the method used when binning the data
     *
     * @since 5.5
     */
    public BinningMethod getMethod() {
        return m_method;
    }

    /**
     * @param method the method to set
     *
     * @since 5.5
     */
    public void setMethod(final BinningMethod method) {
        m_method = method;
    }

    /**
     * @return the binCount
     */
    public int getBinCount() {
        return m_binCount;
    }

    /**
     * @param binCount the binCount to set
     */
    public void setBinCount(final int binCount) {
        m_binCount = binCount;
    }

    /**
     * @return the equalityMethod
     */
    public EqualityMethod getEqualityMethod() {
        return m_equalityMethod;
    }

    /**
     * @param equalityMethod the equalityMethod to set
     */
    public void setEqualityMethod(final EqualityMethod equalityMethod) {
        m_equalityMethod = equalityMethod;
    }

    /**
     * @return the integerBounds
     */
    public boolean getIntegerBounds() {
        return m_integerBounds;
    }

    /**
     * @param integerBounds the integerBounds to set
     */
    public void setIntegerBounds(final boolean integerBounds) {
        m_integerBounds = integerBounds;
    }

    /**
     * @return the sampleQuantiles
     */
    public double[] getSampleQuantiles() {
        return m_sampleQuantiles;
    }

    /**
     * @param sampleQuantiles the sampleQuantiles to set
     */
    public void setSampleQuantiles(final double[] sampleQuantiles) {
        m_sampleQuantiles = sampleQuantiles;
    }

    /**
     * @return the binNaming
     */
    public BinNaming getBinNaming() {
        return m_binNaming;
    }

    /**
     * @param binNaming the binNaming to set
     */
    public void setBinNaming(final BinNaming binNaming) {
        m_binNaming = binNaming;
    }

    /**
     * @return the replaceColumn
     */
    public boolean getReplaceColumn() {
        return m_replaceColumn;
    }

    /**
     * @param replaceColumn the replaceColumn to set
     */
    public void setReplaceColumn(final boolean replaceColumn) {
        m_replaceColumn = replaceColumn;
    }

    /**
     * @return the advancedFormatting
     */
    public boolean getAdvancedFormatting() {
        return m_advancedFormatting;
    }

    /**
     * @param advancedFormatting the advancedFormatting to set
     */
    public void setAdvancedFormatting(final boolean advancedFormatting) {
        m_advancedFormatting = advancedFormatting;
    }

    /**
     * @return the outputFormat
     */
    public OutputFormat getOutputFormat() {
        return m_outputFormat;
    }

    /**
     * @param outputFormat the outputFormat to set
     */
    public void setOutputFormat(final OutputFormat outputFormat) {
        m_outputFormat = outputFormat;
    }

    /**
     * @return the precision
     */
    public int getPrecision() {
        return m_precision;
    }

    /**
     * @param precision the precision to set
     */
    public void setPrecision(final int precision) {
        m_precision = precision;
    }

    /**
     * @return the precisionMode
     */
    public PrecisionMode getPrecisionMode() {
        return m_precisionMode;
    }

    /**
     * @param precisionMode the precisionMode to set
     */
    public void setPrecisionMode(final PrecisionMode precisionMode) {
        m_precisionMode = precisionMode;
    }

    /**
     * @return the roundingMode
     */
    public RoundingMode getRoundingMode() {
        return m_roundingMode;
    }

    /**
     * @param roundingMode the roundingMode to set
     */
    public void setRoundingMode(final RoundingMode roundingMode) {
        m_roundingMode = roundingMode;
    }

    /**
     * @return the fixed lower bound
     */
    public Optional<Double> getFixedLowerBound() {
        return Optional.ofNullable(m_fixedLowerBound);
    }

    /**
     * Sets a fixed lower bound of the first bin instead of using the domain minimum.
     *
     * @param fixedLowerBound the fixed lower bound of the first bin
     */
    public void setFixedLowerBound(final Double fixedLowerBound) {
        m_fixedLowerBound = fixedLowerBound;
    }

    /**
     * @return the fixed upper bound
     */
    public Optional<Double> getFixedUpperBound() {
        return Optional.ofNullable(m_fixedUpperBound);
    }

    /**
     * @return the name suffix to be appended to the bin names
     */
    public Optional<String> getNameSuffix() {
        return Optional.ofNullable(m_NameSuffix);
    }

    /**
     * Sets the name suffix to be appended to the bin names.
     *
     * @param nameSuffix the name suffix to be appended to the bin names
     */
    public void setNameSuffix(final String nameSuffix) {
        m_NameSuffix = nameSuffix;
    }

    /**
     * Sets a fixed upper bound of the last bin instead of using the domain maximum.
     *
     * @since 5.4
     * @param fixedUpperBound the fixed upper bound of the last bin
     */
    public void setFixedUpperBound(final Double fixedUpperBound) {
        m_fixedUpperBound = fixedUpperBound;
    }

    /**
     * Loads the settings from the node settings object.
     *
     * @param settings a node settings object
     * @throws InvalidSettingsException if some settings are missing
     */
    public void loadSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration config = createDCSFilterConfiguration();
        config.loadConfigurationInModel(settings);
        m_filterConfiguration = config;
        m_method = BinningMethod.valueOf(camelToScreamingSnake(settings.getString(CFG_METHOD)));
        m_binCount = settings.getInt(CFG_BIN_COUNT);
        m_equalityMethod = EqualityMethod.valueOf(camelToScreamingSnake(settings.getString(CFG_EQUALITY_METHOD)));
        m_integerBounds = settings.getBoolean(CFG_INTEGER_BOUNDS);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES);
        m_binNaming = BinNaming.valueOf(camelToScreamingSnake(settings.getString(CFG_BIN_NAMING)));
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN);
        m_advancedFormatting = settings.getBoolean(CFG_ADVANCED_FORMATTING);
        m_outputFormat = OutputFormat.valueOf(camelToScreamingSnake(settings.getString(CFG_OUTPUT_FORMAT)));
        m_precision = settings.getInt(CFG_PRECISION);
        m_precisionMode = PrecisionMode.valueOf(camelToScreamingSnake(settings.getString(CFG_PRECISION_MODE)));
        m_roundingMode = RoundingMode.valueOf(settings.getString(CFG_ROUNDING_MODE));
    }

    /**
     * Loads the settings from the node settings object using default values if some settings are missing.
     *
     * @param settings a node settings object
     * @param spec the data table specification to use for the filter configuration
     */
    public void loadSettingsForDialog(final NodeSettingsRO settings, final DataTableSpec spec) {
        DataColumnSpecFilterConfiguration config = createDCSFilterConfiguration();
        config.loadConfigurationInDialog(settings, spec);
        m_filterConfiguration = config;

        m_method = Optional.ofNullable(settings.getString(CFG_METHOD, null)) //
            .map(AutoBinningSettings::screamingSnakeToCamel) //
            .map(BinningMethod::valueOf) //
            .orElse(BinningMethod.FIXED_NUMBER);
        m_binCount = settings.getInt(CFG_BIN_COUNT, 5);
        m_equalityMethod = Optional.ofNullable(settings.getString(CFG_EQUALITY_METHOD, null)) //
            .map(AutoBinningSettings::camelToScreamingSnake) //
            .map(EqualityMethod::valueOf) //
            .orElse(EqualityMethod.WIDTH);
        m_integerBounds = settings.getBoolean(CFG_INTEGER_BOUNDS, false);
        m_sampleQuantiles = settings.getDoubleArray(CFG_SAMPLE_QUANTILES, new double[]{0, 0.25, 0.5, 0.75, 1});
        m_binNaming = Optional.ofNullable(settings.getString(CFG_BIN_NAMING, null)) //
            .map(AutoBinningSettings::camelToScreamingSnake) //
            .map(BinNaming::valueOf) //
            .orElse(BinNaming.NUMBERED);
        m_replaceColumn = settings.getBoolean(CFG_REPLACE_COLUMN, false);
        m_advancedFormatting = settings.getBoolean(CFG_ADVANCED_FORMATTING, false);
        m_outputFormat = Optional.ofNullable(settings.getString(CFG_OUTPUT_FORMAT, null)) //
            .map(AutoBinningSettings::camelToScreamingSnake) //
            .map(OutputFormat::valueOf) //
            .orElse(OutputFormat.STANDARD);
        m_precision = settings.getInt(CFG_PRECISION, 3);
        m_precisionMode = Optional.ofNullable(settings.getString(CFG_PRECISION_MODE, null)) //
            .map(AutoBinningSettings::camelToScreamingSnake) //
            .map(PrecisionMode::valueOf) //
            .orElse(PrecisionMode.DECIMAL);
        m_roundingMode = RoundingMode.valueOf(settings.getString(CFG_ROUNDING_MODE, RoundingMode.HALF_UP.name()));
    }

    /**
     * Needed when saving the settings, since the old settings used camelCase and the new enums use
     * SCREAMING_SNAKE_CASE.
     *
     * @param screamingSnake a string in screaming snake case (e.g. "SOME_CONSTANT")
     * @return the string in camel case (e.g. "someConstant")
     */
    private static String screamingSnakeToCamel(final String screamingSnake) {
        var camelCase = new StringBuilder();
        boolean nextUpper = false;
        for (char c : screamingSnake.toCharArray()) {
            if (c == '_') {
                nextUpper = true;
            } else {
                camelCase.append(nextUpper ? Character.toUpperCase(c) : Character.toLowerCase(c));
                nextUpper = false;
            }
        }
        return camelCase.toString();
    }

    /**
     * Needed when loading the settings, since the old settings used camelCase and the new enums use
     * SCREAMING_SNAKE_CASE.
     *
     * @param camelCase a string in camel case (e.g. "someConstant")
     * @return the string in screaming snake case (e.g. "SOME_CONSTANT")
     */
    private static String camelToScreamingSnake(final String camelCase) {
        var screamingSnake = new StringBuilder();
        for (char c : camelCase.toCharArray()) {
            if (Character.isUpperCase(c) && screamingSnake.length() > 0) {
                screamingSnake.append('_');
            }
            screamingSnake.append(Character.toUpperCase(c));
        }
        return screamingSnake.toString();
    }

    /**
     * Saves the settings into the node settings object.
     *
     * @param settings a node settings object
     */
    public void saveSettings(final NodeSettingsWO settings) {
        m_filterConfiguration.saveConfiguration(settings);
        settings.addString(CFG_METHOD, screamingSnakeToCamel(m_method.name()));
        settings.addInt(CFG_BIN_COUNT, m_binCount);
        settings.addString(CFG_EQUALITY_METHOD, screamingSnakeToCamel(m_equalityMethod.name()));
        settings.addBoolean(CFG_INTEGER_BOUNDS, m_integerBounds);
        settings.addDoubleArray(CFG_SAMPLE_QUANTILES, m_sampleQuantiles);
        settings.addString(CFG_BIN_NAMING, screamingSnakeToCamel(m_binNaming.name()));
        settings.addBoolean(CFG_REPLACE_COLUMN, m_replaceColumn);
        settings.addBoolean(CFG_ADVANCED_FORMATTING, m_advancedFormatting);
        settings.addString(CFG_OUTPUT_FORMAT, screamingSnakeToCamel(m_outputFormat.name()));
        settings.addInt(CFG_PRECISION, m_precision);
        settings.addString(CFG_PRECISION_MODE, screamingSnakeToCamel(m_precisionMode.name()));
        settings.addString(CFG_ROUNDING_MODE, m_roundingMode.name());
    }

    /**
     * @param config a filter configuration
     */
    public void setFilterConfiguration(final DataColumnSpecFilterConfiguration config) {
        m_filterConfiguration = config;

    }

    /**
     * @return filter configuration
     */
    public DataColumnSpecFilterConfiguration getFilterConfiguration() {
        return m_filterConfiguration;
    }

    /**
     * A new configuration to store the settings. Only Columns of Type String are available.
     *
     * @return filter configuration
     */
    public static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter", new InputFilter<DataColumnSpec>() {

            @Override
            public boolean include(final DataColumnSpec name) {
                return name.getType().isCompatible(DoubleValue.class);
            }
        });
    }

}
