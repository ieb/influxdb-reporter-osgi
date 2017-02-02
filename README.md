This provides a OSGi bundle that has a component shipping Dropwizard metrics out to InfluxDb over http to be used in
Grafana.


[![Build Status](https://travis-ci.org/ieb/influx-reporter-osgi.svg?branch=master)](https://travis-ci.org/ieb/influx-reporter-osgi)

# Quick Setup for the impatient

If you dont have a Grafana InfluxDB instanlled somewhere, you will need one to collect the data.

    On Ubuntu

    apt-get install influxdb grafana-data grafana

Then go to http://localhost:8083  create a database (eg metrics) and a user in that database (eg aemuser) with password
Then go to http://localhost:3000 login with admin admin, set the admin passwor and connect Grafana to the InfluxDB instnace (add a datasource)
Then configure some dashboards.

Install the bundle, configure the bundle (see below) and use.


# OSGi Properties for org.apache.sling.influxdb.CodehaleMetricsReporter

## host (default: 127.0.0.1)

The host where InfluxDB is running

## port (default: 8086)

Port where InfluxDB is running.

## secure (default:false)

If true https is used, otherwise http


## username (default: none)

The username to connect to InfluxDB with. This should be a non admin user dedicated to this purpose.

## password (default: none)

The password for the user in InfluxDB. This password will get stored plain text in the OSGi configuration so dont use
a valuable user.

## db (default: metrics)

The name of the Influx DB




