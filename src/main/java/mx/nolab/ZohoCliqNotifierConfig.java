package mx.nolab;


import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.StaplerRequest;

@Extension
public class ZohoCliqNotifierConfig extends GlobalConfiguration {

    private String webhookUrl;

    public ZohoCliqNotifierConfig() {
        load(); // Carga configuración persistente si existe
    }

    public static ZohoCliqNotifierConfig get() {
        return GlobalConfiguration.all().get(ZohoCliqNotifierConfig.class);
    }

    public String getWebhookUrl() {
        return webhookUrl;
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
        save(); // Guarda automáticamente la configuración
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) throws FormException {
        req.bindJSON(this, json);
        save();
        return true;
    }
}
