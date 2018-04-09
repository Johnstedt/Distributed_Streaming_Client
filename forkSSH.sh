#!/bin/bash

path=$(pwd)
# '/Home/staff/jwestin/private/kandidat-exjobb/simulation;'
running='./testscript.sh'
labcomputers=('wall-e' 'ultron' 'tinman' 't-1000' 'mettaton' 'meka-nicke' 'iron-giant' 'bender' 'c-3po' 'r2-d2' 'robotman' 'gort' 'ed-209' 'chappie' 'dot-matrix' 'voldemort' 'weasley' 'sirius' 'hedwig' 'hermione' 'harry' 'hagrid' 'lucius' 'mad-eye' 'gryffindor' 'gilderoy' 'dobby' 'dolores' 'draco' 'unsigned' 'typedef' 'void' 'volatile' 'long' 'if' 'main' 'goto' 'return' 'short' 'extern' 'sizeof' 'angel' 'banshee' 'magneto' 'iceman' 'beast' 'cyclops' 'toad' 'wolverine' 'silverfox' 'storm')
labcomputers=( $(shuf -e "${labcomputers[@]}") )

args=$#
argarray=("$@")
noOfComputers=$1
if ((args != 1)); then
        echo "Wrong number of args [[noOfComputers] ..]"
        exit;
fi
ant
echo "Number of computers:$noOfComputers"
echo "Path:$path"
it=0
i=0
labNo=1
users=("jwestin" "c14jmt")

for u in "${users[@]}"
do 
    for ((i=0; i < $noOfComputers; i++))
    do
        until ping -c 1 "${labcomputers[$it]}" &> /dev/null
        do
            echo "Cant access ${labcomputers[$it]}"
        done
        echo "$i: ${labcomputers[$it]} ${u}"
        xterm +hold -e ssh "${labcomputers[$it]}" -t "echo hello;hostname;cd $path;$running $u >> ${u}_results.txt 2>> ${u}_error.txt" &
        let it++
    done    
done
wait


