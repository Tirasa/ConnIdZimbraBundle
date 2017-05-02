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

import static org.junit.Assert.assertNotNull;

import com.zimbra.common.soap.AdminConstants;
import com.zimbra.cs.account.Provisioning;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorInfoManagerFactory;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Attempts to test that the configuration options can validate the input given
 * them. It also attempt to make sure the properties are correct.
 */
public class ZimbraConnectorTests {

    private static final String CONNECTOR_NAME = "net.tirasa.connid.bundles.zimbra.ZimbraConnector";

    private static final String CONNECTOR_BUNDLE_DIRECTORY = "./dist";

    private static final String CONNECTOR_BUNDLE_NAME = "net.tirasa.connid.bundles.zimbra";

    private static final String CONNECTOR_BUNDLE_VERSION = "1.0.0-SNAPSHOT";

    private static final String ADMIN_SERVICE_LOCATION_KEY = "adminServiceLocation";

    private static final String ADMIN_SERVICE_LOCATION =
            "https://sanita.padova.dev:7071" + AdminConstants.ADMIN_SERVICE_URI;

    private static final String ADMIN_USERNAME_KEY = "adminUsername";

    private static final String ADMIN_USERNAME = "admin";

    private static final String ADMIN_PASSWORD_KEY = "adminPassword";

    private static final GuardedString ADMIN_PASSWORD = new GuardedString("s1r102001".toCharArray());

    private static final String EMAIL_DOMAIN_NAME_KEY = "emailDomainName";

    private static final String EMAIL_DOMAIN_NAME = "sanita.padova.dev";

    private static final String ACCOUNT_NAME = "guest";

    private static final String ACCOUNT_ALIAS = "ospite";

    private static final String ACCOUNT_DESCRIPTION = "Guest User";

    private static final GuardedString ACCOUNT_PASSWORD = new GuardedString("welcome".toCharArray());

    private static final String GIVEN_NAME = "Guest";

    private static final String SN = "User";

    private static final String GROUP_NAME = "everyone";

    private static final String GROUP_ALIAS = "tutti";

    private static final String GROUP_DESCRIPTION = "Everyone Group";

    private static ConfigurationProperties zimbraConfigurationProperties;

    private static ConnectorFacade zimbraConnectorFacade;

    @BeforeClass
    public static void oneTimeSetUp() throws Exception {
        File connectorBundleDirectory = new File(CONNECTOR_BUNDLE_DIRECTORY);
        URL zimbraConnectorBundleURL = IOUtil.makeURL(connectorBundleDirectory, CONNECTOR_BUNDLE_NAME + "-"
                + CONNECTOR_BUNDLE_VERSION + ".jar");
        ConnectorInfoManagerFactory zimbraConnectorInfoManagerFactory = ConnectorInfoManagerFactory.getInstance();
        ConnectorInfoManager zimbraConnectorInfoManager = zimbraConnectorInfoManagerFactory.getLocalManager(
                zimbraConnectorBundleURL);
        ConnectorInfo zimbraConnectorInfo = zimbraConnectorInfoManager.findConnectorInfo(new ConnectorKey(
                CONNECTOR_BUNDLE_NAME, CONNECTOR_BUNDLE_VERSION, CONNECTOR_NAME));
        APIConfiguration zimbraAPIConfiguration = zimbraConnectorInfo.createDefaultAPIConfiguration();
        zimbraConfigurationProperties = zimbraAPIConfiguration.getConfigurationProperties();
        zimbraConfigurationProperties.setPropertyValue(ADMIN_SERVICE_LOCATION_KEY, ADMIN_SERVICE_LOCATION);
        zimbraConfigurationProperties.setPropertyValue(ADMIN_USERNAME_KEY, ADMIN_USERNAME);
        zimbraConfigurationProperties.setPropertyValue(ADMIN_PASSWORD_KEY, ADMIN_PASSWORD);
        zimbraConfigurationProperties.setPropertyValue(EMAIL_DOMAIN_NAME_KEY, EMAIL_DOMAIN_NAME);
        zimbraConnectorFacade = ConnectorFacadeFactory.getInstance().newInstance(zimbraAPIConfiguration);
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        zimbraConnectorFacade = null;
        zimbraConfigurationProperties = null;
    }

    /**
     * Tests setting and validating the parameters provided.
     */
    @Test
    public void test01() throws Exception {
        zimbraConnectorFacade.validate();
    }

    @Test
    public void test02() throws Exception {
        zimbraConnectorFacade.test();
    }

    @Test
    public void test03() throws Exception {
        Schema schema = zimbraConnectorFacade.schema();
    }

