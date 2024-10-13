package wordlike

import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object Indexer {
    private val logger = KotlinLogging.logger {}

    fun getTrigrams(word: String): List<String> =
        if (word.length < 3) {
            listOf(word)
        } else {
            word.windowed(3, 1)
        }

    private fun createTables() {
        logger.info { "creating tables." }
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            conn.createStatement().use {
                it.execute("DROP TABLE IF EXISTS words")
                it.execute("CREATE TABLE IF NOT EXISTS words (word TEXT PRIMARY KEY)")
            }
            conn.createStatement().execute("PRAGMA foreign_keys = ON")
        }
        connection.close()
    }

    private fun getWordOrNull(word: String): String? {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val resultSet = conn.createStatement().executeQuery("SELECT word FROM words WHERE word = '$word'")
            return if (resultSet.next()) (resultSet.getString("word")) else null
        }
    }

    fun insertWord(word: String): String {
        getWordOrNull(word)?.let { return it }
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val preparedStatement = conn.prepareStatement("INSERT OR IGNORE INTO words (word) VALUES (?)")
            preparedStatement.setString(1, word)
            preparedStatement.execute()
            return word
        }
    }

    private fun loadWords(filePath: String) {
        logger.info { "loading words..." }
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        File(filePath).forEachLine { line ->
            line.toCharArray().forEach { if (!it.isLetter()) return@forEachLine }
            insertWord(line)
        }
        connection.close()
        logger.info { "words were loaded..." }
    }

    private fun countTotalWords(): Int {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val resultSet = conn.createStatement().executeQuery("SELECT COUNT(*) FROM words")
            return if (resultSet.next()) resultSet.getInt(1) else 0
        }
    }

    fun execute(filePath: String) {
        createTables()
        loadWords(filePath)
        logger.info { "total words: ${countTotalWords()}" }
    }
}
