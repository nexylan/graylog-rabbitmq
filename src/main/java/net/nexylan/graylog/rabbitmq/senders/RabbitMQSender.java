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
            LOG.info("[RabbitMQ] The channel has been successfully created.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred during the channel creation.");
            e.printStackTrace();
        }

        // Check to see if the exchange exists.  If it doesn't then try to declare it.
        // If it does exist then just move along.
        Channel dummy_channel = null;
        try {
            // Create a dummy channel.
            dummy_channel = this.connection.createChannel();
            LOG.info("[RabbitMQ] " + this.exchange + " exists. We'll use it.");

            // exchangeDeclarePassive throws IOException -
            // the server will raise a 404 channel exception if the named exchange does not exist.
            dummy_channel.exchangeDeclarePassive(this.exchange);

            // Close the channel
            try {
                dummy_channel.close();
            } catch (Exception e) {
                LOG.error("[RabbitMQ] Exception occurred closing the dummy channel.", e);
            }
        } catch (IOException oe) {
            // The exchange doesn't exist.  Try to declare it.
            try {
                LOG.info("[RabbitMQ] " + this.exchange + " does not exist. Will attempt to declare as a direct, durable exchange");
                this.channel.exchangeDeclare(this.exchange, "direct", true);
            } catch (IOException e) {
                LOG.error("[RabbitMQ] An error occurred declaring the exchange." + this.exchange, e);
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
