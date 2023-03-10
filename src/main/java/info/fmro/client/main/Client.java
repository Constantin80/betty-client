package info.fmro.client.main;

import info.fmro.client.objects.Statics;
import info.fmro.client.threads.MaintenanceThread;
import info.fmro.client.threads.TimeJumpDetectorThread;
import info.fmro.shared.enums.ProgramName;
import info.fmro.shared.objects.SharedStatics;
import info.fmro.shared.utility.CustomUncaughtExceptionHandler;
import info.fmro.shared.utility.Generic;
import javafx.application.Application;
import javafx.application.Platform;
import org.jetbrains.annotations.Contract;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UtilityClass")
final class Client {
    private static final Logger logger = LoggerFactory.getLogger(Client.class);

    @Contract(pure = true)
    private Client() {
    }

    @SuppressWarnings({"MethodWithMultipleReturnPoints", "OverlyLongMethod"})
    public static void main(final String[] args) {
        SharedStatics.programName.set(ProgramName.CLIENT);
        Statics.standardStreamsList = Generic.replaceStandardStreams(Statics.STDOUT_FILE_NAME, Statics.STDERR_FILE_NAME, Statics.LOGS_FOLDER_NAME, !Statics.closeStandardStreamsNotInitialized);

        FileOutputStream outFileOutputStream = null, errFileOutputStream = null;
        PrintStream outPrintStream = null, errPrintStream = null;

        Thread.setDefaultUncaughtExceptionHandler(new CustomUncaughtExceptionHandler());
        Generic.changeDefaultCharset(Generic.UTF8_CHARSET);

        try {
            outFileOutputStream = (FileOutputStream) Statics.standardStreamsList.get(0);
            outPrintStream = (PrintStream) Statics.standardStreamsList.get(1);
            errFileOutputStream = (FileOutputStream) Statics.standardStreamsList.get(2);
            errPrintStream = (PrintStream) Statics.standardStreamsList.get(3);

            //noinspection ResultOfMethodCallIgnored
            new File(Statics.DATA_FOLDER_NAME).mkdirs();

            for (final String arg : args) {
                if ("-help".equals(arg)) {
                    logger.info("Available options:\n  -help             prints this help screen\n");
                    return;
                } else {
                    logger.error("Bogus argument: {}", arg);
                }
            }

            Generic.disableHTTPSValidation();
            VarsIO.readObjectsFromFiles();

            if (Statics.programIsRunningMultiThreaded.getAndSet(true)) {
                logger.error("initial programMultithreaded state is true");
            }

            // threads only get started below this line
            new Thread(() -> Application.launch(GUI.class)).start(); // this needs to start before the sslClientThread
//            GUI.hasStarted.await();

            final MaintenanceThread maintenanceThread = new MaintenanceThread();
            maintenanceThread.start();
            final Thread timeJumpDetectorThread = new Thread(new TimeJumpDetectorThread());
            timeJumpDetectorThread.start();
            Statics.sslClientThread.start();

            while (!SharedStatics.mustStop.get()) {
                //noinspection NestedTryStatement
                try {

                    // this is where the work is placed
                    Generic.threadSleep(100);
                } catch (Throwable throwable) { // safety net inside the loop
                    logger.error("STRANGE ERROR inside Client loop", throwable);
                }
            } // end while

            if (Statics.sslClientThread.isAlive()) {
                Statics.sslClientThread.closeSocket();
                logger.debug("joining sslClientThread thread");
                Statics.sslClientThread.join();
            }
            if (timeJumpDetectorThread.isAlive()) {
                logger.debug("joining timeJumpDetectorThread");
                timeJumpDetectorThread.join();
            }
            if (maintenanceThread.isAlive()) {
                logger.debug("joining maintenance thread");
                maintenanceThread.join();
            }

            Statics.timer.cancel();
            SharedStatics.threadPoolExecutor.shutdown();
            SharedStatics.threadPoolExecutorImportant.shutdown();
            Statics.scheduledThreadPoolExecutor.shutdown();

            if (!SharedStatics.threadPoolExecutor.awaitTermination(10L, TimeUnit.MINUTES)) {
                logger.error("threadPoolExecutor hanged: {}", SharedStatics.threadPoolExecutor.getActiveCount());
                final List<Runnable> runnableList = SharedStatics.threadPoolExecutor.shutdownNow();
                if (!runnableList.isEmpty()) {
                    logger.error("threadPoolExecutor not commenced: {}", runnableList.size());
                }
            }
            if (!SharedStatics.threadPoolExecutorImportant.awaitTermination(10L, TimeUnit.MINUTES)) {
                logger.error("threadPoolExecutor hanged: {}", SharedStatics.threadPoolExecutorImportant.getActiveCount());
                final List<Runnable> runnableList = SharedStatics.threadPoolExecutorImportant.shutdownNow();
                if (!runnableList.isEmpty()) {
                    logger.error("threadPoolExecutor not commenced: {}", runnableList.size());
                }
            }
            if (!Statics.scheduledThreadPoolExecutor.awaitTermination(10L, TimeUnit.MINUTES)) {
                logger.error("scheduledThreadPoolExecutor hanged: {}", Statics.scheduledThreadPoolExecutor.getActiveCount());
                final List<Runnable> runnableList = Statics.scheduledThreadPoolExecutor.shutdownNow();
                if (!runnableList.isEmpty()) {
                    logger.error("scheduledThreadPoolExecutor not commenced: {}", runnableList.size());
                }
            }

            logger.debug("All threads finished");

            if (!Statics.programIsRunningMultiThreaded.getAndSet(false)) {
                logger.error("final programMultithreaded state is false");
            }

            VarsIO.writeObjectsToFiles();
            Platform.exit();
            SharedStatics.alreadyPrintedMap.clear(); // also prints the important properties
        } catch (NumberFormatException | InterruptedException exception) {
            logger.error("STRANGE ERROR inside Client", exception);
        } catch (Throwable throwable) { // attempts to catch fatal errors
            logger.error("EVEN STRANGER ERROR inside Client", throwable);
        } finally {
            logger.info("Program ends"); // after this point, streams are getting closed, so logging might no longer work

            Generic.closeStandardStreams();

            //noinspection ConstantConditions
            Generic.closeObjects(outPrintStream, outFileOutputStream, errPrintStream, errFileOutputStream);
        }
    }

    public static void clearCachedObjects() {

    }
}
