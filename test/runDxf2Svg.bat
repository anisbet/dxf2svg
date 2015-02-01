@ECHO OFF
REM Written to simplify running Dxf2Svg. It determines the which is the correct share 
REM where the files are located so that everyone runs the same config.d2s without them
REM having to find, copy (or move) the config.d2s file and 

:BEGIN
FOR %%A IN (*.*) DO IF EXIST E:\DATA\CC130 SET DIRECTORY=E:
FOR %%A IN (*.*) DO IF EXIST F:\DATA\CC130 SET DIRECTORY=F:
FOR %%A IN (*.*) DO IF EXIST G:\DATA\CC130 SET DIRECTORY=G:
FOR %%A IN (*.*) DO IF EXIST I:\DATA\CC130 SET DIRECTORY=I:
FOR %%A IN (*.*) DO IF EXIST Q:\DATA\CC130 SET DIRECTORY=Q:
FOR %%A IN (*.*) DO IF EXIST M:\DATA\CC130 SET DIRECTORY=M:

:END
REM Now we have the drive letter that the files are on, let's identify where
REM the config.d2s is. NOTE: you cannot run specific files from this batch file.
REM SET CONFIGDIR=%DIRECTORY%\DATA\CC130\config.d2s
REM ECHO %CONFIGDIR%
ECHO Using local directory...
REM CALL java dxf2svg.Dxf2Svg %CONFIGDIR%
CALL java dxf2svg.Dxf2Svg
PAUSE