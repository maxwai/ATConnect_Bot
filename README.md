![Java](https://badgen.net/badge/language/Java/green)
[![GitHub license](https://badgen.net/github/license/maxwai/ATConnect_Bot)](LICENSE)
[![release](https://badgen.net/github/release/maxwai/ATConnect_Bot)](https://github.com/maxwai/ATConnect_Bot/releases)


# ATConnect Bot
The source code for the ATConnect Bot

## Getting Started

### Prerequisites

You will need Java Version 15 or later to make it work.
It may work with lower Java versions, but it was programmed using the Java 15 JDK.

This Bot isa supposed to be running on only one Server at a Time.

### Installing

Download the jar file from the latest release and save it in a folder.
You will need to create at 2 files (These files will be created by the program if not present,
and you will be asked to fill them):
* Token.cfg (contains only the Bot Token)
* Roles.cfg <br> Layout:
  * Guild=\<ID of the Guild>
  * Owner=\<ID of the Owner Role>
  * Admin=\<ID of the Admin Role>
  * Event_Organizer=\<ID of the Event_Organizer Role>
  * Instructor=\<ID of the Instructor Role>
  * Trained=\<ID of the Trained Roles>

### How to Use

* start the jar file in a terminal with the command `java -jar ATConnect_Bot.jar` <br>
  (do not just double click it to open it)
* For now, all commands are only in Discord, command line commands are in the works

## TODO

- [X] Countdown functionality that carries on after restart
- [X] Purge function for Admin
- [X] Timezone functionality to know the timezone / current local time of a user
- [X] Possibility to add the Trained Role to Users for Instructors
- [ ] Event functionality where users can choose their position

## License [![GitHub license](https://badgen.net/github/license/maxwai/ATConnect_Bot)](LICENSE)

This project is licensed under the GNU General Public License - see the [LICENSE](LICENSE) file for details