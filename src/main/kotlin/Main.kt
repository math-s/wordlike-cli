package wordlike

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import mu.KotlinLogging
import java.math.RoundingMode

val logger = KotlinLogging.logger {}

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    val parser = ArgParser("similarto")

    val similarto =
        object : Subcommand("similarto", "Find similar words.") {
            val word by argument(ArgType.String, description = "Target word to find similar words.")

            override fun execute() {
                JaccardResolver.findSimilar(word).forEach { logger.info { "${it.first.setScale(2, RoundingMode.UP)} ${it.second}" } }
            }
        }

    val index =
        object : Subcommand("index", "Index new words.") {
            val fileName by argument(ArgType.String, description = "Path to file to be loaded.")

            override fun execute() = Indexer.execute(fileName)
        }

    parser.subcommands(similarto, index)
    parser.parse(args)
}
