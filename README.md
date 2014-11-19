Rise Of Lords Sequencer
===========

You play RoL, but you're tired of repetitive tasks such as attacking a whole series of players?
This is the solution : a simple sequencer for http://www.riseoflords.com/

What does it do?
-----------
This program is meant to do the boring task of attacking players for you. All you have to do is setting what kind of players you want to attack (rank and minimum gold). The sequencer takes care of the rest:
- **attacks players** (up to a maximum that you set)
- **repairs weapons** from time to time (can be customized)
- **stores the gold** in the chest from time to time (can be customized)
 
Then you only have to focus on the interesting part of the game: how to spend your gold!

Get started in 3 minutes
-----------
1. Download the [JAR of the sequencer here](https://github.com/joffrey-bion/RiseOfLords/blob/master/dist/RiseOfLords.jar?raw=true)
2. Create a `bot.properties` file, similar to the following one ([download here](https://raw.githubusercontent.com/joffrey-bion/RiseOfLords/master/dist/bot.properties)), customize the settings and put it **next to the JAR archive**:

        # credentials for connection
        account.login=myLogin
        account.password=myPassword
        
        # attack only players within the given bounds
        filter.minRank=5000
        filter.maxRank=6500
        # attack only player with more gold than specified
        filter.minGold=400000
        
        # maximum number of attacks
        attack.maxTurns=80
        # store gold into chest every N attacks
        attack.storingFrequency=2
        # repair weapons every N attacks
        attack.repairFrequency=5
        
        # duration in hours to wait between each attack session
        sequence.hoursBetweenAttacks=1
        # number of attack sessions to perform
        sequence.nbOfAttacks=5

3. Finally, for convenience, use [this tiny script](https://raw.githubusercontent.com/joffrey-bion/RiseOfLords/master/dist/launcher.cmd) to launch the program on Windows, or launch the JAR directly from the command line yourself:

        java -jar RiseOfLords.jar
       
4. Enjoy the gold

