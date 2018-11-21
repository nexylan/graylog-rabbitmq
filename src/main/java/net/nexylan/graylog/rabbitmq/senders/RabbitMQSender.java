package net.nexylan.graylog.rabbitmq.senders;

import com.google.gson.Gson;
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
    private String queue;
    private String user;
    private String password;
    private int ttl;
    private boolean durable;

    //Message properties
    private int message_format;

    //RabbitMQ objects
    private Connection connection;
    private Channel channel;
    private boolean lock;
    private AMQP.BasicProperties sendProperties;


    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQSender.class);

    private boolean is_initialized = false;

    public RabbitMQSender(String host, int port, String queue, String user, String password, int ttl, boolean durable, int message_format)
    {
        this.host = host;
        this.port = port;
        this.queue = queue;
        this.user = user;
        this.password = password;
        this.ttl = ttl;
        this.durable = durable;
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
        factory.setUsername(this.user);
        factory.setPassword(this.password);

        try {
            this.connection = factory.newConnection();
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

        try {
            this.channel.queueDeclare(this.queue, false, this.durable, false, null);
            LOG.info("[RabbitMQ] The queue have been successfully created.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] Impossible to declare the queue.");
            e.printStackTrace();
        }

        AMQP.BasicProperties.Builder builder = new AMQP.BasicProperties.Builder();

        if(this.ttl != -1)
            builder.expiration(Integer.toString(this.ttl));

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
    public void send(Message message)
    {
        try {
            switch(this.message_format){
                case 0:
                    this.channel.basicPublish("", this.queue, this.sendProperties, message.getMessage().getBytes());
                    break;
                case 1:
                    this.channel.basicPublish("", this.queue, this.sendProperties, this.formatToJson(message.getFields()).getBytes());
                    break;
            }
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred while publishing message.");
            e.printStackTrace();
        }
    }

    private String formatToJson(Map<String, Object> data)
    {
        Gson gson = new Gson();
        return gson.toJson(data);
    }

    @Override
    public boolean isInitialized()
    {
        return this.is_initialized;
    }

}