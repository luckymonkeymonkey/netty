package nio.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
/**
 * 服务端
 * @author xiaoyl
 *
 */
public class NIOServer {
	ServerSocketChannel server ;
	
	private Integer port = 8080;
	
	private Selector selector;
	
	private ByteBuffer receivedBuffer = ByteBuffer.allocate(1024);
	
	private ByteBuffer sendBuffer = ByteBuffer.allocate(1024);

	private Map<SelectionKey,String> sessionMsg = new HashMap<SelectionKey,String>();
	
	/**
	 * 
	 * @param port
	 * @throws IOException
	 */
	public NIOServer(Integer port) throws IOException{
		this.port = port;
		
		//先把高速公路建立起来
		server = ServerSocketChannel.open();
		
		//打开关卡 ，开始多路复用
		server.socket().bind(new InetSocketAddress(this.port));
		
		//默认是阻塞，手动设置为非阻塞
		server.configureBlocking(false);
		
		//管家开始营业
		selector = Selector.open();
		
		//告诉管家，Boss已经准备就绪，等会有客人来，就要通知我一下 
		server.register(selector, SelectionKey.OP_ACCEPT);
		
		System.out.println("NIO 服务已经启动，监听端口是：" + this.port);
	}
	
	/**
	 * 监听客户端的行为
	 * @throws IOException
	 */
	public void listener() throws IOException{
		//轮询
		while(true){
			//判断一下，当前是否有客户来注册，有没有排队的，有没有取号的
			int i = selector.select();
			
			//如果没有，进行下一次轮询
			if(i== 0){
				continue;
			}
			
			//取出被轮询到的keys
			Set<SelectionKey> keys = selector.selectedKeys();
			Iterator<SelectionKey> iterator = keys.iterator();
			
			while(iterator.hasNext()){
				//来一个处理一个
				process(iterator.next());
				
				//处理完，移走
				iterator.remove();
			}
		}
	}
	
	/**
	 * 处理数据
	 * @param key
	 * @throws IOException
	 */
	private void process(SelectionKey key) throws IOException{
		if(key == null){
			return;
		}
		
		if(key.isAcceptable()){//判断客户有没有跟Boss建立好连接
			//客户端和服务端初次交互
			SocketChannel client = server.accept();

			//客户端设立非阻塞模式
			client.configureBlocking(false);
			
			//客户告诉管家我与服务端已建立好连接，我可以被读数据了。下一步就是等你叫我。（此时服务端这边的管家只对read感兴趣）
			client.register(selector,SelectionKey.OP_READ);
		}else if(key.isReadable()){//判断是否可以读数据
			//获取通道
			SocketChannel client = (SocketChannel) key.channel();

			//读入客户端的数据进缓冲区
			receivedBuffer.clear();
			int len = client.read(receivedBuffer);
			if(len > 0){
				String msg = new String (receivedBuffer.array(),0,len);
				sessionMsg.put(key, msg);
				System.out.println("获取客户端发送来的数据：" + msg);
			}
			
			//数据被读完成之后，客户告诉管家可以被写了（此时服务端这边的管家只对write感兴趣）
			client.register(selector, SelectionKey.OP_WRITE);
		}else if(key.isWritable()){//是否可以写数据
			//判断消息缓存里面是否当前用户
			if(!sessionMsg.containsKey(key)){
				return;
			}
			
			//获取通道
			SocketChannel client = (SocketChannel) key.channel();

			//在客户那里写入缓冲区域，并在缓冲区域里面写入数据
			client.write(sendBuffer);
			sendBuffer.clear();
			sendBuffer.put(new String(sessionMsg.get(key) + "，你好，你的请求已处理完成").getBytes());
			sendBuffer.flip();
			
			//数据被写完成之后，客户告诉管家可以被读了 （此时服务端这边的管家只对read感兴趣）
			client.register(selector, SelectionKey.OP_READ);
		}	
	}
	public static void main(String[] args) throws IOException {
		new NIOServer(8080).listener();
	}
}
