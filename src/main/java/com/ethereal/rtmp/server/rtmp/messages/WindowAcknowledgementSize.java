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
public class WindowAcknowledgementSize extends RtmpControlMessage {
	int windowSize;

	@Override
	public ByteBuf encodePayload() {
		return Unpooled.buffer(4).writeInt(windowSize);
	}
	
	@Override
	public int getMsgType() {
		return Constants.MSG_WINDOW_ACKNOWLEDGEMENT_SIZE;
	}
}
