package de.jotschi.jvm.loom.netty;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderValues.CLOSE;
import static io.netty.handler.codec.http.HttpHeaderValues.KEEP_ALIVE;
import static io.netty.handler.codec.http.HttpHeaderValues.TEXT_PLAIN;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.StructuredExecutor;

import io.netty.buffer.ByteBuf;

/*
 * Copyright 2013 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpUtil;

public class HttpHelloWorldServerHandler extends SimpleChannelInboundHandler<HttpObject> {
  private static final byte[] CONTENT = { 'H', 'e', 'l', 'l', 'o', ' ', 'W', 'o', 'r', 'l', 'd' };

  @Override
  public void channelReadComplete(ChannelHandlerContext ctx) {
    ctx.flush();
  }

  @Override
  public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws InterruptedException, ExecutionException {
    if (msg instanceof HttpRequest) {
      try (var s = StructuredExecutor.open("netty")) {
        HttpRequest req = (HttpRequest) msg;
        Future<Boolean> keepAliveFut = s.fork(() -> {
          return HttpUtil.isKeepAlive(req);
        });

        Future<FullHttpResponse> r = s.fork(() -> {
          ByteBuf contentBuffer = Unpooled.wrappedBuffer(CONTENT);
          FullHttpResponse response = new DefaultFullHttpResponse(req.protocolVersion(), OK, contentBuffer);
          // System.out.println(Thread.currentThread().toString());
          response.headers().set(CONTENT_TYPE, TEXT_PLAIN).setInt(CONTENT_LENGTH, response.content().readableBytes());

          return response;
        });
        s.join();

        Boolean keepAlive = keepAliveFut.get();
        FullHttpResponse resp = r.get();
        if (keepAlive) {
          if (!req.protocolVersion().isKeepAliveDefault()) {
            resp.headers().set(CONNECTION, KEEP_ALIVE);
          }
        } else {
          // Tell the client we're going to close the connection.
          resp.headers().set(CONNECTION, CLOSE);
        }

        ChannelFuture f = ctx.write(r.get());
        if (!keepAlive) {
          f.addListener(ChannelFutureListener.CLOSE);
        }

      }
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
    cause.printStackTrace();
    ctx.close();
  }
}