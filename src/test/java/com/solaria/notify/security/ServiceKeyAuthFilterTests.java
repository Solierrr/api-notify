package com.solaria.notify.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class ServiceKeyAuthFilterTests {

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private Authentication run(String configuredKey, String header) throws Exception {
        ServiceKeyProperties props = new ServiceKeyProperties();
        props.put("api-core", configuredKey);
        ServiceKeyAuthFilter filter = new ServiceKeyAuthFilter(props);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/internal/notifications");
        if (header != null) {
            request.addHeader("X-Service-Key", header);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void authenticatesKnownKeyAsServiceName() throws Exception {
        Authentication auth = run("k1", "k1");
        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo("api-core");
        assertThat(auth.getAuthorities()).extracting(Object::toString).containsExactly("ROLE_SERVICE");
    }

    @Test
    void ignoresUnknownKey() throws Exception {
        assertThat(run("k1", "zz")).isNull();
    }

    @Test
    void blankConfiguredKeyNeverMatches() throws Exception {
        assertThat(run("", "")).isNull();
    }

    @Test
    void missingHeaderLeavesContextEmpty() throws Exception {
        assertThat(run("k1", null)).isNull();
        assertThat(run("", null)).isNull();
    }
}
