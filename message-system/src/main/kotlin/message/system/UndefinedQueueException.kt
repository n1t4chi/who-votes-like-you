package message.system

data class UndefinedQueueException(val aClass: Class<*>) : RuntimeException("Queue for ${aClass.simpleName} was not defined.")
