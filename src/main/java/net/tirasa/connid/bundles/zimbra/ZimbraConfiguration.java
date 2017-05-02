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

import com.zimbra.common.soap.AdminConstants;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Set of configuration properties for connecting to Zimbra.
 */
public class ZimbraConfiguration extends AbstractConfiguration {

    private static final Log LOG = Log.getLog(ZimbraConfiguration.class);

    /**
     * The Administration Service Location value
     */
    private String adminServiceLocation = "https://localhost:7071" + AdminConstants.ADMIN_SERVICE_URI;

    /**
     * The Administrator Username value
     */
    private String adminUsername = "admin";

    /**
     * The Administrator Password value
     */
    private GuardedString adminPassword = null;

    /**
     * The E-mail Domain Name value
     */
    private String emailDomainName = null;

    /**
     * Administration Service Location getter
     *
     * @return adminServiceLocation value
     */
    @ConfigurationProperty(order = 1,
            displayMessageKey = ZimbraConstants.ZIMBRA_ADMIN_SERVICE_LOCATION_DISPLAY,
            helpMessageKey = ZimbraConstants.ZIMBRA_ADMIN_SERVICE_LOCATION_HELP, required = true)
    public String getAdminServiceLocation() {
        final String VARIABLE = "adminServiceLocation";
        LOG.ok("{0} is {1}", VARIABLE, adminServiceLocation);
        return adminServiceLocation;
    }

    /**
     * Administration Service Location setter
     *
     * @param adminServiceLocation value
     */
    public void setAdminServiceLocation(String adminServiceLocation) {
        final String VARIABLE = "adminServiceLocation";
        LOG.ok("set {0} to {1}", VARIABLE, adminServiceLocation);
        this.adminServiceLocation = adminServiceLocation;
    }

    /**
     * Administrator Username getter
     *
     * @return adminUsername value
     */
    @ConfigurationProperty(order = 2,
            displayMessageKey = ZimbraConstants.ZIMBRA_ADMIN_USERNAME_DISPLAY,
            helpMessageKey = ZimbraConstants.ZIMBRA_ADMIN_USERNAME_HELP, required = true)
    public String getAdminUsername() {
        final String VARIABLE = "adminUsername";
        LOG.ok("{0} is {1}", VARIABLE, adminUsername);
        return adminUsername;
    }

    /**
     * Administrator Username setter
     *
     * @param adminUsername value
     */
    public void setAdminUsername(String adminUsername) {
        final String VARIABLE = "adminUsername";
        LOG.ok("set {0} to {1}", VARIABLE, adminUsername);
        this.adminUsername = adminUsername;
    }

    /**
     * Administrator Password getter
     *
     * @return adminPassword value
     */
    @ConfigurationProperty(order = 3, confidential = true,
            displayMessageKey = ZimbraConstants.ZIMBRA_ADMIN_PASSWORD_DISPLAY,
            helpMessageKey = ZimbraConstants.ZIMBRA_ADMIN_PASSWORD_HELP, required = true)
    public GuardedString getAdminPassword() {
        final String VARIABLE = "adminPassword";
        LOG.ok("{0} is {1}", VARIABLE, (adminPassword != null ? "********" : null));
        return adminPassword;
    }

    /**
     * Administrator Password setter
     *
     * @param adminPassword value
     */
    public void setAdminPassword(GuardedString adminPassword) {
        final String VARIABLE = "adminPassword";
        LOG.ok("set {0} to {1}", VARIABLE, (adminPassword != null ? "********" : null));
        this.adminPassword = adminPassword;
    }

    /**
     * E-mail Domain Name getter
     *
     * @return emailDomainName value
     */
    @ConfigurationProperty(order = 4,
            displayMessageKey = ZimbraConstants.ZIMBRA_EMAIL_DOMAIN_NAME_DISPLAY,
            helpMessageKey = ZimbraConstants.ZIMBRA_EMAIL_DOMAIN_NAME_HELP)
    public String getEmailDomainName() {
        final String VARIABLE = "emailDomainName";
        LOG.ok("{0} is {1}", VARIABLE, emailDomainName);
        return emailDomainName;
    }

    /**
     * E-mail Domain Name setter
     *
     * @param emailDomainName value
     */
    public void setEmailDomainName(String emailDomainName) {
        final String VARIABLE = "emailDomainName";
        LOG.ok("set {0} to {1}", VARIABLE, emailDomainName);
        this.emailDomainName = emailDomainName;
    }

    /**
     * Attempt to validate the arguments added to the Configuration.
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        final String METHOD = "validate";
        LOG.ok("enter {0}", METHOD);
        if (StringUtil.isBlank(adminServiceLocation)) {
            throw new ConfigurationException(getConnectorMessages().format(
                    ZimbraConstants.ZIMBRA_ADMIN_SERVICE_LOCATION_REQUIRED, null));
        }
        if (StringUtil.isBlank(adminUsername)) {
            throw new ConfigurationException(getConnectorMessages().format(
                    ZimbraConstants.ZIMBRA_ADMIN_USERNAME_REQUIRED, null));
        }
        LOG.ok("exit {0}", METHOD);
    }
}
