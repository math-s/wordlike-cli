package wordlike

import org.junit.jupiter.api.Test

class MainTest {
    @Test
    fun testSimilartoCommand() {
        val args = arrayOf("similarto", "word")
        main(args)
    }
}
