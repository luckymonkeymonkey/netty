package nio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class Program {

	public static void readFile() throws Exception {
		FileInputStream fin = new FileInputStream("c:\\test.txt");

		FileChannel fc = fin.getChannel();

		ByteBuffer buffer = ByteBuffer.allocate(1024);

		fc.read(buffer);

		buffer.flip();

		while (buffer.remaining() > 0) {
			byte b = buffer.get();
			System.out.print((char) b);
		}

		fin.close();
	}

	static private final byte message[] = { 83, 111, 109, 101, 32, 98, 121, 116, 101, 115, 46 };

	public static void writeFile() throws Exception {
		FileOutputStream fout = new FileOutputStream("c:\\test.txt");

		FileChannel fc = fout.getChannel();

		ByteBuffer buffer = ByteBuffer.allocate(1024);

		for (int i = 0; i < message.length; ++i) {
			buffer.put(message[i]);
		}

		buffer.flip();

		fc.write(buffer);

		fout.close();
	}

	/**
	 * 测试position limit capacity
	 * 在第一篇中，我们介绍了NIO中的两个核心对象：缓冲区和通道，在谈到缓冲区时，我们说缓冲区对象本质上是一个数组，但它其实是一个特殊的数组，
	 * 缓冲区对象内置了一些机制，能够跟踪和记录缓冲区的状态变化情况，如果我们使用get()方法从缓冲区获取数据或者使用put()方法把数据写入缓冲区，
	 * 都会引起缓冲区状态的变化。本文为NIO使用及原理分析的第二篇，将会分析NIO中的Buffer对象。
	 * 在缓冲区中，最重要的属性有下面三个，它们一起合作完成对缓冲区内部状态的变化跟踪：
	 * position：指定了下一个将要被写入或者读取的元素索引，它的值由get()/put()方法自动更新，在新创建一个Buffer对象时，
	 * position被初始化为0。
	 * limit：指定还有多少数据需要取出(在从缓冲区写入通道时)，或者还有多少空间可以放入数据(在从通道读入缓冲区时)。
	 * capacity：指定了可以存储在缓冲区中的最大数据容量，实际上，它指定了底层数组的大小，或者至少是指定了准许我们使用的底层数组的容量。
	 * 以上四个属性值之间有一些相对大小的关系：0 <= position <= limit <=
	 * capacity。如果我们创建一个新的容量大小为10的ByteBuffer对象，在初始化的时候，position设置为0，limit和
	 * capacity被设置为10，在以后使用ByteBuffer对象过程中，capacity的值不会再发生变化，而其它两个个将会随着使用而变化。
	 * 四个属性值分别如图所示：
	 * 
	 * 现在我们可以从通道中读取一些数据到缓冲区中，注意从通道读取数据，相当于往缓冲区中写入数据。如果读取4个自己的数据，则此时position的值为4，
	 * 即下一个将要被写入的字节索引为4，而limit仍然是10，如下图所示：
	 * 
	 * 下一步把读取的数据写入到输出通道中，相当于从缓冲区中读取数据，在此之前，必须调用flip()方法，该方法将会完成两件事情： 1.
	 * 把limit设置为当前的position值 2. 把position设置为0
	 * 由于position被设置为0，所以可以保证在下一步输出时读取到的是缓冲区中的第一个字节，而limit被设置为当前的position，
	 * 可以保证读取的数据正好是之前写入到缓冲区中的数据，如下图所示：
	 * 
	 * 现在调用get()方法从缓冲区中读取数据写入到输出通道，这会导致position的增加而limit保持不变，
	 * 但position不会超过limit的值，所以在读取我们之前写入到缓冲区中的4个自己之后，position和limit的值都为4，如下图所示：
	 * 
	 * 在从缓冲区中读取数据完毕后，limit的值仍然保持在我们调用flip()方法时的值，调用clear()方法能够把所有的状态变化设置为初始化时的值，
	 * 如下图所示：
	 * 
	 * 最后我们用一段代码来验证这个过程，如下所示：
	 * 
	 * @throws IOException
	 * 
	 */
	public static void testPosition() throws IOException {
		FileInputStream fin = new FileInputStream("c:\\test.txt");
		FileChannel fc = fin.getChannel();

		ByteBuffer buffer = ByteBuffer.allocate(10);
		output("初始化", buffer);

		fc.read(buffer);
		output("调用read()", buffer);

		buffer.flip();
		output("调用flip()", buffer);

		while (buffer.remaining() > 0) {
			byte b = buffer.get();
		}
		output("调用get", buffer);

		buffer.clear();
		output("调用clear", buffer);
	}

	public static void output(String step, Buffer buffer) {
		System.out.println(step + " : ");
		System.out.print("capacity: " + buffer.capacity() + ", ");
		System.out.print("position: " + buffer.position() + ", ");
		System.out.println("limit: " + buffer.limit());
		System.out.println();
	}

	/**
	 * 测试子缓存区
	 * 在NIO中，除了可以分配或者包装一个缓冲区对象外，还可以根据现有的缓冲区对象来创建一个子缓冲区，即在现有缓冲区上切出一片来作为一个新的缓冲区，
	 * 但现有的缓冲区与创建的子缓冲区在底层数组层面上是数据共享的，也就是说，子缓冲区相当于是现有缓冲区的一个视图窗口。调用slice()
	 * 方法可以创建一个子缓冲区，让我们通过例子来看一下：
	 */
	public static void testSlice() {
		ByteBuffer buffer = ByteBuffer.allocate(10);

		// 缓冲区中的数据0-9
		for (int i = 0; i < buffer.capacity(); i++) {
			buffer.put((byte) i);
		}

		// 创建子缓冲区
		buffer.position(3);
		buffer.limit(7);
		ByteBuffer slice = buffer.slice();

		for (int i = 0; i < slice.capacity(); i++) {
			byte b = slice.get(i);
			b *= 10;
			slice.put(i, b);
		}

		buffer.position(0);
		buffer.limit(buffer.capacity());

		while (buffer.remaining() > 0) {
			System.out.print(buffer.get() + "; ");
		}
	}

	/**
	 * 只读缓冲区
	 * 只读缓冲区非常简单，可以读取它们，但是不能向它们写入数据。可以通过调用缓冲区的asReadOnlyBuffer()方法，将任何常规缓冲区转
	 * 换为只读缓冲区，这个方法返回一个与原缓冲区完全相同的缓冲区，并与原缓冲区共享数据，只不过它是只读的。如果原缓冲区的内容发生了变化，
	 * 只读缓冲区的内容也随之发生变化：
	 */
	public static void testReadonly() {
		ByteBuffer buffer = ByteBuffer.allocate(10);

		// 缓冲区中的数据0-9
		for (int i = 0; i < buffer.capacity(); i++) {
			buffer.put((byte) i);
		}

		// 创建只读缓冲区
		ByteBuffer readOnly = buffer.asReadOnlyBuffer();

		// 改变原缓冲区的内容
		for (int i = 0; i < buffer.capacity(); i++) {
			byte b = buffer.get(i);
			b *= 10;
			buffer.put(i, b);
		}

		readOnly.position(0);
		readOnly.limit(buffer.capacity());

		while (readOnly.remaining() > 0) {
			System.out.print(readOnly.get() + "; ");
		}
	}

	/**
	 * @throws Exception
	 * 
	 */
	public static void testDirect() throws Exception {
		FileInputStream fin = new FileInputStream("c:\\test.txt");
		FileChannel fcin = fin.getChannel();

		String outFile = String.format("c:\\testCopy.txt");
		FileOutputStream fout = new FileOutputStream(outFile);

		FileChannel fcout = fout.getChannel();
		ByteBuffer buffer = ByteBuffer.allocateDirect(1024);

		while (true) {
			buffer.clear();

			int r = fcin.read(buffer);

			if (r == -1) {
				break;
			}

			buffer.flip();

			fcout.write(buffer);
		}

	}

	/**
	 * 内存映射文件I/O
	 * 内存映射文件I/O是一种读和写文件数据的方法，它可以比常规的基于流或者基于通道的I/O快的多。内存映射文件I/O是通过使文件中的数据出现为
	 * 内存数组的内容来完成的，这其初听起来似乎不过就是将整个文件读到内存中，但是事实上并不是这样。一般来说，
	 * 只有文件中实际读取或者写入的部分才会映射到内存中
	 * 
	 * @throws IOException
	 */
	public static void testMapped() throws IOException {
		RandomAccessFile raf = new RandomAccessFile("c:\\test.txt", "rw");
		FileChannel fc = raf.getChannel();

		MappedByteBuffer mbb = fc.map(FileChannel.MapMode.READ_WRITE, 0, 1024);

		mbb.put(0, (byte) 97);
		mbb.put(1023, (byte) 122);

		raf.close();
	}

	public static void main(String[] args) {
		try {
			// readFile();
			// writeFile();
			// testPosition();
			// testSlice();
			// testReadonly();
//			testDirect();
			testMapped();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
