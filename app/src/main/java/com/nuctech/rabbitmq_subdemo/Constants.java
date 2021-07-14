package com.nuctech.rabbitmq_subdemo;

class Constants {

    //服务端主机地址
    public static String MQ_HOST = "8.140.39.115";
    //服务端amqp协议端口
    public static int MQ_PORT = 5672;
    //用户名
    public static String MQ_USERNAME = "admin";
    //密码
    public static String MQ_PASSWORD = "admin";
    //交换机
    public static String MQ_EXCHANGE = "faceExchange";
    //路由键
    public static String MQ_ROUTINGKEY = "face.TextTipsKey";
    //队列名
    public static String MQ_QUEUE = "textTipsQueue";

    public static String MQ_SEND = "faceQueue";
    public static String MQ_SEND_EXCHANGE = "faceExchange";
    public static String MQ_ROUTINGKEY_SEND = "face.RecognitionKey";
}
