/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.sling.influxdb;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import metrics_influxdb.HttpInfluxdbProtocol;
import metrics_influxdb.InfluxdbReporter;
import org.apache.felix.scr.annotations.*;
import org.omg.PortableInterceptor.INACTIVE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * Created by boston on 02/02/2017.
 */
@Component(immediate = true , metatype = true)
@References(value = {@Reference(
        referenceInterface = MetricRegistry.class,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
        policy = ReferencePolicy.DYNAMIC,
        bind = "bindMetricRegistry",
        unbind = "unbindMetricRegistry")}
)
public class CodehaleMetricsReporterComponent {

    private static final Logger LOG = LoggerFactory.getLogger(CodehaleMetricsReporterComponent.class);

    private ScheduledReporter reporter;

    @Property(value = "13.94.149.199", description = "the hostname of the influx db server")
    public static final String INFLUXDB_SERVER = "host";
    @Property(intValue = 8086, description = "The port of the influx db server")
    public static final String INFLUXDB_PORT = "port";
    @Property(intValue = 5, description = "The period in seconds the reporter reports at")
    public static final String REPORT_PERIOD = "period";
    @Property(boolValue = false, description = "If true, user https, otherwise use http")
    public static final String INFLUXDB_SECURE_TRANSPORT = "secure";
    @Property(value = "", description = "The Username for the influx DB server.")
    public static final String INFLUXDB_USERNAME = "username";
    @Property(value = "", description = "Password for the influx db server.")
    public static final String INFLUXDB_PASSWORD = "password";
    @Property(value = "metrics", description = "InfluxDB name")
    public static final String INFLUXDB_DB = "db";

    private ConcurrentMap<String, CopyMetricRegistryListener> listeners = new ConcurrentHashMap<String, CopyMetricRegistryListener>();
    private MetricRegistry metricRegistry = new MetricRegistry();

    @Activate
    public void acivate(Map<String, Object> properties) throws IOException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        LOG.info("Starting InfluxDB Metrics reporter ");
        String server = (String) properties.get(INFLUXDB_SERVER);
        int port = (int) properties.get(INFLUXDB_PORT);
        int period = (int) properties.get(REPORT_PERIOD);
        boolean secure = (boolean) properties.get(INFLUXDB_SECURE_TRANSPORT);
        String username = (String) properties.get(INFLUXDB_USERNAME);
        String password = (String) properties.get(INFLUXDB_PASSWORD);
        String db = (String) properties.get(INFLUXDB_DB);
        reporter = InfluxdbReporter.forRegistry(metricRegistry)
                .protocol(new HttpInfluxdbProtocol(secure?"https":"http", server, port, username, password, db))
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MICROSECONDS)
                .tag("cluster", "CL01")
                .tag("client", "OurImportantClient")
                .tag("server", getHostName())
                .build();
        reporter.start(period, TimeUnit.SECONDS);
        LOG.info("Started InfluxDB Metrics reporter to {}://{}:{} username:{} password:**** {} ", new Object[]{secure ? "https" : "http", server, port, username, db});
    }

    @Deactivate
    public void deacivate(Map<String, Object> properties) {
        reporter.stop();
        reporter = null;
    }

    protected void bindMetricRegistry(MetricRegistry metricRegistry, Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null) {
            name = metricRegistry.toString();
        }
        CopyMetricRegistryListener listener = new CopyMetricRegistryListener(this.metricRegistry, name);
        listener.start(metricRegistry);
        this.listeners.put(name, listener);
        LOG.info("Bound Metrics Registry {} ",name);
    }
    protected void unbindMetricRegistry(MetricRegistry metricRegistry, Map<String, Object> properties) {
        String name = (String) properties.get("name");
        if (name == null) {
            name = metricRegistry.toString();
        }
        CopyMetricRegistryListener metricRegistryListener = listeners.get(name);
        if ( metricRegistryListener != null) {
            metricRegistryListener.stop(metricRegistry);
            this.listeners.remove(name);
        }
        LOG.info("Unbound Metrics Registry {} ",name);
    }

    public String getHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch ( Exception ex ) {
            try {
                return InetAddress.getLocalHost().getHostAddress();
            } catch (UnknownHostException e) {
                return "Unknown ip";
            }
        }
    }
}
