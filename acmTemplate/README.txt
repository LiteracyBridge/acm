Template for creating a new ACM.

Copy this directory tree as the new ACM-WHATEVER. The batch files will determine
  the project based on their working directory.

From time to time the batch files in TB-Loaders may be updated. There is a shell
  script, TB-Loaders/deploy.sh, that will copy the updated versions to any ACMs
  present on this computer.

Edit accesslist.txt to add whatever new users are desired.

Edit category.whitelist to control which categories from the taxonomy are shown
  to users, and are available to assign to messages.

Edit properties.config and change 'HAS_OLD_TBS=FALSE' to '...=TRUE' if 
  appropriate. (This controls whether a TB-Loader icon for old style Talking Books
  is added to the desktop.)

Share the ACM directory with users in Dropbox. On the users' computers, sync with
  Dropbox and then run Install-ACM.bat.

Copy the necessary languages from ACM-languages to TB-Loaders/TB_Options/languages.
  Edit the config.properties file to include the languages.

Create the community directories inside TB-Loaders/communities. Every community
  should have a directory like this:
        LAMBUSSIE/
        ├── languages
        │   └── ssl
        │       └── 10.a18
        └── system
            └── ssl.grp
 
When it is time to create a Deployment, you will probably need to create a file
  to list the Subject Categories for the Deployment. To do so, first determine
  the Subject Categories to be included in the Deployment. Create a file with
  a name that is suggestive of those Subject Categories, and place it in
  TB-Loaders/TB_Options/activeLists. You may use the existing
  Health_Gender_Farming.txt file as a guide. Note that the file's name contains
  no spaces; your new file's name should also contain no spaces.

After the deployment is created, go to the users' computers, and go to the 
  TB-Loaders directory, and run UPDATE-TB-LOADER.bat.
