package itmo.polik;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Component
public class Bot extends TelegramLongPollingBot {
    private final String BOT_TOKEN;

    private static final String SINGLE_FILE = "src/files/Single.mp3";
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final String hashedPassword = passwordEncoder.encode("0303");
    private final String doorHashedPassword = passwordEncoder.encode("163710");
    private boolean authenticated = false;
    private boolean isPhoneFound = false;
    private boolean isDoorFound = false;
    private final List<String> filePaths = Arrays.asList(
            "src/files/Поцелуй!!.png",
            "src/files/заметки.docx",
            "src/files/Скандал.png",
            "src/files/Таро.jpeg",
            "src/files/Я.jpeg"
    );

    public Bot() {
        Dotenv dotenv = Dotenv.load();
        this.BOT_TOKEN = dotenv.get("TELEGRAM_BOT_TOKEN");
        if (BOT_TOKEN == null || BOT_TOKEN.isEmpty()) {
            throw new RuntimeException("Telegram bot token is not set. Please create a .env file with TELEGRAM_BOT_TOKEN.");
        }
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            if (messageText.equals("/start")) {
                sendMessage(chatId, "Введите пароль: \n ▢ ▢ ▢ ▢");
                authenticated = false;
                isPhoneFound = true;
            } else if (isPhoneFound && passwordEncoder.matches(messageText, hashedPassword) && !authenticated) {
                sendMessage(chatId, "Правильный пароль");
                sendFile(chatId);
                sendMessageWithDelay(chatId, 600000);
                authenticated = true;
            } else if (messageText.equals("/door")) {
                sendMessage(chatId, "Введите пароль: \n ▢ ▢ ▢ ▢ ▢ ▢");
                isDoorFound = true;
            } else if (isDoorFound && passwordEncoder.matches(messageText, doorHashedPassword)) {
                sendMessage(chatId, "Дверь открыта");
            } else if ((!authenticated && isPhoneFound) || (isDoorFound)){
                sendMessage(chatId, "Неправильный пароль");
            }
        }
    }

    private void sendFile(long chatId) {
        try {
            for (String filePath : filePaths) {
                InputFile inputFile = new InputFile(new File(filePath));
                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(chatId);
                sendDocument.setDocument(inputFile);
                execute(sendDocument);
            }
        } catch (TelegramApiException e) {
            sendMessage(chatId, "Ошибка отправки файла: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendMessage(long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMessageWithDelay(long chatId, long delayMillis) {
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                InputFile inputFile = new InputFile(new File(SINGLE_FILE));
                SendDocument sendDocument = new SendDocument();
                sendDocument.setChatId(chatId);
                sendDocument.setDocument(inputFile);
                try {
                    execute(sendDocument);
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                }
            }
        }, delayMillis);
    }

    @Override
    public String getBotUsername() {
        return "detective_password_bot";
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

}
