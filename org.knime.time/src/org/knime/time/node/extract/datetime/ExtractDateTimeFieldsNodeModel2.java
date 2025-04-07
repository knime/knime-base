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
 *   22 Jan 2024 (albrecht): created
 */
package org.knime.time.node.extract.datetime;

import static org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationUtils.validatePossiblyEmptyColumnName;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.Temporal;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.knime.core.data.DataCell;
import org.knime.core.data.DataColumnDomainCreator;
import org.knime.core.data.DataColumnSpec;
import org.knime.core.data.DataColumnSpecCreator;
import org.knime.core.data.DataRow;
import org.knime.core.data.DataTableSpec;
import org.knime.core.data.DataType;
import org.knime.core.data.DataValue;
import org.knime.core.data.container.ColumnRearranger;
import org.knime.core.data.container.SingleCellFactory;
import org.knime.core.data.def.IntCell;
import org.knime.core.data.def.IntCell.IntCellFactory;
import org.knime.core.data.def.StringCell;
import org.knime.core.data.def.StringCell.StringCellFactory;
import org.knime.core.data.time.localdate.LocalDateValue;
import org.knime.core.data.time.localdatetime.LocalDateTimeValue;
import org.knime.core.data.time.localtime.LocalTimeValue;
import org.knime.core.data.time.zoneddatetime.ZonedDateTimeValue;
import org.knime.core.node.InvalidSettingsException;
import org.knime.core.util.UniqueNameGenerator;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder;
import org.knime.core.webui.node.dialog.defaultdialog.widget.validation.ColumnNameValidationMessageBuilder.ColumnNameSettingContext;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.extract.datetime.ExtractDateTimeFieldsSettings.DateTimeField;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ExtractDateTimeFieldsNodeModel2
    extends WebUISimpleStreamableFunctionNodeModel<ExtractDateTimeFieldsSettings> {

    /**
     * @param configuration
     */
    protected ExtractDateTimeFieldsNodeModel2(final WebUINodeConfiguration configuration) {
        super(configuration, ExtractDateTimeFieldsSettings.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void validateSettings(final ExtractDateTimeFieldsSettings settings) throws InvalidSettingsException {
        if (Arrays.stream(settings.m_extractFields).anyMatch(field -> field.m_field == null)) {
            throw new InvalidSettingsException("No empty fields allowed. Please remove to continue.");
        }
        for (final var columnSetting : settings.m_extractFields) {
            final var invalidColNameToErrorMessage = new ColumnNameValidationMessageBuilder("column name") //
                .withSpecificSettingContext(ColumnNameSettingContext.INSIDE_COMPACT_ARRAY_LAYOUT) //
                .withArrayItemIdentifier(columnSetting.m_field.getLabelValue()).build();
            validatePossiblyEmptyColumnName(columnSetting.m_columnName, invalidColNameToErrorMessage);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ExtractDateTimeFieldsSettings modelSettings) throws InvalidSettingsException {
        final String selectedCol = modelSettings.m_selectedColumn;
        if (selectedCol == null || selectedCol.isEmpty()) {
            throw new InvalidSettingsException("No Date&Time compatible column selected.");
        }
        final int selectedColIdx = spec.findColumnIndex(selectedCol);
        if (selectedColIdx < 0) {
            throw new InvalidSettingsException("Column " + selectedCol + " not found in the input table.");
        }
        final var selectedColSpec = spec.getColumnSpec(selectedCol);
        final boolean isDate = ExtractDateTimeFieldsSettings.isDateType(selectedColSpec);
        final boolean isTime = ExtractDateTimeFieldsSettings.isTimeType(selectedColSpec);
        if (!isDate && !isTime) {
            throw new InvalidSettingsException("Column " + selectedCol + " does not contain a Date&Time type.");
        }

        final Locale locale = LocaleProvider.JAVA_11.stringToLocale(modelSettings.m_locale);

        final UniqueNameGenerator nameGenerator = new UniqueNameGenerator(spec);
        final DataColumnDomainCreator domainCreator = new DataColumnDomainCreator();
        final ColumnRearranger rearranger = new ColumnRearranger(spec);

        Arrays.stream(modelSettings.m_extractFields)
            .forEachOrdered(field -> extractField(field.m_field, field.m_columnName, selectedColIdx,
                selectedColSpec.getType(), locale, nameGenerator, domainCreator, rearranger));

        if (rearranger.getColumnCount() == spec.getNumColumns()) {
            getLogger().info("No fields will be extracted. Output table will equal input table.");
        }

        return rearranger;
    }

    private static void extractField(final DateTimeField field, final String newColumnName, final int selectedColIdx,
        final DataType selectedColType, final Locale locale, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {

        final var extractor = new TemporalExtractor(selectedColIdx, selectedColType, rearranger);
        String suggestedColName = newColumnName;
        if (StringUtils.isEmpty(suggestedColName)) {
            suggestedColName = field.getLabelValue();
        }

        // Date fields
        if (field == DateTimeField.YEAR) {
            extractor.extractFromDate(nameGenerator.newColumn(suggestedColName, IntCell.TYPE), //
                t -> IntCellFactory.create(t.getYear()), //
                t -> IntCellFactory.create(t.getYear()), //
                t -> IntCellFactory.create(t.getYear()) //
            );
        } else if (field == DateTimeField.YEAR_WEEK_BASED) {
            extractor.extractFromDate(nameGenerator.newColumn(suggestedColName, IntCell.TYPE), //
                t -> IntCellFactory.create(t.get(WeekFields.of(locale).weekBasedYear())) //
            );
        } else if (field == DateTimeField.QUARTER) {
            extractor.extractFromDate(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 4), //
                t -> IntCellFactory.create((t.getMonthValue() + 2) / 3), //
                t -> IntCellFactory.create((t.getMonthValue() + 2) / 3), //
                t -> IntCellFactory.create((t.getMonthValue() + 2) / 3) //
            );
        } else if (field == DateTimeField.MONTH_NUMBER) {
            extractor.extractFromDate(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 12), //
                t -> IntCellFactory.create(t.getMonthValue()), //
                t -> IntCellFactory.create(t.getMonthValue()), //
                t -> IntCellFactory.create(t.getMonthValue()) //
            );
        } else if (field == DateTimeField.MONTH_NAME) {
            extractor.extractFromDate(nameGenerator.newColumn(suggestedColName, StringCell.TYPE), //
                t -> StringCellFactory.create(t.getMonth().getDisplayName(TextStyle.FULL, locale)), //
                t -> StringCellFactory.create(t.getMonth().getDisplayName(TextStyle.FULL, locale)), //
                t -> StringCellFactory.create(t.getMonth().getDisplayName(TextStyle.FULL, locale)) //
            );
        } else if (field == DateTimeField.WEEK) {
            extractor.extractFromDate(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 52), //
                t -> IntCellFactory.create(t.get(WeekFields.of(locale).weekOfWeekBasedYear()))//
            );
        } else if (field == DateTimeField.DAY_OF_YEAR) {
            extractor.extractFromDate(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 366), //
                t -> IntCellFactory.create(t.getDayOfYear()), //
                t -> IntCellFactory.create(t.getDayOfYear()), //
                t -> IntCellFactory.create(t.getDayOfYear()) //
            );
        } else if (field == DateTimeField.DAY_OF_MONTH) {
            extractor.extractFromDate(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 31), //
                t -> IntCellFactory.create(t.getDayOfMonth()), //
                t -> IntCellFactory.create(t.getDayOfMonth()), //
                t -> IntCellFactory.create(t.getDayOfMonth()) //
            );
        } else if (field == DateTimeField.DAY_OF_WEEK_NUMBER) {
            extractor.extractFromDate(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 7), //
                t -> IntCellFactory.create(t.get(WeekFields.of(locale).dayOfWeek())) //
            );
        } else if (field == DateTimeField.DAY_OF_WEEK_NAME) {
            extractor.extractFromDate(nameGenerator.newColumn(suggestedColName, StringCell.TYPE), //
                t -> StringCellFactory.create(t.getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale)), //
                t -> StringCellFactory.create(t.getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale)), //
                t -> StringCellFactory.create(t.getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale)) //
            );
        } /* Time fields */ else if (field == DateTimeField.HOUR) {
            extractor.extractFromTime(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 23), //
                t -> IntCellFactory.create(t.getHour()), //
                t -> IntCellFactory.create(t.getHour()), //
                t -> IntCellFactory.create(t.getHour()) //
            );
        } else if (field == DateTimeField.MINUTE) {
            extractor.extractFromTime(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 59), //
                t -> IntCellFactory.create(t.getMinute()), //
                t -> IntCellFactory.create(t.getMinute()), //
                t -> IntCellFactory.create(t.getMinute()) //
            );
        } else if (field == DateTimeField.SECOND) {
            extractor.extractFromTime(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 59), //
                t -> IntCellFactory.create(t.getSecond()), //
                t -> IntCellFactory.create(t.getSecond()), //
                t -> IntCellFactory.create(t.getSecond()) //
            );
        } else if (field == DateTimeField.MILLISECOND) {
            extractor.extractFromTime(createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 999), //
                t -> IntCellFactory.create(t.get(ChronoField.MILLI_OF_SECOND)) //
            );
        } else if (field == DateTimeField.MICROSECOND) {
            extractor.extractFromTime(
                createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 999_999), //
                t -> IntCellFactory.create(t.get(ChronoField.MICRO_OF_SECOND)) //
            );
        } else if (field == DateTimeField.NANOSECOND) {
            extractor.extractFromTime(
                createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 999_999_999), //
                t -> IntCellFactory.create(t.get(ChronoField.NANO_OF_SECOND)) //
            );
        } /* Time zone fields */ else if (field == DateTimeField.TIME_ZONE_NAME) {
            extractor.extractFromZonedDateTime(nameGenerator.newColumn(suggestedColName, StringCell.TYPE), //
                t -> StringCellFactory.create(t.getZone().getId())//
            );
        } else if (field == DateTimeField.TIME_ZONE_OFFSET) {
            extractor.extractFromZonedDateTime(nameGenerator.newColumn(suggestedColName, StringCell.TYPE), //
                t -> StringCellFactory.create(t.getOffset().getDisplayName(TextStyle.FULL_STANDALONE, locale))//
            );
        }
    }

    private static DataColumnSpec createBoundedIntColumn(final DataColumnDomainCreator domainCreator,
        final UniqueNameGenerator nameGenerator, final String suggestedName, final int lower, final int upper) {
        domainCreator.setLowerBound(IntCellFactory.create(lower));
        domainCreator.setUpperBound(IntCellFactory.create(upper));
        final DataColumnSpecCreator specCreator = nameGenerator.newCreator(suggestedName, IntCell.TYPE);
        specCreator.setDomain(domainCreator.createDomain());
        return specCreator.createSpec();
    }

    static final class TemporalExtractor {

        private final boolean m_isZonedDateTime;

        private final boolean m_isLocalDateTime;

        private final boolean m_isLocalDate;

        private boolean m_isLocalTime;

        private final int m_selectedColIdx;

        private final ColumnRearranger m_rearranger;

        TemporalExtractor(final int selectedColIdx, final DataType selectedColType, final ColumnRearranger rearranger) {
            m_selectedColIdx = selectedColIdx;
            m_isLocalDate = selectedColType.isCompatible(LocalDateValue.class);
            m_isLocalDateTime = selectedColType.isCompatible(LocalDateTimeValue.class);
            m_isZonedDateTime = selectedColType.isCompatible(ZonedDateTimeValue.class);
            m_isLocalTime = selectedColType.isCompatible(LocalTimeValue.class);
            m_rearranger = rearranger;
        }

        void extractFromDate(final DataColumnSpec colSpec, final Function<LocalDate, DataCell> createFromLocalDate,
            final Function<LocalDateTime, DataCell> createFromLocalDateTime,
            final Function<ZonedDateTime, DataCell> createFromZonedDateTime) {
            if (m_isLocalDate) {
                addFromLocalDate(colSpec, createFromLocalDate);
            } else if (m_isLocalDateTime) {
                addFromLocalDateTime(colSpec, createFromLocalDateTime);
            } else if (m_isZonedDateTime) {
                addFromZonedDateTime(colSpec, createFromZonedDateTime);
            }
        }

        void extractFromDate(final DataColumnSpec colSpec, final Function<Temporal, DataCell> createFromTemporal) {
            extractFromDate(colSpec, //
                createFromTemporal::apply, //
                createFromTemporal::apply, //
                createFromTemporal::apply //
            );
        }

        void extractFromTime(final DataColumnSpec colSpec, final Function<LocalTime, DataCell> createFromLocalTime,
            final Function<LocalDateTime, DataCell> createFromLocalDateTime,
            final Function<ZonedDateTime, DataCell> createFromZonedDateTime) {
            if (m_isLocalTime) {
                addFromLocalTime(colSpec, createFromLocalTime);
            } else if (m_isLocalDateTime) {
                addFromLocalDateTime(colSpec, createFromLocalDateTime);
            } else if (m_isZonedDateTime) {
                addFromZonedDateTime(colSpec, createFromZonedDateTime);
            }
        }

        void extractFromTime(final DataColumnSpec colSpec, final Function<Temporal, DataCell> createFromTemporal) {
            extractFromTime(colSpec, //
                createFromTemporal::apply, //
                createFromTemporal::apply, //
                createFromTemporal::apply //
            );
        }

        void extractFromZonedDateTime(final DataColumnSpec colSpec,
            final Function<ZonedDateTime, DataCell> createFromZonedDateTime) {
            if (m_isZonedDateTime) {
                addFromZonedDateTime(colSpec, createFromZonedDateTime);
            }
        }

        private void addFromZonedDateTime(final DataColumnSpec colSpec,
            final Function<ZonedDateTime, DataCell> createFromZonedDateTime) {
            m_rearranger.append(new AbstractExtractFieldCellFactory<ZonedDateTimeValue>(m_selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return createFromZonedDateTime.apply(value.getZonedDateTime());
                }
            });
        }

        private void addFromLocalDateTime(final DataColumnSpec colSpec,
            final Function<LocalDateTime, DataCell> createFromLocalDateTime) {
            m_rearranger.append(new AbstractExtractFieldCellFactory<LocalDateTimeValue>(m_selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return createFromLocalDateTime.apply(value.getLocalDateTime());
                }
            });
        }

        private void addFromLocalDate(final DataColumnSpec colSpec,
            final Function<LocalDate, DataCell> createFromLocalDate) {
            m_rearranger.append(new AbstractExtractFieldCellFactory<LocalDateValue>(m_selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return createFromLocalDate.apply(value.getLocalDate());
                }
            });
        }

        private void addFromLocalTime(final DataColumnSpec colSpec,
            final Function<LocalTime, DataCell> createFromLocalTime) {
            m_rearranger.append(new AbstractExtractFieldCellFactory<LocalTimeValue>(m_selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return createFromLocalTime.apply(value.getLocalTime());
                }
            });
        }

        private abstract static class AbstractExtractFieldCellFactory<V extends DataValue> extends SingleCellFactory {

            private final int m_colIdx;

            AbstractExtractFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
                super(true, newColSpec);
                m_colIdx = colIdx;
            }

            @Override
            public DataCell getCell(final DataRow row) {
                final DataCell cell = row.getCell(m_colIdx);
                if (cell.isMissing()) {
                    return DataType.getMissingCell();
                }
                @SuppressWarnings("unchecked")
                final V value = (V)cell;
                return getCell(value);
            }

            protected abstract DataCell getCell(final V value);
        }

    }

}
