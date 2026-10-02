package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [LocalFile::class, SshConnection::class, MarketplaceExtension::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun fileDao(): FileDao
    abstract fun sshDao(): SshDao
    abstract fun extensionDao(): ExtensionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "devcode_editor_db"
                )
                .fallbackToDestructiveMigration()
                .addCallback(DatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateInitialData(database)
                }
            }
        }

        suspend fun populateInitialData(database: AppDatabase) {
            val fileDao = database.fileDao()
            val extDao = database.extensionDao()
            val sshDao = database.sshDao()

            // 1. Initial Extensions
            val defaultExtensions = listOf(
                MarketplaceExtension(
                    id = "kotlin-support",
                    name = "Kotlin Refactoring Support",
                    description = "Adds advanced syntax linting, auto-import, and formatting for Kotlin.",
                    author = "JetBrains Community",
                    version = "1.9.4",
                    isInstalled = true,
                    iconName = "kotlin",
                    category = "Language",
                    downloads = "4.2M"
                ),
                MarketplaceExtension(
                    id = "html-webview",
                    name = "Live HTML/CSS Web Previewer",
                    description = "Generates high fidelity rendered web canvases for active HTML, CSS, and JS documents.",
                    author = "WebDev Tools",
                    version = "2.1.0",
                    isInstalled = true,
                    iconName = "html",
                    category = "Language",
                    downloads = "1.8M"
                ),
                MarketplaceExtension(
                    id = "copilot-assistant",
                    name = "Gemini pairprogrammer AI",
                    description = "A powerful context-aware code completion assistant powered by Google Gemini.",
                    author = "Google AI Studio",
                    version = "1.0.0",
                    isInstalled = true,
                    iconName = "gemini",
                    category = "Language",
                    downloads = "950K"
                ),
                MarketplaceExtension(
                    id = "dracula-vscode",
                    name = "Dracula Dark Theme Palette",
                    description = "A gorgeous high-contrast dark theme optimized for coding in tablets and mobile widgets.",
                    author = "Dracula Org",
                    version = "3.2.1",
                    isInstalled = false,
                    iconName = "theme",
                    category = "Themes",
                    downloads = "550K"
                ),
                MarketplaceExtension(
                    id = "prettier-formatter",
                    name = "Prettier Code Formatter",
                    description = "An opinionated offline-capable code formatter that enforces clean indentation.",
                    author = "Prettier Team",
                    version = "3.0.3",
                    isInstalled = false,
                    iconName = "format",
                    category = "Linter",
                    downloads = "3.1M"
                ),
                MarketplaceExtension(
                    id = "bash-shell",
                    name = "SSH Interactive Shell Extension",
                    description = "Integrate secure shell capabilities with full terminal compatibility.",
                    author = "Linux Foundation",
                    version = "1.4.0",
                    isInstalled = false,
                    iconName = "terminal",
                    category = "Snippets",
                    downloads = "650K"
                )
            )
            extDao.insertExtensions(defaultExtensions)

            // 2. Initial Sample project files
            val files = listOf(
                LocalFile(
                    name = "project",
                    path = "/project",
                    content = "",
                    isDirectory = true,
                    language = ""
                ),
                LocalFile(
                    name = "index.html",
                    path = "/project/index.html",
                    content = """<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>My DevCode Profile Webpage</title>
    <style>
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background-color: #0f172a;
            color: #f8fafc;
            display: flex;
            justify-content: center;
            align-items: center;
            height: 100vh;
            margin: 0;
            overflow: hidden;
        }
        .container {
            text-align: center;
            background: linear-gradient(135deg, #1e1b4b, #0f172a);
            padding: 3rem;
            border-radius: 20px;
            box-shadow: 0 10px 30px rgba(0,0,0,0.5);
            border: 1px solid #3730a3;
            max-width: 500px;
        }
        h1 {
            background: linear-gradient(to right, #6366f1, #a855f7);
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
            font-size: 2.5rem;
            margin-bottom: 0.5rem;
        }
        p {
            color: #94a3b8;
            font-size: 1.1rem;
            line-height: 1.6;
        }
        .cta-btn {
            background: linear-gradient(to right, #4f46e5, #7c3aed);
            color: white;
            border: none;
            padding: 0.8rem 2rem;
            border-radius: 50px;
            font-size: 1rem;
            cursor: pointer;
            margin-top: 1.5rem;
            transition: transform 0.2s, box-shadow 0.2s;
            font-weight: bold;
        }
        .cta-btn:hover {
            transform: translateY(-2px);
            box-shadow: 0 4px 15px rgba(99, 102, 241, 0.4);
        }
    </style>
</head>
<body>
    <div class="container">
        <h1 id="title">AIDE Hello Website!</h1>
        <p>Built using the production-ready <b>DevCode Editor</b> for Android of VS Code standard. Tap below to see dynamic changes!</p>
        <button class="cta-btn" onclick="animateHeading()">Tap Me</button>
    </div>
    
    <script src="script.js"></script>
</body>
</html>
""".trimIndent(),
                    isDirectory = false,
                    language = "html"
                ),
                LocalFile(
                    name = "script.js",
                    path = "/project/script.js",
                    content = """// Dynamic interaction for HTML Web Canvas
function animateHeading() {
    const titleElement = document.getElementById("title");
    titleElement.style.transition = "transform 0.5s ease";
    titleElement.style.transform = "scale(1.15) rotate(2deg)";
    
    setTimeout(() => {
        titleElement.style.transform = "scale(1.0) rotate(0deg)";
    }, 500);

    // Dynamic color shifting demo
    const randomColors = ['#f43f5e', '#3b82f6', '#10b981', '#f59e0b', '#ec4899', '#a855f7'];
    const randomColor = randomColors[Math.floor(Math.random() * randomColors.length)];
    titleElement.style.background = `linear-gradient(to right, ${'$'}{randomColor}, #ffffff)`;
    titleElement.style.webkitBackgroundClip = "text";
    titleElement.style.webkitTextFillColor = "transparent";
}
""".trimIndent(),
                    isDirectory = false,
                    language = "javascript"
                ),
                LocalFile(
                    name = "styles.css",
                    path = "/project/styles.css",
                    content = """/* Additional Custom Stylesheets */
.container {
    animation: fadeIn 1.2s ease-in-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(20px); }
    to { opacity: 1; transform: translateY(0); }
}
""".trimIndent(),
                    isDirectory = false,
                    language = "css"
                ),
                LocalFile(
                    name = "readme.md",
                    path = "/project/readme.md",
                    content = """# DevCode IDE Project - Getting Started

This is your workspace in **DevCode**. You can write and compile web codes directly in the environment!

## Key Capabilities:
- **Real-time AI Gemini Pair Programmer**: Toggle the assistant tab on the left tab navigation to scan your codes, ask questions, explain components, or correct syntax warnings.
- **Visual split live previewer**: Tap the split-preview bar on top of an active *.html file to see web render changes side-by-side or toggled.
- **Interactive Terminal**: Type instructions inside the terminal (e.g. `ls`, `help`, `cat script.js`) for full developer agency.
- **SSH integration**: Connect remote boxes seamlessly using authentication credentials.

Let's begin scripting!
""".trimIndent(),
                    isDirectory = false,
                    language = "txt"
                )
            )

            for (file in files) {
                fileDao.insertFile(file)
            }

            // 3. Initial SSH profile
            sshDao.insertSsh(
                SshConnection(
                    name = "AI Studio Production Node",
                    host = "ssh.aistudio.com",
                    port = 22,
                    username = "devcode",
                    rsaKey = "RSA_KEY_MOCK_STUDIO_DEVCODE_KEY_2026_PRODUCTION"
                )
            )
        }
    }
}
