package com.solaria.notify.push.fcm;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.solaria.notify.push.LoggingPushSender;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FcmConfigTests {

    private static final String MESSAGE = "FCM habilitado, mas FCM_CREDENTIALS_BASE64 está vazio ou inválido";

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(FcmConfig.class, LoggingPushSender.class);

    @Test
    void disabledByDefaultUsesLoggingSender() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LoggingPushSender.class);
            assertThat(context).doesNotHaveBean(FirebaseMessaging.class);
        });
    }

    @Test
    void enabledWithBlankCredentialsFailsWithoutLeaking() {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=").run(context -> {
            assertThat(context).hasFailed();
            assertThat(rootChain(context.getStartupFailure())).contains(MESSAGE);
        });
    }

    @Test
    void enabledWithGarbageCredentialsFailsWithoutLeaking() {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=bm90LWpzb24=")
                .run(context -> {
                    assertThat(context).hasFailed();
                    String chain = rootChain(context.getStartupFailure());
                    assertThat(chain).contains(MESSAGE).doesNotContain("not-json");
                    assertThat(context.getStartupFailure().getMessage()).doesNotContain("not-json");
                });
    }

    @Test
    void enabledWithNonBase64CredentialsFailsWithoutLeaking() {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=%%%segredo%%%")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootChain(context.getStartupFailure())).contains(MESSAGE).doesNotContain("segredo");
                });
    }

    @Test
    void enabledWithValidCredentialsCreatesMessaging() throws Exception {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=" + fakeCredentials())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(FirebaseMessaging.class);
                    assertThat(context.getBean(FirebaseApp.class).getName()).isEqualTo("api-notify");
                    assertThat(context).doesNotHaveBean(LoggingPushSender.class);
                });
    }

    @Test
    void authorizedUserCredentialsAreRejected() {
        String json = "{\"type\":\"authorized_user\",\"client_id\":\"x\",\"client_secret\":\"segredo\","
                + "\"refresh_token\":\"token-secreto\"}";
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=" + encode(json))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootChain(context.getStartupFailure())).contains(MESSAGE)
                            .doesNotContain("token-secreto").doesNotContain("segredo");
                });
    }

    @Test
    void wrongTypedFieldIsRejectedWithStandardMessage() {
        runner.withPropertyValues("app.fcm.enabled=true",
                "app.fcm.credentials-base64=" + encode(serviceAccountJson("123"))).run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(rootChain(context.getStartupFailure())).contains(MESSAGE);
                });
    }

    @Test
    void httpTimeoutsHaveDefaults() throws Exception {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=" + fakeCredentials())
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    FirebaseApp app = context.getBean(FirebaseApp.class);
                    assertThat(app.getOptions().getConnectTimeout()).isEqualTo(5000);
                    assertThat(app.getOptions().getReadTimeout()).isEqualTo(10000);
                });
    }

    @Test
    void httpTimeoutsAreOverridable() throws Exception {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=" + fakeCredentials(),
                "app.fcm.connect-timeout=1234", "app.fcm.read-timeout=4321").run(context -> {
                    assertThat(context).hasNotFailed();
                    FirebaseApp app = context.getBean(FirebaseApp.class);
                    assertThat(app.getOptions().getConnectTimeout()).isEqualTo(1234);
                    assertThat(app.getOptions().getReadTimeout()).isEqualTo(4321);
                });
    }

    @Test
    void appIsDeletedOnContextClose() throws Exception {
        runner.withPropertyValues("app.fcm.enabled=true", "app.fcm.credentials-base64=" + fakeCredentials())
                .run(context -> assertThat(context).hasNotFailed());
        assertThat(FirebaseApp.getApps()).noneMatch(app -> "api-notify".equals(app.getName()));
    }

    private static String rootChain(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            sb.append(c.getClass().getName()).append(": ").append(c.getMessage()).append('\n');
        }
        return sb.toString();
    }

    // Conta de serviço FALSA: chave RSA gerada na hora, nunca uma credencial real
    private static String fakeCredentials() throws Exception {
        return encode(serviceAccountJson("\"" + escapedPem() + "\""));
    }

    private static String encode(String json) {
        return Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static String serviceAccountJson(String privateKeyJsonValue) {
        return """
                {"type":"service_account","project_id":"test-project","private_key_id":"fake",
                "private_key":%s,"client_email":"fake@test-project.iam.gserviceaccount.com",
                "client_id":"123","token_uri":"https://oauth2.googleapis.com/token"}
                """.formatted(privateKeyJsonValue);
    }

    private static String escapedPem() throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();
        String pem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.UTF_8))
                        .encodeToString(pair.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        return pem.replace("\n", "\\n");
    }
}
