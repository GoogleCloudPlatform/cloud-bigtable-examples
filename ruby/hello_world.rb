# frozen_string_literal: true

require 'bundler/setup'
require 'google/cloud/bigtable'
require 'google/cloud/bigtable/admin'

# Connect to bigtable and perform basic operations
# 1. Create table with column family
# 2. Insert rows
# 3. Read rows
# 4. Delete table
class HelloWorld
  attr_accessor :project_id, :instance_id, :keyfile

  # @param project_id [String]
  # @param instance_id [String]
  # @param keyfile [String]
  # 	keyfile .json or .p12 file path. It is optional in case of vm/machine
  # 	have not in authorization scope
  def initialize(project_id, instance_id, keyfile: nil)
    @project_id = project_id
    @instance_id = instance_id
    @keyfile = keyfile
  end

  def do_hello_world
    table_name = 'Hello-Bigtable'
    column_family_name = 'cf'
    column_name = 'greeting'

    table_admin_client = Google::Cloud::Bigtable::Admin::BigtableTableAdmin.new(
      credentials: keyfile
    )

    ## 1. Create table with column familty
    puts "Creating table '#{table_name}'"

    table_admin_client.create_table(
      format_instance_name,
      table_name,
      granularity: 0,
      column_families: {
        column_family_name => Google::Bigtable::Admin::V2::ColumnFamily.new
      }
    )

    bigtable_client = Google::Cloud::Bigtable.new(credentials: keyfile)
    formated_table_name = format_table_name(table_name)

    ## 2. Insert rows
    puts "Write some greetings to the table '#{table_name}'"

    # Each row has a unique row key.
    #
    # Note: This example uses sequential numeric IDs for simplicity, but
    # this can result in poor performance in a production application.
    # Since rows are stored in sorted order by key, sequential keys can
    # result in poor distribution of operations across nodes.
    #
    # For more information about how to design a Bigtable schema for the
    # best performance, see the documentation:
    #
    #	https://cloud.google.com/bigtable/docs/schema-design
    #
    # Insert rows one by one
    greetings = ['Hello World!', 'Hello Bigtable!', 'Hello Ruby!']

    greetings.each_with_index do |value, index|
      puts "  Writing,  Row key: greeting#{index}, Value: #{value}"

      mutation = {
        set_cell: {
          family_name: column_family_name,
          column_qualifier: column_name,
          value: value
        }
      }

      bigtable_client.mutate_row(
        formated_table_name,
        "greeting#{index}",
        [mutation]
      )
    end

    # Note: To perform multiple mutation on multiple rows use `mutate_rows`.

    ## 3. Read rows steam
    puts 'Reading all rows using streaming'

    bigtable_client.read_rows(formated_table_name).each do |response|
      response.chunks.each do |row|
        puts "  Row key: #{row.row_key}, Value: #{row.value}"
      end
    end

    ## 4. Delete table
    puts "Deleting the table '#{table_name}'"

    table_admin_client.delete_table(formated_table_name)
  end

  private

  # Build formated table name
  # i.e projects/my-project/instances/my-instances/tables/Hello-Bigtable
  #
  # @param table_name [String]
  # @return [String]
  def format_table_name(table_name)
    Google::Cloud::Bigtable::Admin::V2::BigtableTableAdminClient.table_path(
      project_id,
      instance_id,
      table_name
    )
  end

  # Build formated instance name
  # i.e projects/my-project/instances/my-instance
  #
  # @return [String]
  def format_instance_name
    Google::Cloud::Bigtable::Admin::V2::BigtableTableAdminClient.instance_path(
      project_id,
      instance_id
    )
  end
end

# Main
hello_world = HelloWorld.new(ENV['PROJECT_ID'], ENV['INSTANCE_ID'])
hello_world.do_hello_world

# Using keyfile
# hello_world = HelloWorld.new(ENV['PROJECT_ID'], ENV['INSTANCE_ID'],
#  keyfile: 'keyfile.json')
