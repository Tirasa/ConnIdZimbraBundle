/**
 * Copyright (C) 2017 ConnId (connid-dev@googlegroups.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tirasa.connid.bundles.zimbra;

import java.io.UnsupportedEncodingException;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsIgnoreCaseFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;

public class ZimbraFilterTranslator extends AbstractFilterTranslator<String> {

    private static final Log LOG = Log.getLog(ZimbraFilterTranslator.class);

    private final ObjectClass objectClass;

    public ZimbraFilterTranslator(ObjectClass objectClass) {
        this.objectClass = objectClass;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createAndExpression(String leftExpression, String rightExpression) {
        return createAndOrExpression("&", leftExpression, rightExpression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createOrExpression(String leftExpression, String rightExpression) {
        return createAndOrExpression("|", leftExpression, rightExpression);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createContainsExpression(ContainsFilter filter, boolean not) {
        return createNotExpression(filter, "=", "*", "*", not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createEndsWithExpression(EndsWithFilter filter, boolean not) {
        return createNotExpression(filter, "=", "*", "", not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createStartsWithExpression(StartsWithFilter filter, boolean not) {
        return createNotExpression(filter, "=", "", "*", not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createGreaterThanExpression(GreaterThanFilter filter, boolean not) {
        return createNotExpression(filter, "<=", "", "", !not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createGreaterThanOrEqualExpression(GreaterThanOrEqualFilter filter, boolean not) {
        return createNotExpression(filter, ">=", "", "", not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createLessThanExpression(LessThanFilter filter, boolean not) {
        return createNotExpression(filter, ">=", "", "", !not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createLessThanOrEqualExpression(LessThanOrEqualFilter filter, boolean not) {
        return createNotExpression(filter, "<=", "", "", not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createEqualsExpression(EqualsFilter filter, boolean not) {
        return createNotExpression(filter, "=", "", "", not);
    }

    @Override
    protected String createEqualsIgnoreCaseExpression(EqualsIgnoreCaseFilter filter, boolean not) {
        // Zimbra is on LDAP, then generally case-insensitive, reverting to EqualsFilter
        Attribute attr = filter.getValue() == null
                ? AttributeBuilder.build(filter.getName())
                : AttributeBuilder.build(filter.getName(), filter.getValue());
        return createEqualsExpression(new EqualsFilter(attr), not);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String createContainsAllValuesExpression(ContainsAllValuesFilter filter, boolean not) {
        return createNotExpression(filter, "=", "", "", not);
    }

    private String createNotExpression(AttributeFilter filter, String operator, String prefix, String suffix,
            boolean not) {
        String expression = createExpression(filter, operator, prefix, suffix);
        if (not) {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            sb.append("!");
            sb.append(expression);
            sb.append(")");
            return sb.toString();
        }
        return expression;
    }

    private static String createAndOrExpression(String operator, String... operands) {
        StringBuilder sb = new StringBuilder();
        sb.append("(");
        sb.append(operator);
        for (String s : operands) {
            sb.append(s);
        }
        sb.append(")");
        return sb.toString();
    }

    private String createExpression(AttributeFilter filter, String operator, String prefix, String suffix) {
        LOG.ok("filter {0} ({1}) = {2}", filter.getName(), filter.getAttribute().getName(), filter.getAttribute().
                getValue());
        StringBuilder sb = new StringBuilder();

        String zimbraName = ZimbraConnector.toZimbraAttributeName(objectClass, filter.getName());
        Object zimbraValue = ZimbraConnector.toZimbraAttributeValue(objectClass, filter.getAttribute());

        if (zimbraValue instanceof Object[]) {
            sb.append('(');
            sb.append('&');
            for (Object value : (Object[]) zimbraValue) {
                createCondition(sb, zimbraName, operator, prefix, suffix, value);
            }
            sb.append(')');
        } else {
            createCondition(sb, zimbraName, operator, prefix, suffix, zimbraValue);
        }
        return sb.toString();
    }

    private static void createCondition(StringBuilder sb, String name, String operator, String prefix, String suffix,
            Object value) {
        sb.append('(');
        sb.append(name);
        sb.append(operator);
        sb.append(prefix);
        if (value instanceof byte[]) {
            escapeByteArray(sb, (byte[]) value);
        } else {
            escapeString(sb, value.toString());
        }
        sb.append(suffix);
        sb.append(')');
    }

    private static void escapeByte(StringBuilder sb, byte b) {
        sb.append('\\');
        sb.append(String.format("%02x", 0xff & b));
    }

    private static void escapeByteArray(StringBuilder sb, byte[] bytes) {
        for (byte b : bytes) {
            escapeByte(sb, b);
        }
    }

    private static void escapeChar(StringBuilder sb, char c) {
        try {
            byte[] utf8bytes = String.valueOf(c).getBytes("UTF-8");
            escapeByteArray(sb, utf8bytes);
        } catch (UnsupportedEncodingException originalException) {
            throw new ConnectionBrokenException(originalException);
        }
    }

    private static void escapeString(StringBuilder sb, String s) {
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\0':
                case '(':
                case ')':
                case '*':
                case '\\':
                    escapeChar(sb, c);
                    break;
                default:
                    if (c <= 0x7f) {
                        sb.append(c);
                    } else {
                        escapeChar(sb, c);
                    }
                    break;
            }
        }
    }
}
