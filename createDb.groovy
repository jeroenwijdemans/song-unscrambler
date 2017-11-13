/**
 * Batch size = 20000
 * Data set = 261_463
 * Total time = 11.33
 *
 * Batch size = 10000
 * Data set = 261_463
 * Total time = 11.43
 *
 * Batch size = 5000
 * Data set = 261_463
 * Total time = 12.29
 *
 */
@Grapes([
        @Grab(group = 'org.xerial', module = 'sqlite-jdbc', version = '3.7.2'),
        @Grab(group = 'net.sf.opencsv', module = 'opencsv', version = '2.3'),
        @GrabConfig(systemClassLoader = true)
])
import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import groovy.sql.Sql

import java.text.Normalizer
import java.util.regex.Pattern

String.metaClass.green = { -> "\033[32;1m" + delegate + "\033[0m" }
String.metaClass.red = { -> "\033[31;1m" + delegate + "\033[0m" }

start = System.nanoTime()
batchSize = 20000
printLine "starting application"
pattern = Pattern.compile("[^\\p{ASCII}]")
sql = Sql.newInstance("jdbc:sqlite:test.db", "org.sqlite.JDBC")

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
        println("id=${it.id},song=${it.song},artist=${it.artist}, scramble= ${it.scramble}, l=${it.size}")
    }
}

def readCsvIntoDb() {
    CSVReader reader = new CSVReader(new FileReader(new File("songs.txt")),
            '\t' as char, CSVParser.DEFAULT_ESCAPE_CHARACTER, CSVParser.DEFAULT_QUOTE_CHARACTER, 1)
    counter = 0
    String[] nextLine
    def batch = []
    while ((nextLine = reader.readNext()) != null) { // stream it line for line
        if (nextLine.length != 2) {
            println "Cannot read line ${counter} - skipping value ${nextLine}".red()
            continue
        }
        def scramble = this.scramble(nextLine[0], nextLine[1])
        batch[counter % batchSize] = [nextLine[0], nextLine[1], scramble, scramble.length()]
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
}

def recreateDb() {
    sql.execute("DROP TABLE if exists songs")
    sql.execute """CREATE TABLE songs (
        id INTEGER PRIMARY KEY AUTOINCREMENT,
        artist VARCHAR NOT NULL,
        song VARCHAR NOT NULL,
        scramble VARCHAR NOT NULL,
        size INT NOT NULL
    ); \n"""
    printLine "recreate db"
}

def insertRows(batch) {
    sql.execute 'BEGIN TRANSACTION;'
    for (row in batch) {
        if (row.size() != 4) {
            println "Error writing $row".red()
        }
        params = [row[0], row[1], row[2], row[3]]
        sql.execute "INSERT INTO songs (artist, song, scramble, size) values (?,?,?,?); ", params
    }
    sql.execute "COMMIT;"
}

def printLine(line) {
    println "${(System.nanoTime() - start) / 1_000_000_000} - $line".green()
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