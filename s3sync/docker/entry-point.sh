#!/bin/bash

JAVA_OPS=${JAVA_OPS:--server -Xmx4096m -Xms1024m}
java $JAVA_OPS -jar /app/application.jar