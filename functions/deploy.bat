@echo off
echo Installing Firebase CLI if needed...
npm install -g firebase-tools

echo Logging in to Firebase...
firebase login

echo Installing dependencies...
cd %~dp0
npm install

echo Deploying functions...
firebase deploy --only functions

echo Done!
pause 