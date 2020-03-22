#!/bin/sh
docker build -t esmartit/seen-devices-data-store:"$1" -t esmartit/seen-devices-data-store:latest .
exit