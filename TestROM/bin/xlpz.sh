#!/bin/bash

fullfile=$1

xlpcdir=$(dirname "${BASH_SOURCE[0]}")
xlpcdir=$(realpath "${xlpcdir}")


if [ -e "$xlpcdir/xlpz.exe" ]
then

    if [ -e "$fullfile" ]
    then
        DIR=$(dirname "$fullfile")
        FILENAME=$(basename -- "$fullfile")
        EXT="${FILENAME##*.}"
        FILENAME="${FILENAME%.*}"
        TARGET=$(echo "$FILENAME" | tr a-z A-Z).LPZ

	echo packing $FILENAME.$EXT to $TARGET

	dosbox -c "cycles max" -c "MOUNT E $xlpcdir" -c "MOUNT D $DIR" -c "d:" -c "e:\xlpz.exe $FILENAME.$EXT" -c "exit"

       if [ -e "$DIR/$TARGET" ]
	 then
	 echo ok
    	 exit 0
	else
    	 echo Can\'t pack
    	 exit 1
	fi

    else
	echo Could not find file $fullfile
	exit 1
    fi
else
    echo Can\'t find xlpz.exe
    exit 1
fi