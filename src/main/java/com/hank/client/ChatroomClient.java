package com.hank.client;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

public class ChatroomClient {
    SocketChannel channel;
    Selector selector;
    int port = 9999;
    String ip = "127.0.0.1";

    public void init() {
        try {
            channel = SocketChannel.open(new InetSocketAddress(ip, port));
            channel.configureBlocking(false);
            selector = Selector.open();
            channel.register(selector, SelectionKey.OP_READ);

            // 启动线程
            new MySelectorThread(selector).start();
            while (true) {
                // 获取控制台输入
                Scanner scanner = new Scanner(System.in);
                String content = scanner.nextLine();
                channel.write(Charset.forName("GBK").encode(content));
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                channel.close();
                selector.close();
            } catch (IOException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }

    }

    class MySelectorThread extends Thread {
        Selector selector;

        public MySelectorThread(Selector selector) {
            // TODO Auto-generated constructor stub
            this.selector = selector;
        }

        @Override
        public void run() {
            // 处理事件
            try {
                while (true) {
                    int readyChannels = selector.select();// 等待事件发生，反应当前有多少事件发生
                    if (readyChannels == 0) {
                        continue;
                    }
                    // 开始处理事件
                    Set<SelectionKey> keys = selector.selectedKeys();// 因为同时可能有很多时间发生，并且需要知道发生的具体内容以及主体
                    Iterator<SelectionKey> iterator = keys.iterator();// 创建一个迭代器，迭代获取对象
                    while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        if (selectionKey.isReadable()) {
                            SocketChannel channel = (SocketChannel) selectionKey.channel();
                            ByteBuffer buffer = ByteBuffer.allocate(128);
                            StringBuffer stringBuffer = new StringBuffer();
                            while (channel.read(buffer) > 0) {
                                buffer.flip();
                                stringBuffer.append(Charset.forName("GBK").decode(buffer));
                                buffer.clear();
                            }
                            System.out.println(stringBuffer.toString());
                        }
                        iterator.remove();
                    }
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    public static void main(String[] args) {
        ChatroomClient chatRoomClient = new ChatroomClient();
        chatRoomClient.init();
    }


}
