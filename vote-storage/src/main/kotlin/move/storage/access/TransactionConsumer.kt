package move.storage.access

fun interface TransactionConsumer {
    fun accept(transaction: DbTransaction)
}
