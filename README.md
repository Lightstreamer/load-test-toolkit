
Lightstreamer - Load Test Toolkit
=================================

## Overview

For a general overview of the Load Test Toolkit and its complete functionality, please refer to the [main branch README](../../tree/main#readme).

#### This branch contains a modified version of the *ClientSimulator* that can operate without the corresponding adapter-side component of LLTT.
The client configuration has been extended to allow specifying a custom Adapter Set and Data Adapter to connect to, as well as defining parameters for session subscription settings.

## New Configuration Parameters

This version introduces several new configuration parameters for enhanced symbol list management and dynamic subscription switching:

### Symbol List Configuration

* **`firstList`** - Comma-separated list of symbols for the first subscription group
  - Example: `item2,item5,item12,item19,item21,item29`
  - These symbols will be used when sessions initially connect or when switching back from the second list

* **`secondList`** - Comma-separated list of symbols for the second subscription group  
  - Example: `item17,item15,item20,item25,item27`
  - These symbols will be used when sessions switch from the first list

### Dynamic Subscription Switching

* **`enableSymbolListSwitching`** - Enable/disable automatic switching between symbol lists
  - Values: `true` or `false`
  - Default: `false`
  - When enabled, sessions will periodically switch between `firstList` and `secondList`
  - When disabled, sessions will only subscribe to `firstList`

* **`symbolListSwitchingPeriodMillis`** - Time interval for switching between symbol lists
  - Values: Any positive integer (milliseconds)
  - Default: `15000` (15 seconds)
  - Example: `30000` for 30-second switching intervals
  - Only applies when `enableSymbolListSwitching` is `true`

### Configuration Example

```xml
<param name="firstList">EURUSD,USDJPY,GBPUSD,USDCAD,AUDUSD</param>
<param name="secondList">EURJPY,GBPJPY,USDCHF,EURGBP,AUDJPY</param>
<param name="enableSymbolListSwitching">true</param>
<param name="symbolListSwitchingPeriodMillis">30000</param>
```

This configuration will make sessions initially subscribe to the first list (EURUSD, USDJPY, etc.), then switch to the second list (EURJPY, GBPJPY, etc.) every 30 seconds, alternating continuously between the two lists.

### Latency Measurement Configuration

The client can measure end-to-end latency by comparing a timestamp embedded in each server update against the local time at the moment the update is received.

* **`tsField4Latency`** - Name of the field in the server update that carries the timestamp
  - Default: `timestamp`
  - The field must be included in `listOfFields` (or sent automatically by the adapter)
  - The client looks for this field in every received update and uses its value to compute the latency

* **`tsDateFormat`** - Pattern used to parse the timestamp field when it contains a human-readable date/time string
  - Follows [`java.time.format.DateTimeFormatter`](https://docs.oracle.com/en/java/docs/api/java.base/java/time/format/DateTimeFormatter.html) conventions
  - If **omitted**, the field value is expected to be a numeric epoch timestamp in milliseconds (i.e. the output of `System.currentTimeMillis()`)
  - If the pattern contains only a time component (no date), the current date is assumed

  | Timestamp value example | `tsDateFormat` value |
  |---|---|
  | `1759747213345` | *(omit parameter — epoch ms assumed)* |
  | `2026-02-26 09:48:56.123` | `yyyy-MM-dd HH:mm:ss.SSS` |
  | `09:48:56` | `HH:mm:ss` |

  The field value may also arrive in the form `fieldname=value` (e.g. `timestamp=1759747213345`); in that case the client automatically strips the prefix before parsing.

#### Configuration Examples

```xml
<!-- Use the field "time" as latency source, formatted as a time-only string -->
<param name="tsField4Latency">time</param>
<param name="tsDateFormat">HH:mm:ss</param>
```

```xml
<!-- Use the field "timestamp" carrying an epoch ms value (default behaviour) -->
<param name="tsField4Latency">timestamp</param>
<!-- tsDateFormat omitted: epoch ms assumed -->
```

> **Note:** Latency reporting is only active when the logger `com.lightstreamer.load_test.reports.latency_reporting` is set to `INFO` level or finer in `log_conf.xml`. When this logger is disabled, the timestamp field is not read and the data can be fully ignored for better performance.

