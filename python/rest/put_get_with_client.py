__author__ = 'waprin'

import random
from string import ascii_uppercase, digits
import rest_client

base_url = 'http://130.211.170.242:8080'
table_name = 'some-table2'

client = rest_client.HbaseRestClient(base_url, table_name)

row_key = ''.join(random.choice(ascii_uppercase + digits) for _ in range(10))
column = "cf:count"
value = "hello world"

client.put_row(row_key, column, value)
got_value = client.get_row(row_key)
assert got_value == value

client.delete(row_key)
got_value = client.get_row(row_key)
assert got_value is None

print "Done!"
