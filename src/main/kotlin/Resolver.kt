package wordlike

import mu.KotlinLogging
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager

object JaccardResolver {
    private val logger = KotlinLogging.logger {}

    private fun getWordsByTrigram(targetWord: String): Array<Pair<BigDecimal, String>> {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val statement =
                conn.prepareStatement(
                    "SELECT DISTINCT target, " +
                        "       candidate, " +
                        "       shared_trigrams_count, " +
                        "       total_trigrams_target, " +
                        "       total_trigrams_candidate, " +
                        "       shared_trigrams_count * 1.0 / (total_trigrams_target + total_trigrams_candidate - shared_trigrams_count) AS jaccard_index " +
                        "FROM (SELECT w1.word                                                     AS target, " +
                        "             w2.word                                                     AS candidate, " +
                        "             COUNT(tw1.trigram_id)                                       AS shared_trigrams_count, " +
                        "             (SELECT COUNT(*) FROM trigrams_words WHERE word_id = w1.id) AS total_trigrams_target, " +
                        "             (SELECT COUNT(*) FROM trigrams_words WHERE word_id = w2.id) AS total_trigrams_candidate " +
                        "      FROM trigrams_words tw1" +
                        "               JOIN trigrams_words tw2 ON tw1.trigram_id = tw2.trigram_id AND tw1.word_id != tw2.word_id " +
                        "               JOIN words w1 ON tw1.word_id = w1.id " +
                        "               JOIN words w2 ON tw2.word_id = w2.id " +
                        "      WHERE w1.word = '$targetWord' " +
                        "      GROUP BY w1.id, w2.id " +
                        "      ORDER BY shared_trigrams_count DESC " +
                        "      LIMIT 10) subquery " +
                        "ORDER BY jaccard_index DESC;".trimIndent(),
                )
            val resultSet = statement.executeQuery()
            val results = mutableListOf<Pair<BigDecimal, String>>()
            while (resultSet.next()) {
                results.add(Pair(resultSet.getBigDecimal("jaccard_index"), resultSet.getString("candidate")))
            }
            return results.toTypedArray()
        }
    }

    fun findSimilar(target: String): Array<Pair<BigDecimal, String>> {
        Indexer.insertWord(target)
        return getWordsByTrigram(target)
    }
}
