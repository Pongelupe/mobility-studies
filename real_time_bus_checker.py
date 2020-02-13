#!/usr/bin/env python:
import http.client

connection = http.client.HTTPSConnection('temporeal.pbh.gov.br')
connection.request('GET', '/gtfsrt.php')
response = connection.getresponse()
print(response.read().decode())
connection.close()
