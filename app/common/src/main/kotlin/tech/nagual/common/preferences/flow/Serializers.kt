package tech.nagual.common.preferences.flow

interface Serializer<T : Any> {
    fun deserialize(serialized: String): T
    fun serialize(value: T): String
}

interface NullableSerializer<T> {
    fun deserialize(serialized: String?): T?
    fun serialize(value: T?): String?
}
