# Application - Petstore Java EE 6 (Couchbase)

## Summary

This application is an implementation of the Yet Another Petstore project written by [Andonio Goncalves] (http://antoniogoncalves.org/). This project was written to remove the Apache Derby database and to use Couchbase 2.0 as the database for the project. The original project has [instructions] (http://antoniogoncalves.org/2012/06/25/yet-another-petstore/) for getting set up and working. Those instructions will work for this project as well. The only difference will be the Couchbase setup portion.

For our builds we use Ubuntu 12.10 server edition. We started from a clean install with only the updates applied, ssh installed (for copying files to and from the server) and Java (we used Oracle Java 1.7.0_17). Also, these instructions are adaptations from the Couchbase installation instructions on their download <a href="http://www.couchbase.com/download">site</a> as well as from the original project instructions in the <a href="https://github.com/agoncal/agoncal-application-petstore-ee6">readme</a> of the project on Github.

## Installing and Configuring Couchbase

Download the version of Couchbase for your os from http://www.couchbase.com/download and copy it somewhere on your server (for us, we use Ubuntu 64-bit). Then we need to log into the server and set up prerequisites. The only one needed is SSL:

    <code>sudo apt-get install libssl0.9.8</code>

After that we need to install Couchbase itself using dpkg:

    <code>sudo dpkg -i couchbase-server-{edition}-{version}.deb</code>

Alright, that is it. Couchbase has been installed. Now we just need to configure it. Open a browser and point it to the IP/DNS of the machine where you installed Couchbase and port 8091 (ie... http://localhost:8091). You should get a Couchbase screen with a setup button. Go ahead and click that button.

Leave the defaults for the disk storage and for starting a new cluster (we want to start a new cluster). The only thing to change (if you would like) is the memory. Keep in mind, that we need room for Glassfish later on, so adjust the memory appropriately and click Next.

We don't need to install the samples as we will be using a different bucket, so skip this screen by clicking next.

The next screen is the default bucket screen. We won't actually be using this bucket, but it is automatically created. You can leave the Bucket Type as Couchbase and then for the memory, lower it to half of the amount of memory allocated to the cluster (If you allocate all of the memory here, there will not be any memory for the petstore bucket we will create later). Uncheck the Enable Replicas box as we will only have one instance of Couchbase running for this project. The rest of the defaults will be fine, so click next.

On the next screen, choose whether or not you want software update notifications and whether or not you would like register your installation. You must accept the terms and conditions and then click next.

The next screen sets up the default administrator and password. Fill these in as appropriate for you and your environment and click Next.

This will bring you to the main console for Couchbase. It is now running and configured. The next thing we need to do is to add our petstore bucket for use in our project. Click on the link titled Data Buckets and then click Create New Data Bucket. This will bring up a modal window for creating the new bucket. For the name of the bucket, type petstore (case matters). Leave the type as Couchbase and you can leave the memory at it's level. It will use the rest of the memory in the Couchbase "cluster". Uncheck the Enable Replicas box and leave the rest of the items at their defaults and then click Create.

You will now have another Bucket called petstore (It will stay yellow for a minute and the go green when the setup is completed). Couchbase is now ready for us to start deploying our application (We will restore the petstore bucket in a minute once we have the code).

## Installing Glassfish

For Glassfish, we chose to go with an external implementation. We used Glassfish 3.1.2.2 from http://glassfish.java.net/downloads/3.1.2.2-final.html. We chose to use the zip installation since we are running on server with no gui. Change directory to /opt to install Glassfish there and then unzip Glassfish:

    <code>sudo unzip /path/to/glassfish-{version}.zip</code>

Type the following to start your Glassfish installation:

    <code>sudo /opt/glassfish3/bin/asadmin start-domain domain1</code>

## Getting and building the code

We first need to install Maven and and git onto our server as they don't exist yet.

    <code>sudo apt-get install maven git</code>

Then we need to get the code from the repository. Browse to the directory where you would like the code to be and type:

    <code>git clone https://github.com/osintegrators/CBPetStore.git</code>

Before we compile the code, let's restore the Couchbase database. The database backup files are in a folder called couchbasedb in the root of the project directory. From a command line change directories into the root of the project. Then we need to run the following command using the appropriate host, username and password. The source name must be specified exactly as it is and it is case sensitive.

    <code>/opt/couchbase/bin/cbrestore ./couchbasedb http://host:8091 -u Administrator -p password --bucket-source=petstore</code>

You will now have the restored petstore bucket with the appropriate structure. There is no data in there yet. That will not happen until we fire up the application for the first time. The next step is to build the application to get it ready for deployment. Run the following from the root of the project (might take a while to download all of the maven dependencies on a new machine):

    <code>mvn clean package</code>

Once that is complete, it's time to deploy to Glassfish

    <code>sudo /opt/glassfish3/bin/asadmin deploy target/applicationPetstore.war</code>

Now, your code is running. Go to a web browser and type in http://yourhost:8080/applicationPetstore and you can see it in action.
