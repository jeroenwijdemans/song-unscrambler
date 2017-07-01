
wip - i use this code to learn rust

# Song Unscrambler

Suppose you take an artist and song combination and scramble the letters. 
How would you retrieve the artist and song again without knowing that first step?

This application unscrambles that string into the artist and song.

## How

Using the MusicBrainz database it creates all possible artist - song combinations. 
All letters of this value are then put in alphabetical order. This becomes the key.

If we now do the same with the unknown value we are able to look it up directly and derive
the value.

## Other implementation

Another implementation would use fuzzy search to find nearest neighbours.


# Steps

both steps done in rust

## one time

- download database: https://mirrors.dotsrc.org/MusicBrainz/data/fullexport/20170628-001505/mbdump.tar.bz2

- extract into musicdb ` tar -xvjf mbdump.tar.bz2 -C ${PROJECT}/musicdb`
( this directory is not in git for (2G) of obvious reasons ... )

- read artist and song and write it into own k,v table

## application

- sanitize scrambled word 

- find sanitized word in table

- return song

## start postgres

```bash
docker run -d \
    --name mdb \
    -p 5432:5432 \
    -v ${PWD}/musicdb:/dbimport \
    postgres
```

