package nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;

/**
 * 
 * @author xiaoyl
 *
 */
public class NIOClient {
	
	private SocketChannel client;
	
	private Selector selector;
	
	private ByteBuffer receivedBuffer = ByteBuffer.allocate(1024);
	
	private ByteBuffer sendBuffer = ByteBuffer.allocate(1024);
	
	//服务端地址
	private InetSocketAddress serverAddress = new InetSocketAddress("localhost",8080);
	
	public NIOClient() throws IOException{
		client = SocketChannel.open();
		client.configureBlocking(false);
		client.connect(serverAddress);		
		selector = Selector.open();
		//向管家注册一次连接事件，服务端的管家会select出来此次注册事件
		client.register(selector, SelectionKey.OP_CONNECT);
	}
	
	public void session() throws IOException{
		//先要判断是否已经建立连接
		if(client.isConnectionPending()){
			//向服务端完成连接
			client.finishConnect();
			
			//告诉管家我已与boss完成连接，并且我可以写数据了，等着管家叫号
			//服务端或者客户端的管家都会select出来此次注册事件（由于并没有向服务端发送数据通信，所以此次事件会在客户端的管家轮询出来）
			client.register(selector, SelectionKey.OP_WRITE);
			
			System.out.println("请在控制台登记姓名");
		}
		
		Scanner scan = new Scanner(System.in);
		while(scan.hasNextLine()){
			String name = scan.nextLine();
			if("".equals(name)){
				continue;
			}
			
			process(name);
		}
	}
	
	
	public void process(String name) throws IOException{
		boolean isUnFinished = true;
		
		//开始轮询
		while(isUnFinished){
			//判断一下，当前是否有客户来注册，有没有排队的，有没有取号的
			//如果没有，进行下一次轮询
			int i = selector.select();
			if(i == 0){
				continue;
			}
			
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();

			while(iterator.hasNext()){
				SelectionKey key = iterator.next();

				if(key.isWritable()){ //如果key是可写状态
					//写入数据进入channel(向服务端通信，服务端会接收到)
					sendBuffer.clear();
					sendBuffer.put(name.getBytes());
					sendBuffer.flip();
					client.write(sendBuffer);
					
					//向管家注册一次，告诉管家我已经写完数据，已经可以读数据啦，由于向服务端那边进行过数据IO通信，服务端的那边管家会select出来这次注册事件
					client.register(selector, SelectionKey.OP_READ);	
				}else if(key.isReadable()){
					//从client读入数据进入receivedBuffer
					int len = client.read(receivedBuffer);
					
					if(len > 0){
						receivedBuffer.flip();
						System.out.println("获取到服务端反馈的消息：" + new String(receivedBuffer.array() , len));
						
						//告诉管家我读完数据，已经可以写啦
						client.register(selector, SelectionKey.OP_WRITE);
					}
					
					isUnFinished = false;
				}
			}
		}
	}
	
	public static void main(String[] args) throws IOException {
		new NIOClient().session();
	}
}
