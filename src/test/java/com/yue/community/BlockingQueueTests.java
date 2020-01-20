package com.yue.community;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTests {

    public static void main(String[] args) {
        // 实例化阻塞队列，生产者、消费者共用一个阻塞队列
        BlockingQueue queue = new ArrayBlockingQueue(10); // 数组长度、队列容量为10
        // 一个生产者生产数据，三个消费者并发消费数据
        new Thread(new Producer(queue)).start(); // 实例化生产者的线程，线程体是Producer。start()启动线程
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
        new Thread(new Consumer(queue)).start();
    }
}

// 定义生产者线程
class Producer implements Runnable{

    // 实例化 producer线程时，要求调用方把阻塞队列传入
    private BlockingQueue<Integer> queue;

    public Producer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        try{
            // 每20ms生产一个数，交给队列管理
            for (int i = 0; i < 100; i++){
                Thread.sleep(20); // 模拟真实业务，用户访问网站，无论是生产数据还是使用数据，肯定会存在间隔
                queue.put(i);
                System.out.println(Thread.currentThread().getName() + "生产：" + queue.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

// 定义生产者线程
class Consumer implements Runnable{

    private BlockingQueue<Integer> queue;

    public Consumer(BlockingQueue<Integer> queue){
        this.queue = queue;
    }

    @Override
    public void run() {
        try {
            // 只要队列中有数据，就一直消费
            while (true){
                Thread.sleep(new Random().nextInt(1000)); // 用户使用数据的时间随机化，不确定. 0-1000随机数，大概率比20ms多，没有生产者生产速度快
                queue.take();
                System.out.println(Thread.currentThread().getName() + "消费：" + queue.size());
            }
        }catch (Exception e){
            e.printStackTrace();
        }

    }
}
