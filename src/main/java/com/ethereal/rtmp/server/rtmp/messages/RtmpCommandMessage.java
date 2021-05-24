package com.ethereal.rtmp.server.rtmp.messages;

import com.ethereal.rtmp.amf.AMF0;
import com.ethereal.rtmp.server.rtmp.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RtmpCommandMessage extends RtmpMessage {
	List<Object> command;

	@Override
	public int getOutboundCsid() {
		return 3;

	}

	@Override
	public ByteBuf encodePayload() {
		ByteBuf buffer = Unpooled.buffer();
		AMF0.encode(buffer, command);
		return buffer;
	}

	@Override
	public int getMsgType() {
		return Constants.MSG_TYPE_COMMAND_AMF0;

	}
}