    @Test
    public void test04() throws Exception {
        Set<Attribute> accountAttrs = new HashSet<Attribute>();
        accountAttrs.add(new Name(ACCOUNT_NAME));
        accountAttrs.add(AttributeBuilder.build(OperationalAttributes.PASSWORD_NAME, ACCOUNT_PASSWORD));
        accountAttrs.add(AttributeBuilder.build(ZimbraSpecialAttributes.ALIASES_NAME, ACCOUNT_ALIAS));
        accountAttrs.add(AttributeBuilder.build(Provisioning.A_description, ACCOUNT_DESCRIPTION));
        accountAttrs.add(AttributeBuilder.build(Provisioning.A_givenName, GIVEN_NAME));
        accountAttrs.add(AttributeBuilder.build(Provisioning.A_sn, SN));
        Uid accountId = zimbraConnectorFacade.create(ObjectClass.ACCOUNT, accountAttrs, null);
        assertNotNull(accountId);
    }

    @Test
    public void test05() throws Exception {
        Set<Attribute> groupAttrs = new HashSet<Attribute>();
        groupAttrs.add(new Name(GROUP_NAME));
        groupAttrs.add(AttributeBuilder.build(ZimbraSpecialAttributes.ALIASES_NAME, GROUP_ALIAS));
        groupAttrs.add(AttributeBuilder.build(Provisioning.A_description, GROUP_DESCRIPTION));
        groupAttrs.add(AttributeBuilder.build(ZimbraSpecialAttributes.MEMBERS_NAME, ACCOUNT_NAME + '@'
                + EMAIL_DOMAIN_NAME));
        Uid groupId = zimbraConnectorFacade.create(ObjectClass.GROUP, groupAttrs, null);
        assertNotNull(groupId);
    }

    @Test
    public void test06() throws Exception {
        Uid accountId = zimbraConnectorFacade.
                authenticate(ObjectClass.ACCOUNT, ACCOUNT_NAME, ACCOUNT_PASSWORD, null);
        assertNotNull(accountId);
    }

    @Test
    public void test07() throws Exception {
        Uid accountId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.ACCOUNT, ACCOUNT_NAME, null);
        assertNotNull(accountId);
    }

    @Test
    public void test08() throws Exception {
        Uid groupId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.GROUP, GROUP_NAME, null);
        assertNotNull(groupId);
    }

    @Test
    public void test09() throws Exception {
        Set<Attribute> accountAttrs = new HashSet<Attribute>();
        accountAttrs.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.FALSE));
        Uid accountId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.ACCOUNT, ACCOUNT_NAME, null);
        accountId = zimbraConnectorFacade.
                update(ObjectClass.ACCOUNT, accountId, accountAttrs, null);
        assertNotNull(accountId);
    }

    @Test
    public void test10() throws Exception {
        Set<Attribute> groupAttrs = new HashSet<Attribute>();
        groupAttrs.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.FALSE));
        Uid groupId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.GROUP, GROUP_NAME, null);
        groupId = zimbraConnectorFacade.update(ObjectClass.GROUP, groupId, groupAttrs, null);
        assertNotNull(groupId);
    }

    @Test
    public void test11() throws Exception {
        Set<Attribute> accountAttrs = new HashSet<Attribute>();
        accountAttrs.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.TRUE));
        Uid accountId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.ACCOUNT, ACCOUNT_NAME, null);
        accountId = zimbraConnectorFacade.
                update(ObjectClass.ACCOUNT, accountId, accountAttrs, null);
        assertNotNull(accountId);
    }

    @Test
    public void test12() throws Exception {
        Set<Attribute> groupAttrs = new HashSet<Attribute>();
        groupAttrs.add(AttributeBuilder.build(OperationalAttributes.ENABLE_NAME, Boolean.TRUE));
        Uid groupId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.GROUP, GROUP_NAME, null);
        groupId = zimbraConnectorFacade.update(ObjectClass.GROUP, groupId, groupAttrs, null);
        assertNotNull(groupId);
    }

    @Test
    public void test13() throws Exception {
        Map<String, Object> operationOptions = new HashMap<String, Object>();
//		operationOptions.put(OperationOptions.OP_ATTRIBUTES_TO_GET, new String[] { Provisioning.A_zimbraId, Provisioning.A_zimbraMailAlias, Provisioning.A_zimbraMemberOf, Provisioning.A_zimbraAccountStatus, Provisioning.A_zimbraMailStatus, Provisioning.A_zimbraDomainStatus } );
        OperationOptions options = new OperationOptions(operationOptions);

        ResultsHandler resultsHandler = new ResultsHandler() {

            @Override
            public boolean handle(ConnectorObject cobject) {
                for (Attribute attribute : cobject.getAttributes()) {
                    System.out.println(attribute.toString());
                }
                return true;
            }
        };

        zimbraConnectorFacade.search(ObjectClass.ACCOUNT, null, resultsHandler, options);
        zimbraConnectorFacade.search(ObjectClass.GROUP, null, resultsHandler, options);
    }

    @Test
    public void test14() throws Exception {
        Uid accountId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.ACCOUNT, ACCOUNT_NAME, null);
        zimbraConnectorFacade.delete(ObjectClass.ACCOUNT, accountId, null);
    }

    @Test
    public void test15() throws Exception {
        Uid groupId = zimbraConnectorFacade.
                resolveUsername(ObjectClass.GROUP, GROUP_NAME, null);
        zimbraConnectorFacade.delete(ObjectClass.GROUP, groupId, null);
    }
}
