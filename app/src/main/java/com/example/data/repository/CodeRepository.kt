package com.example.data.repository

import com.example.data.database.*
import kotlinx.coroutines.flow.Flow

class CodeRepository(
    private val fileDao: FileDao,
    private val sshDao: SshDao,
    private val extensionDao: ExtensionDao
) {
    val allFiles: Flow<List<LocalFile>> = fileDao.getAllFilesFlow()
    val allSshConnections: Flow<List<SshConnection>> = sshDao.getAllSshFlow()
    val allExtensions: Flow<List<MarketplaceExtension>> = extensionDao.getAllExtensionsFlow()

    suspend fun getFileByPath(path: String): LocalFile? = fileDao.getFileByPath(path)
    suspend fun getFileById(id: Long): LocalFile? = fileDao.getFileById(id)

    suspend fun insertFile(file: LocalFile): Long = fileDao.insertFile(file)
    suspend fun updateFile(file: LocalFile) = fileDao.updateFile(file)
    suspend fun deleteFile(file: LocalFile) = fileDao.deleteFile(file)
    suspend fun deleteDirectoryAndChildren(dirPath: String) = fileDao.deleteDirectoryAndChildren(dirPath)

    suspend fun insertSsh(conn: SshConnection): Long = sshDao.insertSsh(conn)
    suspend fun deleteSsh(conn: SshConnection) = sshDao.deleteSsh(conn)

    suspend fun updateExtensionState(id: String, installed: Boolean) = extensionDao.updateInstalledState(id, installed)
    suspend fun insertExtension(ext: MarketplaceExtension) = extensionDao.insertExtension(ext)

    suspend fun checkDatabaseInitialized(): Boolean {
        return fileDao.getCount() > 0
    }
}
