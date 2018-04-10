#!/bin/bash
counter=1
for y in {20..500..10}
do
	java -jar client.jar $1 1000 60 $y $i $i
	counter=$((counter+1))
done
