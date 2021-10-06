package com.mycompany.my.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class OutMessageHandler extends ChannelOutboundHandlerAdapter {

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("Сработал OutMessageHandler");
        if (msg instanceof ByteBuf){
            ctx.writeAndFlush(msg);
        }
        System.out.println("OutMessageHandler отправляет в сеть текстовое сообщение...");
        String message = (String) msg;
        System.out.println("... пытается отправить сообщения = " + message);
        ByteBuf buf = ctx.alloc().buffer(4 + message.length());
        buf.writeInt(message.length());
        buf.writeBytes(message.getBytes());
        ctx.writeAndFlush(buf);
        buf.release();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        promise.cause().printStackTrace();
        ctx.close();
    }
}
