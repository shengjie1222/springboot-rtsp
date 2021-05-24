package com.ethereal.rtmp.server.handlers;

import com.ethereal.rtmp.server.rtmp.Tools;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 *
 * rtmp三次握手：
 * 要建立一个有效的RTMP Connection链接，首先要“握手”:客户端要向服务器发送C0,C1,C2（按序）三个chunk，
 * 服务器向客户端发送S0,S1,S2（按序）三个chunk，然后才能进行有效的信息传输。
 * RTMP协议本身并没有规定这6个Message的具体传输顺序，但RTMP协议的实现者需要保证这几点:
 * 1.客户端主动发送C0，C1；客户端必须等待S1到达才能发送C2;必须等待S2到达才能发送其他数据；
 * 2.服务端必须等待C0到达才能发送S0和S1；必须等待C1到达才能发送S2；必须等待C2到达才能发送其他数据；
 *
 * C0和S0格式
 * 1.长度：1字节
 * 2.C0表示客户端需求的rtmp版本；S0表示服务端选择的rtmp版本。目前版本为3，0到2是之前使用的版本，4到31保留，32到255被禁用。
 *
 * C1和S1
 * 1.长度：1536字节，其中包含以下字段。
 * 时间戳（4字节）
 * 0标示（4字节）
 * 随机字段（1528字节），用于区分握手的返回数据；
 *
 *
 * C2和S2
 * 1.长度：1536字节，相当于的C1和S1的返回数据，包含数据如下：
 * 时间戳（4字节），对于C2来说是指发送C1的时间戳；对于S2来说是指发送S1的时间戳；
 * 时间戳（4字节），对于C2来说是指读到S1中的时间戳；对于S2来说是指读到C2中的时间戳；
 * 随机字段(1528字节),对于C2来说是指读到S1中的随机字段；对于S2来说是指读到C2中的随机字段；
 *
 *
 * 理论上来讲只要满足以上条件，如何安排6个Message的顺序都是可以的，
 * 但实际实现中为了在保证握手的身份验证功能的基础上尽量减少通信的次数，
 * 一般的发送顺序是这样的，这一点可以通过wireshark抓ffmpeg推流包进行验证：
 * ｜client｜Server ｜
 * ｜－－－C0+C1—->|
 * ｜<－－S0+S1+S2– |
 * ｜－－－C2-－－－> ｜
 *
 */
@Slf4j
public class HandShakeDecoder extends ByteToMessageDecoder {

	boolean c0c1done;

	boolean c2done;

	static int HANDSHAKE_LENGTH = 1536;
	static int VERSION_LENGTH = 1;

	// server rtmp version
	static byte S0 = 3;

	byte[] CLIENT_HANDSHAKE = new byte[HANDSHAKE_LENGTH];
	boolean handshakeDone;

	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		//handshake 完成，向下一个节点传递
		if (handshakeDone) {
			ctx.fireChannelRead(in);
			return;
		}

		ByteBuf buf = in;
		if (!c0c1done) {
			// read c0 and c1
			if (buf.readableBytes() < VERSION_LENGTH + HANDSHAKE_LENGTH) {
				return;
			}

			buf.readByte();

			buf.readBytes(CLIENT_HANDSHAKE);

			writeS0S1S2(ctx);
			c0c1done = true;

		} else {
			// read c2
			if (buf.readableBytes() < HANDSHAKE_LENGTH) {
				return;
			}

			buf.readBytes(CLIENT_HANDSHAKE);

			// handshake done
			CLIENT_HANDSHAKE = null;
			handshakeDone = true;

			//after handshake done, remove this handler
			ctx.channel().pipeline().remove(this);
		}

	}

	private void writeS0S1S2(ChannelHandlerContext ctx) {
		// S0+S1+S2
		ByteBuf responseBuf = Unpooled.buffer(VERSION_LENGTH + HANDSHAKE_LENGTH + HANDSHAKE_LENGTH);
		// version = 3
		responseBuf.writeByte(S0);
		// s1 time
		responseBuf.writeInt(0);
		// s1 zero
		responseBuf.writeInt(0);
		// s1 random bytes
		responseBuf.writeBytes(Tools.generateRandomData(HANDSHAKE_LENGTH - 8));
		// s2 time
		responseBuf.writeInt(0);
		// s2 time2
		responseBuf.writeInt(0);
		// s2 random bytes
		responseBuf.writeBytes(Tools.generateRandomData(HANDSHAKE_LENGTH - 8));

		ctx.writeAndFlush(responseBuf);
	}

}
