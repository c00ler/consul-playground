#!/usr/bin/env bash

docker run --rm -it -p 8400:8400 -p 8500:8500 --name=dev-consul consul
