package de.jotschi.jvm.loom.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;

import org.junit.Test;

/**
 * NIO Server test which uses the non-async API.
 */
public class NioServerExampleTest2 {

	private final static int PORT = 8080;

	private final static int THREAD_COUNT = 100;

	@Test
	public void testServer() throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel socket = ServerSocketChannel.open();
		socket.configureBlocking(false);
		socket.bind(new InetSocketAddress("localhost", PORT));
		socket.register(selector, SelectionKey.OP_ACCEPT);
		System.err.println("Server running on http://127.0.0.1:" + PORT + '/');

		ThreadFactory executor = Thread.ofVirtual().factory();

		for (int i = 0; i < THREAD_COUNT; i++) {
			executor.newThread(() -> {
				try {
					while (true) {

						if (selector.selectNow() < 0) {
							continue;
						}
						Set<SelectionKey> selectorKeys = selector.selectedKeys();
						Iterator<SelectionKey> selectorIt = selectorKeys.iterator();
						synchronized (selectorIt) {

							while (selectorIt.hasNext()) {
								SelectionKey currentKey = selectorIt.next();
								selectorIt.remove();

								// Tests whether this key's channel is ready to accept a new socket connection
								if (currentKey.isAcceptable()) {
									ServerSocketChannel severChannel = (ServerSocketChannel) currentKey.channel();
									SocketChannel channel = severChannel.accept();
									channel.configureBlocking(false);
									channel.register(selector, SelectionKey.OP_READ);

								} else if (currentKey.isValid() && currentKey.isReadable()) {
									SocketChannel clientCh2 = (SocketChannel) currentKey.channel();

									ByteBuffer requestBuffer = ByteBuffer.allocate(1024);
									clientCh2.read(requestBuffer);
									String result = new String(requestBuffer.array()).trim();

									if (result.endsWith("*/*")) {
										String response = createResponse();
										ByteBuffer responseBuffer = ByteBuffer
												.wrap(response.getBytes(Charset.defaultCharset()));
										clientCh2.write(responseBuffer);
										clientCh2.close();
									}
								}
							}

						}
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			}).start();
		}

		System.in.read();
		System.out.println("Wait for termination. Press enter to stop server.");
	}

	private String createResponse() {
		return """
				HTTP/1.1 200 OK
				Content-Length: 13
				Content-Type: text/plain; charset=utf-8

				Hello World!
				""";
	}

	private void log(int threadNr, String str) {
		System.out.println("[" + threadNr + "] " + str);
	}
}
