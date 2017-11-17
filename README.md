
wip - i use this code to learn rust and kotlin 

# Song Unscrambler

Suppose you take an artist and song combination and scramble the letters. 
How would you retrieve the artist and song again without knowing that first step?

This application unscrambles that string into the artist and song.

## How

This application builds an android application. The application contains an input field for scrambled data. The scrambled data is ordered and fed to query of MusicBrainz data. The corresponding artist and song are displayed back on the screen.

__input__

we can type the input by hand. Or use the Google Vision API to decode a photo to text.

__database__

Using the MusicBrainz database it creates all possible artist - song combinations. 
All letters of these values are then put in alphabetical order. This becomes the scrambled value.

If we now do the same with the unknown value we are able to look it up directly and derive
the value.

__Other implementation__

Another implementation would use fuzzy search to find nearest neighbours.

# Steps

application is in Android Kotlin, the PoC is done in rust

## one time setup: creating the dataset

### transform musicdb data to csv

The following steps show how to download the musicdb dataset, open it using docker, transform the data and export it to csv. 
We then have all artist song combinations.

- download and extract database : 
( this directory is not in git for (2G) of obvious reasons ... )
```bash
VERSION=$(curl -s https://mirrors.dotsrc.org/MusicBrainz/data/fullexport/LATEST)
curl -o mbdump.tar.bz2 -s https://mirrors.dotsrc.org/MusicBrainz/data/fullexport/${VERSION}/mbdump.tar.bz2
mkdir -p ${PWD}/musicdb/input
tar -xvjf mbdump.tar.bz2 -C ${PWD}/musicdb/input
```
- download schema into `mbdump/input`:
`curl -o ${PWD}/musicdb/input/CreateTables.sql https://raw.githubusercontent.com/metabrainz/musicbrainz-server/master/admin/sql/CreateTables.sql`

and the manualy remove the toc CUBE on file 3272 (and the comma on the line before)

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
for t in * ; do \
	echo `date` $t ; echo "\\copy $t from ./$t" | psql -U musicbrainz_user importtest ; \
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
    select ac.name as artist, r.name as track 
    from recording r, artist_credit ac 
    where r.artist_credit = ac.id
  ) AS my; 
```
- export data to csv:
```postgresplsql
COPY tmp to '/tmp/tracks.csv' DELIMITER E'\t' QUOTE '"' CSV FORCE QUOTE *;
```
- login to container as root and move it to the share so its available on the host system

`mv /tmp/tracks.csv /datadump/`

the resulting file is about 800M 

### prepare sqlite database

Here use `createDb.groovy` script to read csv export, create a scrambled text and add it to an sqlite database.
    
To run this with test data:
    
```bash
head -1000 ${PWD}/musicdb/tracks.csv > tracks.csv
groovy ./createDb.groovy 
```

The initial dataset contains 23.528.878 records. Or 14.121.143 records when using dropping all between brackets: (...)
                                                     
However, for the first 10.000 records `select count(*) from songs;` yields 10.000
But `select count(*) from (select distinct artist,song from songs);` yields 68.000

So I replaced the `id` pk with a song,artist pk. And changed the `INSERT` into `INSERT OR REPLACE`

Together with the index on scramble slowed the creation of the database by a lot. However we now have
a fast (and smaller) read-only database.

## application

- sanitize scrambled word.

- find sanitized word in table

- return song


# Android App


## Firebase and Google Cloud

https://cloud.google.com/vision/docs/quickstart

### recognition

#### from android

https://cloud.google.com/vision/docs/reference/libraries#client-libraries-install-java

#### command line & testing

__upload__

Copy test file to google storage. This will be a photo from the phone's camera.

```bash
gsutil cp ~/Downloads/DSC_1419.JPG gs://song-unscrambler.appspot.com/test/DSC_1419.JPG
gsutil cp ~/Downloads/DSC_1421.JPG gs://song-unscrambler.appspot.com/test/DSC_1421.JPG
```

__enable api__

`https://console.cloud.google.com/flows/enableapi?apiid=vision.googleapis.com&redirect=https:%2F%2Fconsole.cloud.google.com&pli=1&authuser=1`

__query__


payload.json:

```json
{
  "requests": [
    {
      "image": {
        "source": {
          "imageUri": "https://storage.googleapis.com/song-unscrambler.appspot.com/test/DSC_1421.JPG"
        }
      },
      "features": [
        {
          "type": "TEXT_DETECTION",
          "maxResults": 2
        }
      ]
    }
  ]
}
```

find key here:

`https://console.cloud.google.com/apis/credentials?project=song-unscrambler&authuser=1`

fire curl command:

```
API_KEY_VISION=my-key
curl -X POST \
    -H "Content-Type: application/json" -d @./scripts/vision-api-request.json \
    https://vision.googleapis.com/v1/images:annotate?key=$API_KEY_VISION > scripts/vision-api-response.json
```
"

### add firebase

project is located here:
`https://console.firebase.google.com/u/1/project/song-unscrambler/settings/general/android:com.wijdemans.songunscrambler`

__download json__

`google-services.json` can be downloaded

__setup gradle__

Project-level build.gradle (<project>/build.gradle):

```
buildscript {
  dependencies {
    // Add this line
    classpath 'com.google.gms:google-services:3.1.0'
  }
}
```

App-level build.gradle (<project>/<app-module>/build.gradle):

```
// Add to the bottom of the file
apply plugin: 'com.google.gms.google-services'
```