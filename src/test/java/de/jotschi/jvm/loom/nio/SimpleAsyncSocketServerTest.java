package de.jotschi.jvm.loom.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.charset.Charset;
import java.util.concurrent.Future;

import org.junit.Before;
import org.junit.Test;

/**
 *	Very basic single thread async socket server which uses one platform thread.  
 */
public class SimpleAsyncSocketServerTest {

	private final static int PORT = 8080;
	private AsynchronousServerSocketChannel serverChannel;

	@Before
	public void setupServer() {
		try {
			serverChannel = AsynchronousServerSocketChannel.open();
			InetSocketAddress hostAddress = new InetSocketAddress("localhost", PORT);
			serverChannel.bind(hostAddress);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Test
	public void testServer() {
		try {
			System.err.println("Server running on http://127.0.0.1:" + PORT + '/');
			// Keep the server running and accept new connections.
			while (true) {
				Future<AsynchronousSocketChannel> acceptResult = serverChannel.accept();
				AsynchronousSocketChannel clientChannel = acceptResult.get();

				if ((clientChannel != null) && (clientChannel.isOpen())) {
					
					while (true) {

						ByteBuffer buffer = ByteBuffer.allocate(32);
						Future<Integer> readResult = clientChannel.read(buffer);
						readResult.get();
						buffer.flip();
						String message = new String(buffer.array()).trim();

						if (message.endsWith("*/*")) {
							String response = createResponse();
							ByteBuffer responseBuffer = ByteBuffer.wrap(response.getBytes(Charset.defaultCharset()));
							Future<Integer> writeResult = clientChannel.write(responseBuffer);

							writeResult.get();
							buffer.clear();
							break;
						}
					}

					clientChannel.close();
				}
			}
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

	private String createResponse() {
		return """
				HTTP/1.1 200 OK
				Content-Length: 13
				Content-Type: text/plain; charset=utf-8

				Hello World!
				""";
	}

}