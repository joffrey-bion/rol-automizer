# Rise Of Lords Automizer

[![Github Build](https://img.shields.io/github/actions/workflow/status/joffrey-bion/rol-automizer/build.yml?branch=main&logo=github)](https://github.com/joffrey-bion/rol-automizer/actions/workflows/build.yml)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/rol-automizer/blob/master/LICENSE)

This is a simple bot for the game [Rise of Lords](http://www.riseoflords.com/).

## What does it do?

This program performs a regular routine on your behalf: 

- clone as many sorcerers as possible given the current gold and mana
- attack players to steal gold
- repair weapons from time to time
- store the gold in the chest from time to time

## Usage

You can run the program using [Docker](https://www.docker.com/) with the following command:

```
docker run --rm -it hildan/rol-automizer -u YOUR_USERNAME [OPTIONS]
```

You will be prompted for your password, unless the `ROL_PASSWORD` environment variable is defined.
You can also provide your username as the `ROL_USERNAME` environment variable if you don't want to type it on the 
command line.

Here are the program options:

```
Usage: rol-automizer [<options>]

Options:
  -u, --username=<text>      your Rise of Lords username
  -p, --password=<text>      your Rise of Lords password
  -m, --min-rank=<int>       the minimum rank of the players to attack
                             (default: 400)
  -M, --max-rank=<int>       the maximum rank of the players to attack
                             (default: 2200)
  -g, --min-gold=<int>       the minimum gold of the enemy player to consider
                             an attack worth it (default: 400000)
  -t, --max-turns=<int>      the maximum number of turns to use during an
                             attack session (default: 40)
  -r, --repair-period=<int>  the number of attacks between weapon repairs
                             (default: every 5 attacks)
  --storage-threshold=<int>  the threshold above which we need to store the
                             current gold into the chest (default: 300000)
  --attacks-count=<int>      the number of attack sessions to perform (default:
                             1)
  --rest-time=<hours>        the number of hours to wait between attack
                             sessions (default: 12h)
  -h, --help                 Show this message and exit
```

## License

Code released under [the MIT license](https://github.com/joffrey-bion/rol-automizer/blob/master/LICENSE)
