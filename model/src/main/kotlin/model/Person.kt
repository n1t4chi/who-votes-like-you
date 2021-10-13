package model

data class Person(val name: String) {
    override fun toString(): String {
        return name
    }
}