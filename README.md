# Rise Of Lords Automizer

[![Bintray](https://img.shields.io/bintray/v/joffrey-bion/applications/rol-automizer.svg)](https://bintray.com/joffrey-bion/applications/rol-automizer/_latestVersion)
[![Travis Build](https://img.shields.io/travis/joffrey-bion/rol-automizer/master.svg)](https://travis-ci.org/joffrey-bion/rol-automizer)
[![Dependency Status](https://www.versioneye.com/user/projects/56d2f52c157a6913c1e6c83f/badge.svg)](https://www.versioneye.com/user/projects/56d2f52c157a6913c1e6c83f)
[![GitHub license](https://img.shields.io/badge/license-MIT-blue.svg)](https://github.com/joffrey-bion/rol-automizer/blob/master/LICENSE)

You play RoL, but you're tired of repetitive tasks such as attacking a whole series of players?
This is the solution: a simple sequencer for the game [Rise of Lords](http://www.riseoflords.com/).

## What does it do?

This program is meant to do the boring task of attacking players for you. All you have to do is setting what kind of players you want to attack (rank and minimum gold). The sequencer takes care of the rest:
- **attacks players** (up to a maximum that you set)
- **repairs weapons** from time to time (can be customized)
- **stores the gold** in the chest from time to time (can be customized)
 
Then you only have to focus on the interesting part of the game: how to spend your gold!

## Get started in 3 minutes

1. Download the [binary program here](https://dl.bintray.com/joffrey-bion/applications/org/hildan/bots/rol-automizer/1.3.0/rol-automizer-1.3.0.exe)
2. Create a `.rol` file with a text editor, similar to the following one ([download here](https://raw.githubusercontent.com/joffrey-bion/RiseOfLords/master/dist/template.rol)):

        # credentials for connection
        account.login=myLogin
        account.password=myPassword
        
        # attack only players within the given bounds
        filter.minRank=4000
        filter.maxRank=6500
        # attack only player with more gold than specified
        filter.minGold=350000
        
        # maximum number of attacks
        attack.maxTurns=80
        # store gold into chest as soon as this threshold is reached
        attack.storageThreshold=300000
        # repair weapons every N attacks
        attack.repairPeriod=5
        
        # duration in hours to wait between each attack session
        sequence.hoursBetweenAttacks=1
        # number of attack sessions to perform
        sequence.nbOfAttacks=5

3. You may now open your .rol file with the main program (right click > open with...)
4. Enjoy the gold

## License

Code released under [the MIT license](https://github.com/joffrey-bion/rol-automizer/blob/master/LICENSE)
