use std::io::{BufRead, BufReader};
use std::fs::File;
use std::collections::HashMap;

fn main() {
    let input = "wind motor hawk head";
    let s_input = sanitize(input);

    let map = read_songs();
    println!();
    println!("Input {}", input);
    println!("Search value: {}", s_input);
    println!("Found: {:?}", map.get(&s_input));
}

fn read_songs() -> HashMap<String, String> {
    let f = File::open("songs.txt").expect("no such file");
    let file = BufReader::new(&f);
    return file.lines()
        .map(|v| {
            let z = v.expect("could not parse line");
            println!("{}", z);
            let k = sanitize(&z);
            (k, z)
        }).collect();
}

// to upper case
// split into char array
// order array
// collect all alphabetical chars
fn sanitize<'a>(input: &str) -> String {
    let mut chars: Vec<char> = input.to_uppercase().chars().collect();
    chars.sort_by(|a, b| a.cmp(b));

    let mut string = String::new();
    for c in chars {
        if c.is_alphabetic() {
            string.push(c);
        }
    }
    return string.to_string()
}


#[cfg(test)]
mod test {
    use super::*;

    #[test]
    fn scramble_test() {
        assert_eq!( sanitize("Hawkwind - Motorhead"), "AADDEHHIKMNOORTWW"  );
    }

    #[test]
    fn read_map_test() {
        assert_eq!( read_songs().len(), 3);
    }
}
