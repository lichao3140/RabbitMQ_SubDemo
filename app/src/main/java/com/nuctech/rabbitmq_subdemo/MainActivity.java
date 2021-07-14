package com.nuctech.rabbitmq_subdemo;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 功能：RabbitMQ消息发送和接收
 * 说明：1.项目gradle内依赖rabbitmq；implementation 'com.rabbitmq:amqp-client:5.12.0'
 *      2.manifest添加联网权限：<uses-permission android:name="android.permission.INTERNET"/>
 *      3.已搭建好消息发送和接受的RabbitMQ服务器，管理网页地址：http://8.140.39.115:15672,用户名admin,密码admin
 */
public class MainActivity extends AppCompatActivity {

    private Button tv_sub;
    private ConnectionFactory factory;
    private Thread publishThread;
    private final BlockingDeque<String> queue = new LinkedBlockingDeque<>();  // 创建内部消息队列，供消费者发布消息用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_sub = findViewById(R.id.mTV_sub);

        factory = new ConnectionFactory();// 声明ConnectionFactory对象
        setUpConnectionFactory();// 连接设置

        /**
         * 发消息
         */
        tv_sub.setOnClickListener(new View.OnClickListener() { //按钮监听，创建连接发布消息
            @Override
            public void onClick(View v) {
                basicPublish();
            }
        });

        /**
         * 收消息
         */
        basicConsume();
    }

    /**
     * 连接设置（rabbit配置）
     */
    private void setUpConnectionFactory() {
        factory.setHost(Constants.MQ_HOST);//主机地址：8.140.39.115
        factory.setPort(Constants.MQ_PORT);// 端口号:5672
        factory.setUsername(Constants.MQ_USERNAME);// 用户名
        factory.setPassword(Constants.MQ_PASSWORD);// 密码
        factory.setAutomaticRecoveryEnabled(true);// 设置连接恢复
        factory.setNetworkRecoveryInterval(10000);// 设置每个10s ，重试一次
        factory.setTopologyRecoveryEnabled(true);// 设置重新声明交换器，队列等信息
        factory.setRequestedHeartbeat(20); // 心跳包设置
        factory.setVirtualHost("/door");
    }

    /**
     * 发消息
     */
    private void basicPublish() {
        //消息发送的配置
        publishToAMPQ(Constants.MQ_ROUTINGKEY);
        //队列填充消息内容
        publishMessage("hello world");
    }

    /**
     * 创建连接通道，发布消息
     * @param routingKey
     */
    private void publishToAMPQ(final String routingKey) {
        publishThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        // 创建连接
                        Connection connection = factory.newConnection();
                        // 创建通道
                        Channel ch = connection.createChannel();
                        //声明了一个交换和一个服务器命名的队列，然后将它们绑定在一起。
                        ch.exchangeDeclare(Constants.MQ_EXCHANGE, "topic" , true);//声明exchange durable声明持久交换
                        //创建队列
                        String queueName = Constants.MQ_QUEUE;

                        // 声明队列（持久的、非独占的、连接断开后队列会自动删除）
                        ch.queueDeclare(queueName, true, false, false, null);// 声明共享队列
                        ch.queueBind(queueName, Constants.MQ_EXCHANGE, routingKey);//将exchange和queue绑定
                        ch.confirmSelect();
                        while (true) {
                            String message = queue.takeFirst(); //从队列取消息传输
                            Log.e("TAG", "即将发送消息  --   " + message);
                            try {
                                // 发布消息，配置交换机、路由键、路由标头等、消息体byte[]
                                ch.basicPublish(Constants.MQ_EXCHANGE, routingKey, null, message.getBytes());
                                ch.waitForConfirmsOrDie();
                                Thread.sleep(5000); //sleep and then try again

                            } catch (Exception e) {
                                queue.putFirst(message);
                                throw e;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    } catch (Exception e) {
                        Log.e("TAG", "异常Connection broken: " + e.getClass().getName());
                        try {
                            Thread.sleep(3000); //sleep and then try again
                        } catch (InterruptedException e1) {
                            break;
                        }
                    }
                }
            }
        });
        publishThread.start();
    }

    /**
     * 消息处理
     * @param messageData
     */
    private void publishMessage(String messageData) {
        //向内部阻塞队列添加一条消息
        try {
            queue.putLast(messageData);

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * 消息接收
     */
    private void basicConsume() {
        // 连接设置
        setUpConnectionFactory();
        //开启消费者线程
        new Thread(new RbSubMessage(factory, incomingMessageHandler)).start();
    }

    // 处理handler发送的消息，然后进行操作（在主线程）
    private final Handler incomingMessageHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // 获取RabbitMQ的消息数据
            switch (msg.what){
                case 0:
                    String messageData = (String) msg.obj;
                    Log.e("TAG", "主线程处理已接收的消息   ---    " + messageData);
                    break;
            }
        }
    };
}