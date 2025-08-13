package mx.nolab;

import hudson.Extension;
import hudson.tools.ToolInstallation;
import hudson.tools.ToolDescriptor;
import hudson.tools.ToolProperty;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.List;

public class ZohoCliqInstallation extends ToolInstallation {

    private final String webhookUrl;

    @DataBoundConstructor
    public ZohoCliqInstallation(String name, String webhookUrl, List<? extends ToolProperty<?>> properties) {
        super(name, "", properties);
        this.webhookUrl = webhookUrl;
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    @Extension
    public static class DescriptorImpl extends ToolDescriptor<ZohoCliqInstallation> {

        @Override
        public String getDisplayName() {
            return "Zoho Cliq Webhook";
        }

        @Override
        public ZohoCliqInstallation[] getInstallations() {
            return super.getInstallations(); // Devuelve ZohoCliqInstallation[]
        }

        public ListBoxModel doFillInstallationNameItems() {
            ListBoxModel m = new ListBoxModel();
            for (ZohoCliqInstallation inst : getInstallations()) {
                m.add(inst.getName());
            }
            return m;
        }
    }
}
