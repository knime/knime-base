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

import java.time.format.TextStyle;
import java.time.temporal.ChronoField;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.Locale;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.WordUtils;
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
import org.knime.core.webui.node.dialog.defaultdialog.widget.Label;
import org.knime.core.webui.node.impl.WebUINodeConfiguration;
import org.knime.core.webui.node.impl.WebUISimpleStreamableFunctionNodeModel;
import org.knime.time.node.extract.datetime.ExtractDateTimeFieldsSettings.DateTimeField;

/**
 *
 * @author Christian Albrecht, KNIME GmbH, Konstanz, Germany
 */
@SuppressWarnings("restriction")
public class ExtractDateTimeFieldsNodeModelWebUI extends WebUISimpleStreamableFunctionNodeModel<ExtractDateTimeFieldsSettings> {

    /**
     * @param configuration
     */
    protected ExtractDateTimeFieldsNodeModelWebUI(final WebUINodeConfiguration configuration) {
        super(configuration, ExtractDateTimeFieldsSettings.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ColumnRearranger createColumnRearranger(final DataTableSpec spec,
        final ExtractDateTimeFieldsSettings modelSettings) throws InvalidSettingsException {
        final String selectedCol = modelSettings.m_selectedColumn.getSelected();
        if (selectedCol == null || selectedCol.isEmpty()) {
            throw new InvalidSettingsException("Node must be configured.");
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

        Arrays.stream(modelSettings.m_extractFields).forEachOrdered(field -> {
            extractField(field.m_field, field.m_columnName, selectedColIdx, selectedColSpec.getType(), locale,
                nameGenerator, domainCreator, rearranger);
        });

        if (rearranger.getColumnCount() == spec.getNumColumns()) {
            getLogger().info("No fields will be extracted. Output table will equal input table.");
        }

        return rearranger;
    }

    private static void extractField(final DateTimeField field, final String newColumnName, final int selectedColIdx,
        final DataType selectedColType, final Locale locale, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {

        final boolean isLocalDate = selectedColType.isCompatible(LocalDateValue.class);
        final boolean isLocalTime = selectedColType.isCompatible(LocalTimeValue.class);
        final boolean isLocalDateTime = selectedColType.isCompatible(LocalDateTimeValue.class);
        final boolean isZonedDateTime = selectedColType.isCompatible(ZonedDateTimeValue.class);

        String suggestedColName = newColumnName;
        if (StringUtils.isEmpty(suggestedColName)) {
            try {
                suggestedColName = DateTimeField.class.getField(field.name()).getAnnotation(Label.class).value();
                //TODO: find or create a general util for column names?
                suggestedColName = WordUtils.capitalizeFully(suggestedColName);
            } catch (NoSuchFieldException | SecurityException ex) {
                // TODO: throw an implementation error
            }
        }

        // Date fields
        if (field == DateTimeField.YEAR) {
            extractYear(suggestedColName, selectedColIdx, isLocalDate, isLocalTime, isZonedDateTime, nameGenerator,
                rearranger);
        } else if (field == DateTimeField.YEAR_WEEK_BASED) {
            extractYearWeekBased(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                locale, nameGenerator, rearranger);
        } else if (field == DateTimeField.QUARTER) {
            extractQuarter(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.MONTH_NUMBER) {
            extractMonthNumber(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.MONTH_NAME) {
            extractMonthName(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime, locale,
                nameGenerator, rearranger);
        } else if (field == DateTimeField.WEEK) {
            extractWeekOfYearNumber(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                locale, nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.DAY_OF_YEAR) {
            extractDayOfYearNumber(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.DAY_OF_MONTH) {
            extractDayOfMonthNumber(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.DAY_OF_WEEK_NUMBER) {
            extractDayOfWeekNumber(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                locale, nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.DAY_OF_WEEK_NAME) {
            extractDayOfWeekName(suggestedColName, selectedColIdx, isLocalDate, isLocalDateTime, isZonedDateTime,
                locale, nameGenerator, rearranger);
        }
        // Time fields
        else if (field == DateTimeField.HOUR) {
            extractHours(suggestedColName, selectedColIdx, isLocalTime, isLocalDateTime, isZonedDateTime, nameGenerator,
                domainCreator, rearranger);
        } else if (field == DateTimeField.MINUTE) {
            extractMinutes(suggestedColName, selectedColIdx, isLocalTime, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.SECOND) {
            extractSeconds(suggestedColName, selectedColIdx, isLocalTime, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.MILLISECOND) {
            extractMillis(suggestedColName, selectedColIdx, isLocalTime, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.MICROSECOND) {
            extractMicros(suggestedColName, selectedColIdx, isLocalTime, isLocalDateTime, isZonedDateTime,
                nameGenerator, domainCreator, rearranger);
        } else if (field == DateTimeField.NANOSECOND) {
            extractNanos(suggestedColName, selectedColIdx, isLocalTime, isLocalDateTime, isZonedDateTime, nameGenerator,
                domainCreator, rearranger);
        }
        // Time zone fields
        else if (field == DateTimeField.TIME_ZONE_NAME) {
            extractTimeZoneName(suggestedColName, selectedColIdx, locale, isZonedDateTime, nameGenerator, rearranger);
        } else if (field == DateTimeField.TIME_ZONE_OFFSET) {
            extractTimeZoneOffset(suggestedColName, selectedColIdx, locale, isZonedDateTime, nameGenerator, rearranger);
        }
    }

    private static void extractYear(final String suggestedColName, final int selectedColIdx, final boolean isLocalDate,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = nameGenerator.newColumn(suggestedColName, IntCell.TYPE);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().getYear());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getYear());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getYear());
                }
            });
        }
    }

    private static void extractYearWeekBased(final String suggestedColName, final int selectedColIdx,
        final boolean isLocalDate, final boolean isLocalDateTime, final boolean isZonedDateTime, final Locale locale,
        final UniqueNameGenerator nameGenerator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = nameGenerator.newColumn(suggestedColName, IntCell.TYPE);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().get(WeekFields.of(locale).weekBasedYear()));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().get(WeekFields.of(locale).weekBasedYear()));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().get(WeekFields.of(locale).weekBasedYear()));
                }
            });
        }
    }

    private static void extractQuarter(final String suggestedColName, final int selectedColIdx,
        final boolean isLocalDate, final boolean isLocalDateTime, final boolean isZonedDateTime,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 4);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create((value.getLocalDate().getMonthValue() + 2) / 3);
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create((value.getLocalDateTime().getMonthValue() + 2) / 3);
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create((value.getZonedDateTime().getMonthValue() + 2) / 3);
                }
            });
        }
    }

    private static void extractMonthNumber(final String suggestedColName, final int selectedColIdx, final boolean isLocalDate,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 12);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().getMonthValue());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getMonthValue());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getMonthValue());
                }
            });
        }
    }

    private static void extractMonthName(final String suggestedColName, final int selectedColIdx, final boolean isLocalDate,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final Locale locale,
        final UniqueNameGenerator nameGenerator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = nameGenerator.newColumn(suggestedColName, StringCell.TYPE);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return StringCellFactory
                        .create(value.getLocalDate().getMonth().getDisplayName(TextStyle.FULL, locale));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return StringCellFactory
                        .create(value.getLocalDateTime().getMonth().getDisplayName(TextStyle.FULL, locale));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return StringCellFactory
                        .create(value.getZonedDateTime().getMonth().getDisplayName(TextStyle.FULL, locale));
                }
            });
        }
    }

    private static void extractWeekOfYearNumber(final String suggestedColName, final int selectedColIdx, // NOSONAR we need all this info
        final boolean isLocalDate, final boolean isLocalDateTime, final boolean isZonedDateTime, final Locale locale,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 52);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().get(WeekFields.of(locale).weekOfWeekBasedYear()));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory
                        .create(value.getLocalDateTime().get(WeekFields.of(locale).weekOfWeekBasedYear()));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory
                        .create(value.getZonedDateTime().get(WeekFields.of(locale).weekOfWeekBasedYear()));
                }
            });
        }
    }

    private static void extractDayOfYearNumber(final String suggestedColName, final int selectedColIdx, final boolean isLocalDate,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 366);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().getDayOfYear());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getDayOfYear());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getDayOfYear());
                }
            });
        }
    }

    private static void extractDayOfMonthNumber(final String suggestedColName, final int selectedColIdx, final boolean isLocalDate,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 31);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().getDayOfMonth());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getDayOfMonth());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getDayOfMonth());
                }
            });
        }
    }

    private static void extractDayOfWeekNumber(final String suggestedColName, final int selectedColIdx, // NOSONAR we need all this info
        final boolean isLocalDate, final boolean isLocalDateTime, final boolean isZonedDateTime, final Locale locale,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 1, 7);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return IntCellFactory.create(value.getLocalDate().get(WeekFields.of(locale).dayOfWeek()));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().get(WeekFields.of(locale).dayOfWeek()));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().get(WeekFields.of(locale).dayOfWeek()));
                }
            });
        }
    }

    private static void extractDayOfWeekName(final String suggestedColName, final int selectedColIdx, final boolean isLocalDate,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final Locale locale,
        final UniqueNameGenerator nameGenerator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = nameGenerator.newColumn(suggestedColName, StringCell.TYPE);
        if (isLocalDate) {
            rearranger.append(new AbstractLocalDateFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateValue value) {
                    return StringCellFactory
                        .create(value.getLocalDate().getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return StringCellFactory.create(
                        value.getLocalDateTime().getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return StringCellFactory.create(
                        value.getZonedDateTime().getDayOfWeek().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                }
            });
        }
    }

    private static void extractHours(final String suggestedColName, final int selectedColIdx, final boolean isLocalTime,
        final boolean isLocalDateTime, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 23);
        if (isLocalTime) {
            rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return IntCellFactory.create(value.getLocalTime().getHour());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getHour());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getHour());
                }
            });
        }
    }

    private static void extractMinutes(final String suggestedColName, final int selectedColIdx,
        final boolean isLocalTime, final boolean isLocalDateTime, final boolean isZonedDateTime,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 59);
        if (isLocalTime) {
            rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return IntCellFactory.create(value.getLocalTime().getMinute());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getMinute());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getMinute());
                }
            });
        }
    }

    private static void extractSeconds(final String suggestedColName, final int selectedColIdx,
        final boolean isLocalTime, final boolean isLocalDateTime, final boolean isZonedDateTime,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec = createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 59);
        if (isLocalTime) {
            rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return IntCellFactory.create(value.getLocalTime().getSecond());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getSecond());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getSecond());
                }
            });
        }
    }

    private static void extractMillis(final String suggestedColName, final int selectedColIdx,
        final boolean isLocalTime, final boolean isLocalDateTime, final boolean isZonedDateTime,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec =
            createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 999);
        if (isLocalTime) {
            rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return IntCellFactory.create(value.getLocalTime().get(ChronoField.MILLI_OF_SECOND));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().get(ChronoField.MILLI_OF_SECOND));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().get(ChronoField.MILLI_OF_SECOND));
                }
            });
        }
    }

    private static void extractMicros(final String suggestedColName, final int selectedColIdx,
        final boolean isLocalTime, final boolean isLocalDateTime, final boolean isZonedDateTime,
        final UniqueNameGenerator nameGenerator, final DataColumnDomainCreator domainCreator,
        final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec =
            createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 999_999);
        if (isLocalTime) {
            rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return IntCellFactory.create(value.getLocalTime().get(ChronoField.MICRO_OF_SECOND));
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().get(ChronoField.MICRO_OF_SECOND));
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().get(ChronoField.MICRO_OF_SECOND));
                }
            });
        }
    }

    private static void extractNanos(final String suggestedColName, final int selectedColIdx, final boolean isLocalTime, final boolean isLocalDateTime,
        final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final DataColumnDomainCreator domainCreator, final ColumnRearranger rearranger) {
        final DataColumnSpec colSpec =
            createBoundedIntColumn(domainCreator, nameGenerator, suggestedColName, 0, 999_999_999);

        if (isLocalTime) {
            rearranger.append(new AbstractLocalTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalTimeValue value) {
                    return IntCellFactory.create(value.getLocalTime().getNano());
                }
            });
        } else if (isLocalDateTime) {
            rearranger.append(new AbstractLocalDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final LocalDateTimeValue value) {
                    return IntCellFactory.create(value.getLocalDateTime().getNano());
                }
            });
        } else if (isZonedDateTime) {
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return IntCellFactory.create(value.getZonedDateTime().getNano());
                }
            });
        }
    }

    private static void extractTimeZoneName(final String suggestedColName, final int selectedColIdx,
        final Locale locale, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final ColumnRearranger rearranger) {
        if (isZonedDateTime) {
            final DataColumnSpec colSpec = nameGenerator.newColumn(suggestedColName, StringCell.TYPE);
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return StringCellFactory.create(value.getZonedDateTime().getZone().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                }
            });
        }
    }

    private static void extractTimeZoneOffset(final String suggestedColName, final int selectedColIdx,
        final Locale locale, final boolean isZonedDateTime, final UniqueNameGenerator nameGenerator,
        final ColumnRearranger rearranger) {
        if (isZonedDateTime) {
            final DataColumnSpec colSpec = nameGenerator.newColumn(suggestedColName, StringCell.TYPE);
            rearranger.append(new AbstractZonedDateTimeFieldCellFactory(selectedColIdx, colSpec) {

                @Override
                protected DataCell getCell(final ZonedDateTimeValue value) {
                    return StringCellFactory
                        .create(value.getZonedDateTime().getOffset().getDisplayName(TextStyle.FULL_STANDALONE, locale));
                }
            });
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

    // cell factories:

    private abstract static class AbstractLocalDateFieldCellFactory
        extends AbstractExtractFieldCellFactory<LocalDateValue> {
        AbstractLocalDateFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract static class AbstractLocalTimeFieldCellFactory
        extends AbstractExtractFieldCellFactory<LocalTimeValue> {
        AbstractLocalTimeFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract static class AbstractLocalDateTimeFieldCellFactory
        extends AbstractExtractFieldCellFactory<LocalDateTimeValue> {
        AbstractLocalDateTimeFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
    }

    private abstract static class AbstractZonedDateTimeFieldCellFactory
        extends AbstractExtractFieldCellFactory<ZonedDateTimeValue> {
        AbstractZonedDateTimeFieldCellFactory(final int colIdx, final DataColumnSpec newColSpec) {
            super(colIdx, newColSpec);
        }
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
