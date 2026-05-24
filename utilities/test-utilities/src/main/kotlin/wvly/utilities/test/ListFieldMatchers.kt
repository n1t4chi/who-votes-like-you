package wvly.utilities.test

import io.kotest.assertions.print.print
import io.kotest.matchers.*
import io.kotest.matchers.collections.appendMissingAndExtra
import io.kotest.matchers.equality.*

infix fun <T : Any, C : Collection<T>> C.shouldContainExactlyInAnyOrderUsingFields(block: FieldEqualityConfig.() -> C): C {
    val config = FieldEqualityConfig()
    val other = block.invoke(config)
    this should containExactlyInAnyOrderUsingFields(other, config)
    return this
}

infix fun <T : Any, C : Collection<T>> C.shouldContainExactlyUsingFields(block: FieldEqualityConfig.() -> C): C {
    val config = FieldEqualityConfig()
    val other = block.invoke(config)
    this should containExactlyUsingFields(other, config)
    return this
}

fun <T : Any, C : Collection<T>> containExactlyInAnyOrderUsingFields(
    expected: C,
    config: FieldEqualityConfig,
): Matcher<C?> =
    neverNullMatcher { actual ->
        val missing = expected.filterNot { t ->
            actual.any { fieldEqual(it, t, config) }
        }
        val extra = actual.filterNot { t ->
            expected.any { fieldEqual(it, t, config) }
        }
        val passed = missing.isEmpty() && extra.isEmpty()

        val failureMessage = {
            buildString {
                append("Collection should contain ${expected.print().value} in any order (using fields), but was ${actual.print().value}")
                appendLine()
                appendMissingAndExtra(missing, extra)
            }
        }

        val negatedFailureMessage =
            { "Collection should not contain exactly ${expected.print().value} in any order (using fields)" }

        MatcherResult(
            passed,
            failureMessage,
            negatedFailureMessage,
        )
    }

fun <T : Any, C : Collection<T>> containExactlyUsingFields(
    expected: C,
    config: FieldEqualityConfig,
): Matcher<C?> =
    neverNullMatcher { actual ->
        val actualList = actual.toList()
        val expectedList = expected.toList()
        var index = 0
        while (index < actualList.size && index < expectedList.size) {
            if (!fieldEqual(actualList[index], expectedList[index], config)) {
                return@neverNullMatcher MatcherResult(
                    false,
                    {
                        "Collection should contain exactly: ${expected.print().value} but was: ${actual.print().value}\nElements differ at index $index"
                    },
                    { "Collection should not contain exactly: ${expected.print().value}" },
                )
            }
            index++
        }
        if (actualList.size != expectedList.size) {
            return@neverNullMatcher MatcherResult(
                false,
                { "Collection should contain exactly: ${expected.print().value} but was: ${actual.print().value}" },
                { "Collection should not contain exactly: ${expected.print().value}" },
            )
        }
        MatcherResult(true, { "" }, { "Collection should not contain exactly: ${expected.print().value}" })
    }

private fun <T : Any> fieldEqual(
    actual: T,
    expected: T,
    config: FieldEqualityConfig,
): Boolean =
    try {
        val result = compareUsingFields(actual, expected, config)
        result.errors.isEmpty()
    } catch (_: Throwable) {
        false
    }
