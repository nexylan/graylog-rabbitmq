package net.nexylan.graylog.rabbitmq;

import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.plugin.Version;

import java.net.URI;
import java.util.Collections;
import java.util.Set;

/**
 * Implement the PluginMetaData interface here.
 */
public class RabbitMqMetaData implements PluginMetaData {
    private static final String PLUGIN_PROPERTIES = "net.nexylan.graylog.plugins.graylog-rabbitmq/graylog-plugin.properties";

    @Override
    public String getUniqueId() {
        return "net.nexylan.graylog.rabbitmq.RabbitMqPlugin";
    }

    @Override
    public String getName() {
        return "RabbitMq";
    }

    @Override
    public String getAuthor() {
        return "Henri Devigne <henri.devigne@nexylan.com>";
    }

    @Override
    public URI getURL() {
        return URI.create("https://github.com/https://git.nexylan.net/hdevigne/graylog-rabbitmq");
    }

    @Override
    public Version getVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public String getDescription() {
        // TODO Insert correct plugin description
        return "Description of RabbitMq plugin";
    }

    @Override
    public Version getRequiredVersion() {
        return Version.fromPluginProperties(getClass(), PLUGIN_PROPERTIES, "graylog.version", Version.from(0, 0, 0, "unknown"));
    }

    @Override
    public Set<ServerStatus.Capability> getRequiredCapabilities() {
        return Collections.emptySet();
    }
}
