package com.ethereal.rtmp.server.handlers;

import com.ethereal.rtmp.server.rtmp.messages.RtmpMessage;
import com.ethereal.rtmp.server.rtmp.messages.SetChunkSize;
import com.ethereal.rtmp.server.rtmp.RtmpMessageDecoder;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;

import static com.ethereal.rtmp.server.rtmp.Constants.*;

/**
 * Chunk Stream是对传输RTMP Chunk的流的逻辑上的抽象，客户端和服务器之间有关RTMP的信息都在这个流上通信。
 * 这个流上的操作也是我们关注RTMP协议的重点。
 *
 * Message是指满足该协议格式的、可以切分成Chunk发送的消息，消息包含的字段如下：
 *
 * Timestamp（时间戳）：消息的时间戳（但不一定是当前时间，后面会介绍），4个字节
 * Length(长度)：是指Message Payload（消息负载）即音视频等信息的数据的长度，3个字节
 * TypeId(类型Id)：消息的类型Id，1个字节
 * Message Stream ID（消息的流ID）：每个消息的唯一标识，划分成Chunk和还原Chunk为Message的时候都是根据这个ID来辨识是否是同一个消息的Chunk的，4个字节，并且以小端格式存储
 */
@Slf4j
public class ChunkDecoder extends ReplayingDecoder<DecodeState> {

	// changed by client command
	int clientChunkSize = 128;

	HashMap<Integer/* csid */, RtmpHeader> prevousHeaders = new HashMap<>(4);
	HashMap<Integer/* csid */, ByteBuf> inCompletePayload = new HashMap<>(4);

	ByteBuf currentPayload = null;
	int currentCsid;

	int ackWindowSize = -1;

	/**
	 * RTMP在收发数据的时候并不是以Message为单位的，而是把Message拆分成Chunk发送，
	 * 而且必须在一个Chunk发送完成之后才能开始发送下一个Chunk。每个Chunk中带有MessageID代表属于哪个Message，
	 * 接受端也会按照这个id来将chunk组装成Message。
	 *
	 * 为什么RTMP要将Message拆分成不同的Chunk呢？通过拆分，数据量较大的Message可以被拆分成较小的“Message”，
	 * 这样就可以避免优先级低的消息持续发送阻塞优先级高的数据，比如在视频的传输过程中，会包括视频帧，
	 * 音频帧和RTMP控制信息，如果持续发送音频数据或者控制数据的话可能就会造成视频帧的阻塞，
	 * 然后就会造成看视频时最烦人的卡顿现象。同时对于数据量较小的Message，可以通过对Chunk Header的字段来压缩信息，
	 * 从而减少信息的传输量。
	 *
	 * Chunk的默认大小是128字节，在传输过程中，通过一个叫做Set Chunk Size的控制信息可以设置Chunk数据量的最大值，
	 * 在发送端和接受端会各自维护一个Chunk Size，可以分别设置这个值来改变自己这一方发送的Chunk的最大大小。
	 * 大一点的Chunk减少了计算每个chunk的时间从而减少了CPU的占用率，但是它会占用更多的时间在发送上，
	 * 尤其是在低带宽的网络情况下，很可能会阻塞后面更重要信息的传输。小一点的Chunk可以减少这种阻塞问题，
	 * 但小的Chunk会引入过多额外的信息（Chunk中的Header），
	 * 少量多次的传输也可能会造成发送的间断导致不能充分利用高带宽的优势，因此并不适合在高比特率的流中传输。
	 * 在实际发送时应对要发送的数据用不同的Chunk Size去尝试，通过抓包分析等手段得出合适的Chunk大小，
	 * 并且在传输过程中可以根据当前的带宽信息和实际信息的大小动态调整Chunk的大小，
	 * 从而尽量提高CPU的利用率并减少信息的阻塞机率。
	 *
	 */
	@Override
	protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {

		DecodeState state = state();

		if (state == null) {
			state(DecodeState.STATE_HEADER);
		}
		if (state == DecodeState.STATE_HEADER) {
			RtmpHeader rtmpHeader = readHeader(in);
			log.debug("rtmpHeader read:{}", rtmpHeader);

			completeHeader(rtmpHeader);
			currentCsid = rtmpHeader.getCsid();

			// initialize the payload
			if (rtmpHeader.getFmt() != CHUNK_FMT_3) {
				ByteBuf buffer = Unpooled.buffer(rtmpHeader.getMessageLength(), rtmpHeader.getMessageLength());
				inCompletePayload.put(rtmpHeader.getCsid(), buffer);
				prevousHeaders.put(rtmpHeader.getCsid(), rtmpHeader);
			}

			currentPayload = inCompletePayload.get(rtmpHeader.getCsid());
			if (currentPayload == null) {
				// when fmt=3 and previous body completely read, the previous msgLength play the
				// role of length
				RtmpHeader previousHeader = prevousHeaders.get(rtmpHeader.getCsid());
				log.debug("current payload null,previous header:{}", previousHeader);
				currentPayload = Unpooled.buffer(previousHeader.getMessageLength(), previousHeader.getMessageLength());
				inCompletePayload.put(rtmpHeader.getCsid(), currentPayload);
				log.debug("current payload assign as :{}",currentPayload);
			}

			checkpoint(DecodeState.STATE_PAYLOAD);
		} else if (state == DecodeState.STATE_PAYLOAD) {

			final byte[] bytes = new byte[Math.min(currentPayload.writableBytes(), clientChunkSize)];
			in.readBytes(bytes);
			currentPayload.writeBytes(bytes);
			checkpoint(DecodeState.STATE_HEADER);

			if (currentPayload.isWritable()) {
				return;
			}
			inCompletePayload.remove(currentCsid);

			// then we can decode out payload
			ByteBuf payload = currentPayload;
			RtmpHeader header = prevousHeaders.get(currentCsid);

			RtmpMessage msg = RtmpMessageDecoder.decode(header, payload);
			if (msg == null) {
				log.error("RtmpMessageDecoder.decode NULL");
				return;
			}

			if (msg instanceof SetChunkSize) {
				// we need chunksize to decode the chunk
				SetChunkSize scs = (SetChunkSize) msg;
				clientChunkSize = scs.getChunkSize();
				log.debug("------------>client set chunkSize to :{}", clientChunkSize);
			} else {
				out.add(msg);
			}
		}

	}

