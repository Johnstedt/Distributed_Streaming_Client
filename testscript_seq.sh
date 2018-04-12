#!/bin/bash
counter=1

for y in {1..10..1}
do
	java -jar client.jar $1 1000 100 100 6 6
	counter=$((counter+1))
done
