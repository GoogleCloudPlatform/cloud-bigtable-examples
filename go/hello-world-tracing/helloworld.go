/*
Copyright 2018 Google Inc. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package main

import (
	"flag"
	"fmt"
	"log"
	"net/http"
	"time"

	"cloud.google.com/go/bigtable"
	"go.opencensus.io/exporter/stackdriver"
	ocgrpc "go.opencensus.io/plugin/grpc"
	"go.opencensus.io/stats"
	"go.opencensus.io/trace"
	"go.opencensus.io/zpages"
	"golang.org/x/net/context"
	"google.golang.org/api/option"
	"google.golang.org/grpc"
)

const (
	tableName        = "Hello-Bigtable"
	columnFamilyName = "cf1"
	columnName       = "greeting"
)

var greetings = []string{"Hello World!", "Hello Cloud Bigtable!", "Hello golang!"}

// A minimal application to connect to bigtable, do basic operations
// and demonstrate use of opencensus to push traces and stats to stackdriver
// Usage: go run helloworld.go -project <p> -instance <i>
func main() {
	project := flag.String("project", "", "The Google Cloud Platform project ID. Required.")
	instance := flag.String("instance", "", "The Google Cloud Bigtable instance ID. Required.")
	flag.Parse()

	for _, f := range []string{"project", "instance"} {
		if flag.Lookup(f).Value.String() == "" {
			log.Fatalf("The %s flag is required.", f)
		}
	}

	ctx := context.Background()
	e, err := stackdriver.NewExporter(stackdriver.Options{ProjectID: *project})
	if err != nil {
		log.Fatalf("Could not create stackdriver exporter %v", err)
	}
	trace.RegisterExporter(trace.Exporter(e))
	stats.RegisterExporter(e)

	// Start server on port 8080 for tracing data
	go func() { log.Fatal(http.ListenAndServe(":8080", nil)) }()
	zpages.AddDefaultHTTPHandlers()

	// Always sample in the example
	trace.SetDefaultSampler(trace.AlwaysSample())

	ctx = traceStartSpan(ctx, "cloud.google.com/go/bigtable/example.helloworld")
	doHelloWorld(ctx, *project, *instance)
	traceEndSpan(ctx, err)

	// Sleep for 60 sec to see the output at http://localhost:8080/tracez
	fmt.Println("Pausing to view trace at http://localhost:8080/tracez")
	time.Sleep(60 * time.Second)
}

func sliceContains(list []string, target string) bool {
	for _, s := range list {
		if s == target {
			return true
		}
	}
	return false
}

func openCensusOptions() []option.ClientOption {
	return []option.ClientOption{
		option.WithGRPCDialOption(grpc.WithStatsHandler(ocgrpc.NewClientStatsHandler())),
	}
}

// doHelloWorld connects to Cloud Bigtable, runs basic operations and print the results.
func doHelloWorld(ctx context.Context, project, instance string) {
	tableCount, err := stats.NewMeasureInt64("cloud.google.com/go/bigtable/example.helloworld/numtables", "count of tables", "Num")
	if err != nil {
		log.Fatalf("Table count measure not created: %v", err)
	}

	// Create view to see the processed table count cumulatively.
	view, err := stats.NewView(
		"cloud.google.com/go/bigtable/example.helloworld/views/num_table_view",
		"count of tables",
		nil,
		tableCount,
		stats.CountAggregation{},
		stats.Cumulative{},
	)
	if err != nil {
		log.Fatalf("Cannot create view: %v", err)
	}

	// Set reporting period to report data at every second.
	stats.SetReportingPeriod(1 * time.Second)

	// Subscribe will allow view data to be exported.
	// Once no longer need, you can unsubscribe from the view.
	if err := view.Subscribe(); err != nil {
		log.Fatalf("Cannot subscribe to the view: %v", err)
	}
	defer func() { view.Unsubscribe() }()

	adminClient, err := bigtable.NewAdminClient(ctx, project, instance)
	if err != nil {
		log.Fatalf("Could not create admin client: %v", err)
	}

	tables, err := adminClient.Tables(ctx)
	if err != nil {
		log.Fatalf("Could not fetch table list: %v", err)
	}

	if !sliceContains(tables, tableName) {
		tracePrintf(ctx, nil, "Creating a new table")
		log.Printf("Creating table %s", tableName)
		if err := adminClient.CreateTable(ctx, tableName); err != nil {
			log.Fatalf("Could not create table %s: %v", tableName, err)
		}
	}

	// Record data points.
	stats.Record(ctx, tableCount.M(int64(len(tables)+1)))

	tblInfo, err := adminClient.TableInfo(ctx, tableName)
	if err != nil {
		log.Fatalf("Could not read info for table %s: %v", tableName, err)
	}

	if !sliceContains(tblInfo.Families, columnFamilyName) {
		tracePrintf(ctx, nil, "Creating column family")
		if err := adminClient.CreateColumnFamily(ctx, tableName, columnFamilyName); err != nil {
			log.Fatalf("Could not create column family %s: %v", columnFamilyName, err)
		}
	}

	client, err := bigtable.NewClient(ctx, project, instance, openCensusOptions()...)
	if err != nil {
		log.Fatalf("Could not create data operations client: %v", err)
	}

	tbl := client.Open(tableName)
	muts := make([]*bigtable.Mutation, len(greetings))
	rowKeys := make([]string, len(greetings))

	log.Printf("Writing greeting rows to table")
	for i, greeting := range greetings {
		muts[i] = bigtable.NewMutation()
		muts[i].Set(columnFamilyName, columnName, bigtable.Now(), []byte(greeting))

		rowKeys[i] = fmt.Sprintf("%s%d", columnName, i)
	}

	rowErrs, err := tbl.ApplyBulk(ctx, rowKeys, muts)
	if err != nil {
		log.Fatalf("Could not apply bulk row mutation: %v", err)
	}
	if rowErrs != nil {
		for _, rowErr := range rowErrs {
			log.Printf("Error writing row: %v", rowErr)
		}
		log.Fatalf("Could not write some rows")
	}

	log.Printf("Getting a single greeting by row key:")
	row, err := tbl.ReadRow(ctx, rowKeys[0], bigtable.RowFilter(bigtable.ColumnFilter(columnName)))
	if err != nil {
		log.Fatalf("Could not read row with key %s: %v", rowKeys[0], err)
	}
	log.Printf("\t%s = %s\n", rowKeys[0], string(row[columnFamilyName][0].Value))

	log.Printf("Reading all greeting rows:")
	err = tbl.ReadRows(ctx, bigtable.PrefixRange(columnName), func(row bigtable.Row) bool {
		item := row[columnFamilyName][0]
		log.Printf("\t%s = %s\n", item.Row, string(item.Value))
		return true
	}, bigtable.RowFilter(bigtable.ColumnFilter(columnName)))

	if err = client.Close(); err != nil {
		log.Fatalf("Could not close data operations client: %v", err)
	}

	log.Printf("Deleting the table")
	if err = adminClient.DeleteTable(ctx, tableName); err != nil {
		log.Fatalf("Could not delete table %s: %v", tableName, err)
	}

	if err = adminClient.Close(); err != nil {
		log.Fatalf("Could not close admin client: %v", err)
	}
}

func traceStartSpan(ctx context.Context, name string) context.Context {
	ctx, _ = trace.StartSpan(ctx, name)
	return ctx
}

func traceEndSpan(ctx context.Context, err error) {
	span := trace.FromContext(ctx)
	if err != nil {
		span.SetStatus(trace.Status{Message: err.Error()})
	}

	span.End()
}

func tracePrintf(ctx context.Context, attrMap map[string]interface{}, format string, args ...interface{}) {
	var attrs []trace.Attribute
	for k, v := range attrMap {
		var a trace.Attribute
		switch v := v.(type) {
		case string:
			a = trace.StringAttribute{k, v}
		case bool:
			a = trace.BoolAttribute{k, v}
		case int:
			a = trace.Int64Attribute{k, int64(v)}
		case int64:
			a = trace.Int64Attribute{k, v}
		default:
			a = trace.StringAttribute{k, fmt.Sprintf("%#v", v)}
		}
		attrs = append(attrs, a)
	}
	trace.FromContext(ctx).Annotatef(attrs, format, args...)
}
