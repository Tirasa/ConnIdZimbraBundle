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

import java.util.Set;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;

public final class ZimbraSpecialAttributes {

    /** __ALIASES__ */
    public static final String ALIASES_NAME = AttributeUtil.createSpecialName("ALIASES");

    /** __GROUPS__ */
    public static final String GROUPS_NAME = AttributeUtil.createSpecialName("GROUPS");

    /** __MEMBERS__ */
    public static final String MEMBERS_NAME = AttributeUtil.createSpecialName("MEMBERS");
    
    public static final String DISTRIBUTION_LISTS_NAME = AttributeUtil.createSpecialName("DISTRIBUTION_LISTS");

    public final static Set<String> ZIMBRA_ATTRIBUTE_NAMES =
            CollectionUtil.newReadOnlySet(ALIASES_NAME, GROUPS_NAME, MEMBERS_NAME, DISTRIBUTION_LISTS_NAME);

    public static Set<String> getZimbraSpecialAttributeNames() {
        return CollectionUtil.newReadOnlySet(ZIMBRA_ATTRIBUTE_NAMES);
    }

    public static boolean isZimbraSpecialAttribute(Attribute attr) {
        if (attr != null) {
            return ZIMBRA_ATTRIBUTE_NAMES.contains(attr.getName());
        }
        return false;
    }
}