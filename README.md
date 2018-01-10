[![License GNU GPL3](https://img.shields.io/badge/license-GNU%20GPL%203-yellow.svg)](http://www.gnu.org/licenses/gpl.html)
[![Java 7.0+](https://img.shields.io/badge/java-7.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](http://yasobe.ru/na/iamoss)


![ZX-Poly logo](https://raw.githubusercontent.com/raydac/zxpoly/master/docs/zxpoly_logo.png)

# Introduction
It is not just a yet another ZX-Spectrum emulator, but the proof of concept of the ZX-Poly platform. The ZX-Poly platform was developed by Igor Maznitsa in Summer 1994 and it was not something absolutely new one but just a hardware modification of the old well-known ZX-Spectrum 128 platform (it even didn't  provide or require any new operating system and could use the ZX-Spectrum SOS), the main purpose of the modification was to remove the main graphic issue of ZX-Spectrum (color clashing) and save the same speed of graphic processing without injecting any extra GPU into the system with saving back-compatibility with the original platform. The Very big pros of the platform is that it could be assembled with the electronic components in the end of 80th and it shows that we could have some multi-CPU SIMD home platform in that time even in the USSR (where ZX-Spectrums were produced mainly by enthusiasts). Unfortunately the idea was born too late and already didn't meet any interest from ZX-Spectrum producers in Russia (it was time of ZX-Spectrum sunset).

# License
The Emulator and all its parts are published under [GNU GPL3 license](http://www.gnu.org/licenses/gpl.html). So that it is absolutely free for non-commercial use.

# UI
![The Main Window of the Emulator](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/tapeloading.png)

# Theory and structure of the ZX-Poly platform
![ZXPoly test ROM screen](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxpoly_test_rom_video.gif)    
The Base of the platform is the theory that stable synchronous systems (without any inside random processes) which are built on the same components (because different element base can be also source of random processes) by being started synchronously in the same time from the same state will have the same state in any point of time if all synchronous system get the same input signal states in the same time.
![Structure of ZXPoly](https://github.com/raydac/zxpoly/blob/master/docs/zxpolystruct.png)
ZX-Poly platform adds several ports to manipulate work modes and the main port of the platform is #3D00. [In more details it is desribed in wiki.](https://github.com/raydac/zxpoly/wiki/Short-description-of-ZX-Poly-platform)

# F.A.Q.
## Is there a hardware implementation?
The Idea of the Platform was born in 1994 and it was too late to invest money and resources into hardware implementation because in Russia the sunset of ZX-Spectrum was in 1992-1993. I had some discussions with management of the Peters Plus company (the main developer of the Sprinter platform, the first world FPGA based ZX-Spectrum clone) but their platform was single-CPU one and the on-board FPGA was too weak to emulate a multi-Z80 platform.

## Does the platform need a custom OS?
Not, it works under standard ZX-Spectrum 128 OS + TR-DOS. On start ZX-Poly even starts only one Z80 and three others in sleep mode and it is no so easy to detect difference in work with regular ZX-Spectrum 128 (I didn't find).

## What is difference with Spec256?
Both ZX-Poly and Spec256 work as [SIMD](https://en.wikipedia.org/wiki/SIMD) computers but ZX-Poly is based on 4 Z80 "real" CPUs and Spec256 has the only virtual 64 bit Z80 CPU under the hood, I tried to convert some games from Spec256 to ZX-Poly but it was impossible because Spec256 is much more tolerant for damage of execution code and desynchronization. Also ZX-Poly allows its CPUs to work separately in real hardware multi-threading mode, the platform even have some primitive semaphore mechanism (of course it doesn't make the platform very easy to be implemented). So that the main difference - ZX-Poly doesn't have magic and fantastic virtual devices under the hood and SIMD is one of possible modes for ZX-Poly but the mode is only possible for Spec256.

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
![Standard ZX screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/atw_standard.png)

## ZX-Poly 256x192 (16 colors per pixel)
The Mode doesn't use attributes and each pixel can have color from 16 color palette. Also there is modification of the mode which uses attributes from CPU0 module to mask screen areas by INK color if it is the sime as PAPER color in attribute.

[TRD disk with the example can be downloaded from here, the game has been partly colorized](https://raydac.github.io/downloads/zips/atw1_partly_colorized.trd)
![ZXPoly256x192 screenshot](https://raw.githubusercontent.com/raydac/zxpoly/master/docs/screenshots/atw_zxpoly.png)   

## ZX-Poly 512x384 (2 colors per pixel placed in chess order)
The Mode uses attributes but places pixels in chess order.

[TRD disk with the example can be downloaded from here](https://raydac.github.io/downloads/zips/zxw26_adapted_for_zxpoly.trd)
![ZXPoly512x384 screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxw_zxpoly512x384.png)

# Adaptation of games

To adapt old games, I have developed special utility called ZX-Poly Sprite corrector, which can be found in releases. It is a very easy editor which shows images in data blocks and allows to redraw them. It supports import from Z80 and SNA snapshots and as the result it generates ZXP snapshots.
![ZXPoly Sprite Corrector screenshot](https://github.com/raydac/zxpoly/blob/master/docs/zxpoly_sprite_editor.png)

## "Official Father Christmas" (1989)

On Christmas 2017 I made some adaptation of the old game ["Official Father Christmas" (1989)](http://www.worldofspectrum.org/infoseekid.cgi?id=0003493) for ZX-Poly. Of course not all was smoothly, as you can see in the game, some elements of third level were not colorized because their colorization broke game process and made desynchronize CPU modules, it looks like that there is some optimization in graphics processing for those elements and presented some check for empty areas to optimize speed.
![Official Father Christmas GIF](https://raw.githubusercontent.com/raydac/zxpoly/master/adapted/OfficialFatherChristmas/movie.gif)
