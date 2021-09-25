package com.mycompany.my.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;

public class AnswerHandler extends ChannelOutboundHandlerAdapter {

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        System.out.println("Сработал AnswerHandler");
        Long message = (Long)msg;
        System.out.println("AnswerHandler пытается отправить длину файла = " + message);
        ByteBuf buf = ctx.alloc().buffer(8);
        buf.writeLong(message);
        ctx.writeAndFlush(buf);
        buf.release();
    }

    @Override
    public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
        promise.cause().printStackTrace();
        ctx.close();
    }
}
