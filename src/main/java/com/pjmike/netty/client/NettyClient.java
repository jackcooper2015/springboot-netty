package com.pjmike.netty.client;

import com.pjmike.netty.protocol.protobuf.MessageBase;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

/**
 * @author pjmike
 * @create 2018-10-24 16:31
 */
@Component
@Slf4j
public class NettyClient  {
    private EventLoopGroup group = new NioEventLoopGroup();
    @Value("${netty.port}")
    private int port;
    @Value("${netty.host}")
    private String host;
    private SocketChannel socketChannel;

    //写出失败自动重试5次
    public void send(MessageBase.Message message, int counts) {
        final int after = ++counts;
        socketChannel.writeAndFlush(message).addListener(future -> {
            if (future.isSuccess()) {
                log.info("{}写出完成!", message);
            } else {
                log.error("{}写出失败,重新send-{}!", message, after);
                if (after < 5) {
                    Thread.sleep(50);
                    send(message, after);
                }
            }
        });
    }

    public void sendMsg(MessageBase.Message message) {
        //发送消息，处理异常
        socketChannel.writeAndFlush(message).addListener(future -> {
            if(future.isSuccess()){
                log.info("{}发送成功",message.getRequestId());
            }else{
                log.error("{}发送异常：",message.getRequestId(),future.cause());
            }
        });
        //失败重试方案(弱网环境下可用)
//        this.send(message,1);
    }

    @PostConstruct
    public void start()  {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .remoteAddress(host, port)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ClientHandlerInitilizer());
        ChannelFuture future = bootstrap.connect();
        //客户端断线重连逻辑
        future.addListener((ChannelFutureListener) future1 -> {
            if (future1.isSuccess()) {
                log.info("连接Netty服务端成功");
            } else {
                log.info("连接失败，进行断线重连");
                future1.channel().eventLoop().schedule(() -> start(), 20, TimeUnit.SECONDS);
            }
        });
        socketChannel = (SocketChannel) future.channel();
    }
}
