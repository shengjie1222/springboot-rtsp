package com.ethereal.rtmp.server.manager;

import com.ethereal.rtmp.server.entities.Stream;
import com.ethereal.rtmp.server.entities.StreamName;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 所有的流都会被储存在这里，包括发布方，订阅方，视频和音频的类型
 */
public class StreamManager {
	private ConcurrentHashMap<StreamName, Stream> streams=new ConcurrentHashMap<>();
	
	public void newStream(StreamName streamName,Stream s) {
		streams.put(streamName, s);
	}
	
	public boolean exist(StreamName streamName) {
		return streams.containsKey(streamName);
	}
	
	public Stream getStream(StreamName streamName) {
		return streams.get(streamName);
	}
	
	public void remove(StreamName streamName) {
		streams.remove(streamName);
	}
}

