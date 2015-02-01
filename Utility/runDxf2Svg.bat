@ECHO OFF
REM Written to simplify running Dxf2Svg. It determines the which is the correct share 
REM where the files are located so that everyone runs the same config.d2s without them
REM having to find, copy (or move) the config.d2s file and 

:BEGIN
FOR %%A IN (*.*) DO IF EXIST E:\DATA\CC130 SET DIRECTORY=E:
FOR %%A IN (*.*) DO IF EXIST F:\DATA\CC130 SET DIRECTORY=F:
FOR %%A IN (*.*) DO IF EXIST G:\DATA\CC130 SET DIRECTORY=G:
FOR %%A IN (*.*) DO IF EXIST I:\DATA\CC130 SET DIRECTORY=I:

:END
REM Now we have the drive letter that the files are on, let's identify where
REM the config.d2s is. NOTE: you cannot run specific files from this batch file.
SET CONFIGDIR=%DIRECTORY%\DATA\CC130\config.d2s
ECHO %CONFIGDIR%

CALL java dxf2svg.Dxf2Svg %CONFIGDIR%

PAUSE