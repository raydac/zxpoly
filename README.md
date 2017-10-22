[![License GNU GPL3](https://img.shields.io/badge/license-GNU%20GPL%203-yellow.svg)](http://www.gnu.org/licenses/gpl.html)
[![Java 7.0+](https://img.shields.io/badge/java-7.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](http://yasobe.ru/na/iamoss)


![ZX-Poly logo](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxpoly_logo.png)

# Introduction
It is not just a yet another ZX-Spectrum emulator, but the proof of concept of the ZX-Poly platform. The ZX-Poly platform was developed by Igor Maznitsa in Summer 1994 and it was not something absolutely new one but just a hardware modification of the old well-known ZX-Spectrum 128 platform (it even didn't  provide or require any new operating system and could use the ZX-Spectrum SOS), the main purpose of the modification was to remove the main graphic issue of ZX-Spectrum (color clashing) and save the same speed of graphic processing without injecting any extra GPU into the system with saving back-compatibility with the original platform.

# License
The Emulator and all its parts are published under [GNU GPL3 license](http://www.gnu.org/licenses/gpl.html). So that it is absolutely free for non-commercial use.

# UI
![The Main Window of the Emulator](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/tapeloading.png)

# Theory and structure of the ZX-Poly platform
![ZXPoly test ROM screen](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxpolytestromscreen.png)    
The Base of the platform is the theory that stable synchronous systems (without any inside random processes) which are based on the same element base (because different element base can be also source of random processes) by being started synchronously in the same time from the same state will have the same state in any point of time if all synchronous system get the same input signal states in the same time.
![Structure of ZXPoly](https://github.com/raydac/zxpoly/blob/master/docs/zxpolystruct.png)
ZX-Poly platform adds several ports to manipulate work modes and the main port of the platform is #3D00. [In more details it is desribed in wiki.](https://github.com/raydac/zxpoly/wiki/Short-description-of-ZX-Poly-platform)

# Supported formats
 - Snapshots .Z80, .SNA, .ZXP (ZX-Poly snapshot format produced by the Sprite Editor from .Z80 snapshots and included into the project)
 - Tape .TAP (allows export to WAV file)
 - Disks .TRD, .SCL

# A Prebuilt versions
The Latest prebuilt versions of the emulator and sprite editor can be downloaded from [the release page](https://github.com/raydac/zxpoly/releases).   
By default it uses the embedded ZX-Poly Test ROM image for start, but you can pass through **File->Options->Active ROM** and select another SOS+TR-DOS 128 ROM source (from list of links) and after that reload the emulator, keep in mind that WorldOfSpectrum FTP works not very stable so I recommend prefer VirtualTRDOS as ROM source. After loading, the ROM will be cached on your machine for future usage.

# Supported videomodes
## Standard ZX-Spectrum 256x192 (2 colors per pixel in 8x8 block)
It is just regular ZX-Spectrum movde 256x192 with 2 attributed colors for 8x8 pixel block.
![Standard ZX screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/atw_standard.png)

## ZX-Poly 256x192 (16 colors per pixel)
The Mode doesn't use attributes and each pixel can have color from 16 color palette.

[TRD disk with the example can be downloaded from here, the game has been partly colorized](https://raydac.github.io/downloads/zips/atw1_partly_colorized.trd)
![ZXPoly256x192 screenshot](https://raw.githubusercontent.com/raydac/zxpoly/master/docs/screenshots/atw_zxpoly.png)

## ZX-Poly 512x384 (2 colors per pixel placed in chess order)
The Mode uses attributes but places pixels in chess order.

[TRD disk with the example can be downloaded from here](https://raydac.github.io/downloads/zips/zxw26_adapted_for_zxpoly.trd)
![ZXPoly512x384 screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxw_zxpoly512x384.png)
