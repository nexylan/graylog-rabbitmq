package net.nexylan.graylog.rabbitmq.senders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.*;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;


public class RabbitMQSender implements Sender {

    //Queue properties
    private String host;
    private int port;
    private String queue;
    private String user;
    private String password;
    private int ttl;
    private boolean durable;
    private boolean queueCluster;

    //Message properties
    private int message_format;

    //RabbitMQ objects
    private Connection connection;
    private Channel channel;
    private boolean lock;
    private AMQP.BasicProperties sendProperties;


    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQSender.class);

    private boolean is_initialized = false;

    public RabbitMQSender(String host, int port, String queue, String user, String password, int ttl, boolean durable, boolean queueCluster, int message_format) {
        this.host = host;
        this.port = port;
        this.queue = queue;
        this.user = user;
        this.password = password;
        this.ttl = ttl;
        this.durable = durable;
        this.message_format = message_format;
        this.queueCluster = queueCluster;
        initialize();
    }

    @Override
    public void initialize() {
        if (lock) {
            return;
        }
        lock = true;

        ConnectionFactory factory;
        factory = new ConnectionFactory();

        AddressResolver addressResolver;
        if (this.host.contains(",")) {
            List<Address> addressList = Arrays.stream(this.host.split(",")).map(a -> new Address(a.trim(), this.port)).collect(Collectors.toList());
            addressResolver = new ListAddressResolver(addressList);
        } else {
            addressResolver = new DnsRecordIpAddressResolver(this.host, this.port);
        }
        factory.setUsername(this.user);
        factory.setPassword(this.password);

        try {
            this.connection = factory.newConnection(addressResolver);
            LOG.info("[RabbitMQ] Successfully connected to the server.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] Failed to connect to RabbitMQ Server.");
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            LOG.error("[RabbitMQ] The RabbitMQ Server timed out.");
        }
        try {
            this.channel = this.connection.createChannel();
            LOG.info("[RabbitMQ] The channel have been successfully created.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred during the channel creation.");
            e.printStackTrace();
        }

        Map<String, Object> declareArguments = new HashMap<>();
        if (this.queueCluster) {
            declareArguments.put("x-queue-type", "quorum");
        } else {
            declareArguments.put("x-queue-type", "classic");
        }
        try {
            this.channel.queueDeclare(this.queue, this.durable || this.queueCluster, false, false, declareArguments);
            LOG.info("[RabbitMQ] The queue have been successfully created.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] Impossible to declare the queue.");
            e.printStackTrace();
        }

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();

        if (this.ttl != -1)
            builder.expiration(Integer.toString(this.ttl));

        this.sendProperties = builder.build();
        this.is_initialized = true;
        lock = false;
    }

    @Override
    public void stop() {
        try {
            this.channel.close();
        } catch (TimeoutException e) {
            LOG.error("[RabbitMQ] An error occurred while closing the channel.");
            e.printStackTrace();
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred.");
            e.printStackTrace();
        }
        try {
            this.connection.close();
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred while closing the connection.");
            e.printStackTrace();
        }
        LOG.info("Stopping the output");
    }

    @Override
    public void send(Message message) {
        try {
            switch (this.message_format) {
                case 0:
                    this.channel.basicPublish("", this.queue, this.sendProperties, message.getMessage().getBytes());
                    break;
                case 1:
                    this.channel.basicPublish("", this.queue, this.sendProperties, this.formatToJson(message.getFields()).getBytes());
                    break;
            }
        } catch (JsonProcessingException exception) {
            LOG.error("[RabbitMQ] Impossible to format message to json.");
            exception.printStackTrace();
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred while publishing message.");
            e.printStackTrace();
        }
    }

    private String formatToJson(Map<String, Object> data) throws JsonProcessingException {
        Map<String, Object> updatedData = new HashMap<>();
        data.forEach((s, o) -> {
            if (o instanceof DateTime) {
                o = ((DateTime) o).toString(ISODateTimeFormat.dateTime());
            }
            updatedData.put(s, o);
        });
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(updatedData);
    }

    @Override
    public boolean isInitialized() {
        return this.is_initialized;
    }

}
