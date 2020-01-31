package info.fmro.client.main;

import javafx.scene.control.TreeItem;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class GUITest {
    @Test
    void testTreeItemEquals() {
        final TreeItem<String> item1 = new TreeItem<>("blah"), item2 = new TreeItem<>("blah");
        assertNotEquals(item1, item2, "1");
        //noinspection SimplifiableJUnitAssertion
        assertFalse(Objects.equals(item1, item2), "2");
    }
}
