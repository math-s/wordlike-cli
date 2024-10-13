package wordlike

import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object Indexer {
    private val logger = KotlinLogging.logger {}

    private fun getNGram(
        word: String,
        n: Int,
    ): List<Pair<Int, String>> =
        if (word.length < n) {
            listOf(Pair(n, word.lowercase()))
        } else {
            word.windowed(n, 1).map { Pair(n, it) }
        }

    fun getSubsetsOfWord(word: String): List<Pair<Int, String>> = (1..4).flatMap { getNGram(word, it) }

    private fun createTables() {
        logger.info { "creating tables." }
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            registerLevenshteinFunction(conn)
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

    private fun insertWord(word: String): String {
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
            insertWord(line.lowercase())
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
