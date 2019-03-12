package main

import (
	"context"
	"flag"
	"log"
	"time"

	"./btrefresh"
	"cloud.google.com/go/bigtable"
)

var project = flag.String("project", "", "google cloud project")
var instance = flag.String("bt_instance", "", "cloud bigtable instance")
var table = flag.String("bt_table", "", "cloud bigtable table")

// A row key to read in your table - held constant
// to show the difference in performance with the connection
// instead of bigtable performance.
const rowKey = "0000010000"

// tableRefreshTime is the amount of time between refreshing the connection
// to cloud bigtable.
const tableRefreshTime = 45 * time.Minute

func main() {
	flag.Parse()

	// Create a new bigtable.Table that will refresh the connection periodically.
	table, err := btrefresh.NewRotatingTable(func() (*bigtable.Client, error) {
		return bigtable.NewClient(context.Background(), *project, *instance)
	}, *table, tableRefreshTime)

	if err != nil {
		log.Fatal(err)
	}

	// Watch for background errors from the rotating table.
	go func() {
		for err := range table.BackgroundErrors() {
			log.Fatal(err)
		}
	}()

	latencies := make(chan time.Duration, 1)

	// Every second, print out the largest latency
	// of any one request over that second.
	go func() {
		t := time.NewTicker(1 * time.Second)
		var maxL time.Duration
		for {
			select {
			case <-t.C:
				log.Printf("Max latency over 1s: %v", maxL)
				maxL = 0
			case l := <-latencies:
				if l > maxL {
					maxL = l
				}
			}
		}
	}()

	for {
		start := time.Now()
		_, err := table.ReadRow(context.Background(), rowKey)
		latencies <- time.Since(start)
		if err != nil {
			log.Fatal(err)
		}
	}
}
