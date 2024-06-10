
Lightstreamer - Load Test Toolkit
=================================

#### Specialized version for Benchmarking the Lightstreamer Kafka Connector

# Introduction

This README will focus exclusively on the specifics of using LLTT within the scope of the [Lightstreamer Kafka Connector Benchmarking Tool](https://github.com/Lightstreamer/lightstreamer-kafka-connector-loadtest) project.
For all other details and instructions, please refer to the [user guide](https://github.com/Lightstreamer/load-test-toolkit/tree/master) of the main branch.

In this context, the part of LLTT that was actually used is the client simulator, which allows generating a load of tens of thousands of concurrent clients connected to the same instance of the Lightstreamer server. The adapter simulator, on the other hand, was not used because the load was generated via Kafka message producers, present in the 'Lightstreamer Kafka Connector Benchmarking Tool' project. This message stream was then sent to a Kafka broker, and through the Lightstreamer Kafka Connector, finally injected into the Lightstreamer server to be dispatched to the various clients.

It is important to note that the adapter simulator must still be installed in Lightstreamer because it is required by the simulated clients for various configurations dictated by a preliminary connection.

Let's examine in detail the necessary configurations for the [various planned test scenarios](https://github.com/Lightstreamer/lightstreamer-kafka-connector-loadtest?tab=readme-ov-file#scenarios).

## Adapter Simulator

After installing the Simulator Adapter in the Lightstreamer server, most default settings can be preserved as the functionalities utilized in this scenario are limited. Before initiating the subscription specific to the test case to be executed, clients establish a preliminary connection and subscribe to a service item associated with this Adapter to receive configurations. In our case, the two crucial parameters that need to be configured for our tests are:
```xml
    <!-- Subscription mode used by the Client Simulators. -->
    <param name="subscriptionMode">RAW</param>

    <!-- Optional. If true, requires tick-by-tick delivery.
        This guarantees that even items with a high frequency
        are received without filtering. If false or missing,
        event filtering is possible, especially in case of
        bandwidth or "MAX_DELAY_MILLIS" constraints. -->
    <param name="unfilteredSubscription">true</param>
```

## Client Simulator

As for the client simulator configuration, the main parameters to set are:
```xml
<client_conf>

    <!-- Protocol, host and port for the Lightstreamer Server connections.
         Supported values for the protocol are "ws://", "wss://", "http://" and "https://".
         Note that the client is optimized for ws or http, whereas, for wss and https,
         the memory and processing power requirements may be significant and
         their usage is not fully optimized.

         Further notes for TLS/SSL tests:
         - In order to run thousands of sessions with a single client process,
           a huge amount of memory may be required; hence, check the supplied
           launch scripts to ensure that any limits on the maximum heap size
           granted to the JVM have been removed.
         - If the client is configured to report latency statistics, the main
           optimizations will be lost and; this should be affordable as long as few
           sessions are configured for such a client.
         - The use of a self-signed TLS/SSL certificate on the Server is supported,
           provided that the certificate is added to the client's set of trusted
           certificates; this can be done by setting the javax.net.ssl.trustStore
           and javax.net.ssl.trustStorePassword JVM properties, as described in
           the Java documentation. -->
    <param name="protocol">ws://</param>
    <param name="host">localhost</param>
    <param name="port">8080</param>

    <param name="scenarioLKC">3td</param>

    <!-- This is the adapter set name of the Adapter Simulator. -->
    <param name="adapterSetName">KafkaConnector</param>
```

### Latency report

To turn on latency reporting on a Client Simulator instance, you need to:
* Set <param name="injectTimestamps"> to true in the adapters.xml file of the Adapter Simulator within Lightstreamer Server installation.
* Set priority value to INFO for the "com.lightstreamer.load_test.reports.latency_reporting" category in the log_conf.xml file of the Client Simulator, before launching the client.
