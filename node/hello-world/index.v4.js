'use strict';

const bigtable = require('@google-cloud/bigtable');

const TABLE_NAME = 'Hello-Bigtable';
const COLUMN_FAMILY_NAME = 'cf1';
const COLUMN_NAME = 'greeting';
const INSTANCE_ID = process.env.INSTANCE_ID;

if (!INSTANCE_ID) {
  console.error(
    'Environment variables for INSTANCE_ID must be set!'
  );
  process.exit(1);
}

const getRowGreeting = (row) => {
  return row.data[COLUMN_FAMILY_NAME][COLUMN_NAME][0].value;
};

let table;

Promise.resolve()
  .then(() => {
    const bigtableClient = bigtable();
    const instance = bigtableClient.instance(INSTANCE_ID);

    /* If the table already exists, add `//` to the start of this line.
    return [instance.table(TABLE_NAME)];
    /*/

    console.log(`Creating table ${TABLE_NAME}`);
    const options = {
      families: [COLUMN_FAMILY_NAME],
    };
    return instance.createTable(TABLE_NAME, options);

    // */
  })
  .then((data) => {
    table = data[0];
    console.log('Write some greetings to the table');
    const greetings = ['Hello World!', 'Hello Bigtable!', 'Hello Node!'];
    const rowsToInsert = greetings.map((greeting, index) => ({
      key: `greeting${index}`,
      data: {
        [COLUMN_FAMILY_NAME]: {
          [COLUMN_NAME]: greeting,
        },
      },
    }));
    return table.insert(rowsToInsert);
  })
  .then(() => {
    console.log('Reading a single row by row key');
    return table.row('greeting0').get();
  })
  .then((data) => {
    const row = data[0];
    console.log(`\tRead: ${getRowGreeting(row)}`);

    console.log('Reading the entire table');
    return table.getRows();
  })
  .then((data) => {
    const rows = data[0];
    for (const row of rows) {
      console.log(`\tRead: ${getRowGreeting(row)}`);
    }
  })
  .then(() => {
    console.log('Delete the table');
    return table.delete();
  })
  .catch((error) => {
    console.error('Something went wrong:', error);
  });
