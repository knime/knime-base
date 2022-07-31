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
 *   24.07.2022. (Kesa): created
 */
package org.knime.base.node.io.filehandling.arff.reader;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.text.StringTokenizer;
import org.apache.commons.text.matcher.StringMatcherFactory;

/**
 *  Data from declarations written in the ARFF file header section.
 *
 * @author Dragan Keselj, Redfield SE
 */
public class ARFFSpec {

    private static final Set<Character> INVALID_CHARS = Set.of('{', '}', ',', '%');

    private static final String DECLARATION_RELATION = "@RELATION";

    private static final String DECLARATION_ATTRIBUTE = "@ATTRIBUTE";

    private static final String DECLARATION_DATA = "@DATA";

    private String m_relationName;

    private final List<Attribute> m_attributes;

    /**
     * Date/time formatters per column index for columns where date-format is specified in the arff file.
     */
    private final Map<Integer, Pair<String, DateTimeFormatter>> m_formattedDateColumns;

    /**
     * Reads file specs from the ARFF file header and sets pointer to the first data row.
     *
     * @param reader
     * @throws IOException
     */
    public ARFFSpec(final BufferedReader reader) throws IOException {
        super();
        this.m_relationName = getRelationName(reader);
        this.m_attributes = getAttributes(reader);
        this.m_formattedDateColumns = m_attributes.stream() //
            .filter(a -> a.getType() == AttributeType.DATE && !StringUtils.isBlank(a.getOther())) //
            .collect(Collectors.toUnmodifiableMap(a -> m_attributes.indexOf(a), //NOSONAR
                a -> Pair.of(a.getOther().trim(), //
                    new DateTimeFormatterBuilder() //
                        .appendPattern(a.getOther().trim()) //
                        .toFormatter(Locale.getDefault()))) //
            );
    }

