package com.solaria.notify.push.fcm;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do Firebase Cloud Messaging.
 * credentialsBase64 é o JSON da conta de serviço em base64 (nunca logar o valor).
 */
@ConfigurationProperties(prefix = "app.fcm")
public class FcmProperties {

    private boolean enabled;
    private String credentialsBase64 = "";
    private String projectId = "";
    private boolean dryRun;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getCredentialsBase64() {
        return credentialsBase64;
    }

    public void setCredentialsBase64(String credentialsBase64) {
        this.credentialsBase64 = credentialsBase64;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }
}
