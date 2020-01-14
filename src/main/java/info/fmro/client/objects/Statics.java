package info.fmro.client.objects;

import info.fmro.client.threads.SSLClientThread;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("UtilityClass")
public final class Statics {
    public static final int SERVER_PORT = 4375;
    public static final long EXECUTOR_KEEP_ALIVE = 10_000L, DELAY_PRINT_AVERAGES = 20_000L, PROGRAM_START_TIME = System.currentTimeMillis();
    public static final String KEY_STORE_TYPE = "pkcs12", PROJECT_PREFIX = "info.fmro", SERVER_ADDRESS = "fmro.info";
    public static final String STDOUT_FILE_NAME = "out.txt", STDERR_FILE_NAME = "err.txt", KEY_STORE_FILE_NAME = "input/client.p12", KEY_STORE_PASSWORD = "", LOGS_FOLDER_NAME = "logs", DATA_FOLDER_NAME = "data";
    public static final boolean closeStandardStreamsNotInitialized = false; // modified by reflection for tests; can't initialize, as that inlines the value and it can no longer be modified; no longer modified in tests, now I initialize it
    @SuppressWarnings({"StaticVariableMayNotBeInitialized", "PublicStaticCollectionField", "StaticNonFinalField"})
    public static ArrayList<? extends OutputStream> standardStreamsList;
    public static final AtomicBoolean mustStop = new AtomicBoolean(), mustWriteObjects = new AtomicBoolean(), programIsRunningMultiThreaded = new AtomicBoolean();
    public static final AtomicLong timeLastSaveToDisk = new AtomicLong();
    public static final TimeStamps timeStamps = new TimeStamps();
    @SuppressWarnings("PublicStaticCollectionField")
    public static final LinkedBlockingQueue<Runnable> linkedBlockingQueue = new LinkedBlockingQueue<>();
    public static final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(64, 64, EXECUTOR_KEEP_ALIVE, TimeUnit.MILLISECONDS, linkedBlockingQueue);
    public static final SSLClientThread sslClientThread = new SSLClientThread();

    private Statics() {
    }

    static {
        threadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    private static final Map<String, String> privateObjectFileNamesMap = new LinkedHashMap<>(32);

    static { // map used in two methods in VarsIO class
        privateObjectFileNamesMap.put("timeStamps", Statics.DATA_FOLDER_NAME + "/timeStamps.txt");
    }

    public static final Map<String, String> objectFileNamesMap = Collections.unmodifiableMap(privateObjectFileNamesMap);
}
