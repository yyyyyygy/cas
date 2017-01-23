package org.apereo.cas.authentication.principal;

import org.apereo.cas.adaptors.ldap.AbstractLdapTests;
import org.apereo.cas.authentication.CoreAuthenticationTestUtils;
import org.apereo.cas.authentication.EchoingPrincipalResolver;
import org.apereo.cas.authentication.UsernamePasswordCredential;
import org.apereo.cas.config.CasPersonDirectoryConfiguration;
import org.apereo.services.persondir.IPersonAttributeDao;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ldaptive.LdapEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * This is {@link PersonDirectoryPrincipalResolverLdaptiveTests}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {CasPersonDirectoryConfiguration.class})
@TestPropertySource(locations={"classpath:/ldap.properties"})
public class PersonDirectoryPrincipalResolverLdaptiveTests extends AbstractLdapTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(PersonDirectoryPrincipalResolverLdaptiveTests.class);
    
    private static final String ATTR_NAME_PASSWORD = "userPassword";
    
    @Autowired
    @Qualifier("attributeRepository")
    private IPersonAttributeDao attributeRepository;

    @BeforeClass
    public static void bootstrap() throws Exception {
        LOGGER.debug("Running [{}]", PersonDirectoryPrincipalResolverLdaptiveTests.class.getSimpleName());
        initDirectoryServer();
    }

    @Test
    public void verifyResolver() {
        for (final LdapEntry entry : this.getEntries()) {
            final String username = entry.getAttribute("sAMAccountName").getStringValue();
            final String psw = entry.getAttribute(ATTR_NAME_PASSWORD).getStringValue();
            final PersonDirectoryPrincipalResolver resolver = new PersonDirectoryPrincipalResolver();
            resolver.setAttributeRepository(this.attributeRepository);
            final Principal p = resolver.resolve(new UsernamePasswordCredential(username, psw), CoreAuthenticationTestUtils.getPrincipal());
            assertNotNull(p);
            assertTrue(p.getAttributes().containsKey("displayName"));
        }
    }

    @Test
    public void verifyChainedResolver() {
        for (final LdapEntry entry : this.getEntries()) {
            final String username = entry.getAttribute("sAMAccountName").getStringValue();
            final String psw = entry.getAttribute(ATTR_NAME_PASSWORD).getStringValue();
            final PersonDirectoryPrincipalResolver resolver = new PersonDirectoryPrincipalResolver();
            resolver.setAttributeRepository(this.attributeRepository);

            final ChainingPrincipalResolver chain = new ChainingPrincipalResolver();
            chain.setChain(Arrays.asList(resolver, new EchoingPrincipalResolver()));
            final Map<String, Object> attributes = new HashMap<>(2);
            attributes.put("a1", "v1");
            attributes.put("a2", "v2");
            final Principal p = chain.resolve(new UsernamePasswordCredential(username, psw), CoreAuthenticationTestUtils.getPrincipal(username, attributes));
            assertNotNull(p);
            assertTrue(p.getAttributes().containsKey("displayName"));
            assertTrue(p.getAttributes().containsKey("a1"));
            assertTrue(p.getAttributes().containsKey("a2"));
        }
    }
}
