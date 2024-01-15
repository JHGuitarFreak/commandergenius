#!/bin/bash

# Define the folder path
obj_path="project/obj"
libs_path="project/libs"
build_path="project/app/build"
update_settings=false

echo "Folder paths defined"

while getopts "ah:" OPT
do
    case $OPT in
        a) update_settings=true;;
        h)
            echo "Usage: $0 [-a]"
            echo "    -a:       run \"./changeAppsettings -u\""
            exit 0
            ;;
    esac
done
shift $(expr $OPTIND - 1)

# Check if the folder exists
if [ ! -d "$obj_path" ]; then
    echo "Error: Folder $obj_path does not exist."
else
    # Remove the folder and its contents
    echo "Removing $obj_path and its contents..."
    rm -r "$obj_path"

    echo "$obj_path removed!"
fi

# Check if the folder exists
if [ ! -d "$libs_path" ]; then
    echo "Error: Folder $libs_path does not exist."
else
    # Remove the folder and its contents
    echo "Removing $libs_path and its contents..."
    rm -r "$libs_path"

    echo "$libs_path removed!"
fi


# Check if the folder exists
if [ ! -d "$build_path" ]; then
    echo "Error: Folder $build_path does not exist."
else
    # Remove the folder and its contents
    echo "Removing $build_path and its contents..."
    rm -r "$build_path"

    echo "$build_path removed!"
fi

echo "Cleanup complete!"

if $update_settings; then
    echo "activating \"changeAppSettings.sh -u\""
    ./changeAppSettings.sh -u
fi