import com.github.ricksbrown.cowsay.Cowsay
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import kotlin.random.Random

/**
 * Telegram bot that responds with a cowSay quote when the /quote command is received.
 * @param token Telegram bot token.
 */
class CowBot(private val token: String) : TelegramLongPollingBot(token) {

    override fun getBotUsername(): String = "CowQuoteBot"

    override fun onUpdateReceived(update: Update) {
        if (update.message?.text == "/quote") {
            val chatId = update.message.chatId.toString()
            val quote = Quotes.list.random(Random)
            val cow = Cowsay.say(arrayOf(quote))
            val text = """```\n$cow\n```"""
            val message = SendMessage(chatId, text).apply {
                parseMode = "MarkdownV2"
            }
            try {
                execute(message)
            } catch (e: TelegramApiException) {
                e.printStackTrace()
            }
        }
    }
}
