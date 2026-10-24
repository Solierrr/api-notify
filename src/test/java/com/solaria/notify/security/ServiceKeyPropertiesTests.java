package com.solaria.notify.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class ServiceKeyPropertiesTests {

    private static ServiceKeyProperties props(String service, String key) {
        ServiceKeyProperties props = new ServiceKeyProperties();
        props.put(service, key);
        return props;
    }

    @Test
    void shortKeyFailsNamingServiceButNotKey() {
        assertThatThrownBy(() -> props("api-core", "curta-demais").validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("api-core")
                .hasMessageNotContaining("curta-demais");
    }

    @Test
    void keyWith32CharsPasses() {
        assertThatCode(() -> props("api-core", "a".repeat(32)).validate()).doesNotThrowAnyException();
    }

    @Test
    void blankOrMissingKeyIsAllowed() {
        assertThatCode(() -> props("api-core", "").validate()).doesNotThrowAnyException();
        assertThatCode(() -> props("api-core", null).validate()).doesNotThrowAnyException();
    }
}
