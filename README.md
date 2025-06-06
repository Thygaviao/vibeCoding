# Cow Quote Bot

A simple Telegram bot written in Kotlin that responds to the `/quote` command with a cowSay message containing a random scammer-style quote. The bot uses the [Telegram Bots API](https://github.com/rubenlagus/TelegramBots) and the [cowsay](https://github.com/RobertFischer/CowsayJava) library.

## Features

- Responds to the `/quote` command
- Quotes are stored in code (over one hundred phrases)
- Uses cowsay to format the reply
- Requires Java 17+

## Setup on Ubuntu 20.04 LTS

1. Install JDK and required tools:

   ```bash
   sudo apt update
   sudo apt install openjdk-17-jdk git curl unzip gradle
   ```

2. Clone the repository and build:

   ```bash
   git clone <repo_url>
   cd vibeCoding
   gradle build
   ```

3. Set the `BOT_TOKEN` environment variable with your Telegram bot token and run the bot:

   ```bash
   export BOT_TOKEN=YOUR_TELEGRAM_BOT_TOKEN
   gradle run
   ```

The bot will start and listen for `/quote` commands.
