#!/bin/bash
set -e
cd "$(dirname "${BASH_SOURCE[0]}")/.."

while true
do
	stg=0
	while true
	do
		t=$(find src -name \*.java -printf '%T@\n' | sort -n)

		[[ $stg -eq 1 ]] &&
		{
			[[ "x$ot" == "x$t" ]] &&
				break

			ot=$t
			sleep 0.3
		}

		[[ $stg -eq 0 ]] &&
		{
			sleep 0.3
			[[ "x$ot" == "x$t" ]] &&
				continue

			ot=$t
			stg=1
		}
	done
	ot=$t

	head -c 10 /dev/zero | tr '\0' '\n'
	
	mvn package &&
	scripts/run.sh &&
	echo ok ||
	echo err
done

