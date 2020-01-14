package info.fmro.client.threads;

import info.fmro.client.objects.Statics;
import info.fmro.shared.stream.objects.PoisonPill;
import info.fmro.shared.stream.objects.StreamObjectInterface;
import info.fmro.shared.utility.Generic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.LinkedBlockingQueue;

public class SSLClientThread
        extends Thread {
    private static final Logger logger = LoggerFactory.getLogger(SSLClientThread.class);
    @SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
    public final LinkedBlockingQueue<StreamObjectInterface> sendQueue = new LinkedBlockingQueue<>();
    private SSLSocket socket;

    public synchronized void closeSocket() {
        logger.info("closing SSLClientThread socket");
        Generic.closeObjects(this.socket);
    }

    @SuppressWarnings("OverlyNestedMethod")
    @Override
    public void run() {
        final KeyManager[] keyManagers = Generic.getKeyManagers(Statics.KEY_STORE_FILE_NAME, Statics.KEY_STORE_PASSWORD, Statics.KEY_STORE_TYPE);
        SSLContext sSLContext = null;
        try {
            sSLContext = SSLContext.getInstance("TLS");
            sSLContext.init(keyManagers, null, new SecureRandom());
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            logger.error("SSLContext exception in InterfaceServer", e);
        }

        final SSLSocketFactory socketFactory = sSLContext != null ? sSLContext.getSocketFactory() : null;
        if (socketFactory != null) {
            do {
                SSLWriterThread writerThread = null;
                ObjectInputStream objectInputStream = null;
                try {
                    this.socket = (SSLSocket) socketFactory.createSocket(Statics.SERVER_ADDRESS, Statics.SERVER_PORT);
                    // todo ssl auth check test

                    if (this.socket != null) {
                        writerThread = new SSLWriterThread(this.socket, this.sendQueue);
                        writerThread.start();
                        //noinspection resource,IOResourceOpenedButNotSafelyClosed
                        objectInputStream = new ObjectInputStream(this.socket.getInputStream());

                        Object receivedObject;
                        do {
                            receivedObject = objectInputStream.readObject();
                            if (receivedObject instanceof StreamObjectInterface) {
                                final StreamObjectInterface receivedCommand = (StreamObjectInterface) receivedObject;
                                receivedCommand.runAfterReceive();
                            } else if (receivedObject == null) { // nothing to be done, will reach end of loop and exit loop
                            } else {
                                logger.error("unknown type of object in interfaceConnection stream: {} {}", receivedObject.getClass(), Generic.objectToString(receivedObject));
                            }
                        } while (receivedObject != null && !Statics.mustStop.get() && !writerThread.finished.get());
                    } else {
                        logger.error("STRANGE socket null in SSLClientThread thread, timeStamp={}", System.currentTimeMillis());
                    }
                } catch (@SuppressWarnings("OverlyBroadCatchBlock") IOException e) {
                    logger.error("IOException in SSLClientThread, will retry", e);
                } catch (ClassNotFoundException e) {
                    logger.error("STRANGE ClassNotFoundException in SSLClientThread, will retry", e);
                } finally {
                    //noinspection ConstantConditions
                    Generic.closeObjects(objectInputStream, this.socket);
                    if (writerThread != null) {
                        writerThread.finished.set(true);
                    } else { // writerThread is null, nothing to be done about it
                    }
                }

                if (Statics.mustStop.get()) { // no need for anti throttle, nothing to be done
                } else {
                    Generic.threadSleep(1_000L);
                }

                if (writerThread != null && writerThread.isAlive()) {
                    this.sendQueue.add(new PoisonPill());
                    try {
                        writerThread.join();
                    } catch (InterruptedException e) {
                        logger.error("InterruptedException in sslClientThread end", e);
                    }
                }
            } while (!Statics.mustStop.get());
        } else {
            logger.error("STRANGE socketFactory null in SSLClientThread thread, timeStamp={}", System.currentTimeMillis());
            Statics.mustStop.set(true);
        }

        logger.info("SSLClientThread ends");
    }
}
