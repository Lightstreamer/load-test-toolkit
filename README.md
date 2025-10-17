
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
