package io.netty.channel.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import io.netty.channel.EventLoop;
import io.netty.util.concurrent.EventExecutor;

/**
 * Common interface for NIO based eventloops.  
 */
public interface INioEventLoop extends EventLoop, EventExecutor {

	void cancel(SelectionKey key);

	int selectNow() throws IOException;

	Selector unwrappedSelector();

}
