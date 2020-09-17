package net.nexylan.graylog.rabbitmq.senders;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.rabbitmq.client.AMQP;
import org.graylog2.plugin.Message;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RabbitMQSender implements Sender {

    //Queue properties
    private String host;
    private int port;
    private String vhost;
    private String exchange;
    private String user;
    private String password;
    private int ttl;
    private String routing_key;

    //Message properties
    private int message_format;

    //RabbitMQ objects
    private Connection connection;
    private Channel channel;
    private Channel dummy_channel;
    private boolean lock;
    private AMQP.BasicProperties sendProperties;


    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQSender.class);

    private boolean is_initialized = false;

    public RabbitMQSender(String host, int port, String vhost, String exchange, String user, String password, int ttl, String routing_key, int message_format)
    {
        this.host = host;
        this.port = port;
        this.vhost = vhost;
        this.exchange = exchange;
        this.user = user;
        this.password = password;
        this.ttl = ttl;
        this.routing_key = routing_key;
        this.message_format = message_format;

        initialize();
    }

    @Override
    public void initialize()
    {
        if(lock){
           return;
        }
        lock = true;

        ConnectionFactory factory;
        factory = new ConnectionFactory();
        factory.setHost(this.host);
        factory.setPort(this.port);
        factory.setVirtualHost(this.vhost);
        factory.setUsername(this.user);
        factory.setPassword(this.password);

        try {
            this.connection = factory.newConnection();
            LOG.info("[RabbitMQ] Successfully connected to vhost " + this.vhost + " on server " + this.host + ":" + this.port);
        } catch (IOException e) {
            LOG.error("[RabbitMQ] Failed to connect to vhost " + this.vhost + " on server " + this.host + ":" + this.port);
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
            LOG.error("[RabbitMQ] Time out connecting to vhost " + this.vhost + " on server " + this.host + ":" + this.port);
        }

        try {
            this.channel = this.connection.createChannel();
            LOG.info("[RabbitMQ] The channel have been successfully created.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred during the channel creation.");
            e.printStackTrace();
        }

        // Attempt to declare the exchange.  If it already exists then capture the exception
        try {
            // Create a dummy channel.
            this.dummy_channel = this.connection.createChannel();
            this.dummy_channel.exchangeDeclarePassive(this.exchange);
        } catch (IOException oe) {
            try {
                LOG.info("[RabbitMQ] Attempting to declare " + this.exchange + " as a direct, durable exchange (if it doesn't already exist)");
                this.channel.exchangeDeclare(this.exchange, "direct", true);
                try {
                    this.dummy_channel.close();
                } catch (TimeoutException ie) {
                    LOG.error("[RabbitMQ] Timeout occurred while closing the channel.");
                    ie.printStackTrace();
                }
            } catch (IOException e) {
                LOG.error("[RabbitMQ] An error occurred declaring the exchange.");
                e.printStackTrace();
            }
        }

        if (this.routing_key == "") {
            LOG.info("[RabbitMQ] Routing key was not specified.  Defaulting to an empty value.");
            this.routing_key = "";
        } else {
            LOG.info("[RabbitMQ] Using routing key " + this.routing_key);
        }

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();

        if (this.ttl >= 0) {
            LOG.info("[RabbitMQ] Setting TTL to " + this.ttl);
            builder.expiration(Integer.toString(this.ttl));
        } else {
            LOG.info("[RabbitMQ] Disabling TTL");
        }

        this.sendProperties = builder.build();
        this.is_initialized = true;
        lock = false;
    }

    @Override
    public void stop()
    {
        try {
            this.channel.close();
        } catch (TimeoutException e) {
            LOG.error("[RabbitMQ] Timeout while closing the channel.");
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
    public void send(Message message)
    {
        try {
            switch(this.message_format){
                case 0:
                    this.channel.basicPublish(this.exchange, this.routing_key, this.sendProperties, message.getMessage().getBytes());
                    break;
                case 1:
                    this.channel.basicPublish(this.exchange, this.routing_key, this.sendProperties, this.formatToJson(message.getFields()).getBytes());
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

    private String formatToJson(Map<String, Object> data) throws JsonProcessingException
    {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(data);
    }

    @Override
    public boolean isInitialized()
    {
        return this.is_initialized;
    }

}
