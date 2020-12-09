package info.fmro.client.objects;

import info.fmro.client.threads.SSLClientThread;
import info.fmro.shared.entities.Event;
import info.fmro.shared.entities.MarketCatalogue;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressWarnings("UtilityClass")
public final class Statics {
    public static final int SERVER_PORT = 4375;
    public static final long DELAY_PRINT_AVERAGES = 20_000L;
    public static final String KEY_STORE_TYPE = "pkcs12", PROJECT_PREFIX = "info.fmro", SERVER_ADDRESS = "fmro.info";
    public static final String KEY_STORE_FILE_NAME = "input/client.p12", KEY_STORE_PASSWORD = "test12";
    public static final String STDOUT_FILE_NAME = "out.txt", STDERR_FILE_NAME = "err.txt", LOGS_FOLDER_NAME = "logs", DATA_FOLDER_NAME = "data";
    public static final boolean closeStandardStreamsNotInitialized = false; // modified by reflection for tests; can't initialize, as that inlines the value and it can no longer be modified; no longer modified in tests, now I initialize it
    @SuppressWarnings({"StaticVariableMayNotBeInitialized", "PublicStaticCollectionField", "StaticNonFinalField"})
    public static ArrayList<? extends OutputStream> standardStreamsList;
    public static final AtomicBoolean programIsRunningMultiThreaded = new AtomicBoolean();
    public static final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(64);
    public static final SSLClientThread sslClientThread = new SSLClientThread();
    public static final ClientRulesManager rulesManager = new ClientRulesManager();
    public static final ObservedExistingFunds existingFunds = new ObservedExistingFunds();
    //    public static final OrdersThreadInterface pendingOrdersThread = null; // not used for now
    public static final ClientStreamSynchronizedMap<String, MarketCatalogue> marketCataloguesMap = new ClientStreamSynchronizedMap<>(MarketCatalogue.class, 128); // <marketId, MarketCatalogue>
    public static final ClientStreamSynchronizedMap<String, Event> eventsMap = new ClientStreamSynchronizedMap<>(Event.class, 128); // <eventId, Event>
    public static final Timer timer = new Timer();
    public static final Set<String> unsupportedEventNames = Set.of("Set 01", "Set 02", "Set 03", "Set 04", "Set 05");
    public static final AtomicBoolean clientHadDisconnectionFromServer = new AtomicBoolean();

    private Statics() {
    }

    static {
        scheduledThreadPoolExecutor.allowCoreThreadTimeOut(true);
    }

    private static final Map<String, String> privateObjectFileNamesMap = new LinkedHashMap<>(32);

    static {
        privateObjectFileNamesMap.put("timeStamps", Statics.DATA_FOLDER_NAME + "/timeStamps.txt"); // map used in two methods in VarsIO class
    }

    public static final Map<String, String> objectFileNamesMap = Collections.unmodifiableMap(privateObjectFileNamesMap);
}
