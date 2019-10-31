package redstoneparadox.paradoxconfig.config

import redstoneparadox.paradoxconfig.serialization.ConfigDeserializer
import redstoneparadox.paradoxconfig.serialization.ConfigSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KProperty

open class ConfigOption<T: Any>(val type: KClass<T>, var value: T, val key: String, val comment: String) {

    open operator fun getValue(thisRef : Any?, property: KProperty<*>): T {
        return value
    }

    open operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    internal fun serialize(serializer: ConfigSerializer) {
        serializer.writeOption(key, value, comment)
    }

    internal fun deserialize(deserializer: ConfigDeserializer) {
        val newVal = deserializer.readOption(key)
        if (newVal != null && type.isInstance(newVal)) {
            value = newVal as T
        }
    }
}