package de.jotschi.jvm.loom.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Test which explores how virtual threads can be used with the
 * {@link AsynchronousServerSocketChannel} API.
 */
public class AsyncSocketServerTest {

	private final static int PORT = 8080;

	private InetSocketAddress hostAddress = new InetSocketAddress("localhost", PORT);
	private ThreadFactory factory = Thread.ofVirtual().name("server", 0).factory();

	@Test
	public void testServerWithGroup() throws IOException {
		AsynchronousChannelGroup group = AsynchronousChannelGroup.withFixedThreadPool(500, factory);
		AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open(group);
		serverChannel.bind(hostAddress);
		acceptConnection(serverChannel, false);
	}

	@Test
	public void testServerWithFork() throws IOException {
		AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
		serverChannel.bind(hostAddress);
		acceptConnection(serverChannel, true);
	}

	private void acceptConnection(AsynchronousServerSocketChannel serverChannel, boolean forkThread) {
		try {
			System.err.println("Server running on http://127.0.0.1:" + PORT + '/');
			serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
				@Override
				public void completed(AsynchronousSocketChannel clientChannel, Void att) {
					if (forkThread) {
						factory.newThread(() -> {
							handleRequest(serverChannel, clientChannel, this);
						}).start();
					} else {
						handleRequest(serverChannel, clientChannel, this);
					}
				}

				@Override
				public void failed(Throwable exc, Void att) {
					exc.printStackTrace();
				}

			});
			System.out.println("Server running. Press enter to terminate.");
			System.in.read();

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				serverChannel.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	private void handleRequest(AsynchronousServerSocketChannel serverChannel, AsynchronousSocketChannel clientChannel,
			CompletionHandler<AsynchronousSocketChannel, Void> completionHandler) {
		if (serverChannel.isOpen()) {
			serverChannel.accept(null, completionHandler);
		}
		if ((clientChannel != null) && (clientChannel.isOpen())) {

			ByteBuffer buffer = ByteBuffer.allocate(512);
			clientChannel.read(buffer, 700, TimeUnit.MILLISECONDS, buffer,
					new CompletionHandler<Integer, ByteBuffer>() {

						@Override
						public void completed(Integer result, ByteBuffer attachment) {
							buffer.flip();
							String message = new String(buffer.array()).trim();

							if (message.endsWith("*/*")) {
								String response = createResponse();
								ByteBuffer responseBuffer = ByteBuffer
										.wrap(response.getBytes(Charset.defaultCharset()));
								clientChannel.write(responseBuffer, 700, TimeUnit.MILLISECONDS, null,
										new CompletionHandler<Integer, Void>() {

											@Override
											public void completed(Integer result, Void attachment) {
//											System.out.println("Response send");
											}

											@Override
											public void failed(Throwable exc, Void attachment) {
												exc.printStackTrace();
											}
										});
								buffer.clear();
								try {
									clientChannel.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}

						@Override
						public void failed(Throwable exc, ByteBuffer attachment) {
							exc.printStackTrace();
						}
					});

		}

	}

	private String createResponse() {
		return """
				HTTP/1.1 200 OK
				Content-Length: 13
				Content-Type: text/plain; charset=utf-8

				Hello World!
				""";
	}

}