	private RtmpHeader readHeader(ByteBuf in) {
		RtmpHeader rtmpHeader = new RtmpHeader();

		// alway from the beginning
		int headerLength = 0;

		byte firstByte = in.readByte();
		headerLength += 1;

		// CHUNK HEADER is divided into
		// BASIC HEADER
		// MESSAGE HEADER
		// EXTENDED TIMESTAMP

		// BASIC HEADER
		// fmt and chunk steam id in first byte
		int fmt = (firstByte & 0xff) >> 6;
		int csid = (firstByte & 0x3f);

		if (csid == 0) {
			// 2 byte form
			csid = in.readByte() & 0xff + 64;
			headerLength += 1;
		} else if (csid == 1) {
			// 3 byte form
			byte secondByte = in.readByte();
			byte thirdByte = in.readByte();
			csid = (thirdByte & 0xff) << 8 + (secondByte & 0xff) + 64;
			headerLength += 2;
		} else if (csid >= 2) {
			// that's it!
		}

		rtmpHeader.setCsid(csid);
		rtmpHeader.setFmt(fmt);

		// basic header complete

		// MESSAGE HEADER
		switch (fmt) {
		case CHUNK_FMT_0: {
			int timestamp = in.readMedium();
			int messageLength = in.readMedium();
			short messageTypeId = (short) (in.readByte() & 0xff);
			int messageStreamId = in.readIntLE();
			headerLength += 11;
			if (timestamp == MAX_TIMESTAMP) {
				long extendedTimestamp = in.readInt();
				rtmpHeader.setExtendedTimestamp(extendedTimestamp);
				headerLength += 4;
			}

			rtmpHeader.setTimestamp(timestamp);
			rtmpHeader.setMessageTypeId(messageTypeId);
			rtmpHeader.setMessageStreamId(messageStreamId);
			rtmpHeader.setMessageLength(messageLength);

		}
			break;
		case CHUNK_FMT_1: {
			int timestampDelta = in.readMedium();
			int messageLength = in.readMedium();
			short messageType = (short) (in.readByte() & 0xff);

			headerLength += 7;
			if (timestampDelta == MAX_TIMESTAMP) {
				long extendedTimestamp = in.readInt();
				rtmpHeader.setExtendedTimestamp(extendedTimestamp);
				headerLength += 4;
			}

			rtmpHeader.setTimestampDelta(timestampDelta);
			rtmpHeader.setMessageLength(messageLength);
			rtmpHeader.setMessageTypeId(messageType);
		}
			break;
		case CHUNK_FMT_2: {
			int timestampDelta = in.readMedium();
			headerLength += 3;
			rtmpHeader.setTimestampDelta(timestampDelta);

			if (timestampDelta == MAX_TIMESTAMP) {
				long extendedTimestamp = in.readInt();
				rtmpHeader.setExtendedTimestamp(extendedTimestamp);
				headerLength += 4;
			}

		}
			break;

		case CHUNK_FMT_3: {
			// nothing
		}
			break;

		default:
			throw new RuntimeException("illegal fmt type:" + fmt);

		}

		rtmpHeader.setHeaderLength(headerLength);

		return rtmpHeader;
	}

	private void completeHeader(RtmpHeader rtmpHeader) {
		RtmpHeader prev = prevousHeaders.get(rtmpHeader.getCsid());
		if (prev == null) {
			return;
		}
		switch (rtmpHeader.getFmt()) {
		case CHUNK_FMT_1:
			rtmpHeader.setMessageStreamId(prev.getMessageStreamId());
//			rtmpHeader.setTimestamp(prev.getTimestamp());
			break;
		case CHUNK_FMT_2:
//			rtmpHeader.setTimestamp(prev.getTimestamp());
			rtmpHeader.setMessageLength(prev.getMessageLength());
			rtmpHeader.setMessageStreamId(prev.getMessageStreamId());
			rtmpHeader.setMessageTypeId(prev.getMessageTypeId());
			break;
		case CHUNK_FMT_3:
			rtmpHeader.setMessageStreamId(prev.getMessageStreamId());
			rtmpHeader.setMessageTypeId(prev.getMessageTypeId());
			rtmpHeader.setTimestamp(prev.getTimestamp());
			rtmpHeader.setTimestampDelta(prev.getTimestampDelta());
			break;
		default:
			break;
		}

	}

}
