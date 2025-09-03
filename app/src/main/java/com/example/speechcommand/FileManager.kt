package com.example.speechcommand

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class FileManager(private val context: Context) {

    private val commandsFileName = "speech_commands.txt"
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    /**
     * Save a command to the text file with timestamp
     */
    fun saveCommand(command: String): Boolean {
        return try {
            val file = File(context.filesDir, commandsFileName)
            val writer = FileWriter(file, true) // Append mode
            val timestamp = dateFormat.format(Date())
            writer.append("[$timestamp] $command\n")
            writer.close()
            Log.d("FileManager", "Command saved: $command")
            true
        } catch (e: IOException) {
            Log.e("FileManager", "Error saving command: $command", e)
            false
        }
    }

    /**
     * Read all commands from the file
     */
    fun readAllCommands(): List<String> {
        return try {
            val file = File(context.filesDir, commandsFileName)
            if (file.exists()) {
                file.readLines()
            } else {
                emptyList()
            }
        } catch (e: IOException) {
            Log.e("FileManager", "Error reading commands file", e)
            emptyList()
        }
    }

    /**
     * Get only the command part (without timestamp) from file lines
     */
    fun getCommandsOnly(): List<String> {
        return readAllCommands().mapNotNull { line ->
            try {
                // Extract command from "[timestamp] COMMAND" format
                val commandStart = line.indexOf("] ") + 2
                if (commandStart > 1 && commandStart < line.length) {
                    line.substring(commandStart).trim()
                } else null
            } catch (e: Exception) {
                null
            }
        }.filter { it.isNotEmpty() }
    }

    /**
     * Clear all commands from the file
     */
    fun clearCommands(): Boolean {
        return try {
            val file = File(context.filesDir, commandsFileName)
            if (file.exists()) {
                file.delete()
            }
            Log.d("FileManager", "Commands file cleared")
            true
        } catch (e: Exception) {
            Log.e("FileManager", "Error clearing commands file", e)
            false
        }
    }

    /**
     * Get the commands file path
     */
    fun getCommandsFilePath(): String {
        return File(context.filesDir, commandsFileName).absolutePath
    }

    /**
     * Check if commands file exists and has content
     */
    fun hasCommands(): Boolean {
        val file = File(context.filesDir, commandsFileName)
        return file.exists() && file.length() > 0
    }

    /**
     * Get recent commands (last N commands)
     */
    fun getRecentCommands(count: Int = 10): List<String> {
        return readAllCommands().takeLast(count)
    }

    /**
     * Export commands to external storage (if permission available)
     */
    fun exportCommands(): String? {
        return try {
            val file = File(context.filesDir, commandsFileName)
            if (file.exists()) {
                file.readText()
            } else {
                null
            }
        } catch (e: IOException) {
            Log.e("FileManager", "Error exporting commands", e)
            null
        }
    }

    /**
     * Get file statistics
     */
    fun getFileStats(): CommandFileStats {
        return try {
            val file = File(context.filesDir, commandsFileName)
            if (file.exists()) {
                val lines = file.readLines()
                CommandFileStats(
                    totalCommands = lines.size,
                    fileSize = file.length(),
                    lastModified = Date(file.lastModified()),
                    filePath = file.absolutePath
                )
            } else {
                CommandFileStats(0, 0, null, "File not found")
            }
        } catch (e: Exception) {
            Log.e("FileManager", "Error getting file stats", e)
            CommandFileStats(0, 0, null, "Error reading file")
        }
    }
}

data class CommandFileStats(
    val totalCommands: Int,
    val fileSize: Long,
    val lastModified: Date?,
    val filePath: String
)