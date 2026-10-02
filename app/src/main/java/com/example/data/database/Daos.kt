package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {
    @Query("SELECT * FROM local_files ORDER BY path ASC")
    fun getAllFilesFlow(): Flow<List<LocalFile>>

    @Query("SELECT * FROM local_files WHERE path = :path LIMIT 1")
    suspend fun getFileByPath(path: String): LocalFile?

    @Query("SELECT * FROM local_files WHERE id = :id LIMIT 1")
    suspend fun getFileById(id: Long): LocalFile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: LocalFile): Long

    @Update
    suspend fun updateFile(file: LocalFile)

    @Delete
    suspend fun deleteFile(file: LocalFile)

    @Query("DELETE FROM local_files WHERE path LIKE :dirPath || '/%'")
    suspend fun deleteDirectoryAndChildren(dirPath: String)

    @Query("SELECT COUNT(*) FROM local_files")
    suspend fun getCount(): Int
}

@Dao
interface SshDao {
    @Query("SELECT * FROM ssh_connections ORDER BY lastConnected DESC")
    fun getAllSshFlow(): Flow<List<SshConnection>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSsh(conn: SshConnection): Long

    @Delete
    suspend fun deleteSsh(conn: SshConnection)
}

@Dao
interface ExtensionDao {
    @Query("SELECT * FROM marketplace_extensions")
    fun getAllExtensionsFlow(): Flow<List<MarketplaceExtension>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtension(ext: MarketplaceExtension)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtensions(exts: List<MarketplaceExtension>)

    @Query("UPDATE marketplace_extensions SET isInstalled = :installed WHERE id = :id")
    suspend fun updateInstalledState(id: String, installed: Boolean)

    @Query("SELECT COUNT(*) FROM marketplace_extensions")
    suspend fun getCount(): Int
}
