# Course

[rockthejvm.com](https://rockthejvm.com/).

## Run

We need the database to be running, so make sure you have it active or run docker-compose up in the db directory.
We need the Application in the server module to run, which you can start either in IntelliJ/Metals or in SBT.
We need to compile the frontend, so open an SBT console, run project app, then run ~fastOptJS to continuously compile the Scala code to JS.
We need to serve the resulting HTML and JS, so in the root of the app directory, run npm run start
After this, navigate to http://localhost:1234 and you should see the list of all the jobs in the database displayed on the front page. True, they’re just regular strings, but you can now show them with any sort of fancy UIs, with nice layouts and CSS.

### Server

sbt -> project server -> run

### Front

sbt -> ;fastOptJS
cd app -> npm run start
