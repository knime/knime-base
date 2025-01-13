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
 *   Oct 19, 2016 (simon): created
 */
package org.knime.time.node.convert.stringtodatetime;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.chrono.Chronology;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.MissingCell;
import org.knime.core.data.StringValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.time.localdate.LocalDateCellFactory;
import org.knime.core.data.time.localdatetime.LocalDateTimeCellFactory;
import org.knime.core.data.time.localtime.LocalTimeCellFactory;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeCellFactory;
import org.knime.core.node.BufferedDataTable;
import org.knime.core.node.ExecutionContext;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.node.KNIMEException;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.NodeSettingsRO;
import org.knime.core.node.NodeSettingsWO;
import org.knime.core.node.defaultnodesettings.SettingsModelBoolean;
import org.knime.core.node.defaultnodesettings.SettingsModelColumnFilter2;
import org.knime.core.node.defaultnodesettings.SettingsModelString;
import org.knime.core.node.message.Message;
import org.knime.core.node.message.MessageBuilder;
import org.knime.core.node.streamable.simple.SimpleStreamableFunctionWithInternalsNodeModel;
import org.knime.core.node.streamable.simple.SimpleStreamableOperatorInternals;
import org.knime.core.node.util.StringHistory;
import org.knime.core.node.util.filter.InputFilter;
import org.knime.time.util.DateTimeType;
import org.knime.time.util.ReplaceOrAppend;

/**
 * The node model of the node which converts strings to the new date&time types.
 *
 * @author Simon Schmid, KNIME.com, Konstanz, Germany
 */
