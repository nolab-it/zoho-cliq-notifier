package mx.nolab;

import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.BuildStepMonitor;
import hudson.tasks.Notifier;
import hudson.tasks.Publisher;
import jenkins.tasks.SimpleBuildStep;
import edu.umd.cs.findbugs.annotations.NonNull;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import hudson.model.Run;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ZohoCliqNotifier extends Notifier implements SimpleBuildStep {

    private String installationName;

    @DataBoundConstructor
    public ZohoCliqNotifier(String installationName) {
        this.installationName = installationName;
    }

    public String getInstallationName() {
        return installationName;
    }

    @DataBoundSetter
    public void setInstallationName(String installationName) {
        this.installationName = installationName;
    }

    @Override
    public void perform(@NonNull Run<?, ?> run,
                        @NonNull FilePath workspace,
                        @NonNull Launcher launcher,
                        @NonNull TaskListener listener) {

        ZohoCliqInstallation installation = null;
        for (ZohoCliqInstallation inst : Jenkins.get()
                .getDescriptorByType(ZohoCliqInstallation.DescriptorImpl.class)
                .getInstallations()) {
            if (inst.getName().equals(installationName)) {
                installation = inst;
                break;
            }
        }

        if (installation == null) {
            listener.getLogger().println("[ZohoCliqNotifier] ERROR: No se encontró la instalación seleccionada.");
            return;
        }

        String webhookUrl = installation.getWebhookUrl();
        if (webhookUrl == null || webhookUrl.trim().isEmpty()) {
            listener.getLogger().println("[ZohoCliqNotifier] ERROR: La URL del webhook está vacía.");
            return;
        }

        listener.getLogger().println("[ZohoCliqNotifier] Preparando notificación...");

        // Estado y emoji
        String status = run.getResult() != null ? run.getResult().toString() : "UNKNOWN";
        String emoji = "SUCCESS".equals(status) ? "✅" : "❌";

        // Variables de entorno
        String buildUrl = "N/A";
        try {
            buildUrl = run.getEnvironment(listener).get("BUILD_URL");
        } catch (Exception ignored) {}

        String jobName = run.getParent().getName();
        String buildNumber = String.valueOf(run.getNumber());
        String buildDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Obtener commit info directamente desde Git
        String commitHash = "N/A";
        String commitAuthor = "N/A";
        String commitMessage = "N/A";

        try {
            ProcessBuilder pb = new ProcessBuilder("git", "log", "-1", "--pretty=format:%H;%an;%s");
            pb.directory(new File(workspace.getRemote()));
            Process process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null && !line.isEmpty()) {
                    String[] parts = line.split(";", 3);
                    commitHash = parts[0];
                    commitAuthor = parts[1];
                    commitMessage = parts[2];
                }
            }
            process.waitFor();
        } catch (Exception e) {
            listener.getLogger().println("[ZohoCliqNotifier] Error leyendo Git: " + e.getMessage());
        }

        // Texto descriptivo
        String descriptionText = String.format(
                "**Job:** %s\n**Build #:** %s\n**Commit:** %s\n**Autor:** %s\n**Mensaje:** %s\n**Fecha:** %s",
                jobName,
                buildNumber,
                commitHash,
                commitAuthor,
                commitMessage,
                buildDate
        );

        // Payload JSON
        JSONObject payload = new JSONObject();
        payload.put("botname", "Jenkins Bot");
        payload.put("text", "🔔 Jenkins Build Notification");

        JSONObject attachmentContent = new JSONObject();
        attachmentContent.put("theme", "modern-inline");
        attachmentContent.put("title", new JSONObject().put("text", emoji + " Jenkins Build: " + status));
        attachmentContent.put("description", new JSONObject().put("text", descriptionText));

        JSONArray buttons = new JSONArray();
        buttons.put(new JSONObject()
                .put("label", "Ver en Jenkins")
                .put("type", "link")
                .put("url", buildUrl != null ? buildUrl : "#")
        );
        attachmentContent.put("buttons", buttons);

        JSONObject attachment = new JSONObject();
        attachment.put("contentType", "application/vnd.zoho.card");
        attachment.put("content", attachmentContent);

        JSONArray attachments = new JSONArray();
        attachments.put(attachment);
        payload.put("attachments", attachments);

        // Enviar POST
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(webhookUrl);
            post.setEntity(new StringEntity(payload.toString(), StandardCharsets.UTF_8));
            post.setHeader("Content-Type", "application/json");
            client.execute(post).close();
            listener.getLogger().println("[ZohoCliqNotifier] Notificación enviada correctamente.");
        } catch (Exception e) {
            listener.getLogger().println("[ZohoCliqNotifier] Error enviando notificación: " + e.getMessage());
        }
    }

    @Override
    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Extension
    public static final class DescriptorImpl extends BuildStepDescriptor<Publisher> {

        @Override
        public boolean isApplicable(Class jobType) {
            return true;
        }

        @Override
        public String getDisplayName() {
            return "Notificar a Zoho Cliq";
        }

        public ListBoxModel doFillInstallationNameItems() {
            return Jenkins.get()
                    .getDescriptorByType(ZohoCliqInstallation.DescriptorImpl.class)
                    .doFillInstallationNameItems();
        }
    }
}
