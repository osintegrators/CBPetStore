# Application - Petstore Java EE 6

## Summary

This application is an implementation of the Yet Another Petstore project written by [Andonio Goncalves] (http://antoniogoncalves.org/). This project was written to remove the Apache Derby database and to use Couchbase 2.0 as the database for the project. The original project has [instructions] (http://antoniogoncalves.org/2012/06/25/yet-another-petstore/) for getting set up and working. Those instructions will work for this project as well. The only difference will be the Couchbase setup portion.

## Couchbase setup
* Download Couchbase server from [http://www.couchbase.com/download] (http://www.couchbase.com/download) (This writing uses 2.0)
* Install the server per the instructions for your platform directly underneath the Operating System name
* Open (or keep open the console) and click on Data Buckets
* Create a new Couchbase bucket and name it whatever you would like as well as change the RAM Quota to what you would like.
* On the command line run, browse to the directory of the root of this project.
* Run the following command to restore a blank Bucket to the Bucket you just created. The bucket source must be petstore and it is case sensitive
	
	cbrestore ./couchbasedb http://host:8091 -u Administrator -p password --bucket-source=petstore (--bucket-destination=petstore_new)
	
* Deploy the project as per the original instructions

