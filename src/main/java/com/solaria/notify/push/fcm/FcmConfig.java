package com.solaria.notify.push.fcm;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "app.fcm.enabled", havingValue = "true")
@EnableConfigurationProperties(FcmProperties.class)
public class FcmConfig {

    static final String APP_NAME = "api-notify";
    static final String INVALID_CREDENTIALS = "FCM habilitado, mas FCM_CREDENTIALS_BASE64 está vazio ou inválido";

    @Bean(destroyMethod = "delete")
    FirebaseApp fcmFirebaseApp(FcmProperties properties) {
        String encoded = properties.getCredentialsBase64();
        if (encoded == null || encoded.isBlank()) {
            throw new IllegalStateException(INVALID_CREDENTIALS);
        }
        try {
            byte[] json = Base64.getMimeDecoder().decode(encoded.trim());
            GoogleCredentials credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(json));
            FirebaseOptions.Builder options = FirebaseOptions.builder().setCredentials(credentials);
            if (properties.getProjectId() != null && !properties.getProjectId().isBlank()) {
                options.setProjectId(properties.getProjectId().trim());
            }
            return FirebaseApp.initializeApp(options.build(), APP_NAME);
        } catch (IllegalArgumentException | IOException e) {
            // Sem causa: a mensagem do parser pode citar trechos do conteúdo da credencial
            throw new IllegalStateException(INVALID_CREDENTIALS);
        }
    }

    @Bean
    FirebaseMessaging firebaseMessaging(FirebaseApp fcmFirebaseApp) {
        return FirebaseMessaging.getInstance(fcmFirebaseApp);
    }
}
