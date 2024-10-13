package wordlike

import java.math.BigDecimal

interface Resolver {
    fun findSimilar(word: String): List<Pair<BigDecimal, String>>
}
