package vote.fetcher

import java.lang.RuntimeException

class CannotParseDocumentException(reason: String) : RuntimeException(reason)