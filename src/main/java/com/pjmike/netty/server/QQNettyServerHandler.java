package com.pjmike.netty.server;

import com.google.protobuf.TextFormat;
import com.pjmike.netty.protocol.message.HeartbeatResponsePacket;
import com.pjmike.netty.protocol.protobuf.MessageBase;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.extern.slf4j.Slf4j;

import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @desc 模拟qq广播消息
 * @author jackcooper
 * @create 2020-03-08
 */
@Deprecated
@Slf4j
@ChannelHandler.Sharable
public class QQNettyServerHandler extends SimpleChannelInboundHandler<MessageBase.Message> {

    /**
     * 在Netty中提供了ChannelGroup接口，该接口继承Set接口，因此可以通过ChannelGroup可管理服务器端所有的连接的Channel，然后对所有的连接Channel广播消息。
     */
    private static ChannelGroup channelGroup = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final String DATE_PARTTEN = "yyyy-MM-dd HH:mm:ss:SSS";

    /**
     * 这个必须用啊，当收到对方发来的数据后，就会触发，参数msg就是发来的信息，可以是基础类型，也可以是序列化的复杂对象。
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, MessageBase.Message msg) throws Exception {
        if (msg.getCmd().equals(MessageBase.Message.CommandType.HEARTBEAT_REQUEST)) {
            //TextFormat.printToUnicodeString(msg) 注意:解决中文乱码问题
            log.info("收到客户端发来的心跳消息：{}", TextFormat.printToUnicodeString(msg));
            //回应pong
            ctx.writeAndFlush(new HeartbeatResponsePacket());
        } else if (msg.getCmd().equals(MessageBase.Message.CommandType.NORMAL)) {
            //TextFormat.printToUnicodeString(msg) 注意:解决中文乱码问题
            log.info("收到客户端的业务消息：{}",TextFormat.printToUnicodeString(msg));
            //todo 在此做业务
            MessageBase.Message message = MessageBase.Message.newBuilder().setCmd(MessageBase.Message.CommandType.NORMAL).setRequestId(msg.getRequestId()).setContent("服务器已经收到："+msg.getContent()).build();
            ctx.writeAndFlush(message);
        }
    }


    /**
     * Channel注册到EventLoop，并且可以处理IO请求
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        super.channelRegistered(ctx);
    }

    /**
     * Channel从EventLoop中被取消注册，并且不能处理任何IO请求
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    /**
     * Channel已经连接到远程服务器，并准备好了接收数据
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        System.out.println(channel.remoteAddress() + " 已连接！");
    }

    /**
     * channel未连接到远程服务器
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        System.out.println(channel.remoteAddress() + " 已下线！");
    }

    /**
     * channelRead执行后触发
     * @param ctx
     * @throws Exception
     */
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

    /**
     * 当用户调用Channel.fireUserEventTriggered方法的时候触发，用户可以传递一个自定义的对象当这个方法里
     * @param ctx
     * @param evt
     * @throws Exception
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {

    }

    /**
     * 客户端链接建立的时候调用
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        // 服务端与客户端建立
        Channel channel = ctx.channel();
        // 向其他链接的客户端发送广播信息
        SocketAddress socketAddress = channel.remoteAddress();
        String date = DateTimeFormatter.ofPattern(DATE_PARTTEN).format(LocalDateTime.now());
        // 向channelGroup中的每一个channel对象发送一个消息
        channelGroup.writeAndFlush(date + " [服务器] - " + socketAddress + " 加入 \n");
        // 保存该客户端链接
        channelGroup.add(channel);
    }

    /**
     * 链接断开
     * @param ctx
     * @throws Exception
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        Channel channel = ctx.channel();
        String date = DateTimeFormatter.ofPattern(DATE_PARTTEN).format(LocalDateTime.now());
        channelGroup.writeAndFlush(date + " [服务器] - " + channel.remoteAddress() + " 离开 \n");
    }

    /**
     * ❤❤❤❤❤❤❤注意：处理异常, 一般将实现异常处理逻辑的Handler放在ChannelPipeline的最后
     * 这样确保所有入站消息都总是被处理，无论它们发生在什么位置，下面只是简单的关闭Channel并打印异常信息
     *
     * @param ctx
     * @param cause
     * @throws Exception
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        log.error("异常：",cause);
        ctx.close();
    }





}
