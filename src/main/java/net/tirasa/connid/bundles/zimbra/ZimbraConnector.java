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

import com.zimbra.common.account.Key;
import com.zimbra.common.account.ProvisioningConstants;
import com.zimbra.common.service.ServiceException;
import com.zimbra.cs.account.Account;
import com.zimbra.cs.account.Config;
import com.zimbra.cs.account.DistributionList;
import com.zimbra.cs.account.Domain;
import com.zimbra.cs.account.NamedEntry;
import com.zimbra.cs.account.Provisioning;
import com.zimbra.cs.account.SearchDirectoryOptions;
import com.zimbra.cs.account.SearchDirectoryOptions.SortOpt;
import com.zimbra.cs.account.auth.AuthContext;
import com.zimbra.cs.account.soap.SoapProvisioning;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConnectionBrokenException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.InvalidCredentialException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;

@ConnectorClass(
        displayNameKey = ZimbraConstants.ZIMBRA_CONNECTOR_DISPLAY, configurationClass = ZimbraConfiguration.class)
public class ZimbraConnector implements PoolableConnector, AuthenticateOp, CreateOp, DeleteOp, ResolveUsernameOp,
        SchemaOp, SearchOp<String>, TestOp, UpdateOp {

    private static final Log LOG = Log.getLog(ZimbraConnector.class);

    private ZimbraConfiguration configuration;

    private SoapProvisioning sp;

    private String emailDomainName;

    private Domain domain;

    private Schema schema;

    private String cos;

    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    @Override
    public void init(Configuration configuration) {
        this.configuration = (ZimbraConfiguration) configuration;
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.Connector#getConfiguration()
     */
    @Override
    public Configuration getConfiguration() {
        return configuration;
    }


    /* (non-Javadoc)
     * @see org.identityconnectors.framework.spi.Connector#dispose()
     */
    @Override
    public void dispose() {
        schema = null;
        domain = null;
        emailDomainName = null;
        sp = null;
        configuration = null;
        cos = null;
    }

    private SoapProvisioning sp() {
        if ((sp == null || sp.isExpired()) && configuration != null) {
            configuration.validate();

            String adminServiceLocation = configuration.getAdminServiceLocation();
            String adminUsername = configuration.getAdminUsername();
            GuardedString adminPassword = configuration.getAdminPassword();
            try {
                sp = new SoapProvisioning();
                sp.soapSetURI(adminServiceLocation);
                sp.soapAdminAuthenticate(adminUsername, GuardedStringAccessor.toString(adminPassword));

            } catch (ServiceException ex) {
                throw new ConnectionFailedException(configuration.getConnectorMessages().format(
                        ZimbraConstants.ZIMBRA_CONNECT_FAILED_MSG, null, adminServiceLocation, adminUsername), ex);
            }

            emailDomainName = configuration.getEmailDomainName();
            if (StringUtil.isBlank(emailDomainName)) {
                Config spConfig;
                try {
                    spConfig = sp.getConfig();
                } catch (ServiceException ex) {
                    throw connectorException(ex, ZimbraConstants.ZIMBRA_GET_CONFIG_FAILED_MSG);
                }
                emailDomainName = spConfig.getAttr(Provisioning.A_zimbraDefaultDomainName, null);
            }

            try {
                domain = sp.get(Key.DomainBy.name, emailDomainName);
            } catch (ServiceException ex) {
                throw connectorException(ex, ZimbraConstants.ZIMBRA_GET_DOMAIN_FAILED_MSG);
            }

            try {
                if (StringUtil.isNotBlank(configuration.getCos())) {
                    cos = sp.getCosByName(configuration.getCos()).getId();
                }
            } catch (ServiceException ex) {
                throw connectorException(ex, ZimbraConstants.ZIMBRA_GET_DOMAIN_FAILED_MSG);
            }

            schema = schema();
        }
        return sp;
    }

    @Override
    public void checkAlive() {
        final String METHOD = "checkAlive";
        LOG.ok("enter {0}()", METHOD);
        try {
            sp().healthCheck();
        } catch (Throwable ex) {
            LOG.error(ex, "fail {0}()", METHOD);
            throw new ConnectionBrokenException(configuration.getConnectorMessages().format(
                    ZimbraConstants.ZIMBRA_CHECK_ALIVE_FAILED_MSG, null), ex);
        }
        LOG.ok("exit {0}()", METHOD);
    }

    @Override
    public void test() {
        final String METHOD = "test";
        LOG.ok("enter {0}()", METHOD);
        try {
            sp().healthCheck();
        } catch (ServiceException ex) {
            throw new ConnectionBrokenException(configuration.getConnectorMessages().
                    format(ZimbraConstants.ZIMBRA_CHECK_TEST_MSG, null), ex);
        }
        LOG.ok("exit {0}()", METHOD);
    }

    private static String getName(Set<Attribute> attrs) {
        if (attrs != null) {
            Name name = AttributeUtil.getNameFromAttributes(attrs);
            if (name != null) {
                return name.getNameValue();
            }
        }
        return null;
    }

    private static String toObjectUid(ObjectClass objClass, String name) {
        if (name != null) {
            int atIndex = name.indexOf('@');
            return atIndex != -1 ? name.substring(0, atIndex) : name;
        }
        return null;
    }

    private String toObjectName(ObjectClass objClass, String name) {
        if (name != null) {
            StringBuilder sb = new StringBuilder();
            if (objClass.is(ObjectClass.ACCOUNT_NAME)
                    || objClass.is(ObjectClass.GROUP_NAME)) {
                sb.append(name);
                if (name.indexOf('@') == -1) {
                    sb.append('@');
                    sb.append(emailDomainName);
                }
            } else {
                throw new UnsupportedOperationException();
            }
            return sb.toString();
        }
        return null;
    }

    private NamedEntry getZimbraObject(ObjectClass objClass, String zimbraName) throws ServiceException {
        NamedEntry zimbraEntry = null;
        if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
            zimbraEntry = sp().get(Key.AccountBy.name, zimbraName);
        } else if (objClass.is(ObjectClass.GROUP_NAME)) {
            zimbraEntry = sp().get(Key.DistributionListBy.name, zimbraName);
        }
        return zimbraEntry;
    }

    @Override
    public Uid authenticate(ObjectClass objClass, String username, GuardedString password, OperationOptions options) {
        final String METHOD = "authenticate";
        Uid uid = new Uid(toObjectUid(objClass, username));
        LOG.ok("enter {0}(\"{1}\", \"{2}\")", METHOD, objClass, uid);
        NamedEntry zimbraEntry = null;
        try {
            String zimbraName = toObjectName(objClass, username);
            zimbraEntry = getZimbraObject(objClass, zimbraName);
        } catch (ServiceException ex) {
            throw connectorException(ex, ZimbraConstants.ZIMBRA_AUTHENTICATE_FAILED_MSG, objClass, uid.getUidValue());
        }
        if (zimbraEntry == null) {
            throw new UnknownUidException(uid, objClass);
        }
        try {
            ((Account) zimbraEntry).authAccount(GuardedStringAccessor.toString(password),
                    AuthContext.Protocol.http_basic);
        } catch (ServiceException ex) {
            throw new InvalidCredentialException();
        }
        LOG.ok("exit {0}() == \"{1}\"", METHOD, uid);
        return uid;
    }

    @Override
    public Uid resolveUsername(ObjectClass objClass, String username, OperationOptions options) {
        final String METHOD = "resolveUsername";
        Uid uid = new Uid(toObjectUid(objClass, username));
        LOG.ok("enter {0}(\"{1}\", \"{2}\")", METHOD, objClass, uid);
        NamedEntry zimbraEntry = null;
        try {
            String zimbraName = toObjectName(objClass, username);
            zimbraEntry = getZimbraObject(objClass, zimbraName);
        } catch (ServiceException ex) {
            throw connectorException(
                    ex, ZimbraConstants.ZIMBRA_RESOLVE_USERNAME_FAILED_MSG, objClass, uid.getUidValue());
        }
        if (zimbraEntry == null) {
            throw new UnknownUidException(uid, objClass);
        }
        LOG.ok("exit {0}() == \"{1}\"", METHOD, uid);
        return uid;
    }

    @Override
    public Uid create(ObjectClass objClass, Set<Attribute> attrs, OperationOptions options) {
        final String METHOD = "create";
        String name = getName(attrs);
        Uid uid = new Uid(toObjectUid(objClass, name));
        LOG.ok("enter {0}(\"{1}\", \"{2}\")", METHOD, objClass, uid);
        try {
            String zimbraNewName = toObjectName(objClass, uid.getUidValue());
            NamedEntry zimbraEntry = getZimbraObject(objClass, zimbraNewName);
            if (zimbraEntry != null) {
                return update(objClass, uid, attrs, options);
            }

            Map<String, Object> zimbraAttrs = toZimbraAttributes(objClass, attrs);
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                if (cos != null) {
                    zimbraAttrs.put(Provisioning.A_zimbraCOSId, cos);
                }
                GuardedString password = AttributeUtil.getPasswordValue(attrs);
                Account account = sp().createAccount(zimbraNewName, GuardedStringAccessor.toString(password),
                        zimbraAttrs);
                zimbraEntry = account;
                String[] aliases = findAttribute(attrs, ZimbraSpecialAttributes.ALIASES_NAME);
                if (aliases != null && aliases.length > 0) {
                    for (String alias : aliases) {
                        sp().addAlias(account, toObjectName(objClass, alias));
                    }
                }
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                DistributionList distributionList = sp().createDistributionList(zimbraNewName, zimbraAttrs);
                zimbraEntry = distributionList;
                String[] aliases = findAttribute(attrs, ZimbraSpecialAttributes.ALIASES_NAME);
                if (aliases != null && aliases.length > 0) {
                    for (String alias : aliases) {
                        sp().addAlias(distributionList, toObjectName(objClass, alias));
                    }
                }
                String[] members = findAttribute(attrs, ZimbraSpecialAttributes.MEMBERS_NAME);
                if (members != null && members.length > 0) {
                    sp().addMembers(distributionList, members);
                }
            }
        } catch (ServiceException ex) {
            throw connectorException(ex, ZimbraConstants.ZIMBRA_CREATE_FAILED_MSG, objClass, uid.getUidValue());
        }
        LOG.ok("exit {0}() == \"{1}\"", METHOD, uid);
        return uid;
    }

    @Override
    public Uid update(ObjectClass objClass, Uid uid, Set<Attribute> attrs, OperationOptions options) {
        final String METHOD = "update";
        LOG.ok("enter {0}(\"{1}\", \"{2}\")", METHOD, objClass, uid);
        try {
            String zimbraName = toObjectName(objClass, uid.getUidValue());
            NamedEntry zimbraEntry = getZimbraObject(objClass, zimbraName);
            if (zimbraEntry == null) {
                return create(objClass, attrs, options);
            }

            Map<String, Object> zimbraAttrs = toZimbraAttributes(objClass, attrs);

            if (cos != null) {
                zimbraAttrs.put(Provisioning.A_zimbraCOSId, cos);
            }          
            sp().modifyAttrs(zimbraEntry, zimbraAttrs);
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                Account account = (Account) zimbraEntry;
                GuardedString password = AttributeUtil.getPasswordValue(attrs);
                if (password != null) {
                    sp().setPassword(account, GuardedStringAccessor.toString(password));
                }
                String[] aliases = findAttribute(attrs, ZimbraSpecialAttributes.ALIASES_NAME);
                if (aliases != null && aliases.length > 0) {
                    String[] oldAliases = account.getMultiAttr(Provisioning.A_zimbraMailAlias);
                    if (oldAliases != null && oldAliases.length > 0) {
                        for (String alias : oldAliases) {
                            sp().removeAlias(account, toObjectName(objClass, alias));
                        }
                    }
                    for (String alias : aliases) {
                        sp().addAlias(account, toObjectName(objClass, alias));
                    }
                }
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                DistributionList distributionList = (DistributionList) zimbraEntry;
                String[] aliases = findAttribute(attrs, ZimbraSpecialAttributes.ALIASES_NAME);
                if (aliases != null && aliases.length > 0) {
                    String[] oldAliases = distributionList.getAliases();
                    if (oldAliases != null && oldAliases.length > 0) {
                        for (String alias : oldAliases) {
                            sp().removeAlias(distributionList, toObjectName(objClass, alias));
                        }
                    }
                    for (String alias : aliases) {
                        sp().addAlias(distributionList, toObjectName(objClass, alias));
                    }
                }

                String[] oldMembers = distributionList.getAllMembers();
                if (oldMembers != null && oldMembers.length > 0) {
                    sp().removeMembers(distributionList, oldMembers);
                }
                String[] members = findAttribute(attrs, ZimbraSpecialAttributes.MEMBERS_NAME);
                if (members != null && members.length > 0) {
                    sp().addMembers(distributionList, members);
                }
            }

            // rename
            String newName = getName(attrs);
            if (newName != null && !newName.equalsIgnoreCase(uid.getUidValue())) {
                String zimbraNewName = toObjectName(objClass, newName);
                String zimbraId = zimbraEntry.getId();
                if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                    sp().renameAccount(zimbraId, zimbraNewName);
                } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                    sp().renameDistributionList(zimbraId, zimbraNewName);
                }
                uid = new Uid(toObjectUid(objClass, newName));
            }
        } catch (ServiceException ex) {
            throw connectorException(ex, ZimbraConstants.ZIMBRA_UPDATE_FAILED_MSG, objClass, uid.getUidValue());
        }
        LOG.ok("exit {0}() == \"{1}\"", METHOD, uid);
        return uid;
    }

    @Override
    public void delete(ObjectClass objClass, Uid uid, OperationOptions options) {
        final String METHOD = "delete";
        LOG.ok("enter {0}(\"{1}\", \"{2}\")", METHOD, objClass, uid);
        try {
            String zimbraName = toObjectName(objClass, uid.getUidValue());
            NamedEntry zimbraEntry = getZimbraObject(objClass, zimbraName);
            if (zimbraEntry == null) {
                throw new UnknownUidException(uid, objClass);
            }

            String zimbraId = zimbraEntry.getId();
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                sp().deleteAccount(zimbraId);
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                sp().deleteDistributionList(zimbraId);
            }
        } catch (ServiceException ex) {
            throw connectorException(ex, ZimbraConstants.ZIMBRA_DELETE_FAILED_MSG, objClass, uid.getUidValue());
        }
        LOG.ok("exit {0}()", METHOD);
    }

    @Override
    public FilterTranslator<String> createFilterTranslator(ObjectClass objClass, OperationOptions options) {
        return new ZimbraFilterTranslator(objClass);
    }

    @Override
    public void executeQuery(ObjectClass objClass, String query, ResultsHandler handler, OperationOptions options) {
        final String METHOD = "executeQuery";
        LOG.ok("enter {0}(\"{1}\", \"{2}\")", METHOD, objClass, query);
        String[] mappedAttributesToGet = toZimbraAttributesToGet(objClass, options);
        try {
            SearchDirectoryOptions searchOptions = new SearchDirectoryOptions();
            searchOptions.setDomain(domain);
            searchOptions.setFilterString(null, query);
            searchOptions.setReturnAttrs(mappedAttributesToGet);
            searchOptions.setSortOpt(SortOpt.SORT_ASCENDING);
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                searchOptions.addType(SearchDirectoryOptions.ObjectType.accounts);
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                searchOptions.addType(SearchDirectoryOptions.ObjectType.distributionlists);
            } else {
                throw new UnsupportedOperationException();
            }
            List<NamedEntry> allEntries = sp().searchDirectory(searchOptions);
            for (NamedEntry zimbraEntry : allEntries) {
                ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
                builder.setObjectClass(objClass);
                String zimbraName = zimbraEntry.getAttr(Provisioning.A_uid, true, true);
                Uid uid = new Uid(toObjectUid(objClass, zimbraName));
                builder.setUid(uid);
                builder.setName(toObjectName(objClass, zimbraName));
                Map<String, Object> zimbraAttrs = zimbraEntry.getAttrs(true, false);
                builder.addAttributes(getAttributes(objClass, zimbraAttrs));
                if (!handler.handle(builder.build())) {
                    break;
                }
            }
        } catch (ServiceException ex) {
            throw connectorException(ex, ZimbraConstants.ZIMBRA_EXECUTE_QUERY_FAILED_MSG, objClass, query);
        }
        LOG.ok("exit {0}()", METHOD);
    }

    @Override
    public Schema schema() {
        if (schema != null) {
            return schema;
        }

        SchemaBuilder schemaBld = new SchemaBuilder(ZimbraConnector.class);

        Set<AttributeInfo> accountAttrsInfo = new HashSet<AttributeInfo>();
        accountAttrsInfo.add(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
//		accountAttrsInfo.add(OperationalAttributeInfos.CURRENT_PASSWORD);
//		accountAttrsInfo.add(OperationalAttributeInfos.DISABLE_DATE);
        accountAttrsInfo.add(OperationalAttributeInfos.ENABLE);
//		accountAttrsInfo.add(OperationalAttributeInfos.ENABLE_DATE);
        accountAttrsInfo.add(OperationalAttributeInfos.LOCK_OUT);
        accountAttrsInfo.add(OperationalAttributeInfos.PASSWORD);
        accountAttrsInfo.add(OperationalAttributeInfos.PASSWORD_EXPIRED);
//		accountAttrsInfo.add(OperationalAttributeInfos.PASSWORD_EXPIRATION_DATE);
//		accountAttrsInfo.add(PredefinedAttributeInfos.SHORT_NAME);
//		accountAttrsInfo.add(PredefinedAttributeInfos.DESCRIPTION);
//		accountAttrsInfo.add(PredefinedAttributeInfos.LAST_PASSWORD_CHANGE_DATE);
//		accountAttrsInfo.add(PredefinedAttributeInfos.PASSWORD_CHANGE_INTERVAL);
//		accountAttrsInfo.add(PredefinedAttributeInfos.LAST_LOGIN_DATE);
//		accountAttrsInfo.add(PredefinedAttributeInfos.GROUPS);
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_uid, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_mail, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_givenName, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_sn, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_cn, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_displayName, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraCreateTimestamp, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraAccountStatus, String.class));
        accountAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraId, String.class));
        accountAttrsInfo.add(ZimbraSpecialAttributeInfos.ALIASES);
        ObjectClassInfoBuilder accountInfoBld = new ObjectClassInfoBuilder();
        accountInfoBld.setType(ObjectClass.ACCOUNT_NAME);
        accountInfoBld.addAllAttributeInfo(accountAttrsInfo);
        ObjectClassInfo accountInfo = accountInfoBld.build();
        schemaBld.defineObjectClass(accountInfo);

        ObjectClassInfoBuilder groupInfoBld = new ObjectClassInfoBuilder();
        groupInfoBld.setType(ObjectClass.GROUP_NAME);
        Set<AttributeInfo> groupAttrsInfo = new HashSet<AttributeInfo>();
        groupAttrsInfo.add(AttributeInfoBuilder.build(Name.NAME, String.class, EnumSet.of(Flags.REQUIRED)));
        groupAttrsInfo.add(OperationalAttributeInfos.ENABLE);
        groupAttrsInfo.add(ZimbraSpecialAttributeInfos.ALIASES);
        groupAttrsInfo.add(ZimbraSpecialAttributeInfos.MEMBERS);
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_cn, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_displayName, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_description, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraHideInGal, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraDistributionListSubscriptionPolicy,
                String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraDistributionListUnsubscriptionPolicy,
                String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraNotes, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraPrefReplyToEnabled, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraPrefReplyToDisplay, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraPrefReplyToAddress, String.class));
        groupAttrsInfo.add(AttributeInfoBuilder.build(Provisioning.A_zimbraMailForwardingAddress, String.class));
        groupInfoBld.addAllAttributeInfo(groupAttrsInfo);
        ObjectClassInfo groupInfo = groupInfoBld.build();
        schemaBld.defineObjectClass(groupInfo);
        schemaBld.removeSupportedObjectClass(AuthenticateOp.class, groupInfo);

        return schemaBld.build();
    }

    static String toZimbraAttributeName(ObjectClass objClass, String name) {
        if (AttributeUtil.namesEqual(Name.NAME, name)) {
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                return Provisioning.A_uid;
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                return Provisioning.A_uid;
            }
        } else if (AttributeUtil.namesEqual(Uid.NAME, name)) {
            return Provisioning.A_uid;
        } else if (AttributeUtil.namesEqual(OperationalAttributes.ENABLE_NAME, name)) {
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                return Provisioning.A_zimbraAccountStatus;
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                return Provisioning.A_zimbraMailStatus;
            }
        } else if (AttributeUtil.namesEqual(OperationalAttributes.LOCK_OUT_NAME, name)) {
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                return Provisioning.A_zimbraAccountStatus;
            }
        } else if (AttributeUtil.namesEqual(OperationalAttributes.PASSWORD_NAME, name)) {
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                return Provisioning.A_userPassword;
            }
        } else if (AttributeUtil.namesEqual(ZimbraSpecialAttributes.ALIASES_NAME, name)) {
            return Provisioning.A_zimbraMailAlias;
        } else if (AttributeUtil.namesEqual(ZimbraSpecialAttributes.MEMBERS_NAME, name)) {
            return Provisioning.A_zimbraMailForwardingAddress;
        } else if (AttributeUtil.namesEqual(OperationalAttributes.PASSWORD_EXPIRED_NAME, name)) {
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                return Provisioning.A_zimbraPasswordMustChange;
            }
        }
        return name;
    }

    static String[] toZimbraAttributesToGet(ObjectClass objClass, OperationOptions options) {
        if (options != null) {
            String[] attrsToGet = options.getAttributesToGet();
            if (attrsToGet != null) {
                Set<String> attrsToGetSet = new HashSet<String>();
                attrsToGetSet.add(toZimbraAttributeName(objClass, Uid.NAME));
                for (String attrName : attrsToGet) {
                    attrsToGetSet.add(toZimbraAttributeName(objClass, attrName));
                }
                return attrsToGetSet.toArray(new String[0]);
            }
        }
        return null;
    }

    static Set<Attribute> getAttributes(ObjectClass objClass, Map<String, Object> zimbraAttrs) {
        Set<Attribute> attrs = new HashSet<Attribute>();
        for (String zimbraName : zimbraAttrs.keySet()) {
            Object zimbraValue = zimbraAttrs.get(zimbraName);
            List<Object> value = new ArrayList<Object>();
            if (zimbraValue instanceof Object[]) {
                for (Object o : (Object[]) zimbraValue) {
                    value.add(o);
                }
            } else {
                value.add(zimbraValue);
            }
            if (Provisioning.A_zimbraAccountStatus.equalsIgnoreCase(zimbraName)) {
                if (Provisioning.ACCOUNT_STATUS_LOCKED.equalsIgnoreCase((String) zimbraValue)) {
                    attrs.add(AttributeBuilder.buildLockOut(true));
                } else {
                    attrs.add(AttributeBuilder.buildEnabled(Provisioning.ACCOUNT_STATUS_ACTIVE.equalsIgnoreCase(
                            (String) zimbraValue)));
                }
            } else if (Provisioning.A_zimbraMailStatus.equalsIgnoreCase(zimbraName)) {
                attrs.add(AttributeBuilder.buildEnabled(Provisioning.MAIL_STATUS_ENABLED.equalsIgnoreCase(
                        (String) zimbraValue)));
            } else if (Provisioning.A_userPassword.equalsIgnoreCase(zimbraName)) {
                attrs.add(AttributeBuilder.buildPassword(((String) zimbraValue).toCharArray()));
            } else if (Provisioning.A_zimbraPasswordMustChange.equalsIgnoreCase(zimbraName)) {
                attrs.add(AttributeBuilder.buildPasswordExpired(ProvisioningConstants.TRUE.equalsIgnoreCase(
                        (String) zimbraValue)));
            } else if (Provisioning.A_zimbraMailAlias.equalsIgnoreCase(zimbraName)) {
                attrs.add(AttributeBuilder.build(ZimbraSpecialAttributes.ALIASES_NAME, value));
            } else {
                attrs.add(AttributeBuilder.build(zimbraName, value));
            }
        }
        return attrs;
    }

    static String[] findAttribute(Set<Attribute> attrs, String attrName) {
        if (attrs != null) {
            Attribute attr = AttributeUtil.find(attrName, attrs);
            if (attr != null) {
                List<Object> attrValue = attr.getValue();
                if (attrValue != null) {
                    int attrSize = attrValue.size();
                    String[] result = new String[attrSize];
                    for (int i = 0; i < attrSize; i++) {
                        result[i] = attrValue.get(i).toString();
                    }
                    return result;
                }
            }
        }
        return null;
    }

    static Object toZimbraAttributeValue(ObjectClass objClass, Attribute attr) {
        if (attr.is(Name.NAME)) {
            return ((Name) attr).getNameValue();
        } else if (attr.is(Uid.NAME)) {
            return toObjectUid(objClass, ((Uid) attr).getUidValue());
        } else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
            Boolean isEnabled = AttributeUtil.getBooleanValue(attr);
            if (objClass.is(ObjectClass.ACCOUNT_NAME)) {
                return isEnabled ? Provisioning.ACCOUNT_STATUS_ACTIVE : Provisioning.ACCOUNT_STATUS_CLOSED;
            } else if (objClass.is(ObjectClass.GROUP_NAME)) {
                return isEnabled ? Provisioning.MAIL_STATUS_ENABLED : Provisioning.MAIL_STATUS_DISABLED;
            }
        } else if (attr.is(OperationalAttributes.LOCK_OUT_NAME)) {
            Boolean isLockedOut = AttributeUtil.getBooleanValue(attr);
            return isLockedOut ? Provisioning.ACCOUNT_STATUS_LOCKED : Provisioning.ACCOUNT_STATUS_ACTIVE;
        } else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
            return GuardedStringAccessor.toString(AttributeUtil.getGuardedStringValue(attr));
        } else if (attr.is(OperationalAttributes.PASSWORD_EXPIRED_NAME)) {
            Boolean isPasswordExpired = AttributeUtil.getBooleanValue(attr);
            return isPasswordExpired ? ProvisioningConstants.TRUE : ProvisioningConstants.FALSE;
        }
        List<Object> attrValue = attr.getValue();
        if (attrValue == null) {
            return null;
        }
        int attrSize = attrValue.size();
        if (attrSize == 1) {
            return attrValue.get(0);
        }
        Object[] result = new Object[attrSize];
        for (int i = 0; i < attrSize; i++) {
            result[i] = attrValue.get(i);
        }
        return result;
    }

    static Map<String, Object> toZimbraAttributes(ObjectClass objClass, Set<Attribute> attrs) {
        Map<String, Object> zimbraAttrs = new HashMap<String, Object>();
        for (Attribute attr : attrs) {
            if (!(attr.is(Name.NAME) || attr.is(Uid.NAME) || attr.is(OperationalAttributes.PASSWORD_NAME)
                    || ZimbraSpecialAttributes.isZimbraSpecialAttribute(attr))) {
                String zimbraName = toZimbraAttributeName(objClass, attr.getName());
                Object zimbraValue = toZimbraAttributeValue(objClass, attr);
                zimbraAttrs.put(zimbraName, zimbraValue);
            }
        }
        return zimbraAttrs;
    }

    private RuntimeException connectorException(Throwable ex, String key, Object... args) {
        if (args != null) {
            for (int i = 0; i < args.length; i++) {
                if (args[i] instanceof ObjectClass) {
                    args[i] = configuration.getConnectorMessages().format(((ObjectClass) args[i]).getDisplayNameKey(),
                            null);
                }
            }
        }
        return new ConnectorException(configuration.getConnectorMessages().format(key, null, args), ex);
    }
}
