#!/usr/bin/env bash

set -euo pipefail

while [ true ]
do
	./runtests.java
	if [ $? -eq 0 ]
	then
	  echo "The script ran ok"
#	  exit 0
	else
	  echo "The script failed" >&2
	  exit 1
	fi
done
