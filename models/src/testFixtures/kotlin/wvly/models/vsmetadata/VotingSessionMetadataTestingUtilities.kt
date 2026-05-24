package wvly.models.vsmetadata

import io.kotest.equals.ReflectionIgnoringFieldsEquality
import io.kotest.matchers.collections.containExactly
import io.kotest.matchers.should
import java.util.*

val dummyDescriptionId = VotingSessionDescriptionId(UUID(0, 0))

infix fun <T : Any> List<T>.shouldMatch(other: List<T>) {
    this should containExactly(
        other,
        verifier = ReflectionIgnoringFieldsEquality(
            property = VotingSessionDescription::id,
            others = emptyArray(),
        ),
    )
}
