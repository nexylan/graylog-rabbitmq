package net.nexylan.graylog.rabbitmq.senders;

import org.graylog2.plugin.Message;

import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.concurrent.TimeoutException;


public class RabbitMQSender implements Sender {

    private String host;
    private int port;
    private String queue;

    //RabbitMQ objects
    private ConnectionFactory factory;
    private Connection connection;
    private Channel channel;

    private boolean is_initialized;

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
        this.factory = new ConnectionFactory();
        this.factory.setHost(this.host);
        this.factory.setPort(this.port);

        try {
            this.connection = this.factory.newConnection();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
        try {
            this.channel = this.connection.createChannel();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            this.channel.queueDeclare(this.queue, false, false, false, null);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void stop()
    {
        try {
            this.channel.close();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.connection.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void send(Message message)
    {
        try {
            this.channel.basicPublish("", this.queue, null, message.getField(Message.FIELD_FULL_MESSAGE).toString().getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Override
    public boolean isInitialized()
    {
        return is_initialized;
    }

}