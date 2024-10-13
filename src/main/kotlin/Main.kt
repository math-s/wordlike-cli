package wordlike

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.ExperimentalCli
import kotlinx.cli.Subcommand
import mu.KotlinLogging

val logger = KotlinLogging.logger {}

@OptIn(ExperimentalCli::class)
fun main(args: Array<String>) {
    val parser = ArgParser("similarto")

    val similarto =
        object : Subcommand("similarto", "Find similar words.") {
            val phrase by argument(ArgType.String, description = "Target word to find similar words.")

            override fun execute() {
                val result =
                    buildString {
                        phrase
                            .split(
                                " ",
                            ).forEach { word ->
                                append(JaccardResolver.findSimilar(word).first().second + " ")
                            }
                    }
                logger.info { result }
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
