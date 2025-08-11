package mx.nolab;

import hudson.Launcher;
import hudson.Extension;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.tasks.Builder;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Publisher;
import hudson.util.FormValidation;
import org.kohsuke.stapler.DataBoundConstructor;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ZohoCliqNotifierBuilder extends Builder {

    private final String message;

    @DataBoundConstructor
    public ZohoCliqNotifierBuilder(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) {
        listener.getLogger().println("Enviando mensaje a Zoho Cliq...");

        try {
            String webhookUrl = ZohoCliqNotifierConfig.get().getWebhookUrl();

            if (webhookUrl == null || webhookUrl.isEmpty()) {
                listener.getLogger().println("Webhook no configurado. Ve a 'Configure System'.");
                return false;
            }

            String payload = "{\"text\": \"" + message + "\"}";

            URL url = new URL(webhookUrl);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setRequestProperty("Content-Type", "application/json");

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = payload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int code = connection.getResponseCode();
            listener.getLogger().println("Respuesta de Zoho Cliq: " + code);

        } catch (Exception e) {
            listener.getLogger().println("Error al enviar mensaje: " + e.getMessage());
            return false;
        }

        return true;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Override
        public boolean isApplicable(Class type) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Enviar notificación a Zoho Cliq";
        }
    }
}
