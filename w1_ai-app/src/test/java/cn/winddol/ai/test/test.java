package cn.winddol.ai.test;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


@Slf4j
@SpringBootTest
public class test {
    private static volatile int count = 0;
    private static final ReentrantLock LOCK = new ReentrantLock();
    private static final Condition A = LOCK.newCondition();
    private static final Condition B = LOCK.newCondition();
    private static final Condition C = LOCK.newCondition();
    private static final CountDownLatch latch = new CountDownLatch(3);
    @Test
    public void test() throws InterruptedException {
        Thread r1 = new Thread(()->print("A",A,B));
        Thread r2 = new Thread(()->print("B",B,C));
        Thread r3 = new Thread(()->print("C",C,A));

        r1.start();
        r2.start();
        r3.start();
        latch.await();
        boolean b = LOCK.tryLock();
        if(b){
            try {
                A.signal();
            }finally {
                LOCK.unlock();
            }

        }
        r1.join();
        r2.join();
        r3.join();


    }

    public void print(String x, Condition x1, Condition x2){
        latch.countDown();
        while(count < 31){
            boolean b = LOCK.tryLock();
            if(b) {
                try {
                    x1.await();
                    System.out.print(x);
                    count++;
                    x2.signal();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } finally {
                    LOCK.unlock();
                }
            }
        }
    }


}
