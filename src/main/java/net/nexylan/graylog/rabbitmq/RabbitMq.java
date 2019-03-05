package net.nexylan.graylog.rabbitmq;

import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.*;
import org.graylog2.plugin.inputs.annotations.ConfigClass;
import org.graylog2.plugin.inputs.annotations.FactoryClass;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.outputs.MessageOutputConfigurationException;
import org.graylog2.plugin.streams.Stream;

import com.google.inject.assistedinject.Assisted;
import javax.inject.Inject;

import net.nexylan.graylog.rabbitmq.senders.*;

import java.util.List;
import java.util.Map;

/**
 * This is the plugin. Your class should implement one of the existing plugin
 * interfaces. (i.e. AlarmCallback, MessageInput, MessageOutput)
 */
public class RabbitMq implements MessageOutput{
    private static final String RABBIT_HOST = "rabbit_host";
    private static final String RABBIT_PORT = "rabbit_port";
    private static final String RABBIT_QUEUE = "rabbit_queue";
    private static final String RABBIT_USER = "rabbit_user";
    private static final String RABBIT_PASSWORD = "rabbit_password";
    private static final String RABBIT_TTL = "rabbit_ttl";
    private static final String RABBIT_DURABLE = "rabbit_durable";
    private static final String RABBIT_MESSAGE_FORMAT = "rabbit_message_format";

    private boolean running;

    private final Sender sender;

    @Inject
    public RabbitMq(@Assisted Configuration configuration) throws MessageOutputConfigurationException {
        // Check configuration.
        if (!checkConfiguration(configuration)) {
            throw new MessageOutputConfigurationException("Missing configuration.");
        }

        //Convertir format to integer
        final Map<String, Integer> message_formats = ImmutableMap.of("message", 0, "json", 1);

        // Set up sender
        sender = new RabbitMQSender(
                configuration.getString(RABBIT_HOST),
                configuration.getInt(RABBIT_PORT),
                configuration.getString(RABBIT_QUEUE),
                configuration.getString(RABBIT_USER),
                configuration.getString(RABBIT_PASSWORD),
                configuration.getInt(RABBIT_TTL),
                configuration.getBoolean(RABBIT_DURABLE),
                message_formats.get(configuration.getString(RABBIT_MESSAGE_FORMAT))
        );

        running = true;
    }

    @Override
    public void stop() {
        sender.stop();
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public void write(Message message) {
        if (message == null || message.getFields() == null || message.getFields().isEmpty()) {
            return;
        }

        if(!sender.isInitialized()) {
            sender.initialize();
        }

        sender.send(message);
    }

    @Override
    public void write(List<Message> list) {
        if (list == null) {
            return;
        }

        for(Message m : list) {
            write(m);
        }
    }

    private boolean checkConfiguration(Configuration c) {
        return c.stringIsSet(RABBIT_HOST) && c.intIsSet(RABBIT_PORT) && c.stringIsSet(RABBIT_QUEUE) && c.stringIsSet(RABBIT_USER) && c.stringIsSet(RABBIT_PASSWORD) && c.intIsSet(RABBIT_TTL);
    }

    @FactoryClass
    public interface Factory extends MessageOutput.Factory<RabbitMq> {
        @Override
        RabbitMq create(Stream stream, Configuration configuration);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    @ConfigClass
    public static class Config extends MessageOutput.Config {
        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final ConfigurationRequest configurationRequest = new ConfigurationRequest();

            configurationRequest.addField(new TextField(
                    RABBIT_HOST, "RabbitMQ Host", "127.0.0.1",
                    "Hostname or IP address of a RabbitMQ amqp instance",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new NumberField(
                    RABBIT_PORT, "RabbitMQ Port", 5672,
                    "Port of a RabbitMQ instance",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                    RABBIT_QUEUE, "RabbitMQ Queue", "my_queue",
                    "Name of the RabbitMQ Queue to push messages",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                    RABBIT_USER, "RabbitMQ User", "guest",
                    "Name of the RabbitMQ User ( guest is restricted to local )",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new TextField(
                    RABBIT_PASSWORD, "RabbitMQ Password", "guest",
                    "Password of the rabbitMQ user",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new NumberField(
                    RABBIT_TTL, "RabbitMQ TTL", -1,
                    "The TTL of a message set -1 to disable",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            configurationRequest.addField(new BooleanField(RABBIT_DURABLE,
                    "RabbitMQ Durable",
                    true,
                    "May this queue must be durable ?"
            ));

            final Map<String, String> formats = ImmutableMap.of("message", "Message", "json", "JSON ( all fields )");
            configurationRequest.addField(new DropdownField(
                    RABBIT_MESSAGE_FORMAT,
                    "Message format",
                    "message",
                    formats,
                    "Message format: Message only, or JSON which contains all fields including extracted ones",
                    ConfigurationField.Optional.NOT_OPTIONAL)
            );

            return configurationRequest;
        }
    }

    public static class Descriptor extends MessageOutput.Descriptor {
        public Descriptor() {
            super("RabbitMQ Output", false, "", "Writes messages to your RabbitMQ installation.");
        }
    }

}
