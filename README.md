![Java](https://badgen.net/badge/language/Java/green)
[![GitHub license](https://badgen.net/github/license/maxwai/ATConnect_Bot)](LICENSE)
[![release](https://badgen.net/github/release/maxwai/ATConnect_Bot)](https://github.com/maxwai/ATConnect_Bot/releases)

# ATConnect Bot

The source code for the ATConnect Bot

## Getting Started

### Prerequisites

You will need Java Version 15 or later to make it work. It may work with lower Java versions, but it
was programmed using the Java 15 JDK.

**This Bot is supposed to be running on only one Discord Server at a Time.**

### Installing

Download the jar file from the latest release and save it in a folder. You will need to create a
Config.xml file (A dummy file will be created by the program if not present, and you will be asked
to fill it)

### How to Use

* Start the jar file in a terminal with the command `java -jar ATConnect_Bot.jar` <br>
  (do not just double click it to open it)
* All commands are only in Discord, command line commands are not necessary since the Bot is
  supposed to be run as a daemon and a service for easiness
* It is possible to stop the bot by sending `stop` to the Telegram Bot

## TODO

- [X] Countdown functionality that carries on after restart
- [X] Purge function for Admin
- [X] Timezone functionality to know the timezone / current local time of a user
- [X] Possibility to add the Trained Role to Users for Instructors
- [X] Get the Logs via 2 Telegram Bots
- [X] Change the Config to XML
- [X] Event functionality where users can choose their position

## License [![GitHub license](https://badgen.net/github/license/maxwai/ATConnect_Bot)](LICENSE)

This project is licensed under the GNU General Public License - see the [LICENSE](LICENSE) file for
details