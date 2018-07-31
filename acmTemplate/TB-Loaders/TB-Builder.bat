call ..\..\LB-software\ACM-install\Ensure-ACM-UpToDate.bat
pushd %USERPROFILE%\LiteracyBridge\ACM\software
java -cp acm.jar;lib\* org.literacybridge.acm.tbbuilder.TBBuilder %*
popd
