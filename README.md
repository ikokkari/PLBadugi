# Heads-Up Pot Limit Badugi

[Pot limit badugi](https://en.wikipedia.org/wiki/Badugi) Java programming project used by [Ilkka Kokkarinen](https://www.cs.ryerson.ca/~ikokkari/) for a couple of semesters in the course [CCPS 721 Artificial Intelligence I](https://github.com/ikokkari/AI).

Badugi is a variation of [lowball draw poker](https://en.wikipedia.org/wiki/Lowball_(poker)). This game lets students experience the game-theoretically interesting decision theoretic aspects of poker, but without the real poker's complicated combinatorics to calculate the potential for a hand to improve. The freedom to organize the logic of the agent any which way the students want produced some very interesting solutions in this aspect in all semesters that this project was used in CCPS 721.

Students simply implement their agents as Java classes that implement the interface `PLBadugiPlayer` that defines the methods assumed by the game engine `PLBadugiRunner`. This engine easily be used to organize a heads-up tournaments between any number of participating agents. This allows study groups to organize their own mini-tournaments before the main event at the end of the course. In this final rumble, every agent plays enough hands against heads-up every other agent to eliminate the effect of luck. The student who creates the winning agent has canonically been granted the  purely ceremonial title of "The Fastest Gun East of Mississauga" to honor that achievement.

Two implementations of `PLBadugiPlayer` are provided for students to sharpen their agents against. Their source code is not entirely the cleanest. The agent `IlkkaPlayer3` is a modest rule-based approach that probably still contains several bugs, whereas `RLPlayer` was trained to play with reinforcement learning. The file `tt.gz` contains the data used by `RLPlayer`.

All instructors of intro courses on artificial intelligence are welcome to use, adapt and distribute this code. All source code and the specification document are released under GNU General Public License v3.
