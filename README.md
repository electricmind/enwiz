enwiz
=====

Wizard that creates English sentences

Build & Run
-------------------
This script rely on MongoDB. Create one and olace two configures
enwizdb.cfg and enwizauth.cfg into your home directory:

 - enwizdb.cfg contains varaibles dbname, port, user, password and host to connect
   database;

 - enwizauth.cfg contains variables username and password to grant access to load 
   text sample into database.

Then run following script: 

```sh
$ cd EnWiz
$ ./sbt
> container:start
> browse
```

Environment should contain variables JELASTIC_USERNAME and JELASTIC_PWD to deploy (with command deploy) 
script on jelastica cloud:

```
export JELASTIC_PWD="12345678"
export JELASTIC_USERNAME="user@example.org"
sbt package-war
sbt deploy
```

If `browse` doesn't launch your browser, manually open [http://localhost:8080/](http://localhost:8080/) in your browser.
