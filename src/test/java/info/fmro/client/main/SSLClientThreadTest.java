package info.fmro.client.main;

import info.fmro.shared.logic.ManagedRunner;
import info.fmro.shared.objects.Exposure;
import info.fmro.shared.stream.objects.MarketCatalogueInterface;
import info.fmro.shared.stream.objects.RunnerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SSLClientThreadTest {
    @Test
    void testRunAfterReceive() {
        final Object testObject = MarketCatalogueInterface.class;
        final Class<?> clazz = (Class<?>) testObject;
        //noinspection AssertEqualsMayBeAssertSame
        assertEquals(MarketCatalogueInterface.class, clazz, "1");

        assertNotEquals(testObject.getClass(), clazz, "2 - java.lang.Class vs MarketCatalogueInterface");

        assertEquals(testObject, clazz, "3");

        final Object managedRunner = new ManagedRunner("id", new RunnerId(1234L, 9876.123d));
        final Object exposureObjectClass = Exposure.class;
        final Class<?> exposureClass = (Class<?>) exposureObjectClass;
        assertTrue(exposureClass.isAssignableFrom(managedRunner.getClass()), "Exposure.class is assignable from its subclass ManagedRunner");
    }
}
