package net.nexylan.graylog.rabbitmq.senders;

import org.graylog2.plugin.Message;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RabbitMQSender implements Sender {

    private String host;
    private int port;
    private String queue;

    //RabbitMQ objects
    private Connection connection;
    private Channel channel;

    private static final Logger LOG = LoggerFactory.getLogger(RabbitMQSender.class);

    private boolean is_initialized = false;

    public RabbitMQSender(String host, int port, String queue)
    {
        this.host = host;
        this.port = port;
        this.queue = queue;

        this.initialize();
    }

    @Override
    public void initialize()
    {
        ConnectionFactory factory;
        factory = new ConnectionFactory();
        factory.setHost(this.host);
        factory.setPort(this.port);

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
            this.channel.queueDeclare(this.queue, false, false, false, null);
            LOG.info("[RabbitMQ] The queue have been successfully created.");
        } catch (IOException e) {
            LOG.error("[RabbitMQ] Impossible to declare the queue.");
            e.printStackTrace();
        }
        this.is_initialized = true;
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
    }

    @Override
    public void send(Message message)
    {
        try {
            this.channel.basicPublish("", this.queue, null, message.getField(Message.FIELD_FULL_MESSAGE).toString().getBytes());
        } catch (IOException e) {
            LOG.error("[RabbitMQ] An error occurred while publishing message.");
            e.printStackTrace();
        }

    }

    @Override
    public boolean isInitialized()
    {
        return is_initialized;
    }

}