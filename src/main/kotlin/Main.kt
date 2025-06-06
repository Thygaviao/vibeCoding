import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession

/**
 * Entry point for the application.
 * Reads the bot token from the BOT_TOKEN environment variable
 * and registers the Telegram bot.
 */
fun main() {
    val token = System.getenv("BOT_TOKEN")
        ?: error("BOT_TOKEN environment variable not set")

    val botsApi = TelegramBotsApi(DefaultBotSession::class.java)
    botsApi.registerBot(CowBot(token))
}
