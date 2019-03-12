# Connection Refresh Example
This is a simple example of how a Cloud Bigtable connection
could be managed to mitigate the periodic connection refreshes
that occur in normal operation. The principle is to start up
a new connection in the background at some interval and run some
RPCs on it before swapping it into the serving path.

Note that this works best for short reads. A single scan will use
just one connection. The lameduck time should be larger than any single
RPC you perform, and the refresh interval should be low enough that an
existing connection should not be close enough to the maximum life + duration
of an RPC (or the serving connection will hit the cutoff and be reset).

The maximum life is here considered to be about an hour.