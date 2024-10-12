package wordlike

import mu.KotlinLogging
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

object Indexer {
    private val logger = KotlinLogging.logger {}

    private fun getTrigrams(word: String): List<String> =
        if (word.length < 3) {
            listOf(word)
        } else {
            word.windowed(3, 1)
        }

    private fun createTables() {
        logger.info { "creating tables." }
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            conn.createStatement().execute("PRAGMA foreign_keys = OFF")
            conn.createStatement().use {
                it.execute("DROP TABLE IF EXISTS words")
                it.execute("DROP TABLE IF EXISTS trigrams")
                it.execute("DROP TABLE IF EXISTS trigrams_words")

                it.execute("CREATE TABLE IF NOT EXISTS words (id INTEGER PRIMARY KEY, word TEXT)")
                it.execute("CREATE INDEX IF NOT EXISTS idx_word ON words(word)")

                it.execute("CREATE TABLE IF NOT EXISTS trigrams (id INTEGER PRIMARY KEY, chars TEXT)")
                it.execute("CREATE INDEX IF NOT EXISTS idx_chars ON trigrams(chars)")
                it.execute(
                    "CREATE TABLE IF NOT EXISTS trigrams_words (id INTEGER PRIMARY KEY, trigram_id INTEGER, word_id INTEGER, count INTEGER DEFAULT 1, FOREIGN KEY(trigram_id) REFERENCES trigrams(id), FOREIGN KEY(word_id) REFERENCES words(id))",
                )
                it.execute("CREATE INDEX IF NOT EXISTS idx_trigram_id ON trigrams_words(trigram_id)")
                it.execute("CREATE INDEX IF NOT EXISTS idx_word_id ON trigrams_words(word_id)")
            }
            conn.createStatement().execute("PRAGMA foreign_keys = ON")
        }
        connection.close()
    }

    private fun populateTrigrams(
        word: String,
        wordId: Int,
    ) {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            try {
                getTrigrams(word).forEach { trigram ->
                    if (trigram.length < 3) return
                    val trigramId =
                        conn
                            .createStatement()
                            .executeQuery("SELECT id FROM trigrams WHERE chars = '$trigram'")
                            .let {
                                if (it.next()) {
                                    it.getInt("id")
                                } else {
                                    conn.createStatement().execute("INSERT INTO trigrams (chars) VALUES ('$trigram')")
                                    conn.createStatement().executeQuery("SELECT last_insert_rowid()").getInt(1)
                                }
                            }

                    conn.createStatement().execute(
                        "INSERT OR IGNORE INTO trigrams_words (trigram_id, word_id) VALUES ($trigramId, $wordId)",
                    )
                }
            } catch (e: Exception) {
                logger.error { "$word failed to load trigrams with error: ${e.message}" }
            }
        }
        connection.close()
    }

    private fun getWordOrNull(word: String): Pair<Int, String>? {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val resultSet = conn.createStatement().executeQuery("SELECT id, word FROM words WHERE word = '$word'")
            return if (resultSet.next()) Pair(resultSet.getInt(1), resultSet.getString(2)) else null
        }
    }

    fun insertWord(word: String): Pair<Int, String> {
        getWordOrNull(word)?.let { return it }
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val preparedStatement = conn.prepareStatement("INSERT OR IGNORE INTO words (word) VALUES (?)")
            preparedStatement.setString(1, word)
            preparedStatement.execute()
            val wordId = conn.createStatement().executeQuery("SELECT last_insert_rowid()").getInt(1)
            populateTrigrams(word, conn.createStatement().executeQuery("SELECT last_insert_rowid()").getInt(1))
            return Pair(wordId, word)
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
