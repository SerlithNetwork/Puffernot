package gg.pufferfish.pufferfish.util;

import com.google.common.collect.Queues;
import gg.pufferfish.pufferfish.PufferfishLogger;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import java.util.Queue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

@NullMarked
public class AsyncExecutor implements Runnable {

    private final Queue<Runnable> jobs = Queues.newArrayDeque();
    private final Lock mutex = new ReentrantLock();
    private final Condition cond = mutex.newCondition();
    private final Thread thread;
    private volatile boolean killswitch = false;

    public AsyncExecutor(String threadName) {
        this.thread = new Thread(this, threadName);
    }

    public void start() {
        this.thread.start();
    }

    public void kill() {
        this.killswitch = true;
        this.cond.signalAll();
    }

    public void submit(Runnable runnable) {
        this.mutex.lock();
        try {
            this.jobs.offer(runnable);
            this.cond.signalAll();
        } finally {
            this.mutex.unlock();
        }
    }

    @Override
    public void run() {
        while (!this.killswitch) {
            try {
                Runnable runnable = this.takeRunnable();
                if (runnable != null) {
                    runnable.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                PufferfishLogger.LOGGER.log(Level.SEVERE, e, () -> "Failed to execute async mob spawning job for thread " + thread.getName());
            }
        }
    }

    private @Nullable Runnable takeRunnable() throws InterruptedException {
        this.mutex.lock();
        try {
            while (this.jobs.isEmpty() && !this.killswitch) {
                this.cond.await();
            }

            if (this.jobs.isEmpty()) return null; // We've set killswitch

            return this.jobs.remove();
        } finally {
            this.mutex.unlock();
        }
    }

    public int getJobs() {
        return this.jobs.size();
    }

}
