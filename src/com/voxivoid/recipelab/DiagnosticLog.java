package com.voxivoid.recipelab;

import java.io.File;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/** Bounded background event logging. Fatal errors use a separate file without waiting for the event worker. */
final class DiagnosticLog {
    private DiagnosticLog() {}
    static final int MAX_MESSAGE = 4096, MAX_CRASH = 16 * 1024;

    private static final class Worker {
        static final ThreadPoolExecutor LOG = create();
        private static ThreadPoolExecutor create() {
            ThreadPoolExecutor pool = new ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(32), new ThreadFactory() {
                        public Thread newThread(Runnable r) {
                            Thread t = new Thread(r, "diagnostic-log"); t.setDaemon(true); return t;
                        }
                    }, new ThreadPoolExecutor.DiscardOldestPolicy());
            pool.allowCoreThreadTimeOut(true);
            return pool;
        }
    }

    static void record(final File dir, final String kind, String message) {
        if (dir == null) return;
        final String line = CardLog.line(kind, limit(message, MAX_MESSAGE));
        Worker.LOG.execute(new Runnable() {
            public void run() { try { CardLog.append(dir, CardLog.LOG, line); } catch (Throwable ignored) {} }
        });
    }

    static void crash(File dir, Thread thread, Throwable error) {
        if (dir == null) return;
        try {
            String text = CardLog.line(CardLog.CRASH, "thread " + thread.getName() + "\n" + limit(CardLog.stack(error), MAX_CRASH));
            CardLog.replace(dir, CardLog.CRASH_FILE, text);
        } catch (Throwable ignored) {} // the caller must still reach Android's crash handler
    }

    private static String limit(String text, int max) {
        String s = String.valueOf(text);
        return s.length() <= max ? s : s.substring(0, max) + "\n[truncated]";
    }
}
