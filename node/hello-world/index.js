// A version of this script is available for node v4 in index.v4.js

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
        families: [COLUMN_FAMILY_NAME],
      };
      await table.create(options);
    }

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
    await table.insert(rowsToInsert);

    console.log('Reading a single row by row key');
    let [singeRow] = await table.row('greeting0').get();
    console.log(`\tRead: ${getRowGreeting(singeRow)}`);

    console.log('Reading the entire table');
    const [allRows] = await table.getRows();
    for (const row of allRows) {
      console.log(`\tRead: ${getRowGreeting(row)}`);
    }

    console.log('Delete the table');
    await table.delete();
  } catch (error) {
    console.error('Something went wrong:', error);
  }
})();