    /**
     * Reads relation name.
     *
     * @param reader of the file.
     * @return relation name from <code>@RELATION</code> declaration.
     * @throws IOException
     */
    private static String getRelationName(final BufferedReader reader) throws IOException {
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                if (line.trim().toUpperCase().startsWith(DECLARATION_RELATION)) {
                    return parseRelationName(line);
                } else {
                    throw new IllegalArgumentException(DECLARATION_RELATION + " section must be at beginning!");
                }
            }
        }
        throw new IllegalArgumentException(DECLARATION_RELATION + " section is missing!");
    }

    private static String parseRelationName(final String line) {
        final var tokenizer = new StringTokenizer(line);
        tokenizer.setDelimiterMatcher(StringMatcherFactory.INSTANCE.charSetMatcher(' ', '\t')) //
            .setQuoteMatcher(StringMatcherFactory.INSTANCE.quoteMatcher());
        final var relTokens = tokenizer.getTokenArray();
        if (relTokens == null || relTokens.length != 2) {
            throw new IllegalArgumentException("Invalid " + DECLARATION_RELATION + " section!");
        }
        final var relationName = relTokens[1];
        checkName(relationName);
        return relationName;
    }

    /**
     * Reads attributes.
     *
     * @param reader of the file.
     * @return <code>List<{@link Attribute}></code> from <code>@ATTRIBUTE</code> declaration.
     * @throws IOException
     */
    private static List<Attribute> getAttributes(final BufferedReader reader) throws IOException {
        List<Attribute> attributes = new ArrayList<>();

        final var tokenizer = new StringTokenizer();
        tokenizer.setDelimiterMatcher(StringMatcherFactory.INSTANCE.charSetMatcher(' ', '\t')) //
            .setQuoteMatcher(StringMatcherFactory.INSTANCE.quoteMatcher());

        String line = null;
        while ((line = reader.readLine()) != null) {
            if (!line.isBlank()) {
                if (!line.trim().toUpperCase().startsWith(DECLARATION_ATTRIBUTE)) {
                    break;
                } else {
                    final var attribute = parseAttribute(line, tokenizer);
                    attributes.add(attribute);
                }
            }
        }
        if (attributes.isEmpty()) {
            throw new IllegalArgumentException(DECLARATION_ATTRIBUTE + " section is missing!");
        }
        // sets pointer to the first data line
        while (line != null && !line.trim().equalsIgnoreCase(DECLARATION_DATA)) {
            line = reader.readLine();
            if (line == null) {
                throw new IllegalArgumentException(DECLARATION_DATA + " section is missing!");
            }
        }
        return attributes;
    }

    /**
     * Extracts attribute's type and name from a <code>String</code>.
     *
     * @param line with <code>@ATTRIBUTE</code> declaration.
     * @param tokenizer for parsing <code>@ATTRIBUTE</code> line.
     * @return {@link Attribute} with name and {@link AttributeType}.
     */
    private static Attribute parseAttribute(final String line, final StringTokenizer tokenizer) {
        if (!line.trim().toUpperCase().startsWith(DECLARATION_ATTRIBUTE)) {
            throw new IllegalArgumentException(DECLARATION_ATTRIBUTE + " declaration is missing!");
        }
        tokenizer.reset(line);
        final var attrTokens = tokenizer.getTokenArray();
        if (attrTokens == null || attrTokens.length < 3) {
            throw new IllegalArgumentException("Invalid data in " + DECLARATION_ATTRIBUTE + " section! " + attrTokens);
        }
        final var attrName = attrTokens[1];
        checkName(attrName);
        final var attrType = AttributeType.getType(attrTokens[2]);
        var attrOther = "";
        if (attrType == AttributeType.NOMINAL) {
            final var nominalStr = Arrays.stream(Arrays.copyOfRange(attrTokens, 2, attrTokens.length))
                .collect(Collectors.joining("")).trim();
            if (nominalStr.charAt(nominalStr.length() - 1) != '}') {
                throw new IllegalArgumentException("'}' is missing");
            }
            attrOther = nominalStr.trim().substring(1, nominalStr.length() - 1);
        } else {
            attrOther = attrTokens.length > 3 ? attrTokens[3] : "";
        }
        return new Attribute(attrName, attrType, attrOther);
    }

    /**
     * Checks if given name is valid.
     *
     * @param name
     */
    private static void checkName(final String name) {
        final var firstChar = name.charAt(0);
        if (firstChar < '\u0021' || INVALID_CHARS.contains(firstChar)) {
            throw new IllegalArgumentException(name + " is invalid!");
        }
    }

    String getRelationName() {
        return m_relationName;
    }

    List<Attribute> getAttributes() {
        return m_attributes;
    }

    Map<Integer, Pair<String, DateTimeFormatter>> getFormattedDateColumns() {
        return m_formattedDateColumns;
    }

    static class Attribute {

        private String m_name;

        private AttributeType m_type;

        private String m_other;

        Attribute(final String name, final AttributeType type, final String other) {
            super();
            m_name = name;
            m_type = type;
            m_other = other;
        }

        String getName() {
            return m_name;
        }

        AttributeType getType() {
            return m_type;
        }

        String getOther() {
            return m_other;
        }

        Class<?> getDefaultJavaType() { //NOSONAR
            switch(m_type) {
                case DATE:
                    return LocalDateTime.class;
                case INTEGER:
                    return Integer.class;
                case NOMINAL:
                    return String.class;
                case NUMERIC:
                    return Double.class;
                case REAL:
                    return Double.class;
                case STRING:
                    return String.class;
                default:
                    return String.class;
            }
        }

    }

    enum AttributeType {

            STRING("STRING"), NUMERIC("NUMERIC"), REAL("REAL"),
            INTEGER("INTEGER"), DATE("DATE"), NOMINAL("NOMINAL");

        private String m_id;

        private AttributeType(final String id) {
            this.m_id = id;
        }

        String getId() {
            return m_id;
        }

        static AttributeType getType(final String typeId) {
            if (typeId == null) {
                throw new IllegalArgumentException("Attribute type id is null");
            }
            final String id = typeId.trim();
            if (id.charAt(0) == '{') {
                return NOMINAL;
            }
            for (var type : AttributeType.values()) {
                if (type.getId().equalsIgnoreCase(id.trim())) {
                    return type;
                }
            }
            throw new IllegalArgumentException("Invalid attribute type id: " + typeId);
        }
    }
}
