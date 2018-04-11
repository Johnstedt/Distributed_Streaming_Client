#!/bin/bash
counter=1

for y in {50..500..50}
do
	java -jar client.jar $1 1000 60 $y 6 6
	counter=$((counter+1))
done
