package com.mycompany.my.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.*;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class CommandHandler extends ChannelInboundHandlerAdapter {
    /**
     * Протокол для обработки команды LIST:
     * КЛИЕНТ --> СЕРВЕР: сигнальный байт типа команды 15(1 байт)
     * СЕРВЕР --> КЛИЕНТ: длина строки с именами файлов (1 int) --> строка (n байт). Завернуто в ByteBuf
     */
    private enum CommandType {
        LIST((byte) 15), SENDFILE((byte) 16), DELETEFILE((byte) 17), EMPTY((byte) -1);

        byte firstMessageByte;

        CommandType(byte firstMessageByte) {
            this.firstMessageByte = firstMessageByte;
        }

        static CommandType getCommandTypeFromByte(byte b) {
            if (b == LIST.firstMessageByte) {
                return LIST;
            }
            if (b == SENDFILE.firstMessageByte) {
                return SENDFILE;
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
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        System.out.println("СommandHandler started");
        ByteBuf buf = (ByteBuf) msg;
        byte commandByte = buf.readByte();

        switch (CommandType.getCommandTypeFromByte(commandByte)) {
            case LIST:
                System.out.println("Получен запрос на обновление списка файлов");
                sendFileList(ctx, buf);
                break;
            case SENDFILE:
                System.out.println("Получен запрос на отправку файла");
                executeSendFile(ctx, buf);
                sendFileList(ctx, buf);
                break;
            case DELETEFILE:
                System.out.println("Получен запрос на удаление файла");
                executeDeleteFile(buf);
                sendFileList(ctx, buf);
                break;
            case EMPTY:
                System.out.println("Получен некорректный запрос");
                break;
            default:
                break;
        }
    }

    private void sendFileList(ChannelHandlerContext ctx, ByteBuf buf) {
        Path path = Paths.get("server_repository", username);
        StringBuilder sb = new StringBuilder();
        try {
            Files.list(path).map(p -> {
                try {
                    return p.getFileName().toString() + " "
                            + Files.isDirectory(p) + " "
                            + Files.size(p) + " "
                            + LocalDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneOffset.ofHours(3)).toString() + " ";
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }).forEach(sb::append);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String fileList = sb.toString().trim();
        System.out.println("CommandHandler: собрал вот такой fileList - [" + fileList + "]");
        ctx.writeAndFlush(fileList);
        System.out.println("fileList отправлен");
        // OutMessageHandler преобразует String в ByteBuf-сообщение из int(длина строки) + сама строка
    }

    private void executeDeleteFile(ByteBuf buf) {
        System.out.println("Удаляю файл");
        int reqLen = 4;
        String filename;
        if (buf.readableBytes() < reqLen) {
            return;
        }
        reqLen = buf.readInt();
        if (buf.readableBytes() < reqLen) {
            return;
        }
        byte[] data = new byte[reqLen];
        buf.readBytes(data);
        filename = new String(data);
        System.out.println("required file's filename is " + filename);
        Path pathReq = Paths.get("server_repository", username, filename);
        if (Files.exists(pathReq)) {
            try {
                Files.delete(pathReq);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("file " + filename + "has been deleted");
    }

    private void executeSendFile(ChannelHandlerContext ctx, ByteBuf buf) {
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

        Path pathReq = Paths.get("server_repository", username, filename);
        // отправить длину файла (нужен OutboundHandler)
        ctx.pipeline().addLast(new AnswerHandler());
        Long fileSize = null;
        try {
            fileSize = Files.size(pathReq);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("fileSize is : " + fileSize);
        ctx.channel().writeAndFlush(fileSize);
        ctx.pipeline().remove(AnswerHandler.class);

        if (Files.exists(pathReq)) {
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
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void sendFile(Path path, Channel channel, ChannelFutureListener finishListener) throws IOException {
        FileRegion region = new DefaultFileRegion(new FileInputStream(path.toFile()).getChannel(), 0, Files.size(path));

        ChannelFuture transferOperationFuture = channel.writeAndFlush(region);
        if (finishListener != null) {
            transferOperationFuture.addListener(finishListener);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
