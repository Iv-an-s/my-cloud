package com.mycompany.my.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class CommandHandler extends ChannelInboundHandlerAdapter {
    /**
     * Протокол для обработки команды:
     * (сигнальный байт типа команды (1 байт)) ->
     */
    private enum CommandType {
        SENDFILE((byte) 17), LIST((byte) 18), DELETEFILE((byte) 19), EMPTY((byte)-1);

        byte firstMessageByte;

        CommandType(byte firstMessageByte) {
            this.firstMessageByte = firstMessageByte;
        }

        static CommandHandler.CommandType getDataTypeFromByte(byte b) {
            if (b == SENDFILE.firstMessageByte) {
                return SENDFILE;
            }
            if (b == LIST.firstMessageByte) {
                return LIST;
            }
            if (b == DELETEFILE.firstMessageByte) {
                return DELETEFILE;
            }
            return EMPTY;
        }
    }

    private String username;

    public CommandHandler(String username) {
        this.username = username;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){
        System.out.println("СommandHandler started");
        ByteBuf buf = (ByteBuf)msg;
        byte commandByte = buf.readByte();

        if (CommandType.getDataTypeFromByte(commandByte) == CommandType.SENDFILE){
            try {
                executeSendFile(ctx, buf);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (CommandType.getDataTypeFromByte(commandByte) == CommandType.DELETEFILE){
            executeDeleteFile(buf);
        }
    //todo
    }

    private void executeDeleteFile(ByteBuf buf) {
        System.out.println("Удаляю файл");
    }

    private void executeSendFile(ChannelHandlerContext ctx, ByteBuf buf) throws IOException {
        int reqLen = 4;
        String filename;
        System.out.println("Отправляю файл клиету: " + username);
        // defining the length of filename - int
        if (buf.readableBytes() < reqLen) {
            return;
        }
        reqLen = buf.readInt();
        System.out.println("filename size: " + reqLen);

        // defining the filename - String (byte[])
        if (buf.readableBytes() < reqLen) {
            return;
        }
        byte[] data = new byte[reqLen];
        buf.readBytes(data);
        filename = new String(data);
        System.out.println("required file's filename is " + filename);

            Path pathReq = Paths.get("server_repository", filename);
            // отправить длину файла (нужен OutboundHandler)
            ctx.pipeline().addLast(new AnswerHandler());
            Long fileSize = Files.size(pathReq);
            System.out.println("fileSize is : " + fileSize);
            ctx.channel().writeAndFlush(fileSize);
            ctx.pipeline().remove(AnswerHandler.class);

            if (Files.exists(pathReq)){
                System.out.println("Файл найден! Отправляю!");
                System.out.println("Размер файла =:" + fileSize);
                //отправляем файл
                try {
                    sendFile(pathReq, ctx.channel(), future -> {
                        if (!future.isSuccess()) {
                            future.cause().printStackTrace();
                            ctx.channel().close();
                        }
                        if (future.isSuccess()) {
                            System.out.println("Файл успешно передан");
                            ctx.channel().close();
                        }
                    });
                }catch (IOException e){
                    e.printStackTrace();
                }
            }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException{
        FileRegion region = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null){
            transferOperationFuture.addListener(finishListener);
        }
    }
}
