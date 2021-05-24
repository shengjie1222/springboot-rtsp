package com.ethereal.rtmp.server.handlers;

import com.ethereal.rtmp.server.entities.Stream;
import com.ethereal.rtmp.server.entities.StreamName;
import com.google.common.base.Splitter;
import com.ethereal.rtmp.server.manager.StreamManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.*;

@Slf4j
public class HttpFlvHandler extends SimpleChannelInboundHandler<HttpObject> {

	StreamManager streamManager;

	public HttpFlvHandler(StreamManager streamManager) {
		this.streamManager = streamManager;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpObject msg) throws Exception {
		if (msg instanceof HttpRequest) {
			HttpRequest req = (HttpRequest) msg;

			String uri = req.uri();
			List<String> appAndStreamName = Splitter.on("/").omitEmptyStrings().splitToList(uri);
			if (appAndStreamName.size() != 2) {
				httpResponseStreamNotExist(ctx, uri);
				return;
			}

			
			String app=appAndStreamName.get(0);
			String streamName= appAndStreamName.get(1);
			if(streamName.endsWith(".flv")) {
				streamName=streamName.substring(0, streamName.length()-4);
			}
			StreamName sn = new StreamName(app, streamName);
			log.info("http stream :{} requested",sn);
			Stream stream = streamManager.getStream(sn);

			if (stream == null) {
				httpResponseStreamNotExist(ctx, uri);
				return;
			}
			DefaultHttpResponse response = new DefaultHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK);
			response.headers().set(CONTENT_TYPE, "video/x-flv");
			response.headers().set(TRANSFER_ENCODING, "chunked");

			response.headers().set(ACCESS_CONTROL_ALLOW_ORIGIN,"*");
			response.headers().set(ACCESS_CONTROL_ALLOW_HEADERS,"Origin, X-Requested-With, Content-Type, Accept");
			response.headers().set(ACCESS_CONTROL_ALLOW_METHODS,"GET, POST, PUT,DELETE");
			ctx.writeAndFlush(response);

			stream.addHttpFlvSubscriber(ctx.channel());

		}

		if (msg instanceof HttpContent) {

		}

	}

	private void httpResponseStreamNotExist(ChannelHandlerContext ctx, String uri) {
		ByteBuf body = Unpooled.wrappedBuffer(("stream [" + uri + "] not exist").getBytes());
		DefaultFullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1,
				HttpResponseStatus.NOT_FOUND, body);
		response.headers().set(CONTENT_TYPE, "text/plain");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}

}
