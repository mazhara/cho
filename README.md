# Course

[rockthejvm.com](https://rockthejvm.com/).

## Run

- We need the database to be running, so make sure you have it active or run docker-compose up in the db directory.
- We need the Application in the server module to run, which you can start either in IntelliJ/Metals or in SBT.
- We need to compile the frontend, so open an SBT console, run project app, then run ~fastOptJS to continuously compile the Scala code to JS.
- We need to serve the resulting HTML and JS, so in the root of the app directory, run npm run start
- After this, navigate to `http://localhost:1234` and you should see the list of all the jobs in the database
displayed on the front page. True, they’re just regular strings, but you can now show them with any sort of fancy UIs,
with nice layouts and CSS.

### Server

```
sbt -> server/run
```

### Front

```
sbt -> ;fastOptJS
cd app -> npm run start
```

### Deployment

1. Build backend archive using `sbt-native-packager` plugin. 

```shell
sbt server/packageZipTarball 
```
This will produce a `server-0.1.0-SNAPSHOT.tgz` file inside `./server/target/universal`
with all libraries and a startup script.

2. Build frontend assets. `Parcel` will store them at `./app/dist`

```shell
sbt app/fullOptJS
cd app
npm run build
```

3. Upload to VPS server over SSH

```shell
scp -r ./server/target/universal/server-0.1.0-SNAPSHOT.tgz cho:~
scp -r ./app/dist cho:~
```

4. Replace assets on the server

```shell
tar -xf server-0.1.0-SNAPSHOT.tgz
rm server-0.1.0-SNAPSHOT.tgz

sudo rm -r /opt/cho
sudo mv server-0.1.0-SNAPSHOT /opt/cho

sudo rm -r /opt/frontend
sudo mv dist /opt/frontend
```

5. Restart backend service
```shell
sudo systemctl restart cho
```
