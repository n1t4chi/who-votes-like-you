package vote.fetcher

import java.io.IOException
import java.lang.Exception
import java.lang.RuntimeException

class CannotGetResponseException : RuntimeException {
    constructor(s: String) : super(s) {}
    constructor(e: Exception) : super(e) {}
    constructor(s: String, e: IOException) : super(s, e) {}
}