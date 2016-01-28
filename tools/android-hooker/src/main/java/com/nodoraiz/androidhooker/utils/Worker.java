package com.nodoraiz.androidhooker.utils;

import java.util.concurrent.*;

public class Worker {

    private static ExecutorService executorService;

    private Worker(){}

    /**
     * Run a callable in a separate thread if there isn't another callable running
     *
     * @param callable Callable to be executed in a separated thread
     * @param secondsForTimeout Number of seconds before timeout when running the callable
     * @return
     */
    public static boolean work(Callable<Void> callable, int secondsForTimeout){

        if(Worker.executorService != null && !Worker.executorService.isTerminated()){
            return false;
        }

        Worker.executorService = Executors.newSingleThreadExecutor();
        final Future task = Worker.executorService.submit(callable);
        Worker.executorService.shutdown();

        // Cancel task
        ScheduledExecutorService stopExecutorService = Executors.newSingleThreadScheduledExecutor();
        stopExecutorService.schedule(new Runnable() {
            public void run() {
                if(!task.isDone() && !task.isCancelled()) {
                    task.cancel(true);
                }
            }
        }, secondsForTimeout, TimeUnit.SECONDS);

        return true;
    }

}
