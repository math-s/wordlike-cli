package wordlike

import mu.KotlinLogging
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager

object JaccardResolver {
    private val logger = KotlinLogging.logger {}

    private fun countSubstringsInWords(target: String): Array<Pair<BigDecimal, String>> {
        val trigrams = Indexer.getTrigrams(target)
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        connection.use { conn ->
            val query =
                buildString {
                    append("SELECT word, ")
                    append(trigrams.joinToString(" + ") { "SUM(CASE WHEN word LIKE '%$it%' THEN 1 ELSE 0 END)" })
                    append(" AS substring_count FROM words GROUP BY word ORDER BY substring_count DESC LIMIT 10;")
                }
            val statement = conn.prepareStatement(query)
            val resultSet = statement.executeQuery()
            val results = mutableListOf<Pair<BigDecimal, String>>()
            while (resultSet.next()) {
                val candidate = resultSet.getString("word")
                if (candidate == target) continue
                val trigramCount = resultSet.getInt("substring_count")
                val candidateTrigrams = Indexer.getTrigrams(resultSet.getString("word"))
                val jaccardIndex =
                    BigDecimal(
                        trigramCount * 1.0 / (trigrams.size + candidateTrigrams.size - trigramCount),
                    )
                results.add(Pair(jaccardIndex, candidate))
            }
            results.sortByDescending { it.first }
            return results.toTypedArray()
        }
    }

    fun findSimilar(target: String): Array<Pair<BigDecimal, String>> {
        Indexer.insertWord(target)
        return countSubstringsInWords(target)
    }
}
