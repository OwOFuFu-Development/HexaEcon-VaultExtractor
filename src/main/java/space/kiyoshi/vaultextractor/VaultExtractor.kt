package space.kiyoshi.vaultextractor

import net.milkbowl.vault.economy.Economy
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.java.JavaPlugin
import space.kiyoshi.hexaecon.api.HexaEconAPI
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger


class VaultExtractor : JavaPlugin(), Listener {
    private var econ: Economy? = null
    private var folder = File(dataFolder, "extracted")
    private var file = File("$dataFolder/extracted/data.csv")

    override fun onEnable() {
        saveDefaultConfig()
        if (!setupEconomy() ) {
            logger.severe(String.format("Disabled due to no Vault dependency found!", description.name))
            return
        } else {
            logger.info("Successfully found Vault")
        }
        if(!(folder.exists())) {
            folder.mkdir()
        }
        if (!(file.exists())) {
            file.createNewFile()
        }
        server.pluginManager.registerEvents(this, this)
    }

    @EventHandler
    fun onJoin(event: PlayerJoinEvent) {
        val player = event.player
        val balance = getEconomy().getBalance(player)
        if(getEconomy().getBalance(player) > 0) {
            if(!(isExtractedPlayerInList(player.name))) {
                HexaEconAPI.deleteBankAccount(player)
                HexaEconAPI.createBankAccount(player, balance.toLong())
                getEconomy().withdrawPlayer(player, balance)
                addExtractedPlayer(player.name)
                log(LogRecord(Level.INFO, "[VaultExtractor] Successfully converted bank account for ${player.name} - ${balance.toLong()}"), "VaultExtractor")
            }
        }
    }

    private fun setupEconomy(): Boolean {
        if (server.pluginManager.getPlugin("Vault") == null) {
            return false
        }
        val rsp = server.servicesManager.getRegistration(
            Economy::class.java
        ) ?: return false
        econ = rsp.provider
        return econ != null
    }

    private fun getEconomy(): Economy {
        return econ!!
    }

    private fun addExtractedPlayer(playerName: String) {
        val writer = PrintWriter(FileWriter(file, true))
        writer.print(playerName)
        writer.print(",")
        writer.close()
    }

    private fun isExtractedPlayerInList(playerName: String): Boolean {
        val lines = file.readLines()
        for (line in lines) {
            val columns = line.split(",")
            if (columns.contains(playerName)) {
                return true
            }
        }
        return false
    }

    private fun colorize(level: Level, message: String): String {
        val color: String = when (level) {
            Level.SEVERE -> "\u001B[31m"
            Level.WARNING -> "\u001B[33m"
            Level.INFO -> "\u001B[1;34m"
            Level.CONFIG, Level.FINE, Level.FINER, Level.FINEST -> "\u001B[32m"
            else -> "\u001B[0m"
        }
        return "$color$message\u001B[0m"
    }

    private fun log(record: LogRecord?, name: String) {
        if (record != null && Logger.getLogger(name).isLoggable(record.level)) {
            val message: String = colorize(record.level, record.message)
            Bukkit.getConsoleSender().sendMessage(message)
        }
    }

}
