# CVTweaks
CVTweaks is a collection of features designed to make playing on the Cubeville minecraft server (`cubeville.org`) more efficiently.

## Features:
### Free:
* RegionScript - a simple scripting language that allows you to make specific regions do different actions, like showing titles, playing music, and creating basic minigames, all on the client
* Toggle gamemode keybind - (default key is Y) lets you quickly change your Creative gamemode with the press of a key
* Fly speed changer - use the - and + keys to increase or decrease your creative fly speed, you can also press both to reset it to the default
* /cvwiki \<search\> - allows you to search for any topic on the *unofficial* Cubeville wiki and get a link to it

### Premium:
* All features of free PLUS:


* AutoHome - when enabled with `/autohome <on/off>`, it will automatically set your home in the Survival world when your health is low, to allow you to easily TP back incase you die
* WCSafety - when toggled with `/wcsafety`, if you are chatting in a group chat or private message and after send a message in global, it will make you send it twice to confirm whether you want to send it in global
* Warp Menu - (default key is H) when pressed in the Creative world, it brings up a popup which allows you to quickly type the username of any person to warp to their plot
* /ignore \<add/remove/list\> - allows you to ignore players, hiding any global messages sent by them (does not hide private messages! if you're having issues with someone in your private messages, it's time to talk to staff instead)
* Channel Toggle - you can use /group or /global to toggle on and off only chatting in your group chat or global chat channels, without you needing to type /p or /y before the message
* Nearest Drop - you can use the command `/nearestdrop [coordinates]` to get the closest drop or warpable location to those coordinates
* And more to be added later!

## RegionScript
RegionScript is a custom-coded, extremely basic, scripting language which you are able to apply to regions or subzones by using a greeting message
For instance:
`/rg flag AllergenX greeting !showTitle welcome to my plot | 1 | !showActionBar 1 second after wow`
will show a title on the player's screen when they enter my plot, wait 1 second, and then show a message in the action bar as well

There are tons of other commands you can use

### RegionScript Commands:
* !playsong <url> - plays sound from the url (url must be a direct link to an mp3 file!)
* !stopall - stops all currently playing sounds from `!playsong`
* !showtitle <text> - shows a title
* !showactionbar <text> - shows a message in the actionbar (the space above the player's hotbar)
* !showsubtitle <text> - shows a subtitle
* !gotohome <username> - goes to the player's creative plot
* !showtimer <duration> - shows a timer which is displayed in the player's actionbar and counts down in seconds based on the duration
* !toggle <option> <on/off> - allows you to toggle specific options like `timer_death`, which will kill the player when the timer hits 0
* !shake <intensity> <duration (in ticks)> - shakes the camera (for 1 second, the duration should be set to 20 as it is in ticks)
* !popup <text> - shows a popup on the player's screen with an ok button
* And more to come!

## Premium
Most of the mod's utility features are premium, which requires you to pay cubes to access those features.
It is only a one-time payment of 80 cubes, and please message me before sending it so I'm aware and I will add your user to the database/

## Download
You can download it from the release tab on the right, where you can find all the versions.
The mod is currently only for Minecraft Fabric 1.21.8, and will be updated as Cubeville updates.

## Source Code
This project is open-source for transparency, since it isnt going to be uploaded to sites like Modrinth or Curseforge.

If you don't trust the jar files, you can feel free to open it up in the editor of your choice and build it yourself.

Please note that as you can modify the code in it, you can also bypass premium checks, I cannot stop you from doing this, however then I do not get any cubes for something I've worked for a long time on, so I would appreciate it if you actually paid cubes for it instead.
We will not be accepting pull requests, but you can feel free to create issues for any bugs you may find