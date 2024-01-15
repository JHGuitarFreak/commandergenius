#!/bin/sh

if [ -e src/src/patched.successfully ]; then
	exit 0
else
	if [ -e project/jni/application/src ]; then
		rm project/jni/application/src
	fi
	ln -s megamod project/jni/application/src
	./changeAppSettings.sh -u
	cd src
	./AndroidPreBuild.sh
fi
