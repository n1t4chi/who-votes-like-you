package message.system

import org.junit.jupiter.api.Assertions

object AssertionsExt {
    @JvmStatic
    fun assertEmpty(collection: Collection<*>, name: String) {
        Assertions.assertTrue(collection.isEmpty()) { "Expected $name to be empty but was:\n" + collection }
    }
    
    @JvmStatic
    fun assertUnorderedEquals(expected: Collection<*>, actual: Collection<*>) {
        Assertions.assertEquals(expected.toSet(), actual.toSet())
    }
}