#java -cp apposcopy.jar:lib/gson-2.3.1.jar:lib/stamp.chord.jar com.apposcopy.util.Obfuscator ./samples/fse14/ADRD/09b143b430e836c513279c0209b7229a4d29a18c.json ./fse14-obs/

find ./samples/fse14-bak/ -name '*.json' | while read line; do
    java -cp apposcopy.jar:lib/gson-2.3.1.jar:lib/stamp.chord.jar com.apposcopy.util.Obfuscator $line ./fse14-obs/
done
