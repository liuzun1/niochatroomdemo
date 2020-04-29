package com.hank.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.Set;

public class ChatroomServer {
    ServerSocketChannel serverSocketChannel;
    Selector selector;
    Charset charset = Charset.forName("GBK");
    int port = 9999;

    //初始化数据
    public void init() {
        try {
            serverSocketChannel = ServerSocketChannel.open();
            selector = Selector.open();
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.bind(new InetSocketAddress(port));//设置IP
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            watching();

        } catch (Exception e) {
            // TODO: handle exception
        } finally {
            try {
                serverSocketChannel.close();
                selector.close();
            } catch (IOException e) {
                // TODO: handle exception
                e.printStackTrace();
            }
        }
    }

    //向客户端发送消息
    public void broadCast(String str, String username) throws IOException {
        Set<SelectionKey> keys = selector.keys();//因为同时可能有很多时间发生，并且需要知道发生的具体内容以及主体,获取所有的keys
        Iterator<SelectionKey> iterator = keys.iterator();//创建一个迭代器，迭代获取对象
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            Channel channel = selectionKey.channel();//排除serverSocket
            if (channel instanceof SocketChannel) {//关心的是SocketChannel事件
                SocketChannel socketChannel = (SocketChannel) channel;
                socketChannel.write(charset.encode(username+ "对大家说 : " + str));
            }
        }
    }

    public void watching() throws IOException {
        System.out.println("服务器启动成功.......");
        while (true){
            int readyChannels = selector.select();//等待事件发生，反应当前有多少事件发生
            if (readyChannels == 0) {
                continue;
            }
            //开始出力事件
            Set<SelectionKey> keys = selector.selectedKeys();//因为同时可能有很多时间发生，并且需要知道发生的具体内容以及主体
            Iterator<SelectionKey> iterator = keys.iterator();//创建一个迭代器，迭代获取对象
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    //客户端接入事件
                    SocketChannel channel = serverSocketChannel.accept();
                    channel.configureBlocking(false);//设置阻塞模式
                    channel.register(selector, SelectionKey.OP_READ);//创建一个read的Registr
                    //写入欢迎信息
                    channel.write(charset.encode("欢迎来到聊天室，请输入姓名"));
                    selectionKey.attach(new UserInfo());
                } else if (selectionKey.isReadable()) {
                    //获取客户端的channel
                    SocketChannel socketChannel = (SocketChannel) selectionKey.channel();
                    //获取channel内容，创建bytebuffer大小为128的容器
                    UserInfo userInfo = (UserInfo) selectionKey.attachment();
                    ByteBuffer buffer = ByteBuffer.allocate(128);

                    int flag = socketChannel.read(buffer);
                    StringBuffer stringBuffer = new StringBuffer();
                    while (flag > 0) {
                        buffer.flip();
                        stringBuffer.append(charset.decode(buffer));//后期需要重构

                        buffer.clear();
                        flag = socketChannel.read(buffer);
                    }
                    if (userInfo != null && userInfo.init) {
                        broadCast(stringBuffer.toString(),userInfo.getName());//将消息内容传递给BroadCast处理
                    } else {
                        UserInfo info = new UserInfo();
                        info.setName(stringBuffer.toString());
                        info.setInit(true);
                        selectionKey.attach(info);
                        socketChannel.write(charset.encode("您好" + info.getName() + "现在您可以聊天室里的人聊天了。"));
                    }
                }
                iterator.remove();
            }
        }

    }

    public static void main(String[] args) {
        //创建serverSocketChannel
        try (ServerSocketChannel  serverSocketChannel = ServerSocketChannel.open();) {
            serverSocketChannel.configureBlocking(false);//证明是非阻塞的，只有继承了selectAbleChanel
            ChatroomServer chatRoomServer = new ChatroomServer();
            //设置为非阻塞
            serverSocketChannel.configureBlocking(false);
            System.out.println("连接成功！，欢迎来到Hank的聊天室");
            Selector selector = Selector.open();//创建一个Selector
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            chatRoomServer.init();
            //注册选择器，在客户端接入的时候，委托selecter管理接入事件
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}

class UserInfo {
    String name;

    boolean init = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isInit() {
        return init;
    }

    public void setInit(boolean init) {
        this.init = init;
    }

}
