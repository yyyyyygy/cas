package org.apereo.cas.config;

import org.apache.commons.lang3.StringUtils;
import org.apereo.cas.CentralAuthenticationService;
import org.apereo.cas.authentication.AuthenticationSystemSupport;
import org.apereo.cas.authentication.DefaultAuthenticationSystemSupport;
import org.apereo.cas.authentication.principal.ServiceFactory;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.support.rest.CredentialFactory;
import org.apereo.cas.support.rest.DefaultCredentialFactory;
import org.apereo.cas.support.rest.TicketsResource;
import org.apereo.cas.ticket.registry.TicketRegistrySupport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This is {@link RestConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Configuration("casRestConfiguration")
@EnableConfigurationProperties(CasCoreAuthenticationConfiguration.class)
public class RestConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    @Qualifier("centralAuthenticationService")
    private CentralAuthenticationService centralAuthenticationService;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired(required = false)
    @Qualifier("defaultAuthenticationSystemSupport")
    private AuthenticationSystemSupport authenticationSystemSupport =
            new DefaultAuthenticationSystemSupport();

    @Autowired(required = false)
    private CredentialFactory credentialFactory = new DefaultCredentialFactory();

    @Autowired
    @Qualifier("webApplicationServiceFactory")
    private ServiceFactory webApplicationServiceFactory;

    @Autowired
    @Qualifier("defaultTicketRegistrySupport")
    private TicketRegistrySupport ticketRegistrySupport;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(defaultRestAuthenticationThrottle()).addPathPatterns("/v1/**");
    }

    @Bean
    public TicketsResource ticketResourceRestController() {
        final TicketsResource r = new TicketsResource();
        r.setAuthenticationSystemSupport(authenticationSystemSupport);
        r.setCredentialFactory(credentialFactory);
        r.setTicketRegistrySupport(ticketRegistrySupport);
        r.setWebApplicationServiceFactory(webApplicationServiceFactory);
        r.setCentralAuthenticationService(centralAuthenticationService);
        return r;
    }

    @Bean(name = {"defaultRestAuthenticationThrottle", "restAuthenticationThrottle"})
    public HandlerInterceptorAdapter defaultRestAuthenticationThrottle() {
        if (StringUtils.isNotBlank(casProperties.getRest().getThrottler())
                && this.applicationContext.containsBean(casProperties.getRest().getThrottler())) {
            return this.applicationContext.getBean(casProperties.getRest().getThrottler(),
                    HandlerInterceptorAdapter.class);
        }
        return new HandlerInterceptorAdapter() {
            @Override
            public boolean preHandle(final HttpServletRequest request,
                                     final HttpServletResponse response,
                                     final Object handler) throws Exception {
                return true;
            }
        };
    }

}
