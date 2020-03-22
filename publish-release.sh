#!/bin/sh
docker login -u $DOCKER_USER -p $DOCKER_PASS
docker push esmartit/seen-devices-data-store:"$1"
docker push esmartit/seen-devices-data-store:latest
helm package seen-devices-data-store --version "$1" --app-version "$1"
touch version.txt
echo "$1" >> version.txt
exit