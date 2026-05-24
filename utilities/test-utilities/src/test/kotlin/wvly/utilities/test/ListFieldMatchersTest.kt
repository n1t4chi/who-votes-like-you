package wvly.utilities.test

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.equality.*
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

data class TestObj(
    val id: UUID,
    val text: String,
    val double: Double,
    val timestamp: Instant,
    val nested: TestNestedObj?,
) {
    companion object {
        fun from(index: Int) =
            TestObj(
                id = UUID(index.toLong(), index.toLong()),
                text = "test$index",
                double = index + 0.125,
                timestamp = Instant.ofEpochSecond(index.toLong()),
                nested = TestNestedObj(
                    id = UUID(index.toLong(), 0L),
                    integer = index,
                    float = index + 0.25f,
                    timestamp = Instant.ofEpochSecond(10 + index.toLong()),
                ),
            )
    }
}

data class TestNestedObj(
    val id: UUID,
    val integer: Int,
    val float: Float,
    val timestamp: Instant,
)

class ListFieldMatchersTest : BehaviorSpec({

    Given("two lists of VotingSessionobj with identical content in the same order") {
        val obj1 = TestObj.from(1)
        val obj2 = TestObj.from(2)
        val actual = listOf(obj1, obj2)
        val expected = listOf(obj1, obj2)

        When("comparing them with shouldContainExactlyUsingFields") {
            Then("the comparison should pass") {
                actual shouldContainExactlyUsingFields {
                    expected
                }
            }
        }

        When("comparing them with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should pass") {
                actual shouldContainExactlyInAnyOrderUsingFields {
                    expected
                }
            }
        }
    }

    Given("two lists of VotingSessionobj with identical content in different order") {
        val obj1 = TestObj.from(1)
        val obj2 = TestObj.from(2)
        val actual = listOf(obj2, obj1)
        val expected = listOf(obj1, obj2)

        When("comparing them with shouldContainExactlyUsingFields (ordered)") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain exactly: [$obj1, $obj2] but was: [$obj2, $obj1]" +
                    "\nElements differ at index 0"
            }
        }

        When("comparing them with containExactlyInAnyOrderUsingFields (unordered)") {
            Then("the comparison should pass") {
                actual shouldContainExactlyInAnyOrderUsingFields {
                    expected
                }
            }
        }
    }

    Given("two lists where actual is missing an element compared to expected") {
        val obj1 = TestObj.from(1)
        val obj2 = TestObj.from(2)
        val actual = listOf(obj1)
        val expected = listOf(obj1, obj2)

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain exactly: [$obj1, $obj2] but was: [$obj1]"
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain [$obj1, $obj2] in any order (using fields), but was [$obj1]" +
                    "\nSome elements were missing: [$obj2]"
            }
        }
    }

    Given("two lists where actual has an extra element compared to expected") {
        val obj1 = TestObj.from(1)
        val obj2 = TestObj.from(2)
        val actual = listOf(obj1, obj2)
        val expected = listOf(obj1)

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj1, $obj2]"
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj1, $obj2]" +
                    "\nSome elements were unexpected: [$obj2]"
            }
        }
    }

    Given("two lists where expected has an extra element compared to actual") {
        val obj1 = TestObj.from(1)
        val obj2 = TestObj.from(2)
        val actual = listOf(obj1)
        val expected = listOf(obj1, obj2)

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain exactly: [$obj1, $obj2] but was: [$obj1]"
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain [$obj1, $obj2] in any order (using fields), but was [$obj1]" +
                    "\nSome elements were missing: [$obj2]"
            }
        }
    }

    Given("two lists with both missing and extra elements compared to expected") {
        val obj1 = TestObj.from(1)
        val obj2 = TestObj.from(2)
        val obj3 = TestObj.from(3)
        val actual = listOf(obj1, obj3)
        val expected = listOf(obj1, obj2)

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain exactly: [$obj1, $obj2] but was: [$obj1, $obj3]" +
                    "\nElements differ at index 1"
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        expected
                    }
                }.message shouldBe "Collection should contain [$obj1, $obj2] in any order (using fields), but was [$obj1, $obj3]" +
                    "\nSome elements were missing: [$obj2] and some elements were unexpected: [$obj3]"
            }
        }
    }

    Given("two empty lists") {
        val expected: List<TestObj> = emptyList()
        val actual: List<TestObj> = emptyList()

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should pass") {
                actual shouldContainExactlyUsingFields { expected }
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should pass") {
                actual shouldContainExactlyInAnyOrderUsingFields { expected }
            }
        }
    }

    Given("an empty actual list and a non-empty expected list") {
        val obj1 = TestObj.from(1)
        val expected: List<TestObj> = listOf(obj1)
        val actual: List<TestObj> = emptyList()

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields { expected }
                }.message shouldBe "Collection should contain exactly: [$obj1] but was: []"
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyInAnyOrderUsingFields { expected }
                }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was []" +
                    "\nSome elements were missing: [$obj1]"
            }
        }
    }

    Given("an empty expected list and a non-empty actual list") {
        val obj1 = TestObj.from(1)
        val expected: List<TestObj> = emptyList()
        val actual: List<TestObj> = listOf(obj1)

        When("comparing with shouldContainExactlyUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyUsingFields { expected }
                }.message shouldBe "Collection should contain exactly: [] but was: [$obj1]"
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            Then("the comparison should fail") {
                shouldThrow<AssertionError> {
                    actual shouldContainExactlyInAnyOrderUsingFields { expected }
                }.message shouldBe "Collection should contain [] in any order (using fields), but was [$obj1]" +
                    "\nSome elements were unexpected: [$obj1]"
            }
        }
    }

    Given("two lists with single elements which have different values in text field") {
        val obj1 = TestObj.from(1)
        val obj2 = obj1.copy(text = "differentText")
        val expected: List<TestObj> = listOf(obj1)
        val actual: List<TestObj> = listOf(obj2)

        When("comparing with shouldContainExactlyUsingFields") {
            And("default matching config") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyUsingFields { expected }
                    }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj2]" +
                        "\nElements differ at index 0"
                }
            }
            And("excluded text field") {
                Then("the comparison should pass") {
                    actual shouldContainExactlyUsingFields {
                        excludedProperties = listOf(TestObj::text)
                        expected
                    }
                }
            }
            And("excluded different field") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyUsingFields { expected }
                    }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj2]" +
                        "\nElements differ at index 0"
                }
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            And("default matching config") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyInAnyOrderUsingFields { expected }
                    }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj2]" +
                        "\nSome elements were missing: [$obj1] and some elements were unexpected: [$obj2]"
                }
            }
            And("excluded text field") {
                Then("the comparison should pass") {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        excludedProperties = listOf(TestObj::text)
                        expected
                    }
                }
            }
            And("excluded different field") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyInAnyOrderUsingFields { expected }
                    }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj2]" +
                        "\nSome elements were missing: [$obj1] and some elements were unexpected: [$obj2]"
                }
            }
        }
    }

    Given("two lists with elements that have timestamp difference of 30 seconds") {
        val obj1 = TestObj.from(1)
        val obj2 = obj1.copy(timestamp = obj1.timestamp.plusSeconds(30))
        val expected: List<TestObj> = listOf(obj1)
        val actual: List<TestObj> = listOf(obj2)

        When("comparing with shouldContainExactlyUsingFields") {
            And("default matching config") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyUsingFields { expected }
                    }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj2]" +
                        "\nElements differ at index 0"
                }
            }
            And("matcher with 60s tolerance") {
                Then("the comparison should pass") {
                    actual shouldContainExactlyUsingFields {
                        overrideMatchers = mapOf(
                            TestObj::timestamp to matchInstantsWithTolerance(60.seconds),
                        )
                        expected
                    }
                }
            }
            And("matcher with 10s tolerance") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyUsingFields {
                            overrideMatchers = mapOf(
                                TestObj::timestamp to matchInstantsWithTolerance(10.seconds),
                            )
                            expected
                        }
                    }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj2]" +
                        "\nElements differ at index 0"
                }
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            And("default matching config") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyInAnyOrderUsingFields { expected }
                    }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj2]" +
                        "\nSome elements were missing: [$obj1] and some elements were unexpected: [$obj2]"
                }
            }
            And("matcher with 60s tolerance") {
                Then("the comparison should pass") {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        overrideMatchers = mapOf(
                            TestObj::timestamp to matchInstantsWithTolerance(60.seconds),
                        )
                        expected
                    }
                }
            }
            And("matcher with 10s tolerance") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyInAnyOrderUsingFields {
                            overrideMatchers = mapOf(
                                TestObj::timestamp to matchInstantsWithTolerance(10.seconds),
                            )
                            expected
                        }
                    }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj2]" +
                        "\nSome elements were missing: [$obj1] and some elements were unexpected: [$obj2]"
                }
            }
        }
    }

    Given("two lists with elements that have different ids at each level") {
        val obj1 = TestObj.from(1)
        val obj2 = obj1.copy(
            id = UUID(3, 1),
            nested = obj1.nested!!.copy(
                id = UUID(3, 2),
            ),
        )
        val expected: List<TestObj> = listOf(obj1)
        val actual: List<TestObj> = listOf(obj2)

        When("comparing with shouldContainExactlyUsingFields") {
            And("default matching config") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyUsingFields { expected }
                    }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj2]" +
                        "\nElements differ at index 0"
                }
            }
            And("matcher with all ids excluded") {
                Then("the comparison should pass") {
                    actual shouldContainExactlyUsingFields {
                        excludedProperties = listOf(
                            TestObj::id,
                            TestNestedObj::id,
                        )
                        expected
                    }
                }
            }
            And("matcher with only TestObj id excluded") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyUsingFields {
                            excludedProperties = listOf(
                                TestObj::id,
                            )
                            expected
                        }
                    }.message shouldBe "Collection should contain exactly: [$obj1] but was: [$obj2]" +
                        "\nElements differ at index 0"
                }
            }
        }

        When("comparing with containExactlyInAnyOrderUsingFields") {
            And("default matching config") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyInAnyOrderUsingFields { expected }
                    }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj2]" +
                        "\nSome elements were missing: [$obj1] and some elements were unexpected: [$obj2]"
                }
            }
            And("matcher with all ids excluded") {
                Then("the comparison should pass") {
                    actual shouldContainExactlyInAnyOrderUsingFields {
                        excludedProperties = listOf(
                            TestObj::id,
                            TestNestedObj::id,
                        )
                        expected
                    }
                }
            }
            And("matcher with only TestObj id excluded") {
                Then("the comparison should fail") {
                    shouldThrow<AssertionError> {
                        actual shouldContainExactlyInAnyOrderUsingFields {
                            excludedProperties = listOf(
                                TestObj::id,
                            )
                            expected
                        }
                    }.message shouldBe "Collection should contain [$obj1] in any order (using fields), but was [$obj2]" +
                        "\nSome elements were missing: [$obj1] and some elements were unexpected: [$obj2]"
                }
            }
        }
    }

    Given("complicated case of differences") {
        val obj1 = TestObj.from(1)
        val nestedObj1 = obj1.nested!!
        val obj2 = obj1.copy(
            id = UUID(3, 1),
            timestamp = obj1.timestamp.plusSeconds(30),
            nested = nestedObj1.copy(
                id = UUID(3, 2),
                timestamp = obj1.timestamp.plusSeconds(60),
            ),
        )
    }
})
