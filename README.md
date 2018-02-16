# ilse
irclog search engine

## building

* `grep -R yoursite.yourTLD .` and replace those
* since you want TLS: apply the nginx config in web/nginx.config
* copy web/index.php to somewhere in your nginx htdocs
* `mvn package`

## running

`java -Xmx1g -jar target/ilse-0.9-dev-jar-with-dependencies.jar $HOME/.znc/users/$(whoami)/moddata/log /dev/shm/ilse-idx 9002 memes`

| | |
|-|-|
| `java -Xmx1g` | lucene recommends 1GB RAM for large index jobs |
| `-jar target/ilse-0.9-dev-jar-with-dependencies.jar` | can i make this shorter somehow |
| `$HOME/.znc/users/$(whoami)/moddata/log` | the znc logfile dir |
| `/dev/shm/ilse-idx` | where to put the index data |
| `9002` | local websocket port (nginx will proxy this to 9001) |
| `memes` | a certified strong passphrase |

## usage

some queries:
* `+(mixtape.moe my.mixtape.moe) -06daisushi +(http https)`
* `ts:[1518648300 TO 1518648310] ts:1518651290`

the following commands can be entered into the search field:
* `:upd` scans logs for new content and updates the index
* `:end` performs a shutdown

## todo

* reindex can be optimized to ~100x the speed by looking for missing days in descending order
* better ui
* better everything since this is the first time i touch java

