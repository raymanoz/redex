@echo off

rem See: https://github.com/raymanoz/redex

set mypath=%~dp0
set config_file=%1
set output_dir=%2

java -Xmx4g --illegal-access=deny -Djava.library.path=%mypath% -jar %mypath%/redex.jar %config_file% %output_dir%