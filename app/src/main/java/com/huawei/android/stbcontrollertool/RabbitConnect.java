package com.huawei.android.stbcontrollertool;

import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Created by 47895 on 2017/1/9.
 */

public class RabbitConnect {
    public RabbitConnect(String rabbitHost,String userName,String password) {
        factory.setHost(rabbitHost);
        factory.setUsername(userName);
        factory.setPassword(password);
        factory.setPort(5672);
    }
    ConnectionFactory factory = new ConnectionFactory();
    Connection connection = null;
    //初始连接
    private void setupConnection() {
        if(connection==null){
            try {
                connection=factory.newConnection();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

    }
    //断开连接
    public void disConnectRabbit() throws IOException {
        connection.close();
    }
    //获取接收器，用于用while接受消息
    public QueueingConsumer getConsumer(String queueName) throws IOException {
        setupConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);
        QueueingConsumer consumer = new QueueingConsumer(channel);
        channel.basicConsume(queueName, true, consumer);
        return  consumer;
    }
    //发送消息
    public void publishMessage(String queueName,String message) throws IOException, TimeoutException {
        setupConnection();
        Channel channelResult = connection.createChannel();
        channelResult.queueDeclare(queueName, false, false, false, null);
        channelResult.basicPublish("", queueName, null, message.getBytes("UTF-8"));
        channelResult.close();
    }
    //获取一个新通道
    public Channel getNewChannel(String queueName) throws IOException {
        setupConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(queueName, false, false, false, null);
        return channel;
    }
}
