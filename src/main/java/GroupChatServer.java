import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Set;

/**
 * 群聊服务器
 *
 * @program: NIO-GroupChat
 * @description:
 * @author: yejc
 * @create: 2019-12-07 21:19
 **/
public class GroupChatServer {
    private ServerSocketChannel serverSocketChannel;
    private Selector selector;
    private static final int PORT = 6667;

    public GroupChatServer() {
        try {
            // 创建serverSocketChannel
            serverSocketChannel = ServerSocketChannel.open();
            // 设置非阻塞
            serverSocketChannel.configureBlocking(false);
            // 绑定端口
            serverSocketChannel.bind(new InetSocketAddress(PORT));
            // 创建selector
            selector = Selector.open();
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 持续监听
     */
    public void listen() {
        while (true) {
            try {
                int count = selector.select();
                // 如果count > 0，表示有事件可以执行
                if (count > 0) {
                    Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isAcceptable()) {
                            // 如果有连接，接受
                            SocketChannel socketChannel = serverSocketChannel.accept();
                            socketChannel.configureBlocking(false);
                            System.out.println(socketChannel.getRemoteAddress() + "上线了");
                            // 将socketChannel注册到selector上，并设置成ACCEPT时间
                            socketChannel.register(selector, SelectionKey.OP_READ);
                        }
                        if (selectionKey.isReadable()) {
                            readData(selectionKey);
                        }
                        // 处理完后删除
                        iterator.remove();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void readData(SelectionKey selectionKey) throws IOException {
        // 可读
        SocketChannel sc = (SocketChannel) selectionKey.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int read = 0;
        try {
            read = sc.read(buffer);
        } catch (IOException e) {
            System.out.println(sc.getRemoteAddress()+"离线了");
            sc.close();
        }
        if (read > 0) {
            String msg = new String(buffer.array());
            System.out.println("接收到" + sc.getRemoteAddress() + ":" + msg);
            // 转发给其他客户端（除自己外）
            sendToOrder((sc.getRemoteAddress() + "说：" + msg).getBytes(), sc);
        }
    }

    private void sendToOrder(byte[] bytes, SocketChannel self) throws IOException {
        Set<SelectionKey> selectionKeys = selector.keys();
        for (SelectionKey selectionKey : selectionKeys) {
            SelectableChannel channel = selectionKey.channel();
            if (channel instanceof SocketChannel && channel != self) {
                SocketChannel socketChannel = ((SocketChannel) channel);
                socketChannel.write(ByteBuffer.wrap(bytes));
            }
        }
    }

    public static void main(String[] args) {
        GroupChatServer server = new GroupChatServer();
        server.listen();
    }
}
