package com.ethereal.rtmp.server.rtmp.messages;

import com.ethereal.rtmp.amf.AMF0;
import com.ethereal.rtmp.server.rtmp.Constants;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 18 for AMF0 and message type value of 15 for AMF3
 * 
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class RtmpDataMessage extends RtmpMessage {
	List<Object> data;

	@Override
	public ByteBuf encodePayload() {
		ByteBuf buffer = Unpooled.buffer();
		AMF0.encode(buffer, data);
		return buffer;
	}

	@Override
	public int getOutboundCsid() {
		return 3;
	}

	@Override
	public int getMsgType() {
		return Constants.MSG_TYPE_DATA_MESSAGE_AMF0;
	}
}
