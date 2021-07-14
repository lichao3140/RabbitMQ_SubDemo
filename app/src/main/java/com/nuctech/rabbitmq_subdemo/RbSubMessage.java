package com.nuctech.rabbitmq_subdemo;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

import java.io.IOException;

/**
 * 创建消费者线程
 * @param //handler
 */
public class RbSubMessage implements Runnable {

    private ConnectionFactory factory;
    private Handler handler;

    public RbSubMessage(ConnectionFactory factory, Handler handler) {
        this.factory = factory;
        this.handler = handler;
    }

    @Override
    public void run() {
        try {
            // 创建新的连接
            Connection connectionC = factory.newConnection();
            // 创建通道
            Channel channel = connectionC.createChannel();
            // 处理完一个消息，再接收下一个消息
            channel.basicQos(1);

            // 命名队列名称
            String queueName = Constants.MQ_SEND;
            // 声明交换机类型
            channel.exchangeDeclare(Constants.MQ_SEND_EXCHANGE, "topic", true);
            // 声明队列（持久的、非独占的、连接断开后队列会自动删除）
            AMQP.Queue.DeclareOk q = channel.queueDeclare(queueName, true, false, false, null);// 声明共享队列
            // 根据路由键将队列绑定到交换机上（需要知道交换机名称和路由键名称）
            channel.queueBind(q.getQueue(), Constants.MQ_SEND_EXCHANGE, Constants.MQ_ROUTINGKEY_SEND);
            // 创建消费者获取rabbitMQ上的消息。每当获取到一条消息后，就会回调handleDelivery（）方法，该方法可以获取到消息数据并进行相应处理
            Consumer consumer = new DefaultConsumer(channel) {
                @Override
                public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                    super.handleDelivery(consumerTag, envelope, properties, body);
                    // 通过geiBody方法获取消息中的数据
                    String message = new String(body);
                    Log.e("TAG", "异步订阅到消息get message  --- " + message);
                    // 发消息通知UI更新
                    Message msg = handler.obtainMessage(0, message);
                    handler.sendMessage(msg);
                }
            };
            channel.basicConsume(q.getQueue(), true, consumer);

        } catch (Exception e) {
            e.printStackTrace();
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        }
    }
}
