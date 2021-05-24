package com.ethereal.rtmp.server.rtmp.messages;

import com.ethereal.rtmp.server.rtmp.Constants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SetPeerBandwidth extends RtmpControlMessage {
	int acknowledgementWindowSize;
	int limitType;

	@Override
	public ByteBuf encodePayload() {
		return Unpooled.buffer(5).writeInt(acknowledgementWindowSize).writeByte(limitType);

	}

	@Override
	public int getMsgType() {
		return Constants.MSG_SET_PEER_BANDWIDTH;
	}
}
