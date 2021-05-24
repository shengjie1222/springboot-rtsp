package com.ethereal.rtmp.server.rtmp.messages;

import com.ethereal.rtmp.server.rtmp.Constants;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * The message types 19 for AMF0 and 16 for AMF3 are reserved for shared object
 * events
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SharedObjectMessage extends RtmpMessage {
	List<Object> body;

	@Override
	public ByteBuf encodePayload() {
		return null;
	}

	@Override
	public int getOutboundCsid() {
		return 4;
	}

	@Override
	public int getMsgType() {
		return Constants.MSG_TYPE_SHARED_OBJECT_MESSAGE_AMF0;
	}
}
