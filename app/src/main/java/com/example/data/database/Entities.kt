package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "local_files")
data class LocalFile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val path: String, // Absolute virtual path, e.g. "/project/index.html"
    val content: String,
    val isDirectory: Boolean,
    val language: String, // e.g. "html", "kotlin", "javascript", "css", "json", "txt"
    val lastModified: Long = System.currentTimeMillis()
)

@Entity(tableName = "ssh_connections")
data class SshConnection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val host: String,
    val port: Int = 22,
    val username: String,
    val rsaKey: String = "",
    val name: String,
    val lastConnected: Long = System.currentTimeMillis()
)

@Entity(tableName = "marketplace_extensions")
data class MarketplaceExtension(
    @PrimaryKey val id: String, // e.g. "kotlin-support"
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val isInstalled: Boolean = false,
    val iconName: String, // e.g. "code", "terminal", "bugs"
    val category: String, // "Linter", "Themes", "Snippets", "Language"
    val downloads: String = "1.2K"
)
