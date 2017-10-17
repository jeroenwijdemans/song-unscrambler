
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

### using just the dataset


- download database: https://mirrors.dotsrc.org/MusicBrainz/data/fullexport/20170628-001505/mbdump.tar.bz2

- extract into mbdump ` tar -xvjf mbdump.tar.bz2 -C ${PROJECT}/musicdb`
( this directory is not in git for (2G) of obvious reasons ... )

- download schema into mbdump: https://raw.githubusercontent.com/metabrainz/musicbrainz-server/master/admin/sql/CreateTables.sql


- start a postgres: `docker run -ti -v ${PWD}:/datadump postgres:9.5`

- in another shell start a postgres `docker exec -u postgres -ti pg bash`

- add user `createuser musicbrainz_user`

- create db `createdb -U postgres --owner=musicbrainz_user --encoding=UNICODE importtest`

- alter user with superuser rigts (needed to export csv)
`alter role musicbrainz_user with superuser;`

- create tables and import tables
(See https://wiki.musicbrainz.org/History:Database_Installation)
```bash
psql -U musicbrainz_user importtest
```
and import sql: `\i ./CreateTables.sql`

exit: `\q`

import data:
```bash
mkdir ../done
for t in * ; do \
	echo `date` $t ; echo "\\copy $t from ./$t" | psql -U musicbrainz_user importtest && mv $t ../done/ ; \
done
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

- exit '\q'

- export data to csv:

```postgresplsql
  psql \copy tmp to '/tmp/tracks.csv' csv header
```
- login to container as root and move it to the share

`mv /tmp/tracks.csv /datadir/tracks.csv`

### using the server

- start vagrant box 
- login using vagrant:vagrant, `ssh -p 2222 vagrant@localhost`
- check if the container runs otherwise start it `docker-compose up /home/vagrant/musicbrainz/musicbrainz-docker`
- enter the container as pg user `docker exec -u 999 -ti musicbrainzdocker_postgresql_1 bash`

...


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


docker create --name=musicbrainz \
-v <path to config >:/config \
-v <path to data >:/data \
-e PGID=<gid> -e PUID=<uid> \
-e BRAINZCODE=<code from musicbrainz> \
-e TZ=Europe/Amsterdam \
-e WEBADDRESS=localhost \
-p 5000:5000 \
linuxserver/musicbrainz



docker run -ti \
    -v ${PWD}/musicdb:/data \
    -p 5432:5432 \
    --name musicbrainz_postgres \
    musicbrainz_postgres 