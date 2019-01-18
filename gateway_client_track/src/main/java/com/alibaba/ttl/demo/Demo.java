package com.alibaba.ttl.demo;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Demo implements Runnable{

    // 在父线程中设置 TransmittableThreadLocal
    public static ThreadLocal<String> parent = new TransmittableThreadLocal<String>();

    public static ExecutorService executorService = Executors.newFixedThreadPool(10);

    public static void main(String[] args) {
        parent.set("value-set-in-parent");
        System.out.println(parent.get());
        Runnable task = new Demo();
        //子线程种读取值
        //System.out.println("----------子线程中读取值------------");
       // new Thread(task).start();

        //线程池中读取值
        System.out.println("----------线程池中读取值------------");
        // 额外的处理，生成修饰了的对象ttlRunnable
        Runnable ttlRunnable = TtlRunnable.get(task);
        executorService.submit(ttlRunnable);

        executorService.shutdown();
    }


    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        System.out.println(parent.get());
    }
}
