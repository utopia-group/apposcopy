#!/bin/bash

dir=astroid-dist

rm -rf $dir
ant clean ; ant

mkdir $dir
cp -r signatures $dir
cp apposcopy.jar $dir
cp approximate.sh $dir
cp -r bin $dir
cp build.xml $dir
cp -r datalog $dir
cp -r lib $dir
cp -r opb $dir
cp frequency.txt $dir
