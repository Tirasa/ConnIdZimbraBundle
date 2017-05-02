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

import java.util.EnumSet;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;

public final class ZimbraSpecialAttributeInfos {

    public static final AttributeInfo ALIASES = AttributeInfoBuilder.build(
            ZimbraSpecialAttributes.ALIASES_NAME, String.class, EnumSet.of(Flags.MULTIVALUED));

    public static final AttributeInfo GROUPS = AttributeInfoBuilder.build(
            ZimbraSpecialAttributes.GROUPS_NAME, String.class, EnumSet.of(Flags.MULTIVALUED));

    public static final AttributeInfo MEMBERS = AttributeInfoBuilder.build(
            ZimbraSpecialAttributes.MEMBERS_NAME, String.class, EnumSet.of(Flags.MULTIVALUED));

}
