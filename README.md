![ZX-Poly logo](docs/zxpoly_git_logo.png)

[![License GNU GPL3](https://img.shields.io/badge/license-GNU%20GPL%203-yellow.svg)](http://www.gnu.org/licenses/gpl.html)
[![Java 21.0+](https://img.shields.io/badge/java-21.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-cyan.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![YooMoney donation](https://img.shields.io/badge/donation-Yoo.money-blue.svg)](https://yoomoney.ru/to/41001158080699)

# Changelog

__2.3.5 (SNAPSHOT)__

- the embedded JDK has been updated to 24.0.1+11
- added Low Pass Filter for sound output

__2.3.4 (07-dec-2024)__

- the minimum required JDK version has been raised to 21
- the embedded JDK has been updated to 23.0.1+13
- upgraded the JInput library to 2.0.10
- refactoring, fixes in Z80 CPU emulation

full [change log](changelog.txt)

__NB! The emulator has been developed as proof of concept of ZX-Poly and by default it starts in ZX-Poly mode, if you need ZX-Spectrum 128 mode then activate it through options!__

# Introduction

I am a ZX-Spectrum fan since 1989. ZX-Spectrum platform was very popular in the USSR. In april of 1994 I got some idea
how to resolve [attribute clash](https://en.wikipedia.org/wiki/Attribute_clash) through moving the platform from
mono-system to poly-system.

The idea was to start four ZX-Spectrum-128 in parallel with small modifications of graphics data. It didn't need any
changes in ROM and SOS but only total synchronization between CPUs and sharing of all control signals.

I can't say that the idea was too original one because I was inspired by the
book ["COMPUTER IMAGES" ("Understanding computer" Time-Life books)](https://youtu.be/rCkGOmcLSa0). There I found several
strings about the Pixar platform (developed by the Lucasfilm team) which processes each color component by dedicated
CPU. In ZX-Poly each CPU also processes dedicated color component (R,G,B,Y). Software no so often checks information
written into VRAM so that big part of games and utilities could be adapted just through edition of their graphic data.

# License

Both the concept and emulator and all its parts are published
under [GNU GPL3 license](https://www.gnu.org/licenses/gpl-3.0.html).


# Pre-built versions

![The Main Window of the Emulator](docs/screenshots/zxpoly234_summersanta2022.png)

| OS                                           | Download link                                                                                                                          | 
|----------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|
| ![Windows](assets/icons/win64x64.png)        | __[for Windows x64 (JRE included)](https://github.com/raydac/zxpoly/releases/download/2.3.4/zxpoly-emul-2.3.4-windows-amd64-jdk.zip)__ |
| ![OSX](assets/icons/macos64x64.png)          | __[for OSX x64 (JRE included)](https://github.com/raydac/zxpoly/releases/download/2.3.4/zxpoly-emul-2.3.4-macos-amd64-jdk.zip)__       |
| ![OSX Arm64](assets/icons/macosarm64x64.png) | __[for OSX Arm64 (JRE included)](https://github.com/raydac/zxpoly/releases/download/2.3.4/zxpoly-emul-2.3.4-macos-aarch64-jdk.zip)__   |
| ![Linux](assets/icons/linux64x64.png)        | __[for Linux x64 (JRE included)](https://github.com/raydac/zxpoly/releases/download/2.3.4/zxpoly-emul-2.3.4-linux-amd64-jdk.tar.gz)__  |
| ![Linux](assets/icons/appimage64x64.png)     | __[Linux AppImage x64 (JRE included)](https://github.com/raydac/zxpoly/releases/download/2.3.4/zxpoly-emul-2.3.4-x86_64.AppImage)__    |
| ![Java](assets/icons/java64x64.png)          | __[cross-platform JAR file](https://github.com/raydac/zxpoly/releases/download/2.3.4/zxpoly-emul-2.3.4.jar)__                          | 

Because the emulator is written in Java, its JAR file can be started just through command
line `java -jar zxpoly-emul-2.3.4.jar`   
By default it starts embedded ZX-Poly Test ROM image, but it can be replaced by ZX-128 SOS in __File->Options->Active
ROM__, the ROM will be downloaded from one of remote network resources.   
![Test-ROM](docs/screenshots/testromscr1.png)

# Supported snapshot formats

- Snapshots: .Z80, .SNA, .SZX, .ZXP (ZX-Poly snapshot format), ZIP (Spec256 with SNA inside), .ROM (ROM images), .PROM (
  ZX-Poly
  ROM images)
- Tape: .TZX (allows export as WAV), .TAP (allows export as WAV), .WAV
- Disks: .TRD, .SCL

# Support of game controllers

Since 2.0.5 the emulator supports game-pads and joysticks. It uses X,Y position and recognizing all buttons as Fire.
Mapping of game controller can be turned on through **Service->Game controllers**. I recommend restart emulator after
new controller connect because JInput works not well for such situation. For work with joystick, the emulator uses
library `jinput` so that it can work not for all host-platforms.

# Theory and structure of the ZX-Poly platform

![ZX-Poly test ROM screen](docs/block_scheme.png)    
The base of the platform is the theory that stable synchronous systems (without any inside random processes) built on
the same components (because different element base can be also source of random processes) by being started
synchronously in the same time from the same state have the same state in any point of time if all synchronous system
components get the same input signal states in the same time.   
![Structure of ZX-Poly](docs/zxpolystruct.png)
ZX-Poly platform adds several ports to manipulate work modes and the main port of the platform is
#3D00. [In more details it is desribed in wiki.](https://github.com/raydac/zxpoly/wiki/Short-description-of-ZX-Poly-platform)

# Supported video-modes
## Standard ZX-Spectrum 256x192 (2 colors per pixel in 8x8 block) (mode 0,1,2,3)
It is just regular ZX-Spectrum mode 256x192 with 2 attributed colors for 8x8 pixel block.   
![Standard ZX screenshot](docs/screenshots/atw_standard.png)

## ZX-Poly 256x192 (16 colors per pixel)
It is a family of video-modes and contain three sub-mode. All modes provide way for each pixel to get a color from standard ZX-Spectrum 16 color palette.

### ZX-Poly 256x192 STANDARD (mode 4)

It is just regular video mode without any masking. It just integrates pixel info from video-ram of all CPU modules and form 4 bit index in ZX-Spectrum palette.
[TRD disk with the example can be downloaded from here, the game has been partly colorized](adapted/Atw2/target/atw2.trd)   
![ZX-Poly256x192 screenshot](docs/screenshots/atw_zxpoly.png)

### ZX-Poly 256x192 EXTENSION MASK_INK+PAPER (6)

It works as previous one and provide way to for 4 bit index for pixel, but also it analyses INK and PAPER colors from CPU0 video-ram and if INK = PAPER then whole block is filled by the INK color. It is made to provide way to make compatibility with games hidding some visual elements in areas with same INK and PAPER colors.

### ZX-Poly 256x192 EXTENSION MASK_FLUSH+INK+PAPER (mode 7)

It is the most complex from the video-mode family. It analyses FLASH bit of each attribute from CPU0 video-memory and turn on ZX-Poly mode only for blocks where FLASH bit is ON. In the same time it analyses INK and PAPER attribute values and if they the same then whole block is filled by INK else form pixel colors as in standard ZX-Poly mode. All blocks where FLASH is OFF are processed as in standard ZX-Spectrum video-mode. FLASH is not working in the mode (but it is no so often feature in games). The video-mode provides way to colorize games with color dynamic indicators and duotone game-play areas.   
![ZX-Poly256x192Mode7_screenshot](adapted/FlyShark/flyshark_zxpoly_mode7.png)

## ZX-Poly 512x384 (2 colors per pixel placed in chess order) (mode 5)
The Mode uses attributes but places pixels in chess order.   
[TRD disk with the example can be downloaded from here](adapted/ZxWord/target/zxword.trd)   
![ZX-Poly512x384 animation](adapted/ZxWord/screenshots/zxpoly_zxword_mode5.gif)

# Adaptation of games

To adapt old games, I have developed special utility called ZX-Poly Sprite corrector, which can be found in releases. It
is a very easy editor which shows images in data blocks and allows to redraw them. It supports import from Z80 and SNA
snapshots and as the result it generates ZXP snapshots.   
![ZX-Poly Sprite Corrector screenshot](docs/zxpoly_sprite_editor.png)   
Its pre-built versions can be found on [the latest release page](https://github.com/raydac/zxpoly/releases/tag/2.3.4).

## "Official Father Christmas" (1989)
Original look of the game   
![Original game screen](adapted/OfficialFatherChristmas/originalgamescreen.gif)

On Christmas 2017 I made some adaptation of the old game ["Official Father Christmas" (1989)](http://www.worldofspectrum.org/infoseekid.cgi?id=0003493) for ZX-Poly (mode 6). Of course not all was smoothly, some elements of third level could not be colorized because their colorization broke game process and made CPU modules out of sync, it looks like that the level contains some optimization in graphics processing for those elements and presented some check for empty areas to optimize speed.   
Adapted game version in ZX-Poly emulator format can be downloaded [from here](adapted/OfficialFatherChristmas/OFCZXPOLY.zxp).   
![Official Father Christmas GIF](adapted/OfficialFatherChristmas/movie.gif)

## "Summer Santa Update 2022" (2022)

On Christmas 2024, I made an adaptation of the
game ["Summer Santa Update 2022" (2022)](https://spectrumcomputing.co.uk/entry/39005/ZX-Spectrum/Summer_Santa_2022_Update)
originally created
by [Paul Jenkinson](https://www.thespectrumshow.net/). This adaptation is optimized for the ZX-Poly emulator,
specifically leveraging its 256x192 video mode 4. However, both the adapted and original TZX versions of the game have a
limitation where gameplay is restricted to three sessions. This may be a bug in the original code.    
Adapted game version in ZX-Poly emulator format can be
downloaded [from here](adapted/SummerSanta2022/SummerSanta2022.zxp).  
![Summer Santa Update 2022 GIF](adapted/SummerSanta2022/SummerSanta2022ZxPoly.gif)

## "Adventures of Buratino" (1993)

I was playing with adaptation of the game for 512x384 (mode 5), program code works well but the game and without
adaptation is so colorful and detailed that big effect is not visible. Also it was impossible to adapt the main hero
sprites because the same sprites are used for both right and left walk through mirroring.   
Adapted game version in ZX-Poly emulator format can be
downloaded [from here](adapted/BuratinoAdventures/buratino_adventures.zxp).   
![Adventures of Buratino comparation](adapted/BuratinoAdventures/comparescr.png)

## "Flying Shark" (1987)

In november 2019, I made some adaptation of "Flying Shark" game for ZX-Poly 256x192 (mode 7). It shows possibility of
combination of standard ZX-Spectrum colorization (for game panels) and ZX-Poly colorization (for game field). Snapshot
for ZX-Poly emulator can be downloaded [from here](/adapted/FlyShark/flyshark.zxp).   
![Flying Shark animation](adapted/FlyShark/flyshark_video.gif)

## "Comando Quatro" (1989)

The colorized version prepared and provided by [@jotarp](https://github.com/jotarp)
> I'm the programmer of "Comando Quatro" (yes, 35 years ago) and I've recently discovered your emulator (TLDR; I've
> recently rejoined my old colleagues from Gamesoft and they even invite us to give talks about those
> years: https://www.youtube.com/watch?v=TKIFtYspv1U , in spanish).
> I have tried to adapt my game to "mode 4", colouring the graphics. It has been fun to meet again with that game that
> took me so many hours of sleep when I was only 20 years old.
> I think the most appropriate place to publish the result is here.
> I haven't added too many colour details. The main feature of the result is that... there is no more "colour clash".

Snapshot for ZX-Poly emulator can be downloaded [from here](/adapted/ComandoQuatro/ComandoQuatro.zxp).   
![Comando Quatro animation](adapted/ComandoQuatro/ComandoQuatro.gif)

## "Alien 8" (1985)

In 2021, during one evening I made adaptation of "Alien 8" game for ZX-Poly 256x192. Snapshot for ZX-Poly emulator can
be downloaded [from here](/adapted/Alien8/Alien8.zxp).   
![Alien 8 animation](adapted/Alien8/alien8_video.gif)

# Support of Spec256 emulation

Since 2.0.1 version, some restricted [Spec256 platform](http://www.emulatronia.com/emusdaqui/spec256/index-eng.htm)
emulation added. Because there is real difference in work of ZX-Poly emulator (which based on parallel CPUs)
and [Spec256](http://www.emulatronia.com/emusdaqui/spec256/index-eng.htm) (which based on CPU with 64bit virtual
registers), not all games work well, but many of them can be started and playable.   
![ScoobyDoo for Spec256](docs/screenshots/Spec256emulationScoobyDoo.png)

Known issues with emulation of listed games:

- __Knight lore__ wrongly colored picture
- __Bubbler__ losing colorization for some game elements

In ZX-Poly emulator working in Spec256 mode, some CPU registers are syncronized every iteration (by default PC, SP and
bits of F excluding C bit), but it works not for all games. In games which need another register set, there can be
provided special `zxpAlignRegs` parameter in CFG file situated in snapshot archive. For
instance `zxpAlignRegs=XxYyPSsHLb` means syncronization of IX,IY,PC,SP,H,L and B' registers.

# F.A.Q.
## Is there a hardware implementation?
No, I had some negotiations with ZX-Spectrum clone manufacturers in Russia in the middle of 90th but I agreed with them that it was too late for market.

## Does the platform need a custom OS?
No, it doesn't. ZX-Poly uses standard ZX-Spectrum 128 OS + TR-DOS. On start ZX-Poly even starts only one Z80 and three others in sleep mode and it is no so easy to detect difference in work with regular ZX-Spectrum 128 (I didn't find).

## What is difference with Spec256?
Both ZX-Poly and Spec256 work as [SIMD](https://en.wikipedia.org/wiki/SIMD) computers but ZX-Poly is based on 4 Z80 "real" CPUs and Spec256 has some mix of 8 bit + 64 bit virtual Z80 CPU under the hood, I made attempt to convert some games from Spec256 to ZX-Poly but attempt failed because Spec256 is much more tolerant for damage of execution code (it executes non changed program and only changed extra-graphical data is sent through virtual 64 bit registers, changed data blocks don't affect executing flow).

## Multi-threading
ZX-Poly is a normal multi-CPU system and can work as in SIMD mode (to be compatible with existing software) as in MIMD mode and in the mode all CPUs work independently. It has some primitive synchronization mechanism called STOP-ADDRESS which allows to align common start address for all CPUs, also CPU0 can get some approximate info about current executing addresses of other CPUs and can get even some signals through interruptions what makes possible to emulate some periphery (like sound devices) in slave CPUs. Each CPU has its own memory area in common heap but it is possible to override memory areas and even map them totally each other (in the case it will be just multi CPU ZX 128).


## Which software can be adapted for the platform?
Any software can be adapted but adpatation of games which don't have too complex sprite output and packed sprites, can be adpated just through edit of their graphics elements.
