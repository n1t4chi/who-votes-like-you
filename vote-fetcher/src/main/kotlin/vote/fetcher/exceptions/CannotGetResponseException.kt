package vote.fetcher.exceptions

import java.io.IOException

class CannotGetResponseException : RuntimeException {
    constructor(s: String) : super(s) {}
    constructor(e: Exception) : super(e) {}
    constructor(s: String, e: IOException) : super(s, e) {}
}