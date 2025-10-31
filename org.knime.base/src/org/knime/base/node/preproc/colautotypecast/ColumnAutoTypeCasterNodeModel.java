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
 */
package org.knime.base.node.preproc.colautotypecast;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalQuery;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Optional;
import java.util.TimeZone;

import org.knime.base.node.io.filereader.DataCellFactory;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataTypeRegistry;
import org.knime.core.data.RowKey;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.date.DateAndTimeCell;
import org.knime.core.data.def.DefaultRow;
import org.knime.core.data.def.DoubleCell;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.LongCell;
import org.knime.core.data.def.StringCell;
import org.knime.core.node.BufferedDataContainer;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.CanceledExecutionException;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.ExecutionMonitor;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.NodeModel;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.util.ConvenienceMethods;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.core.node.util.filter.NameFilterConfiguration;
import org.knime.core.node.util.filter.column.DataColumnSpecFilterConfiguration;

/**
 * The column type changer node model which converts a string column to a numeric or date-type column iff all
 * column-entries could be converted.
 *
 * @author Tim-Oliver Buchholz, University of Konstanz
 */
public class ColumnAutoTypeCasterNodeModel extends NodeModel {

    static final String CFGKEY_DATEFORMAT = "dateFormat";

    static final String CFGKEY_QUICKSANBOOLEAN = "doAQuickScan";

    static final String CFGKEY_QUICKSCANROWS = "numberOfRowsForQuickScan";

    static final String CFGKEY_MISSVALPAT = "missingValuePattern";

    static final String CFGKEY_USELEGACYTYPENAMES = "useLegacyTypeNames";

    static final String CFGKEY_USE_LEGACY_DATE_TIME_TYPE = "useLegacyDateTimeType";

    static final String MISSVALDESC_NONE = "<none>";

    static final String MISSVALDESC_EMPTY = "<empty>";

    private DataColumnSpecFilterConfiguration m_conf;

    private String m_dateFormat = "dd.MM.yy";

    private String[][] m_reasons;

    private boolean m_quickScan = false;

    private int m_numberOfRows = 1000;

    private String m_missValPat = null;

    // added in AP-23571 (5.5)
    private boolean m_useLegacyTypeNames = false;

    // added in AP-24883 (5.8)
    private boolean m_useLegacyDateTimeType;

    private static final Optional<DataType> LOCAL_DATE_TIME_TYPE;

    private static final Optional<DataType> ZONED_DATE_TIME_TYPE;

    private static final Optional<DataType> LOCAL_TIME_TYPE;

    private static final Optional<DataType> LOCAL_DATE_TYPE;

    static {
        /**
         * We cannot refer to those classes directly as the do not live in org.knime.core, but in org.knime.time which
         * has a dependency to org.knime.base -> would introduce a cycle
         */
        final var dateTimeTypes = DataTypeRegistry.getInstance().availableDataTypes().stream() //
            .filter(dataType -> dataType.getCellClass().getName().startsWith("org.knime.core.data.time")).toList();
        LOCAL_DATE_TIME_TYPE = dateTimeTypes.stream().filter(dataType -> dataType.getCellClass().getName()
            .equals("org.knime.core.data.time.localdatetime.LocalDateTimeCell")).findFirst();
        ZONED_DATE_TIME_TYPE = dateTimeTypes.stream().filter(dataType -> dataType.getCellClass().getName()
            .equals("org.knime.core.data.time.zoneddatetime.ZonedDateTimeCell")).findFirst();
        LOCAL_DATE_TYPE = dateTimeTypes.stream().filter(
            dataType -> dataType.getCellClass().getName().equals("org.knime.core.data.time.localdate.LocalDateCell"))
            .findFirst();
        LOCAL_TIME_TYPE = dateTimeTypes.stream().filter(
            dataType -> dataType.getCellClass().getName().equals("org.knime.core.data.time.localtime.LocalTimeCell"))
            .findFirst();
    }

