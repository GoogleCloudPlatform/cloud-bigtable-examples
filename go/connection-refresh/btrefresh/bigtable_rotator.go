package btrefresh

import (
	"context"
	"sync"
	"time"

	"cloud.google.com/go/bigtable"
)

// Time to wait before killing the previous connection
// during rotation. This may kill ongoing requests if they
// take longer than this amount of time.
const lameduckTime = 1 * time.Minute

// Number of requests to issue on a new client
// to warm up each underlying client in the pool.
// The default pool size is 4, so we run 2n to have
// good chances of hitting them all.
const nWarmConnections = 2 * 4

// RotatingTable is a bigtable.Table that automatically
// reconnects to Cloud Bigtable at a given interval.
type RotatingTable struct {
	*bigtable.Table
	client *bigtable.Client
	swap   *time.Ticker
	errors chan error
}

// BackgroundErrors is a channel that will propagate errors
// that come from refreshing the connection. It can either be
// checked with select before performing ops or watched
// in a background thread.
func (r RotatingTable) BackgroundErrors() <-chan error {
	return r.errors
}

// Close will close the connections and stop rotating.
func (r RotatingTable) Close() {
	r.swap.Stop()
	close(r.errors)
	r.client.Close()
}

// BtDialer encapsulates making a connection to your bigtable.
// This way you can use any means for dialing/ getting credentials.
type BtDialer func() (*bigtable.Client, error)

// NewRotatingTable makes a new Table reference that will refresh itself in the background at the given
// interval.
func NewRotatingTable(dialer BtDialer, table string, refresh time.Duration) (*RotatingTable, error) {
	client, err := dialer()
	errors := make(chan error, 1)
	if err != nil {
		return nil, err
	}
	tbl := client.Open(table)
	warmTable(tbl)
	ticker := time.NewTicker(refresh)
	rt := &RotatingTable{tbl, client, ticker, errors}
	go func() {
		for range ticker.C {
			// Close the old client after waiting a bit
			go func() {
				oldC := rt.client
				time.Sleep(lameduckTime)
				oldC.Close()
			}()
			client, err := dialer()
			if err != nil {
				errors <- err
				continue
			}
			tbl := client.Open(table)
			warmTable(tbl)
			rt.Table = tbl
		}
	}()
	return rt, nil
}

// A new table reference will need to have some requests
// go across it to establish each connection in the connection
// pool.
func warmTable(tbl *bigtable.Table) {
	wg := sync.WaitGroup{}
	// Run the warming requests across threads to saturate the
	// connection pool.
	for i := 0; i < nWarmConnections; i++ {
		wg.Add(1)
		go func() {
			// Send a request that does not actually return data.
			tbl.ReadRow(context.Background(), "NOT_A_REAL_ROW")
			wg.Done()
		}()
	}
	wg.Wait()
}
