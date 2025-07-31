#!/bin/bash

if [[ "$#" == 1 && ("$1" == "-h" || "$1" == "--help") ]]; then
    echo "Usage: ./start-tunnel.sh [cluster-address key]"
    echo "Description:"
    echo "  Script used to start a SSH tunnel."
    echo
    echo "Arguments:"
    echo "  cluster-address - the address of the cluster"
    echo "  key - the SSH private key (.pem)"
    echo "If no argument is supplied the script waits until the user input them :)"
    exit 1
elif [[ "$#" == 2 ]]; then
    cluster=$1
    key=$2
else
    read -p "Cluster Address (ec2-xxx-...): " cluster
    read -p "Key (.pem): " key
fi;

echo "Attempting to establish a ssh tunnel to $cluster with key $key"
if [ -f "$key" ]; then
    command="ssh -N -i '$key' hadoop@$cluster -L 8088:$cluster:8088 -L 19888:$cluster:19888 -L 20888:$cluster:20888"
    echo "Tunnel started... press CTRL+C to close"
    eval "$command"
else
    echo "File not found!"
fi
