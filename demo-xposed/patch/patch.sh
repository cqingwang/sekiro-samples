#!/bin/zsh
#!/bin/zsh -x
#read env

mod="../app/build/outputs/apk/release/sekiro.apk"
target=$1

echo "patch:$target"
echo "mod:$mod"
java -jar ./lspatch_manager_jar-v0.6-447.jar "$target" -m "$mod" -l 2 -o ./dist --force
echo "done"
