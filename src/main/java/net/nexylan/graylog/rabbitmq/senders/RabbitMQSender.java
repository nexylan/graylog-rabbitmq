package net.nexylan.graylog.rabbitmq.senders;

import org.graylog2.plugin.Message;

public class RabbitMQSender implements Sender {

    private String host;
    private int port;
    private String queue;

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

    }

    @Override
    public void stop()
    {

    }

    @Override
    public void send(Message message)
    {

    }

    @Override
    public boolean isInitialized()
    {
        return is_initialized;
    }

}