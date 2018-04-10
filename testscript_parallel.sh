#!/bin/bash
counter=1
for i in {1..10..1}
do
	for y in {1..10..1}
	do
		java -jar client.jar $1 1000 60 $y $i $i
		counter=$((counter+1))
	done
done
