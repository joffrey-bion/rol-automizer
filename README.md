# Rise Of Lords Automizer

[![Bintray](https://img.shields.io/bintray/v/joffrey-bion/applications/rol-automizer.svg)](https://bintray.com/joffrey-bion/applications/rol-automizer/_latestVersion)
[![Github Build](https://img.shields.io/github/workflow/status/joffrey-bion/rol-automizer/CI%20Build?label=build&logo=github)](https://github.com/joffrey-bion/rol-automizer/actions?query=workflow%3A%22CI+Build%22)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/rol-automizer/blob/master/LICENSE)

This is a simple sequencer for the game [Rise of Lords](http://www.riseoflords.com/).

## What does it do?

This program performs a regular routine on your behalf: 

- attack players to steal gold
- repair weapons from time to time
- store the gold in the chest from time to time

## Usage

You can run the program using [Docker](https://www.docker.com/) with the following command:

```
docker run -it hildan/rol-automizer -u YOUR_USERNAME [OPTIONS]
```

You will be prompted for your password, unless the `ROL_PASSWORD` environment variable is defined.
You can also provide your username as the `ROL_USERNAME` environment variable if you don't want to type it on the 
command line.

Here are the program options:

```
Usage: rol-automizer [OPTIONS]

Options:
-u, --username TEXT      your Rise of Lords username
-p, --password TEXT      your Rise of Lords password
-m, --min-rank INT       the minimum rank of the players to attack
-M, --max-rank INT       the maximum rank of the players to attack
-g, --min-gold INT       the minimum gold of the enemy player to consider an attack worth it
-t, --max-turns INT      the maximum number of turns to use during an attack session
-r, --repair-period INT  the number of attacks between weapon repairs
--storage-threshold INT  the threshold above which we need to store the current gold into the chest
--attacks-count INT      the number of attack sessions to perform
--rest-time HOURS        the number of hours to wait between attack sessions
-h, --help               Show this message and exit
```

## License

Code released under [the MIT license](https://github.com/joffrey-bion/rol-automizer/blob/master/LICENSE)
