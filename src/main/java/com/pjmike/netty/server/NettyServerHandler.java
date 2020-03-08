package com.pjmike.netty.server;

import com.google.protobuf.TextFormat;
import com.pjmike.netty.protocol.message.HeartbeatResponsePacket;
import com.pjmike.netty.protocol.protobuf.MessageBase;
import com.pjmike.netty.utils.SpringContextUtils;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author pjmike
 * @create 2018-10-24 15:43
 */
@Slf4j
@ChannelHandler.Sharable
public class NettyServerHandler extends SimpleChannelInboundHandler<MessageBase.Message> {


    //❤❤❤❤❤❤❤注意：这里不能使用@Autowired注入bean,可以使用 SpringContextUtils获取

    /**
     * 注意：<pre>
     *     如果server通过IP来给客户端发消息则key为ip:port
     * </pre>
     */
    private static Map<String, ChannelHandlerContext> map = new ConcurrentHashMap<>();

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBase.Message msg) throws Exception {
        if (msg.getCmd().equals(MessageBase.Message.CommandType.HEARTBEAT_REQUEST)) {
            //TextFormat.printToUnicodeString(msg) 注意:解决中文乱码问题
            log.info("收到客户端发来的心跳消息：{}", TextFormat.printToUnicodeString(msg));
            //回应pong
            ctx.writeAndFlush(new HeartbeatResponsePacket());
        } else if (msg.getCmd().equals(MessageBase.Message.CommandType.NORMAL)) {
            log.info("收到客户端的业务消息：{}",msg.toString());
            String responseMsg = SpringContextUtils.getBean(TestService.class).doSomthing(msg.getContent());
            MessageBase.Message message = MessageBase.Message.newBuilder().setCmd(MessageBase.Message.CommandType.NORMAL).setRequestId(msg.getRequestId()).setContent(responseMsg).build();
            ctx.writeAndFlush(message);
        }
    }
}
