'use strict';

const bigtable = require('@google-cloud/bigtable');
const mathjs = require('mathjs');

const COLUMN_FAMILY_NAME = 'cf1';
const COLUMN_NAME = 'isPrime';

const bigtableClient = bigtable();
const instance = bigtableClient.instance('helloworld');
const table = instance.table('cloud-functions');

exports.helloBigtable = (event, callback) => {
  const pubsubMessage = event.data;
  const messageString = Buffer.from(pubsubMessage.data, 'base64').toString();
  const messageObject = JSON.parse(messageString);
  const prefix = messageObject.prefix;
  const count = messageObject.count;

  if (!prefix || !count) {
    throw new Error('Message must contain a prefix and count property!');
  }

  let batch = [];
  for (let index = 0; index < count; index++) {
    batch.push({
      method: 'insert',
      key: `${prefix}-${index}`,
      data: {
        [COLUMN_FAMILY_NAME]: {
          [COLUMN_NAME]: (mathjs.isPrime(index) ? '1' : '0'),
        },
      },
    });
  }

  return Promise.resolve()
    .then(() => console.log('starting...'))
    .then(() => table.mutate(batch))
    .then(() => console.log(`${batch.length} rows written`))
    .catch((error) => {
      if (error.name === 'PartialFailureError') {
        console.warn('Partial Error Detected');
        error.errors.forEach((error) => {
          console.error(error.message);
        });
      } else {
        console.error('Something went wrong:', error);
      }
    })
    .then(() => console.log('done!'))
    .then(callback);
};
