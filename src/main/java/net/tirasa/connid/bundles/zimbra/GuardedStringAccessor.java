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

import java.util.Arrays;
import org.identityconnectors.common.security.GuardedString;

public class GuardedStringAccessor implements GuardedString.Accessor {

    private char[] clearChars;

    @Override
    public void access(final char[] clearChars) {
        this.clearChars = new char[clearChars.length];
        System.arraycopy(clearChars, 0, this.clearChars, 0, clearChars.length);
    }

    public void clean() {
        Arrays.fill(clearChars, 0, clearChars.length, '\0');
    }

    public char[] getArray() {
        return clearChars;
    }

    public static String toString(GuardedString gs) {
        if (gs != null) {
            GuardedStringAccessor gsa = new GuardedStringAccessor();
            try {
                gs.access(gsa);
                return new String(gsa.getArray());
            } finally {
                gsa.clean();
            }
        }
        return null;
    }
}
