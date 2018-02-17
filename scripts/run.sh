#!/bin/bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/.."

java -jar target/ilse-0.9-dev-jar-with-dependencies.jar ~/.znc/users/$USER/moddata/log /dev/shm/ilse/ 9002 memes

