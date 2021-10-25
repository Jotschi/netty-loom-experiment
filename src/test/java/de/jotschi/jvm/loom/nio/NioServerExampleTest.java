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
public class NioServerExampleTest {

	private final static int PORT = 8080;

	@Test
	public void testServer() throws IOException {
		Selector selector = Selector.open();
		ServerSocketChannel socket = ServerSocketChannel.open();
		socket.configureBlocking(false);
		socket.bind(new InetSocketAddress("localhost", PORT));
		socket.register(selector, SelectionKey.OP_ACCEPT);
		System.err.println("Server running on http://127.0.0.1:" + PORT + '/');

		ThreadFactory executor = Thread.ofVirtual().factory();

		while (selector.select() > 0) {
			Set<SelectionKey> keys = selector.selectedKeys();
			for (SelectionKey key : keys) {
				keys.remove(key);
				if (key.isValid() && key.isAcceptable()) {
					ServerSocketChannel serverSocketChannel1 = (ServerSocketChannel) key.channel();
					SocketChannel socketChannel = serverSocketChannel1.accept();
					socketChannel.configureBlocking(false);

					// Create new thread to process the connection.
					// I don't think this is a good solution since it is not ensured that threads
					// will directly be terminated afterwards.
					executor.newThread(() -> {
						try {
							Selector threadSelector = Selector.open();
							socketChannel.register(threadSelector, SelectionKey.OP_READ);

							while (true) {
								if (threadSelector.select(500) <= 0) {
									continue;
								}

								Set<SelectionKey> selectorKeys = threadSelector.selectedKeys();
								Iterator<SelectionKey> selectorIt = selectorKeys.iterator();

								while (selectorIt.hasNext()) {
									SelectionKey myKey = selectorIt.next();
									selectorIt.remove();
									if (myKey.isReadable()) {

										SocketChannel clientCh2 = (SocketChannel) myKey.channel();

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
										return;
									}
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
						}
					}).start();

				}
			}
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
