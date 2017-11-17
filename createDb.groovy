/**
 * Timings without indexes:
 *
 * **) i5-3210M CPU @ 2.50GHz | 16 Gb
 *
 * Batch size = 20000
 * Data set = 261_463
 * Total time = 11.33 (**
 *
 * Batch size = 10000
 * Data set = 261_463
 * Total time = 11.43 (**
 *
 * Batch size = 5000
 * Data set = 261_463
 * Total time = 12.29 (**
 *
 * Batch size = 20000
 * Data set = 23_528_878
 * Total time = 868.22 (**
 *
 * Batch size = 50000
 * Data set = 23_528_878
 * Total time = 834.72 (**
 *
 */
@Grapes([
        @Grab(group = 'org.xerial', module = 'sqlite-jdbc', version = '3.7.2'),
        @Grab(group = 'com.opencsv', module = 'opencsv', version = '4.0'),
        @GrabConfig(systemClassLoader = true)
])
import com.opencsv.CSVReaderBuilder
import com.opencsv.CSVReader
import com.opencsv.RFC4180Parser
import com.opencsv.RFC4180ParserBuilder
import groovy.sql.Sql
import java.text.Normalizer
import java.util.regex.Pattern

String.metaClass.green = { -> "\033[32;1m" + delegate + "\033[0m" }
String.metaClass.red = { -> "\033[31;1m" + delegate + "\033[0m" }

start = System.nanoTime()

songsLocation = "/home/jeroen/apps/song-data/extract/tracks-small.csv"
batchSize = 50000

printLine "starting application"
pattern = Pattern.compile("[^\\p{ASCII}]")
sql = Sql.newInstance("jdbc:sqlite:songs.db", "org.sqlite.JDBC")

importSongsIntoSqlDB()
System.exit(0)

def importSongsIntoSqlDB() {
    printLine "initialized application"

    this.recreateDb()
    this.readCsvIntoDb()

    sql.rows("select count(*) as cnt from songs").each {
        printLine "inserted ${it.cnt} songs"
    }
    printLine "done!"

    println "top 5:"
    sql.eachRow("select * from songs limit 5") {
        println("song=${it.song},artist=${it.artist}, scramble= ${it.scramble}")
    }
}

def readCsvIntoDb() {
    File file = new File(songsLocation)
    if (!file.exists()) {
        println "Could not find input file at [${file.getAbsolutePath()}]".red()
    }
    RFC4180Parser rfc4180Parser = new RFC4180ParserBuilder()
            .withSeparator('\t' as char).build()
    CSVReader reader = new CSVReaderBuilder(new FileReader(file))
            .withCSVParser(rfc4180Parser).build()
    counter = 0
    String[] nextLine
    def batch = []
    while ((nextLine = reader.readNext()) != null) { // stream it line for line
        if (nextLine.length != 2) {
            println "Cannot read line ${counter} - skipping value ${nextLine}".red()
            continue
        }
        def artist = nextLine[0]
        def song = sanitizeSong(nextLine[1])
        def scramble = this.scramble(artist, song)
        batch[counter % batchSize] = [artist, song, scramble]
        counter++
        if (counter % batchSize == 0) {
            insertRows(batch)
            batch = []
            printf "%5d\r", counter
        }
    }
    if (batch.size() > 0) {
        printLine "last ${batch.size()} pieces..."
        insertRows(batch)
    }
    printLine "insert all data"
    printLine "done vacuum"

}

def recreateDb() {
    // 12 mb, 4.4 mb zipped for 100_000 records - inserted in 8.67 sec
    // 56 mb, 23 mb zipped for 500_000 records - inserted in 47.20 sec
    // 230 mb, 89 mb zipped for 2_000_000 records - inserted in 274.50 sec
//    sql.execute("DROP TABLE if exists artist")
//    sql.execute("DROP TABLE if exists song")
//    sql.execute("DROP TABLE if exists songs")
//    sql.execute """CREATE TABLE artist (
//        id INTEGER PRIMARY KEY AUTOINCREMENT,
//        artist VARCHAR NOT NULL UNIQUE
//    );"""
//    sql.execute """CREATE TABLE song (
//        id INTEGER PRIMARY KEY AUTOINCREMENT,
//        song VARCHAR NOT NULL UNIQUE
//    );"""
//    sql.execute """CREATE TABLE songs (
//        id INTEGER PRIMARY KEY AUTOINCREMENT,
//        artist INTEGER NOT NULL,
//        song INTEGER NOT NULL,
//        scramble VARCHAR NOT NULL,
//        FOREIGN KEY (song) REFERENCES song(id),
//        FOREIGN KEY (artist) REFERENCES artist(id)
//    ); \n"""


    // 12 mb, 4.4 mb zipped for 100_000 records - inserted in 6.72 sec
    // 56 mb, 23 mb  zipped for 500_000 records - inserted in 48.27 sec
    // 231 mb, 89 mb  zipped for 2_000_000 records - inserted in 278.51 sec
    sql.execute("DROP TABLE if exists songs")
    sql.execute """CREATE TABLE songs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        artist VARCHAR NOT NULL,
        song VARCHAR NOT NULL,
        scramble VARCHAR NOT NULL,
        CONSTRAINT un UNIQUE (artist, song) ON CONFLICT REPLACE
    ); \n"""

    // without index : 7.2 mb, zipped 3.0 mb
    sql.execute "CREATE INDEX idx_songs_scramble ON songs(scramble);"
    printLine "recreate db"
}

def insertRows(batch) {
    sql.execute 'BEGIN TRANSACTION;'
    for (row in batch) {
        if (row.size() != 3) {
            println "Error writing $row".red()
        }
        params = [row[0], row[1], row[2]]
        sql.execute "INSERT INTO songs (artist, song, scramble) values (?,?,?); ", params
    }
    sql.execute "COMMIT;"
}

def printLine(line) {
    println "${(System.nanoTime() - start) / 1_000_000_000} - $line".green()
}

def sanitizeSong(song) {
    return song.replaceAll("\\(.*?\\)", "")
//    return song
}

def scramble(artist, title) {
    String t = artist + title
    // filter all non-letters
    StringBuffer r = new StringBuffer()
    for (int i = 0; i < t.length(); i++) {
        if (Character.isLetter(t.charAt(i)))
            r.append(t.charAt(i).toUpperCase())
    }
    // normalize all letters: Motörhead -> Motorhead
    String onlyLetters = r.toString()
    onlyLetters = Normalizer.normalize(onlyLetters, Normalizer.Form.NFD)
    onlyLetters = pattern.matcher(onlyLetters).replaceAll("")
    // sort the result
    char[] c = onlyLetters.toCharArray()
    Arrays.sort(c)
    return new String(c)
}