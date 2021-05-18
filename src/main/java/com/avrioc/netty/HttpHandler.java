package com.avrioc.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.util.CharsetUtil;

public class HttpHandler extends SimpleChannelInboundHandler<FullHttpRequest> {
    WebSocketServerHandshaker handshaker;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest msg) {

        if (msg instanceof HttpRequest) {

            HttpRequest httpRequest = (HttpRequest) msg;

            System.out.println("Http Request Received");

            HttpHeaders headers = httpRequest.headers();
            System.out.println("Connection : " +headers.get("Connection"));
            System.out.println("Upgrade : " + headers.get("Upgrade"));

            if ("Upgrade".equalsIgnoreCase(headers.get(HttpHeaderNames.CONNECTION)) &&
                    "WebSocket".equalsIgnoreCase(headers.get(HttpHeaderNames.UPGRADE))) {

                //Adding new handler to the existing pipeline to handle WebSocket Messages
                ctx.pipeline().replace(this, "websocketHandler", new WebSocketHandler());

                System.out.println("WebSocketHandler added to the pipeline");
                System.out.println("Opened Channel : " + ctx.channel());
                System.out.println("Handshaking....");
                //Do the Handshake to upgrade connection from HTTP to WebSocket protocol
                handleHandshake(ctx, httpRequest);
                System.out.println("Handshake is done");
            }
        } else {
            System.out.println("Incoming request is unknown");
        }

        /*ByteBuf content = Unpooled.copiedBuffer("Hello World!", CharsetUtil.UTF_8);
        FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/html");
        response.headers().set(HttpHeaderNames.CONTENT_LENGTH, content.readableBytes());
        ctx.write(response);
        ctx.flush();*/
    }

    /* Do the handshaking for WebSocket request */
    protected void handleHandshake(ChannelHandlerContext ctx, HttpRequest req) {
        WebSocketServerHandshakerFactory wsFactory =
                new WebSocketServerHandshakerFactory(getWebSocketURL(req), null, true);
        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req);
        }
    }

    protected String getWebSocketURL(HttpRequest req) {
        System.out.println("Req URI : " + req.getUri());
        String url =  "ws://" + req.headers().get("Host") + req.getUri() ;
        System.out.println("Constructed URL : " + url);
        return url;
    }

}