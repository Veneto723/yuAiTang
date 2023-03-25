package com.yuAiTang.moxa.util;


import java.util.concurrent.*;

public class ThreadPool {

    private static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newScheduledThreadPool(8);
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadExecutor();

    public static void getInfo(){
        System.out.println(SCHEDULED_EXECUTOR_SERVICE.toString());
        System.out.println(EXECUTOR_SERVICE.toString());
    }

    /**
     * 开辟单独线程给需要阻塞的线程任务
     * <p>线程会一直等待直到运行完毕</p>
     * @param run 线程Runnable
     * @return 线程运行结果
     * @throws ExecutionException 如果在运行{@code run}时，线程内部报错，就会通过这个Exception抛出来
     * @throws InterruptedException 这个懂的都懂，线程被打断了
     */
    public static Object singleBlockedThread(Runnable run) throws ExecutionException, InterruptedException {
        return EXECUTOR_SERVICE.submit(run).get();
    }

    /**
     * 开辟单独线程给需要阻塞的线程任务
     * <p>线程会至多等待{@code timeout}毫秒</p>
     * @param run 线程Runnable
     * @param timeout 至多等待时间（毫秒）
     * @return 线程运行结果
     * @throws ExecutionException 如果在运行{@code run}时，线程内部报错，就会通过这个Exception抛出来
     * @throws InterruptedException 这个懂的都懂，线程被打断了
     * @throws TimeoutException {@code timeout}毫秒的等待时间到了后，线程依然未结束=>就会报这个错
     */
    public static Object singleBlockedThread(Runnable run, long timeout) throws ExecutionException, InterruptedException, TimeoutException {
        return EXECUTOR_SERVICE.submit(run).get(timeout, TimeUnit.MILLISECONDS);
    }

    /**
     * 运行一次非阻塞的线程
     * <p>
     *     所有非阻塞线程，除非后续对返回的{@code Future}使用get()方法。否则
     *     是不会主动抛出ExecutionException用表明进程运行中出现异常。所以，
     *     需要在{@code run}中try-catch好所有可能出现的异常。
     * </p>
     * @param run 线程Runnable
     * @param initialDelay {@code initialDelay}毫秒后运行{@code run}线程
     * @return 线程的Future返回
     * @see Future
     * @see ExecutionException
     */
    public static Future<?> schedule(Runnable run, long initialDelay) {
        //  本质上schedule===submit，都只执行1次=>所以用哪个都行
        return SCHEDULED_EXECUTOR_SERVICE.schedule(run, initialDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * 运行一次非阻塞的线程
     * <p>
     *     所有非阻塞线程，除非后续对返回的{@code Future}使用get()方法。否则
     *     是不会主动抛出ExecutionException用于表明进程运行中出现异常。所以，
     *     需要在{@code run}中try-catch好所有可能出现的异常
     * </p>
     * @param call 线程Callable
     * @param initialDelay {@code initialDelay}毫秒后运行{@code call}线程
     * @return 线程的Future返回
     * @see Future
     * @see ExecutionException
     */
    public static Future<?> schedule(Callable<?> call, long initialDelay) {
        return SCHEDULED_EXECUTOR_SERVICE.schedule(call, initialDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * 以每{@code rate}毫秒试图运行一次的频率运行非阻塞线程{@code run}
     * <p>
     *     所有非阻塞线程，除非后续对返回的{@code Future}使用get()方法。否则
     *     是不会主动抛出ExecutionException用于表明进程运行中出现异常。所以，
     *     需要在{@code run}中try-catch好所有可能出现的异常。如果在schedule
     *     的时候，出现了未被捕获到的异常。线程将无法循环执行。并不是这一次执行异常
     *     等到下一个时间到了，就执行下一次。
     * </p>
     * @param run 线程Runnable
     * @param initialDelay 在{@code initialDelay}毫秒后，开始循环运行
     * @param rate 开始运行第n次线程和开始运行第n+1次线程的时间间隔毫秒数
     * @return 线程的ScheduledFuture返回
     * @see ScheduledFuture
     * @see ExecutionException
     */
    public static ScheduledFuture<?> scheduleAtRate(Runnable run, long initialDelay, long rate){
        return SCHEDULED_EXECUTOR_SERVICE.scheduleAtFixedRate(run, initialDelay, rate, TimeUnit.MILLISECONDS);
    }

    /**
     * 以{@code delay}毫秒为间隔的频率运行非阻塞线程{@code run}
     * <p>
     *     所有非阻塞线程，除非后续对返回的{@code Future}使用get()方法。否则
     *     是不会主动抛出ExecutionException用于表明进程运行中出现异常。所以，
     *     需要在{@code run}中try-catch好所有可能出现的异常。如果在schedule
     *     的时候，出现了未被捕获到的异常。线程将无法循环执行。并不是这一次执行异常
     *     等到下一个时间到了，就执行下一次。
     * </p>
     * @param run 线程Runnable
     * @param initialDelay 在{@code initialDelay}毫秒后，开始循环运行
     * @param delay 结束运行第n次线程和开始运行第n+1线程的时间间隔毫秒数
     * @return 线程的ScheduledFuture返回
     * @see ScheduledFuture
     * @see ExecutionException
     */
    public static ScheduledFuture<?> scheduleWithDelay(Runnable run, long initialDelay, long delay){
        return SCHEDULED_EXECUTOR_SERVICE.scheduleWithFixedDelay(run, initialDelay, delay, TimeUnit.MILLISECONDS);
    }

    /**
     * 结束所有运行中的线程并关闭线程池
     * <p>
     *     使用shutdownNow()而不是shutdown()的原因是
     *     shutdown()会允许正在执行的线程继续执行
     * </p>
     */
    public static void shutdown(){
        if(!SCHEDULED_EXECUTOR_SERVICE.isTerminated()) SCHEDULED_EXECUTOR_SERVICE.shutdownNow();
        if(!EXECUTOR_SERVICE.isTerminated()) EXECUTOR_SERVICE.shutdownNow();
    }

    /**
     * 等待线程池中的所有线程运行结束至多{@code timeout}毫秒，然后结束所有运行中的线程并关闭线程池
     * @param timeout 至多等待线程运行毫秒数
     * @throws InterruptedException 你选的safeShutdown，线程被打断自己解决去
     */
    public static void safeShutdown(long timeout) throws InterruptedException {
        if(!SCHEDULED_EXECUTOR_SERVICE.isTerminated()) SCHEDULED_EXECUTOR_SERVICE.awaitTermination(timeout, TimeUnit.MILLISECONDS);
        if(!EXECUTOR_SERVICE.isTerminated()) EXECUTOR_SERVICE.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    }

    @Override
    public String toString() {
        return "EXECUTOR_SERVICE: " + EXECUTOR_SERVICE.toString() +
                "\nSCHEDULED_EXECUTOR_SERVICE: " + SCHEDULED_EXECUTOR_SERVICE.toString();
    }

}
