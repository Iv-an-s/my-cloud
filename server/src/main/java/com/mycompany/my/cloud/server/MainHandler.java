package com.mycompany.my.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Paths;

public class MainHandler extends ChannelInboundHandlerAdapter {
    // Протокол для передачи файла (после того как прочитали сигнальный байт):
    // (длина имени файла (4 байта(int n))) -> (имя файла (byte[n])) -> (размер файла (8 байт (long m)) -> (файл (m байт)))
    private enum DataType {
        EMPTY((byte) -1), FILE((byte) 15), COMMAND((byte) 16);

        byte firstMessageByte;

        DataType(byte firstMessageByte) {
            this.firstMessageByte = firstMessageByte;
        }

        static DataType getDataTypeFromByte(byte b) {
            if (b == FILE.firstMessageByte) {
                return FILE;
            }
            if (b == COMMAND.firstMessageByte) {
                return COMMAND;
            }
            return EMPTY;
        }
    }

    private int state = -1;
    private int reqLen = -1;
    private DataType type = DataType.EMPTY;
    boolean isCommandHandlerCreated = false;

    private String username;
    private String filename;
    private long fileSize;

    public MainHandler(String username) {
        this.username = username;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        ByteBuf buf = ((ByteBuf) msg);
        System.out.println("Работает MainHandler. Подготовлен буфер");
        // defining the message type
        if (state == -1) {
            byte firstByte = buf.readByte();
            type = DataType.getDataTypeFromByte(firstByte);
            state = 0;
            reqLen = 4;
            System.out.println("Message type is: " + type);
        }
        if (type.equals(DataType.COMMAND)) {
            if (!isCommandHandlerCreated) {
                ctx.pipeline().addLast(new CommandHandler(username));
                isCommandHandlerCreated = true;
            }
            ctx.fireChannelRead(buf);
            state = -1;
            return;
        }
        if (type.equals(DataType.EMPTY)) {
            state = -1;
            System.out.println("Incorrect message");
            return;
        }

        // defining the length of filename - int
        if (state == 0) {
            if (buf.readableBytes() < reqLen) {
                return;
            }
            reqLen = buf.readInt();
            state = 1;
            System.out.println("text size: " + reqLen);
        }

        // defining the filename - String (byte[])
        if (state == 1) {
            if (buf.readableBytes() < reqLen) {
                return;
            }
            byte[] data = new byte[reqLen];
            buf.readBytes(data);
            String str = new String(data);
            System.out.println(type + " " + str);

            filename = str;
            reqLen = 8;
            state = 2;
        }
        if (type.equals(DataType.FILE)) {
            getFile(buf);
        }
    }

    private void getFile(ByteBuf buf) {
        // получаем размер файла
        if (state == 2) {
            if (buf.readableBytes() < reqLen) {
                return;
            }
            fileSize = buf.readLong();
            System.out.println("Filesize is: " + fileSize + " bytes");
            state = 3;
        }
        // получаем файл
        if (state == 3) {
            long count = 0;
            try (OutputStream out = new BufferedOutputStream(new FileOutputStream(Paths.get("server_repository", filename).toFile()))) {
                while (buf.readableBytes() != -1) {
                    out.write(buf.readByte());
                    count++;
                    if (count == fileSize) {
                        break;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.println("Файл " + filename + " получен");
            state = -1;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}