final class StringToDateTimeNodeModel
    extends SimpleStreamableFunctionWithInternalsNodeModel<SimpleStreamableOperatorInternals> {

    private static final NodeLogger LOGGER = NodeLogger.getLogger(StringToDateTimeNodeModel.class);

    static final String FORMAT_HISTORY_KEY = "string_to_date_formats";

    private static final int FORMAT_HISTORY_SIZE = 256;

    static final String OPTION_APPEND = "Append selected columns";

    static final String OPTION_REPLACE = "Replace selected columns";

    /*
     * Config key for the operator internals to propagate error messages.
     */
    private static final String CFG_KEY_ERROR_MESSAGES = "error_message";

    private final SettingsModelColumnFilter2 m_colSelect = createColSelectModel();

    private final SettingsModelString m_isReplaceOrAppend = createReplaceAppendStringBool();

    private final SettingsModelString m_suffix = createSuffixModel(m_isReplaceOrAppend);

    private final SettingsModelString m_format = createFormatModel();

    private final SettingsModelString m_locale = createLocaleModel();

    private final SettingsModelBoolean m_cancelOnFail = createCancelOnFailModel();

    private String m_selectedType = DateTimeType.LOCAL_DATE_TIME.name();

    private int m_failCounter;

    private MessageBuilder m_messageBuilder;

    private boolean m_hasValidatedConfiguration = false;

    /** @return the column select model, used in both dialog and model. */
    @SuppressWarnings("unchecked")
    static SettingsModelColumnFilter2 createColSelectModel() {
        return new SettingsModelColumnFilter2("col_select", StringValue.class);
    }

    /** @return the string model, used in both dialog and model. */
    static SettingsModelString createReplaceAppendStringBool() {
        return new SettingsModelString("replace_or_append", OPTION_REPLACE);
    }

    /**
     * @param replaceOrAppendModel model for the replace/append button group
     * @return the string model, used in both dialog and model.
     */
    public static SettingsModelString createSuffixModel(final SettingsModelString replaceOrAppendModel) {
        final SettingsModelString suffixModel = new SettingsModelString("suffix", "(Date&Time)");
        replaceOrAppendModel.addChangeListener(
            e -> suffixModel.setEnabled(replaceOrAppendModel.getStringValue().equals(OPTION_APPEND)));
        suffixModel.setEnabled(false);
        return suffixModel;
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createFormatModel() {
        return new SettingsModelString("date_format", "yyyy-MM-dd'T'HH:mm[:ss[.SSS]]");
    }

    /** @return the string select model, used in both dialog and model. */
    static SettingsModelString createLocaleModel() {
        return new SettingsModelString("locale", Locale.getDefault().toLanguageTag());
    }

    /** @return the boolean model, used in both dialog and model. */
    static SettingsModelBoolean createCancelOnFailModel() {
        return new SettingsModelBoolean("cancel_on_fail", true);
    }

    /**
     * Constructor
     */
    public StringToDateTimeNodeModel() {
        super(SimpleStreamableOperatorInternals.class);
    }

    /**
     * @return a set of all predefined formats plus the formats added by the user
     */
    static Collection<String> createPredefinedFormats() {
        // unique values
        Set<String> formats = new LinkedHashSet<>();
        formats.add("dd.MM.yyyy HH:mm:ss.S");
        formats.add("dd.MM.yyyy");
        formats.add("dd.MM.yyyy;HH:mm:ss.S");
        formats.add("HH:mm:ss");
        formats.add("HH:mm[:ss[.SSS]]");
        formats.add("M/d/yyyy"); // standard google sheets date format
        formats.add("yyyy-DDDXXX");
        formats.add("yyyy-MM-dd G");
        formats.add("yyyy-MM-dd HH:mm:ss'Z'");
        formats.add("yyyy-MM-dd HH:mm:ss,SSS");
        formats.add("yyyy-MM-dd HH:mm:ss,SSS'['VV']'");
        formats.add("yyyy-MM-dd HH:mm:ss,SSS'Z'");
        formats.add("yyyy-MM-dd HH:mm:ss,SSSXXX");
        formats.add("yyyy-MM-dd HH:mm:ss,SSSXXX'['VV']'");
        formats.add("yyyy-MM-dd HH:mm:ss.S");
        formats.add("yyyy-MM-dd HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd HH:mm:ss.SSS'['VV']'");
        formats.add("yyyy-MM-dd HH:mm:ss.SSS'Z'");
        formats.add("yyyy-MM-dd HH:mm:ss.SSSXXX");
        formats.add("yyyy-MM-dd HH:mm:ss.SSSXXX'['VV']'");
        formats.add("yyyy-MM-dd HH:mm:ssX");
        formats.add("yyyy-MM-dd HH:mm:ssXXX");
        formats.add("yyyy-MM-dd HH:mm:ssXXX'['VV']'");
        formats.add("yyyy-MM-dd HH:mm:ssZ");
        formats.add("yyyy-MM-dd");
        formats.add("yyyy-MM-dd'T'HH:mm:ss");
        formats.add("yyyy-MM-dd'T'HH:mm:ss'Z'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss,SSS");
        formats.add("yyyy-MM-dd'T'HH:mm:ss,SSS'['VV']'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss,SSS'Z'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss,SSSXXX");
        formats.add("yyyy-MM-dd'T'HH:mm:ss,SSSXXX'['VV']'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS'['VV']'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSVV'['zzzz']'");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
        formats.add("yyyy-MM-dd'T'HH:mm:ss.SSSXXX'['VV']'");
        formats.add("yyyy-MM-dd'T'HH:mm:ssX");
        formats.add("yyyy-MM-dd'T'HH:mm:ssXXX");
        formats.add("yyyy-MM-dd'T'HH:mm:ssXXX'['VV']'");
        formats.add("yyyy-MM-dd'T'HH:mm:ssZ");
        formats.add("yyyy-MM-dd'T'HH:mm[:ss[.SSS]]VV['['zzzz']']");
        formats.add("yyyy-MM-dd'T'HH:mm[:ss[.SSSSSS]]z");
        formats.add("yyyy-MM-dd;HH:mm:ss[.SSS][.SS][.S]");
        formats.add("yyyy-MM-dd;HH:mm:ss[.SSS][.SS][.S][z]");
        formats.add("yyyy-MM-dd;HH:mm:ssVV");
        formats.add("yyyy-MM-ddXXX");
        formats.add("yyyy/dd/MM");
        formats.add("yyyyMMdd");
        formats.add("yyyyMMddZ");

        // check also the StringHistory....
        formats.addAll(Arrays.asList(StringHistory.getInstance(FORMAT_HISTORY_KEY, FORMAT_HISTORY_SIZE).getHistory()));
        return formats;
    }

    /**
     * @param dateTimeType whether input Strings represent a date, date and time, zoned date and time, etc.
     * @param format string that allows to parse input Strings as an instance of the specified date time type
     *
     * @author Carl Witt, KNIME AG, Zurich, Switzerland
     */
    record DateTimeFormat(DateTimeType dateTimeType, String format) {
    }

    /**
     * Tries the {@link #createPredefinedFormats()} and returns the first that is able to parse the input.
     *
     * @param input an example string, e.g., "2023-03-21T11:29:17.394856Z"
     * @param locale the locale under which the formats are to be interpreted
     * @return a {@link DateTimeType} and a format string that allows to parse inputs
     */
    static Optional<DateTimeFormat> guessFormat(final String input, final Locale locale) {
        for (final String format : createPredefinedFormats()) {
            final DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern(format, locale).withChronology(Chronology.ofLocale(locale));
            try {
                ZonedDateTime.parse(input, formatter);
                return Optional.of(new DateTimeFormat(DateTimeType.ZONED_DATE_TIME, format));
            } catch (DateTimeException e) { // NOSONAR, just checking if the format works
            }
            try {
                LocalDateTime.parse(input, formatter);
                return Optional.of(new DateTimeFormat(DateTimeType.LOCAL_DATE_TIME, format));
            } catch (DateTimeException e) { // NOSONAR, just checking if the format works
            }
            try {
                LocalDate.parse(input, formatter);
                return Optional.of(new DateTimeFormat(DateTimeType.LOCAL_DATE, format));
            } catch (DateTimeException e) { // NOSONAR, just checking if the format works
            }
            try {
                LocalTime.parse(input, formatter);
                return Optional.of(new DateTimeFormat(DateTimeType.LOCAL_TIME, format));
            } catch (DateTimeException e) { // NOSONAR, just checking if the format works
            }
        }
        return Optional.empty();
    }

    /**
     * Sets the column selections to not include any columns.
     *
     * @param tableSpec the corresponding spec
     */
    private void setDefaultColumnSelection(final DataTableSpec tableSpec) {
        final InputFilter<DataColumnSpec> filter = new InputFilter<DataColumnSpec>() {
            @Override
            public boolean include(final DataColumnSpec spec) {
                return spec.getType().getPreferredValueClass() == StringValue.class;
            }
        };
        m_colSelect.loadDefaults(tableSpec, filter, true);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected DataTableSpec[] configure(final DataTableSpec[] inSpecs) throws InvalidSettingsException {
        if (!m_hasValidatedConfiguration) {
            setDefaultColumnSelection(inSpecs[0]);
            throw new InvalidSettingsException("Node must be configured.");
        }
        return super.configure(inSpecs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected BufferedDataTable[] execute(final BufferedDataTable[] inData, final ExecutionContext exec)
        throws Exception {

        var operatorInternals = createStreamingOperatorInternals();
        final ColumnRearranger columnRearranger =
            createColumnRearranger(inData[0].getDataTableSpec(), operatorInternals);
        final BufferedDataTable out = exec.createColumnRearrangeTable(inData[0], columnRearranger, exec);
        setWarning(operatorInternals);
        return new BufferedDataTable[]{out};
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec inSpec,
        final SimpleStreamableOperatorInternals internals) {
        m_messageBuilder = createMessageBuilder(); // Initialize message builder to be used by CellFactory
        final String[] includeList = m_colSelect.applyTo(inSpec).getIncludes();

        return ( //
        m_isReplaceOrAppend.getStringValue().equals(OPTION_REPLACE) //
            ? ReplaceOrAppend.REPLACE //
            : ReplaceOrAppend.APPEND //
        ).createRearranger(includeList, inSpec, (inputColumSpec, newColumnName) -> {
            var outputType = switch (DateTimeType.valueOf(m_selectedType)) {
                case LOCAL_DATE -> LocalDateCellFactory.TYPE;
                case LOCAL_TIME -> LocalTimeCellFactory.TYPE;
                case LOCAL_DATE_TIME -> LocalDateTimeCellFactory.TYPE;
                case ZONED_DATE_TIME -> ZonedDateTimeCellFactory.TYPE;
            };
            var outSpec = new DataColumnSpecCreator(newColumnName, outputType).createSpec();

            return new StringToTimeCellFactory(outSpec, inSpec.findColumnIndex(inputColumSpec.getName()), internals);
        }, m_suffix.getStringValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void saveSettingsTo(final NodeSettingsWO settings) {
        m_colSelect.saveSettingsTo(settings);
        m_isReplaceOrAppend.saveSettingsTo(settings);
        m_suffix.saveSettingsTo(settings);
        m_format.saveSettingsTo(settings);
        m_cancelOnFail.saveSettingsTo(settings);
        settings.addString("typeEnum", m_selectedType);
        try {
            // conversion necessary for backwards compatibility (AP-8915)
            final var locale = LocaleUtils.toLocale(m_locale.getStringValue());
            m_locale.setStringValue(locale.toLanguageTag());
        } catch (final IllegalArgumentException e) { // NOSONAR
            // do nothing, locale is already in correct format
        }
        m_locale.saveSettingsTo(settings);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.validateSettings(settings);
        m_isReplaceOrAppend.validateSettings(settings);
        m_suffix.validateSettings(settings);
        m_format.validateSettings(settings);
        m_locale.validateSettings(settings);
        m_cancelOnFail.validateSettings(settings);
        final SettingsModelString formatClone = m_format.createCloneWithValidatedValue(settings);
        final String format = formatClone.getStringValue();
        if (StringUtils.isEmpty(format)) {
            throw new InvalidSettingsException("Format must not be empty.");
        }
        try {
            DateTimeFormatter.ofPattern(format);
        } catch (IllegalArgumentException e) {
            String msg = "Invalid date format: \"" + format + "\".";
            final String errMsg = e.getMessage();
            if (!StringUtils.isEmpty(errMsg)) {
                msg += " Reason: " + errMsg;
            }
            throw new InvalidSettingsException(msg, e);
        }
        settings.getString("typeEnum");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void loadValidatedSettingsFrom(final NodeSettingsRO settings) throws InvalidSettingsException {
        m_colSelect.loadSettingsFrom(settings);
        m_isReplaceOrAppend.loadSettingsFrom(settings);
        m_suffix.loadSettingsFrom(settings);
        m_format.loadSettingsFrom(settings);
        m_locale.loadSettingsFrom(settings);
        m_cancelOnFail.loadSettingsFrom(settings);
        m_selectedType = settings.getString("typeEnum");
        final String dateformat = m_format.getStringValue();
        // if it is not a predefined one -> store it
        if (!createPredefinedFormats().contains(dateformat)) {
            StringHistory.getInstance(FORMAT_HISTORY_KEY, FORMAT_HISTORY_SIZE).add(dateformat);
        }
        m_hasValidatedConfiguration = true;

        try {
            // check for backwards compatibility (AP-8915)
            LocaleUtils.toLocale(m_locale.getStringValue());
        } catch (IllegalArgumentException e) {
            try {
                final String iso3Country = Locale.forLanguageTag(m_locale.getStringValue()).getISO3Country();
                final String iso3Language = Locale.forLanguageTag(m_locale.getStringValue()).getISO3Language();
                if (iso3Country.isEmpty() && iso3Language.isEmpty()) {
                    throw new InvalidSettingsException("Unsupported locale '" + m_locale.getStringValue() + "'.");
                }
            } catch (MissingResourceException ex) {
                throw new InvalidSettingsException(
                    "Unsupported locale '" + m_locale.getStringValue() + "': " + ex.getMessage(), ex);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void reset() {
        m_failCounter = 0;
    }

    /**
     * This cell factory converts a single String cell to a Date&Time cell.
     */
    final class StringToTimeCellFactory extends SingleCellFactory {
        private final int m_colIndex;

        private final SimpleStreamableOperatorInternals m_internals;

        private final DataColumnSpec m_spec;

        private final DateTimeFormatter m_formatter;

        private final DateTimeType m_enumType;

        /**
         * @param outSpec spec of the column after computation
         * @param colIndex index of the column to work on
         * @param internals streamable operator internals to propagate the error messages
         */
        public StringToTimeCellFactory(final DataColumnSpec outSpec, final int colIndex,
            final SimpleStreamableOperatorInternals internals) {
            super(outSpec);
            m_colIndex = colIndex;
            m_internals = internals;
            m_spec = outSpec;
            final var locale = Locale.forLanguageTag(m_locale.getStringValue());
            m_formatter = DateTimeFormatter.ofPattern(m_format.getStringValue(), locale)
                .withChronology(Chronology.ofLocale(locale));
            m_enumType = DateTimeType.valueOf(m_selectedType);
        }

        @Override
        public DataCell getCell(final DataRow row, final long rowIndex) {
            final DataCell cell = row.getCell(m_colIndex);
            if (cell.isMissing()) {
                return cell;
            }

            final String input = ((StringValue)cell).getStringValue();
            try {
                switch (m_enumType) {
                    case LOCAL_DATE: {
                        final var ld = LocalDate.parse(input, m_formatter);
                        return LocalDateCellFactory.create(ld);
                    }
                    case LOCAL_TIME: {
                        final var lt = LocalTime.parse(input, m_formatter);
                        return LocalTimeCellFactory.create(lt);
                    }
                    case LOCAL_DATE_TIME: {
                        final var ldt = LocalDateTime.parse(input, m_formatter);
                        return LocalDateTimeCellFactory.create(ldt);
                    }
                    case ZONED_DATE_TIME: {
                        final var zdt = ZonedDateTime.parse(input, m_formatter);
                        return ZonedDateTimeCellFactory.create(zdt);
                    }
                    default:
                        throw new IllegalStateException("Unhandled date&time type: " + m_selectedType);
                }
            } catch (DateTimeParseException e) {
                m_failCounter++;
                var msg = String.format(
                    "Could not parse date in cell [%s, column \"%s\", row number %d]: "
                        + "Pattern \"%s\" does not match \"%s\".", //
                    StringUtils.abbreviate(row.getKey().getString(), 15), m_spec.getName(), rowIndex + 1, //
                    StringUtils.abbreviate(m_format.getStringValue(), 32), StringUtils.abbreviate(input, 32));
                if (m_messageBuilder.getIssueCount() == 0) {
                    m_messageBuilder.withSummary(msg);
                }
                m_messageBuilder.addRowIssue(0, m_colIndex, rowIndex, msg);

                if (m_cancelOnFail.getBooleanValue()) {
                    if (m_failCounter == 1L) {
                        m_messageBuilder.addResolutions(
                            "Deselect the \"Fail on error\" option to output missing values for non-matching strings.");
                    }
                    throw KNIMEException.of(m_messageBuilder.build().orElseThrow(), e).toUnchecked();
                }
                return new MissingCell(e.getMessage());
            }
        }

        @Override
        public void afterProcessing() {
            if (m_messageBuilder.getIssueCount() > 0) {
                final var messageConfig = m_internals.getConfig().addConfig(CFG_KEY_ERROR_MESSAGES);
                m_messageBuilder
                    .withSummary("Problems in " + m_failCounter + " rows. First error: "
                        + m_messageBuilder.getSummary().orElseThrow()) //
                    .addResolutions("Change the date and time pattern to match all provided strings.") //
                    .build() //
                    .ifPresent(m -> m.saveTo(messageConfig));
            }
        }
    }

    private static Optional<Message>
        getMessageFromOperatorInternals(final SimpleStreamableOperatorInternals internals) {
        try {
            return Message.load(internals.getConfig().getConfig(CFG_KEY_ERROR_MESSAGES));
        } catch (InvalidSettingsException e) {
            return Optional.empty();
        }
    }

    @Override
    protected SimpleStreamableOperatorInternals
        mergeStreamingOperatorInternals(final SimpleStreamableOperatorInternals[] operatorInternals) {

        var anyMsg = Arrays.stream(operatorInternals).map(StringToDateTimeNodeModel::getMessageFromOperatorInternals)
            .findFirst();

        SimpleStreamableOperatorInternals res = new SimpleStreamableOperatorInternals();
        if (anyMsg.isPresent()) {
            var firstMsg = anyMsg.orElseThrow();
            if (firstMsg.isPresent()) {
                firstMsg.orElseThrow().saveTo(res.getConfig().addConfig(CFG_KEY_ERROR_MESSAGES));
            }
        }
        return res;
    }

    @Override
    protected void finishStreamableExecution(final SimpleStreamableOperatorInternals internals) {
        setWarning(internals);
    }

    private void setWarning(final SimpleStreamableOperatorInternals internals) {
        try {
            Message.load(internals.getConfig().getConfig(CFG_KEY_ERROR_MESSAGES)).ifPresent(this::setWarning);
        } catch (InvalidSettingsException ex) {
            LOGGER.debug("Unable to restore warning message from streaming operators", ex);
        }
    }
}
