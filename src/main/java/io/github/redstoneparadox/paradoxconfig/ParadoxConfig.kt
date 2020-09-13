package io.github.redstoneparadox.paradoxconfig

import net.fabricmc.loader.api.FabricLoader
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint
import net.minecraft.util.Identifier
import org.apache.logging.log4j.LogManager
import io.github.redstoneparadox.paradoxconfig.ParadoxConfig.MODID
import io.github.redstoneparadox.paradoxconfig.config.RootConfigCategory
import java.io.File
import java.io.FileNotFoundException

object ParadoxConfig: PreLaunchEntrypoint {
    const val MODID: String = "pconfig"

    internal var initialized: Boolean = false
    internal val CONFIGS: HashMap<String, RootConfigCategory> = hashMapOf()

    @Suppress("unused")
    override fun onPreLaunch() {
        initConfigs()
    }

    /**
     * Reloads the specified config file.
     *
     * @param id The identifier (mod id + path) of the config.
     */
    fun forceReloadConfig(id: Identifier) {
        val config = CONFIGS[id.toString()]
        if (config != null) {
            loadConfig(config, id.namespace)
        }
    }

    internal fun getConfigData(): Collection<ConfigData> {
        val data = arrayListOf<ConfigData>();

        for (mod in FabricLoader.getInstance().allMods) {
            if (mod.metadata.containsCustomValue(MODID)) {
                val classNames = mod.metadata.getCustomValue(MODID).asArray

                val configNames = arrayListOf<String>()
                for (className in classNames) {
                    configNames.add(className.asString)
                }

                if (mod.metadata.id == MODID && !FabricLoader.getInstance().isDevelopmentEnvironment) continue

                data.add(ConfigData(configNames, mod.metadata.id))
            }
        }

        return data
    }

    internal fun initConfigs() {
        if (initialized) return

        val configData = getConfigData()
        val baseClass = RootConfigCategory::class

        for (data in configData) {
            for (configName in data.configNames) {
                try {
                    val config = Class.forName(configName).kotlin.objectInstance

                    if (config is RootConfigCategory) {
                        config.init()
                        CONFIGS["${data.modid}:${config.file}"] = config
                        loadConfig(config, data.modid)
                    }
                    else {
                        println("Object $configName either doesn't extend ${baseClass.simpleName} or is not an object.")
                    }
                } catch (e: ClassNotFoundException) {
                    e.printStackTrace()
                }
            }
        }

        initialized = true
    }

    private fun loadConfig(config: RootConfigCategory, modid: String) {
        val serializer = config.serializer
        val deserializer = config.deserializer
        val configFile = File(FabricLoader.getInstance().configDirectory, "${modid}/${config.file}")

        try {
            val configString = configFile.readText()
            if (deserializer.receiveSource(configString)) config.deserialize(deserializer)
        } catch (e: FileNotFoundException) {
            PConfigLogger.log("Config file $modid:${config.file} was not found; a new one will be created.")
            try {
                configFile.parentFile.mkdirs()
            } catch (e: SecurityException) {
                PConfigLogger.error("Could not create config file $modid:${config.file} due to security issues.")
                e.printStackTrace()
                return
            }
        }
        config.serialize(serializer)
        val configString = serializer.complete()
        configFile.writeText(configString)
    }
}

internal class ConfigData(val configNames: Collection<String>, val modid: String)

internal object PConfigLogger {
    private val LOGGER = LogManager.getFormatterLogger(MODID)
    private val MODNAME = "ParadoxConfig"

    internal fun log(msg: String) {
        LOGGER.info("[$MODNAME] $msg")
    }

    internal fun warn(msg: String) {
        LOGGER.warn("[$MODNAME] $msg")
    }

    internal fun error(msg: String) {
        LOGGER.warn("[$MODNAME] $msg")
    }
}

