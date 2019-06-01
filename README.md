[![License GNU GPL3](https://img.shields.io/badge/license-GNU%20GPL%203-yellow.svg)](http://www.gnu.org/licenses/gpl.html)
[![Java 7.0+](https://img.shields.io/badge/java-7.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](http://yasobe.ru/na/iamoss)


![ZX-Poly logo](docs/zxpoly_logo.png)

# Introduction
The idea of the platform was developed by me in April 1994, I had some free time to think about well-known problem of ZX-Spectrum platform - ATTRIBUTE CLASH. I found good solution to resolve the issue with saving of back compatibility with existing software (as minimum with most of them). The solution was - to rework mono-system into poly-system and add extra CPUs working in parallel to process color data. The solution provides some way not only keep back compatibility with software but also use existing OS.   

I believe that ZX-Poly could be assembled with electronic components of 80th and it shows that we could have some four core CPU home platform in that time even in the USSR (where ZX-Spectrum was produced by many enthusiasts and small companies). Unfortunately the idea was born too late and couldn't meet any interest from ZX-Spectrum producers in Russia. So that the emulator can be recognized as some proof of concept.

# License
The Emulator and all its parts are published under [GNU GPL3 license](http://www.gnu.org/licenses/gpl.html). So that it is absolutely free for non-commercial use.

# UI
![The Main Window of the Emulator](docs/screenshots/tapeloading.png)

# Theory and structure of the ZX-Poly platform
![ZXPoly test ROM screen](docs/screenshots/zxpoly_test_rom_video.gif)    
The Base of the platform is the theory that stable synchronous systems (without any inside random processes) built on the same components (because different element base can be also source of random processes) by being started synchronously in the same time from the same state have the same state in any point of time if all synchronous system components get the same input signal states in the same time.
![Structure of ZXPoly](docs/zxpolystruct.png)
ZX-Poly platform adds several ports to manipulate work modes and the main port of the platform is #3D00. [In more details it is desribed in wiki.](https://github.com/raydac/zxpoly/wiki/Short-description-of-ZX-Poly-platform)

# F.A.Q.
## Is there a hardware implementation?
The Idea of the Platform was born in 1994 and it was too late to invest money and resources into hardware implementation because in Russia the sunset of ZX-Spectrum was in 1992-1993. I had some discussions with management of the Peters Plus company (the main developer of the Sprinter platform, the first world FPGA based ZX-Spectrum clone) but their platform was single-CPU one and the on-board FPGA was too weak to emulate a multi-Z80 platform.

## Does the platform need a custom OS?
No, it doesn't. ZX-Poly uses standard ZX-Spectrum 128 OS + TR-DOS. On start ZX-Poly even starts only one Z80 and three others in sleep mode and it is no so easy to detect difference in work with regular ZX-Spectrum 128 (I didn't find).

## What is difference with Spec256?
Both ZX-Poly and Spec256 work as [SIMD](https://en.wikipedia.org/wiki/SIMD) computers but ZX-Poly is based on 4 Z80 "real" CPUs and Spec256 has some mix of 8 bit + 64 bit virtual Z80 CPU under the hood, I made attempt to convert some games from Spec256 to ZX-Poly but attempt failed because Spec256 is much more tolerant for damage of execution code (it executes non changed program and only changed extra-graphical data is sent through virtual 64 bit registers, changed data blocks don't affect executing flow).

## Multi-threading
ZX-Poly is a normal multi-CPU system and can work as in SIMD mode (to be compatible with existing software) as in MIMD mode and in the mode all CPUs work independently. It has some primitive synchronization mechanism called STOP-ADDRESS which allows to align common start address for all CPUs, also CPU0 can get some approximate info about current executing addresses of other CPUs and can get even some signals through interruptions what makes possible to emulate some periphery (like sound devices) in slave CPUs. Each CPU has its own memory area in common heap but it is possible to override memory areas and even map them totally each other (in the case it will be just multi CPU ZX 128).


## Which software can be adapted for the platform?
The Main requirement - the software should not have optimization of graphic output and should not have check what it outputs on the screen, enough number of games work in such manner and also system utilities (ZX-Poly has 512x384 mode and it is possible to increase resolution of text utilities and editors just through their fonts and icons correction)

# Emulator supports formats
 - Snapshots .Z80, .SNA, .ZXP (ZX-Poly snapshot format produced by the Sprite Editor from .Z80 snapshots and included into the project)
 - Tape .TAP (allows export to WAV file)
 - Disks .TRD, .SCL

# Prebuilt versions
The Latest prebuilt versions of the emulator and sprite editor can be downloaded from [the release page](https://github.com/raydac/zxpoly/releases).   
The Emulator is written in Java and its pure JAR file can be started in command line with just `java -jar zxpoly-emul-2.0-SNAPSHOT.jar`   
By default it uses the embedded ZX-Poly Test ROM image for start, but you can pass through **File->Options->Active ROM** and select another SOS+TR-DOS 128 ROM source (from list of links) and after that reload the emulator, keep in mind that WorldOfSpectrum FTP works not very stable so I recommend prefer VirtualTRDOS as ROM source. After loading, the ROM will be cached on your machine for future usage.

# Supported videomodes
## Standard ZX-Spectrum 256x192 (2 colors per pixel in 8x8 block)
It is just regular ZX-Spectrum movde 256x192 with 2 attributed colors for 8x8 pixel block.
![Standard ZX screenshot](docs/screenshots/atw_standard.png)

## ZX-Poly 256x192 (16 colors per pixel)
The Mode doesn't use attributes and each pixel can have color from 16 color palette. Also there is modification of the mode which uses attributes from CPU0 module to mask screen areas by INK color if it is the sime as PAPER color in attribute.

[TRD disk with the example can be downloaded from here, the game has been partly colorized](https://raydac.github.io/downloads/zips/atw1_partly_colorized.trd)
![ZXPoly256x192 screenshot](docs/screenshots/atw_zxpoly.png)   

## ZX-Poly 512x384 (2 colors per pixel placed in chess order)
The Mode uses attributes but places pixels in chess order.

[TRD disk with the example can be downloaded from here](trds/zxw26_adapted_for_zxpoly.trd)
![ZXPoly512x384 screenshot](docs/screenshots/zxw_zxpoly512x384.png)

# Adaptation of games

To adapt old games, I have developed special utility called ZX-Poly Sprite corrector, which can be found in releases. It is a very easy editor which shows images in data blocks and allows to redraw them. It supports import from Z80 and SNA snapshots and as the result it generates ZXP snapshots.
![ZXPoly Sprite Corrector screenshot](https://github.com/raydac/zxpoly/blob/master/docs/zxpoly_sprite_editor.png)

## "Official Father Christmas" (1989)
Original look of the game   
![Original game screen](adapted/OfficialFatherChristmas/originalgamescreen.gif)

On Christmas 2017 I made some adaptation of the old game ["Official Father Christmas" (1989)](http://www.worldofspectrum.org/infoseekid.cgi?id=0003493) for ZX-Poly. Of course not all was smoothly, some elements of third level could not be colorized because their colorization broke game process and made CPU modules out of sync, it looks like that the level contains some optimization in graphics processing for those elements and presented some check for empty areas to optimize speed.   
Adapted game version in ZX-Poly emulator format can be downloaded [from here](adapted/OfficialFatherChristmas/OFCZXPOLY.zxp).   
![Official Father Christmas GIF](adapted/OfficialFatherChristmas/movie.gif)

## "Adventures of Buratino" (1993) 
I was playing with adaptation of the game for 512x384 mode, program code works well but the game and without adaptation is so colorful and detailed that big effect is not visible. Also it was impossible to adapt the main hero sprites because the same sprites are used for both right and left walk through mirroring.   
Adapted game version in ZX-Poly emulator format can be downloaded [from here](adapted/BuratinoAdventures/buratino_adventures.zxp).   
![Adventures of Buratino comparation](adapted/BuratinoAdventures/comparescr.png)
