package games.stendhal.server.entity.npc.condition;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

public class AdminConditionTest {

    @Test
    public void testConstructor() throws Throwable {
        AdminCondition adminCondition = new AdminCondition();
        assertEquals("adminCondition.hashCode()", 5000, adminCondition.hashCode());
    }

    @Test
    public void testConstructor1() throws Throwable {
        AdminCondition adminCondition = new AdminCondition(100);
        assertEquals("adminCondition.hashCode()", 100, adminCondition.hashCode());
    }
    @Test
    public void testEquals() throws Throwable {
        AdminCondition obj = new AdminCondition(100);
        assertTrue( obj.equals(obj));
        assertTrue(new AdminCondition().equals(new AdminCondition()));
        assertFalse(new AdminCondition(100).equals(new AdminCondition(1000)));
        assertFalse( new AdminCondition(100).equals("testString"));
        assertFalse(new AdminCondition(100).equals(null));
        assertFalse("subclass is not equal",new AdminCondition(100).equals(new AdminCondition(100){}));
    }
    @Test
    public void testFire() throws Throwable {
        assertTrue( new AdminCondition(0).fire(PlayerTestHelper.createPlayer(), "testAdminConditionText", SpeakerNPCTestHelper.createSpeakerNPC()));
        assertFalse(new AdminCondition().fire(PlayerTestHelper.createPlayer(), "testAdminConditionText", SpeakerNPCTestHelper.createSpeakerNPC()));

    }
    @Test
    public void testHashCode() throws Throwable {
        assertEquals("result", 0, new AdminCondition(0).hashCode());
        assertEquals("result", 100, new AdminCondition(100).hashCode());
    }
    @Test
    public void testToString() throws Throwable {
        assertEquals("result", "admin <100>", new AdminCondition(100).toString());
    }
    @Test
    public void testFireThrowsNullPointerException() throws Throwable {
        try {
            new AdminCondition(100).fire(null, "testAdminConditionText", SpeakerNPCTestHelper.createSpeakerNPC());
            fail("Expected NullPointerException to be thrown");
        } catch (NullPointerException ex) {
            assertNull("ex.getMessage()", ex.getMessage());
       }
    }
}

