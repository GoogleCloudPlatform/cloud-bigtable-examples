# Cloud Bigtable Python REST Examples

## Instructions

This directory demonstrates some simple ways to interface directly with the 
REST interface via the Python http library requests.

You can install the dependencies by 
 
  `pip install -r requirements.txt`
  
Note: it is always recommended you install Python dependencies in a 
[virtualenv](https://virtualenv.pypa.io/en/latest/).
 
While it's possible to interface with the REST server via a tradational 
command  line tool such as curl, because the values are returned and accepted
in a base64 format, and since you often want to iterate through rows and 
objects, using a programming language like Python which provides excellent
library support for http, JSON, and base64 is recommended.