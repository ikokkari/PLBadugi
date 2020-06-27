# Heads-Up Pot Limit Badugi

[Pot limit badugi](https://en.wikipedia.org/wiki/Badugi) Java programming project used by [Ilkka Kokkarinen](https://www.cs.ryerson.ca/~ikokkari/) for a couple of semesters in the course [CCPS 721 Artificial Intelligence I](https://github.com/ikokkari/AI).

Badugi is a variation of [lowball draw poker](https://en.wikipedia.org/wiki/Lowball_(poker)). This game let students to experience the decision-theoretic aspects of poker, but without the real poker's complicated evaluation of combinatorics of the potential of a hand to make better hands. The freedom to organize the logic of the agent any which way they want produced some very interesting results throughout the semesters that the author used this project in his course.

Students implement their agents as Java classes that implement the interface `PLBadugiPlayer` that defines the methods assumed by the game engine `PLBadugiRunner`. This engine easily be used to organize a heads-up tournaments between any number of participating agents, which allows the students to organize their own mini-tournaments before the main event. In the final rumble, every agent plays against every other agent a large enough number of hands to eliminate the effect of luck. The student who wrote the winning agent has canonically received the entirely ceremonial title of "The Fastest Gun East of Mississauga" to honor this achievement.

All instructors of intro courses on artificial intelligence are welcome to use, adapt and distribute this code. All source code and the specification document are released under GNU General Public License v3.
