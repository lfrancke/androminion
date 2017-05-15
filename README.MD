# About this project

This is an Android version of the board game [Dominion](http://riograndegames.com/Game/278-Dominion) by Rio Grande Games.

This version is in no way official or endorsed by Rio Grande Games.

This specific fork is mostly a learning experience for me and I have no specific goals.

## History of the project

The version was originally developed on Google Code (which has been discontinued) and exported to [Github](https://github.com/mehtank/androminion) by Ankur Mehta. Ankur originally based his Github fork on version r561 (2012-08-02) of the
Google Code project and later updated it to r638 (2012-08-31).

In the years since the Github fork has seen major improvements (expansions, better AI, multiplayer) but the underlying project structure hasn't changed much since.

In May 2017 I (lfrancke) decided to fork the project again (based on commit [4301e438baf3893ce7dc3e6c9f9a18c0f5576cfe](https://github.com/mehtank/androminion/commit/4301e438baf3893ce7dc3e6c9f9a18c0f5576cfe)) and work a bit on the code. Originally the idea came along because of a bug I wanted fixed fast but having never ever done any Android programming I got curious.

So this current fork features the following changes from the mehtank version:

* Refactored the project from the old Eclipse style android project to a Gradle based version
* Removed ActionBarSherlock in favor of AppCompat (mind you, I have no idea what I'm doing here as it's my first time touching an Android app)
* Major code cleanup & reformatting to make the code style more consistent
* Some minor bugs fixed
* Removed all the branches and moved development back to the master branch

Licensed under LGPL: http://www.gnu.org/licenses/lgpl-3.0.html
