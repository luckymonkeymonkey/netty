package nio;

import java.nio.ByteBuffer;

public class BufferWrap {
	
	public static void myMethod()  
    {  
        // 分配指定大小的缓冲区  
        ByteBuffer buffer1 = ByteBuffer.allocate(10);  
          
        // 包装一个现有的数组  
        byte array[] = new byte[11];  
        ByteBuffer buffer2 = ByteBuffer.wrap( array );  
    } 
	
	public static void main(String[] args) {
		myMethod();
		System.out.println("done");
	}
}
