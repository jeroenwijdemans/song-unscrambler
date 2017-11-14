
wip - i use this code to learn rust and kotlin 

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

application is in Android Kotlin, the PoC is done in rust

## one time setup: creating the dataset

### transform musicdb data to csv

The following steps show how to download the musicdb dataset, open it using docker, transform the data and export it to csv. 
We then have all artist song combinations.

- download database: https://mirrors.dotsrc.org/MusicBrainz/data/fullexport/20170628-001505/mbdump.tar.bz2

- extract into mbdump ` tar -xvjf mbdump.tar.bz2 -C ${PWD}/musicdb/input`
( this directory is not in git for (2G) of obvious reasons ... )

- download schema into `mbdump/input`: https://raw.githubusercontent.com/metabrainz/musicbrainz-server/master/admin/sql/CreateTables.sql

- enter the `${PWD}/musicdb` directory

- start a postgres container: `docker run -ti -u postgres --name pg -v ${PWD}:/datadump postgres:9.5`

- in another shell enter the container as postgres `docker exec -u postgres -ti pg bash`

- add user `createuser musicbrainz_user`

- create db `createdb -U postgres --owner=musicbrainz_user --encoding=UNICODE importtest`

- start psql

- alter user with superuser rigts (needed to export csv)
`alter role musicbrainz_user with superuser;`

- exit `\q`

- create tables and import tables
(See https://wiki.musicbrainz.org/History:Database_Installation)
```bash
psql -U musicbrainz_user importtest
```
and import sql: `\i /datadump/CreateTables.sql`

exit: `\q`

import data:
```bash
mkdir ../done
for t in * ; do \
	echo `date` $t ; echo "\\copy $t from ./$t" | psql -U musicbrainz_user importtest && mv $t ../done/ ; \
done
```
- enter psql again
```bash
psql -U musicbrainz_user importtest
```

- select the data into temp table

```sql
  SELECT my.* 
  INTO tmp 
  FROM (
    select ac.name as artist, t.name as track 
    from track t, artist_credit ac 
    where t.artist_credit = ac.id
  ) AS my; 
```
- export data to csv:
```postgresplsql
COPY tmp to '/tmp/tracks.csv' DELIMITER E'\t' QUOTE '"' CSV HEADER;
```
- login to container as root and move it to the share so its available on the host system

`mv /tmp/tracks.csv /datadir/`

the resulting file is about 800M 

### prepare sqlite database

Here use `createDb.groovy` script to read csv export, create a scrambled text and add it to an sqlite database.
    
To run this with test data:
    
```bash
head -1000 ${PWD}/musicdb/tracks.csv > tracks.csv
groovy ./createDb.groovy 
```


## application

- sanitize scrambled word.

- find sanitized word in table

- return song

