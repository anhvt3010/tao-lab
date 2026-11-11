package com.voxivoid.recipelab;

/** Process-wide handler whose sink must not capture an Activity. Always delegates, even if logging fails. */
final class CrashHandler implements Thread.UncaughtExceptionHandler {
    interface Sink { void write(Thread thread, Throwable error); }
    private static CrashHandler installed;
    private final Thread.UncaughtExceptionHandler previous;
    private final Sink sink;

    CrashHandler(Thread.UncaughtExceptionHandler previous, Sink sink) {
        this.previous = previous; this.sink = sink;
    }

    static synchronized void install(Sink sink) {
        if (installed != null) return;
        installed = new CrashHandler(Thread.getDefaultUncaughtExceptionHandler(), sink);
        Thread.setDefaultUncaughtExceptionHandler(installed);
    }

    public void uncaughtException(Thread thread, Throwable error) {
        try { sink.write(thread, error); } catch (Throwable ignored) {}
        finally {
            if (previous != null) previous.uncaughtException(thread, error);
            else System.exit(1);
        }
    }
}
