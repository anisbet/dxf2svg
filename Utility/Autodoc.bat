REM Creates javadoc information in an appropriate subdirectory.
REM If the doc directory exists it is deleted and replaced with a new one.
REM Date January 07, 2004
REM ##############################################
REM #    Warning: if source files missing old    #
REM #    documentation will be deleted but new   #
REM #    documentation will not be generated.    #
REM ##############################################

SET LIBCATDOC="libcatdocs"
SET SVGUNZIPDOCS="svgunzipdocs"

del /Q /F /S %SVGUNZIPDOCS%
del /Q /F /S %LIBCATDOC%

mkdir %SVGUNZIPDOCS%
mkdir %LIBCATDOC%

javadoc -version -author -d ./%SVGUNZIPDOCS% SvgUnZip.java -breakiterator
javadoc -version -author -d ./%LIBCATDOC% LibCatUtil.java -breakiterator