    /**
     * Creates a new node model with one in- and outport.
     */
    public ColumnAutoTypeCasterNodeModel() {
        super(1, 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        // no op
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        double progress = 0;
        final BufferedDataTable data = inData[0];
        BufferedDataTable outTable = inData[0];
        final String[] incls = m_conf.applyTo(data.getDataTableSpec()).getIncludes();
        final DataType[] types = new DataType[incls.length];
        final double max = incls.length + data.size();

        final String[] colNames = {"Column name", "Final column type", "Row determining final column type"};
        final DataType[] colTypes = new DataType[]{StringCell.TYPE, StringCell.TYPE, StringCell.TYPE};

        BufferedDataContainer reasonsCon = exec.createDataContainer(new DataTableSpec(colNames, colTypes));

        setReasons(new String[incls.length][3]);

        if (data.size() > 0) {
            // empty table check
            SimpleDateFormat legacyDateFormat = new SimpleDateFormat(m_dateFormat);
            final var dateFormat = DateTimeFormatter.ofPattern(m_dateFormat);

            long numberOfRows = m_quickScan ? Math.min(m_numberOfRows, data.size()) : data.size();
            for (DataRow row : data) {
                if (!(0 < numberOfRows--)) {
                    data.iterator().close();
                    break;
                }
                for (int i = 0; i < incls.length; i++) {
                    // guess for each cell in each column the best matching datatype
                    DataCell c = row.getCell(data.getDataTableSpec().findColumnIndex(incls[i]));
                    if (!c.isMissing() && c.toString().equals(m_missValPat)) {
                        continue;
                    }
                    DataType newType = typeGuesser(c, legacyDateFormat, dateFormat, m_useLegacyDateTimeType);
                    if (types[i] != null) {
                        DataType toSet = setType(types[i], newType);
                        if (!toSet.equals(types[i])) {
                            m_reasons[i][2] = row.getKey().getString();
                            m_reasons[i][1] = m_useLegacyTypeNames ? toSet.toLegacyString() : toSet.getIdentifier();
                            m_reasons[i][0] = incls[i];
                        }
                        types[i] = toSet;
                    } else {
                        types[i] = newType;
                        String r = row.getKey().toString();
                        r += m_quickScan ? (" based on a quickscan.") : "";
                        m_reasons[i][2] = r;
                        m_reasons[i][1] = m_useLegacyTypeNames ? newType.toLegacyString() : newType.getIdentifier();
                        m_reasons[i][0] = incls[i];
                    }
                    exec.checkCanceled();
                }
                exec.checkCanceled();
                progress++;
                exec.setProgress(progress / max);
            }

            for (int i = 0; i < types.length; i++) {
                // if one column only contains missingCells than set column type to StringCell
                if (types[i] == null || types[i].equals(DataType.getMissingCell().getType())) {
                    types[i] = StringCell.TYPE;
                    m_reasons[i][2] = "";
                    m_reasons[i][1] = m_useLegacyTypeNames ? types[i].toLegacyString() : types[i].getIdentifier();
                    m_reasons[i][0] = incls[i];
                }
            }

            ColumnRearranger arrange = new ColumnRearranger(data.getDataTableSpec());
            for (int i = 0; i < incls.length; i++) {
                final int colIdx = data.getDataTableSpec().findColumnIndex(incls[i]);
                final DataType type = types[i];

                DataColumnSpecCreator colSpecCreator = new DataColumnSpecCreator(incls[i], types[i]);
                DataColumnSpec colSpec = colSpecCreator.createSpec();

                if (m_useLegacyDateTimeType && type.equals(DateAndTimeCell.TYPE)) {
                    arrange.replace(createLegacyDateAndTimeConverter(colIdx, colSpec), colIdx);
                } else if (!m_useLegacyDateTimeType && type.equals(ZONED_DATE_TIME_TYPE.get())) {
                    arrange.replace(
                        createDateAndTimeConverter(colIdx, colSpec, ZONED_DATE_TIME_TYPE.get(), ZonedDateTime::from),
                        colIdx);
                } else if (!m_useLegacyDateTimeType && type.equals(LOCAL_DATE_TIME_TYPE.get())) {
                    arrange.replace(
                        createDateAndTimeConverter(colIdx, colSpec, LOCAL_DATE_TIME_TYPE.get(), LocalDateTime::from),
                        colIdx);
                } else if (!m_useLegacyDateTimeType && type.equals(LOCAL_DATE_TYPE.get())) {
                    arrange.replace(createDateAndTimeConverter(colIdx, colSpec, LOCAL_DATE_TYPE.get(), LocalDate::from),
                        colIdx);
                } else if (!m_useLegacyDateTimeType && type.equals(LOCAL_TIME_TYPE.get())) {
                    arrange.replace(createDateAndTimeConverter(colIdx, colSpec, LOCAL_TIME_TYPE.get(), LocalTime::from),
                        colIdx);
                } else if (type.equals(LongCell.TYPE)) {
                    arrange.replace(createLongConverter(colIdx, colSpec), colIdx);
                } else {
                    arrange.replace(createNumberConverter(colIdx, type, colSpec), colIdx);
                }

                progress++;
                exec.setProgress(progress / max);
                exec.checkCanceled();
            }

            outTable = exec.createColumnRearrangeTable(data, arrange, exec);

            for (int i = 0; i < m_reasons.length; i++) {
                DataCell[] row = new DataCell[m_reasons[i].length];
                for (int j = 0; j < m_reasons[i].length; j++) {
                    row[j] = new StringCell(m_reasons[i][j]);
                }
                reasonsCon.addRowToTable(new DefaultRow(RowKey.createRowKey((long)i), row));
            }
        }

        reasonsCon.close();

        BufferedDataTable outReasons = reasonsCon.getTable();
        return new BufferedDataTable[]{outTable, outReasons};

    }

    private SingleCellFactory createNumberConverter(final int colIdx, final DataType type,
        final DataColumnSpec colSpec) {
        return new SingleCellFactory(colSpec) {
            private final DataCellFactory m_fac = new DataCellFactory();

            @Override
            public DataCell getCell(final DataRow row) {
                m_fac.setMissingValuePattern(m_missValPat);
                DataCell cell = row.getCell(colIdx);
                if (!cell.isMissing()) {

                    String str = cell.toString();
                    if (str == null) {
                        return DataType.getMissingCell();
                    }

                    // create String-, Int- or DoubleCell

                    DataCell c = m_fac.createDataCellOfType(type, str);
                    if (c == null) {
                        throw new NumberFormatException("Can't convert '" + str + "' to " + type.toString() + ". In "
                            + row.getKey() + " Column" + colIdx + ". Disable " + "quickscan and try again.");
                    }

                    return c;

                } else {
                    // create MissingCell
                    return DataType.getMissingCell();
                }
            }
        };
    }

    private SingleCellFactory createLongConverter(final int colIdx, final DataColumnSpec colSpec) {
        return new SingleCellFactory(colSpec) {
            @Override
            public DataCell getCell(final DataRow row) {

                DataCell cell = row.getCell(colIdx);
                if (!cell.isMissing()) {
                    String str = ((StringValue)cell).getStringValue();
                    if (!str.equals(m_missValPat)) {
                        // create LongCell
                        try {
                            return new LongCell(Long.parseLong(str));
                        } catch (NumberFormatException nfe) {
                            throw new NumberFormatException(
                                "Can't convert '" + str + "' to " + LongCell.TYPE.toString() + ". In " + row.getKey()
                                    + " Column" + colIdx + ". Disable " + "quickscan and try again.");
                        }
                    } else {
                        return DataType.getMissingCell();
                    }
                } else {
                    // create MissingCell
                    return DataType.getMissingCell();
                }
            }
        };
    }

    private SingleCellFactory createLegacyDateAndTimeConverter(final int colIdx, final DataColumnSpec colSpec) {
        return new SingleCellFactory(colSpec) {
            private final Calendar m_cal = Calendar.getInstance(TimeZone.getDefault());

            private final SimpleDateFormat m_format = new SimpleDateFormat(m_dateFormat);

            private final boolean m_hasDate;

            private final boolean m_hasTime;

            private final boolean m_hasMillis;

            {
                TimeZone timeZone = TimeZone.getTimeZone("UTC");
                m_format.setTimeZone(timeZone);
                m_cal.setTimeZone(timeZone);
                m_hasDate = m_dateFormat.contains("d");
                m_hasTime = m_dateFormat.contains("H");
                m_hasMillis = m_dateFormat.contains("S");
            }

            @Override
            public DataCell getCell(final DataRow row) {

                DataCell cell = row.getCell(colIdx);
                if (!cell.isMissing()) {
                    String str = ((StringValue)cell).getStringValue();
                    if (!str.equals(m_missValPat)) {
                        try {
                            m_cal.setTime(m_format.parse(str));
                            return new DateAndTimeCell(m_cal.getTimeInMillis(), m_hasDate, m_hasTime, m_hasMillis);
                        } catch (ParseException e) {
                            throw new IllegalArgumentException(
                                "Can't convert '" + str + "' to " + DateAndTimeCell.TYPE.toString() + ". In "
                                    + row.getKey() + " Column" + colIdx + ". Disable quickscan and try again.",
                                e);
                        }
                    } else {
                        return DataType.getMissingCell();
                    }

                } else {
                    // create MissingCell
                    return DataType.getMissingCell();
                }
            }
        };
    }

    private <T> SingleCellFactory createDateAndTimeConverter(final int colIdx, final DataColumnSpec colSpec,
        final DataType dataType, final TemporalQuery<T> temporalQuery) {
        return new SingleCellFactory(colSpec) {

            private final DateTimeFormatter m_format = DateTimeFormatter.ofPattern(m_dateFormat);

            private static final DataCellFactory CELL_FACTORY = new DataCellFactory();

            @Override
            public DataCell getCell(final DataRow row) {
                DataCell cell = row.getCell(colIdx);
                if (cell.isMissing()) {
                    return DataType.getMissingCell();
                }
                String str = ((StringValue)cell).getStringValue();
                if (str.equals(m_missValPat)) {
                    return DataType.getMissingCell();
                }
                try {
                    final var parsedDateTime = m_format.parse(str, temporalQuery);
                    /**
                     * to string returns the iso format of the given LocalDate, LocalTime, LocalDateTime, or
                     * ZonedDateTime, which is needed as CELL_FACTORY.createDataCellOfType calls the default create of
                     * the different cells which all use the ISO format
                     */
                    return CELL_FACTORY.createDataCellOfType(dataType, parsedDateTime.toString());
                } catch (DateTimeParseException e) {
                    throw new IllegalArgumentException("Can't convert '" + str + "' with format: '" + m_dateFormat
                        + "' to " + dataType.toString() + ". In " + row.getKey() + " Column" + colIdx
                        + ". Disable quickscan or change the format and try again.", e);
                }
            }
        };
    }

    /**
     * @param curType currently stored DataType
     * @param newType possible new DataType
     */
    private DataType setType(final DataType curType, final DataType newType) {
        // if one of the types represents the missing cell type, we
        // return the other type.
        if (curType.equals(DataType.getMissingCell().getType())) {
            return newType;
        }
        if (newType.equals(DataType.getMissingCell().getType())) {
            return curType;
        }

        // handles also the equals case
        if (curType.isASuperTypeOf(newType)) {
            return curType;
        }
        if (newType.isASuperTypeOf(curType)) {
            return newType;
        }

        // if both are not super type, return default StringCell
        return StringCell.TYPE;

    }

    /**
     * try to parse all different numeric types and the date type based on the given format-pattern.
     *
     * @param cell
     * @return new DataType if string could be parsed else return old DataType
     */
    private static DataType typeGuesser(final DataCell cell, final DateFormat legacyDateFormat,
        final DateTimeFormatter dateTimeFormat, final boolean useLegacyDateTimeType) {
        if (!cell.isMissing()) {
            String str = cell.toString();

            if (useLegacyDateTimeType) {
                try {
                    legacyDateFormat.parse(str);
                    return DateAndTimeCell.TYPE;
                } catch (ParseException e1) {
                    // try another one
                }
            } else {
                final var dateTimeType = guessDateTimeType(dateTimeFormat, str);
                if (dateTimeType.isPresent()) {
                    return dateTimeType.get();
                }
            }

            try {
                Integer.parseInt(str);
                return IntCell.TYPE;
            } catch (NumberFormatException eInt) {
                // try another one
            }

            try {
                Long.parseLong(str);
                return LongCell.TYPE;
            } catch (NumberFormatException eLong) {
                // try another one
            }

            try {
                double d = Double.parseDouble(str);
                if (Double.isInfinite(d) && str.matches("[01]+")) {
                    // if every cell above matched [01]+ and this cell matches to, we assume this column (until now) is
                    // a bit-vector column. Bit-Vectors are stored in string representation.
                    return StringCell.TYPE;
                }
                return DoubleCell.TYPE;
            } catch (NumberFormatException e) {
                // too bad
            }
        }
        return cell.getType();
    }

    private static Optional<DataType> guessDateTimeType(final DateTimeFormatter dateTimeFormat, final String value) {
        try {
            final var parsedDateTime = dateTimeFormat.parse(value);
            if (ZONED_DATE_TIME_TYPE.isEmpty() || LOCAL_DATE_TIME_TYPE.isEmpty() || LOCAL_DATE_TYPE.isEmpty()
                || LOCAL_TIME_TYPE.isEmpty()) {
                throw new IllegalStateException(String.format(
                    "Trying to convert '%s' to a date&time, but the date&time types could not be found.", value));
            }
            try {
                ZonedDateTime.from(parsedDateTime);
                return Optional.of(ZONED_DATE_TIME_TYPE.get());
            } catch (DateTimeException e) {
                // try another date&time type
            }
            try {
                LocalDateTime.from(parsedDateTime);
                return Optional.of(LOCAL_DATE_TIME_TYPE.get());
            } catch (DateTimeException e) {
                // try another date&time type
            }
            try {
                LocalTime.from(parsedDateTime);
                return Optional.of(LOCAL_TIME_TYPE.get());
            } catch (DateTimeException e) {
                // try another date&time type
            }
            try {
                LocalDate.from(parsedDateTime);
                return Optional.of(LOCAL_DATE_TYPE.get());
            } catch (DateTimeException e) {
                // try another data type
            }
        } catch (DateTimeParseException e) {
            // try another data type
        }
        return Optional.empty();
    }

    /**
     * {@inheritDoc}
     *
     * @throws InvalidSettingsException thrown if some input columns are not available during execution.
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (m_conf == null) {
            m_conf = createDCSFilterConfiguration();
            // auto-configure
            m_conf.loadDefaults(inSpecs[0], true);
        }
        String[] removedFromIncludes = m_conf.applyTo(inSpecs[0]).getRemovedFromIncludes();
        if (m_conf.isEnforceInclusion() && (removedFromIncludes.length != 0)) {
            throw new InvalidSettingsException("Input table does not match "
                + "selected include columns, unable to find column(s): "
                + ConvenienceMethods.getShortStringFrom(new HashSet<String>(Arrays.asList(removedFromIncludes)), 3));
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * @param settings NodeSettings
     * @see org.knime.core.node.NodeModel#loadValidatedSettingsFrom(org.knime.core.node.NodeSettingsRO)
     * @throws InvalidSettingsException invalid settings exception
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createDCSFilterConfiguration();
        conf.loadConfigurationInModel(settings);
        m_conf = conf;
        m_dateFormat = settings.getString(CFGKEY_DATEFORMAT);
        final String missValDesc = settings.getString(CFGKEY_MISSVALPAT);
        if (missValDesc.equals(MISSVALDESC_NONE)) {
            m_missValPat = null;
        } else if (missValDesc.equals(MISSVALDESC_EMPTY)) {
            m_missValPat = "";
        } else {
            m_missValPat = missValDesc;
        }
        m_quickScan = settings.getBoolean(CFGKEY_QUICKSANBOOLEAN);
        m_numberOfRows = settings.getInt(CFGKEY_QUICKSCANROWS);
        m_useLegacyTypeNames = settings.getBoolean(CFGKEY_USELEGACYTYPENAMES, true);
        m_useLegacyDateTimeType = settings.getBoolean(CFGKEY_USE_LEGACY_DATE_TIME_TYPE, true);

    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveInternals(final File nodeInternDir, final ExecutionMonitor exec)
        throws IOException, CanceledExecutionException {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        if (m_conf != null) {
            m_conf.saveConfiguration(settings);
        }
        settings.addString(CFGKEY_DATEFORMAT, m_dateFormat);
        if (m_missValPat == null) {
            settings.addString(CFGKEY_MISSVALPAT, MISSVALDESC_NONE);
        } else if (m_missValPat.equals("")) {
            settings.addString(CFGKEY_MISSVALPAT, MISSVALDESC_EMPTY);
        } else {
            settings.addString(CFGKEY_MISSVALPAT, m_missValPat);
        }
        settings.addBoolean(CFGKEY_QUICKSANBOOLEAN, m_quickScan);
        settings.addInt(CFGKEY_QUICKSCANROWS, m_numberOfRows);
        settings.addBoolean(CFGKEY_USELEGACYTYPENAMES, m_useLegacyTypeNames);
        settings.addBoolean(CFGKEY_USE_LEGACY_DATE_TIME_TYPE, m_useLegacyDateTimeType);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        DataColumnSpecFilterConfiguration conf = createDCSFilterConfiguration();
        conf.loadConfigurationInModel(settings);
        String tmpDateFormat = settings.getString("dateFormat");
        try {
            new SimpleDateFormat(tmpDateFormat);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingsException(e.getMessage(), e);
        }
        try {
            settings.getBoolean(CFGKEY_QUICKSANBOOLEAN);
        } catch (IllegalArgumentException e1) {
            throw new InvalidSettingsException(e1.getMessage(), e1);
        }
        try {
            int tmpQuickScanRows = settings.getInt(CFGKEY_QUICKSCANROWS);
            if (tmpQuickScanRows < 1) {
                throw new InvalidSettingsException("Number of rows for quickscan is to small.");
            }
        } catch (IllegalArgumentException e2) {
            throw new InvalidSettingsException(e2.getMessage(), e2);
        }
    }

    /**
     * A new configuration to store the settings. Only Columns of Type String are available.
     *
     * @return ...
     */
    static final DataColumnSpecFilterConfiguration createDCSFilterConfiguration() {
        return new DataColumnSpecFilterConfiguration("column-filter", new InputFilter<DataColumnSpec>() {

            @Override
            public boolean include(final DataColumnSpec name) {
                return name.getType().getCellClass() == null || name.getType().equals(StringCell.TYPE);
            }
        }, NameFilterConfiguration.FILTER_BY_NAMEPATTERN);
    }

    /**
     * @return the reasons
     */
    public String[][] getReasons() {
        return m_reasons;
    }

    /**
     * @param reasons the reasons to set
     */
    private void setReasons(final String[][] reasons) {
        this.m_reasons = reasons;
    }
}
