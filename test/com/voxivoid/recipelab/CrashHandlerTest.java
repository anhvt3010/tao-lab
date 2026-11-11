package com.voxivoid.recipelab;

import java.io.File;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class CrashHandlerTest {
    @Test void eventWorkerWritesWithoutTheCallerDoingFileIo(@TempDir File root) throws Exception {
        File dir = CardLog.dir(root);
        DiagnosticLog.record(dir, CardLog.FAIL, "background probe");
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(3);
        String text;
        do {
            text = CardLog.tail(null, new File(dir, CardLog.LOG), 8192);
            if (text.contains("background probe")) break;
            Thread.sleep(10);
        } while (System.nanoTime() < deadline);
        assertTrue(text.contains("background probe"));
        DiagnosticLog.record(null, CardLog.FAIL, "no card");
    }

    @Test void loggingFailureStillReachesThePlatformHandler() {
        final Throwable error = new IllegalStateException("probe");
        final int[] delegated = { 0 };
        CrashHandler handler = new CrashHandler((thread, thrown) -> {
            assertSame(error, thrown); assertSame(Thread.currentThread(), thread); delegated[0]++;
        }, (thread, thrown) -> { throw new OutOfMemoryError("logger failed"); });
        handler.uncaughtException(Thread.currentThread(), error);
        assertEquals(1, delegated[0]);
    }

    @Test void installingAgainDoesNotChainHandlers() {
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        try {
            CrashHandler.install((thread, error) -> {});
            Thread.UncaughtExceptionHandler first = Thread.getDefaultUncaughtExceptionHandler();
            CrashHandler.install((thread, error) -> fail("must not replace the original sink"));
            assertSame(first, Thread.getDefaultUncaughtExceptionHandler());
        } finally { Thread.setDefaultUncaughtExceptionHandler(previous); }
    }

    @Test void fatalRecordSurvivesAndIsIncludedInTheReport(@TempDir File root) {
        File dir = CardLog.dir(root);
        DiagnosticLog.crash(dir, Thread.currentThread(), new IllegalStateException("test crash"));
        String report = CardLog.report(dir, "now", "camera", "app", "state", null);
        assertTrue(report.contains("===== CRASH ====="));
        assertTrue(report.contains("test crash"));
        DiagnosticLog.crash(null, Thread.currentThread(), new RuntimeException("no card"));
    }
}
