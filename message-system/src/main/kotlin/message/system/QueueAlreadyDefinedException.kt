package message.system

data class QueueAlreadyDefinedException(val aClass: Class<*>) : RuntimeException("Queue for ${aClass.simpleName} was already defined.")
