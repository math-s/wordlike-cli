package wordlike
import org.sqlite.Function
import java.math.BigDecimal
import java.sql.Connection
import java.sql.DriverManager

fun levenshteinDistance(
    lhs: CharSequence,
    rhs: CharSequence,
): Int {
    val lhsLength = lhs.length
    val rhsLength = rhs.length

    var cost = Array(lhsLength + 1) { it }
    var newCost = Array(lhsLength + 1) { 0 }

    for (i in 1..rhsLength) {
        newCost[0] = i

        for (j in 1..lhsLength) {
            val match = if (lhs[j - 1] == rhs[i - 1]) 0 else 1

            val costReplace = cost[j - 1] + match
            val costInsert = cost[j] + 1
            val costDelete = newCost[j - 1] + 1

            newCost[j] = minOf(costInsert, costDelete, costReplace)
        }

        val swap = cost
        cost = newCost
        newCost = swap
    }

    return cost[lhsLength]
}

class LevenshteinFunction : Function() {
    override fun xFunc() {
        val arg1 = value_text(0)
        val arg2 = value_text(1)
        result(levenshteinDistance(arg1, arg2))
    }
}

fun registerLevenshteinFunction(connection: Connection) {
    Function.create(connection, "LEVENSHTEIN", LevenshteinFunction())
}

object LevenshteinResolver : Resolver {
    override fun findSimilar(word: String): List<Pair<BigDecimal, String>> {
        val connection: Connection = DriverManager.getConnection("jdbc:sqlite:words.db")
        registerLevenshteinFunction(connection)
        connection.use { conn ->
            val query = """
            SELECT word, LEVENSHTEIN(word, ?) AS distance
            FROM words
            ORDER BY distance ASC
            LIMIT 10;
        """
            val statement = conn.prepareStatement(query)
            statement.setString(1, word)
            val resultSet = statement.executeQuery()
            val results = mutableListOf<Pair<BigDecimal, String>>()
            while (resultSet.next()) {
                val candidate = resultSet.getString("word")
                val distance = resultSet.getInt("distance")
                results.add(Pair(distance.toBigDecimal(), candidate))
            }
            return results
        }
    }
}
