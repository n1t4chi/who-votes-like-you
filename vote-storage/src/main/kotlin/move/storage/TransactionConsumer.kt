package move.storage

fun interface TransactionConsumer {
    fun accept(transaction: DbTransaction)
}
