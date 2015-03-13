Introduction
============
It is not just a yet another ZX-Spectrum emulator, but the proof of concept for ZX-Poly platform. The ZX-Poly platform was developed by Igor Maznitsa in 1994 and it is a modification of the well-known ZX-Spectrum 128 platform, the main purpose of ZX-Poly is to remove the main graphic issue of ZX-Spectrum - the use of only 2 colours per 8x8 pixel blocks.
_Of course there are another solutions of the graphic issue but ZX-Poly is the only solution which could be implemented in hardware based on the element base of 80's, it doesn't contain any smart video cards and 64bit Z80 variants. It shows that we had only step to have multi-CPU home computers in the middle of 80's._

License
========
The Emulator and all its parts are published under [GNU GPL3 license]. 

The Emulator Main window
=========================
![The Main Window of the Emulator](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/tapeloading.png)

Theory and structure of the ZX-Poly platform
==============================================
The Base of the platform is the theory that stable synchronous systems (without any inside random processes) which are based on the same element base (because different element base can be also source of random processes) by being started synchronously in the same time from the same state will have the same state in any point of time if all synchronous system get the same input signal states in the same time.
![Structure of ZXPoly](https://github.com/raydac/zxpoly/blob/master/docs/zxpolystruct.png)
ZX-Poly platform adds several ports to manipulate work modes and the main port of the platform is #3D00. [In more details it is desribed in wiki.](https://github.com/raydac/zxpoly/wiki/Short-description-of-ZX-Poly-platform) 

Supported formats
==================
 - Snapshots .Z80, .SNA
 - Tape .TAP (allows export to WAV file)
 - Disks .TRD, .SCL
 
Prebuilt versions
==================
 - [Java JAR version, start as java -jar zxpoly-emul-2.0-SNAPSHOT.jar](https://googledrive.com/host/0BxHnNp97IgMRSHUzREtwbUQtT28/zxpoly-emul-2.0-SNAPSHOT.jar)
 - [Windows version (needs installed Java), start as zxpoly_2.0-SNAPSHOT.exe](https://googledrive.com/host/0BxHnNp97IgMRSHUzREtwbUQtT28/zxpoly_2.0-SNAPSHOT.exe)
 
#Videomodes
## Standard ZX-Spectrum 256x192 (2 colors per pixel in 8x8 block)
It is just regular ZX-Spectrum movde 256x192 with 2 attributed colors for 8x8 pixel block.
![Standard ZX screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/atw_standard.png)

## ZX-Poly 256x192 (16 colors per pixel)
The Mode doesn't use attributes and each pixel can have color from 16 color palette.

[TRD disk with the example can be downloaded from here, the game has been partly colorized](https://googledrive.com/host/0BxHnNp97IgMRSHUzREtwbUQtT28/atw1_partly_colorized.trd)
![ZXPoly256x192 screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/atw_zxpoly.png)

## ZX-Poly 512x384 (2 colors per pixel placed in chess order)
The Mode uses attributes but places pixels in chess order.

[TRD disk with the example can be downloaded from here](https://googledrive.com/host/0BxHnNp97IgMRSHUzREtwbUQtT28/zxw26_adapted_for_zxpoly.trd)
![ZXPoly512x384 screenshot](https://github.com/raydac/zxpoly/blob/master/docs/screenshots/zxw_zxpoly512x384.png)

[GNU GPL3 License]:http://www.gnu.org/licenses/gpl.html
