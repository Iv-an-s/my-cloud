package com.mycompany.my.cloud.server;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Optional;

public class AuthHandler extends ChannelInboundHandlerAdapter {
    private boolean authOk = false;

    // протокол для авторизации:
    // 5 байт (/auth) -> 4 байта(int - длина логина) -> n байт (логин) -> 4 байта (int - длина пароля) -> n байт (пароль)


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg){

        if(authOk){
            ctx.fireChannelRead(msg);
            return;
        }

        System.out.println("Попытка подключения нового клиента");
        AuthenticationProvider authenticationProvider = new DBAuthenticationProvider();
        authenticationProvider.init();
        ByteBuf buf = (ByteBuf) msg;
        String login = "";
        String password = "";
        String username;
        int loginLength = 0;
        int passwordLength = 0;
        int state = -1;

        if(state == -1) {
            if (buf.readableBytes() < 5) {
                return;
            }
            byte[] data = new byte[5];
            buf.readBytes(data);
            String str = new String(data);
            if (!str.equals("/auth")) {
                System.out.println("Вы не авторизованы! Пройдите авторизацию!");
                return;
            }
            System.out.println("Прочитали /auth");
            state = 0;
        }

        if (state == 0) {
            if (buf.readableBytes() < 4) {
                return;
            }
            loginLength = buf.readInt();
            state = 1;
            System.out.println("Прочитали loginLength = " + loginLength);
        }

        if(state == 1) {
            if(buf.readableBytes()< loginLength) {
                return;
            }
            byte[]loginBytes = new byte[loginLength];
            buf.readBytes(loginBytes);
            login = new String(loginBytes);
            state = 2;
            System.out.println("Прочитали login: " + login);
        }

        if (state == 2){
            if (buf.readableBytes() < 4) {
                return;
            }
            passwordLength = buf.readInt();
            state = 3;
            System.out.println("Прочитали passwordLength: " + passwordLength);
        }

        if (state == 3) {
            if (buf.readableBytes() < passwordLength) {
                return;
            }
            byte[] passwordBytes = new byte[passwordLength];
            buf.readBytes(passwordBytes);
            password = new String(passwordBytes);
            state = 4;
            System.out.println("Прочитали password: " + password);
        }

        if (state == 4){
            // проверка логина и пароля в БД
            // если пользователь найден, то сохраняем имя пользователя и прокидываем сообщение в следующий handler:

            Optional<String> optional = authenticationProvider.getNicknameByLoginAndPassword(login, password);
            if(!optional.isPresent()) {
                System.out.println("Incorrect login or password. Try again");
                ctx.writeAndFlush("/login_failed Incorrect login or password!");
                state = -1;
                return;
            }
            username = optional.get();
            System.out.println(username + " подключился");
            authOk = true;
            ctx.writeAndFlush("/login_ok " + username);
            ctx.pipeline().addLast(new MainHandler(username));
            ctx.fireChannelRead(msg);
            buf.release();
            state = -1;
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause){
        cause.printStackTrace();
        ctx.close();
    }
}
