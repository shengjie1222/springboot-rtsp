package com.ethereal.rtmp.server.rtmp.messages;

import com.ethereal.rtmp.server.rtmp.Constants;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString(callSuper = true)
public class AudioMessage extends RtmpMediaMessage {
	byte[] audioData;

	@Override
	public int getOutboundCsid() {
		return 10;
	}

	@Override
	public ByteBuf encodePayload() {
		return Unpooled.wrappedBuffer(audioData);
	}

	@Override
	public int getMsgType() {
		return Constants.MSG_TYPE_AUDIO_MESSAGE;
	}

	@Override
	public byte[] raw() {

		return audioData;
	}
	
	public boolean isAACAudioSpecificConfig(){
		return audioData.length>1 && audioData[1]==0;
	}
}
