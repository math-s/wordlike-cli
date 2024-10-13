package wordlike

import org.junit.jupiter.api.Test
import java.math.RoundingMode

class JaccardResolverTest {
    @Test
    fun testFindSimilar() {
        val result =
            buildString {
                JaccardResolver.findSimilar("ths").forEach {
                    append("${it.first.setScale(2, RoundingMode.UP)} ${it.second} \n")
                }
            }
        println(result)
    }

    @Test
    fun testFindSimilarLevenshtein() {
        val result =
            buildString {
                LevenshteinResolver.findSimilar("ths").forEach {
                    append("${it.second} \n")
                }
            }
        println(result)
    }
}
