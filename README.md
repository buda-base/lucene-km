# Lucene analyzer for Khmer

(Unicode, old orthography & Middle Khmer, Pali written in Khmer script, transliteration)


## Building from source

First, make sure the submodule is initialized (`git submodule init`, then `git submodule update` from the root of the repo)

The base command line to build a jar is:

```
mvn clean compile
```

The following options alter the packaging:

- `-DperformRelease=true` signs the jar file with gpg

## Previous work
- https://github.com/wikimedia/search-extra/tree/master/extra-analysis-khmer
- https://arxiv.org/abs/1703.02166
- http://www.unicode.org/versions/Unicode13.0.0/ch16.pdf

