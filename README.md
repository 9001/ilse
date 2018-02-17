# ilse

irc log search engine <sup>(actually znc log search engine but zlse sounds dumb)</sup>

java backend provides a websocket search interface

not related to https://nl.wikipedia.org/wiki/Ilse_(zoekmachine)



## building

the java backend:
* `mvn package`

the html client:
* `web/install.sh`

fair warning, you'll probably have to fiddle with the nginx config a bit



## running the backend

`java -Xmx1g -jar target/ilse-0.9-dev-jar-with-dependencies.jar $HOME/.znc/users/$(whoami)/moddata/log /dev/shm/ilse-idx 9002 memes`

| | |
|-|-|
| `java -Xmx1g` | lucene recommends 1GB RAM for large index jobs |
| `-jar target/ilse-0.9-dev-jar-with-dependencies.jar` | can i make this shorter somehow |
| `$HOME/.znc/users/$(whoami)/moddata/log` | the znc logfile dir |
| `/dev/shm/ilse-idx` | where to put the index data |
| `9002` | local websocket port (nginx will proxy this to 9001) |
| `memes` | a certified strong passphrase |

you probably wanna use a local folder rather than `/dev/shm` unless you share my problem of having way too much ram



## usage

some queries:
* `+(mixtape.moe my.mixtape.moe) -06daisushi +(http https)`
* `ts:[1518648300 TO 1518648310] ts:1518651290`

the following commands can be entered into the search field:
* `:upd` scans logs for new content and updates the index
* `:end` performs a shutdown



## todo

* nothing really

