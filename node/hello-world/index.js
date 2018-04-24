// A version of this script is available for node v6 in index.v6.js

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

(async () => {
  try {
    const bigtableClient = bigtable();
    const instance = bigtableClient.instance(INSTANCE_ID);

    const table = instance.table(TABLE_NAME);
    const [tableExists] = await table.exists();
    if (!tableExists) {
      console.log(`Creating table ${TABLE_NAME}`);
      const options = {
        families: [
          {
            name: COLUMN_FAMILY_NAME,
            rule: {
              versions: 1,
            },
          },
        ],
      };
      await table.create(options);
    }

    console.log('Write some greetings to the table');
    const greetings = ['Hello World!', 'Hello Bigtable!', 'Hello Node!'];
    const rowsToInsert = greetings.map((greeting, index) => ({
      key: `greeting${index}`,
      data: {
        [COLUMN_FAMILY_NAME]: {
          [COLUMN_NAME]: {
            value: greeting,
            timestamp: new Date(),
          },
        },
      },
    }));
    await table.insert(rowsToInsert);

    const filter = [{
      column: {
        cellLimit: 1, // Only retrieve the most recent version of the cell.
      },
    }];

    console.log('Reading a single row by row key');
    let [singeRow] = await table.row('greeting0').get({filter});
    console.log(`\tRead: ${getRowGreeting(singeRow)}`);

    console.log('Reading the entire table');
    const [allRows] = await table.getRows({filter});
    for (const row of allRows) {
      console.log(`\tRead: ${getRowGreeting(row)}`);
    }

    console.log('Delete the table');
    await table.delete();
  } catch (error) {
    console.error('Something went wrong:', error);
  }
})();
