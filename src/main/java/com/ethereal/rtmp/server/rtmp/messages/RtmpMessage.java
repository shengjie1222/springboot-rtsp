package com.ethereal.rtmp.server.rtmp.messages;

import io.netty.buffer.ByteBuf;
import lombok.Data;

@Data
public abstract class RtmpMessage {

//	RtmpHeader inboundHeader;

	int inboundHeaderLength;
	int inboundBodyLength;
	
//	public RtmpMessage attachInboundHeader(RtmpHeader theHeader) {
//		inboundHeader = theHeader;
//		return this;
//	}
//
//	public RtmpHeader retrieveInboundHeader() {
//		return inboundHeader;
//	}

	public abstract int getOutboundCsid()  ;
	
	public abstract int getMsgType();
	
	public abstract ByteBuf encodePayload();

}
