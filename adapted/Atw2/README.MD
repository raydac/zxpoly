Partly adapted version of AfterTheWar2 for ZX-Poly. Just colorized some sprites and added multi-CPU loader.

# Build process

It needs installdes DOSBox to use some tools to form TRD disk. Build process is executed through start `make.sh` script.

# Folders

## SRC

Contains original file and work project from ZX-Poly Sprite corrector.

## PLANES

Prepared ZX-Poly data for each CPU as separated HOBETA file.

## TOOLS

Additional tools needed to build project

## OTHERS

Additional files to be added to formed TRD disk

## TARGET

Created after build process, it contains created TRD file.


# Loading process
- loading from 24500 (#5FB4)
- starting from 48800 (#BEA0)