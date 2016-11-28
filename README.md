[![License GNU GPL3](https://img.shields.io/badge/license-GNU%20GPL%203-yellow.svg)](http://www.gnu.org/licenses/gpl.html)
[![Java 7.0+](https://img.shields.io/badge/java-7.0%2b-green.svg)](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
[![PayPal donation](https://img.shields.io/badge/donation-PayPal-red.svg)](https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2)
[![Yandex.Money donation](https://img.shields.io/badge/donation-Я.деньги-yellow.svg)](https://money.yandex.ru/embed/small.xml?account=41001158080699&quickpay=small&yamoney-payment-type=on&button-text=01&button-size=l&button-color=orange&targets=%D0%9F%D0%BE%D0%B6%D0%B5%D1%80%D1%82%D0%B2%D0%BE%D0%B2%D0%B0%D0%BD%D0%B8%D0%B5+%D0%BD%D0%B0+%D0%BF%D1%80%D0%BE%D0%B5%D0%BA%D1%82%D1%8B+%D1%81+%D0%BE%D1%82%D0%BA%D1%80%D1%8B%D1%82%D1%8B%D0%BC+%D0%B8%D1%81%D1%85%D0%BE%D0%B4%D0%BD%D1%8B%D0%BC+%D0%BA%D0%BE%D0%B4%D0%BE%D0%BC&default-sum=100&successURL=)


Introduction
============
It is not just a yet another ZX-Spectrum emulator, but the proof of concept for ZX-Poly platform. The ZX-Poly platform was developed by Igor Maznitsa in 1994 and it is a modification of the well-known ZX-Spectrum 128 platform, the main purpose of ZX-Poly is to remove the main graphic issue of ZX-Spectrum - the use of only 2 colours per 8x8 pixel blocks.  
p.s.  
_Of course there are another solutions for the ZX-Spectrum graphic approach but ZX-Poly is the only solution which could be implemented in hardware based on the element base of 80's, it doesn't contain any smart video cards and 64bit Z80 variants. It shows that we had only step to have multi-CPU home computers in the middle of 80's._

License
========
The Emulator and all its parts are published under [GNU GPL3 license](http://www.gnu.org/licenses/gpl.html). 

UI
=========================
![The Main Window of the Emulator](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/tapeloading.png)

Theory and structure of the ZX-Poly platform
==============================================
![ZXPoly test ROM screen](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxpolytestromscreen.png)
The Base of the platform is the theory that stable synchronous systems (without any inside random processes) which are based on the same element base (because different element base can be also source of random processes) by being started synchronously in the same time from the same state will have the same state in any point of time if all synchronous system get the same input signal states in the same time.
![Structure of ZXPoly](https://github.com/raydac/zxpoly/blob/master/docs/zxpolystruct.png)
ZX-Poly platform adds several ports to manipulate work modes and the main port of the platform is #3D00. [In more details it is desribed in wiki.](https://github.com/raydac/zxpoly/wiki/Short-description-of-ZX-Poly-platform) 

Supported formats
==================
 - Snapshots .Z80, .SNA, .ZXP (ZX-Poly snapshot format produced by the Sprite Editor from .Z80 snapshots and included into the project)
 - Tape .TAP (allows export to WAV file)
 - Disks .TRD, .SCL
 
Prebuilt versions
==================
The Latest prebuilt versions of the emulator and the sprite editor in Java JAR and Windows EXE formats can be downloaded from my [Google Drive folder](https://drive.google.com/open?id=0BxHnNp97IgMRSHUzREtwbUQtT28&authuser=0)  
By default it uses the embedded ZX-Poly Test ROM image for start, but you can pass through **File->Options->Active ROM** and select another SOS+TR-DOS 128 ROM source (from list of links) and after that reload the emulator, keep in mind that WorldOfSpectrum FTP works not very stable so I recommend prefer VirtualTRDOS as ROM source. After loading, the ROM will be cached on your machine for future usage.

#Videomodes
## Standard ZX-Spectrum 256x192 (2 colors per pixel in 8x8 block)
It is just regular ZX-Spectrum movde 256x192 with 2 attributed colors for 8x8 pixel block.
![Standard ZX screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/atw_standard.png)

## ZX-Poly 256x192 (16 colors per pixel)
The Mode doesn't use attributes and each pixel can have color from 16 color palette.

[TRD disk with the example can be downloaded from here, the game has been partly colorized](https://googledrive.com/host/0BxHnNp97IgMRSHUzREtwbUQtT28/atw1_partly_colorized.trd)
![ZXPoly256x192 screenshot](https://raw.githubusercontent.com/raydac/zxpoly/master/docs/screenshots/atw_zxpoly.png)

## ZX-Poly 512x384 (2 colors per pixel placed in chess order)
The Mode uses attributes but places pixels in chess order.

[TRD disk with the example can be downloaded from here](https://googledrive.com/host/0BxHnNp97IgMRSHUzREtwbUQtT28/zxw26_adapted_for_zxpoly.trd)
![ZXPoly512x384 screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxw_zxpoly512x384.png)

