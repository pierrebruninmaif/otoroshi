#!/bin/sh

LOCATION=`pwd`

docker login -u $DOCKER_USER -p $DOCKER_PASSWORD maif-docker-docker.bintray.io

cd $LOCATION/docker/build
docker build --no-cache -t otoroshi .
docker tag otoroshi maif-docker-docker.bintray.io/otoroshi
docker push maif-docker-docker.bintray.io/otoroshi

cd $LOCATION/docker/otoroshicli
docker build --no-cache -t otoroshicli .
docker tag otoroshicli maif-docker-docker.bintray.io/otoroshicli
docker push maif-docker-docker.bintray.io/otoroshicli

cd $LOCATION


